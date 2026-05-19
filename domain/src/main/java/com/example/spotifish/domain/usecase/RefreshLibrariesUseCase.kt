package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.MusicRepository
import javax.inject.Inject

class RefreshLibrariesUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke() {
        musicRepository.refreshLocalLibrary()
        musicRepository.refreshDriveLibrary()
    }
}

