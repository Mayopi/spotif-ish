package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.PlaylistRepository
import javax.inject.Inject

class AddSongToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: String, songId: String) {
        playlistRepository.addSong(playlistId, songId)
    }
}
