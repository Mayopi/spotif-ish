package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.AppSettings
import com.example.musicapp.domain.model.FolderConnection
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateTheme(isDarkTheme: Boolean)
    suspend fun updateSelectedFolders(folders: List<String>)
    suspend fun updateDriveFolder(connection: FolderConnection?)
}

