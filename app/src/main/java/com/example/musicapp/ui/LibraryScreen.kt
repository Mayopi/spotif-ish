package com.example.musicapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.musicapp.domain.model.Playlist
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.library.AlbumGroup
import com.example.musicapp.ui.library.ArtistGroup
import com.example.musicapp.ui.library.LibraryTab

@Composable
fun LibraryScreen(
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
