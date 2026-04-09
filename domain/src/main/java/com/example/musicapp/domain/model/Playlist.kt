package com.example.musicapp.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

