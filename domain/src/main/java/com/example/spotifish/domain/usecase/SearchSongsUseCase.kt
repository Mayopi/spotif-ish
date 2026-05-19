package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.Song
import com.example.spotifish.domain.repository.MusicRepository
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(query: String): List<Song> = musicRepository.search(query)
}

