package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SedentarismoScheduler(private val context: Context) {

    companion object {
        const val ACTION_SEDENTARISMO_CHECK = "com.carlos.controlmedicamentos.notifications.SEDENTARISMO_CHECK"
        const val EXTRA_PATIENT_ID = "SED_PATIENT_ID"
        const val ACTION_NATIVE_SEDENTARISMO_CHECK = "com.carlos.controlmedicamentos.notifications.NATIVE_SEDENTARISMO_CHECK"
        const val EXTRA_NATIVE_PATIENT_ID = "NATIVE_SED_PATIENT_ID"
        const val EXTRA_NATIVE_START_TIME = "NATIVE_SED_START_TIME"
        const val EXTRA_NATIVE_END_TIME = "NATIVE_SED_END_TIME"
        private const val RC_BASE = 900_000
        private const val RC_NATIVE = 930_000
        private const val NATIVE_CHECK_INTERVAL_MS = 15L * 60L * 1000L
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

        private val alarmUpdateFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        private val alarmNoCreateFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
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

        // Respaldo nativo: AlarmManager + contador de pasos del hardware
        CoroutineScope(Dispatchers.IO).launch {
            programarNativeCheck(patientId)
        }
    }

    fun cancelar(patientId: Int) {
        cancelarAlarmasLegacy(patientId)
        cancelarNativeCheck(patientId)
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ActivityRecognitionReceiver.ACTION
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)
        try {
            client.removeActivityUpdates(pi)
        } catch (_: SecurityException) { }
        pi.cancel()
    }

    /**
     * Programa la siguiente comprobación nativa dentro de la ventana de monitoreo.
     * Si la ventana actual ya terminó, programa el inicio de la siguiente jornada.
     */
    internal suspend fun programarNativeCheck(patientId: Int) {
        val db = AppDatabase.getDatabase(context)
        val config = db.sedentarismoDao().obtenerConfig(patientId) ?: return
        if (!config.activado) return

        cancelarNativeCheck(patientId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        fun computeWindow(baseDay: Long): Pair<Long, Long> {
            val start = Calendar.getInstance().apply {
                timeInMillis = baseDay
                set(Calendar.HOUR_OF_DAY, config.horaInicioMonitoreo)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + ((config.horaFinMonitoreo - config.horaInicioMonitoreo).coerceAtLeast(1)) * 60L * 60L * 1000L
            return start to end
        }

        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val (currentStart, currentEnd) = computeWindow(today)
        val (nextStart, nextEnd) = computeWindow(today + 24L * 60L * 60L * 1000L)

        val (startWindow, endWindow) = if (now in currentStart..currentEnd) {
            currentStart to currentEnd
        } else {
            nextStart to nextEnd
        }

        val nextCheck = if (now < startWindow) {
            startWindow + NATIVE_CHECK_INTERVAL_MS
        } else {
            (now + NATIVE_CHECK_INTERVAL_MS).coerceAtMost(endWindow - 1)
        }

        if (nextCheck >= endWindow) {
            // La ventana actual ya terminó o termina antes de la siguiente comprobación.
            // Reprogramar al inicio del día siguiente.
            val reproAt = nextStart + 60_000L
            val reproIntent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
                action = ACTION_NATIVE_SEDENTARISMO_CHECK
                putExtra(EXTRA_NATIVE_PATIENT_ID, patientId)
                putExtra(EXTRA_NATIVE_START_TIME, nextStart)
                putExtra(EXTRA_NATIVE_END_TIME, nextEnd)
            }
            val reproPi = PendingIntent.getBroadcast(context, RC_NATIVE + patientId, reproIntent, alarmUpdateFlags)
            scheduleExact(alarmManager, reproAt, reproPi)
            return
        }

        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ACTION_NATIVE_SEDENTARISMO_CHECK
            putExtra(EXTRA_NATIVE_PATIENT_ID, patientId)
            putExtra(EXTRA_NATIVE_START_TIME, startWindow)
            putExtra(EXTRA_NATIVE_END_TIME, endWindow)
        }
        val pi = PendingIntent.getBroadcast(context, RC_NATIVE + patientId, intent, alarmUpdateFlags)
        scheduleExact(alarmManager, nextCheck, pi)
    }

    private fun cancelarNativeCheck(patientId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ACTION_NATIVE_SEDENTARISMO_CHECK
        }
        val pi = PendingIntent.getBroadcast(context, RC_NATIVE + patientId, intent, alarmNoCreateFlags)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun cancelarAlarmasLegacy(patientId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, RC_BASE + patientId,
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SEDENTARISMO_CHECK },
            alarmNoCreateFlags
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun scheduleExact(alarmManager: AlarmManager, time: Long, pi: PendingIntent) {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi)
            }
        } catch (_: SecurityException) { }
    }
}
