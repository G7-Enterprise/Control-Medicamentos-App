package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

class SedentarismoScheduler(private val context: Context) {

    companion object {
        const val ACTION_SEDENTARISMO_CHECK = "com.carlos.controlmedicamentos.notifications.SEDENTARISMO_CHECK"
        const val EXTRA_PATIENT_ID = "SED_PATIENT_ID"
        private const val RC_BASE = 900_000
        private const val PREFS_NAME = "sedentarismo_sound_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"

        fun saveSoundEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        }

        fun loadSoundEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true)
        }

        private val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    private val client: ActivityRecognitionClient = ActivityRecognition.getClient(context)

    fun programar(patientId: Int) {
        cancelarAlarmasLegacy(patientId)
        ActivityRecognitionReceiver.guardarPacienteActivo(context, patientId)
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ActivityRecognitionReceiver.ACTION
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)
        try {
            client.requestActivityUpdates(60_000L, pi)
        } catch (_: SecurityException) { }
    }

    fun cancelar(patientId: Int) {
        cancelarAlarmasLegacy(patientId)
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ActivityRecognitionReceiver.ACTION
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)
        try {
            client.removeActivityUpdates(pi)
        } catch (_: SecurityException) { }
        pi.cancel()
    }

    private fun cancelarAlarmasLegacy(patientId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, RC_BASE + patientId,
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SEDENTARISMO_CHECK },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }
}
