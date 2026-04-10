package com.example.musicapp.player.controller

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.core.DriveAuthSessionStore
import com.example.musicapp.domain.model.PlaybackQueue
import com.example.musicapp.domain.model.RepeatMode
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.player.PlaybackState
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

@Singleton
class Media3PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val driveAuthSessionStore: DriveAuthSessionStore,
) : PlaybackController {

    private val appContext = context
    private var exoPlayer = ExoPlayer.Builder(context).build()
    private val state = MutableStateFlow(PlaybackState())
    private var currentQueue = emptyList<Song>()
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionTicker: Job? = null
    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publishState()
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) = publishState()

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publishState()
        }

    init {
        exoPlayer.addListener(playerListener)
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
        configureDataSource(queue)
        exoPlayer.setMediaItems(queue.map(::toMediaItem), startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.playableUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build(),
            )
            .build()
    }

    private suspend fun configureDataSource(queue: List<Song>) {
        val driveSong = queue.firstOrNull { it.authAccountEmail != null }
        val token = driveAuthSessionStore.tokenFor(driveSong?.authAccountEmail)
        val mediaSourceFactory = if (token != null) {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            DefaultMediaSourceFactory(DefaultDataSource.Factory(appContext, httpFactory))
        } else {
            DefaultMediaSourceFactory(appContext)
        }
        stopPositionTicker()
        val currentPlayer = exoPlayer
        currentPlayer.removeListener(playerListener)
        currentPlayer.release()
        exoPlayer = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().also { it.addListener(playerListener) }
    }
}
