package com.example.musicapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentSongTitle: String = "Nothing playing",
    val currentSongArtist: String = "",
    val isPlaying: Boolean = false,
    val queueSize: Int = 0,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playbackController.observeState()
        .map { state ->
            PlayerUiState(
                currentSongTitle = state.currentSong?.title ?: "Nothing playing",
                currentSongArtist = state.currentSong?.artist.orEmpty(),
                isPlaying = state.isPlaying,
                queueSize = state.queue.items.size,
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
}

