package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.DriveFolder
import com.example.spotifish.domain.repository.DriveRepository
import javax.inject.Inject

class ListDriveFoldersUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
) {
    suspend operator fun invoke(parentId: String, parentPath: String): List<DriveFolder> =
        driveRepository.listFolders(parentId = parentId, parentPath = parentPath)
}
