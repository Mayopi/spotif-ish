package com.example.musicapp.data.repository

import android.content.Context
import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent JSON cache for the Drive-sourced library.
 *
 * The Drive sync flow used to live entirely in memory, so closing the app cleared the
 * library and forced a full re-scan on next launch. This store survives process death
 * by serialising the song list to `filesDir/drive_library.json` after each successful
 * sync (and after partial failures), and is rehydrated by [DefaultMusicRepository] in
 * its init block so the UI shows the cached library before any network call runs.
 *
 * Mutations are serialised through a [Mutex] so concurrent saves can't corrupt the
 * file mid-write.
 */
@Singleton
class DriveLibraryStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatchersProvider: DispatchersProvider,
) {

    private val file = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()

    suspend fun load(): List<Song> = mutex.withLock {
        withContext(dispatchersProvider.io) {
            if (!file.exists()) return@withContext emptyList()
            runCatching {
                val text = file.readText()
                if (text.isBlank()) return@runCatching emptyList()
                val arr = JSONArray(text)
                List(arr.length()) { i -> deserialize(arr.getJSONObject(i)) }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun save(songs: List<Song>) {
        mutex.withLock {
            withContext(dispatchersProvider.io) {
                runCatching {
                    val arr = JSONArray()
                    songs.forEach { song -> arr.put(serialize(song)) }
                    file.writeText(arr.toString())
                }
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            withContext(dispatchersProvider.io) {
                runCatching { file.delete() }
            }
        }
    }

    private fun serialize(song: Song): JSONObject = JSONObject().apply {
        put("id", song.id)
        put("title", song.title)
        put("artist", song.artist)
        put("album", song.album)
        put("durationMs", song.durationMs)
        song.albumArtUri?.let { put("albumArtUri", it) }
        put("playableUri", song.playableUri)
        song.mimeType?.let { put("mimeType", it) }
        put("addedAtEpochMillis", song.addedAtEpochMillis)
        song.authAccountEmail?.let { put("authAccountEmail", it) }
    }

    private fun deserialize(obj: JSONObject): Song = Song(
        id = obj.getString("id"),
        title = obj.getString("title"),
        artist = obj.getString("artist"),
        album = obj.getString("album"),
        durationMs = obj.optLong("durationMs", 0L),
        albumArtUri = obj.optString("albumArtUri").takeIf { it.isNotBlank() },
        // The cache only ever holds Drive songs — local files are scanned fresh each
        // launch from MediaStore, so they're never persisted here.
        sourceType = SourceType.DRIVE,
        playableUri = obj.getString("playableUri"),
        mimeType = obj.optString("mimeType").takeIf { it.isNotBlank() },
        addedAtEpochMillis = obj.optLong("addedAtEpochMillis", 0L),
        authAccountEmail = obj.optString("authAccountEmail").takeIf { it.isNotBlank() },
    )

    private companion object {
        private const val FILE_NAME = "drive_library.json"
    }
}
