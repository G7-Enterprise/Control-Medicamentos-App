package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.VaccinationRecord

class VaccinationScheduler(private val context: Context) {

    companion object {
        const val EXTRA_VACCINATION_ID = "VACCINATION_ID"
        const val EXTRA_VACCINE_NAME = "VACCINE_NAME"
        const val EXTRA_NEXT_DOSE_AT = "NEXT_DOSE_AT"
        private const val REQUEST_CODE_BASE = 800_000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programar(record: VaccinationRecord) {
        cancelar(record.id)
        if (!record.alarmEnabled || record.nextDoseAt == null || record.nextDoseAt <= System.currentTimeMillis()) {
            return
        }

        val triggerAt = record.nextDoseAt.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_VACCINATION_ID, record.id)
            putExtra(EXTRA_VACCINE_NAME, record.vaccineName)
            putExtra(EXTRA_NEXT_DOSE_AT, record.nextDoseAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(record.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelar(vaccinationId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(vaccinationId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildRequestCode(vaccinationId: Int): Int = REQUEST_CODE_BASE + vaccinationId
}
