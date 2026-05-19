package com.example.spotifish.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spotifish.ui.home.HomeViewModel
import com.example.spotifish.ui.library.LibraryEvent
import com.example.spotifish.ui.library.LibraryViewModel
import com.example.spotifish.ui.player.PlayerUiState
import com.example.spotifish.ui.player.PlayerViewModel
import com.example.spotifish.ui.search.SearchViewModel
import com.example.spotifish.ui.settings.SettingsEvent
import com.example.spotifish.ui.settings.SettingsViewModel

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", "Home", Icons.Default.Home, Icons.Outlined.Home),
    SEARCH("search", "Search", Icons.Default.Search, Icons.Outlined.Search),
    LIBRARY("library", "Your Library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic),
    PLAYER("player", "Now Playing", Icons.Default.PlayCircleFilled, Icons.Outlined.PlayCircle),
    SETTINGS("settings", "Profile", Icons.Default.Person, Icons.Outlined.Person),
}

private object HomeDestination {
    const val ARTIST_ARG = "artistName"
    const val ALBUM_ARTIST_ARG = "albumArtist"
    const val ALBUM_ARG = "albumName"
    const val ARTIST_DETAIL_ROUTE = "home/artist/{$ARTIST_ARG}"
    const val ALBUM_DETAIL_ROUTE = "home/album/{$ALBUM_ARTIST_ARG}/{$ALBUM_ARG}"

    fun artistDetailRoute(artistName: String): String =
        "home/artist/${Uri.encode(artistName)}"

    fun albumDetailRoute(albumArtist: String, albumName: String): String =
        "home/album/${Uri.encode(albumArtist)}/${Uri.encode(albumName)}"
}

@Composable
fun SpotifishApp() {
    val navController = rememberNavController()
    val destinations = remember { TopLevelDestination.entries }
    val rootPlayerViewModel: PlayerViewModel = hiltViewModel()
    val rootHomeViewModel: HomeViewModel = hiltViewModel()

    AppTheme {
        Surface(color = Color.Transparent) {
            Scaffold(
                containerColor = SpotifyBackground,
                bottomBar = {
                    SpotifishBottomBar(
                        navController = navController,
                        destinations = destinations,
                        playerViewModel = rootPlayerViewModel,
                    )
                },
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = TopLevelDestination.HOME.route,
                    modifier = Modifier.padding(paddingValues),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable(TopLevelDestination.HOME.route) {
                        val state by rootHomeViewModel.uiState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        HomeScreen(
                            state = state,
                            onOpenArtist = { artist ->
                                navController.navigate(HomeDestination.artistDetailRoute(artist.name))
                            },
                            onOpenAlbum = { album ->
                                navController.navigate(
                                    HomeDestination.albumDetailRoute(
                                        albumArtist = album.artist,
                                        albumName = album.name,
                                    ),
                                )
                            },
                            onPlaySong = { song, queue ->
                                Toast.makeText(
                                    context,
                                    "Loading song, please wait...",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                rootHomeViewModel.playSong(song, queue)
                            },
                            onToggleFavorite = rootHomeViewModel::toggleFavorite,
                        )
                    }
                    composable(HomeDestination.ARTIST_DETAIL_ROUTE) { backStackEntry ->
                        val state by rootHomeViewModel.uiState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val artistName = remember(backStackEntry) {
                            Uri.decode(
                                backStackEntry.arguments
                                    ?.getString(HomeDestination.ARTIST_ARG)
                                    .orEmpty(),
                            )
                        }
                        val artist = remember(state.sections, artistName) {
                            buildHomeGrouping(state.sections).artists.firstOrNull { it.name == artistName }
                        }
                        ArtistDetailScreen(
                            artist = artist,
                            onBack = { navController.popBackStack() },
                            onPlaySong = { song, queue ->
                                Toast.makeText(
                                    context,
                                    "Loading song, please wait...",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                rootHomeViewModel.playSong(song, queue)
                            },
                            onToggleFavorite = rootHomeViewModel::toggleFavorite,
                        )
                    }
                    composable(HomeDestination.ALBUM_DETAIL_ROUTE) { backStackEntry ->
                        val state by rootHomeViewModel.uiState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val albumArtist = remember(backStackEntry) {
                            Uri.decode(
                                backStackEntry.arguments
                                    ?.getString(HomeDestination.ALBUM_ARTIST_ARG)
                                    .orEmpty(),
                            )
                        }
                        val albumName = remember(backStackEntry) {
                            Uri.decode(
                                backStackEntry.arguments
                                    ?.getString(HomeDestination.ALBUM_ARG)
                                    .orEmpty(),
                            )
                        }
                        val album = remember(state.sections, albumArtist, albumName) {
                            buildHomeGrouping(state.sections).albums.firstOrNull {
                                it.artist == albumArtist && it.name == albumName
                            }
                        }
                        AlbumDetailScreen(
                            album = album,
                            onBack = { navController.popBackStack() },
                            onPlaySong = { song, queue ->
                                Toast.makeText(
                                    context,
                                    "Loading song, please wait...",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                rootHomeViewModel.playSong(song, queue)
                            },
                            onToggleFavorite = rootHomeViewModel::toggleFavorite,
                        )
                    }
                    composable(TopLevelDestination.SEARCH.route) {
                        val viewModel = hiltViewModel<SearchViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        SearchScreen(
                            state = state,
                            onQueryChange = viewModel::updateQuery,
                            onPlaySong = { song, queue ->
                                Toast.makeText(
                                    context,
                                    "Loading song, please wait...",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                viewModel.playSong(song, queue)
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                        )
                    }
                    composable(TopLevelDestination.LIBRARY.route) {
                        val viewModel = hiltViewModel<LibraryViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        LaunchedEffect(viewModel.events, context) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    is LibraryEvent.Message -> {
                                        Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        LibraryScreen(
                            state = state,
                            onCreatePlaylist = viewModel::createPlaylist,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onAddSongToPlaylist = viewModel::addSongToPlaylist,
                            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onSelectTab = viewModel::selectTab,
                            onPlayGroup = viewModel::playGroup,
                        )
                    }
                    composable(TopLevelDestination.PLAYER.route) {
                        PlayerRoute(playerViewModel = rootPlayerViewModel)
                    }
                    composable(TopLevelDestination.SETTINGS.route) {
                        val viewModel = hiltViewModel<SettingsViewModel>()
                        val state by viewModel.screenState.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = context.findActivity()
                        val driveConsentLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                        ) { result ->
                            activity?.let { viewModel.completeDriveConnect(it, result.data) }
                        }
                        val localFolderLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocumentTree(),
                        ) { uri ->
                            viewModel.addLocalFolder(uri)
                        }
                        LaunchedEffect(viewModel.events, context) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    is SettingsEvent.Message -> {
                                        Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                                    }

                                    SettingsEvent.SignOut -> Unit
                                    is SettingsEvent.LaunchDriveConsent -> {
                                        driveConsentLauncher.launch(
                                            IntentSenderRequest.Builder(
                                                event.pendingIntent.intentSender,
                                            ).build(),
                                        )
                                    }
                                }
                            }
                        }
                        SettingsScreen(
                            state = state,
                            onConnectDrive = { activity?.let(viewModel::connectDrive) },
                            onDisconnectDrive = viewModel::disconnectDrive,
                            onChooseFolder = viewModel::openFolderPicker,
                            onChooseLocalFolder = { localFolderLauncher.launch(null) },
                            onRemoveLocalFolder = viewModel::removeLocalFolder,
                            onClearLocalFolders = viewModel::clearLocalFolders,
                            onNavigateUpFolder = viewModel::navigateUpDriveFolders,
                            onDismissFolderPicker = viewModel::dismissFolderPicker,
                            onOpenFolder = viewModel::navigateIntoDriveFolder,
                            onSelectCurrentFolder = viewModel::selectCurrentDriveFolder,
                            onRefresh = viewModel::refreshLibraries,
                            onPauseSync = viewModel::pauseSync,
                            onResumeSync = viewModel::resumeSync,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifishBottomBar(
    navController: NavHostController,
    destinations: List<TopLevelDestination>,
    playerViewModel: PlayerViewModel,
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Column {
        if (playerState.hasSong && route != TopLevelDestination.PLAYER.route) {
            MiniPlayer(
                state = playerState,
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onClick = {
                    navController.navigate(TopLevelDestination.PLAYER.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        SpotifishBottomBar(
            currentRoute = route,
            destinations = destinations,
            onNavigate = { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}

@Composable
private fun PlayerRoute(
    playerViewModel: PlayerViewModel,
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    PlayerScreen(
        state = playerState,
        onToggleFavorite = playerViewModel::toggleCurrentSongFavorite,
        onToggleShuffle = playerViewModel::toggleShuffle,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onNext = playerViewModel::skipNext,
        onPrevious = playerViewModel::skipPrevious,
        onSeek = playerViewModel::seekTo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotifishBottomBar(
    currentRoute: String?,
    destinations: List<TopLevelDestination>,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Black,
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            val selected = when (destination) {
                TopLevelDestination.HOME ->
                    currentRoute == destination.route || currentRoute?.startsWith("home/") == true

                else -> currentRoute == destination.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SpotifyWhite,
                    unselectedIconColor = SpotifyTextMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val song = state.currentSong
            if (song != null) {
                ArtworkThumb(
                    song = song,
                    modifier = Modifier.size(42.dp),
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpotifyMuted),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = state.currentSongTitle,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                androidx.compose.material3.Text(
                    text = state.currentSongArtist.ifBlank { "Tap to open" },
                    color = SpotifyTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = SpotifyWhite,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = SpotifyWhite,
            trackColor = SpotifyWhite.copy(alpha = 0.22f),
        )
    }
}
