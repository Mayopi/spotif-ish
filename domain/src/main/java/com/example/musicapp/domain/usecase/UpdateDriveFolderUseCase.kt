package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.FolderConnection
import com.example.musicapp.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateDriveFolderUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(connection: FolderConnection?) {
        settingsRepository.updateDriveFolder(connection)
    }
}
