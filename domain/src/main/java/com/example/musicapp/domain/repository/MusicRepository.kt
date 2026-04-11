package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.HomeSection
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.DriveSyncState
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun observeAllSongs(): Flow<List<Song>>
    fun observeHomeSections(): Flow<List<HomeSection>>
    fun observeDriveSyncState(): Flow<DriveSyncState>
    suspend fun refreshLocalLibrary()
    suspend fun refreshDriveLibrary()
    fun enqueueDriveLibraryRefresh()
    fun pauseDriveLibraryRefresh()
    fun resumeDriveLibraryRefresh()
    suspend fun search(query: String): List<Song>
    suspend fun getSong(songId: String): Song?
}
