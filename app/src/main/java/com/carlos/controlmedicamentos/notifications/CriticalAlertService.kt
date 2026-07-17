package com.carlos.controlmedicamentos.notifications

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.carlos.controlmedicamentos.notifications.NotificacionHelper.CRITICAL_PLAYBACK_NOTIFICATION_ID

class CriticalAlertService : Service() {

    companion object {
        private const val ACTION_START = "com.carlos.controlmedicamentos.notifications.START_CRITICAL_ALERT"
        private const val ACTION_STOP = "com.carlos.controlmedicamentos.notifications.STOP_CRITICAL_ALERT"
        private const val EXTRA_SOUND_URI = "EXTRA_SOUND_URI"
        private const val ALERT_PLAY_COUNT = 2
        private const val ALERT_PLAY_DURATION_MS = 4_500L
        private const val ALERT_GAP_MS = 650L

        fun start(context: Context, soundUri: String) {
            val intent = Intent(context, CriticalAlertService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CriticalAlertService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousInterruptionFilter: Int? = null
    private var playCounter = 0

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                startForeground(
                    CRITICAL_PLAYBACK_NOTIFICATION_ID,
                    NotificacionHelper.buildPlaybackNotification(this)
                )
                beginCriticalPlayback(intent.getStringExtra(EXTRA_SOUND_URI).orEmpty())
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun beginCriticalPlayback(soundUri: String) {
        stopPlayback()
        requestAudioFocus()
        elevateInterruptionFilterIfPossible()
        playCounter = 0
        playSoundTwice(soundUri)
    }

    private fun playSoundTwice(soundUri: String) {
        if (playCounter >= ALERT_PLAY_COUNT) {
            handler.postDelayed({
                stopPlayback()
                stopSelf()
            }, ALERT_GAP_MS)
            return
        }

        playCounter += 1
        ringtone = buildRingtone(soundUri)?.also { tone ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tone.isLooping = false
            }
            tone.play()
        }

        handler.postDelayed({
            ringtone?.stop()
            ringtone = null
            handler.postDelayed({ playSoundTwice(soundUri) }, ALERT_GAP_MS)
        }, ALERT_PLAY_DURATION_MS)
    }

    private fun buildRingtone(soundUri: String): Ringtone? {
        val resolvedUri = NotificacionHelper.resolveCriticalSoundUri(soundUri)
        return RingtoneManager.getRingtone(this, resolvedUri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(manager::abandonAudioFocusRequest)
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun elevateInterruptionFilterIfPossible() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && manager.isNotificationPolicyAccessGranted) {
            if (previousInterruptionFilter == null) {
                previousInterruptionFilter = manager.currentInterruptionFilter
            }
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    private fun restoreInterruptionFilterIfNeeded() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && manager.isNotificationPolicyAccessGranted) {
            previousInterruptionFilter?.let(manager::setInterruptionFilter)
        }
        previousInterruptionFilter = null
    }

    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(null)
        ringtone?.stop()
        ringtone = null
        abandonAudioFocus()
        restoreInterruptionFilterIfNeeded()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}