package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.Song
import com.example.spotifish.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke(): Flow<List<Song>> = musicRepository.observeAllSongs()
}

