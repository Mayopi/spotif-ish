package com.example.musicapp.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
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
        startInForeground()
        mediaSession = MediaSession.Builder(this, playbackController.player()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun startInForeground() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Spotifish")
            .setContentText("Playing music")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        val existing = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Spotifish playback controls"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "playback"
    }
}
