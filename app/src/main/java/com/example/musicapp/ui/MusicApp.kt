package com.example.musicapp.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.ui.home.HomeViewModel
import com.example.musicapp.ui.library.LibraryViewModel
import com.example.musicapp.ui.player.PlayerViewModel
import com.example.musicapp.ui.search.SearchViewModel
import com.example.musicapp.ui.settings.SettingsEvent
import com.example.musicapp.ui.settings.SettingsViewModel

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Default.Home),
    SEARCH("search", "Search", Icons.Default.Search),
    LIBRARY("library", "Your Library", Icons.Default.LibraryMusic),
    PLAYER("player", "Player", Icons.Default.PlayCircleFilled),
    SETTINGS("settings", "Profile", Icons.Default.Person),
}

@Composable
fun MusicApp(
) {
    val navController = rememberNavController()
    val destinations = TopLevelDestination.entries

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val route = backStackEntry?.destination?.route
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
                        val state by viewModel.screenState.collectAsStateWithLifecycle()
                        val context = LocalContext.current
                        val activity = context.findActivity()
                        val authorizationLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                        ) { result ->
                            if (activity != null) {
                                viewModel.completeDriveAuthorization(activity, result.data)
                            }
                        }
                        LaunchedEffect(viewModel.events, context, activity) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    is SettingsEvent.LaunchDriveAuthorization -> {
                                        authorizationLauncher.launch(
                                            IntentSenderRequest.Builder(event.pendingIntent.intentSender).build(),
                                        )
                                    }

                                    is SettingsEvent.Message -> {
                                        Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        SettingsScreen(
                            state = state,
                            onConnectDrive = { activity?.let(viewModel::connectDrive) },
                            onDisconnectDrive = { activity?.let(viewModel::disconnectDrive) },
                            onChooseFolder = viewModel::openFolderPicker,
                            onNavigateUpFolder = viewModel::navigateUpDriveFolders,
                            onDismissFolderPicker = viewModel::dismissFolderPicker,
                            onOpenFolder = viewModel::navigateIntoDriveFolder,
                            onSelectCurrentFolder = viewModel::selectCurrentDriveFolder,
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
    val allSongs = state.sections.flatMap { it.songs }.distinctBy { it.id }
    val quickPicks = allSongs.take(6)
    val featuredSongs = allSongs.drop(6).ifEmpty { allSongs }.take(8)
    val recentlyPlayed = allSongs.filter { it.isFavorite }.ifEmpty { allSongs }.take(5)
    val chips = listOf("All", "Music", "Podcasts")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            HomeHeader(chips = chips)
        }
        item {
            QuickPickGrid(
                songs = quickPicks,
                onPlaySong = { song -> onPlaySong(song, quickPicks.ifEmpty { allSongs }) },
            )
        }
        if (featuredSongs.isNotEmpty()) {
            item {
                FeatureSection(
                    title = "Picked for you",
                    subtitle = "Based on your favorite rotations",
                    songs = featuredSongs,
                    onPlaySong = { song -> onPlaySong(song, featuredSongs) },
                )
            }
        }
        if (recentlyPlayed.isNotEmpty()) {
            item {
                RecentSection(
                    title = "Recently played",
                    songs = recentlyPlayed,
                    onPlaySong = { song -> onPlaySong(song, recentlyPlayed) },
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
        items(state.sections) { section ->
            FeatureSection(
                title = section.title,
                subtitle = "Fresh picks from your library",
                songs = section.songs,
                onPlaySong = { song -> onPlaySong(song, section.songs) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotifishBottomBar(
    currentRoute: String?,
    destinations: List<TopLevelDestination>,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp)),
            containerColor = Color.Black.copy(alpha = 0.42f),
            tonalElevation = 0.dp,
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (selected) SpotifyWhite else SpotifyTextMuted,
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            color = if (selected) SpotifyWhite else SpotifyTextMuted,
                            maxLines = 1,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyWhite,
                        selectedTextColor = SpotifyWhite,
                        unselectedIconColor = SpotifyTextMuted,
                        unselectedTextColor = SpotifyTextMuted,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(chips: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SpotifyCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Good evening",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "Create",
                tint = SpotifyWhite,
                modifier = Modifier.size(24.dp),
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEachIndexed { index, chip ->
                FilterChip(
                    selected = index == 0,
                    onClick = {},
                    label = { Text(chip) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        selectedLabelColor = SpotifyBackground,
                        containerColor = SpotifyCard,
                        labelColor = SpotifyWhite,
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuickPickGrid(
    songs: List<Song>,
    onPlaySong: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        songs.chunked(2).forEach { rowSongs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowSongs.forEach { song ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPlaySong(song) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ArtworkThumb(
                                song = song,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(
                                text = song.title,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = SpotifyWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (rowSongs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    onPlaySong: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SpotifyWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyTextMuted,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(songs) { song ->
                FeaturedAlbumCard(song = song, onClick = { onPlaySong(song) })
            }
        }
    }
}

@Composable
private fun RecentSection(
    title: String,
    songs: List<Song>,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = SpotifyWhite,
        )
        songs.forEach { song ->
            SongRow(
                song = song,
                onClick = { onPlaySong(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
            )
        }
    }
}

@Composable
private fun FeaturedAlbumCard(
    song: Song,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(164.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2C),
                            SpotifyBackground,
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtworkThumb(
                song = song,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = song.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ArtworkThumb(
    song: Song,
    modifier: Modifier = Modifier,
) {
    if (song.albumArtUri != null) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = song.title,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(SpotifyGreen.copy(alpha = 0.8f), SpotifyMuted),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song.artist.take(1).uppercase(),
                color = SpotifyWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
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
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SpotifyWhite,
        )
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
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SpotifyWhite,
            )
        }
        item {
            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = SpotifyBackground,
                ),
            ) {
                Text("Create Playlist")
            }
        }
        item {
            Text("Playlists", style = MaterialTheme.typography.titleLarge, color = SpotifyWhite)
        }
        items(state.playlists) { playlist ->
            PlaylistRow(playlist)
        }
        item {
            Text("All Songs", style = MaterialTheme.typography.titleLarge, color = SpotifyWhite)
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
        Text("Now Playing", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(text = state.currentSongTitle, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.currentSongArtist, style = MaterialTheme.typography.bodyLarge, color = SpotifyTextMuted)
        Text(text = if (state.isPlaying) "Playing" else "Paused", color = SpotifyGreen)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPrevious, colors = ButtonDefaults.buttonColors(containerColor = SpotifyCard)) { Text("Previous") }
            Button(onClick = onTogglePlayPause, colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = SpotifyBackground)) { Text(if (state.isPlaying) "Pause" else "Play") }
            Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = SpotifyCard)) { Text("Next") }
        }
        Text("Queue size: ${state.queueSize}", color = SpotifyTextMuted)
    }
}

@Composable
private fun SettingsScreen(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onChooseFolder: () -> Unit,
    onNavigateUpFolder: () -> Unit,
    onDismissFolderPicker: () -> Unit,
    onOpenFolder: (DriveFolder) -> Unit,
    onSelectCurrentFolder: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = SpotifyWhite)
        Card(
            colors = CardDefaults.cardColors(containerColor = SpotifyCard),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (state.isDriveConnected) SpotifyGreen.copy(alpha = 0.18f) else SpotifyMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.isDriveConnected) state.connectedDriveName.take(1).uppercase() else "G",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (state.isDriveConnected) state.connectedDriveName else "Google Drive",
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.connectedDriveEmail.isNotBlank()) state.connectedDriveEmail else "Connect your Google account to import Drive audio.",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (state.isDriveConnected) SpotifyGreen.copy(alpha = 0.18f) else SpotifyMuted.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (state.isDriveConnected) "Connected" else "Offline",
                        color = if (state.isDriveConnected) SpotifyGreen else SpotifyTextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = SpotifyCard),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Drive access", color = SpotifyWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (state.isDriveConnected) {
                            "Your Google account is linked and ready for Drive sync."
                        } else {
                            "Sign in with Google and grant Drive access to connect your account."
                        },
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = if (state.isDriveConnected) onDisconnectDrive else onConnectDrive,
                        enabled = !state.isWorking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isDriveConnected) SpotifyCard else SpotifyGreen,
                            contentColor = if (state.isDriveConnected) SpotifyWhite else SpotifyBackground,
                        ),
                    ) {
                        Text(if (state.isWorking) "Working..." else if (state.isDriveConnected) "Disconnect" else "Connect Google Drive")
                    }
                    Button(
                        onClick = onChooseFolder,
                        enabled = state.isDriveConnected && !state.isWorking && !state.isFolderLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyMuted.copy(alpha = 0.35f),
                            contentColor = SpotifyWhite,
                        ),
                    ) {
                        Text(if (state.isFolderLoading) "Loading..." else "Browse")
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = SpotifyCard),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Selected root", color = SpotifyWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        state.connectedDriveFolderName,
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !state.isWorking && !state.isFolderLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBackground,
                    ),
                ) {
                    Text("Sync")
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = SpotifyCard),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Imported roots", color = SpotifyWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Drive roots tracked by the app", color = SpotifyTextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = state.selectedFolderCount.toString(),
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }

    if (state.isFolderPickerVisible) {
        AlertDialog(
            onDismissRequest = onDismissFolderPicker,
            containerColor = SpotifyCard,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Drive Folder", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            state.currentDriveFolderPath,
                            modifier = Modifier.weight(1f),
                            color = SpotifyTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.canNavigateUpFolders) {
                            Text(
                                "Back",
                                modifier = Modifier.clickable(enabled = !state.isFolderLoading, onClick = onNavigateUpFolder),
                                color = SpotifyWhite,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            },
            text = {
                if (state.isFolderLoading && state.availableDriveFolders.isEmpty()) {
                    Text(
                        "Loading folders...",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (state.availableDriveFolders.isEmpty()) {
                    Text(
                        "This folder has no subfolders. You can use the current folder.",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.availableDriveFolders) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isFolderLoading) { onOpenFolder(folder) },
                                colors = CardDefaults.cardColors(containerColor = SpotifyBackground),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(folder.name, color = SpotifyWhite, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            folder.path,
                                            color = SpotifyTextMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        "Open",
                                        color = SpotifyWhite,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSelectCurrentFolder,
                    enabled = !state.isFolderLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBackground,
                    ),
                ) {
                    Text("Use This Folder")
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismissFolderPicker,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyMuted.copy(alpha = 0.35f),
                        contentColor = SpotifyWhite,
                    ),
                ) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SpotifyBackground),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArtworkThumb(
                    song = song,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpotifyTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = if (song.isFavorite) "Liked" else "Like",
                color = if (song.isFavorite) SpotifyGreen else SpotifyTextMuted,
                modifier = Modifier.clickable(onClick = onToggleFavorite),
            )
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = SpotifyWhite)
            Text("${playlist.songIds.size} songs", style = MaterialTheme.typography.bodyMedium, color = SpotifyTextMuted)
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
