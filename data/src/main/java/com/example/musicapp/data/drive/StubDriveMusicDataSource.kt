package com.example.musicapp.data.drive

import com.example.musicapp.domain.model.Song
import javax.inject.Inject

class StubDriveMusicDataSource @Inject constructor() : DriveMusicDataSource {
    override suspend fun fetchSongs(): List<Song> = emptyList()
}

