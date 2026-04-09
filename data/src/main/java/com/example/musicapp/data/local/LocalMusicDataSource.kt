package com.example.musicapp.data.local

import com.example.musicapp.domain.model.Song

interface LocalMusicDataSource {
    suspend fun scan(): List<Song>
}

