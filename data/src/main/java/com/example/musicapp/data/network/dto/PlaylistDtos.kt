package com.example.musicapp.data.network.dto

import com.example.musicapp.domain.model.Playlist
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class CreatePlaylistRequest(val name: String)

@Serializable
data class RenamePlaylistRequest(val name: String)

@Serializable
data class AddSongRequest(val songId: String)

@Serializable
data class ReorderSongsRequest(val songIds: List<String>)

fun PlaylistDto.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    songIds = songIds,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
