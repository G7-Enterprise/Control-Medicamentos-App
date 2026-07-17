package com.carlos.controlmedicamentos.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val WORK_NAME = "controlmedicamentos_auto_backup"
    private const val PREFS_NAME = "backup_preferences"
    private const val KEY_FREQUENCY = "backup_frequency"
    private const val KEY_HOUR = "backup_hour"
    private const val KEY_MINUTE = "backup_minute"

    const val FREQUENCY_MANUAL = "manual"
    const val FREQUENCY_DAILY = "daily"
    const val FREQUENCY_WEEKLY = "weekly"

    fun saveFrequency(context: Context, frequency: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FREQUENCY, frequency)
            .apply()
    }

    fun getFrequency(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FREQUENCY, FREQUENCY_MANUAL)
            ?: FREQUENCY_MANUAL
    }

    fun saveTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun getTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt(KEY_HOUR, 2), prefs.getInt(KEY_MINUTE, 0))
    }

    fun applySchedule(context: Context, frequency: String, hour: Int, minute: Int) {
        saveFrequency(context, frequency)
        saveTime(context, hour, minute)
        val workManager = WorkManager.getInstance(context)
        when (frequency) {
            FREQUENCY_DAILY, FREQUENCY_WEEKLY -> {
                val intervalDays = if (frequency == FREQUENCY_DAILY) 1L else 7L
                val initialDelay = calculateInitialDelayMillis(hour, minute)
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, TimeUnit.DAYS)
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build()
                )
            }

            else -> workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    private fun calculateInitialDelayMillis(targetHour: Int, targetMinute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            set(java.util.Calendar.MINUTE, targetMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
