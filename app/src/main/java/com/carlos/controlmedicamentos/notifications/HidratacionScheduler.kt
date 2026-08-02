package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class HidratacionScheduler(private val context: Context) {

    companion object {
        const val ACTION_HIDRATACION_REMINDER = "com.carlos.controlmedicamentos.notifications.HIDRATACION_REMINDER"
        const val EXTRA_PATIENT_ID   = "HID_PATIENT_ID"
        const val EXTRA_PATIENT_NAME = "HID_PATIENT_NAME"
        private const val REQUEST_CODE_BASE = 700_000
        private const val PREFS_NAME        = "hidratacion_reminder"
        private const val KEY_ENABLED       = "enabled"
        private const val KEY_INTERVAL_H    = "interval_hours"
        private const val KEY_START_HOUR    = "start_hour"
        private const val KEY_END_HOUR      = "end_hour"
        private const val KEY_PATIENT_ID    = "patient_id"
        private const val KEY_PATIENT_NAME  = "patient_name"
        private const val KEY_SOUND_ENABLED = "sound_enabled"

        fun saveSettings(context: Context, enabled: Boolean, intervalHours: Int, startHour: Int, endHour: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putBoolean(KEY_ENABLED, enabled)
                putInt(KEY_INTERVAL_H, intervalHours)
                putInt(KEY_START_HOUR, startHour)
                putInt(KEY_END_HOUR, endHour)
                apply()
            }
        }

        fun savePatientInfo(context: Context, patientId: Int, patientName: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putInt(KEY_PATIENT_ID, patientId)
                putString(KEY_PATIENT_NAME, patientName.ifBlank { "Usuario" })
                apply()
            }
        }

        /** Triple(intervalHours, startHour, endHour, enabled) */
        fun loadSettings(context: Context): Settings {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return Settings(
                enabled       = p.getBoolean(KEY_ENABLED, false),
                intervalHours = p.getInt(KEY_INTERVAL_H, 2),
                startHour     = p.getInt(KEY_START_HOUR, 7),
                endHour       = p.getInt(KEY_END_HOUR, 22)
            )
        }

        fun loadPatientInfo(context: Context): Pair<Int, String> {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return Pair(p.getInt(KEY_PATIENT_ID, 0), p.getString(KEY_PATIENT_NAME, "Usuario").orEmpty())
        }

        fun saveSoundEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        }

        fun loadSoundEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true)
        }

        data class Settings(val enabled: Boolean, val intervalHours: Int, val startHour: Int, val endHour: Int)
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programar(patientId: Int, patientName: String) {
        savePatientInfo(context, patientId, patientName)
        cancelar(patientId)
        val s = loadSettings(context)
        if (!s.enabled || patientId <= 0) return
        val triggerAt = proximoDisparo(s)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HIDRATACION_REMINDER
            putExtra(EXTRA_PATIENT_ID, patientId)
            putExtra(EXTRA_PATIENT_NAME, patientName)
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE_BASE + patientId, intent,
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

    fun cancelar(patientId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_HIDRATACION_REMINDER }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE_BASE + patientId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) { alarmManager.cancel(pi); pi.cancel() }
    }

    private fun proximoDisparo(s: Settings): Long {
        val ahora = System.currentTimeMillis()
        val intervalMs = s.intervalHours * 60L * 60L * 1000L
        var candidato = ahora + intervalMs
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = candidato
        val hora = cal.get(java.util.Calendar.HOUR_OF_DAY)
        if (hora >= s.endHour) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, s.startHour)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            candidato = cal.timeInMillis
        } else if (hora < s.startHour) {
            cal.set(java.util.Calendar.HOUR_OF_DAY, s.startHour)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            candidato = cal.timeInMillis
        }
        return candidato
    }
}
