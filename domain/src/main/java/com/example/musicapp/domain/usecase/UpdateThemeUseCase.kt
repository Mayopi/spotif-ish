package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(isDarkTheme: Boolean) = settingsRepository.updateTheme(isDarkTheme)
}

