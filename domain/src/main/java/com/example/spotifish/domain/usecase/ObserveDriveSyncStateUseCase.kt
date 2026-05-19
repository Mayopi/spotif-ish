package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.MusicRepository
import javax.inject.Inject

class ObserveDriveSyncStateUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke() = musicRepository.observeDriveSyncState()
}
