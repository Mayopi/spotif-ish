package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: String) {
        playlistRepository.delete(playlistId)
    }
}
