package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

class RefreshLocalLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke() {
        musicRepository.refreshLocalLibrary()
    }
}
