package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke(): Flow<List<Song>> = musicRepository.observeAllSongs()
}

