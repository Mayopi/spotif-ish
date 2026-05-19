package com.example.spotifish.domain.repository

import com.example.spotifish.domain.model.DriveFolder

interface DriveRepository {
    suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder>
}
