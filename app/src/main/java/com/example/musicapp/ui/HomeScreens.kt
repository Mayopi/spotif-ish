package com.example.musicapp.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicapp.domain.model.HomeSection
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.home.HomeUiState

data class HomeArtistGroup(
    val name: String,
    val songs: List<Song>,
) {
    val songCount: Int get() = songs.size
    val artworkSong: Song? get() = songs.firstOrNull { it.albumArtUri != null } ?: songs.firstOrNull()
}

data class HomeAlbumGroup(
    val name: String,
    val artist: String,
    val songs: List<Song>,
) {
    val songCount: Int get() = songs.size
    val artworkSong: Song? get() = songs.firstOrNull { it.albumArtUri != null } ?: songs.firstOrNull()
}

data class HomeGrouping(
    val allSongs: List<Song> = emptyList(),
    val artists: List<HomeArtistGroup> = emptyList(),
    val albums: List<HomeAlbumGroup> = emptyList(),
)

private val HOME_FILTER_CHIPS = listOf("Semua", "Musik", "Podcast")

fun buildHomeGrouping(sections: List<HomeSection>): HomeGrouping {
    val allSongs = sections.flatMap { it.songs }.distinctBy { it.id }
    val artists = allSongs
        .groupBy { song -> song.artist.ifBlank { "Unknown Artist" } }
        .map { (name, songs) ->
            HomeArtistGroup(
                name = name,
                songs = songs.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.name.lowercase() }
    val albums = allSongs
        .groupBy { song ->
            (song.album.ifBlank { "Unknown Album" }) to (song.artist.ifBlank { "Unknown Artist" })
        }
        .map { (key, songs) ->
            HomeAlbumGroup(
                name = key.first,
                artist = key.second,
                songs = songs.sortedBy { it.title.lowercase() },
            )
        }
        .sortedWith(compareBy({ it.name.lowercase() }, { it.artist.lowercase() }))
    return HomeGrouping(
        allSongs = allSongs,
        artists = artists,
        albums = albums,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenArtist: (HomeArtistGroup) -> Unit,
    onOpenAlbum: (HomeAlbumGroup) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val grouping = remember(state.sections) { buildHomeGrouping(state.sections) }
    val recentlyPlayed = remember(state.sections) { extractRecentlyPlayed(state.sections) }
    val quickPicks = remember(grouping.allSongs, recentlyPlayed) {
        recentlyPlayed.ifEmpty { grouping.allSongs }.take(8)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { HomeHeader(chips = HOME_FILTER_CHIPS) }
            if (grouping.allSongs.isEmpty()) {
                item { EmptyLibraryHint() }
            } else {
                if (quickPicks.isNotEmpty()) {
                    item {
                        QuickPickGrid(
                            songs = quickPicks,
                            onPlaySong = { song -> onPlaySong(song, quickPicks) },
                        )
                    }
                }
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = "Sering kamu putar baru-baru ini",
                            subtitle = "${recentlyPlayed.size} lagu terakhir",
                        )
                    }
                    items(recentlyPlayed.take(6), key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            onClick = { onPlaySong(song, recentlyPlayed) },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeSectionHeader(
                            title = "Artists",
                            subtitle = "${grouping.artists.size} artists in your library",
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(grouping.artists, key = { artist -> artist.name }) { artist ->
                                ArtistGroupCard(
                                    group = artist,
                                    onClick = { onOpenArtist(artist) },
                                )
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeSectionHeader(
                            title = "Albums",
                            subtitle = "${grouping.albums.size} albums available",
                        )
                        grouping.albums.chunked(2).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowAlbums.forEach { album ->
                                    AlbumGroupCard(
                                        group = album,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenAlbum(album) },
                                    )
                                }
                                if (rowAlbums.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            Text(
                "Home",
                color = SpotifyTextMuted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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

private fun extractRecentlyPlayed(sections: List<HomeSection>): List<Song> {
    val section = sections.firstOrNull { candidate ->
        candidate.title.equals("Recently Played", ignoreCase = true) ||
            candidate.title.equals("Recent", ignoreCase = true) ||
            candidate.title.contains("recent", ignoreCase = true)
    }
    return section?.songs.orEmpty().distinctBy { it.id }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = subtitle,
            color = SpotifyTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ArtistGroupCard(
    group: HomeArtistGroup,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val artworkSong = group.artworkSong
        if (artworkSong != null) {
            ArtworkThumb(
                song = artworkSong,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(SpotifyCard),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = group.name.take(1).uppercase(),
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = group.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SpotifyWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${group.songCount} songs",
            color = SpotifyTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AlbumGroupCard(
    group: HomeAlbumGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val artworkSong = group.artworkSong
            if (artworkSong != null) {
                ArtworkThumb(
                    song = artworkSong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpotifyMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.name.take(1).uppercase(),
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(
                text = group.name,
                color = SpotifyWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = group.artist,
                color = SpotifyTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artist: HomeArtistGroup?,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (artist == null) {
        DetailNotFoundScreen(
            title = "Artist not found",
            onBack = onBack,
        )
        return
    }
    val queue = artist.songs
    val artworkSong = artist.artworkSong
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(artworkSong?.let { gradientForSong(it.id) } ?: Brush.verticalGradient(listOf(SpotifyCard, SpotifyBackground))),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SpotifyWhite,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (artworkSong != null) {
                        ArtworkThumb(
                            song = artworkSong,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        artist.name,
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${artist.songCount} songs",
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            queue.firstOrNull()?.let { first ->
                                onPlaySong(first, queue)
                            }
                        },
                        enabled = queue.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = SpotifyBackground,
                        ),
                    ) {
                        Text("Play all", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val shuffled = queue.shuffled()
                            shuffled.firstOrNull()?.let { first ->
                                onPlaySong(first, shuffled)
                            }
                        },
                        enabled = queue.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyCard,
                            contentColor = SpotifyWhite,
                        ),
                    ) {
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(queue, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onPlaySong(song, queue) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: HomeAlbumGroup?,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (album == null) {
        DetailNotFoundScreen(
            title = "Album not found",
            onBack = onBack,
        )
        return
    }
    val tracks = remember(album.songs) {
        album.songs.sortedBy { it.title.lowercase() }
    }
    val artworkSong = album.artworkSong
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(artworkSong?.let { gradientForSong(it.id) } ?: Brush.verticalGradient(listOf(SpotifyCard, SpotifyBackground))),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SpotifyWhite,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (artworkSong != null) {
                        ArtworkThumb(
                            song = artworkSong,
                            modifier = Modifier
                                .size(190.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Text(
                        album.name,
                        color = SpotifyWhite,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        album.artist,
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            tracks.firstOrNull()?.let { first ->
                                onPlaySong(first, tracks)
                            }
                        },
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = SpotifyBackground,
                        ),
                    ) {
                        Text("Play all", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val shuffled = tracks.shuffled()
                            shuffled.firstOrNull()?.let { first ->
                                onPlaySong(first, shuffled)
                            }
                        },
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyCard,
                            contentColor = SpotifyWhite,
                        ),
                    ) {
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(tracks.size, key = { tracks[it].id }) { index ->
                val song = tracks[index]
                AlbumTrackRow(
                    index = index + 1,
                    song = song,
                    onClick = { onPlaySong(song, tracks) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryHint() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpotifyCard),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No songs available",
                color = SpotifyWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Connect Drive or add local folders, then refresh your library.",
                color = SpotifyTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DetailNotFoundScreen(
    title: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SpotifyWhite,
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = SpotifyCard),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                color = SpotifyWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            color = SpotifyTextMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = SpotifyWhite,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDuration(song.durationMs),
                color = SpotifyTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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

private fun greetingForNow(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
