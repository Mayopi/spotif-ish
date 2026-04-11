package com.example.musicapp.player.controller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.musicapp.core.PlaybackTokenSource
import com.example.musicapp.domain.model.PlaybackQueue
import com.example.musicapp.domain.model.RepeatMode
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.player.PlaybackState
import com.example.musicapp.player.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Media3-backed playback controller.
 *
 * Auth plumbing for Drive bytes is gone — every Drive song's `playableUri` is now an
 * opaque backend stream URL (`/v1/songs/{id}/stream`) that the
 * [com.example.musicapp.data.network.AuthInterceptor] is supposed to authorize. Since
 * ExoPlayer creates its own HTTP client, we attach an HTTP header at the data-source
 * level via a [TokenSource] indirection that the app module wires up against the
 * real `SessionStore`. The player module itself stays free of any auth knowledge.
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenSource: PlaybackTokenSource,
) : PlaybackController {

    private val appContext = context
    private val state = MutableStateFlow(PlaybackState())
    private var currentQueue = emptyList<Song>()
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionTicker: Job? = null

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory().apply {
            // Auth header is set per-request below via setDefaultRequestProperties().
            // We refresh it on every play() so newly minted tokens propagate.
            setAllowCrossProtocolRedirects(true)
        }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(appContext)
                .setDataSourceFactory(
                    DefaultDataSource.Factory(appContext, httpDataSourceFactory),
                ),
        )
        .build()
        .also { it.addListener(playerListener()) }

    private fun playerListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishState()
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publishState()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publishState()
    }

    private fun startPositionTicker() {
        if (positionTicker?.isActive == true) return
        positionTicker = controllerScope.launch {
            while (true) {
                publishState()
                delay(500L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    override fun observeState() = state.asStateFlow()

    override suspend fun play(song: Song, queue: List<Song>) {
        currentQueue = queue
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        // Stamp the current bearer onto the HTTP factory before playback starts so
        // every range request to the backend's stream proxy is authorized. We don't
        // bother filtering by host here — the only HTTP source the player ever hits
        // in this app is the backend itself.
        val token = tokenSource.currentAccessToken()
        if (!token.isNullOrBlank()) {
            httpDataSourceFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer $token"),
            )
        }

        exoPlayer.setMediaItems(queue.map(::toMediaItem), startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        startPlaybackServiceIfNeeded()
        publishState()
    }

    override suspend fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        publishState()
    }

    override suspend fun skipNext() {
        exoPlayer.seekToNextMediaItem()
        publishState()
    }

    override suspend fun skipPrevious() {
        exoPlayer.seekToPreviousMediaItem()
        publishState()
    }

    override suspend fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        publishState()
    }

    fun player(): ExoPlayer = exoPlayer

    fun release() {
        stopPositionTicker()
        exoPlayer.release()
    }

    private fun startPlaybackServiceIfNeeded() {
        val intent = Intent(appContext, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun publishState() {
        val index = exoPlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: 0
        state.value = PlaybackState(
            currentSong = currentQueue.getOrNull(index),
            isPlaying = exoPlayer.isPlaying,
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.takeIf { it > 0 } ?: state.value.currentSong?.durationMs ?: 0L,
            queue = PlaybackQueue(
                items = currentQueue,
                currentIndex = index,
                shuffleEnabled = exoPlayer.shuffleModeEnabled,
                repeatMode = when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
            ),
        )
    }

    private fun toMediaItem(song: Song): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
        song.albumArtUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.playableUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
}
