package com.example.musicapp.data.drive

import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.Song

interface DriveMusicDataSource {
    suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder>

    /**
     * Scans the connected Drive folder recursively.
     *
     * @param previousById Songs from the previous successful sync, keyed by their
     *   stable Song id (e.g. `"drive:<fileId>"`). Files whose Drive `modifiedTime`
     *   matches the cached entry's `addedAtEpochMillis` are reused as-is, so re-syncs
     *   skip metadata downloading for unchanged files.
     * @param onSongDiscovered Invoked as soon as each audio file has been resolved
     *   (either reused from cache or freshly extracted), so callers can surface songs
     *   to the UI incrementally instead of waiting for the full scan to finish.
     * @param onProgress Invoked with the running count of audio files visited.
     */
    suspend fun fetchSongs(
        previousById: Map<String, Song> = emptyMap(),
        onSongDiscovered: (suspend (Song) -> Unit)? = null,
        onProgress: ((processedFileCount: Int) -> Unit)? = null,
    ): List<Song>
}
