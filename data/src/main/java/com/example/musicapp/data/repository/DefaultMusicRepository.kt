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
    private val favoritesRepository: FavoritesRepository,
    private val dispatchersProvider: DispatchersProvider,
) : MusicRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val driveSongs = MutableStateFlow<List<Song>>(emptyList())
    private val driveSyncState = MutableStateFlow(DriveSyncState())
    private var driveRefreshJob: Job? = null

    init {
        scope.launch { refreshLocalLibrary() }
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
        // Reset the Drive library at the start of a sync so stale entries from a
        // previous folder selection are cleared. Songs are appended incrementally as
        // they are discovered below, so the UI updates in real time.
        driveSongs.value = emptyList()
        driveSyncState.value = DriveSyncState(
            isSyncing = true,
            lastError = null,
            lastSyncedSongCount = 0,
            processedFileCount = 0,
        )

        val finalSongs = withContext(dispatchersProvider.io) {
            driveMusicDataSource.fetchSongs(
                onSongDiscovered = { song ->
                    // Append the freshly discovered song and push an updated state so the
                    // home/library/search screens render it immediately instead of waiting
                    // for the whole scan to finish.
                    val updated = driveSongs.value + song
                    driveSongs.value = updated
                    driveSyncState.value = DriveSyncState(
                        isSyncing = true,
                        lastError = null,
                        lastSyncedSongCount = updated.size,
                        processedFileCount = updated.size,
                    )
                },
                onProgress = null,
            )
        }

        // Replace with the final, deterministic ordering once the scan finishes so the
        // user sees a stable list after the live stream settles.
        driveSongs.value = finalSongs
        driveSyncState.value = DriveSyncState(
            isSyncing = false,
            lastError = null,
            lastSyncedSongCount = finalSongs.size,
            processedFileCount = finalSongs.size,
        )
    }

    override fun enqueueDriveLibraryRefresh() {
        if (driveRefreshJob?.isActive == true) return
        driveRefreshJob = scope.launch {
            runCatching {
                refreshDriveLibrary()
            }.onFailure { throwable ->
                // Keep whatever songs were already streamed in so the UI still shows them
                // even though the overall sync failed partway through.
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
        if (normalized.isBlank()) return observeAllSongs().map { it.take(20) }.firstValue()
        return observeAllSongs().map { songs ->
            songs.filter { song ->
                song.title.lowercase().contains(normalized) ||
                    song.artist.lowercase().contains(normalized) ||
                    song.album.lowercase().contains(normalized)
            }
        }.firstValue()
    }

    override suspend fun getSong(songId: String): Song? {
        return observeAllSongs().map { songs -> songs.firstOrNull { it.id == songId } }.firstValue()
    }
}

private suspend fun <T> Flow<T>.firstValue(): T {
    return first()
}
