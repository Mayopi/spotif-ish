package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.DriveFolder

interface DriveRepository {
    suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder>
}
