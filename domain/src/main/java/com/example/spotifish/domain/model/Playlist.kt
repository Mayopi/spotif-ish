package com.example.spotifish.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val songCount: Int = songIds.size,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
