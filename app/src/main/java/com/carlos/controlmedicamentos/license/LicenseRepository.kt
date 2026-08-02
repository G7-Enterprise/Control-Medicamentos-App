package com.carlos.controlmedicamentos.license

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.CancellationException

sealed interface ActivationResult {
    data object Success : ActivationResult
    data object InvalidKey : ActivationResult
    data object ExpiredOrDisabledKey : ActivationResult
    data object NetworkError : ActivationResult
    data object FirestoreError : ActivationResult
}

interface LicenseRepository {
    suspend fun verifyLicense(): LicenseStatus
    suspend fun activateWithKey(licenseKey: String): ActivationResult
}

class FirebaseLicenseRepository(private val context: Context) : LicenseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("licencias")
    private val licenseKeysCollection = db.collection("licencias_por_clave")

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun verifyLicense(): LicenseStatus = withContext(Dispatchers.IO) {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener ANDROID_ID", e)
            return@withContext LicenseStatus.Error(
                message = "No se pudo identificar el dispositivo.",
                canRetry = true
            )
        }

        if (androidId.isNullOrBlank()) {
            return@withContext LicenseStatus.Error(
                message = "Identificador de dispositivo no disponible.",
                canRetry = true
            )
        }

        val now = System.currentTimeMillis()

        try {
            val docRef = collection.document(androidId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val tipo = snapshot.getString(FIELD_TIPO) ?: LicenseType.TRIAL.name
                val fechaFin = snapshot.getLong(FIELD_FECHA_FIN) ?: 0L
                val fechaInicio = snapshot.getLong(FIELD_FECHA_INICIO) ?: 0L
                val licenciaKey = snapshot.getString(FIELD_LICENCIA_KEY)

                cacheResult(type = tipo, startDate = fechaInicio, endDate = fechaFin, licenseKey = licenciaKey)

                return@withContext if (now <= fechaFin) {
                    LicenseStatus.Valid(
                        type = LicenseType.valueOf(tipo.uppercase()),
                        startDate = fechaInicio,
                        endDate = fechaFin
                    )
                } else {
                    LicenseStatus.Expired(
                        type = LicenseType.valueOf(tipo.uppercase()),
                        endDate = fechaFin
                    )
                }
            }

            // No existe: crear trial de 180 días bloqueado por dispositivo (versión Embajador)
            val trialStart = now
            val trialEnd = now + TRIAL_DURATION_MS
            val data = hashMapOf(
                FIELD_DISPOSITIVO to androidId,
                FIELD_TIPO to LicenseType.TRIAL.name,
                FIELD_FECHA_INICIO to trialStart,
                FIELD_FECHA_FIN to trialEnd,
                FIELD_CREADO_EN to now
            )

            docRef.set(data, SetOptions.merge()).await()
            cacheResult(type = LicenseType.TRIAL.name, startDate = trialStart, endDate = trialEnd)

            LicenseStatus.Valid(
                type = LicenseType.TRIAL,
                startDate = trialStart,
                endDate = trialEnd
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando Firestore", e)

            // Fallback a caché local si existe
            val cached = getCachedStatus(now)
            if (cached != null) {
                Log.d(TAG, "Usando licencia cacheada tras error de red")
                return@withContext cached
            }

            LicenseStatus.Error(
                message = "No se pudo verificar la licencia. Revisa tu conexión a internet.",
                canRetry = true
            )
        }
    }

    override suspend fun activateWithKey(licenseKey: String): ActivationResult = withContext(Dispatchers.IO) {
        when (val validation = LemonSqueezyService.validateKey(licenseKey)) {
            is ValidationResult.Invalid -> return@withContext ActivationResult.InvalidKey
            is ValidationResult.ExpiredOrDisabled -> return@withContext ActivationResult.ExpiredOrDisabledKey
            is ValidationResult.NetworkError -> return@withContext ActivationResult.NetworkError
            is ValidationResult.Success -> Unit
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener ANDROID_ID", e)
            return@withContext ActivationResult.FirestoreError
        }
        if (androidId.isNullOrBlank()) return@withContext ActivationResult.FirestoreError

        val normalizedKey = licenseKey.trim()
        val now = System.currentTimeMillis()

        try {
            // Buscar si esta llave ya fue activada antes (en este u otro dispositivo/reinstalacion)
            // para preservar la fecha de inicio original en vez de reiniciar el conteo.
            val keyDocRef = licenseKeysCollection.document(normalizedKey)
            val keySnapshot = keyDocRef.get().await()
            val fechaInicioExistente = if (keySnapshot.exists()) keySnapshot.getLong(FIELD_FECHA_INICIO) else null

            val fechaInicio = fechaInicioExistente ?: now
            val fechaFin = fechaInicio + ANNUAL_DURATION_MS

            keyDocRef.set(
                hashMapOf(
                    FIELD_FECHA_INICIO to fechaInicio,
                    FIELD_FECHA_FIN to fechaFin,
                    FIELD_DISPOSITIVO to androidId,
                    FIELD_CREADO_EN to (keySnapshot.getLong(FIELD_CREADO_EN) ?: now)
                ),
                SetOptions.merge()
            ).await()

            val data = hashMapOf<String, Any>(
                FIELD_DISPOSITIVO to androidId,
                FIELD_TIPO to LicenseType.ANNUAL.name,
                FIELD_FECHA_INICIO to fechaInicio,
                FIELD_FECHA_FIN to fechaFin,
                FIELD_LICENCIA_KEY to normalizedKey
            )
            collection.document(androidId).set(data, SetOptions.merge()).await()
            cacheResult(type = LicenseType.ANNUAL.name, startDate = fechaInicio, endDate = fechaFin, licenseKey = normalizedKey)
            ActivationResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando licencia en Firestore", e)
            ActivationResult.FirestoreError
        }
    }

    private fun cacheResult(type: String, startDate: Long, endDate: Long, licenseKey: String? = null) {
        val editor = prefs.edit()
            .putString(KEY_TYPE, type)
            .putLong(KEY_START_DATE, startDate)
            .putLong(KEY_END_DATE, endDate)
            .putLong(KEY_CACHED_AT, System.currentTimeMillis())
        if (licenseKey != null) {
            editor.putString(KEY_LICENSE_KEY, licenseKey)
        } else {
            editor.remove(KEY_LICENSE_KEY)
        }
        editor.apply()
    }

    private fun getCachedStatus(now: Long): LicenseStatus? {
        val endDate = prefs.getLong(KEY_END_DATE, 0L).takeIf { it > 0L } ?: return null
        val type = prefs.getString(KEY_TYPE, null) ?: LicenseType.TRIAL.name
        val startDate = prefs.getLong(KEY_START_DATE, 0L)

        return if (now <= endDate) {
            LicenseStatus.Valid(
                type = LicenseType.valueOf(type.uppercase()),
                startDate = startDate,
                endDate = endDate
            )
        } else {
            LicenseStatus.Expired(
                type = LicenseType.valueOf(type.uppercase()),
                endDate = endDate
            )
        }
    }

    private companion object {
        private const val TAG = "LicenseRepository"
        private const val PREFS_NAME = "license_cache"
        private const val KEY_TYPE = "license_type"
        private const val KEY_START_DATE = "license_start"
        private const val KEY_END_DATE = "license_end"
        private const val KEY_CACHED_AT = "license_cached_at"
        private const val KEY_LICENSE_KEY = "license_key"

        private const val FIELD_DISPOSITIVO = "dispositivo_id"
        private const val FIELD_TIPO = "tipo"
        private const val FIELD_FECHA_INICIO = "fecha_inicio"
        private const val FIELD_FECHA_FIN = "fecha_fin"
        private const val FIELD_CREADO_EN = "creado_en"
        private const val FIELD_LICENCIA_KEY = "licencia_key"

        private const val TRIAL_DAYS = 180
        private val TRIAL_DURATION_MS: Long = TRIAL_DAYS * 24L * 60L * 60L * 1000L
        private const val ANNUAL_DAYS = 365
        private val ANNUAL_DURATION_MS: Long = ANNUAL_DAYS * 24L * 60L * 60L * 1000L
    }
}
