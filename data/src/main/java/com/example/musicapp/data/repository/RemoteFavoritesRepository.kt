package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.domain.repository.FavoritesRepository
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

    init {
        scope.launch { runCatching { reload() } }
    }

    override fun observeFavorites(): Flow<Set<String>> = favorites

    override suspend fun toggleFavorite(songId: String) {
        // Optimistic local toggle so the heart icon flips instantly. The remote call
        // is fired-and-awaited; on failure we restore the previous set.
        val current = favorites.value
        val next = if (songId in current) current - songId else current + songId
        favorites.value = next

        runCatching {
            if (songId in current) api.unlikeSong(songId) else api.likeSong(songId)
        }.onFailure {
            favorites.value = current
        }
    }

    private suspend fun reload() {
        favorites.value = withContext(dispatchersProvider.io) {
            api.listFavoriteIds().toSet()
        }
    }
}
