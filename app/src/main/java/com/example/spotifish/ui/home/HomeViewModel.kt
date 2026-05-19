package com.example.spotifish.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifish.domain.model.HomeSection
import com.example.spotifish.domain.model.Song
import com.example.spotifish.domain.player.PlaybackController
import com.example.spotifish.domain.usecase.ObserveHomeSectionsUseCase
import com.example.spotifish.domain.usecase.RecordPlaybackStartedUseCase
import com.example.spotifish.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val sections: List<HomeSection> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeSectionsUseCase: ObserveHomeSectionsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordPlaybackStartedUseCase: RecordPlaybackStartedUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeHomeSectionsUseCase()
        .map { HomeUiState(sections = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleFavorite(songId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(songId) }
    }

    fun playSong(song: Song, queue: List<Song>) {
        viewModelScope.launch {
            runCatching { recordPlaybackStartedUseCase(song.id) }
            playbackController.play(song, queue)
        }
    }
}
