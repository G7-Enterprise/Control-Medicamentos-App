package com.carlos.controlmedicamentos.fall

import kotlin.math.sqrt

/**
 * Perfil biométrico del usuario para calibrar la detección de caídas.
 * @param edad Edad en años (default 55 = sin modificador)
 * @param alturaCm Estatura en centímetros (default 170 = sin modificador)
 */
data class PerfilUsuario(
    val edad: Int = 55,
    val alturaCm: Int = 170
)

/**
 * Algoritmo de detección de caídas con acelerómetro + giroscopio opcional.
 *
 * Fases de una caída típica:
 * 1. Caída libre (o giro brusco por giroscopio): magnitud acelerómetro < freeFallThreshold
 * 2. Impacto: magnitud acelerómetro > umbralImpacto
 * 3. Inmovilidad: magnitud vuelve a nivel normal (< motionlessThreshold)
 *
 * Los umbrales se ajustan según sensibilidad de UI y perfil biométrico del usuario.
 *
 * @param sensitivity Sensibilidad de UI (0.1 = muy baja, 1.0 = muy alta)
 * @param perfil Perfil biométrico del usuario (edad + altura)
 */
class FallDetectionAlgorithm(
    private val sensitivity: Float = 0.5f,
    private val perfil: PerfilUsuario = PerfilUsuario()
) {
    // --- Umbrales base (máxima sensibilidad teórica) ---
    private val BASE_IMPACTO_FUERTE = 9.0f
    private val BASE_IMPACTO_MODERADO = 7.0f

    // --- Modificador biométrico de edad: -1% por año sobre 55, máx 40% de reducción ---
    private val edadExtra = (perfil.edad - 55).coerceAtLeast(0)
    private val modificadorEdad = (1.0f - (edadExtra / 100f).coerceAtMost(0.40f))

    // --- Modificador de altura: normalizado a 170cm (< 170cm = más sensible) ---
    private val modificadorAltura = perfil.alturaCm / 170f

    // --- Margen UI: 100% UI = 0 margen; 0% UI = margen máximo ---
    private val factorUI = (1.0f - sensitivity).coerceIn(0f, 1f)
    private val margenFuerte = factorUI * 5.0f    // 0 a +5 m/s²
    private val margenModerado = factorUI * 3.0f   // 0 a +3 m/s²

    // --- Umbrales finales calibrados ---
    private val hardImpactThresholdMs2: Float =
        ((BASE_IMPACTO_FUERTE + margenFuerte) * modificadorEdad * modificadorAltura).coerceAtLeast(10.5f)
    private val impactThresholdMs2: Float =
        ((BASE_IMPACTO_MODERADO + margenModerado) * modificadorEdad * modificadorAltura).coerceAtLeast(9.8f)

    // --- Caída libre: fijo en 6 m/s² — tolerante a caídas cortas y con giro ---
    private val freeFallThresholdMs2: Float = 6.0f
    private val motionlessThresholdMs2: Float = 16.0f
    private val freeFallMinDurationMs: Long = (80L - (sensitivity * 50).toLong()).coerceAtLeast(20L)
    private val impactTimeoutMs: Long = 1500L
    private val motionlessTimeoutMs: Long = (400L - (sensitivity * 280).toLong()).coerceAtLeast(80L)
    private val hardImpactMotionlessMs: Long = (180L - (sensitivity * 120).toLong()).coerceAtLeast(60L)

    // --- Giroscopio: umbral de giro brusco en rad/s (equivale a ~200°/s) ---
    private val gyroThresholdRadS: Float = (3.5f - sensitivity * 1.5f).coerceAtLeast(1.5f) // 100%→2.0, 0%→3.5
    private var gyroSpike = false
    private var gyroSpikeTime: Long = 0L
    private val gyroSpikeWindowMs: Long = 800L

    private enum class State { IDLE, FREE_FALL, IMPACT, HARD_IMPACT, CONFIRMED }
    private var state = State.IDLE
    private var freeFallStartTime: Long = 0L
    private var impactTime: Long = 0L
    private var hardImpactTime: Long = 0L

    init {
        android.util.Log.d("FallAlgorithm", "Calibrado: hardThr=${"%.1f".format(hardImpactThresholdMs2)} " +
            "impThr=${"%.1f".format(impactThresholdMs2)} " +
            "gyroThr=${"%.1f".format(gyroThresholdRadS)} " +
            "edad=${perfil.edad} altura=${perfil.alturaCm}cm " +
            "modEdad=${"%.2f".format(modificadorEdad)} modAltura=${"%.2f".format(modificadorAltura)}")
    }

    /**
     * Alimenta datos del giroscopio. Llamar desde onSensorChanged con TYPE_GYROSCOPE.
     * Detecta giro brusco previo a la caída (ej. persona que se tambalea).
     */
    fun processGyro(gx: Float, gy: Float, gz: Float, timestampMs: Long) {
        val gyroMag = sqrt(gx * gx + gy * gy + gz * gz)
        if (gyroMag >= gyroThresholdRadS) {
            gyroSpike = true
            gyroSpikeTime = timestampMs
            android.util.Log.d("FallAlgorithm", "GYRO_SPIKE gyroMag=${"%.1f".format(gyroMag)} thr=$gyroThresholdRadS")
        } else if (gyroSpike && timestampMs - gyroSpikeTime > gyroSpikeWindowMs) {
            gyroSpike = false
        }
    }

    /**
     * Procesa una muestra de aceleración. Devuelve true si se confirma una caída.
     */
    fun processSample(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        lastMagnitude = magnitude
        android.util.Log.v("FallAlgorithm", "state=$state mag=${"%.1f".format(magnitude)} hardThr=${"%.1f".format(hardImpactThresholdMs2)} gyroSpike=$gyroSpike")

        when (state) {
            State.IDLE -> {
                if (magnitude >= hardImpactThresholdMs2) {
                    android.util.Log.d("FallAlgorithm", "DIRECT_HARD_IMPACT mag=${"%.1f".format(magnitude)} thr=${"%.1f".format(hardImpactThresholdMs2)}")
                    state = State.HARD_IMPACT
                    hardImpactTime = timestampMs
                } else if (magnitude < freeFallThresholdMs2 || gyroSpike) {
                    state = State.FREE_FALL
                    freeFallStartTime = timestampMs
                    if (gyroSpike) android.util.Log.d("FallAlgorithm", "FREE_FALL triggered by GYRO_SPIKE")
                }
            }
            State.FREE_FALL -> {
                if (magnitude >= freeFallThresholdMs2) {
                    val elapsed = timestampMs - freeFallStartTime
                    if (elapsed < freeFallMinDurationMs && !gyroSpike) {
                        state = State.IDLE
                    } else if (magnitude > impactThresholdMs2) {
                        android.util.Log.d("FallAlgorithm", "IMPACT after FREE_FALL mag=${"%.1f".format(magnitude)} elapsed=${elapsed}ms gyro=$gyroSpike")
                        state = State.IMPACT
                        impactTime = timestampMs
                    } else {
                        state = State.IDLE
                    }
                } else if (timestampMs - freeFallStartTime > 2000L) {
                    state = State.IDLE
                }
            }
            State.IMPACT -> {
                val elapsed = timestampMs - impactTime
                if (magnitude < motionlessThresholdMs2) {
                    if (elapsed >= motionlessTimeoutMs) {
                        android.util.Log.d("FallAlgorithm", "CONFIRMED via IMPACT elapsed=${elapsed}ms")
                        state = State.CONFIRMED
                        return true
                    }
                } else if (elapsed > impactTimeoutMs) {
                    state = State.IDLE
                }
            }
            State.HARD_IMPACT -> {
                val elapsed = timestampMs - hardImpactTime
                if (magnitude < motionlessThresholdMs2) {
                    if (elapsed >= hardImpactMotionlessMs) {
                        android.util.Log.d("FallAlgorithm", "CONFIRMED via HARD_IMPACT elapsed=${elapsed}ms")
                        state = State.CONFIRMED
                        return true
                    }
                } else if (elapsed > impactTimeoutMs) {
                    state = State.IDLE
                }
            }
            State.CONFIRMED -> {
                return true
            }
        }
        return false
    }

    fun reset() {
        state = State.IDLE
        freeFallStartTime = 0L
        impactTime = 0L
        hardImpactTime = 0L
        gyroSpike = false
    }

    var lastMagnitude: Float = 0f
        private set
}
