package com.example.spotifish.domain.usecase

import com.example.spotifish.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(songId: String) = favoritesRepository.toggleFavorite(songId)
}

