package com.example.musicapp.player.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicapp.player.controller.Media3PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackController: Media3PlaybackController

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, playbackController.player()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        playbackController.release()
        super.onDestroy()
    }
}
