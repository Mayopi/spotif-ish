package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

class RefreshLibrariesUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke() {
        musicRepository.refreshLocalLibrary()
        musicRepository.refreshDriveLibrary()
    }
}

