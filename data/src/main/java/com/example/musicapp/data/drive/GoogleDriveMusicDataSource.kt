package com.example.musicapp.data.drive

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.musicapp.core.DriveAuthSessionStore
import com.example.musicapp.data.repository.DriveTokenStore
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SourceType
import com.example.musicapp.domain.repository.SettingsRepository
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
    private val driveTokenStore: DriveTokenStore,
) : DriveMusicDataSource {

    override suspend fun listFolders(parentId: String, parentPath: String): List<DriveFolder> {
        return withContext(Dispatchers.IO) {
            val settings = settingsRepository.observeSettings().first()
            val accountEmail = settings.connectedDriveFolder?.accountEmail ?: return@withContext emptyList()
            queryFoldersAuth(parentId = parentId, parentPath = parentPath, accountEmail = accountEmail)
        }
    }

    override suspend fun fetchSongs(
        previousById: Map<String, Song>,
        onSongDiscovered: (suspend (Song) -> Unit)?,
        onProgress: ((processedFileCount: Int) -> Unit)?,
    ): List<Song> {
        return withContext(Dispatchers.IO) {
            val settings = settingsRepository.observeSettings().first()
            val connection = settings.connectedDriveFolder ?: return@withContext emptyList()
            val accountEmail = connection.accountEmail ?: return@withContext emptyList()
            val collected = mutableListOf<Song>()
            var processedFileCount = 0
            querySongsRecursively(
                folderId = connection.folderId.ifBlank { ROOT_ID },
                folderPath = connection.folderName.ifBlank { "My Drive" },
                accountEmail = accountEmail,
                previousById = previousById,
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

    private suspend fun queryFoldersAuth(
        parentId: String,
        parentPath: String,
        accountEmail: String,
    ): List<DriveFolder> {
        val query = "'$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val response = executeAuthenticated(accountEmail) { token ->
            driveListRequest(query, token)
        }
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
        previousById: Map<String, Song>,
        onSongFound: suspend (Song) -> Unit,
    ) {
        val response = executeAuthenticated(accountEmail) { token ->
            driveListRequest("'$folderId' in parents and trashed = false", token)
        }
        val children = response.optJSONArray("files").toJsonObjectList()

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
                    val songId = "drive:$id"
                    val modifiedTime = isoToEpochMillis(item.optString("modifiedTime"))
                    val cached = previousById[songId]
                    val song = if (cached != null && cached.addedAtEpochMillis == modifiedTime) {
                        // Re-sync optimization: the file hasn't changed since the last
                        // successful scan, so reuse the previously parsed metadata
                        // (and album art URI) without downloading anything new.
                        cached
                    } else {
                        buildSong(item, folderPath, accountEmail)
                    }
                    onSongFound(song)
                }
            }
        }

        subfolders.forEach { (childId, childPath) ->
            querySongsRecursively(
                folderId = childId,
                folderPath = childPath,
                accountEmail = accountEmail,
                previousById = previousById,
                onSongFound = onSongFound,
            )
        }
    }

    private suspend fun buildSong(
        item: JSONObject,
        folderPath: String,
        accountEmail: String,
    ): Song {
        val id = item.getString("id")
        val name = item.getString("name")
        val fileSize = item.optString("size").toLongOrNull() ?: 0L
        val streamUrl = "$DRIVE_FILES_ENDPOINT/$id?alt=media"
        val mimeType = item.optString("mimeType").ifBlank { guessMimeTypeFromName(name).orEmpty() }
        val embeddedMetadata = extractMetadataAuth(
            songId = id,
            mediaUrl = streamUrl,
            fileSize = fileSize,
            accountEmail = accountEmail,
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
            addedAtEpochMillis = isoToEpochMillis(item.optString("modifiedTime")),
            authAccountEmail = accountEmail,
        )
    }

    /**
     * Option A metadata extraction: download a bounded prefix of the audio file to
     * a scratch file in the cache directory and run [MediaMetadataRetriever] against
     * the local path. Running the retriever locally avoids the Authorization-header
     * propagation quirks that make remote extraction unreliable for FLAC and for
     * embedded picture data.
     *
     * The scratch file is deleted in `finally` so it lives only for the lifetime of
     * one call. Album art bytes are persisted separately under `cacheDir/drive_art`
     * for Coil to load.
     */
    private suspend fun extractMetadataAuth(
        songId: String,
        mediaUrl: String,
        fileSize: Long,
        accountEmail: String,
    ): EmbeddedMetadata {
        val tempDir = File(context.cacheDir, "drive_meta").apply { mkdirs() }
        val tempFile = File(tempDir, "${sanitize(songId)}.bin")
        val prefixSize = when {
            fileSize <= 0L -> METADATA_PREFIX_SIZE
            fileSize < METADATA_PREFIX_SIZE -> fileSize
            else -> METADATA_PREFIX_SIZE
        }
        return try {
            executeAuthenticated(accountEmail) { token ->
                downloadPrefix(mediaUrl, token, prefixSize, tempFile)
            }
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(tempFile.absolutePath)
                val pictureBytes = runCatching { retriever.embeddedPicture }.getOrNull()
                val pictureUri = pictureBytes?.let { saveAlbumArt(songId, it) }
                EmbeddedMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                    albumArtUri = pictureUri,
                )
            }
        } catch (_: Throwable) {
            EmbeddedMetadata()
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun downloadPrefix(
        mediaUrl: String,
        token: String,
        sizeBytes: Long,
        dest: File,
    ) {
        val connection = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            if (sizeBytes > 0) {
                setRequestProperty("Range", "bytes=0-${sizeBytes - 1}")
            }
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            val code = connection.responseCode
            if (code == 401) throw HttpUnauthorizedException("Drive prefix download unauthorized")
            if (code !in 200..299 && code != 206) {
                throw IOException("Drive prefix download HTTP $code")
            }
            connection.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Persist extracted album art to a stable cache file so Coil can load it via a
     * `file://` URI from any future composition. This is the *output* of the
     * extractor — not a scratch file used during extraction.
     */
    private fun saveAlbumArt(songId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val dir = File(context.cacheDir, "drive_art").apply { mkdirs() }
            val file = File(dir, "${sanitize(songId)}.img")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    /**
     * Wraps an authenticated Drive call with one-shot 401 retry. If the cached token
     * has expired, [HttpUnauthorizedException] from the inner [block] triggers a
     * forced refresh via [refreshAccessToken] and a single retry. A second 401 is
     * propagated so the caller can surface the failure.
     */
    private suspend fun <T> executeAuthenticated(
        accountEmail: String,
        block: (String) -> T,
    ): T {
        val token = getAccessToken(accountEmail)
        return try {
            block(token)
        } catch (_: HttpUnauthorizedException) {
            val refreshed = refreshAccessToken(accountEmail)
            block(refreshed)
        }
    }

    private suspend fun getAccessToken(accountEmail: String): String {
        // 1. In-memory cache hit?
        authSessionStore.tokenFor(accountEmail)?.let { return it }

        // 2. Persisted from a previous run? The Drive token stays in encrypted prefs
        //    so it survives process death. Hydrate the in-memory store from it so
        //    the rest of the sync flow doesn't have to know it came from disk.
        driveTokenStore.load()
            ?.takeIf { it.accountEmail == accountEmail }
            ?.let { persisted ->
                authSessionStore.update(persisted.accountEmail, persisted.accessToken)
                return persisted.accessToken
            }

        // 3. Nothing cached anywhere — request a fresh token via silent re-auth.
        //    If the user previously granted Drive scope this completes without UI;
        //    otherwise [fetchToken] throws and the caller surfaces the failure.
        return fetchToken(accountEmail)
    }

    /**
     * Force-refresh the access token after a 401. Both caches are overwritten by
     * [fetchToken] on success, so a stale value never lingers past a successful
     * refresh.
     */
    private suspend fun refreshAccessToken(accountEmail: String): String {
        return fetchToken(accountEmail)
    }

    /**
     * Silent token fetch via Google Identity Services.
     *
     * The original sign-in went through Credential Manager + `AuthorizationClient`,
     * which means the granted Drive scope is owned by Identity Services — NOT by the
     * legacy `GoogleAuthUtil` AccountManager flow. Calling `AuthorizationClient` from
     * the application context returns a fresh access token with no UI as long as the
     * user previously granted the scope. If consent has been revoked,
     * `hasResolution() == true` and we surface an error so the user can reconnect
     * from Settings.
     */
    private suspend fun fetchToken(accountEmail: String): String {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(Scope("https://www.googleapis.com/auth/drive.readonly")),
            )
            .build()

        val result = withContext(Dispatchers.IO) {
            Tasks.await(Identity.getAuthorizationClient(context).authorize(request))
        }

        if (result.hasResolution()) {
            throw IllegalStateException(
                "Google Drive needs to be re-authorized. Please reconnect from Settings.",
            )
        }
        val token = result.accessToken
            ?: throw IllegalStateException("Google Drive returned no access token.")

        authSessionStore.update(accountEmail, token)
        driveTokenStore.save(accountEmail, token)
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

    private fun openJsonRequest(url: URL, token: String): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        return try {
            val code = connection.responseCode
            if (code == 401) throw HttpUnauthorizedException("Drive API unauthorized")
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(
                    buildString {
                        append("Drive API error ")
                        append(code)
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

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private data class EmbeddedMetadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val durationMs: Long? = null,
        val albumArtUri: String? = null,
    )

    private class HttpUnauthorizedException(message: String) : IOException(message)

    private companion object {
        private const val ROOT_ID = "root"
        private const val DRIVE_FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val METADATA_PREFIX_SIZE = 4L * 1024L * 1024L
        private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "aiff", "aif", "alac")
    }
}
