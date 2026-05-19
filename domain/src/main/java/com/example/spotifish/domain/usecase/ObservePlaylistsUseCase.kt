package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.model.Playlist
import com.example.spotifish.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = playlistRepository.observePlaylists()
}

