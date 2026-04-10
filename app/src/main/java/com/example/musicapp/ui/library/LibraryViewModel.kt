package com.example.musicapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.usecase.CreatePlaylistUseCase
import com.example.musicapp.domain.usecase.ObservePlaylistsUseCase
import com.example.musicapp.domain.usecase.ObserveSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    Playlists("Playlists"),
    Songs("Songs"),
    Artists("Artists"),
    Albums("Albums"),
}

/**
 * Aggregation of all songs attributed to a single artist.
 */
data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
) {
    val songCount: Int get() = songs.size
    val primaryArtworkUri: String? get() = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri
}

/**
 * Aggregation of all songs belonging to the same album (keyed by album + artist so
 * that two albums with the same name by different artists stay separate).
 */
data class AlbumGroup(
    val name: String,
    val artist: String,
    val songs: List<Song>,
) {
    val songCount: Int get() = songs.size
    val primaryArtworkUri: String? get() = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri
}

data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.Playlists,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    observeSongsUseCase: ObserveSongsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(LibraryTab.Playlists)

    val uiState: StateFlow<LibraryUiState> = combine(
        observePlaylistsUseCase(),
        observeSongsUseCase(),
        selectedTab,
    ) { playlists, songs, tab ->
        LibraryUiState(
            playlists = playlists,
            songs = songs,
            artists = groupByArtist(songs),
            albums = groupByAlbum(songs),
            selectedTab = tab,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(tab: LibraryTab) {
        selectedTab.value = tab
    }

    fun createPlaylist() {
        viewModelScope.launch {
            createPlaylistUseCase("My Playlist")
        }
    }

    fun playGroup(songs: List<Song>) {
        val first = songs.firstOrNull() ?: return
        viewModelScope.launch { playbackController.play(first, songs) }
    }

    private fun groupByArtist(songs: List<Song>): List<ArtistGroup> =
        songs.groupBy { it.artist.ifBlank { UNKNOWN_ARTIST } }
            .map { (name, grouped) ->
                ArtistGroup(
                    name = name,
                    songs = grouped.sortedWith(
                        compareBy({ it.album.lowercase() }, { it.title.lowercase() }),
                    ),
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun groupByAlbum(songs: List<Song>): List<AlbumGroup> =
        songs.groupBy { song ->
            (song.album.ifBlank { UNKNOWN_ALBUM }) to (song.artist.ifBlank { UNKNOWN_ARTIST })
        }
            .map { (key, grouped) ->
                AlbumGroup(
                    name = key.first,
                    artist = key.second,
                    songs = grouped.sortedBy { it.title.lowercase() },
                )
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.artist.lowercase() }))

    private companion object {
        private const val UNKNOWN_ARTIST = "Unknown Artist"
        private const val UNKNOWN_ALBUM = "Unknown Album"
    }
}
