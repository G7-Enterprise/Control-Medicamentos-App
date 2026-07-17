package com.carlos.controlmedicamentos.fall

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.carlos.controlmedicamentos.FallAlertActivity
import com.carlos.controlmedicamentos.MainActivity
import com.carlos.controlmedicamentos.R
import com.carlos.controlmedicamentos.notifications.NotificacionHelper

/**
 * Servicio en primer plano que monitorea el acelerómetro para detectar caídas.
 * Al confirmar una caída, inicia FallAlertActivity y guarda el evento en la base de datos.
 */
class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var algorithm: FallDetectionAlgorithm = FallDetectionAlgorithm()
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentPatientId: Int = 0
    private var currentContactPhone: String = ""
    private var currentSensitivity: Float = 0.5f
    private var sensorSampleCount: Int = 0

    companion object {
        const val ACTION_START = "com.carlos.controlmedicamentos.fall.START"
        const val ACTION_STOP = "com.carlos.controlmedicamentos.fall.STOP"
        const val EXTRA_PATIENT_ID = "EXTRA_PATIENT_ID"
        const val EXTRA_CONTACT_PHONE = "EXTRA_CONTACT_PHONE"
        const val EXTRA_SENSITIVITY = "EXTRA_SENSITIVITY"
        const val EXTRA_EDAD = "EXTRA_EDAD"
        const val EXTRA_ALTURA_CM = "EXTRA_ALTURA_CM"
        const val NOTIFICATION_ID = 92_001

        fun start(context: Context, patientId: Int, contactPhone: String = "", sensitivity: Float = 0.5f, edad: Int = 55, alturaCm: Int = 170) {
            val intent = Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PATIENT_ID, patientId)
                putExtra(EXTRA_CONTACT_PHONE, contactPhone)
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_EDAD, edad)
                putExtra(EXTRA_ALTURA_CM, alturaCm)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isRunning(context: Context): Boolean {
            // Simplificación: la UI gestionará el estado mediante SharedPreferences o DataStore.
            // Este método puede mejorarse consultando ActivityManager en versiones futuras.
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                currentPatientId = intent?.getIntExtra(EXTRA_PATIENT_ID, 0) ?: 0
                currentContactPhone = intent?.getStringExtra(EXTRA_CONTACT_PHONE) ?: ""
                currentSensitivity = intent?.getFloatExtra(EXTRA_SENSITIVITY, 0.5f) ?: 0.5f
                val edad = intent?.getIntExtra(EXTRA_EDAD, 55) ?: 55
                val alturaCm = intent?.getIntExtra(EXTRA_ALTURA_CM, 170) ?: 170
                val perfil = PerfilUsuario(edad = edad, alturaCm = alturaCm)
                algorithm = FallDetectionAlgorithm(currentSensitivity, perfil)
                android.util.Log.d("FallDetectionService", "Iniciando servicio patientId=$currentPatientId sensitivity=$currentSensitivity edad=$edad alturaCm=$alturaCm")
                startForeground(NOTIFICATION_ID, buildForegroundNotification(currentPatientId))
                registerSensors()
                acquireWakeLock()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensors()
        releaseWakeLock()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val timestamp = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                algorithm.processGyro(event.values[0], event.values[1], event.values[2], timestamp)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                sensorSampleCount++
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                if (sensorSampleCount % 50 == 0) {
                    android.util.Log.d("FallDetectionService", "Accel activo magnitude=$magnitude")
                }
                val detected = algorithm.processSample(x, y, z, timestamp)
                if (detected) {
                    android.util.Log.d("FallDetectionService", "Caída detectada magnitude=$magnitude")
                    val mag = algorithm.lastMagnitude
                    algorithm.reset()
                    onFallConfirmed(currentPatientId, mag)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No requiere acción para este algoritmo.
    }

    private fun registerSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        if (gyroscope == null) {
            android.util.Log.w("FallDetectionService", "Giroscopio no disponible en este dispositivo")
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ControlMedicamentos::FallDetectionWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // Máximo 10 minutos
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildForegroundNotification(patientId: Int): android.app.Notification {
        ensureChannel()

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificacionHelper.FALL_DETECTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fall_alert)
            .setContentTitle("Detección de caídas activa")
            .setContentText("Monitoreando movimientos para tu seguridad")
            .setOngoing(true)
            .setContentIntent(pendingContentIntent)
            .build()
    }

    private fun ensureChannel() {
        NotificacionHelper.ensureChannels(this)
    }

    private fun onFallConfirmed(patientId: Int, impactMagnitude: Float) {
        android.util.Log.d("FallDetectionService", "onFallConfirmed patientId=$patientId magnitude=$impactMagnitude")
        val alertIntent = Intent(this, FallAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(FallAlertActivity.EXTRA_PATIENT_ID, patientId)
            putExtra(FallAlertActivity.EXTRA_IMPACT_MAGNITUDE, impactMagnitude)
            putExtra(FallAlertActivity.EXTRA_CONTACT_PHONE, currentContactPhone)
        }
        startActivity(alertIntent)
    }
}
