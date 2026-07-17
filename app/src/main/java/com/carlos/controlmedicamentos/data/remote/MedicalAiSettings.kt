package com.carlos.controlmedicamentos.data.remote

import android.content.Context

data class MedicalAiConfig(
    val endpointUrl: String = DEFAULT_ENDPOINT_URL,
    val modelName: String = DEFAULT_MODEL_NAME
)

private const val DEFAULT_ENDPOINT_URL = "http://192.168.40.162:11434/api/generate"
private const val DEFAULT_MODEL_NAME = "medgemma"

object MedicalAiSettings {
    private const val PREFS_NAME = "medical_ai_settings"
    private const val KEY_ENDPOINT_URL = "endpoint_url"
    private const val KEY_MODEL_NAME = "model_name"

    fun load(context: Context): MedicalAiConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MedicalAiConfig(
            endpointUrl = prefs.getString(KEY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL).orEmpty().ifBlank { DEFAULT_ENDPOINT_URL },
            modelName = prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME).orEmpty().ifBlank { DEFAULT_MODEL_NAME }
        )
    }

    fun save(context: Context, config: MedicalAiConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENDPOINT_URL, config.endpointUrl.trim())
            .putString(KEY_MODEL_NAME, config.modelName.trim())
            .apply()
    }
}