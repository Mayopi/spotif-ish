package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(name: String) = playlistRepository.create(name)
}

