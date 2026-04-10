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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.example.musicapp.ui.player.PlayerUiState
import com.example.musicapp.ui.player.PlayerViewModel
import com.example.musicapp.ui.search.SearchViewModel
import com.example.musicapp.ui.settings.SettingsEvent
import com.example.musicapp.ui.settings.SettingsViewModel
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

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

@Composable
fun MusicApp() {
    val navController = rememberNavController()
    val destinations = TopLevelDestination.entries
    val rootPlayerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by rootPlayerViewModel.uiState.collectAsStateWithLifecycle()

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val route = backStackEntry?.destination?.route
                    Column {
                        if (playerState.hasSong && route != TopLevelDestination.PLAYER.route) {
                            MiniPlayer(
                                state = playerState,
                                onTogglePlayPause = rootPlayerViewModel::togglePlayPause,
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
                        PlayerScreen(
                            state = playerState,
                            onTogglePlayPause = rootPlayerViewModel::togglePlayPause,
                            onNext = rootPlayerViewModel::skipNext,
                            onPrevious = rootPlayerViewModel::skipPrevious,
                            onSeek = rootPlayerViewModel::seekTo,
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

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

@Composable
private fun HomeScreen(
    state: com.example.musicapp.ui.home.HomeUiState,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val allSongs = state.sections.flatMap { it.songs }.distinctBy { it.id }
    val quickPicks = allSongs.take(6)
    val jumpBackIn = allSongs.drop(6).ifEmpty { allSongs }.take(8)
    val recentlyPlayed = allSongs.filter { it.isFavorite }.ifEmpty { allSongs }.take(5)
    val chips = listOf("All", "Music", "Podcasts")

    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient gradient behind the greeting card — gives the home screen the
        // familiar Spotify "mood color" feel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F4D3A),
                            SpotifyBackground.copy(alpha = 0.92f),
                            SpotifyBackground,
                        ),
                    ),
                ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                HomeHeader(chips = chips)
            }
            item {
                QuickPickGrid(
                    songs = quickPicks,
                    onPlaySong = { song -> onPlaySong(song, allSongs.ifEmpty { quickPicks }) },
                )
            }
            if (jumpBackIn.isNotEmpty()) {
                item {
                    FeatureSection(
                        title = "Jump back in",
                        subtitle = "Made for your recent rotations",
                        songs = jumpBackIn,
                        onPlaySong = { song -> onPlaySong(song, jumpBackIn) },
                    )
                }
            }
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    RecentSection(
                        title = "Your favorites",
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
}

@Composable
private fun HomeHeader(chips: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SpotifyGreen, Color(0xFF1F4D3A)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", color = SpotifyWhite, fontWeight = FontWeight.Black)
                }
                Text(
                    text = greetingForNow(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = SpotifyWhite,
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create",
                tint = SpotifyWhite,
                modifier = Modifier.size(24.dp),
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            chips.forEachIndexed { index, chip ->
                FilterChip(
                    selected = index == 0,
                    onClick = {},
                    label = { Text(chip, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(999.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        selectedLabelColor = SpotifyBackground,
                        containerColor = SpotifyCard,
                        labelColor = SpotifyWhite,
                    ),
                    border = null,
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
    if (songs.isEmpty()) return
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
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.09f)),
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
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                color = SpotifyWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
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
    if (songs.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = SpotifyWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
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
    Column(
        modifier = Modifier
            .width(168.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ArtworkThumb(
            song = song,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = song.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = song.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SpotifyTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ArtworkThumb(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val boxModifier = modifier.clip(RoundedCornerShape(6.dp))
    if (song.albumArtUri != null) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = song.title,
            modifier = boxModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        val gradient = gradientForSong(song)
        Box(
            modifier = boxModifier.background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song.title.take(1).ifBlank { song.artist.take(1) }.uppercase(),
                color = SpotifyWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun gradientForSong(song: Song): Brush {
    // Deterministic palette so the same song always picks the same pair of colors.
    val seed = (song.id.hashCode().absoluteValue) % ARTWORK_PALETTES.size
    val pair = ARTWORK_PALETTES[seed]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}

private val ARTWORK_PALETTES: List<Pair<Color, Color>> = listOf(
    Color(0xFF1DB954) to Color(0xFF0F3B22),
    Color(0xFF8E44AD) to Color(0xFF2C1338),
    Color(0xFFE91E63) to Color(0xFF3B0A1E),
    Color(0xFFF39C12) to Color(0xFF3B2208),
    Color(0xFF3498DB) to Color(0xFF0B2540),
    Color(0xFFE74C3C) to Color(0xFF3A0F09),
    Color(0xFF16A085) to Color(0xFF082D24),
    Color(0xFF9B59B6) to Color(0xFF2A1038),
)

// ---------------------------------------------------------------------------
// Bottom bar & mini player
// ---------------------------------------------------------------------------

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
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpotifyMuted),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.currentSongTitle,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = state.currentSongArtist.ifBlank { "Tap to open" },
                    color = SpotifyTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
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

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
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
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = SpotifyWhite,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Artists, songs, or albums", color = Color.DarkGray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.DarkGray,
                )
            },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = SpotifyBackground,
                focusedTextColor = SpotifyBackground,
                unfocusedTextColor = SpotifyBackground,
            ),
        )
        if (state.query.isBlank()) {
            Text(
                text = "Browse all",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = SpotifyWhite,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                items(BROWSE_GENRES) { genre ->
                    GenreTile(genre)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
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
}

private data class Genre(val label: String, val color: Color)

private val BROWSE_GENRES = listOf(
    Genre("Pop", Color(0xFFE91E63)),
    Genre("Hip-Hop", Color(0xFFFF7043)),
    Genre("Rock", Color(0xFF5C6BC0)),
    Genre("Indie", Color(0xFF26A69A)),
    Genre("Electronic", Color(0xFF7E57C2)),
    Genre("Jazz", Color(0xFF8D6E63)),
    Genre("Classical", Color(0xFF42A5F5)),
    Genre("Chill", Color(0xFF66BB6A)),
    Genre("Workout", Color(0xFFEF5350)),
    Genre("Focus", Color(0xFF26C6DA)),
)

@Composable
private fun GenreTile(genre: Genre) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(8.dp))
            .background(genre.color)
            .padding(14.dp),
    ) {
        Text(
            text = genre.label,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

// ---------------------------------------------------------------------------
// Library
// ---------------------------------------------------------------------------

@Composable
private fun LibraryScreen(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onCreatePlaylist: () -> Unit,
) {
    val tabs = listOf("Playlists", "Songs", "Artists", "Albums")
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SpotifyGreen, Color(0xFF1F4D3A)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("S", color = SpotifyWhite, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SpotifyWhite,
                    )
                }
                IconButton(onClick = onCreatePlaylist) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create playlist",
                        tint = SpotifyWhite,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { index, label ->
                    FilterChip(
                        selected = index == 0,
                        onClick = {},
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen.copy(alpha = 0.18f),
                            selectedLabelColor = SpotifyGreen,
                            containerColor = SpotifyCard,
                            labelColor = SpotifyWhite,
                        ),
                        border = null,
                    )
                }
            }
        }
        if (state.playlists.isNotEmpty()) {
            item {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyWhite,
                )
            }
            items(state.playlists) { playlist ->
                PlaylistRow(playlist)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "All songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyWhite,
                )
                Text(
                    text = "${state.songs.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextMuted,
                )
            }
        }
        items(state.songs) { song ->
            SongRow(song = song, onClick = {}, onToggleFavorite = {})
        }
    }
}

// ---------------------------------------------------------------------------
// Player
// ---------------------------------------------------------------------------

@Composable
private fun PlayerScreen(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val ambient = state.currentSong?.let { gradientForSong(it) }
        ?: Brush.verticalGradient(listOf(SpotifyCard, SpotifyBackground))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambient),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SpotifyBackground),
                        startY = 200f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            val song = state.currentSong
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (song != null) {
                    ArtworkThumb(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .shadow(24.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpotifyCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No song loaded",
                            color = SpotifyTextMuted,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentSongTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SpotifyWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.currentSongArtist.ifBlank { "Unknown artist" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpotifyTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Save",
                    tint = SpotifyWhite,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                var scrubbing by remember { mutableStateOf(false) }
                var scrubPosition by remember { mutableStateOf(0f) }
                val sliderValue = if (scrubbing) scrubPosition else state.progress
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        scrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        if (state.durationMs > 0) {
                            onSeek((scrubPosition * state.durationMs).toLong())
                        }
                        scrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyWhite,
                        activeTrackColor = SpotifyWhite,
                        inactiveTrackColor = SpotifyWhite.copy(alpha = 0.25f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(
                            if (scrubbing) (scrubPosition * state.durationMs).toLong() else state.positionMs,
                        ),
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatDuration(state.durationMs),
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(24.dp),
                )
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SpotifyWhite)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = SpotifyBackground,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(modifier = Modifier.size(24.dp))
            }
            if (state.queueSize > 0) {
                Text(
                    text = "${state.queueSize} songs in queue",
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings / Drive sync
// ---------------------------------------------------------------------------

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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SpotifyWhite,
            )
        }
        item { DriveProfileCard(state) }
        item { DriveAccessCard(state, onConnectDrive, onDisconnectDrive, onChooseFolder) }
        item { DriveSyncCard(state, onRefresh) }
        item { ImportedRootsCard(state) }
    }

    if (state.isFolderPickerVisible) {
        FolderPickerDialog(
            state = state,
            onDismiss = onDismissFolderPicker,
            onNavigateUp = onNavigateUpFolder,
            onOpenFolder = onOpenFolder,
            onSelectCurrentFolder = onSelectCurrentFolder,
        )
    }
}

@Composable
private fun DriveProfileCard(state: com.example.musicapp.ui.settings.SettingsUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isDriveConnected) SpotifyGreen.copy(alpha = 0.22f) else SpotifyMuted.copy(alpha = 0.4f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (state.isDriveConnected) SpotifyGreen else SpotifyTextMuted,
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
                    text = state.connectedDriveEmail.ifBlank { "Connect your Google account to import Drive audio." },
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (state.isDriveConnected) SpotifyGreen.copy(alpha = 0.22f) else SpotifyMuted.copy(alpha = 0.35f),
                    )
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
}

@Composable
private fun DriveAccessCard(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onChooseFolder: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Drive access",
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.isDriveConnected) {
                        "Your Google account is linked and ready for Drive sync."
                    } else {
                        "Sign in with Google and grant Drive access to import your music."
                    },
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (state.isDriveConnected) onDisconnectDrive else onConnectDrive,
                    enabled = !state.isWorking,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isDriveConnected) Color.Transparent else SpotifyGreen,
                        contentColor = if (state.isDriveConnected) SpotifyWhite else SpotifyBackground,
                    ),
                ) {
                    Text(
                        text = when {
                            state.isWorking -> "Working..."
                            state.isDriveConnected -> "Disconnect"
                            else -> "Connect Google Drive"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onChooseFolder,
                    enabled = state.isDriveConnected && !state.isWorking && !state.isFolderLoading,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyMuted.copy(alpha = 0.35f),
                        contentColor = SpotifyWhite,
                    ),
                ) {
                    Text(
                        text = if (state.isFolderLoading) "Loading..." else "Browse",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveSyncCard(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onRefresh: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Selected root",
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
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
                    enabled = !state.isWorking && !state.isFolderLoading && !state.isDriveSyncing,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBackground,
                    ),
                ) {
                    Text(
                        text = if (state.isDriveSyncing) "Syncing..." else "Sync",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Live sync progress — updates in real time as each song streams in from
            // GoogleDriveMusicDataSource via DefaultMusicRepository.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.isDriveSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SpotifyGreen,
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = state.driveSyncStatusText,
                    color = if (state.isDriveSyncing) SpotifyGreen else SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.isDriveSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SpotifyGreen,
                    trackColor = SpotifyGreen.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ImportedRootsCard(state: com.example.musicapp.ui.settings.SettingsUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Imported roots",
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Drive roots tracked by the app",
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = state.selectedFolderCount.toString(),
                color = SpotifyWhite,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FolderPickerDialog(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onDismiss: () -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFolder: (DriveFolder) -> Unit,
    onSelectCurrentFolder: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose Drive Folder", color = SpotifyWhite, fontWeight = FontWeight.Black)
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
                            modifier = Modifier.clickable(
                                enabled = !state.isFolderLoading,
                                onClick = onNavigateUp,
                            ),
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
                onClick = onDismiss,
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

// ---------------------------------------------------------------------------
// Shared rows
// ---------------------------------------------------------------------------

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
            .padding(vertical = 6.dp),
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
                modifier = Modifier.size(52.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SpotifyWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = formatDuration(song.durationMs),
                color = SpotifyTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Unlike" else "Like",
                    tint = if (song.isFavorite) SpotifyGreen else SpotifyTextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(listOf(SpotifyGreen, Color(0xFF1F4D3A)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = SpotifyWhite,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyWhite,
            )
            Text(
                "Playlist • ${playlist.songIds.size} songs",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextMuted,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "--:--"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun greetingForNow(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
