package com.example.musicapp.domain.model

data class DriveSyncState(
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val lastSyncedSongCount: Int = 0,
    val processedFileCount: Int = 0,
)
