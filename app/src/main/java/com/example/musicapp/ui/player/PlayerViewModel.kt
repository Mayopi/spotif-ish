package com.example.musicapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentSong: Song? = null,
    val currentSongTitle: String = "Nothing playing",
    val currentSongArtist: String = "",
    val currentSongAlbum: String = "",
    val albumArtUri: String? = null,
    val isPlaying: Boolean = false,
    val queueSize: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val hasSong: Boolean get() = currentSong != null
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playbackController.observeState()
        .map { state ->
            val song = state.currentSong
            PlayerUiState(
                currentSong = song,
                currentSongTitle = song?.title ?: "Nothing playing",
                currentSongArtist = song?.artist.orEmpty(),
                currentSongAlbum = song?.album.orEmpty(),
                albumArtUri = song?.albumArtUri,
                isPlaying = state.isPlaying,
                queueSize = state.queue.items.size,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun togglePlayPause() {
        viewModelScope.launch { playbackController.togglePlayPause() }
    }

    fun skipNext() {
        viewModelScope.launch { playbackController.skipNext() }
    }

    fun skipPrevious() {
        viewModelScope.launch { playbackController.skipPrevious() }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch { playbackController.seekTo(positionMs) }
    }
}
