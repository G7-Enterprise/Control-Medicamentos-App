package com.carlos.controlmedicamentos.notifications

import android.content.Context

private const val DEFAULT_CRITICAL_RETRY_INTERVAL_MINUTES = 10
private const val DEFAULT_CRITICAL_MAX_RETRY_COUNT = 6

data class CriticalAlertConfig(
    val retryIntervalMinutes: Int = DEFAULT_CRITICAL_RETRY_INTERVAL_MINUTES,
    val maxRetryCount: Int = DEFAULT_CRITICAL_MAX_RETRY_COUNT,
    val soundUri: String = ""
)

object CriticalAlertSettings {
    private const val PREFS_NAME = "critical_alert_settings"
    private const val KEY_RETRY_INTERVAL = "retry_interval_minutes"
    private const val KEY_MAX_RETRY_COUNT = "max_retry_count"
    private const val KEY_SOUND_URI = "sound_uri"
    const val DEFAULT_RETRY_INTERVAL_MINUTES = DEFAULT_CRITICAL_RETRY_INTERVAL_MINUTES
    const val DEFAULT_MAX_RETRY_COUNT = DEFAULT_CRITICAL_MAX_RETRY_COUNT
    val ALLOWED_RETRY_INTERVALS = listOf(5, 10, 15)
    const val MAX_ALLOWED_RETRY_COUNT = 20

    fun load(context: Context): CriticalAlertConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return CriticalAlertConfig(
            retryIntervalMinutes = normalizeRetryInterval(
                prefs.getInt(KEY_RETRY_INTERVAL, DEFAULT_RETRY_INTERVAL_MINUTES)
            ),
            maxRetryCount = normalizeMaxRetryCount(
                prefs.getInt(KEY_MAX_RETRY_COUNT, DEFAULT_MAX_RETRY_COUNT)
            ),
            soundUri = prefs.getString(KEY_SOUND_URI, "").orEmpty()
        )
    }

    fun save(context: Context, config: CriticalAlertConfig) {
        val normalized = config.copy(
            retryIntervalMinutes = normalizeRetryInterval(config.retryIntervalMinutes),
            maxRetryCount = normalizeMaxRetryCount(config.maxRetryCount)
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_RETRY_INTERVAL, normalized.retryIntervalMinutes)
            .putInt(KEY_MAX_RETRY_COUNT, normalized.maxRetryCount)
            .putString(KEY_SOUND_URI, normalized.soundUri)
            .apply()
    }

    fun getRetryIntervalMinutes(context: Context): Int = load(context).retryIntervalMinutes

    fun getMaxRetryCount(context: Context): Int = load(context).maxRetryCount

    fun getSoundUri(context: Context): String = load(context).soundUri

    fun normalizeRetryInterval(value: Int): Int {
        return value.takeIf(ALLOWED_RETRY_INTERVALS::contains) ?: DEFAULT_RETRY_INTERVAL_MINUTES
    }

    fun normalizeMaxRetryCount(value: Int): Int {
        return value.coerceIn(0, MAX_ALLOWED_RETRY_COUNT)
    }
}