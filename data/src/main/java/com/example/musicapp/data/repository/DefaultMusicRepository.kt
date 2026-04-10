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
        val previousCount = driveSyncState.value.lastSyncedSongCount
        val songs = withContext(dispatchersProvider.io) {
            driveMusicDataSource.fetchSongs { processedCount ->
                driveSyncState.value = DriveSyncState(
                    isSyncing = true,
                    lastError = null,
                    lastSyncedSongCount = previousCount,
                    processedFileCount = processedCount,
                )
            }
        }
        driveSongs.value = songs
        driveSyncState.value = DriveSyncState(
            isSyncing = false,
            lastError = null,
            lastSyncedSongCount = songs.size,
            processedFileCount = songs.size,
        )
    }

    override fun enqueueDriveLibraryRefresh() {
        if (driveRefreshJob?.isActive == true) return
        driveRefreshJob = scope.launch {
            val previousCount = driveSyncState.value.lastSyncedSongCount
            driveSyncState.value = DriveSyncState(
                isSyncing = true,
                lastError = null,
                lastSyncedSongCount = previousCount,
                processedFileCount = 0,
            )
            runCatching {
                refreshDriveLibrary()
            }.onFailure { throwable ->
                driveSyncState.value = DriveSyncState(
                    isSyncing = false,
                    lastError = throwable.message ?: "Drive sync failed.",
                    lastSyncedSongCount = previousCount,
                    processedFileCount = driveSyncState.value.processedFileCount,
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
