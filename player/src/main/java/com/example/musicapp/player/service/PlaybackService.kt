package com.example.musicapp.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicapp.player.controller.Media3PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackController: Media3PlaybackController

    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressTicker: Job? = null
    private var lastNotificationId: Int? = null
    private var lastNotificationTemplate: Notification? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildBootstrapNotification())

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
        launchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, playbackController.player())
            .setSessionActivity(sessionActivity)
            .build()
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID,
        )
            .setMediaDescriptionAdapter(
                PlaybackDescriptionAdapter(sessionActivity),
            )
            .setNotificationListener(ServiceNotificationListener())
            .setSmallIconResourceId(android.R.drawable.ic_media_play)
            .build()
            .apply {
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setPlayer(playbackController.player())
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!playbackController.player().isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopProgressTicker()
        serviceScope.cancel()
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
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

    private fun buildBootstrapNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Spotifish")
            .setContentText("Preparing playback...")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    private inner class PlaybackDescriptionAdapter(
        private val sessionActivity: PendingIntent?,
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence =
            player.mediaMetadata.title ?: "Spotifish"

        override fun createCurrentContentIntent(player: Player): PendingIntent? = sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? =
            player.mediaMetadata.artist

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? = null
    }

    private inner class ServiceNotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean,
        ) {
            lastNotificationId = notificationId
            lastNotificationTemplate = notification
            val decoratedNotification = decorateWithPlaybackProgress(notification)
            if (ongoing) {
                startForeground(notificationId, decoratedNotification)
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(notificationId, decoratedNotification)
            }
            if (playbackController.player().isPlaying) {
                startProgressTickerIfNeeded()
            } else {
                stopProgressTicker()
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopProgressTicker()
            lastNotificationTemplate = null
            lastNotificationId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startProgressTickerIfNeeded() {
        if (progressTicker?.isActive == true) return
        progressTicker = serviceScope.launch {
            while (isActive) {
                val template = lastNotificationTemplate
                val notificationId = lastNotificationId
                if (template == null || notificationId == null) {
                    delay(PROGRESS_TICK_MS)
                    continue
                }
                val updatedNotification = decorateWithPlaybackProgress(template)
                if (playbackController.player().isPlaying) {
                    startForeground(notificationId, updatedNotification)
                } else {
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(notificationId, updatedNotification)
                    break
                }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTicker?.cancel()
        progressTicker = null
    }

    private fun decorateWithPlaybackProgress(notification: Notification): Notification {
        val durationMs = playbackController.player().duration
        val positionMs = playbackController.player().currentPosition
        if (durationMs <= 0L) return notification
        val safeMax = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val safeProgress = positionMs.coerceIn(0L, safeMax.toLong()).toInt()
        return Notification.Builder.recoverBuilder(this, notification)
            .setProgress(safeMax, safeProgress, false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "playback"
        private const val PROGRESS_TICK_MS = 1_000L
    }
}
