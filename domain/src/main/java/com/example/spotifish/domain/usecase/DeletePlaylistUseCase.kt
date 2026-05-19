package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: String) {
        playlistRepository.delete(playlistId)
    }
}
