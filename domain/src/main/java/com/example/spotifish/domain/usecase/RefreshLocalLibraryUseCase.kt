package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.MusicRepository
import javax.inject.Inject

class RefreshLocalLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke() {
        musicRepository.refreshLocalLibrary()
    }
}
