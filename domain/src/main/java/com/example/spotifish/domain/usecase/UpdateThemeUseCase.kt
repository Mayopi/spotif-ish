package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(isDarkTheme: Boolean) = settingsRepository.updateTheme(isDarkTheme)
}

