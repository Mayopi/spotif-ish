package com.example.musicapp.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.ui.home.HomeViewModel
import com.example.musicapp.ui.library.AlbumGroup
import com.example.musicapp.ui.library.ArtistGroup
import com.example.musicapp.ui.library.LibraryEvent
import com.example.musicapp.ui.library.LibraryTab
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

private val HOME_FILTER_CHIPS = listOf("All", "Music", "Podcasts")

@Composable
fun MusicApp() {
    val navController = rememberNavController()
    val destinations = remember { TopLevelDestination.entries }
    val rootPlayerViewModel: PlayerViewModel = hiltViewModel()

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    MusicAppBottomBar(
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
                    // Tab switching should feel instant. Compose Navigation's
                    // default fade-in/fade-out adds ~200ms of perceived latency
                    // on every bottom-bar tap, which is most of the cost of
                    // moving between Home/Search/Library/Player/Settings.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable(TopLevelDestination.HOME.route) {
                        val viewModel = hiltViewModel<HomeViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        val context = LocalContext.current
                        HomeScreen(
                            state = state,
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
                    composable(TopLevelDestination.SEARCH.route) {
                        val viewModel = hiltViewModel<SearchViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        val context = LocalContext.current
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
                        val context = LocalContext.current
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
                        val context = LocalContext.current
                        val activity = context.findActivity()
                        // Receives the result of the Drive consent screen launched
                        // by the IntentSender below, and resumes the connect flow.
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
                                    SettingsEvent.SignOut -> {
                                        // No-op here: AuthGate at the root will swap
                                        // to the SignInScreen as soon as the
                                        // SessionStore clears.
                                    }
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
private fun MusicAppBottomBar(
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
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onNext = playerViewModel::skipNext,
        onPrevious = playerViewModel::skipPrevious,
        onSeek = playerViewModel::seekTo,
    )
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

private data class HomeBuckets(
    val allSongs: List<Song>,
    val quickPicks: List<Song>,
    val jumpBackIn: List<Song>,
    val favoriteSongs: List<Song>,
    val spotlightedSectionTitles: Set<String>,
)

@Composable
private fun HomeScreen(
    state: com.example.musicapp.ui.home.HomeUiState,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val buckets = remember(state.sections) {
        val allSongs = state.sections.flatMap { it.songs }.distinctBy { it.id }
        val recentlyPlayed = state.sections
            .firstOrNull { it.title.equals("Recently Played", ignoreCase = true) }
            ?.songs
            .orEmpty()
        val favoriteSongs = state.sections
            .firstOrNull { it.title.equals("Favorite Songs", ignoreCase = true) }
            ?.songs
            .orEmpty()
        HomeBuckets(
            allSongs = allSongs,
            quickPicks = allSongs.take(6),
            jumpBackIn = recentlyPlayed.ifEmpty { allSongs.drop(6).ifEmpty { allSongs } }.take(8),
            favoriteSongs = favoriteSongs.ifEmpty { allSongs.filter { it.isFavorite }.ifEmpty { allSongs } }.take(5),
            spotlightedSectionTitles = buildSet {
                if (recentlyPlayed.isNotEmpty()) add("Recently Played")
                if (favoriteSongs.isNotEmpty()) add("Favorite Songs")
            },
        )
    }

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
                HomeHeader(chips = HOME_FILTER_CHIPS)
            }
            item {
                QuickPickGrid(
                    songs = buckets.quickPicks,
                    onPlaySong = { song ->
                        onPlaySong(
                            song,
                            buckets.allSongs.ifEmpty { buckets.quickPicks },
                        )
                    },
                )
            }
            if (buckets.jumpBackIn.isNotEmpty()) {
                item {
                    FeatureSection(
                        title = "Jump back in",
                        subtitle = "Made for your recent rotations",
                        songs = buckets.jumpBackIn,
                        onPlaySong = { song -> onPlaySong(song, buckets.jumpBackIn) },
                    )
                }
            }
            if (buckets.favoriteSongs.isNotEmpty()) {
                item {
                    RecentSection(
                        title = "Favorite Songs",
                        songs = buckets.favoriteSongs,
                        onPlaySong = { song -> onPlaySong(song, buckets.favoriteSongs) },
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
            val remainingSections = state.sections.filterNot { section ->
                section.title in buckets.spotlightedSectionTitles
            }
            items(
                items = remainingSections,
                key = { section -> section.title },
            ) { section ->
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
            items(
                items = songs,
                key = { song -> song.id },
            ) { song ->
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
    val gradient = remember(song.id) { gradientForSong(song.id) }
    val initial = remember(song.id, song.title, song.artist) {
        song.title.take(1).ifBlank { song.artist.take(1) }.uppercase()
    }

    // Always render the gradient + initial as a placeholder underneath the image,
    // so a failed Coil load (404, network blip, missing art file on the backend)
    // doesn't leave an empty box. The AsyncImage paints over the placeholder once
    // it successfully loads.
    Box(
        modifier = boxModifier.background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun gradientForSong(songId: String): Brush {
    // Deterministic palette so the same song always picks the same pair of colors.
    val seed = (songId.hashCode().absoluteValue) % ARTWORK_PALETTES.size
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
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (String, String) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onPlayGroup: (List<Song>) -> Unit,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createNameDraft by rememberSaveable { mutableStateOf("") }
    var renamePlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameNameDraft by rememberSaveable { mutableStateOf("") }
    var deletePlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var openPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var addSongsPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    val playlistsById = remember(state.playlists) { state.playlists.associateBy { it.id } }
    val renameTarget = remember(renamePlaylistId, playlistsById) { renamePlaylistId?.let(playlistsById::get) }
    val deleteTarget = remember(deletePlaylistId, playlistsById) { deletePlaylistId?.let(playlistsById::get) }
    val openTarget = remember(openPlaylistId, playlistsById) { openPlaylistId?.let(playlistsById::get) }
    val addSongsTarget = remember(addSongsPlaylistId, playlistsById) { addSongsPlaylistId?.let(playlistsById::get) }

    LaunchedEffect(state.playlists, renamePlaylistId, deletePlaylistId, openPlaylistId, addSongsPlaylistId) {
        if (renamePlaylistId != null && renameTarget == null) renamePlaylistId = null
        if (deletePlaylistId != null && deleteTarget == null) deletePlaylistId = null
        if (openPlaylistId != null && openTarget == null) openPlaylistId = null
        if (addSongsPlaylistId != null && addSongsTarget == null) addSongsPlaylistId = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                IconButton(
                    onClick = {
                        createNameDraft = ""
                        showCreateDialog = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create playlist",
                        tint = SpotifyWhite,
                    )
                }
            }
        }
        item {
            LibraryTabRow(
                selected = state.selectedTab,
                onSelect = onSelectTab,
            )
        }
        when (state.selectedTab) {
            LibraryTab.Playlists -> libraryPlaylistsSection(
                state = state,
                onOpenPlaylist = { playlistId -> openPlaylistId = playlistId },
                onRenamePlaylist = { playlist ->
                    renamePlaylistId = playlist.id
                    renameNameDraft = playlist.name
                },
                onDeletePlaylist = { playlistId -> deletePlaylistId = playlistId },
            )
            LibraryTab.Songs -> librarySongsSection(state, onToggleFavorite)
            LibraryTab.Artists -> libraryArtistsSection(state, onPlayGroup)
            LibraryTab.Albums -> libraryAlbumsSection(state, onPlayGroup)
        }
    }

    if (showCreateDialog) {
        val normalizedDraft = createNameDraft.trim()
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = SpotifyCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SpotifyGreen, Color(0xFF1F4D3A)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SpotifyBackground,
                        )
                    }
                    Text(
                        "Create playlist",
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Pick a name you can recognize fast.",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = createNameDraft,
                        onValueChange = { value ->
                            if (value.length <= 60) createNameDraft = value
                        },
                        singleLine = true,
                        label = { Text("Playlist name") },
                        placeholder = { Text("Late night drive mix") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            focusedLabelColor = SpotifyGreen,
                            unfocusedLabelColor = SpotifyTextMuted,
                            focusedContainerColor = SpotifyBackground.copy(alpha = 0.35f),
                            unfocusedContainerColor = SpotifyBackground.copy(alpha = 0.2f),
                            focusedTextColor = SpotifyWhite,
                            unfocusedTextColor = SpotifyWhite,
                            cursorColor = SpotifyGreen,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Use descriptive name",
                            color = SpotifyTextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "${normalizedDraft.length}/60",
                            color = SpotifyTextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(createNameDraft)
                        showCreateDialog = false
                    },
                    enabled = normalizedDraft.isNotBlank(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBackground,
                    ),
                ) {
                    Text("Create playlist", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SpotifyWhite)
                }
            },
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renamePlaylistId = null },
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(
                    value = renameNameDraft,
                    onValueChange = { renameNameDraft = it },
                    singleLine = true,
                    label = { Text("Playlist name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenamePlaylist(renameTarget.id, renameNameDraft)
                        renamePlaylistId = null
                    },
                    enabled = renameNameDraft.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamePlaylistId = null }) { Text("Cancel") }
            },
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deletePlaylistId = null },
            title = { Text("Delete playlist") },
            text = { Text("Delete \"${deleteTarget.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(deleteTarget.id)
                        deletePlaylistId = null
                        if (openPlaylistId == deleteTarget.id) openPlaylistId = null
                        if (addSongsPlaylistId == deleteTarget.id) addSongsPlaylistId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletePlaylistId = null }) { Text("Cancel") }
            },
        )
    }

    if (openTarget != null) {
        PlaylistSongsDialog(
            playlist = openTarget,
            songs = state.songs,
            onDismiss = { openPlaylistId = null },
            onAddSongs = { addSongsPlaylistId = openTarget.id },
            onRemoveSong = { songId -> onRemoveSongFromPlaylist(openTarget.id, songId) },
        )
    }

    if (addSongsTarget != null) {
        PlaylistSongPickerDialog(
            playlist = addSongsTarget,
            allSongs = state.songs,
            onDismiss = { addSongsPlaylistId = null },
            onAddSong = { songId -> onAddSongToPlaylist(addSongsTarget.id, songId) },
        )
    }
}

@Composable
private fun LibraryTabRow(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(tab) },
                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SpotifyGreen.copy(alpha = 0.22f),
                    selectedLabelColor = SpotifyGreen,
                    containerColor = SpotifyCard,
                    labelColor = SpotifyWhite,
                ),
                border = null,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.libraryPlaylistsSection(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onOpenPlaylist: (String) -> Unit,
    onRenamePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit,
) {
    if (state.playlists.isEmpty()) {
        item { EmptyLibraryMessage(text = "No playlists yet. Tap + to create one.") }
        return
    }
    item {
        LibrarySectionHeader(title = "Playlists", subtitle = "${state.playlists.size} playlists")
    }
    items(
        items = state.playlists,
        key = { playlist -> playlist.id },
    ) { playlist ->
        PlaylistRow(
            playlist = playlist,
            onOpen = { onOpenPlaylist(playlist.id) },
            onRename = { onRenamePlaylist(playlist) },
            onDelete = { onDeletePlaylist(playlist.id) },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.librarySongsSection(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onToggleFavorite: (String) -> Unit,
) {
    if (state.songs.isEmpty()) {
        item { EmptyLibraryMessage(text = "No songs yet. Connect Drive or add local files.") }
        return
    }
    item {
        LibrarySectionHeader(title = "All songs", subtitle = "${state.songs.size} tracks")
    }
    items(state.songs) { song ->
        SongRow(song = song, onClick = {}, onToggleFavorite = { onToggleFavorite(song.id) })
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.libraryArtistsSection(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onPlayGroup: (List<Song>) -> Unit,
) {
    if (state.artists.isEmpty()) {
        item { EmptyLibraryMessage(text = "No artists to show yet.") }
        return
    }
    item {
        LibrarySectionHeader(title = "Artists", subtitle = "${state.artists.size} artists")
    }
    items(state.artists) { artist ->
        ArtistRow(group = artist, onClick = { onPlayGroup(artist.songs) })
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.libraryAlbumsSection(
    state: com.example.musicapp.ui.library.LibraryUiState,
    onPlayGroup: (List<Song>) -> Unit,
) {
    if (state.albums.isEmpty()) {
        item { EmptyLibraryMessage(text = "No albums to show yet.") }
        return
    }
    item {
        LibrarySectionHeader(title = "Albums", subtitle = "${state.albums.size} albums")
    }
    items(state.albums) { album ->
        AlbumRow(group = album, onClick = { onPlayGroup(album.songs) })
    }
}

@Composable
private fun LibrarySectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyWhite,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SpotifyTextMuted,
        )
    }
}

@Composable
private fun EmptyLibraryMessage(text: String) {
    Text(
        text = text,
        color = SpotifyTextMuted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ArtistRow(
    group: ArtistGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            SpotifyGreen.copy(alpha = 0.8f),
                            Color(0xFF1F4D3A),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (group.primaryArtworkUri != null) {
                AsyncImage(
                    model = group.primaryArtworkUri,
                    contentDescription = group.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = group.name.take(1).uppercase(),
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Artist • ${group.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextMuted,
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = SpotifyTextMuted,
        )
    }
}

@Composable
private fun AlbumRow(
    group: AlbumGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            if (group.primaryArtworkUri != null) {
                AsyncImage(
                    model = group.primaryArtworkUri,
                    contentDescription = group.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B2208), Color(0xFFF39C12).copy(alpha = 0.6f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.name.take(1).uppercase(),
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${group.artist} • ${group.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = SpotifyTextMuted,
        )
    }
}

// ---------------------------------------------------------------------------
// Player
// ---------------------------------------------------------------------------

@Composable
private fun PlayerScreen(
    state: PlayerUiState,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val ambient = state.currentSong?.let { gradientForSong(it.id) }
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
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = state.currentSong != null,
                ) {
                    Icon(
                        imageVector = if (state.isCurrentSongFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (state.isCurrentSongFavorite) "Unlike" else "Like",
                        tint = if (state.isCurrentSongFavorite) SpotifyGreen else SpotifyWhite,
                        modifier = Modifier.size(26.dp),
                    )
                }
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
    onChooseLocalFolder: () -> Unit,
    onRemoveLocalFolder: (String) -> Unit,
    onClearLocalFolders: () -> Unit,
    onNavigateUpFolder: () -> Unit,
    onDismissFolderPicker: () -> Unit,
    onOpenFolder: (DriveFolder) -> Unit,
    onSelectCurrentFolder: () -> Unit,
    onRefresh: () -> Unit,
    onPauseSync: () -> Unit,
    onResumeSync: () -> Unit,
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
        item {
            LocalLibraryCard(
                state = state,
                onChooseLocalFolder = onChooseLocalFolder,
                onRemoveLocalFolder = onRemoveLocalFolder,
                onClearLocalFolders = onClearLocalFolders,
            )
        }
        item { DriveAccessCard(state, onConnectDrive, onDisconnectDrive, onChooseFolder) }
        item { DriveSyncCard(state, onRefresh, onPauseSync, onResumeSync) }
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
    onPauseSync: () -> Unit,
    onResumeSync: () -> Unit,
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

            // Action row swaps Sync / Pause / Resume based on current state. The
            // Pause button only appears mid-sync; Resume only when the latest
            // sync job is paused; otherwise the primary action is Sync.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    state.isDriveSyncing -> {
                        Button(
                            onClick = onPauseSync,
                            enabled = !state.isWorking,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyMuted.copy(alpha = 0.35f),
                                contentColor = SpotifyWhite,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Pause", fontWeight = FontWeight.Bold)
                        }
                    }
                    state.isDrivePaused -> {
                        Button(
                            onClick = onResumeSync,
                            enabled = !state.isWorking,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                contentColor = SpotifyBackground,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Resume sync", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onRefresh,
                            enabled = !state.isWorking && !state.isFolderLoading,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                contentColor = SpotifyBackground,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Sync", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Live sync progress — updates whenever the backend's per-song progress
            // counter advances (the Android client polls /v1/sync/status every
            // ~1.5s and refreshes the song list on the same cadence).
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
                    color = when {
                        state.isDriveSyncing -> SpotifyGreen
                        state.isDrivePaused -> SpotifyWhite
                        else -> SpotifyTextMuted
                    },
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
private fun LocalLibraryCard(
    state: com.example.musicapp.ui.settings.SettingsUiState,
    onChooseLocalFolder: () -> Unit,
    onRemoveLocalFolder: (String) -> Unit,
    onClearLocalFolders: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Local music folders",
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Only songs under selected folders show in library.",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = state.localFolders.size.toString(),
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            if (state.localFolders.isEmpty()) {
                Text(
                    "No local folder filter set. App will scan all local audio files.",
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.localFolders.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                folder,
                                modifier = Modifier.weight(1f),
                                color = SpotifyWhite,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Remove",
                                modifier = Modifier.clickable { onRemoveLocalFolder(folder) },
                                color = SpotifyTextMuted,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onChooseLocalFolder,
                    enabled = !state.isWorking,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBackground,
                    ),
                ) {
                    Text("Choose local folder", fontWeight = FontWeight.Bold)
                }
                if (state.localFolders.isNotEmpty()) {
                    Button(
                        onClick = onClearLocalFolders,
                        enabled = !state.isWorking,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyMuted.copy(alpha = 0.35f),
                            contentColor = SpotifyWhite,
                        ),
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose Drive folder",
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Browse folders, then confirm current path.",
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpotifyBackground.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            state.currentDriveFolderPath,
                            modifier = Modifier.weight(1f),
                            color = SpotifyWhite,
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
                                color = SpotifyGreen,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        text = {
            if (state.isFolderLoading && state.availableDriveFolders.isEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SpotifyGreen,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        "Loading folders...",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (state.availableDriveFolders.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpotifyBackground.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "No subfolders here. You can use current path.",
                        modifier = Modifier.padding(12.dp),
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Subfolders",
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.availableDriveFolders) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isFolderLoading) { onOpenFolder(folder) },
                                colors = CardDefaults.cardColors(
                                    containerColor = SpotifyBackground.copy(alpha = 0.55f),
                                ),
                                shape = RoundedCornerShape(14.dp),
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
                                        Text(
                                            folder.name,
                                            color = SpotifyWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            folder.path,
                                            color = SpotifyTextMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(SpotifyGreen.copy(alpha = 0.18f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            "Open",
                                            color = SpotifyGreen,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
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
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = SpotifyBackground,
                )
            ) {
                Text("Use current folder", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotifyWhite)
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
private fun PlaylistRow(
    playlist: Playlist,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
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
                "Playlist • ${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextMuted,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename playlist",
                    tint = SpotifyTextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete playlist",
                    tint = SpotifyTextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaylistSongsDialog(
    playlist: Playlist,
    songs: List<Song>,
    onDismiss: () -> Unit,
    onAddSongs: () -> Unit,
    onRemoveSong: (String) -> Unit,
) {
    val songsById = remember(songs) { songs.associateBy { it.id } }
    val playlistSongs = playlist.songIds.mapNotNull { songId -> songsById[songId] }
    val unavailableCount = playlist.songIds.size - playlistSongs.size
    val hasSongCountOnly = playlist.songCount > 0 && playlist.songIds.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(playlist.name, fontWeight = FontWeight.Black)
                Text(
                    "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextMuted,
                )
            }
        },
        text = {
            if (hasSongCountOnly) {
                Text(
                    "Playlist has songs, but song-level list is not available from current API response. Tap Add songs to manage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyTextMuted,
                )
            } else if (playlist.songIds.isEmpty()) {
                Text(
                    "Playlist empty. Tap Add songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyTextMuted,
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    playlistSongs.forEach { song ->
                        PlaylistSongActionRow(
                            song = song,
                            actionIcon = Icons.Default.Delete,
                            actionDescription = "Remove from playlist",
                            onAction = { onRemoveSong(song.id) },
                        )
                    }
                    if (unavailableCount > 0) {
                        Text(
                            "$unavailableCount songs unavailable in current library view.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotifyTextMuted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onAddSongs) { Text("Add songs") }
        },
    )
}

@Composable
private fun PlaylistSongPickerDialog(
    playlist: Playlist,
    allSongs: List<Song>,
    onDismiss: () -> Unit,
    onAddSong: (String) -> Unit,
) {
    val existingSongIds = remember(playlist.songIds) { playlist.songIds.toSet() }
    val candidates = remember(allSongs, existingSongIds) {
        allSongs.filterNot { song -> song.id in existingSongIds }
            .sortedBy { song -> song.title.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add songs to ${playlist.name}") },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    "No additional songs available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyTextMuted,
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    candidates.forEach { song ->
                        PlaylistSongActionRow(
                            song = song,
                            actionIcon = Icons.Default.Add,
                            actionDescription = "Add to playlist",
                            onAction = { onAddSong(song.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun PlaylistSongActionRow(
    song: Song,
    actionIcon: ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = song.artist,
                color = SpotifyTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = SpotifyTextMuted,
                modifier = Modifier.size(18.dp),
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
