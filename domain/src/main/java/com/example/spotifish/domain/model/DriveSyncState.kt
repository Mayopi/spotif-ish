package com.example.spotifish.domain.model

data class DriveSyncState(
    val isSyncing: Boolean = false,
    val isPaused: Boolean = false,
    val lastError: String? = null,
    val lastSyncedSongCount: Int = 0,
    val processedFileCount: Int = 0,
)
