package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.FolderConnection
import com.example.spotifish.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateDriveFolderUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(connection: FolderConnection?) {
        settingsRepository.updateDriveFolder(connection)
    }
}
