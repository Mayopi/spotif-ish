package com.example.spotifish.data.network.dto

import com.example.spotifish.domain.model.DriveFolder
import com.example.spotifish.domain.model.DriveSyncState
import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusDto(
    // "none" | "queued" | "running" | "paused" | "succeeded" | "failed"
    val state: String,
    val processedCount: Int = 0,
    val totalCount: Int? = null,
    val lastError: String? = null,
    val lastSyncedAt: String? = null, // RFC3339
)

@Serializable
data class SyncRunResponse(
    val syncJobId: String,
    // Backend's handler also returns the current state ("queued" / "paused" / etc.)
    // but we don't need to do anything with it on the client.
    val state: String? = null,
)

@Serializable
data class DriveFolderDto(
    val id: String,
    val name: String,
    // The backend (`model.DriveFolderInfo`) does NOT carry a path, so this is
    // optional. The client computes a display path locally by chaining parent
    // folder names during browsing.
    val path: String? = null,
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

/**
 * Body for `POST /v1/drive/connect`. The backend's handler exchanges this with
 * Google via `oauthConfig.Exchange(...)` to obtain Drive access + refresh tokens,
 * then encrypts and stores them. Field name MUST match
 * `internal/handler/drive_handler.go:23`.
 */
@Serializable
data class ConnectDriveRequest(
    val authCode: String,
)

fun SyncStatusDto.toDomain(): DriveSyncState = DriveSyncState(
    isSyncing = state == "queued" || state == "running",
    isPaused = state == "paused",
    lastError = lastError,
    lastSyncedSongCount = totalCount ?: 0,
    processedFileCount = processedCount,
)

fun DriveFolderDto.toDomain(parentPath: String): DriveFolder = DriveFolder(
    id = id,
    name = name,
    // Compose a display path from the parent we're currently browsing.
    path = if (parentPath.isBlank()) name else "$parentPath/$name",
)
