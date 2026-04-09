package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.store.JsonFileStore
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.repository.PlaylistRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DefaultPlaylistRepository @Inject constructor(
    private val jsonFileStore: JsonFileStore,
    private val dispatchersProvider: DispatchersProvider,
) : PlaylistRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())

    init {
        scope.launch {
            playlists.value = loadPlaylists()
        }
    }

    override fun observePlaylists() = playlists.asStateFlow()

    override suspend fun create(name: String) {
        val now = System.currentTimeMillis()
        val updated = playlists.value + Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            songIds = emptyList(),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playlists.value = updated
        persist(updated)
    }

    override suspend fun rename(playlistId: String, name: String) {
        mutate(playlistId) { it.copy(name = name, updatedAtEpochMillis = System.currentTimeMillis()) }
    }

    override suspend fun delete(playlistId: String) {
        val updated = playlists.value.filterNot { it.id == playlistId }
        playlists.value = updated
        persist(updated)
    }

    override suspend fun addSong(playlistId: String, songId: String) {
        mutate(playlistId) { playlist ->
            playlist.copy(
                songIds = (playlist.songIds + songId).distinct(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun removeSong(playlistId: String, songId: String) {
        mutate(playlistId) { playlist ->
            playlist.copy(
                songIds = playlist.songIds - songId,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun mutate(playlistId: String, transform: (Playlist) -> Playlist) {
        val updated = playlists.value.map { playlist ->
            if (playlist.id == playlistId) transform(playlist) else playlist
        }
        playlists.value = updated
        persist(updated)
    }

    private suspend fun loadPlaylists(): List<Playlist> = withContext(dispatchersProvider.io) {
        val content = jsonFileStore.read(FILE_NAME, "{\"playlists\":[]}")
        val root = JSONObject(content)
        val array = root.optJSONArray("playlists") ?: JSONArray()
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    Playlist(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        songIds = item.getJSONArray("songs").let { songs ->
                            buildList {
                                repeat(songs.length()) { songIndex ->
                                    add(songs.getString(songIndex))
                                }
                            }
                        },
                        createdAtEpochMillis = item.optLong("createdAt", 0L),
                        updatedAtEpochMillis = item.optLong("updatedAt", 0L),
                    ),
                )
            }
        }
    }

    private suspend fun persist(value: List<Playlist>) = withContext(dispatchersProvider.io) {
        val playlistsArray = JSONArray()
        value.forEach { playlist ->
            playlistsArray.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("songs", JSONArray(playlist.songIds))
                    .put("createdAt", playlist.createdAtEpochMillis)
                    .put("updatedAt", playlist.updatedAtEpochMillis),
            )
        }
        jsonFileStore.write(FILE_NAME, JSONObject().put("playlists", playlistsArray).toString())
    }

    private companion object {
        const val FILE_NAME = "playlists.json"
    }
}

