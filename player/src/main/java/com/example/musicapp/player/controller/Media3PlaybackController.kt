package com.example.musicapp.player.controller

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.domain.model.PlaybackQueue
import com.example.musicapp.domain.model.RepeatMode
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.player.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class Media3PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
) : PlaybackController {

    private val exoPlayer = ExoPlayer.Builder(context).build()
    private val state = MutableStateFlow(PlaybackState())
    private var currentQueue = emptyList<Song>()

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) = publishState()

                override fun onPlaybackStateChanged(playbackState: Int) = publishState()

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publishState()
            },
        )
    }

    override fun observeState() = state.asStateFlow()

    override suspend fun play(song: Song, queue: List<Song>) {
        currentQueue = queue
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
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
}
