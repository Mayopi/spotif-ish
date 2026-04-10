package com.example.musicapp.data.drive

import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.Song

interface DriveMusicDataSource {
    suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder>
    suspend fun fetchSongs(): List<Song>
}
