package com.carlos.controlmedicamentos.license

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

sealed interface ValidationResult {
    data object Success : ValidationResult
    data object Invalid : ValidationResult
    data object ExpiredOrDisabled : ValidationResult
    data object NetworkError : ValidationResult
}

object LemonSqueezyService {

    private const val TAG = "LemonSqueezyService"
    private const val VALIDATE_URL = "https://api.lemonsqueezy.com/v1/licenses/validate"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun validateKey(licenseKey: String): ValidationResult = withContext(Dispatchers.IO) {
        val key = licenseKey.trim()
        if (key.isEmpty()) return@withContext ValidationResult.Invalid

        val body = FormBody.Builder()
            .add("license_key", key)
            .build()

        val request = Request.Builder()
            .url(VALIDATE_URL)
            .header("Accept", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (response.code == 404) {
                        ValidationResult.Invalid
                    } else {
                        Log.w(TAG, "HTTP ${response.code} validando llave")
                        ValidationResult.NetworkError
                    }
                }

                val rawBody = response.body?.string()
                if (rawBody.isNullOrBlank()) return@withContext ValidationResult.NetworkError

                val json = try {
                    JsonParser.parseString(rawBody).asJsonObject
                } catch (e: Exception) {
                    Log.e(TAG, "Respuesta no JSON de Lemon Squeezy", e)
                    return@withContext ValidationResult.NetworkError
                }

                val valid = json.get("valid")?.asBoolean == true
                if (!valid) return@withContext ValidationResult.Invalid

                val status = json.getAsJsonObject("license_key")
                    ?.get("status")?.asString?.lowercase()

                when (status) {
                    "active" -> ValidationResult.Success
                    "inactive" -> ValidationResult.Success
                    "expired", "disabled" -> ValidationResult.ExpiredOrDisabled
                    else -> ValidationResult.Invalid
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de red validando llave", e)
            ValidationResult.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado validando llave", e)
            ValidationResult.NetworkError
        }
    }
}
