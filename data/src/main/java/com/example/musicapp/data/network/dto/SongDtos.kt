package com.example.musicapp.data.network.dto

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUrl: String? = null,
    val mimeType: String? = null,
    val addedAtEpochMillis: Long,
    /**
     * Backend-issued opaque stream URL. The client never constructs this itself; it
     * just hands it to ExoPlayer (with the AuthInterceptor attached).
     */
    val streamUrl: String,
)

@Serializable
data class SongPageDto(
    val items: List<SongDto>,
    val nextCursor: String? = null,
)

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

fun SongDto.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    albumArtUri = albumArtUrl,
    sourceType = SourceType.DRIVE,
    // The opaque backend stream URL is what ExoPlayer plays. The AuthInterceptor on
    // the OkHttp data source factory attaches the bearer token at request time.
    playableUri = streamUrl,
    mimeType = mimeType,
    addedAtEpochMillis = addedAtEpochMillis,
    // No per-song Drive auth on the client anymore — the backend owns it.
    authAccountEmail = null,
)
