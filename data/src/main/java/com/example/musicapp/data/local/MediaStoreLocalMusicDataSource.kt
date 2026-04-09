package com.example.musicapp.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaStoreLocalMusicDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalMusicDataSource {

    override suspend fun scan(): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val songs = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                songs += Song(
                    id = "local:$id",
                    title = cursor.getString(titleColumn).orEmpty(),
                    artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown Artist" },
                    album = cursor.getString(albumColumn).orEmpty().ifBlank { "Unknown Album" },
                    durationMs = cursor.getLong(durationColumn),
                    sourceType = SourceType.LOCAL,
                    playableUri = contentUri.toString(),
                    mimeType = cursor.getString(mimeTypeColumn),
                    addedAtEpochMillis = cursor.getLong(dateAddedColumn) * 1000L,
                )
            }
        }

        return songs
    }
}

