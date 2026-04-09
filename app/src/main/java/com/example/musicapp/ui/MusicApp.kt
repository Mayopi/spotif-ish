package com.example.musicapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapp.domain.model.HomeSection
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.home.HomeViewModel
import com.example.musicapp.ui.library.LibraryViewModel
import com.example.musicapp.ui.player.PlayerViewModel
import com.example.musicapp.ui.search.SearchViewModel
import com.example.musicapp.ui.settings.SettingsViewModel

private enum class TopLevelDestination(val route: String, val label: String) {
    HOME("home", "Home"),
    SEARCH("search", "Search"),
    LIBRARY("library", "Library"),
    PLAYER("player", "Player"),
    SETTINGS("settings", "Settings"),
}

@Composable
fun MusicApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val destinations = TopLevelDestination.entries

    AppTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                bottomBar = {
                    BottomAppBar {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            val backStackEntry by navController.currentBackStackEntryAsState()
                            val route = backStackEntry?.destination?.route
                            destinations.forEach { destination ->
                                Text(
                                    text = destination.label,
                                    modifier = Modifier
                                        .clickable {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(16.dp),
                                    color = if (route == destination.route) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                },
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = TopLevelDestination.HOME.route,
                    modifier = Modifier.padding(paddingValues),
                ) {
                    composable(TopLevelDestination.HOME.route) {
                        val viewModel = hiltViewModel<HomeViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = state,
                            onPlaySong = viewModel::playSong,
                            onToggleFavorite = viewModel::toggleFavorite,
                        )
                    }
                    composable(TopLevelDestination.SEARCH.route) {
                        val viewModel = hiltViewModel<SearchViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        SearchScreen(
                            state = state,
                            onQueryChange = viewModel::updateQuery,
                            onPlaySong = viewModel::playSong,
                            onToggleFavorite = viewModel::toggleFavorite,
                        )
                    }
                    composable(TopLevelDestination.LIBRARY.route) {
                        val viewModel = hiltViewModel<LibraryViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        LibraryScreen(
                            state = state,
                            onCreatePlaylist = viewModel::createPlaylist,
                        )
                    }
                    composable(TopLevelDestination.PLAYER.route) {
                        val viewModel = hiltViewModel<PlayerViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        PlayerScreen(
                            state = state,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::skipNext,
                            onPrevious = viewModel::skipPrevious,
                        )
                    }
                    composable(TopLevelDestination.SETTINGS.route) {
                        val viewModel = hiltViewModel<SettingsViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        SettingsScreen(
                            state = state,
                            onToggleTheme = onToggleTheme,
                            onRefresh = viewModel::refreshLibraries,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: com.example.musicapp.ui.home.HomeUiState,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.sections) { section ->
            HomeSectionBlock(
                section = section,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun HomeSectionBlock(
    section: HomeSection,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = section.title, style = MaterialTheme.typography.titleLarge)
        section.songs.forEach { song ->
            SongRow(
                song = song,
                onClick = { onPlaySong(song, section.songs) },
                onToggleFavorite = { onToggleFavorite(song.id) },
            )
        }
    }
}

@Composable
private fun SearchScreen(
    state: com.example.musicapp.ui.search.SearchUiState,
    onQueryChange: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search songs, artist, album") },
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.results) { song ->
                SongRow(
                    song = song,
                    onClick = { onPlaySong(song, state.results) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onCreatePlaylist: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Button(onClick = onCreatePlaylist) {
                Text("Create Playlist")
            }
        }
        item {
            Text("Playlists", style = MaterialTheme.typography.titleLarge)
        }
        items(state.playlists) { playlist ->
            PlaylistRow(playlist)
        }
        item {
            Text("All Songs", style = MaterialTheme.typography.titleLarge)
        }
        items(state.songs) { song ->
            SongRow(song = song, onClick = {}, onToggleFavorite = {})
        }
    }
}

@Composable
private fun PlayerScreen(
    state: com.example.musicapp.ui.player.PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = state.currentSongTitle, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.currentSongArtist, style = MaterialTheme.typography.bodyLarge)
        Text(text = if (state.isPlaying) "Playing" else "Paused")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPrevious) { Text("Previous") }
            Button(onClick = onTogglePlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
            Button(onClick = onNext) { Text("Next") }
        }
        Text("Queue size: ${state.queueSize}")
    }
}

@Composable
private fun SettingsScreen(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Dark theme")
            Switch(checked = state.isDarkTheme, onCheckedChange = { onToggleTheme() })
        }
        Button(onClick = onRefresh) {
            Text("Refresh libraries")
        }
        Text("Connected Drive: ${state.connectedDriveFolderName}")
        Text("Selected folders: ${state.selectedFolderCount}")
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium)
            Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = if (song.isFavorite) "Unlike" else "Like",
            modifier = Modifier.clickable(onClick = onToggleFavorite),
        )
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(playlist.name, style = MaterialTheme.typography.titleMedium)
        Text("${playlist.songIds.size} songs", style = MaterialTheme.typography.bodyMedium)
    }
}
