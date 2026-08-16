package com.carlos.controlmedicamentos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.carlos.controlmedicamentos.MainActivity
import com.carlos.controlmedicamentos.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Recibe campaÃ±as FCM y las muestra como notificaciones locales seguras. */
class CampaignFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        CampaignNotifications.ensureChannel(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRepository.sync(token, applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data[KEY_TITLE] ?: DEFAULT_TITLE
        val body = message.notification?.body ?: data[KEY_BODY] ?: DEFAULT_BODY
        CampaignNotifications.show(
            context = this,
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            url = data[KEY_URL]
        )
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "FCM descartÃ³ mensajes pendientes; la campaÃ±a debe sincronizarse nuevamente si es necesaria.")
    }

    private companion object {
        private const val TAG = "CampaignFCMService"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_URL = "url_actualizacion"
        private const val DEFAULT_TITLE = "Control de Medicamentos"
        private const val DEFAULT_BODY = "Tienes una novedad disponible."
    }
}

object CampaignNotifications {
    const val CHANNEL_ID = "campaign_updates"
    private const val CHANNEL_NAME = "Actualizaciones y novedades"
    private const val CHANNEL_DESCRIPTION = "Actualizaciones de la aplicaciÃ³n y avisos importantes."

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = CHANNEL_DESCRIPTION }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, notificationId: Int, title: String, body: String, url: String?) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val targetIntent = url?.takeIf(::isHttpUrl)?.let { safeUrl ->
            Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
        } ?: Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_campaign)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun isHttpUrl(value: String): Boolean =
        runCatching {
            Uri.parse(value).scheme?.lowercase() in setOf("https", "http")
        }.getOrDefault(false)
}

object FcmTokenRepository {
    private const val TAG = "FcmTokenRepository"
    private const val COLLECTION = "dispositivos_fcm"

    /** Obtiene el token actual al arrancar, incluso si onNewToken ya se ejecutÃ³ antes. */
    suspend fun syncCurrentToken(context: Context) {
        runCatching { FirebaseMessaging.getInstance().token.await() }
            .onSuccess { sync(it, context.applicationContext) }
            .onFailure { Log.w(TAG, "No se pudo obtener el token FCM", it) }
    }

    fun sync(token: String, context: Context) {
        if (token.isBlank()) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (deviceId.isNullOrBlank()) {
                Log.w(TAG, "No se pudo asociar el token FCM: ANDROID_ID no disponible")
                return@launch
            }

            try {
                FirebaseFirestore.getInstance().collection(COLLECTION).document(deviceId).set(
                    mapOf(
                        "dispositivo_id" to deviceId,
                        "fcm_token" to token,
                        "fcm_token_actualizado_en" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (exception: Exception) {
                Log.w(TAG, "No se pudo sincronizar el token FCM", exception)
            }
        }
    }
}
