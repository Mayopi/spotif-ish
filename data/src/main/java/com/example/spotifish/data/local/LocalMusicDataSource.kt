package com.example.spotifish.data.local

import com.example.spotifish.domain.model.Song

interface LocalMusicDataSource {
    suspend fun scan(selectedFolders: List<String> = emptyList()): List<Song>
}
