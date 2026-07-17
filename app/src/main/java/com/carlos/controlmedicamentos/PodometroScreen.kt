package com.carlos.controlmedicamentos

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.PhysicalActivity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDuracion(seg: Long): String {
    val h = seg / 3600; val m = (seg % 3600) / 60; val s = seg % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatRitmo(distanciaMetros: Double, segundos: Long): String {
    if (distanciaMetros < 10.0 || segundos < 1L) return "--:--"
    val ritmoPorKm = (segundos / 60.0) / (distanciaMetros / 1000.0)
    return "%d:%02d min/km".format(ritmoPorKm.toInt(), ((ritmoPorKm % 1) * 60).toInt())
}

private fun decodeRuta(json: String): List<GeoPoint> {
    if (json.isBlank()) return emptyList()
    return json.split(",").mapNotNull {
        val p = it.split(":")
        if (p.size == 2) runCatching { GeoPoint(p[0].toDouble(), p[1].toDouble()) }.getOrNull() else null
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun PodometroScreen(
    pacienteId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Permissions ──────────────────────────────────────────────────────────
    var permisoActividad by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var permisoLocalizacion by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val activityPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permisoActividad = granted }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permisoLocalizacion =
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val haySensorPasos = remember {
        (context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager)
            .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) != null
    }

    // ── Service binding ───────────────────────────────────────────────────────
    var trackingService by remember { mutableStateOf<ActivityTrackingService?>(null) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                trackingService = (binder as ActivityTrackingService.TrackingBinder).getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) { trackingService = null }
        }
    }

    // Try to bind if the service is already running (0 = don't start a new one)
    LaunchedEffect(Unit) {
        val intent = Intent(context, ActivityTrackingService::class.java)
        context.bindService(intent, serviceConnection, 0)
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { context.unbindService(serviceConnection) }
        }
    }

    // Observe service state; fall back to a blank state if not bound
    val tracking by (trackingService?.state ?: kotlinx.coroutines.flow.flowOf(TrackingState()))
        .collectAsState(initial = TrackingState())

    // ── Tabs ─────────────────────────────────────────────────────────────────
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    var mapaFullscreen  by remember { mutableStateOf(false) }

    // ── History ───────────────────────────────────────────────────────────────
    val actividades by remember(pacienteId) {
        database.physicalActivityDao().observarPorPaciente(pacienteId)
    }.collectAsState(initial = emptyList())

    // ── Pending summary dialog (shown after stop) ─────────────────────────────
    var sesionGuardada by remember { mutableStateOf<TrackingState?>(null) }

    // Detect when service finishes (activo → false) and show summary
    var prevActivo by remember { mutableStateOf(false) }
    LaunchedEffect(tracking.activo) {
        if (prevActivo && !tracking.activo && tracking.duracionSegundos > 0 && !tracking.discarded) {
            sesionGuardada = tracking
        }
        prevActivo = tracking.activo
    }

    // ── OSMDroid live map ─────────────────────────────────────────────────────
    fun makeMapView(): MapView {
        val cacheBase = File(context.cacheDir, "osmdroid").also { it.mkdirs() }
        val tileCache = File(context.cacheDir, "osmdroid/tiles").also { it.mkdirs() }
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            userAgentValue    = context.packageName
            osmdroidBasePath  = cacheBase
            osmdroidTileCache = tileCache
        }
        return MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }
    // mapView       → used inline in tab 2 (bicicleta)
    // mapViewFull   → used in the fullscreen dialog (all types)
    val mapView     = remember { makeMapView() }
    val mapViewFull = remember { makeMapView() }

    val polyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 8f
        }
    }
    val polylineFull = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 8f
        }
    }

    fun applyRouteToMap(mv: MapView, pl: Polyline, pts: List<GeoPoint>) {
        mv.overlays.remove(pl)
        if (pts.isNotEmpty()) {
            pl.setPoints(ArrayList(pts))
            mv.overlays.add(pl)
            mv.controller.setCenter(pts.last())
        }
        mv.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume(); mapViewFull.onResume()
        onDispose { mapView.onPause(); mapViewFull.onPause() }
    }

    // When service (re)connects, restore the current route on the fresh MapView
    LaunchedEffect(trackingService) {
        val svc = trackingService ?: return@LaunchedEffect
        val pts = svc.state.value.rutaGps
        if (pts.isNotEmpty()) {
            applyRouteToMap(mapView, polyline, pts)
            mapView.controller.setZoom(17.0)
            applyRouteToMap(mapViewFull, polylineFull, pts)
            mapViewFull.controller.setZoom(17.0)
        }
    }

    LaunchedEffect(tracking.rutaGps) {
        applyRouteToMap(mapView, polyline, tracking.rutaGps)
        applyRouteToMap(mapViewFull, polylineFull, tracking.rutaGps)
    }

    // Center map when opening any tab with GPS map
    LaunchedEffect(tabSeleccionado, permisoLocalizacion) {
        if (tabSeleccionado in 0..2 && tracking.rutaGps.isEmpty()) {
            if (permisoLocalizacion) {
                try {
                    LocationServices.getFusedLocationProviderClient(context).lastLocation
                        .addOnSuccessListener { loc ->
                            val center = if (loc != null) GeoPoint(loc.latitude, loc.longitude)
                                         else GeoPoint(12.1364, -86.2818)
                            for (mv in listOf(mapView, mapViewFull)) {
                                mv.controller.setCenter(center)
                                mv.controller.setZoom(17.0)
                                mv.invalidate()
                            }
                        }
                } catch (_: SecurityException) {
                    val managua = GeoPoint(12.1364, -86.2818)
                    for (mv in listOf(mapView, mapViewFull)) {
                        mv.controller.setCenter(managua)
                        mv.controller.setZoom(14.0)
                        mv.invalidate()
                    }
                }
            } else {
                val managua = GeoPoint(12.1364, -86.2818)
                for (mv in listOf(mapView, mapViewFull)) {
                    mv.controller.setCenter(managua)
                    mv.controller.setZoom(12.0)
                    mv.invalidate()
                }
            }
        }
    }

    // ── Service control helpers ───────────────────────────────────────────────
    fun startService(tipo: String) {
        val intent = Intent(context, ActivityTrackingService::class.java).apply {
            putExtra("tipo", tipo)
        }
        ContextCompat.startForegroundService(context, intent)
        // Bind after starting
        context.bindService(
            Intent(context, ActivityTrackingService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun pauseResume() {
        if (trackingService != null) {
            trackingService!!.togglePause()
        } else {
            // Fallback via intent
            context.startService(Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_PAUSE_RESUME
            })
        }
    }

    fun stopService() {
        trackingService?.stopTracking()
    }

    fun discardService() {
        if (trackingService != null) {
            trackingService!!.stopTrackingDiscard()
        } else {
            context.startService(Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_STOP_DISCARD
            })
        }
    }

    // ── Summary / Save dialog ─────────────────────────────────────────────────
    sesionGuardada?.let { finished ->
        val ruta = remember(finished.rutaGps) { finished.rutaGps }
        val rutaJson = ruta.joinToString(",") { "${it.latitude}:${it.longitude}" }

        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(
                    text = when (finished.tipo) {
                        "correr"    -> "Resumen · Carrera"
                        "bicicleta" -> "Resumen · Bicicleta"
                        else        -> "Resumen · Caminata"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBlock(formatDuracion(finished.duracionSegundos), "Tiempo activo")
                        StatBlock("%.2f km".format(finished.distanciaMetros / 1000.0), "Distancia")
                        StatBlock("${finished.calorias} kcal", "Calorías")
                    }
                    if (finished.pasos > 0) {
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBlock("${finished.pasos}", "Pasos")
                            StatBlock(formatRitmo(finished.distanciaMetros, finished.duracionSegundos), "Ritmo")
                        }
                    }
                    if (finished.altitudMaxMetros > 0.0 || finished.altitudInicioMetros > 0.0) {
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            if (finished.altitudInicioMetros > 0.0)
                                StatBlock("%.0f m".format(finished.altitudInicioMetros), "Altitud inicio")
                            if (finished.altitudMaxMetros > 0.0)
                                StatBlock("%.0f m".format(finished.altitudMaxMetros), "Altitud máxima")
                        }
                        if (finished.desnivelPositivoMetros > 0.0 || finished.desnivelNegativoMetros > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (finished.desnivelPositivoMetros > 0.0)
                                    StatBlock("+%.0f m".format(finished.desnivelPositivoMetros), "Ascenso total")
                                if (finished.desnivelNegativoMetros > 0.0)
                                    StatBlock("-%.0f m".format(finished.desnivelNegativoMetros), "Descenso total")
                            }
                        }
                    }
                    if (ruta.isNotEmpty()) {
                        HorizontalDivider()
                        val dialogMap = remember {
                            MapView(context).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(false)
                                val line = Polyline().apply {
                                    outlinePaint.color = android.graphics.Color.parseColor("#F44336")
                                    outlinePaint.strokeWidth = 7f
                                    setPoints(ArrayList(ruta))
                                }
                                overlays.add(line)
                                if (ruta.isNotEmpty()) {
                                    controller.setCenter(ruta.first())
                                    controller.setZoom(14.0)
                                }
                            }
                        }
                        DisposableEffect(Unit) { dialogMap.onResume(); onDispose { dialogMap.onPause() } }
                        AndroidView(factory = { dialogMap }, modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val longPaso = if (finished.tipo == "correr") 1.2 else 0.762
                        val calPaso  = if (finished.tipo == "correr") 0.07 else 0.04
                        database.physicalActivityDao().insertar(
                            PhysicalActivity(
                                patientId        = pacienteId,
                                tipo             = finished.tipo,
                                fechaInicio      = finished.fechaInicio,
                                fechaFin         = System.currentTimeMillis(),
                                pasos            = finished.pasos,
                                distanciaMetros  = if (finished.tipo == "bicicleta") finished.distanciaMetros
                                                   else finished.pasos * longPaso,
                                duracionSegundos = finished.duracionSegundos,
                                calorias         = if (finished.tipo == "bicicleta") finished.calorias
                                                   else (finished.pasos * calPaso).toInt(),
                                rutaJson             = rutaJson,
                                altitudInicioMetros    = finished.altitudInicioMetros,
                                altitudMaxMetros       = finished.altitudMaxMetros,
                                desnivelPositivoMetros = finished.desnivelPositivoMetros,
                                desnivelNegativoMetros = finished.desnivelNegativoMetros
                            )
                        )
                    }
                    sesionGuardada = null
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                )
                ) { Text("Guardar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Button(
                    onClick = { sesionGuardada = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor   = Color.Black
                    )
                ) { Text("Descartar", fontWeight = FontWeight.Bold) }
            }
        )
    }

    // ── Fullscreen map dialog ─────────────────────────────────────────────────
    if (mapaFullscreen) {
        Dialog(
            onDismissRequest = { mapaFullscreen = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
                AndroidView(factory = { mapViewFull }, modifier = Modifier.fillMaxSize())
                if (tracking.activo) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xCC000000),
                            contentColor = Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatBlock(formatDuracion(tracking.duracionSegundos), "Tiempo")
                                StatBlock("%.2f km".format(tracking.distanciaMetros / 1000.0), "Distancia")
                                if (tracking.tipo == "bicicleta")
                                    StatBlock("%.1f km/h".format(tracking.velocidadKmh), "Velocidad")
                                else
                                    StatBlock("${tracking.pasos}", "Pasos")
                                StatBlock("${tracking.calorias} kcal", "Calorías")
                            }
                            // Fila 1: Pausar / Reanudar
                            if (!tracking.pausado) {
                                OutlinedButton(
                                    onClick = { pauseResume() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pausar") }
                            } else {
                                Button(onClick = { pauseResume() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Reanudar")
                                }
                            }
                            // Fila 2: Finalizar | Descartar
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { stopService() }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Finalizar") }
                                Button(
                                    onClick = { discardService() }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                                ) { Icon(Icons.Default.Close, null); Spacer(Modifier.width(4.dp)); Text("Descartar") }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { mapaFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar mapa", tint = Color.White)
                }
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    val podometroScrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(podometroScrollState), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Actividad Física",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        ScrollableTabRow(selectedTabIndex = tabSeleccionado, edgePadding = 0.dp) {
            Tab(selected = tabSeleccionado == 0, onClick = { tabSeleccionado = 0 },
                icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                text = { Text("Caminar") })
            Tab(selected = tabSeleccionado == 1, onClick = { tabSeleccionado = 1 },
                icon = { Icon(Icons.Default.DirectionsRun, contentDescription = null) },
                text = { Text("Correr") })
            Tab(selected = tabSeleccionado == 2, onClick = { tabSeleccionado = 2 },
                icon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                text = { Text("Bicicleta") })
            Tab(selected = tabSeleccionado == 3, onClick = { tabSeleccionado = 3 },
                text = { Text("Registros") })
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Tabs 0 & 1: Pedometer ────────────────────────────────────────────
        if (tabSeleccionado == 0 || tabSeleccionado == 1) {
            val tipoTab    = if (tabSeleccionado == 0) "caminar" else "correr"
            val tituloTipo = if (tabSeleccionado == 0) "Caminata" else "Carrera"
            val distSesion = tracking.distanciaMetros

            if (!permisoActividad) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Se necesita permiso de actividad física para contar los pasos.")
                        Button(
                            onClick = { activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Conceder permiso") }
                    }
                }
            } else if (!haySensorPasos) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Este dispositivo no tiene sensor de podómetro.", modifier = Modifier.padding(16.dp))
                }
            } else if (tracking.activo && tracking.tipo != tipoTab) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("Hay una sesión de ${tracking.tipo} en curso. Finalízala primero.", modifier = Modifier.padding(12.dp))
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status chip
                        if (tracking.activo) {
                            val chipText  = if (tracking.pausado) "⏸ PAUSADO" else "● EN CURSO"
                            val chipColor = if (tracking.pausado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            Surface(shape = MaterialTheme.shapes.small, color = chipColor.copy(alpha = 0.2f)) {
                                Text(chipText, color = chipColor, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }

                        // Big counter
                        Text(
                            text = if (tracking.activo) "${tracking.pasos}" else "0",
                            fontSize = 72.sp, fontWeight = FontWeight.Bold,
                            color = if (tracking.activo && !tracking.pausado) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("pasos", fontSize = 18.sp)
                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBlock("%.2f km".format(distSesion / 1000.0), "Distancia")
                            StatBlock(formatDuracion(tracking.duracionSegundos), "Tiempo")
                            if (tipoTab == "correr") {
                                StatBlock(formatRitmo(distSesion, tracking.duracionSegundos), "Ritmo")
                            } else {
                                StatBlock("${tracking.calorias} kcal", "Calorías")
                            }
                        }
                        HorizontalDivider()

                        // Controls
                        if (!tracking.activo) {
                            Button(
                                onClick = { startService(tipoTab) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp)); Text("Iniciar $tituloTipo")
                            }
                        } else {
                            // Fila 1: Pausar / Reanudar
                            if (!tracking.pausado) {
                                OutlinedButton(onClick = { pauseResume() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(Modifier.width(4.dp)); Text("Pausar")
                                }
                            } else {
                                Button(onClick = { pauseResume() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp)); Text("Reanudar")
                                }
                            }
                            // Fila 2: Finalizar | Descartar
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { stopService() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(Modifier.width(4.dp)); Text("Finalizar")
                                }
                                Button(
                                    onClick = { discardService() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(Modifier.width(4.dp)); Text("Descartar")
                                }
                            }
                        }

                        TextButton(onClick = { tabSeleccionado = 3 }) { Text("Ver registros guardados") }
                    }
                }
            }
            // ── GPS map for walking/running ──────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recorrido GPS", fontWeight = FontWeight.Medium)
                        IconButton(onClick = { mapaFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa")
                        }
                    }
                    if (!permisoLocalizacion) {
                        Text("Activa la ubicación para registrar el recorrido.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(
                            onClick = {
                                locationPermLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        ) { Text("Conceder permiso de ubicación") }
                    } else {
                        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(250.dp))
                        if (tracking.activo && tracking.tipo == tipoTab && tracking.rutaGps.isEmpty()) {
                            Text("Esperando señal GPS...", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (tracking.activo && tracking.tipo == tipoTab) {
                            Text("${tracking.rutaGps.size} puntos registrados.", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ── Tab 2: Bicycle ────────────────────────────────────────────────────
        if (tabSeleccionado == 2) {
            if (!permisoLocalizacion) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Se necesita permiso de ubicación para rastrear el recorrido.")
                        Button(
                            onClick = {
                                locationPermLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Conceder permiso de ubicación") }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Status chip
                        if (tracking.activo && tracking.tipo == "bicicleta") {
                            val chipText  = if (tracking.pausado) "⏸ PAUSADO" else "● EN CURSO"
                            val chipColor = if (tracking.pausado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            Surface(shape = MaterialTheme.shapes.small, color = chipColor.copy(alpha = 0.2f)) {
                                Text(chipText, color = chipColor, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBlock("%.2f km".format(tracking.distanciaMetros / 1000.0), "Distancia")
                            StatBlock("%.1f km/h".format(tracking.velocidadKmh), "Velocidad")
                            StatBlock(formatDuracion(tracking.duracionSegundos), "Tiempo")
                        }

                        // Map
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recorrido GPS", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            IconButton(onClick = { mapaFullscreen = true }) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa")
                            }
                        }
                        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(300.dp))

                        if (tracking.activo && tracking.tipo == "bicicleta" && !tracking.pausado && tracking.rutaGps.isEmpty()) {
                            Text("Esperando señal GPS...", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        // Controls
                        val biciActiva = tracking.activo && tracking.tipo == "bicicleta"
                        when {
                            !biciActiva && tracking.activo -> {
                                Card(modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Text("Hay una sesión de ${tracking.tipo} activa. Finalízala primero.", modifier = Modifier.padding(12.dp))
                                }
                            }
                            !biciActiva -> Button(
                                onClick = { startService("bicicleta") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp)); Text("Iniciar recorrido")
                            }
                            else -> Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Fila 1: Pausar / Reanudar
                                if (!tracking.pausado) {
                                    OutlinedButton(onClick = { pauseResume() }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Spacer(Modifier.width(4.dp)); Text("Pausar")
                                    }
                                } else {
                                    Button(onClick = { pauseResume() }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(4.dp)); Text("Reanudar")
                                    }
                                }
                                // Fila 2: Finalizar | Descartar
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { stopService() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(Modifier.width(4.dp)); Text("Finalizar")
                                    }
                                    Button(
                                        onClick = { discardService() }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                        Spacer(Modifier.width(4.dp)); Text("Descartar")
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { tabSeleccionado = 3 }) { Text("Ver registros guardados") }
                    }
                }
            }
        }

        // ── Tab 3: Saved records ──────────────────────────────────────────────
        if (tabSeleccionado == 3) {
            if (actividades.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Aún no hay sesiones registradas.", textAlign = TextAlign.Center)
                    }
                }
            } else {
                actividades.forEach { actividad ->
                    ActividadCard(
                        actividad = actividad,
                        onEliminar = { coroutineScope.launch { database.physicalActivityDao().eliminar(actividad.id) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al escritorio") }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ── Stat block ────────────────────────────────────────────────────────────────

@Composable
private fun StatBlock(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(etiqueta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── History card ──────────────────────────────────────────────────────────────

@Composable
private fun ActividadCard(actividad: PhysicalActivity, onEliminar: () -> Unit) {
    val tipoLabel = when (actividad.tipo) { "correr" -> "Carrera"; "bicicleta" -> "Bicicleta"; else -> "Caminata" }
    val fechaStr  = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(actividad.fechaInicio))
    val ruta      = remember(actividad.rutaJson) { decodeRuta(actividad.rutaJson) }
    var mostrarMapa by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tipoLabel, fontWeight = FontWeight.Bold)
                    Text(fechaStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (actividad.pasos > 0) Text("${actividad.pasos} pasos", fontSize = 13.sp)
                Text("%.2f km".format(actividad.distanciaMetros / 1000.0), fontSize = 13.sp)
                Text(formatDuracion(actividad.duracionSegundos), fontSize = 13.sp)
                if (actividad.calorias > 0) Text("${actividad.calorias} kcal", fontSize = 13.sp)
            }
            if (actividad.pasos > 0 && actividad.duracionSegundos > 0) {
                Text("Ritmo: ${formatRitmo(actividad.distanciaMetros, actividad.duracionSegundos)}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actividad.altitudMaxMetros > 0.0 || actividad.altitudInicioMetros > 0.0) {
                if (actividad.altitudInicioMetros > 0.0)
                    Text("Altitud inicio: %.0f m".format(actividad.altitudInicioMetros),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (actividad.altitudMaxMetros > 0.0)
                    Text("Altitud máxima: %.0f m".format(actividad.altitudMaxMetros),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (actividad.desnivelPositivoMetros > 0.0)
                    Text("Ascenso: +%.0f m".format(actividad.desnivelPositivoMetros),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (actividad.desnivelNegativoMetros > 0.0)
                    Text("Descenso: -%.0f m".format(actividad.desnivelNegativoMetros),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ruta.isNotEmpty()) {
                TextButton(onClick = { mostrarMapa = !mostrarMapa }) {
                    Text(if (mostrarMapa) "Ocultar recorrido" else "Ver recorrido en mapa")
                }
                if (mostrarMapa) {
                    val ctx = LocalContext.current
                    val histMap = remember(actividad.id) {
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                        }
                    }
                    DisposableEffect(actividad.id) {
                        histMap.onResume()
                        val line = Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#F44336")
                            outlinePaint.strokeWidth = 7f
                            setPoints(ArrayList(ruta))
                        }
                        histMap.overlays.add(line)
                        histMap.controller.setCenter(ruta.first())
                        histMap.controller.setZoom(15.0)
                        histMap.invalidate()
                        onDispose { histMap.onPause() }
                    }
                    AndroidView(factory = { histMap }, modifier = Modifier.fillMaxWidth().height(250.dp))
                }
            }
        }
    }
}
