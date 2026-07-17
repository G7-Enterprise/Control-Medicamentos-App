package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.VisitaDentista

class DentistaScheduler(private val context: Context) {

    companion object {
        const val ACTION_DENTISTA_CITA_24H    = "com.carlos.controlmedicamentos.notifications.DENTISTA_CITA_24H"
        const val ACTION_DENTISTA_CITA_2H     = "com.carlos.controlmedicamentos.notifications.DENTISTA_CITA_2H"
        const val ACTION_DENTISTA_SEGUIMIENTO = "com.carlos.controlmedicamentos.notifications.DENTISTA_SEGUIMIENTO"
        const val ACTION_DENTISTA_REVISION    = "com.carlos.controlmedicamentos.notifications.DENTISTA_REVISION"
        const val EXTRA_VISITA_ID             = "DENTISTA_VISITA_ID"
        const val EXTRA_PATIENT_ID            = "DENTISTA_PATIENT_ID"
        const val EXTRA_MOTIVO                = "DENTISTA_MOTIVO"
        private const val RC_24H_BASE         = 800_000
        private const val RC_2H_BASE          = 801_000
        private const val RC_SEG_BASE         = 802_000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programarCita(visita: VisitaDentista) {
        cancelarCita(visita.id)
        val ahora = System.currentTimeMillis()

        if (visita.recordatorio24h) {
            val trigger = visita.fechaHora - 24L * 3600_000L
            if (trigger > ahora) programar(ACTION_DENTISTA_CITA_24H, RC_24H_BASE + visita.id, trigger, visita)
        }
        if (visita.recordatorio2h) {
            val trigger = visita.fechaHora - 2L * 3600_000L
            if (trigger > ahora) programar(ACTION_DENTISTA_CITA_2H, RC_2H_BASE + visita.id, trigger, visita)
        }
        if (visita.seguimientoPostConsulta) {
            val trigger = visita.fechaHora + 24L * 3600_000L
            programar(ACTION_DENTISTA_SEGUIMIENTO, RC_SEG_BASE + visita.id, trigger, visita)
        }
    }

    fun cancelarCita(visitaId: Int) {
        listOf(
            ACTION_DENTISTA_CITA_24H to RC_24H_BASE + visitaId,
            ACTION_DENTISTA_CITA_2H  to RC_2H_BASE  + visitaId,
            ACTION_DENTISTA_SEGUIMIENTO to RC_SEG_BASE + visitaId
        ).forEach { (action, rc) ->
            val pi = PendingIntent.getBroadcast(
                context, rc,
                Intent(context, AlarmReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) { alarmManager.cancel(pi); pi.cancel() }
        }
    }

    private fun programar(action: String, requestCode: Int, triggerAt: Long, visita: VisitaDentista) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_VISITA_ID, visita.id)
            putExtra(EXTRA_PATIENT_ID, visita.patientId)
            putExtra(EXTRA_MOTIVO, visita.motivo)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
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
