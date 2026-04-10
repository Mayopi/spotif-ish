package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.drive.DriveMusicDataSource
import com.example.musicapp.data.local.LocalMusicDataSource
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class DefaultMusicRepository @Inject constructor(
    private val localMusicDataSource: LocalMusicDataSource,
    private val driveMusicDataSource: DriveMusicDataSource,
    private val driveLibraryStore: DriveLibraryStore,
    private val favoritesRepository: FavoritesRepository,
    private val dispatchersProvider: DispatchersProvider,
) : MusicRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val driveSongs = MutableStateFlow<List<Song>>(emptyList())
    private val driveSyncState = MutableStateFlow(DriveSyncState())
    private var driveRefreshJob: Job? = null

    init {
        scope.launch {
            // Hydrate the in-memory Drive library from disk before kicking off any
            // network sync. Without this, the home/library screens would flash an
            // empty state every time the app restarts until the next refresh runs.
            val cached = driveLibraryStore.load()
            if (cached.isNotEmpty()) {
                driveSongs.value = cached
                driveSyncState.value = DriveSyncState(
                    isSyncing = false,
                    lastError = null,
                    lastSyncedSongCount = cached.size,
                    processedFileCount = cached.size,
                )
            }
            refreshLocalLibrary()
        }
    }

    override fun observeAllSongs(): Flow<List<Song>> {
        return combine(localSongs, driveSongs, favoritesRepository.observeFavorites()) { local, drive, favorites ->
            (local + drive)
                .map { song -> song.copy(isFavorite = song.id in favorites) }
                .sortedWith(compareBy<Song> { it.title.lowercase() }.thenBy { it.artist.lowercase() })
        }
    }

    override fun observeHomeSections(): Flow<List<HomeSection>> {
        return observeAllSongs().map { songs ->
            listOf(
                HomeSection("Recently Added", songs.sortedByDescending { it.addedAtEpochMillis }.take(12)),
                HomeSection("Local Library", songs.filter { it.sourceType == SourceType.LOCAL }.take(12)),
                HomeSection("Drive Library", songs.filter { it.sourceType == SourceType.DRIVE }.take(12)),
                HomeSection("All Songs", songs.take(20)),
            ).filter { it.songs.isNotEmpty() }
        }
    }

    override fun observeDriveSyncState(): Flow<DriveSyncState> = driveSyncState

    override suspend fun refreshLocalLibrary() {
        localSongs.value = withContext(dispatchersProvider.io) {
            localMusicDataSource.scan()
        }
    }

    override suspend fun refreshDriveLibrary() {
        // The current in-memory list (which itself was hydrated from disk on launch)
        // is the change-detection baseline: any file whose Drive `modifiedTime`
        // matches a cached entry's `addedAtEpochMillis` is reused as-is, skipping the
        // expensive prefix download + MediaMetadataRetriever step.
        val baselineById = driveSongs.value.associateBy { it.id }

        driveSyncState.value = DriveSyncState(
            isSyncing = true,
            lastError = null,
            lastSyncedSongCount = baselineById.size,
            processedFileCount = 0,
        )

        var processed = 0
        val finalSongs = withContext(dispatchersProvider.io) {
            driveMusicDataSource.fetchSongs(
                previousById = baselineById,
                onSongDiscovered = { song ->
                    processed += 1
                    val cached = baselineById[song.id]
                    // Only push UI updates for songs that are actually new or changed.
                    // Cached songs are already in driveSongs from the previous run, so
                    // re-emitting them just churns Compose without changing anything.
                    if (cached == null || cached != song) {
                        val current = driveSongs.value
                        driveSongs.value = current.upsertById(song)
                    }
                    driveSyncState.value = DriveSyncState(
                        isSyncing = true,
                        lastError = null,
                        lastSyncedSongCount = driveSongs.value.size,
                        processedFileCount = processed,
                    )
                },
                onProgress = null,
            )
        }

        // Replace with the canonical post-scan list. This implicitly drops any songs
        // that the scan didn't visit — i.e. files deleted from Drive between syncs —
        // so the persisted library always reflects the current state of the folder.
        val sorted = finalSongs.sortedByDescending { it.addedAtEpochMillis }
        driveSongs.value = sorted
        driveLibraryStore.save(sorted)
        driveSyncState.value = DriveSyncState(
            isSyncing = false,
            lastError = null,
            lastSyncedSongCount = sorted.size,
            processedFileCount = sorted.size,
        )
    }

    override fun enqueueDriveLibraryRefresh() {
        if (driveRefreshJob?.isActive == true) return
        driveRefreshJob = scope.launch {
            runCatching {
                refreshDriveLibrary()
            }.onFailure { throwable ->
                // Persist whatever we managed to scan so the partial library survives
                // restarts even if the sync errored out partway through.
                runCatching { driveLibraryStore.save(driveSongs.value) }
                val partialCount = driveSongs.value.size
                driveSyncState.value = DriveSyncState(
                    isSyncing = false,
                    lastError = throwable.message ?: "Drive sync failed.",
                    lastSyncedSongCount = partialCount,
                    processedFileCount = partialCount,
                )
            }
        }
    }

    override suspend fun search(query: String): List<Song> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return observeAllSongs().map { it.take(20) }.first()
        return observeAllSongs().map { songs ->
            songs.filter { song ->
                song.title.lowercase().contains(normalized) ||
                    song.artist.lowercase().contains(normalized) ||
                    song.album.lowercase().contains(normalized)
            }
        }.first()
    }

    override suspend fun getSong(songId: String): Song? {
        return observeAllSongs().map { songs -> songs.firstOrNull { it.id == songId } }.first()
    }

    private fun List<Song>.upsertById(song: Song): List<Song> {
        val index = indexOfFirst { it.id == song.id }
        return if (index >= 0) {
            toMutableList().also { it[index] = song }
        } else {
            this + song
        }
    }
}
