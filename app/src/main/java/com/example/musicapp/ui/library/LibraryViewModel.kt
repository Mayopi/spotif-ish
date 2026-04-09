package com.example.musicapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.CreatePlaylistUseCase
import com.example.musicapp.domain.usecase.ObservePlaylistsUseCase
import com.example.musicapp.domain.usecase.ObserveSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    observeSongsUseCase: ObserveSongsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        observePlaylistsUseCase(),
        observeSongsUseCase(),
    ) { playlists, songs ->
        LibraryUiState(playlists = playlists, songs = songs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun createPlaylist() {
        viewModelScope.launch {
            createPlaylistUseCase("My Playlist")
        }
    }
}

