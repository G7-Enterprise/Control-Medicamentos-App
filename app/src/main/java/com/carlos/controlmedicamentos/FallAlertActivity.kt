package com.carlos.controlmedicamentos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.FallAlert
import com.carlos.controlmedicamentos.data.local.FALL_STATUS_CONFIRMED
import com.carlos.controlmedicamentos.data.local.FALL_STATUS_DISMISSED
import com.carlos.controlmedicamentos.fall.FallEmergencyNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Actividad de pantalla completa que se muestra cuando se detecta una posible caída.
 * Reproduce una alarma, vibra y muestra un contador regresivo. Si el usuario no cancela,
 * la caída se confirma y se guarda en la base de datos.
 */
class FallAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PATIENT_ID = "EXTRA_PATIENT_ID"
        const val EXTRA_IMPACT_MAGNITUDE = "EXTRA_IMPACT_MAGNITUDE"
        const val EXTRA_CONTACT_PHONE = "EXTRA_CONTACT_PHONE"
        const val EXTRA_ALARM_SOUND_URI = "EXTRA_ALARM_SOUND_URI"
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val COUNTDOWN_SECONDS = 30
        const val ALARM_DURATION_MINUTES = 15
        const val ALARM_DURATION_MS = ALARM_DURATION_MINUTES * 60 * 1000L
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmStopTimer: CountDownTimer? = null
    private var previousAlarmVolume: Int = -1
    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        CoroutineScope(Dispatchers.IO).launch {
            sendAlerts()
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private var emergencyPhone: String = ""
    private var patientId: Int = 0
    private var impactMagnitude: Float = 0f
    private var simulatedLatitude: Double? = null
    private var simulatedLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        acquireWakeLock()

        patientId = intent.getIntExtra(EXTRA_PATIENT_ID, 0)
        impactMagnitude = intent.getFloatExtra(EXTRA_IMPACT_MAGNITUDE, 0f)
        emergencyPhone = intent.getStringExtra(EXTRA_CONTACT_PHONE) ?: ""
        simulatedLatitude = if (intent.hasExtra(EXTRA_LATITUDE)) intent.getDoubleExtra(EXTRA_LATITUDE, 0.0) else null
        simulatedLongitude = if (intent.hasExtra(EXTRA_LONGITUDE)) intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0) else null
        playAlarm()
        startVibration()

        setContent {
            MaterialTheme {
                FallAlertScreen(
                    secondsTotal = COUNTDOWN_SECONDS,
                    onDismiss = { dismissAlert() },
                    onAutoFire = { autoFireEmergency() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        stopVibration()
        releaseWakeLock()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "ControlMedicamentos::FallAlertWakeLock"
            )
            // Mantener pantalla encendida durante todo el countdown + margen
            wakeLock?.acquire(COUNTDOWN_SECONDS * 1000L + 10_000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAlarm() {
        try {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.fall_alert_siren)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                start()
            }
            if (mediaPlayer == null) {
                android.util.Log.e("FallAlertActivity", "MediaPlayer.create devolvió null para fall_alert_siren")
            }
            maximizeAlarmVolume()
            startAlarmStopTimer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            alarmStopTimer?.cancel()
            alarmStopTimer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            restoreAlarmVolume()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun maximizeAlarmVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Guardar volúmenes actuales
            previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            
            // Maximizar todos los streams de audio relevantes
            val streams = listOf(
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_MUSIC
            )
            
            streams.forEach { stream ->
                val maxVolume = audioManager.getStreamMaxVolume(stream)
                audioManager.setStreamVolume(stream, maxVolume, 0)
            }
            
            // Forzar modo de alarma para máxima prioridad
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreAlarmVolume() {
        try {
            if (previousAlarmVolume >= 0) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0)
                previousAlarmVolume = -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAlarmStopTimer() {
        alarmStopTimer = object : CountDownTimer(ALARM_DURATION_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                // No se requiere acción en cada tick
            }
            override fun onFinish() {
                stopAlarm()
            }
        }.start()
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            // Patrón agresivo: ON 800ms / OFF 200ms en loop continuo
            val pattern = longArrayOf(0, 800, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissAlert() {
        saveAlert(FALL_STATUS_DISMISSED)
        stopAlarm()
        stopVibration()
        finish()
    }

    fun autoFireEmergency() {
        android.util.Log.d("FallAlertActivity", "Dead Man's Switch: tiempo agotado, disparando emergencia automática")
        saveAlert(FALL_STATUS_CONFIRMED)
        stopAlarm()
        stopVibration()
        if (emergencyPhone.isNotBlank()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                CoroutineScope(Dispatchers.IO).launch {
                    sendAlerts()
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                requestSmsPermission.launch(Manifest.permission.SEND_SMS)
            }
        } else {
            finish()
        }
    }

    private fun confirmAlert() {
        saveAlert(FALL_STATUS_CONFIRMED)
        stopAlarm()
        stopVibration()
        if (emergencyPhone.isNotBlank()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                CoroutineScope(Dispatchers.IO).launch {
                    sendAlerts()
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                requestSmsPermission.launch(Manifest.permission.SEND_SMS)
            }
        } else {
            finish()
        }
    }

    private fun saveAlert(status: String) {
        val location = getLastKnownLocation()
        val db = AppDatabase.getDatabase(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.fallAlertDao().insert(
                    FallAlert(
                        patientId = patientId,
                        detectedAt = System.currentTimeMillis(),
                        confirmedAt = System.currentTimeMillis(),
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        impactMagnitude = impactMagnitude,
                        status = status
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (simulatedLatitude != null && simulatedLongitude != null) {
            return Location("simulated").apply {
                latitude = simulatedLatitude!!
                longitude = simulatedLongitude!!
                accuracy = 10.0f
                time = System.currentTimeMillis()
            }
        }
        
        return try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            
            if (!isGpsEnabled && !isNetworkEnabled) {
                android.util.Log.w("FallAlertActivity", "GPS y red desactivados")
                return null
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.w("FallAlertActivity", "Sin permiso de ubicación")
                return null
            }
            
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            }
            
            if (bestLocation == null && isGpsEnabled) {
                android.util.Log.d("FallAlertActivity", "No hay última ubicación conocida, solicitando actualización...")
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
                    override fun onLocationChanged(location: Location) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    @Suppress("DEPRECATION")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                }, Looper.getMainLooper())
            }
            
            bestLocation
        } catch (e: Exception) {
            android.util.Log.e("FallAlertActivity", "Error obteniendo ubicación", e)
            null
        }
    }

    private fun sendAlerts() {
        try {
            android.util.Log.d("FallAlertActivity", "sendAlerts called")
            if (emergencyPhone.isBlank()) return

            val ctx = applicationContext
            val location = getLastKnownLocation()
            val customMessage = FallEmergencyNotifier.loadCustomMessage(ctx)
            val message = FallEmergencyNotifier.buildEmergencyMessage(customMessage, location, impactMagnitude)
            val phones = emergencyPhone.split(",").map { it.trim() }.filter { it.isNotBlank() }

            android.util.Log.d("FallAlertActivity", "emergencyPhone: '$emergencyPhone'")
            android.util.Log.d("FallAlertActivity", "Final message: $message")

            // 1) Enviar SMS automático si hay permiso (no requiere interacción del usuario)
            val smsEnviados = if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                FallEmergencyNotifier.sendSmsAlert(ctx, phones, message)
            } else {
                0
            }

            // 2) Pre-llenar WhatsApp para cada contacto (el usuario debe pulsar enviar en WhatsApp)
            FallEmergencyNotifier.sendWhatsAppAlert(ctx, phones, message)

            val mensajeToast = if (smsEnviados > 0) {
                "Alerta enviada por SMS a $smsEnviados contacto(s). WhatsApp preparado para enviar."
            } else {
                "WhatsApp preparado para enviar. Concede permiso SMS para envío automático."
            }
            Toast.makeText(ctx, mensajeToast, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("FallAlertActivity", "Error in sendAlerts", e)
            e.printStackTrace()
            Toast.makeText(applicationContext, "No se pudo enviar la alerta de emergencia", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun FallAlertScreen(
    secondsTotal: Int,
    onDismiss: () -> Unit,
    onAutoFire: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(secondsTotal) }

    // Dead Man's Switch: al expirar dispara emergencia automática
    LaunchedEffect(Unit) {
        object : CountDownTimer(secondsTotal * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = ((millisUntilFinished / 1000) + 1).toInt()
            }
            override fun onFinish() {
                secondsRemaining = 0
                onAutoFire()
            }
        }.start()
    }

    // Color de fondo parpadea rojo más intenso cuando quedan ≤10s
    val urgent = secondsRemaining <= 10
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = if (urgent) 0.7f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )
    val bgColor by animateColorAsState(
        targetValue = if (urgent) Color(0xFFB71C1C) else Color(0xFF8B0000),
        animationSpec = tween(500),
        label = "bgColor"
    )

    // Escala pulsante del botón para llamar la atención
    val btnScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF4A0000), bgColor, Color(0xFF4A0000))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CABECERA ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Alerta",
                    tint = Color.Yellow,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "POSIBLE CAÍDA DETECTADA",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            // --- COUNTDOWN PROMINENTE ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$secondsRemaining",
                    color = if (urgent) Color.Yellow else Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (urgent) "¡ALERTA EN SEGUNDOS!" else "segundos para enviar alerta de emergencia",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            // --- BOTÓN GIGANTE ESTOY BIEN ---
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .scale(btnScale)
            ) {
                Text(
                    text = "ESTOY BIEN\nCANCELAR ALERTA",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    lineHeight = 36.sp
                )
            }
        }
    }
}
