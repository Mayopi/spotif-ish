package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

class ObserveDriveSyncStateUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke() = musicRepository.observeDriveSyncState()
}
