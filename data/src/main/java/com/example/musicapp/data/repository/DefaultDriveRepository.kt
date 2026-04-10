package com.example.musicapp.data.repository

import com.example.musicapp.data.drive.DriveMusicDataSource
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.repository.DriveRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDriveRepository @Inject constructor(
    private val driveMusicDataSource: DriveMusicDataSource,
) : DriveRepository {
    override suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder> =
        driveMusicDataSource.listFolders(parentId = parentId, parentPath = parentPath)
}
