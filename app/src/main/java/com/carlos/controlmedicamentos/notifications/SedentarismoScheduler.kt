package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.carlos.controlmedicamentos.data.local.AppDatabase

class SedentarismoScheduler(private val context: Context) {

    companion object {
        const val ACTION_SEDENTARISMO_CHECK = "com.carlos.controlmedicamentos.notifications.SEDENTARISMO_CHECK"
        const val EXTRA_PATIENT_ID = "SED_PATIENT_ID"
        private const val RC_BASE = 900_000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programar(patientId: Int) {
        cancelar(patientId)
        CoroutineScope(Dispatchers.IO).launch {
            val config = AppDatabase.getDatabase(context).sedentarismoDao().obtenerConfig(patientId) ?: return@launch
            if (!config.activado) return@launch
            val intervalMs = config.limiteInactividadMinutos * 60_000L
            val triggerAt = System.currentTimeMillis() + intervalMs
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SEDENTARISMO_CHECK
                putExtra(EXTRA_PATIENT_ID, patientId)
            }
            val pi = PendingIntent.getBroadcast(
                context, RC_BASE + patientId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    fun cancelar(patientId: Int) {
        val pi = PendingIntent.getBroadcast(
            context, RC_BASE + patientId,
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SEDENTARISMO_CHECK },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) { alarmManager.cancel(pi); pi.cancel() }
    }
}
