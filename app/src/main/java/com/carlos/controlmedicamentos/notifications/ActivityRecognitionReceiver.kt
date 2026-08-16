package com.carlos.controlmedicamentos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.ActivityEventType
import com.carlos.controlmedicamentos.data.local.ActivityOrigin
import com.carlos.controlmedicamentos.data.local.ConfigSedentarismo
import com.carlos.controlmedicamentos.data.local.PhysicalActivity
import java.util.Calendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ActivityRecognitionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.carlos.controlmedicamentos.ACTION_ACTIVITY_RECOGNITION"
        const val PREFS_NAME = "sedentarismo_tracking"
        const val KEY_PATIENT_ID = "patient_id"
        const val KEY_LAST_MOVING = "last_moving"
        const val KEY_MOVEMENT_START = "movement_start"
        const val KEY_MOVEMENT_RECORDED = "movement_recorded"
        const val KEY_STILL_START = "still_start"
        const val KEY_LAST_ALERT_LEVEL = "last_alert_level"
        const val KEY_ALERT_ACTIVE = "alert_active"
        const val KEY_ALERT_START = "alert_start"
        const val KEY_ALERT_META = "alert_meta_minutes"
        const val KEY_SPECIAL_ACTIVE = "special_active"
        const val KEY_SPECIAL_META = "special_meta"
        const val KEY_SPECIAL_START = "special_start"
        const val KEY_SPECIAL_MOVEMENT_START = "special_movement_start"
        const val KEY_LAST_SPECIAL_LEVEL = "last_special_level"

        const val ACTION_NATIVE_SEDENTARISMO_CHECK = "com.carlos.controlmedicamentos.notifications.NATIVE_SEDENTARISMO_CHECK"
        const val EXTRA_NATIVE_PATIENT_ID = "NATIVE_SED_PATIENT_ID"
        const val EXTRA_NATIVE_START_TIME = "NATIVE_SED_START_TIME"
        const val EXTRA_NATIVE_END_TIME = "NATIVE_SED_END_TIME"
        private const val KEY_NATIVE_STEP_COUNT = "native_step_count"
        private const val KEY_NATIVE_STEP_TIME = "native_step_time"

        private const val CONFIDENCE_THRESHOLD = 60
        private const val MOVEMENT_THRESHOLD_MS = 5 * 60 * 1000L
        private const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L

        fun iniciarMonitoreoDespuesAlerta(context: Context, patientId: Int, metaMinutos: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt(KEY_PATIENT_ID, patientId)
                putBoolean(KEY_ALERT_ACTIVE, true)
                putLong(KEY_ALERT_START, System.currentTimeMillis())
                putInt(KEY_ALERT_META, metaMinutos)
                putLong(KEY_STILL_START, System.currentTimeMillis())
                apply()
            }
        }

        fun guardarPacienteActivo(context: Context, patientId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_PATIENT_ID, patientId).apply()
        }

        fun haPermanecidoInactivo(context: Context, patientId: Int, duracionMinutos: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stillStart = prefs.getLong(KEY_STILL_START, 0L)
            return prefs.getInt(KEY_PATIENT_ID, 0) == patientId &&
                !prefs.getBoolean(KEY_LAST_MOVING, false) &&
                stillStart > 0L &&
                System.currentTimeMillis() - stillStart >= duracionMinutos * 60_000L
        }

        fun activarEjercicioEspecial(context: Context, patientId: Int, metaMinutos: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putInt(KEY_PATIENT_ID, patientId)
                putBoolean(KEY_SPECIAL_ACTIVE, true)
                putInt(KEY_SPECIAL_META, metaMinutos)
                putLong(KEY_SPECIAL_START, System.currentTimeMillis())
                putLong(KEY_SPECIAL_MOVEMENT_START, 0L)
                apply()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION -> {
                val result = ActivityRecognitionResult.extractResult(intent) ?: return
                val activity = result.mostProbableActivity ?: return
                val movimientoDetectado = result.probableActivities.any { candidate ->
                    candidate.confidence >= 40 && (
                        candidate.type == DetectedActivity.WALKING ||
                            candidate.type == DetectedActivity.RUNNING ||
                            candidate.type == DetectedActivity.ON_FOOT ||
                            candidate.type == DetectedActivity.ON_BICYCLE
                        )
                }
                val quietoDetectado = activity.type == DetectedActivity.STILL && activity.confidence >= CONFIDENCE_THRESHOLD
                if (!movimientoDetectado && !quietoDetectado) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        procesar(context, movimientoDetectado, quietoDetectado)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_NATIVE_SEDENTARISMO_CHECK -> {
                val pendingResult = goAsync()
                val patientId = intent.getIntExtra(EXTRA_NATIVE_PATIENT_ID, 0)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        nativeCheck(context, patientId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    /**
     * Respaldo nativo: lee el contador de pasos del hardware y decide si hubo movimiento.
     * Se invoca periódicamente desde AlarmManager, por lo que no depende de que Google Play
     * Services entregue actualizaciones de reconocimiento de actividad en segundo plano.
     */
    private suspend fun nativeCheck(context: Context, patientId: Int) {
        if (patientId <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(context)
        val config = db.sedentarismoDao().obtenerConfig(patientId)
        val now = System.currentTimeMillis()

        if (!estaEnHorarioActivo(config)) {
            prefs.edit().putLong(KEY_STILL_START, 0L)
                .putInt(KEY_LAST_ALERT_LEVEL, 0)
                .putInt(KEY_LAST_SPECIAL_LEVEL, 0)
                .apply()
            SedentarismoScheduler(context).programarNativeCheck(patientId)
            return
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val lastStepCount = prefs.getInt(KEY_NATIVE_STEP_COUNT, -1)
        val currentStepCount = stepCounter?.let { readStepCounter(sensorManager, it) }

        if (currentStepCount != null) {
            val hasMovement = lastStepCount >= 0 && currentStepCount > lastStepCount
            prefs.edit().putInt(KEY_NATIVE_STEP_COUNT, currentStepCount).apply()
            if (hasMovement) {
                prefs.edit().putLong(KEY_NATIVE_STEP_TIME, now).apply()
                procesar(context, isMovement = true, isStill = false)
            } else {
                procesar(context, isMovement = false, isStill = true)
            }
        } else {
            // Sin sensor de pasos: forzar la lógica de inactividad para no perder el aviso
            procesar(context, isMovement = false, isStill = true)
        }

        SedentarismoScheduler(context).programarNativeCheck(patientId)
    }

    private suspend fun readStepCounter(sensorManager: SensorManager, sensor: Sensor): Int? {
        val deferred = CompletableDeferred<Int?>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!deferred.isCompleted) {
                    deferred.complete(event.values[0].toInt())
                    sensorManager.unregisterListener(this)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        return try {
            withTimeout(1500L) { deferred.await() }
        } catch (_: Exception) {
            null
        } finally {
            sensorManager.unregisterListener(listener)
        }
    }

    private suspend fun procesar(context: Context, isMovement: Boolean, isStill: Boolean) {

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val lastMoving = prefs.getBoolean(KEY_LAST_MOVING, false)
        val movementStart = prefs.getLong(KEY_MOVEMENT_START, 0L)
        val movementRecorded = prefs.getBoolean(KEY_MOVEMENT_RECORDED, false)
        val stillStart = prefs.getLong(KEY_STILL_START, 0L)
        val lastAlertLevel = prefs.getInt(KEY_LAST_ALERT_LEVEL, 0)
        val alertActive = prefs.getBoolean(KEY_ALERT_ACTIVE, false)
        val alertStart = prefs.getLong(KEY_ALERT_START, 0L)
        val alertMeta = prefs.getInt(KEY_ALERT_META, 5)
        val specialActive = prefs.getBoolean(KEY_SPECIAL_ACTIVE, false)
        val specialMeta = prefs.getInt(KEY_SPECIAL_META, 15)
        val specialMovementStart = prefs.getLong(KEY_SPECIAL_MOVEMENT_START, 0L)
        val patientId = prefs.getInt(KEY_PATIENT_ID, 0).takeIf { it > 0 } ?: return

        val editor = prefs.edit()

        when {
            isMovement -> {
                if (!lastMoving) {
                    editor.putBoolean(KEY_LAST_MOVING, true)
                    editor.putLong(KEY_MOVEMENT_START, now)
                    editor.putLong(KEY_STILL_START, 0L)
                    editor.putInt(KEY_LAST_ALERT_LEVEL, 0)
                    editor.putInt(KEY_LAST_SPECIAL_LEVEL, 0)
                }
                if (specialActive) {
                    val currentSpecialStart = specialMovementStart.takeIf { it > 0 } ?: now
                    if (specialMovementStart == 0L) {
                        editor.putLong(KEY_SPECIAL_MOVEMENT_START, now)
                    }
                    val specialDuration = now - currentSpecialStart
                    if (specialDuration >= specialMeta * 60_000L) {
                        guardarEspecial(context, patientId, specialMeta)
                        editor.putBoolean(KEY_SPECIAL_ACTIVE, false)
                        editor.putLong(KEY_SPECIAL_MOVEMENT_START, 0L)
                    }
                }
            }
            isStill -> {
                if (lastMoving) {
                    val duration = now - movementStart
                    editor.putBoolean(KEY_LAST_MOVING, false)
                    editor.putLong(KEY_MOVEMENT_START, 0L)
                    if (duration >= MOVEMENT_THRESHOLD_MS) {
                        guardarMovimiento(context, patientId, duration, alertActive)
                        if (alertActive) {
                            editor.putBoolean(KEY_ALERT_ACTIVE, false)
                            editor.putLong(KEY_ALERT_START, 0L)
                            editor.putInt(KEY_ALERT_META, 5)
                        }
                    }
                }
                if (specialActive && specialMovementStart > 0L) {
                    editor.putLong(KEY_SPECIAL_MOVEMENT_START, 0L)
                }
                val config = db.sedentarismoDao().obtenerConfig(patientId)
                if (!estaEnHorarioActivo(config)) {
                    // Fuera del horario de monitoreo: no acumular inactividad (ej. durante la noche)
                    if (stillStart != 0L) {
                        editor.putLong(KEY_STILL_START, 0L)
                        editor.putInt(KEY_LAST_ALERT_LEVEL, 0)
                        editor.putInt(KEY_LAST_SPECIAL_LEVEL, 0)
                    }
                } else if (stillStart == 0L) {
                    editor.putLong(KEY_STILL_START, now)
                    editor.putInt(KEY_LAST_ALERT_LEVEL, 0)
                    editor.putInt(KEY_LAST_SPECIAL_LEVEL, 0)
                } else if (stillStart < inicioVentanaHoy(config)) {
                    // stillStart quedo desde antes de que abriera la ventana de hoy (ej. se quedo
                    // quieto anoche y no llego ningun broadcast durante las horas fuera de rango).
                    // Se descarta ese conteo obsoleto en vez de arrastrarlo al nuevo dia.
                    editor.putLong(KEY_STILL_START, now)
                    editor.putInt(KEY_LAST_ALERT_LEVEL, 0)
                    editor.putInt(KEY_LAST_SPECIAL_LEVEL, 0)
                } else {
                    val stillMs = now - stillStart
                    // Aviso del usuario: cada 1-5 horas configurables
                    val limiteMs = ((config?.limiteInactividadMinutos ?: 180) * 60_000L)
                    val level = (stillMs / limiteMs).toInt()
                    if (level > 0 && level > lastAlertLevel && debeNotificarAhora(config)) {
                        val meta = if (level == 1) 15 else 30
                        val totalMin = (stillMs / 60000).toInt()
                        NotificacionHelper.mostrarAlertaSedentarismo(context, patientId, totalMin, meta)
                        editor.putInt(KEY_LAST_ALERT_LEVEL, level)
                    }
                    // Aviso especial: cada 3 horas fijas
                    val lastSpecialLevel = prefs.getInt(KEY_LAST_SPECIAL_LEVEL, 0)
                    val specialLevel = (stillMs / THREE_HOURS_MS).toInt()
                    if (specialLevel > 0 && specialLevel > lastSpecialLevel && debeNotificarAhora(config)) {
                        val meta = if (specialLevel == 1) 15 else 30
                        val totalMin = (stillMs / 60000).toInt()
                        NotificacionHelper.mostrarAlertaSedentarismo(context, patientId, totalMin, meta)
                        editor.putInt(KEY_LAST_SPECIAL_LEVEL, specialLevel)
                    }
                    if (alertActive && alertStart > 0L) {
                        val stillAfterAlert = now - alertStart
                        if (stillAfterAlert >= alertMeta * 60_000L) {
                            guardar(context, patientId, "SIN_MOVIMIENTO", 0, "Sin movimiento después del aviso")
                            editor.putBoolean(KEY_ALERT_ACTIVE, false)
                            editor.putLong(KEY_ALERT_START, 0L)
                            editor.putInt(KEY_ALERT_META, 5)
                        }
                    }
                }
            }
            else -> {
                if (lastMoving) {
                    val duration = now - movementStart
                    editor.putBoolean(KEY_LAST_MOVING, false)
                    editor.putLong(KEY_MOVEMENT_START, 0L)
                    if (duration >= MOVEMENT_THRESHOLD_MS) {
                        guardarMovimiento(context, patientId, duration, alertActive)
                        if (alertActive) {
                            editor.putBoolean(KEY_ALERT_ACTIVE, false)
                            editor.putLong(KEY_ALERT_START, 0L)
                            editor.putInt(KEY_ALERT_META, 5)
                        }
                    }
                }
                if (specialActive && specialMovementStart > 0L) {
                    editor.putLong(KEY_SPECIAL_MOVEMENT_START, 0L)
                }
                editor.putLong(KEY_STILL_START, 0L)
                editor.putInt(KEY_LAST_ALERT_LEVEL, 0)
            }
        }

        editor.apply()
    }

    private fun inicioVentanaHoy(config: ConfigSedentarismo?): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, config?.horaInicioMonitoreo ?: 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun estaEnHorarioActivo(config: ConfigSedentarismo?): Boolean {
        if (config == null || !config.activado) return false
        val cal = Calendar.getInstance()
        val horaActual = cal.get(Calendar.HOUR_OF_DAY)
        return horaActual >= config.horaInicioMonitoreo && horaActual < config.horaFinMonitoreo
    }

    private fun debeNotificarAhora(config: ConfigSedentarismo?): Boolean {
        if (!estaEnHorarioActivo(config)) return false
        val cal = Calendar.getInstance()
        val diaSemana = cal.get(Calendar.DAY_OF_WEEK)
        val diaLunes = if (diaSemana == Calendar.SUNDAY) 7 else diaSemana - 1
        val diasActivos = config!!.diasActivos.split(",").mapNotNull { it.trim().toIntOrNull() }
        return diasActivos.contains(diaLunes)
    }

    private fun guardarMovimiento(context: Context, patientId: Int, durationMs: Long, alertActive: Boolean) {
        val mins = (durationMs / 60000).toInt()
        val secs = ((durationMs % 60000) / 1000).toInt()
        val mensaje = if (alertActive) "Movimiento realizado: $mins minutos y $secs segundos"
                      else "Movimiento detectado: $mins minutos y $secs segundos"
        guardar(context, patientId, "MOVIMIENTO_REGISTRADO", mins, mensaje)
    }

    private fun guardarEspecial(context: Context, patientId: Int, metaMinutos: Int) {
        guardar(context, patientId, "EJERCICIO_ESPECIAL", metaMinutos, "Ejercicio especial realizado: $metaMinutos minutos")
    }

    private fun guardar(context: Context, patientId: Int, tipo: String, minutos: Int, notas: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.physicalActivityDao().insertar(
                    PhysicalActivity(
                        patientId = patientId,
                        tipo = "caminar",
                        fechaInicio = System.currentTimeMillis() - minutos * 60_000L,
                        fechaFin = System.currentTimeMillis(),
                        duracionSegundos = minutos * 60L,
                        origen = ActivityOrigin.BACKGROUND_DETECTED,
                        tipoEvento = if (tipo == "SIN_MOVIMIENTO") ActivityEventType.INACTIVITY else ActivityEventType.MOVEMENT,
                        minutosInactivo = minutos,
                        notas = notas
                    )
                )
            } catch (_: Exception) { }
        }
    }
}
