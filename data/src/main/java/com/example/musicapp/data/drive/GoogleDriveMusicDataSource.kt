package com.example.musicapp.data.drive

import android.accounts.Account
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.musicapp.core.DriveAuthSessionStore
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import com.example.musicapp.domain.repository.SettingsRepository
import com.google.android.gms.auth.GoogleAuthUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GoogleDriveMusicDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val authSessionStore: DriveAuthSessionStore,
) : DriveMusicDataSource {

    override suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder> {
        return withContext(Dispatchers.IO) {
            val settings = settingsRepository.observeSettings().first()
            val accountEmail = settings.connectedDriveFolder?.accountEmail ?: return@withContext emptyList()
            val token = getAccessToken(accountEmail)
            queryFolders(parentId = parentId, parentPath = parentPath, token = token)
        }
    }

    override suspend fun fetchSongs(
        onSongDiscovered: (suspend (Song) -> Unit)?,
        onProgress: ((processedFileCount: Int) -> Unit)?,
    ): List<Song> {
        return withContext(Dispatchers.IO) {
            val settings = settingsRepository.observeSettings().first()
            val connection = settings.connectedDriveFolder ?: return@withContext emptyList()
            val accountEmail = connection.accountEmail ?: return@withContext emptyList()
            val token = getAccessToken(accountEmail)
            val collected = mutableListOf<Song>()
            var processedFileCount = 0
            querySongsRecursively(
                folderId = connection.folderId.ifBlank { ROOT_ID },
                folderPath = connection.folderName.ifBlank { "My Drive" },
                accountEmail = accountEmail,
                token = token,
                onSongFound = { song ->
                    collected += song
                    processedFileCount += 1
                    onSongDiscovered?.invoke(song)
                    onProgress?.invoke(processedFileCount)
                },
            )
            collected.sortedByDescending { it.addedAtEpochMillis }
        }
    }

    private suspend fun queryFolders(
        parentId: String,
        parentPath: String,
        token: String,
    ): List<DriveFolder> {
        val query = "'$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val response = driveListRequest(query = query, token = token)
        return response.optJSONArray("files").toJsonObjectList().map { item ->
            val name = item.getString("name")
            DriveFolder(
                id = item.getString("id"),
                name = name,
                path = "$parentPath/$name",
            )
        }.sortedBy { it.path.lowercase() }
    }

    private suspend fun querySongsRecursively(
        folderId: String,
        folderPath: String,
        accountEmail: String,
        token: String,
        onSongFound: suspend (Song) -> Unit,
    ) {
        val children = driveListRequest(
            query = "'$folderId' in parents and trashed = false",
            token = token,
        ).optJSONArray("files").toJsonObjectList()

        val subfolders = mutableListOf<Pair<String, String>>()

        children.forEach { item ->
            val mimeType = item.optString("mimeType")
            val id = item.getString("id")
            val name = item.getString("name")
            when {
                mimeType == "application/vnd.google-apps.folder" -> {
                    subfolders += id to "$folderPath/$name"
                }

                item.isSupportedAudioFile() -> {
                    val song = item.toSong(folderPath = folderPath, accountEmail = accountEmail, token = token)
                    onSongFound(song)
                }
            }
        }

        subfolders.forEach { (childId, childPath) ->
            querySongsRecursively(
                folderId = childId,
                folderPath = childPath,
                accountEmail = accountEmail,
                token = token,
                onSongFound = onSongFound,
            )
        }
    }

    private suspend fun getAccessToken(accountEmail: String): String {
        authSessionStore.tokenFor(accountEmail)?.let { return it }
        val token = GoogleAuthUtil.getToken(
            context,
            Account(accountEmail, "com.google"),
            "oauth2:https://www.googleapis.com/auth/drive.readonly",
        )
        authSessionStore.update(accountEmail, token)
        return token
    }

    private fun driveListRequest(query: String, token: String): JSONObject {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val encodedFields = URLEncoder.encode(
            "files(id,name,mimeType,modifiedTime,fileExtension,size)",
            Charsets.UTF_8.name(),
        )
        val url = URL("$DRIVE_FILES_ENDPOINT?q=$encodedQuery&pageSize=1000&fields=$encodedFields")
        return openJsonRequest(url, token)
    }

    private fun JSONObject.toSong(
        folderPath: String,
        accountEmail: String,
        token: String,
    ): Song {
        val id = getString("id")
        val name = getString("name")
        val streamUrl = "$DRIVE_FILES_ENDPOINT/$id?alt=media"
        val mimeType = optString("mimeType").ifBlank { guessMimeTypeFromName(name).orEmpty() }
        val embeddedMetadata = extractMetadata(
            songId = id,
            fileName = name,
            mimeType = mimeType,
            mediaUrl = streamUrl,
            token = token,
        )
        val titleFallback = name.substringBeforeLast(".")
        val duration = embeddedMetadata.durationMs ?: 0L
        return Song(
            id = "drive:$id",
            title = embeddedMetadata.title ?: titleFallback,
            artist = embeddedMetadata.artist ?: "Unknown Artist",
            album = embeddedMetadata.album ?: folderPath.substringAfterLast("/"),
            durationMs = duration,
            albumArtUri = embeddedMetadata.albumArtUri,
            sourceType = SourceType.DRIVE,
            playableUri = streamUrl,
            mimeType = mimeType.ifBlank { null },
            addedAtEpochMillis = isoToEpochMillis(optString("modifiedTime")),
            authAccountEmail = accountEmail,
        )
    }

    private fun extractMetadata(
        songId: String,
        fileName: String,
        mimeType: String,
        mediaUrl: String,
        token: String,
    ): EmbeddedMetadata {
        // FLAC: Android's MediaMetadataRetriever is unreliable over HTTP because the
        // native extractor doesn't always propagate the Authorization header on the
        // internal range requests it issues while walking FLAC metadata blocks. We use
        // a pure-Kotlin streaming parser that reads directly from the Drive response.
        val isFlac = mimeType.equals("audio/flac", true) ||
            mimeType.equals("audio/x-flac", true) ||
            fileName.endsWith(".flac", ignoreCase = true)
        if (isFlac) {
            streamFlacMetadata(songId, mediaUrl, token)?.let { return it }
        }

        // Everything else (MP3, M4A, OGG, …) still goes through MediaMetadataRetriever.
        // It works well for these formats, uses a single authenticated request, and
        // does NOT write anything to disk itself. We additionally pull `embeddedPicture`
        // so cover art is surfaced for MP3/ID3 files too.
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(mediaUrl, mapOf("Authorization" to "Bearer $token"))
                val pictureUri = runCatching { retriever.embeddedPicture }
                    .getOrNull()
                    ?.let { saveAlbumArt(songId, it) }
                EmbeddedMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                    albumArtUri = pictureUri,
                )
            }
        }.getOrDefault(EmbeddedMetadata())
    }

    private fun streamFlacMetadata(
        songId: String,
        mediaUrl: String,
        token: String,
    ): EmbeddedMetadata? {
        // Bound the read to the first few megabytes. Drive API supports HTTP range
        // requests, so this prevents the server from trying to stream the entire audio
        // payload just to satisfy the connection. 8 MiB is more than enough to cover
        // STREAMINFO + VORBIS_COMMENT + a generous embedded PICTURE block.
        val connection = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Range", "bytes=0-${FLAC_METADATA_BYTE_BUDGET - 1}")
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299 && code != 206) return null
            connection.inputStream.buffered().use { stream ->
                val parsed = FlacStreamMetadataParser.parse(stream) ?: return null
                val artUri = parsed.pictureBytes?.let { saveAlbumArt(songId, it) }
                EmbeddedMetadata(
                    title = parsed.title,
                    artist = parsed.artist,
                    album = parsed.album,
                    durationMs = parsed.durationMs,
                    albumArtUri = artUri,
                )
            }
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Persist the extracted album art to the app's cache directory and return a
     * `file://` URI that Coil can load. This is persistent image cache, NOT a
     * short-lived scratch file used by the metadata extractor — the bytes already
     * came from the in-memory streaming parser above.
     */
    private fun saveAlbumArt(songId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val dir = File(context.cacheDir, "drive_art").apply { mkdirs() }
            val safeName = songId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "$safeName.img")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    private fun openJsonRequest(url: URL, token: String): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        return try {
            val body = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(
                    buildString {
                        append("Drive API error ")
                        append(connection.responseCode)
                        if (errorBody.isNotBlank()) {
                            append(": ")
                            append(errorBody)
                        }
                    },
                )
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray?.toJsonObjectList(): List<JSONObject> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index) }
    }

    private fun JSONObject.isSupportedAudioFile(): Boolean {
        val mimeType = optString("mimeType")
        val name = optString("name")
        return mimeType.startsWith("audio/") || AUDIO_EXTENSIONS.any { name.endsWith(".$it", ignoreCase = true) }
    }

    private fun guessMimeTypeFromName(name: String): String? {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wma" -> "audio/x-ms-wma"
            "aiff", "aif" -> "audio/aiff"
            "alac" -> "audio/alac"
            else -> null
        }
    }

    private fun isoToEpochMillis(value: String): Long {
        return runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    }

    private data class EmbeddedMetadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val durationMs: Long? = null,
        val albumArtUri: String? = null,
    )

    private companion object {
        private const val ROOT_ID = "root"
        private const val DRIVE_FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val FLAC_METADATA_BYTE_BUDGET = 8L * 1024L * 1024L
        private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "aiff", "aif", "alac")
    }
}
