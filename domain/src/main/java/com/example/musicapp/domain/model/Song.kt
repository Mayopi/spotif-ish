package com.example.musicapp.domain.model

enum class SourceType {
    LOCAL,
    DRIVE,
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: String? = null,
    val sourceType: SourceType,
    val playableUri: String,
    val mimeType: String? = null,
    val isFavorite: Boolean = false,
    val addedAtEpochMillis: Long = 0L,
)

