package com.narrativedj.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.narrativedj.app.MainActivity
import com.narrativedj.app.R

class MediaPlaybackService : android.app.Service() {

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        initMediaSession()
        PlaybackSessionState.metadataListener = { refreshForegroundNotification() }
        refreshForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        PlaybackSessionState.metadataListener = null
        releaseWakeLock()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        PlaybackSessionState.mediaSessionActive = false
        PlaybackSessionState.wakeLockHeld = false
        super.onDestroy()
    }

    private fun initMediaSession() {
        createChannel()
        mediaSession = MediaSessionCompat(this, SESSION_TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlaybackSessionState.transportHandler?.onPlay()
                    updatePlaybackState(true)
                }

                override fun onPause() {
                    PlaybackSessionState.transportHandler?.onPause()
                    updatePlaybackState(false)
                }
            })
            isActive = true
        }
        PlaybackSessionState.mediaSessionActive = true
        updatePlaybackState(PlaybackSessionState.isPlaying)
    }

    private fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE,
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    private fun refreshForegroundNotification() {
        startForeground(NOTIFICATION_ID, buildNotification())
        updatePlaybackState(PlaybackSessionState.isPlaying)
        val title = PlaybackSessionState.title
        val artist = PlaybackSessionState.artist
        if (!title.isNullOrBlank()) {
            mediaSession?.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: "")
                    .build(),
            )
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NarrativeDJ:MediaPlayback",
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
        PlaybackSessionState.wakeLockHeld = true
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        PlaybackSessionState.wakeLockHeld = false
    }

    private fun buildNotification(): android.app.Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val line = PlaybackMetadataFormatter.notificationLine(
            PlaybackSessionState.title,
            PlaybackSessionState.artist,
            getString(R.string.notification_playback_active),
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(line)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "narrativedj_media_playback"
        private const val NOTIFICATION_ID = 1
        private const val SESSION_TAG = "NarrativeDJSession"
        private const val WAKE_LOCK_TIMEOUT_MS = 6 * 60 * 60 * 1000L
    }
}
