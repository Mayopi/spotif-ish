package com.example.spotifish.data.network.dto

import com.example.spotifish.domain.model.Playlist
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val songIds: List<String>? = null,
    val songCount: Int? = null,
    @SerialName("createdAt")
    val createdAtIso: String? = null,
    @SerialName("updatedAt")
    val updatedAtIso: String? = null,
    val createdAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long? = null,
)

@Serializable
data class PlaylistListResponseDto(
    val playlists: List<PlaylistDto>? = null,
) {
    val safePlaylists: List<PlaylistDto> get() = playlists ?: emptyList()
}

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
    songIds = songIds ?: emptyList(),
    songCount = songCount ?: songIds?.size ?: 0,
    createdAtEpochMillis = createdAtEpochMillis
        ?: createdAtIso?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: 0L,
    updatedAtEpochMillis = updatedAtEpochMillis
        ?: updatedAtIso?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: createdAtEpochMillis
        ?: 0L,
)
