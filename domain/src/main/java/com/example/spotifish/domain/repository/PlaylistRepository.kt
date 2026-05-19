package com.example.spotifish.domain.repository

import com.example.spotifish.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun create(name: String)
    suspend fun rename(playlistId: String, name: String)
    suspend fun delete(playlistId: String)
    suspend fun addSong(playlistId: String, songId: String)
    suspend fun removeSong(playlistId: String, songId: String)
}

