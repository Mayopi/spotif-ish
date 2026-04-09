package com.example.musicapp.data.repository

import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.store.JsonFileStore
import com.example.musicapp.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Singleton
class DefaultFavoritesRepository @Inject constructor(
    private val jsonFileStore: JsonFileStore,
    private val dispatchersProvider: DispatchersProvider,
) : FavoritesRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val favorites = MutableStateFlow<Set<String>>(emptySet())

    init {
        scope.launch {
            favorites.value = loadFavorites()
        }
    }

    override fun observeFavorites() = favorites.asStateFlow()

    override suspend fun toggleFavorite(songId: String) {
        val updated = favorites.value.toMutableSet().apply {
            if (!add(songId)) remove(songId)
        }
        favorites.value = updated
        saveFavorites(updated)
    }

    private suspend fun loadFavorites(): Set<String> = withContext(dispatchersProvider.io) {
        val content = jsonFileStore.read(FILE_NAME, "[]")
        val array = JSONArray(content)
        buildSet {
            repeat(array.length()) { index ->
                add(array.getString(index))
            }
        }
    }

    private suspend fun saveFavorites(songIds: Set<String>) = withContext(dispatchersProvider.io) {
        val array = JSONArray()
        songIds.forEach(array::put)
        jsonFileStore.write(FILE_NAME, array.toString())
    }

    private companion object {
        const val FILE_NAME = "favorites.json"
    }
}

