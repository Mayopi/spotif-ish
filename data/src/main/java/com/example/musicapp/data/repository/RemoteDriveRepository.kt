package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.data.network.dto.toDomain
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.repository.DriveRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class RemoteDriveRepository @Inject constructor(
    private val api: SpotifishApi,
    private val dispatchersProvider: DispatchersProvider,
) : DriveRepository {

    override suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder> {
        return withContext(dispatchersProvider.io) {
            api.listDriveFolders(parentId = parentId.takeIf { it.isNotBlank() && it != "root" })
                .folders
                .map { it.toDomain(parentPath = parentPath) }
        }
    }
}
