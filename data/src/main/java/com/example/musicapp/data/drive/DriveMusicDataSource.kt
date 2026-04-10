package com.example.musicapp.data.drive

import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.Song

interface DriveMusicDataSource {
    suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder>

    /**
     * Scans the connected Drive folder recursively.
     *
     * @param onSongDiscovered Invoked as soon as each audio file has been parsed, so callers can
     *   surface songs to the UI incrementally instead of waiting for the full scan to finish.
     * @param onProgress Invoked with the running count of audio files processed so far.
     */
    suspend fun fetchSongs(
        onSongDiscovered: (suspend (Song) -> Unit)? = null,
        onProgress: ((processedFileCount: Int) -> Unit)? = null,
    ): List<Song>
}
