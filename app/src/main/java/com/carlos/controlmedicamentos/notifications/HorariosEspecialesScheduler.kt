package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class HorariosEspecialesScheduler(private val context: Context) {

    companion object {
        const val ACTION_HORARIO_ESPECIAL = "com.carlos.controlmedicamentos.notifications.HORARIO_ESPECIAL"
        const val ACTION_REPROGRAMAR_HORARIOS = "com.carlos.controlmedicamentos.notifications.REPROGRAMAR_HORARIOS"
        const val EXTRA_PATIENT_ID = "HE_PATIENT_ID"
        const val EXTRA_NIVEL = "HE_NIVEL"
        private const val RC_BASE = 910_000
        private const val RC_REPROGRAM = 920_000

        private val updateFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        private val noCreateFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
    }

    fun programar(patientId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val config = AppDatabase.getDatabase(context).sedentarismoDao().obtenerConfig(patientId) ?: return@launch
            if (!config.activado) return@launch
            cancelar(patientId)

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, config.horaInicioMonitoreo)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startWindow = cal.timeInMillis
            val endWindow = startWindow + ((config.horaFinMonitoreo - config.horaInicioMonitoreo).coerceAtLeast(1)) * 60 * 60 * 1000L
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            var nivel = 1
            var hora = startWindow + 3 * 60 * 60 * 1000L
            while (hora < endWindow) {
                if (hora > now) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_HORARIO_ESPECIAL
                        putExtra(EXTRA_PATIENT_ID, patientId)
                        putExtra(EXTRA_NIVEL, nivel)
                    }
                    val pi = PendingIntent.getBroadcast(context, RC_BASE + patientId * 100 + nivel, intent, updateFlags)
                    scheduleExact(alarmManager, hora, pi)
                }
                nivel++
                hora += 3 * 60 * 60 * 1000L
            }

            // Reprogramar automáticamente al inicio de la siguiente jornada
            val nextStart = Calendar.getInstance().apply {
                timeInMillis = startWindow
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            val reproAt = maxOf(nextStart, now + 60_000L)
            val reproIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_REPROGRAMAR_HORARIOS
                putExtra(EXTRA_PATIENT_ID, patientId)
            }
            val reproPi = PendingIntent.getBroadcast(context, RC_REPROGRAM + patientId, reproIntent, updateFlags)
            scheduleExact(alarmManager, reproAt, reproPi)
        }
    }

    fun cancelar(patientId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (nivel in 1..20) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_HORARIO_ESPECIAL
                putExtra(EXTRA_PATIENT_ID, patientId)
                putExtra(EXTRA_NIVEL, nivel)
            }
            val pi = PendingIntent.getBroadcast(context, RC_BASE + patientId * 100 + nivel, intent, noCreateFlags)
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        val reproIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REPROGRAMAR_HORARIOS
            putExtra(EXTRA_PATIENT_ID, patientId)
        }
        val reproPi = PendingIntent.getBroadcast(context, RC_REPROGRAM + patientId, reproIntent, noCreateFlags)
        if (reproPi != null) {
            alarmManager.cancel(reproPi)
            reproPi.cancel()
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
