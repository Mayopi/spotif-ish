package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.HomeSection
import com.example.musicapp.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun observeAllSongs(): Flow<List<Song>>
    fun observeHomeSections(): Flow<List<HomeSection>>
    suspend fun refreshLocalLibrary()
    suspend fun refreshDriveLibrary()
    suspend fun search(query: String): List<Song>
    suspend fun getSong(songId: String): Song?
}

