package com.example.spotifish.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifish.domain.model.Song
import com.example.spotifish.domain.player.PlaybackController
import com.example.spotifish.domain.usecase.RecordPlaybackStartedUseCase
import com.example.spotifish.domain.usecase.SearchSongsUseCase
import com.example.spotifish.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordPlaybackStartedUseCase: RecordPlaybackStartedUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val debouncedQuery = MutableSharedFlow<String>(replay = 1)

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        debouncedQuery
            .debounce(300)
            .flatMapLatest { currentQuery ->
                flow { emit(searchSongsUseCase(currentQuery)) }
            },
    ) { currentQuery, results ->
        SearchUiState(query = currentQuery, results = results)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        viewModelScope.launch {
            debouncedQuery.emit("")
        }
    }

    fun updateQuery(value: String) {
        query.value = value
        viewModelScope.launch {
            debouncedQuery.emit(value)
        }
    }

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
