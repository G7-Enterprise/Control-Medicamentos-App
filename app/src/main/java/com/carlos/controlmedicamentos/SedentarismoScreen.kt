package com.carlos.controlmedicamentos

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.ConfigSedentarismo
import com.carlos.controlmedicamentos.data.local.RegistroSedentarismo
import com.carlos.controlmedicamentos.data.local.SedentarismoDao
import com.carlos.controlmedicamentos.notifications.SedentarismoScheduler
import com.carlos.controlmedicamentos.notifications.HorariosEspecialesScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SedentarismoScreen(
    patientId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val colorVerde = Color(0xFF66BB6A)
    val colorFondo = Color(0xFF0D1A0F)

    val configFlow by db.sedentarismoDao().observarConfig(patientId).collectAsState(initial = null)
    val config = configFlow ?: ConfigSedentarismo(patientId = patientId)

    fun guardar(nuevo: ConfigSedentarismo) {
        scope.launch {
            db.sedentarismoDao().guardarConfig(nuevo)
            if (nuevo.activado) {
                SedentarismoScheduler(context).programar(patientId)
                HorariosEspecialesScheduler(context).programar(patientId)
            } else {
                SedentarismoScheduler(context).cancelar(patientId)
                HorariosEspecialesScheduler(context).cancelar(patientId)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            guardar(config.copy(activado = true))
        } else {
            Toast.makeText(context, "Permiso de reconocimiento de actividad necesario", Toast.LENGTH_LONG).show()
            guardar(config.copy(activado = false))
        }
    }

    fun onToggleActivado(activado: Boolean) {
        if (activado && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            guardar(config.copy(activado = activado))
        }
    }

    fun probarSonido() {
        try {
            val mp = MediaPlayer.create(context, R.raw.heartbeat_sound)?.apply {
                setOnCompletionListener { release() }
                start()
            }
            if (mp != null) {
                scope.launch {
                    delay(5000)
                    if (mp.isPlaying) mp.stop()
                    mp.release()
                }
            }
        } catch (_: Exception) { }
    }

    val inicioHoy = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val inicioMes = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val historial by db.sedentarismoDao().observarHistorial(patientId).collectAsState(initial = emptyList())
    val mesesConHistorial by db.sedentarismoDao().observarMesesConHistorial(patientId).collectAsState(initial = emptyList())
    val mesActual = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val mesesExpandidos = remember { mutableStateMapOf<String, Boolean>() }
    val alertasHoy by db.sedentarismoDao().contarAlertasHoy(patientId, inicioHoy).collectAsState(initial = 0)

    LaunchedEffect(mesesConHistorial) {
        mesesConHistorial.forEach { mes ->
            if (mes !in mesesExpandidos) mesesExpandidos[mes] = mes == mesActual
        }
    }

    var registroDetalle by remember { mutableStateOf<RegistroSedentarismo?>(null) }
    var registroAEliminar by remember { mutableStateOf<RegistroSedentarismo?>(null) }
    var sonidoActivado by remember { mutableStateOf(SedentarismoScheduler.loadSoundEnabled(context)) }

    val movimientosHoy = historial.filter { it.timestamp >= inicioHoy && it.tipoEvento == "MOVIMIENTO_REGISTRADO" }
    val sinMovimientoHoy = historial.filter { it.timestamp >= inicioHoy && it.tipoEvento == "SIN_MOVIMIENTO" }
    val movimientosMes = historial.filter { it.timestamp >= inicioMes && it.tipoEvento == "MOVIMIENTO_REGISTRADO" }
    val sinMovimientoMes = historial.filter { it.timestamp >= inicioMes && it.tipoEvento == "SIN_MOVIMIENTO" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Sedentarismo", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF071A09))
            )
        },
        containerColor = colorFondo
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF071A09), Color(0xFF0D2010), Color(0xFF071A09))))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Panel de resumen hoy ──
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A12))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = colorVerde, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Alertas de inactividad hoy", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        Text(
                            text = "$alertasHoy",
                            color = if (alertasHoy == 0) colorVerde else Color(0xFFFFA726),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (alertasHoy == 0) "¡Excelente! Te has mantenido activo" else "Intenta levantarte y caminar un poco",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Resumen diario / mensual ──
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A12))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Resumen de actividad", color = colorVerde, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        ResumenFila("Hoy", movimientosHoy.size, movimientosHoy.sumOf { it.minutosInactivo }, sinMovimientoHoy.size)
                        HorizontalDivider(color = Color.White.copy(0.1f))
                        ResumenFila("Este mes", movimientosMes.size, movimientosMes.sumOf { it.minutosInactivo }, sinMovimientoMes.size)
                    }
                }
            }

            // ── Configuración ──
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A12))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Configuración", color = colorVerde, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Activar recordatorios", color = Color.White, fontSize = 15.sp)
                                Text("Recibirás avisos si llevas mucho tiempo sin moverte", color = Color.White.copy(0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = config.activado,
                                onCheckedChange = { onToggleActivado(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = colorVerde, checkedTrackColor = colorVerde.copy(0.4f))
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sonido de alerta", color = Color.White, fontSize = 15.sp)
                                Text("Escuchar tono al activarse una alerta", color = Color.White.copy(0.5f), fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = sonidoActivado,
                                    onCheckedChange = { sonidoActivado = it; SedentarismoScheduler.saveSoundEnabled(context, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colorVerde, checkedTrackColor = colorVerde.copy(0.4f))
                                )
                                Spacer(Modifier.width(8.dp))
                                TextButton(
                                    onClick = { probarSonido() },
                                    enabled = sonidoActivado
                                ) {
                                    Text("Probar sonido", color = if (sonidoActivado) colorVerde else Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.1f))

                        Text("Intervalo de aviso", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(60 to "1h", 120 to "2h", 180 to "3h", 240 to "4h", 300 to "5h").forEach { (min, label) ->
                                FilterChip(
                                    selected = config.limiteInactividadMinutos == min,
                                    onClick = { guardar(config.copy(limiteInactividadMinutos = min)) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorVerde.copy(0.3f),
                                        selectedLabelColor = colorVerde
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.1f))

                        Text("Horario de monitoreo", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(context, { _, h, _ -> guardar(config.copy(horaInicioMonitoreo = h)) },
                                        config.horaInicioMonitoreo, 0, true).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorVerde)
                            ) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Inicio: ${String.format("%02d:00", config.horaInicioMonitoreo)}")
                            }
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(context, { _, h, _ -> guardar(config.copy(horaFinMonitoreo = h)) },
                                        config.horaFinMonitoreo, 0, true).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7986CB))
                            ) {
                                Icon(Icons.Filled.Nightlight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Fin: ${String.format("%02d:00", config.horaFinMonitoreo)}")
                            }
                        }
                    }
                }
            }

            if (mesesConHistorial.isNotEmpty()) {
                item {
                    Text("Historial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                mesesConHistorial.forEach { mes ->
                    item(key = "historial-mes-$mes") {
                        HistorialMesSedentarismo(
                            patientId = patientId,
                            mes = mes,
                            expandido = mesesExpandidos[mes] == true,
                            sedentarismoDao = db.sedentarismoDao(),
                            inicioHoy = inicioHoy,
                            onToggle = { mesesExpandidos[mes] = !(mesesExpandidos[mes] == true) },
                            onDetalle = { registroDetalle = it },
                            onEliminar = { registroAEliminar = it }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo de detalle ──
    registroDetalle?.let { reg ->
        val (color, _, etiqueta) = when (reg.tipoEvento) {
            "ALERTA_INACTIVIDAD"   -> Triple(Color(0xFFFFA726), Icons.Filled.Warning, "Alerta de inactividad")
            "MOVIMIENTO"           -> Triple(Color(0xFF66BB6A), Icons.Filled.DirectionsWalk, "Movimiento detectado")
            "MOVIMIENTO_REGISTRADO" -> Triple(Color(0xFF43A047), Icons.Filled.DirectionsWalk, "Movimiento registrado")
            "SIN_MOVIMIENTO"       -> Triple(Color(0xFFEF5350), Icons.Filled.DoNotDisturb, "Sin movimiento")
            else -> Triple(Color(0xFF90A4AE), Icons.Filled.Info, reg.tipoEvento)
        }
        AlertDialog(
            onDismissRequest = { registroDetalle = null },
            title = { Text(etiqueta, color = color, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(reg.timestamp))}", color = Color.White)
                    if (reg.minutosInactivo > 0) {
                        Text("Tiempo: ${reg.minutosInactivo} min", color = Color.White.copy(0.8f))
                    }
                    if (reg.notas.isNotBlank()) {
                        Text("Detalle: ${reg.notas}", color = Color.White.copy(0.8f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { registroDetalle = null }) { Text("Cerrar", color = colorVerde) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        registroDetalle = null
                        registroAEliminar = reg
                    }
                ) { Text("Eliminar", color = Color(0xFFEF5350)) }
            },
            containerColor = Color(0xFF0D2137)
        )
    }

    // ── Diálogo de eliminación ──
    registroAEliminar?.let { reg ->
        AlertDialog(
            onDismissRequest = { registroAEliminar = null },
            title = { Text("Eliminar registro") },
            text = { Text("¿Seguro que quieres eliminar este registro del historial?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { db.sedentarismoDao().eliminarRegistro(reg.id) }
                        registroAEliminar = null
                    }
                ) { Text("Eliminar", color = Color(0xFFEF5350)) }
            },
            dismissButton = {
                TextButton(onClick = { registroAEliminar = null }) { Text("Cancelar") }
            },
            containerColor = Color(0xFF0D2137)
        )
    }
}

@Composable
private fun RegistroSedentarismoCard(
    reg: RegistroSedentarismo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (color, icono, etiqueta) = when (reg.tipoEvento) {
        "ALERTA_INACTIVIDAD"   -> Triple(Color(0xFFFFA726), Icons.Filled.Warning, "Alerta de inactividad")
        "MOVIMIENTO"           -> Triple(Color(0xFF66BB6A), Icons.Filled.DirectionsWalk, "Movimiento detectado")
        "MOVIMIENTO_REGISTRADO" -> Triple(Color(0xFF43A047), Icons.Filled.DirectionsWalk, "Movimiento registrado")
        "SIN_MOVIMIENTO"       -> Triple(Color(0xFFEF5350), Icons.Filled.DoNotDisturb, "Sin movimiento")
        else -> Triple(Color(0xFF90A4AE), Icons.Filled.Info, reg.tipoEvento)
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A12).copy(0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(etiqueta, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (reg.minutosInactivo > 0 && reg.tipoEvento != "MOVIMIENTO_REGISTRADO")
                        Text("${reg.minutosInactivo} min sin moverte", color = color.copy(0.8f), fontSize = 11.sp)
                    if (reg.minutosInactivo > 0 && reg.tipoEvento == "MOVIMIENTO_REGISTRADO")
                        Text("${reg.minutosInactivo} min de actividad", color = color.copy(0.8f), fontSize = 11.sp)
                    if (reg.notas.isNotBlank())
                        Text(reg.notas, color = Color.White.copy(0.5f), fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(reg.timestamp)),
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White.copy(0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ResumenFila(periodo: String, movimientos: Int, minMovimiento: Int, sinMovimiento: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(periodo, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("$movimientos", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("$minMovimiento min", color = Color.White.copy(0.6f), fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("$sinMovimiento", color = Color(0xFFFFA726), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("sin movimiento", color = Color.White.copy(0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun HistorialMesSedentarismo(
    patientId: Int,
    mes: String,
    expandido: Boolean,
    sedentarismoDao: SedentarismoDao,
    inicioHoy: Long,
    onToggle: () -> Unit,
    onDetalle: (RegistroSedentarismo) -> Unit,
    onEliminar: (RegistroSedentarismo) -> Unit
) {
    val limitesMes = remember(mes) { limitesMesSedentarismo(mes) }
    val registrosDelMes by sedentarismoDao
        .observarEnRango(patientId, limitesMes.first, limitesMes.second)
        .collectAsState(initial = emptyList())
    val diasExpandidos = remember(mes) { mutableStateMapOf<Long, Boolean>() }
    val tituloMes = remember(mes) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { isLenient = false }
            .format(SimpleDateFormat("yyyy-MM", Locale.US).parse(mes) ?: Date())
            .replaceFirstChar { it.uppercase() }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A12)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF66BB6A))
                Spacer(Modifier.width(10.dp))
                Text(tituloMes, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (expandido) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (expandido) "Cerrar mes" else "Abrir mes",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                    registrosDelMes.groupBy(::inicioDiaSedentarismo)
                        .toList()
                        .sortedByDescending { it.first }
                        .forEach { (dia, registrosDelDia) ->
                            val diaExpandido = diasExpandidos[dia] == true
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16351A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .clickable { diasExpandidos[dia] = !diaExpandido }
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFFA5D6A7), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${tituloDiaSedentarismo(dia, inicioHoy)} · ${registrosDelDia.size} registros",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            if (diaExpandido) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                            contentDescription = if (diaExpandido) "Cerrar fecha" else "Abrir fecha",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    AnimatedVisibility(visible = diaExpandido) {
                                        Column(
                                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            registrosDelDia.forEach { registro ->
                                                RegistroSedentarismoCard(
                                                    reg = registro,
                                                    onClick = { onDetalle(registro) },
                                                    onDelete = { onEliminar(registro) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}

private fun limitesMesSedentarismo(mes: String): Pair<Long, Long> {
    val calendario = Calendar.getInstance().apply {
        time = SimpleDateFormat("yyyy-MM", Locale.US).parse(mes) ?: Date()
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val inicio = calendario.timeInMillis
    calendario.add(Calendar.MONTH, 1)
    return inicio to calendario.timeInMillis - 1
}

private fun inicioDiaSedentarismo(registro: RegistroSedentarismo): Long {
    return Calendar.getInstance().apply {
        timeInMillis = registro.timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun tituloDiaSedentarismo(dia: Long, inicioHoy: Long): String {
    return when (dia) {
        inicioHoy -> "Hoy"
        inicioHoy - 86_400_000L -> "Ayer"
        else -> SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(Date(dia)).replaceFirstChar { it.uppercase() }
    }
}
