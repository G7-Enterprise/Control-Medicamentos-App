package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.MetodoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.TipoAnticonceptivo
import java.util.Calendar

class AnticonceptivoScheduler(private val context: Context) {

    companion object {
        const val EXTRA_CONTRACEPTIVE_ID = "ANTICONCEPTIVO_ID"
        const val EXTRA_ALERT_TITLE = "ALARM_TITLE"
        const val EXTRA_ALERT_MESSAGE = "ALARM_MESSAGE"
        const val EXTRA_IS_SNOOZE_ALARM = "IS_SNOOZE_ALARM"
        private const val REQUEST_CODE_BASE = 190_000
        private const val SNOOZE_MINUTES = 10
    }

    fun programarAlarma(metodo: MetodoAnticonceptivo) {
        cancelar(metodo.id)
        val tipo = TipoAnticonceptivo.fromDisplayName(metodo.tipo)
        if (!tipo.requiereAlarmaDiaria || !metodo.activo) return

        val partes = metodo.horaToma.split(":").map { it.toIntOrNull() ?: 0 }
        val hora = partes.getOrElse(0) { 8 }
        val minuto = partes.getOrElse(1) { 0 }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val pendingIntent = buildPendingIntent(metodo.id)
        scheduleExact(alarmManager, triggerAt, pendingIntent)
    }

    fun cancelar(metodoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelarPendingIntent(alarmManager, buildPendingIntent(metodoId))
        cancelarPendingIntent(alarmManager, buildSnoozePendingIntent(metodoId))
    }

    fun programarSnooze(metodo: MetodoAnticonceptivo, delayMinutes: Int = SNOOZE_MINUTES) {
        if (!metodo.activo) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelarPendingIntent(alarmManager, buildSnoozePendingIntent(metodo.id))

        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        scheduleExact(alarmManager, triggerAt, buildSnoozePendingIntent(metodo.id))
    }

    private fun buildIntent(metodo: MetodoAnticonceptivo): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_CONTRACEPTIVE_ID, metodo.id)
            putExtra(EXTRA_ALERT_TITLE, "💊 Anticonceptivo")
            putExtra(EXTRA_ALERT_MESSAGE, "Hora de tomar tu ${metodo.tipo}")
        }
    }

    private fun buildPendingIntent(metodoId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_CONTRACEPTIVE_ID, metodoId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + metodoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePendingIntent(metodoId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_CONTRACEPTIVE_ID, metodoId)
            putExtra(EXTRA_IS_SNOOZE_ALARM, true)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + metodoId + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            else ->
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelarPendingIntent(alarmManager: AlarmManager, pendingIntent: PendingIntent) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
