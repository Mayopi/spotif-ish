package com.example.musicapp.domain.model

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

data class PlaybackQueue(
    val items: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

