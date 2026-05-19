package com.example.musicapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicapp.domain.model.DriveFolder

@Composable
fun SettingsScreen(
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
