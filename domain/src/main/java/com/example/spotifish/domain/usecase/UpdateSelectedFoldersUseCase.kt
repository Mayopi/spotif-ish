package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSelectedFoldersUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(folders: List<String>) {
        settingsRepository.updateSelectedFolders(folders)
    }
}
