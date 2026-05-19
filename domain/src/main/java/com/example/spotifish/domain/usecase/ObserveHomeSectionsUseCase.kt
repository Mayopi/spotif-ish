package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.HomeSection
import com.example.spotifish.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHomeSectionsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke(): Flow<List<HomeSection>> = musicRepository.observeHomeSections()
}

