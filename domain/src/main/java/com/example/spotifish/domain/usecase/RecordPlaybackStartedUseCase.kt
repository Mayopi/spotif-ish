package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.MusicRepository
import javax.inject.Inject

class RecordPlaybackStartedUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(songId: String) {
        musicRepository.recordPlaybackStarted(songId)
    }
}
