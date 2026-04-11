package com.example.musicapp.data.network.dto

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    // Backend's `albumArtObjectKey` is a UUID-shaped storage key, NOT a URL.
    // The actual image lives at `${baseUrl}v1/art/{key}` (publicly served by the
    // backend's HealthHandler.ServeArt before the auth middleware kicks in), and
    // [toDomain] composes the full URL before handing it to Coil.
    @SerialName("albumArtObjectKey")
    val albumArtObjectKey: String? = null,
    val mimeType: String? = null,
    @SerialName("addedAt")
    val addedAtStr: String? = null, // RFC3339 received from backend
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
    val streamUrl: String = "",
)

@Serializable
data class SongPageDto(
    // The backend's `gin.H{"songs": songs}` previously serialized a nil Go slice
    // as JSON `null`, which kotlinx-serialization refuses to deserialize into a
    // non-nullable List even with a default. The backend has been fixed to always
    // return `[]`, but we keep `items` nullable defensively so a server regression
    // doesn't silently break sync polling again.
    @SerialName("songs")
    val items: List<SongDto>? = null,
    val nextCursor: String? = null,
) {
    val safeItems: List<SongDto> get() = items ?: emptyList()
}

@Serializable
data class HomeSectionDto(
    val title: String,
    val songs: List<SongDto>,
)

@Serializable
data class HomeResponseDto(
    val sections: List<HomeSectionDto>,
)

@Serializable
data class ArtistGroupDto(
    val name: String,
    val songCount: Int,
    val primaryArtworkUrl: String? = null,
)

@Serializable
data class AlbumGroupDto(
    val name: String,
    val artist: String,
    val songCount: Int,
    val primaryArtworkUrl: String? = null,
)

fun SongDto.toDomain(): Song {
    val baseUrl = com.example.musicapp.data.BuildConfig.SPOTIFISH_BASE_URL
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        // Compose the public art URL from the backend's object key. Coil treats
        // this as a regular HTTPS image source — no Authorization header needed
        // because the /v1/art/:key route is registered before the auth middleware
        // on the backend.
        albumArtUri = albumArtObjectKey
            ?.takeIf { it.isNotBlank() }
            ?.let { "${baseUrl}v1/art/$it" },
        sourceType = SourceType.DRIVE,
        // Construct the backend stream URL locally since the generic song list doesn't supply it.
        playableUri = streamUrl.ifBlank { "${baseUrl}v1/songs/$id/stream" },
        mimeType = mimeType,
        // Parse the RFC3339 date string returned from Go Time to Unix MS.
        addedAtEpochMillis = addedAtStr
            ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: addedAtEpochMillis,
        authAccountEmail = null,
    )
}
