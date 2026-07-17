package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class SignosVitalesScheduler(private val context: Context) {

    companion object {
        const val ACTION_SIGNOS_VITALES_REMINDER = "com.carlos.controlmedicamentos.notifications.SIGNOS_VITALES_REMINDER"
        const val EXTRA_PATIENT_ID = "SV_PATIENT_ID"
        const val EXTRA_PATIENT_NAME = "SV_PATIENT_NAME"
        private const val REQUEST_CODE_BASE = 600_000
        private const val PREFS_NAME = "signos_vitales_reminder"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PATIENT_ID = "patient_id"
        private const val KEY_PATIENT_NAME = "patient_name"

        fun saveSettings(context: Context, hour: Int, minute: Int, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putInt(KEY_HOUR, hour)
                putInt(KEY_MINUTE, minute)
                putBoolean(KEY_ENABLED, enabled)
                apply()
            }
        }

        fun savePatientInfo(context: Context, patientId: Int, patientName: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putInt(KEY_PATIENT_ID, patientId)
                putString(KEY_PATIENT_NAME, patientName.ifBlank { "Paciente" })
                apply()
            }
        }

        fun loadSettings(context: Context): Triple<Int, Int, Boolean> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return Triple(
                prefs.getInt(KEY_HOUR, 8),
                prefs.getInt(KEY_MINUTE, 0),
                prefs.getBoolean(KEY_ENABLED, false)
            )
        }

        fun loadPatientInfo(context: Context): Pair<Int, String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return Pair(
                prefs.getInt(KEY_PATIENT_ID, 0),
                prefs.getString(KEY_PATIENT_NAME, "Paciente").orEmpty()
            )
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programar(patientId: Int, patientName: String) {
        savePatientInfo(context, patientId, patientName)
        cancelar(patientId)
        val (hour, minute, enabled) = loadSettings(context)
        if (!enabled) return
        if (patientId <= 0) return

        val triggerAt = calcularProximoDisparo(hour, minute)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SIGNOS_VITALES_REMINDER
            putExtra(EXTRA_PATIENT_ID, patientId)
            putExtra(EXTRA_PATIENT_NAME, patientName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(patientId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelar(patientId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SIGNOS_VITALES_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(patientId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calcularProximoDisparo(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    private fun buildRequestCode(patientId: Int): Int = REQUEST_CODE_BASE + patientId
}
