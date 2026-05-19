package com.example.spotifish.domain.player

import com.example.spotifish.domain.model.PlaybackQueue
import com.example.spotifish.domain.model.Song
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
    suspend fun toggleShuffle()
    suspend fun skipNext()
    suspend fun skipPrevious()
    suspend fun seekTo(positionMs: Long)
}
