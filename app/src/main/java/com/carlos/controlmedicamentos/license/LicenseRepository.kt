package com.carlos.controlmedicamentos.license

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.carlos.controlmedicamentos.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val syncDebug: StateFlow<LicenseSyncDebug>
    suspend fun verifyLicense(): LicenseStatus
    suspend fun activateWithKey(licenseKey: String): ActivationResult
}

data class LicenseSyncDebug(
    val dispositivoId: String = "Desconocido",
    val licenciasPorId: Boolean? = null,
    val licenciasPorCampo: Boolean? = null,
    val licenciasPorClave: Boolean? = null,
    val consultando: Boolean = true,
    val error: String? = null
)

class FirebaseLicenseRepository(private val context: Context) : LicenseRepository {

    private val _syncDebug = MutableStateFlow(LicenseSyncDebug())
    override val syncDebug: StateFlow<LicenseSyncDebug> = _syncDebug

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
        _syncDebug.value = LicenseSyncDebug(dispositivoId = androidId, consultando = true)

        try {
            val cachedKeyBeforeSync = prefs.getString(KEY_LICENSE_KEY, null)
            check(prefs.edit().clear().commit()) { "No se pudo limpiar la caché de licencia" }

            val docRef = collection.document(androidId)
            val snapshot = docRef.get().await()
            val deviceFieldSnapshot = collection
                .whereEqualTo(FIELD_DISPOSITIVO, androidId)
                .get()
                .await()
                .documents
                .maxByOrNull { it.getLong(FIELD_FECHA_FIN) ?: 0L }
            val remoteLicenseKey = listOfNotNull(
                snapshot.getString(FIELD_LICENCIA_KEY),
                deviceFieldSnapshot?.getString(FIELD_LICENCIA_KEY)
            ).firstOrNull { it.isNotBlank() }
            val cachedKeySnapshot = cachedKeyBeforeSync?.takeIf { it.isNotBlank() }?.let {
                licenseKeysCollection.document(it).get().await()
            }
            val remoteLicenseKeySnapshot = remoteLicenseKey?.let {
                licenseKeysCollection.document(it).get().await()
            }
            val keySnapshots = licenseKeysCollection
                .whereEqualTo(FIELD_DISPOSITIVO, androidId)
                .get()
                .await()
            val keySnapshotByDevice = keySnapshots.documents.maxByOrNull {
                it.getLong(FIELD_FECHA_FIN) ?: 0L
            }

            _syncDebug.value = LicenseSyncDebug(
                dispositivoId = androidId,
                licenciasPorId = snapshot.exists(),
                licenciasPorCampo = deviceFieldSnapshot != null,
                licenciasPorClave = keySnapshotByDevice != null ||
                    cachedKeySnapshot != null ||
                    remoteLicenseKeySnapshot?.exists() == true,
                consultando = false
            )

            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "verifyLicense: licenciasPorId=${snapshot.exists()} " +
                        "licenciasPorCampo=${deviceFieldSnapshot != null} " +
                        "clavesPorDispositivo=${keySnapshots.documents.size}"
                )
            }
            val keySnapshot = listOfNotNull(
                keySnapshotByDevice,
                remoteLicenseKeySnapshot?.takeIf { it.exists() },
                cachedKeySnapshot
            )
                .maxByOrNull { it.getLong(FIELD_FECHA_FIN) ?: 0L }
            val effectiveSnapshot = listOfNotNull(
                snapshot.takeIf { it.exists() },
                deviceFieldSnapshot
            ).maxByOrNull { it.getLong(FIELD_FECHA_FIN) ?: 0L }

            val deviceEnd = effectiveSnapshot?.getLong(FIELD_FECHA_FIN) ?: 0L
            val keyEnd = keySnapshot?.getLong(FIELD_FECHA_FIN) ?: 0L
            val deviceType = effectiveSnapshot?.getString(FIELD_TIPO)?.uppercase()
            val trialStartStored = effectiveSnapshot?.getLong(FIELD_TRIAL_FECHA_INICIO) ?: 0L
            val trialEndStored = effectiveSnapshot?.getLong(FIELD_TRIAL_FECHA_FIN) ?: 0L
            val accumulatedTrial = effectiveSnapshot?.getLong(FIELD_TRIAL_RESTANTE_MS) ?: 0L
            val keyAccumulatedTrial = keySnapshot?.getLong(FIELD_TRIAL_RESTANTE_MS) ?: 0L
            val annualKeyFound = keySnapshot != null && keyEnd > 0L
            val recoveredFromKey = annualKeyFound && keyEnd > deviceEnd

            if (effectiveSnapshot != null || keySnapshot != null) {
                val tipo = if (annualKeyFound || recoveredFromKey || deviceType == LicenseType.ANNUAL.name) LicenseType.ANNUAL.name
                else effectiveSnapshot?.getString(FIELD_TIPO) ?: LicenseType.TRIAL.name
                // Los documentos antiguos de trial no incluÃ­an sus campos especÃ­ficos,
                // pero conservan creado_en. Se usa solamente como recuperaciÃ³n de esos
                // documentos que fueron convertidos a ANNUAL antes de guardar el remanente.
                val trialStart = maxOf(
                    trialStartStored,
                    effectiveSnapshot?.getLong(FIELD_CREADO_EN) ?: 0L
                )
                val trialEnd = maxOf(
                    trialEndStored,
                    if (trialStart > 0L) trialStart + TRIAL_DURATION_MS else 0L
                )
                val trialRemaining = when {
                    accumulatedTrial > 0L -> accumulatedTrial
                    keyAccumulatedTrial > 0L -> keyAccumulatedTrial
                    deviceType == LicenseType.TRIAL.name -> remainingTrialMillis(
                        trialEnd = maxOf(deviceEnd, trialEnd),
                        now = now
                    )
                    tipo == LicenseType.ANNUAL.name && trialEnd > now ->
                        remainingTrialMillis(trialEnd = trialEnd, now = now)
                    else -> 0L
                }
                val fechaFin = if (tipo == LicenseType.ANNUAL.name) {
                    maxOf(deviceEnd, keyEnd, now) +
                        if (trialRemaining > 0L && accumulatedTrial == 0L && keyAccumulatedTrial == 0L) {
                            trialRemaining
                        } else {
                            0L
                        }
                } else {
                    maxOf(deviceEnd, keyEnd)
                }
                val fechaInicio = if (recoveredFromKey) {
                    keySnapshot?.getLong(FIELD_FECHA_INICIO) ?: now
                } else {
                    effectiveSnapshot?.getLong(FIELD_FECHA_INICIO) ?: 0L
                }
                val licenciaKey = keySnapshot?.id ?: effectiveSnapshot?.getString(FIELD_LICENCIA_KEY)

                if (tipo == LicenseType.ANNUAL.name && fechaFin > deviceEnd) {
                    val accumulatedData = hashMapOf<String, Any>(
                        FIELD_DISPOSITIVO to androidId,
                        FIELD_TIPO to LicenseType.ANNUAL.name,
                        FIELD_FECHA_INICIO to fechaInicio,
                        FIELD_FECHA_FIN to fechaFin,
                        FIELD_TRIAL_RESTANTE_MS to trialRemaining,
                        FIELD_TRIAL_FECHA_INICIO to if (trialRemaining > 0L) trialStart else 0L,
                        FIELD_TRIAL_FECHA_FIN to if (trialRemaining > 0L) trialEnd else 0L
                    )
                    if (!licenciaKey.isNullOrBlank()) {
                        accumulatedData[FIELD_LICENCIA_KEY] = licenciaKey
                    }
                    docRef.set(accumulatedData, SetOptions.merge()).await()
                    keySnapshot?.reference?.set(
                        hashMapOf(
                            FIELD_FECHA_FIN to fechaFin,
                            FIELD_TRIAL_RESTANTE_MS to trialRemaining
                        ),
                        SetOptions.merge()
                    )?.await()
                }

                cacheResult(type = tipo, startDate = fechaInicio, endDate = fechaFin, licenseKey = licenciaKey)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Estado remoto resuelto: tipo=$tipo trialRestanteMs=$trialRemaining")
                }

                val resultStatus = if (now <= fechaFin) {
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

                if (BuildConfig.DEBUG && tipo == LicenseType.ANNUAL.name) {
                    Log.d(TAG, "Licencia ANNUAL resuelta; LicenseViewModel publicará el estado en Main Thread")
                }

                return@withContext resultStatus
            }

            // No existe: crear trial de 180 días bloqueado por dispositivo (versión Embajador)
            val trialStart = now
            val trialEnd = now + TRIAL_DURATION_MS
            val data = hashMapOf(
                FIELD_DISPOSITIVO to androidId,
                FIELD_TIPO to LicenseType.TRIAL.name,
                FIELD_FECHA_INICIO to trialStart,
                FIELD_FECHA_FIN to trialEnd,
                FIELD_TRIAL_FECHA_INICIO to trialStart,
                FIELD_TRIAL_FECHA_FIN to trialEnd,
                FIELD_CREADO_EN to now
            )

            docRef.set(data, SetOptions.merge()).await()
            cacheResult(type = LicenseType.TRIAL.name, startDate = trialStart, endDate = trialEnd)
            Log.d(TAG, "No se encontró licencia para dispositivo_id=$androidId; trial creado hasta $trialEnd")

            LicenseStatus.Valid(
                type = LicenseType.TRIAL,
                startDate = trialStart,
                endDate = trialEnd
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando Firestore", e)
            _syncDebug.value = _syncDebug.value.copy(
                consultando = false,
                error = e.message ?: e::class.simpleName
            )

            LicenseStatus.Error(
                message = "No se pudo sincronizar la licencia con Firebase. Revisa tu conexión a internet.",
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
            val deviceDocRef = collection.document(androidId)
            val deviceSnapshot = deviceDocRef.get().await()
            val existingType = deviceSnapshot.getString(FIELD_TIPO)?.uppercase()
            val existingEnd = deviceSnapshot.getLong(FIELD_FECHA_FIN) ?: 0L
            val keyAlreadyAssigned = keySnapshot.exists() &&
                keySnapshot.getString(FIELD_DISPOSITIVO) == androidId
            val keyEnd = keySnapshot.getLong(FIELD_FECHA_FIN) ?: 0L
            val existingTrialStart = deviceSnapshot.getLong(FIELD_FECHA_INICIO) ?: 0L
            val trialRemaining = if (existingType == LicenseType.TRIAL.name) {
                remainingTrialMillis(trialEnd = existingEnd, now = now)
            } else {
                0L
            }

            // Una llave ya asignada conserva su vencimiento: reactivarla no debe regalar
            // tiempo adicional. Una llave nueva comienza su aÃ±o hoy y suma solamente el
            // remanente aÃºn vÃ¡lido del trial del dispositivo.
            val fechaInicio = if (keyAlreadyAssigned) {
                keySnapshot.getLong(FIELD_FECHA_INICIO) ?: now
            } else {
                now
            }
            val fechaFin = if (keyAlreadyAssigned && keyEnd > now) {
                maxOf(existingEnd, keyEnd, now)
            } else {
                now + ANNUAL_DURATION_MS + trialRemaining
            }

            keyDocRef.set(
                hashMapOf(
                    FIELD_FECHA_INICIO to fechaInicio,
                    FIELD_FECHA_FIN to fechaFin,
                    FIELD_DISPOSITIVO to androidId,
                    FIELD_CREADO_EN to (keySnapshot.getLong(FIELD_CREADO_EN) ?: now),
                    FIELD_TRIAL_RESTANTE_MS to trialRemaining
                ),
                SetOptions.merge()
            ).await()

            val data = hashMapOf<String, Any>(
                FIELD_DISPOSITIVO to androidId,
                FIELD_TIPO to LicenseType.ANNUAL.name,
                FIELD_FECHA_INICIO to fechaInicio,
                FIELD_FECHA_FIN to fechaFin,
                FIELD_LICENCIA_KEY to normalizedKey,
                FIELD_TRIAL_FECHA_INICIO to if (trialRemaining > 0L) existingTrialStart else 0L,
                FIELD_TRIAL_FECHA_FIN to if (trialRemaining > 0L) existingEnd else 0L,
                FIELD_TRIAL_RESTANTE_MS to trialRemaining
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

    private fun remainingTrialMillis(trialEnd: Long, now: Long): Long =
        (trialEnd - now).coerceAtLeast(0L)

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
        private const val FIELD_TRIAL_FECHA_INICIO = "trial_fecha_inicio"
        private const val FIELD_TRIAL_FECHA_FIN = "trial_fecha_fin"
        private const val FIELD_TRIAL_RESTANTE_MS = "trial_restante_ms"

        private const val TRIAL_DAYS = 180
        private val TRIAL_DURATION_MS: Long = TRIAL_DAYS * 24L * 60L * 60L * 1000L
        private const val ANNUAL_DAYS = 365
        private val ANNUAL_DURATION_MS: Long = ANNUAL_DAYS * 24L * 60L * 60L * 1000L
    }
}
