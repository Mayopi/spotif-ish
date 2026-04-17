package com.example.musicapp.data.repository

import android.util.Log
import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.local.LocalMusicDataSource
import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.data.network.dto.toDomain
import com.example.musicapp.domain.model.DriveSyncState
import com.example.musicapp.domain.model.HomeSection
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import com.example.musicapp.domain.repository.FavoritesRepository
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Remote-first implementation of [MusicRepository].
 *
 * The Drive sync, metadata extraction, album-art persistence, and library state are
 * now all owned by the backend. This class:
 *
 * 1. Pulls the canonical library list from `GET /v1/songs`
 * 2. Merges it with the device's local-scanned songs (since local scanning still
 *    happens client-side per the locked-in decision in BACKEND_PRD §6 + §14)
 * 3. Folds in favorites
 * 4. Polls `GET /v1/sync/status` while a sync is running so the Settings UI can
 *    show live progress without WebSockets
 *
 * The previous local Drive sync layer (DriveLibraryStore, DriveTokenStore,
 * GoogleDriveMusicDataSource, DriveAuthSessionStore) is intentionally NOT a
 * dependency here — that whole stack is being deleted in the same change set.
 */
@Singleton
class RemoteMusicRepository @Inject constructor(
    private val api: SpotifishApi,
    private val localMusicDataSource: LocalMusicDataSource,
    private val favoritesRepository: FavoritesRepository,
    private val dispatchersProvider: DispatchersProvider,
) : MusicRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val remoteSongs = MutableStateFlow<List<Song>>(emptyList())
    private val driveSyncState = MutableStateFlow(DriveSyncState())
    private val remoteFetchMutex = Mutex()
    private var syncPollJob: Job? = null

    init {
        scope.launch {
            // Local MediaStore scan still runs every cold start.
            refreshLocalLibrary()
            // Pull whatever the backend currently knows about so the UI has data
            // before any explicit sync runs.
            runCatching { fetchRemoteLibrary() }
        }
    }

    override fun observeAllSongs(): Flow<List<Song>> {
        return combine(
            localSongs,
            remoteSongs,
            favoritesRepository.observeFavorites(),
        ) { local, remote, favorites ->
            (local + remote)
                .distinctBy { it.id }
                .map { song -> song.copy(isFavorite = song.id in favorites) }
                .sortedWith(compareBy<Song> { it.title.lowercase() }.thenBy { it.artist.lowercase() })
        }.distinctUntilChanged()
    }

    override fun observeHomeSections(): Flow<List<HomeSection>> {
        return observeAllSongs().map { songs ->
            listOf(
                HomeSection("Recently Added", songs.sortedByDescending { it.addedAtEpochMillis }.take(12)),
                HomeSection("Local Library", songs.filter { it.sourceType == SourceType.LOCAL }.take(12)),
                HomeSection("Drive Library", songs.filter { it.sourceType == SourceType.DRIVE }.take(12)),
                HomeSection("All Songs", songs.take(20)),
            ).filter { it.songs.isNotEmpty() }
        }.distinctUntilChanged()
    }

    override fun observeDriveSyncState(): Flow<DriveSyncState> = driveSyncState

    override suspend fun refreshLocalLibrary() {
        val scanned = withContext(dispatchersProvider.io) {
            localMusicDataSource.scan()
        }
        if (scanned != localSongs.value) {
            localSongs.value = scanned
        }
    }

    override suspend fun refreshDriveLibrary() {
        // The actual Drive scan runs on the backend. From the client's POV "refresh"
        // means: ask the server to enqueue a sync, then poll its status until done,
        // and repull the song list at the end.
        val response = api.runSync()
        pollSyncStatus(jobId = response.syncJobId)
    }

    override fun enqueueDriveLibraryRefresh() {
        if (syncPollJob?.isActive == true) return
        syncPollJob = scope.launch {
            runCatching {
                val response = api.runSync()
                pollSyncStatus(jobId = response.syncJobId)
            }.onFailure { throwable ->
                driveSyncState.value = DriveSyncState(
                    isSyncing = false,
                    lastError = throwable.message ?: "Sync failed.",
                    lastSyncedSongCount = remoteSongs.value.size,
                    processedFileCount = remoteSongs.value.size,
                )
            }
        }
    }

    override fun pauseDriveLibraryRefresh() {
        scope.launch {
            runCatching {
                val response = api.pauseSync()
                // Reflect the pause locally before the next poll lands so the UI
                // flips immediately. The poll loop will catch up on the next tick
                // and overwrite this with the persisted backend state.
                driveSyncState.value = driveSyncState.value.copy(
                    isSyncing = false,
                    isPaused = true,
                )
                // Stop the active poll loop — it'll be restarted by the resume call.
                syncPollJob?.cancel()
                syncPollJob = null
                response // unused, but keeps the runCatching shape consistent
            }.onFailure { throwable ->
                driveSyncState.value = driveSyncState.value.copy(
                    lastError = throwable.message ?: "Pause failed.",
                )
            }
        }
    }

    override fun resumeDriveLibraryRefresh() {
        if (syncPollJob?.isActive == true) return
        syncPollJob = scope.launch {
            runCatching {
                val response = api.resumeSync()
                driveSyncState.value = driveSyncState.value.copy(
                    isSyncing = true,
                    isPaused = false,
                )
                pollSyncStatus(jobId = response.syncJobId)
            }.onFailure { throwable ->
                driveSyncState.value = driveSyncState.value.copy(
                    isSyncing = false,
                    lastError = throwable.message ?: "Resume failed.",
                )
            }
        }
    }

    override suspend fun search(query: String): List<Song> {
        val normalized = query.trim()
        if (normalized.isBlank()) return observeAllSongs().map { it.take(20) }.first()
        // Server search hits the canonical library; we still want client-side
        // filtering on local-only songs so they show up in search results too.
        return runCatching {
            val remote = api.searchSongs(normalized).safeItems.map { it.toDomain() }
            val local = localSongs.value.filter { song ->
                val q = normalized.lowercase()
                song.title.lowercase().contains(q) ||
                    song.artist.lowercase().contains(q) ||
                    song.album.lowercase().contains(q)
            }
            (local + remote).distinctBy { it.id }
        }.getOrElse {
            // Network failure → fall back to local-only filter so the search still
            // returns something usable.
            observeAllSongs().map { songs ->
                songs.filter { song ->
                    val q = normalized.lowercase()
                    song.title.lowercase().contains(q) ||
                        song.artist.lowercase().contains(q) ||
                        song.album.lowercase().contains(q)
                }
            }.first()
        }
    }

    override suspend fun getSong(songId: String): Song? {
        return observeAllSongs().map { songs -> songs.firstOrNull { it.id == songId } }.first()
    }

    private suspend fun fetchRemoteLibrary() {
        remoteFetchMutex.withLock {
            val collected = mutableListOf<Song>()
            var cursor: String? = null
            do {
                // Backend caps limit at 1000, which comfortably fits any realistic
                // Spotifish library — keeps the polling refresh as a single round trip
                // and avoids the broken cursor pagination path entirely.
                val page = api.listSongs(cursor = cursor, limit = 1000)
                collected += page.safeItems.map { it.toDomain() }
                cursor = page.nextCursor
            } while (cursor != null)
            if (collected != remoteSongs.value) {
                remoteSongs.value = collected
            }
        }
    }

    private suspend fun pollSyncStatus(jobId: String) {
        // Backend sync is async — poll its status flow into our DriveSyncState so the
        // existing Settings sync card keeps working without changes.
        //
        // Now that the backend persists processed_count after every song, we can
        // pull a fresh page of songs on every poll instead of every Nth poll. The
        // user perceives synced songs landing in the library in near-real time
        // (~POLL_INTERVAL_MILLIS lag) rather than waiting for the whole job to
        // finish.
        var attempts = 0
        var lastFetchedProcessedCount = -1
        while (true) {
            val statusResult = runCatching { api.syncStatus() }
            val status = statusResult.getOrNull()
            if (status == null) {
                statusResult.exceptionOrNull()?.let {
                    Log.w(TAG, "syncStatus poll failed for job=$jobId", it)
                }
                break
            }
            driveSyncState.value = status.toDomain()
            val isTerminalState =
                status.state == "succeeded" || status.state == "failed" || status.state == "paused"
            val shouldRefreshLibrary =
                isTerminalState || status.processedCount != lastFetchedProcessedCount
            if (shouldRefreshLibrary) {
                // Fetch song catalog only when progress changed (or terminal state)
                // to avoid redundant full-library pulls that churn Compose lists.
                runCatching { fetchRemoteLibrary() }
                    .onSuccess { lastFetchedProcessedCount = status.processedCount }
                    .onFailure {
                        Log.w(
                            TAG,
                            "fetchRemoteLibrary during sync poll failed for job=$jobId: ${it.message}",
                            it,
                        )
                    }
            }
            // Stop polling on terminal states. 'paused' is also terminal from the
            // poll loop's perspective — the loop is restarted by resumeDriveLibraryRefresh.
            if (isTerminalState) break
            attempts += 1
            if (attempts > MAX_POLL_ATTEMPTS) break
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private companion object {
        private const val TAG = "RemoteMusicRepo"
        private const val POLL_INTERVAL_MILLIS = 1_500L
        private const val MAX_POLL_ATTEMPTS = 600 // 15 minutes at 1.5s intervals
    }
}
