package com.example.spotifish.data.repository

import com.example.spotifish.core.DispatchersProvider
import com.example.spotifish.data.network.SpotifishApi
import com.example.spotifish.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RemoteFavoritesRepository @Inject constructor(
    private val api: SpotifishApi,
    private val dispatchersProvider: DispatchersProvider,
) : FavoritesRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val favorites = MutableStateFlow<Set<String>>(emptySet())
    private val favoriteSongIds = MutableStateFlow<List<String>>(emptyList())

    init {
        scope.launch { runCatching { reload() } }
    }

    override fun observeFavorites(): Flow<Set<String>> = favorites

    override fun observeFavoriteSongIds(): Flow<List<String>> = favoriteSongIds

    override suspend fun toggleFavorite(songId: String) {
        // Optimistic local toggle so the heart icon flips instantly. The remote call
        // is fired-and-awaited; on failure we restore the previous set.
        val current = favorites.value
        val currentOrder = favoriteSongIds.value
        val next = if (songId in current) current - songId else current + songId
        val nextOrder = if (songId in current) {
            currentOrder.filterNot { it == songId }
        } else {
            listOf(songId) + currentOrder.filterNot { it == songId }
        }
        favorites.value = next
        favoriteSongIds.value = nextOrder

        runCatching {
            if (songId in current) api.unlikeSong(songId) else api.likeSong(songId)
            reload()
        }.onFailure {
            favorites.value = current
            favoriteSongIds.value = currentOrder
        }
    }

    private suspend fun reload() {
        val response = withContext(dispatchersProvider.io) {
            api.listFavorites()
        }
        val ids = response.safeFavorites.map { it.id }
        favorites.value = ids.toSet()
        favoriteSongIds.value = ids
    }
}
