package com.example.spotifish.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<Set<String>>
    fun observeFavoriteSongIds(): Flow<List<String>>
    suspend fun toggleFavorite(songId: String)
}
