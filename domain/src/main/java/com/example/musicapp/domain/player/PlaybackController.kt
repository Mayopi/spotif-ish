package com.example.musicapp.domain.player

import com.example.musicapp.domain.model.PlaybackQueue
import com.example.musicapp.domain.model.Song
import kotlinx.coroutines.flow.Flow

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: PlaybackQueue = PlaybackQueue(),
)

interface PlaybackController {
    fun observeState(): Flow<PlaybackState>
    suspend fun play(song: Song, queue: List<Song>)
    suspend fun togglePlayPause()
    suspend fun skipNext()
    suspend fun skipPrevious()
    suspend fun seekTo(positionMs: Long)
}

