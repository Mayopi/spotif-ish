package com.example.musicapp.data.drive

import com.example.musicapp.domain.model.Song

interface DriveMusicDataSource {
    suspend fun fetchSongs(): List<Song>
}

