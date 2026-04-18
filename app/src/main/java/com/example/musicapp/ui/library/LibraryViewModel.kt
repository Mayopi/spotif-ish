package com.example.musicapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.usecase.AddSongToPlaylistUseCase
import com.example.musicapp.domain.usecase.CreatePlaylistUseCase
import com.example.musicapp.domain.usecase.DeletePlaylistUseCase
import com.example.musicapp.domain.usecase.ObservePlaylistsUseCase
import com.example.musicapp.domain.usecase.ObserveSongsUseCase
import com.example.musicapp.domain.usecase.RecordPlaybackStartedUseCase
import com.example.musicapp.domain.usecase.RemoveSongFromPlaylistUseCase
import com.example.musicapp.domain.usecase.RenamePlaylistUseCase
import com.example.musicapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed interface LibraryEvent {
    data class Message(val text: String) : LibraryEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    observeSongsUseCase: ObserveSongsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val addSongToPlaylistUseCase: AddSongToPlaylistUseCase,
    private val removeSongFromPlaylistUseCase: RemoveSongFromPlaylistUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordPlaybackStartedUseCase: RecordPlaybackStartedUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(LibraryTab.Playlists)
    private val _events = MutableSharedFlow<LibraryEvent>()
    val events = _events.asSharedFlow()

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

    fun createPlaylist(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        launchPlaylistMutation(defaultErrorMessage = "Could not create playlist.") {
            createPlaylistUseCase(normalizedName)
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return
        launchPlaylistMutation(defaultErrorMessage = "Could not rename playlist.") {
            renamePlaylistUseCase(playlistId, normalizedName)
        }
    }

    fun deletePlaylist(playlistId: String) {
        launchPlaylistMutation(defaultErrorMessage = "Could not delete playlist.") {
            deletePlaylistUseCase(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        launchPlaylistMutation(defaultErrorMessage = "Could not add song to playlist.") {
            addSongToPlaylistUseCase(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        launchPlaylistMutation(defaultErrorMessage = "Could not remove song from playlist.") {
            removeSongFromPlaylistUseCase(playlistId, songId)
        }
    }

    fun playGroup(songs: List<Song>) {
        val first = songs.firstOrNull() ?: return
        viewModelScope.launch {
            runCatching { recordPlaybackStartedUseCase(first.id) }
            playbackController.play(first, songs)
        }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(songId) }
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

    private fun launchPlaylistMutation(
        defaultErrorMessage: String,
        mutation: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                mutation()
            }.onFailure { throwable ->
                _events.emit(LibraryEvent.Message(throwable.message ?: defaultErrorMessage))
            }
        }
    }

    private companion object {
        private const val UNKNOWN_ARTIST = "Unknown Artist"
        private const val UNKNOWN_ALBUM = "Unknown Album"
    }
}
