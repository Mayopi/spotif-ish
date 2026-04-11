package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.data.network.dto.AddSongRequest
import com.example.musicapp.data.network.dto.CreatePlaylistRequest
import com.example.musicapp.data.network.dto.RenamePlaylistRequest
import com.example.musicapp.data.network.dto.toDomain
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RemotePlaylistRepository @Inject constructor(
    private val api: SpotifishApi,
    private val dispatchersProvider: DispatchersProvider,
) : PlaylistRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())

    init {
        scope.launch { runCatching { reload() } }
    }

    override fun observePlaylists(): Flow<List<Playlist>> = playlists

    override suspend fun create(name: String) {
        api.createPlaylist(CreatePlaylistRequest(name = name))
        reload()
    }

    override suspend fun rename(playlistId: String, name: String) {
        api.renamePlaylist(playlistId, RenamePlaylistRequest(name = name))
        reload()
    }

    override suspend fun delete(playlistId: String) {
        api.deletePlaylist(playlistId)
        reload()
    }

    override suspend fun addSong(playlistId: String, songId: String) {
        api.addSongToPlaylist(playlistId, AddSongRequest(songId = songId))
        reload()
    }

    override suspend fun removeSong(playlistId: String, songId: String) {
        api.removeSongFromPlaylist(playlistId, songId)
        reload()
    }

    private suspend fun reload() {
        playlists.value = withContext(dispatchersProvider.io) {
            api.listPlaylists().map { it.toDomain() }
        }
    }
}
