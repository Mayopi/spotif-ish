package com.example.musicapp.data.network.dto

import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.DriveSyncState
import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusDto(
    val state: String, // "idle" | "queued" | "running" | "succeeded" | "failed"
    val processedCount: Int = 0,
    val totalCount: Int? = null,
    val lastError: String? = null,
    val lastSyncedAt: String? = null, // RFC3339
)

@Serializable
data class SyncRunResponse(
    val syncJobId: String,
)

@Serializable
data class DriveFolderDto(
    val id: String,
    val name: String,
    val path: String,
)

@Serializable
data class DriveFolderListDto(
    val folders: List<DriveFolderDto>,
)

@Serializable
data class SetDriveFolderRequest(
    val folderId: String,
    val folderName: String,
)

fun SyncStatusDto.toDomain(): DriveSyncState = DriveSyncState(
    isSyncing = state == "queued" || state == "running",
    lastError = lastError,
    lastSyncedSongCount = totalCount ?: 0,
    processedFileCount = processedCount,
)

fun DriveFolderDto.toDomain(): DriveFolder = DriveFolder(
    id = id,
    name = name,
    path = path,
)
