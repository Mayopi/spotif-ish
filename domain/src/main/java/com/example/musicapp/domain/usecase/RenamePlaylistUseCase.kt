package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.PlaylistRepository
import javax.inject.Inject

class RenamePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: String, name: String) {
        playlistRepository.rename(playlistId, name)
    }
}
