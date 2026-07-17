package com.carlos.controlmedicamentos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale

// ── Actions sent via PendingIntent from the notification ─────────────────────
const val ACTION_PAUSE_RESUME  = "com.carlos.controlmedicamentos.ACTION_PAUSE_RESUME"
const val ACTION_STOP          = "com.carlos.controlmedicamentos.ACTION_STOP"
const val ACTION_STOP_DISCARD  = "com.carlos.controlmedicamentos.ACTION_STOP_DISCARD"

// ── State shared with the UI ──────────────────────────────────────────────────
data class TrackingState(
    val activo: Boolean         = false,
    val pausado: Boolean        = false,
    val tipo: String            = "caminar",       // "caminar" | "correr" | "bicicleta"
    val pasos: Int              = 0,
    val distanciaMetros: Double = 0.0,
    val velocidadKmh: Float     = 0f,
    val duracionSegundos: Long  = 0L,
    val calorias: Int           = 0,
    val rutaGps: List<GeoPoint> = emptyList(),
    val fechaInicio: Long       = 0L,
    val discarded: Boolean      = false,
    val altitudInicioMetros: Double   = 0.0,  // altitud GPS en el primer fix
    val altitudMaxMetros: Double      = 0.0,  // altitud máxima alcanzada
    val desnivelPositivoMetros: Double = 0.0, // metros totales de ascenso
    val desnivelNegativoMetros: Double = 0.0  // metros totales de descenso
)

class ActivityTrackingService : Service() {

    // ── Binder (Bound Service) ────────────────────────────────────────────────
    inner class TrackingBinder : Binder() {
        fun getService(): ActivityTrackingService = this@ActivityTrackingService
    }

    private val binder = TrackingBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    // ── Public state (observed from UI) ──────────────────────────────────────
    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state

    // ── Internal state ────────────────────────────────────────────────────────
    private var sensorManager: SensorManager? = null
    private var sensorListener: SensorEventListener? = null
    private var locationCallback: LocationCallback? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var pasosBase       = -1
    private var kmHablados      = 0
    private var ultimaLoc: Location? = null
    private var ultimosPasos    = -1          // para detectar inactividad en podómetro
    private var autoPausaJob: Job? = null      // monitor de auto-pausa
    private var altitudPreviaMetros: Double? = null  // para calcular desnivel incremental

    // ── TTS ───────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // ── Audio focus (duck music while TTS speaks) ─────────────────────────────
    private var audioManager: AudioManager? = null
    private var pendingUtterances = 0
    private var audioFocusReq: AudioFocusRequest? = null  // API 26+

    // ── Notification ──────────────────────────────────────────────────────────
    companion object {
        const val CHANNEL_ID   = "activity_tracking_channel"
        const val NOTIF_ID     = 9001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_RESUME -> togglePause()
            ACTION_STOP         -> stopTracking()
            ACTION_STOP_DISCARD -> stopTrackingDiscard()
            else                -> {
                val tipo = intent?.getStringExtra("tipo") ?: "caminar"
                startTracking(tipo)
            }
        }
        return START_NOT_STICKY
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    fun startTracking(tipo: String) {
        _state.value = TrackingState(
            activo      = true,
            pausado     = false,
            tipo        = tipo,
            fechaInicio = System.currentTimeMillis()
        )
        pasosBase             = -1
        kmHablados            = 0
        ultimaLoc             = null
        ultimosPasos          = -1
        altitudPreviaMetros   = null

        iniciarForeground(tipo)
        startSensors(tipo)
        startTicker()
        startAutoPausa(tipo)
        speak("${labelTipo(tipo)} iniciada")
    }

    // ── Foreground start with correct type ────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun iniciarForeground(tipo: String) {
        val notif = buildNotification()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // API 34+
                val serviceType = if (tipo == "bicicleta") {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                }
                startForeground(NOTIF_ID, notif, serviceType)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> { // API 29–33
                if (tipo == "bicicleta") {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIF_ID, notif)
                }
            }
            else -> startForeground(NOTIF_ID, notif)
        }
    }

    // ── Pause / Resume ────────────────────────────────────────────────────────
    fun togglePause(esAutomatica: Boolean = false) {
        val current = _state.value
        if (!current.activo) return
        if (current.pausado) {
            _state.value = current.copy(pausado = false)
            startTicker()
            if (!esAutomatica) speak("Actividad reanudada")
        } else {
            _state.value = current.copy(pausado = true)
            tickerJob?.cancel()
            if (esAutomatica) speak("Pausado automáticamente") else speak("Actividad pausada")
        }
        updateNotification()
    }

    // ── Stop ──────────────────────────────────────────────────────────────────
    fun stopTracking() {
        speak("Entrenamiento finalizado. ${formatDistanciaHablada(_state.value.distanciaMetros)} recorridos en ${formatDuracionHablada(_state.value.duracionSegundos)}.")
        tickerJob?.cancel()
        autoPausaJob?.cancel()
        unregisterSensors()
        _state.value = _state.value.copy(activo = false, pausado = false, discarded = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun stopTrackingDiscard() {
        tickerJob?.cancel()
        autoPausaJob?.cancel()
        unregisterSensors()
        _state.value = _state.value.copy(activo = false, pausado = false, discarded = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Sensors ───────────────────────────────────────────────────────────────
    private fun startSensors(tipo: String) {
        if (tipo == "bicicleta") {
            startGps()
        } else {
            startStepSensor()
            startGpsRouteOnly()
        }
    }

    private fun startStepSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val total = it.values[0].toInt()
                    if (pasosBase == -1) { pasosBase = total; ultimosPasos = 0 }
                    val pasos = total - pasosBase
                    val current = _state.value
                    // Auto-reanuda si estaba en pausa automática y hay pasos nuevos
                    if (current.pausado && pasos > ultimosPasos) {
                        _state.value = current.copy(pausado = false)
                        startTicker()
                        speak("Reanudando")
                        updateNotification()
                    }
                    ultimosPasos = pasos
                    val longPaso = if (current.tipo == "correr") 1.2 else 0.762
                    val calPaso  = if (current.tipo == "correr") 0.07 else 0.04
                    val dist     = pasos * longPaso
                    _state.value = _state.value.copy(
                        pasos           = pasos,
                        distanciaMetros = dist,
                        calorias        = (pasos * calPaso).toInt()
                    )
                    checkKilometros(dist)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun startGpsRouteOnly() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val current = _state.value
                    if (current.pausado || !current.activo) return
                    val newPoint = GeoPoint(loc.latitude, loc.longitude)
                    val hasAlt = loc.hasAltitude()
                    val newAltInicio = if (hasAlt && current.altitudInicioMetros == 0.0) loc.altitude else current.altitudInicioMetros
                    val newAltMax = if (hasAlt && loc.altitude > current.altitudMaxMetros) loc.altitude else current.altitudMaxMetros
                    var newDesPos = current.desnivelPositivoMetros
                    var newDesNeg = current.desnivelNegativoMetros
                    if (hasAlt) {
                        altitudPreviaMetros?.let { prev ->
                            val diff = loc.altitude - prev
                            if (diff > 0.5) newDesPos += diff
                            else if (diff < -0.5) newDesNeg += (-diff)
                        }
                        altitudPreviaMetros = loc.altitude
                    }
                    _state.value = current.copy(
                        rutaGps                = current.rutaGps + newPoint,
                        altitudInicioMetros    = newAltInicio,
                        altitudMaxMetros       = newAltMax,
                        desnivelPositivoMetros = newDesPos,
                        desnivelNegativoMetros = newDesNeg
                    )
                }
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun startGps() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val current = _state.value
                    val kmh = loc.speed * 3.6f

                    // Auto-reanuda bicicleta si velocidad > 2 km/h y estaba pausada
                    if (current.pausado && kmh > 2f) {
                        _state.value = current.copy(pausado = false, velocidadKmh = kmh)
                        startTicker()
                        speak("Reanudando")
                        updateNotification()
                        ultimaLoc = loc
                        return
                    }

                    if (current.pausado) {
                        _state.value = current.copy(velocidadKmh = kmh)
                        return
                    }

                    val newPoint = GeoPoint(loc.latitude, loc.longitude)
                    val newRuta  = current.rutaGps + newPoint
                    var newDist  = current.distanciaMetros
                    ultimaLoc?.let { prev ->
                        val r = FloatArray(1)
                        Location.distanceBetween(
                            prev.latitude, prev.longitude,
                            loc.latitude, loc.longitude, r
                        )
                        if (r[0] < 100f) newDist += r[0]
                    }
                    ultimaLoc = loc
                    val cal = (newDist / 1000.0 * 35).toInt()
                    val hasAlt = loc.hasAltitude()
                    val newAltInicio = if (hasAlt && current.altitudInicioMetros == 0.0) loc.altitude else current.altitudInicioMetros
                    val newAltMax = if (hasAlt && loc.altitude > current.altitudMaxMetros) loc.altitude else current.altitudMaxMetros
                    var newDesPos = current.desnivelPositivoMetros
                    var newDesNeg = current.desnivelNegativoMetros
                    if (hasAlt) {
                        altitudPreviaMetros?.let { prev ->
                            val diff = loc.altitude - prev
                            if (diff > 0.5) newDesPos += diff
                            else if (diff < -0.5) newDesNeg += (-diff)
                        }
                        altitudPreviaMetros = loc.altitude
                    }
                    _state.value = current.copy(
                        distanciaMetros        = newDist,
                        velocidadKmh           = kmh,
                        calorias               = cal,
                        rutaGps                = newRuta,
                        altitudInicioMetros    = newAltInicio,
                        altitudMaxMetros       = newAltMax,
                        desnivelPositivoMetros = newDesPos,
                        desnivelNegativoMetros = newDesNeg
                    )
                    checkKilometros(newDist)
                }
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun unregisterSensors() {
        sensorListener?.let { sensorManager?.unregisterListener(it) }
        locationCallback?.let {
            LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it)
        }
        sensorListener    = null
        locationCallback  = null
    }

    // ── Auto-pause monitor ────────────────────────────────────────────────────
    // Podómetro: 5 s sin pasos nuevos → pausa automática
    // Bicicleta: 4 s con velocidad < 1 km/h → pausa automática
    private fun startAutoPausa(tipo: String) {
        autoPausaJob?.cancel()
        autoPausaJob = scope.launch {
            if (tipo == "bicicleta") {
                var segsBajaVelocidad = 0
                while (_state.value.activo) {
                    delay(1000L)
                    val s = _state.value
                    if (!s.activo) break
                    if (!s.pausado) {
                        if (s.velocidadKmh < 1f) {
                            segsBajaVelocidad++
                            if (segsBajaVelocidad >= 4) {
                                segsBajaVelocidad = 0
                                // Pausa automática
                                _state.value = s.copy(pausado = true)
                                tickerJob?.cancel()
                                speak("Pausado automáticamente")
                                updateNotification()
                            }
                        } else {
                            segsBajaVelocidad = 0
                        }
                    }
                }
            } else {
                // Podómetro: comprueba cada segundo si los pasos cambiaron
                var ultimoCheck = _state.value.pasos
                var segsInactivo = 0
                while (_state.value.activo) {
                    delay(1000L)
                    val s = _state.value
                    if (!s.activo) break
                    if (!s.pausado) {
                        if (s.pasos == ultimoCheck) {
                            segsInactivo++
                            if (segsInactivo >= 5) {
                                segsInactivo = 0
                                // Pausa automática
                                _state.value = s.copy(pausado = true)
                                tickerJob?.cancel()
                                speak("Pausado automáticamente")
                                updateNotification()
                            }
                        } else {
                            segsInactivo  = 0
                            ultimoCheck = s.pasos
                        }
                    } else {
                        // Reinicia el contador mientras está pausado
                        ultimoCheck = s.pasos
                        segsInactivo  = 0
                    }
                }
            }
        }
    }

    // ── Ticker: increments active time every second ───────────────────────────
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_state.value.activo && !_state.value.pausado) {
                delay(1000L)
                _state.value = _state.value.copy(duracionSegundos = _state.value.duracionSegundos + 1)
                updateNotification()
            }
        }
    }

    // ── Kilometre announcements ───────────────────────────────────────────────
    private fun checkKilometros(distanciaMetros: Double) {
        val km = (distanciaMetros / 1000.0).toInt()
        if (km > kmHablados) {
            kmHablados = km
            val durSeg = _state.value.duracionSegundos
            val min    = durSeg / 60
            val seg    = durSeg % 60
            val tiempo = if (min > 0) "$min minutos con $seg segundos" else "$seg segundos"
            speak("Kilómetro $km completado, tiempo: $tiempo")
        }
    }

    // ── TTS ───────────────────────────────────────────────────────────────────
    private fun initTts() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { releaseAudioDuck() }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { releaseAudioDuck() }
                    override fun onError(utteranceId: String?, errorCode: Int) { releaseAudioDuck() }
                })
                ttsReady = true
            }
        }
    }

    private fun requestAudioDuck() {
        if (pendingUtterances++ == 0) {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setOnAudioFocusChangeListener {}
                    .build()
                audioFocusReq = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        }
    }

    private fun releaseAudioDuck() {
        if (--pendingUtterances <= 0) {
            pendingUtterances = 0
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusReq?.let { am.abandonAudioFocusRequest(it) }
                audioFocusReq = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            requestAudioDuck()
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "tracking_$text")
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Seguimiento de actividad",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación persistente mientras se realiza seguimiento de actividad física"
            setShowBadge(false)
        }
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(channel)
    }

    private fun pauseResumeIntent(): PendingIntent {
        val intent = Intent(this, ActivityTrackingService::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopIntent(): PendingIntent {
        val intent = Intent(this, ActivityTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun discardIntent(): PendingIntent {
        val intent = Intent(this, ActivityTrackingService::class.java).apply {
            action = ACTION_STOP_DISCARD
        }
        return PendingIntent.getService(
            this, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): Notification {
        val s    = _state.value
        val dur  = formatDuracion(s.duracionSegundos)
        val dist = "%.2f km".format(s.distanciaMetros / 1000.0)
        val tipo = labelTipo(s.tipo)
        val pausaLabel = if (s.pausado) "▶ Reanudar" else "⏸ Pausar"

        val contentText = when {
            s.tipo == "bicicleta" -> "$dur · $dist · %.1f km/h · ${s.calorias} kcal".format(s.velocidadKmh)
            else                  -> "$dur · $dist · ${s.pasos} pasos · ${s.calorias} kcal"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$tipo en curso ${if (s.pausado) "(pausado)" else ""}")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(android.R.drawable.ic_media_pause, pausaLabel, pauseResumeIntent())
            .addAction(android.R.drawable.ic_media_play,  "✅ Guardar",   stopIntent())
            .addAction(android.R.drawable.ic_delete,      "🗑 Descartar", discardIntent())
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun formatDuracion(seg: Long): String {
        val h = seg / 3600; val m = (seg % 3600) / 60; val s = seg % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatDuracionHablada(seg: Long): String {
        val h = seg / 3600; val m = (seg % 3600) / 60; val s = seg % 60
        return when {
            h > 0  -> "$h horas y $m minutos"
            m > 0  -> "$m minutos y $s segundos"
            else   -> "$s segundos"
        }
    }

    private fun formatDistanciaHablada(metros: Double): String {
        val km = metros / 1000.0
        return if (km >= 1.0) "%.2f kilómetros".format(km) else "${metros.toInt()} metros"
    }

    private fun labelTipo(tipo: String) = when (tipo) {
        "correr"    -> "Carrera"
        "bicicleta" -> "Bicicleta"
        else        -> "Caminata"
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tickerJob?.cancel()
        unregisterSensors()
        super.onDestroy()
    }
}
