package com.carlos.controlmedicamentos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.RegistroHidratacion
import com.carlos.controlmedicamentos.notifications.HidratacionScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HidratacionScreen(
    patientId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val inicioDelDia: Long = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val hidratacionDao = remember { database.hidratacionDao() }

    val totalHoy by hidratacionDao
        .obtenerTotalAguaHoy(patientId, inicioDelDia)
        .collectAsState(initial = 0)

    val tomasDeHoy by hidratacionDao
        .obtenerTomasDeHoy(patientId, inicioDelDia)
        .collectAsState(initial = emptyList())

    // Cargar peso del paciente para meta automática
    val paciente by database.patientProfileDao().observeById(patientId)
        .collectAsState(initial = null)

    val pesoKg = remember(paciente) {
        val p = paciente ?: return@remember 0.0
        val pesoStr = p.peso.replace(",", ".").trim()
        val valor = pesoStr.toDoubleOrNull() ?: 0.0
        if (p.pesoUnidad == "lb") valor * 0.4536 else valor
    }
    val metaAutoPorPeso = remember(pesoKg) {
        if (pesoKg > 0) (pesoKg * 35).toInt().coerceIn(1000, 5000) else 0
    }

    // Config persistida
    val prefs = remember { context.getSharedPreferences("hidratacion_prefs_$patientId", android.content.Context.MODE_PRIVATE) }
    var metaDiariaMl by remember { mutableStateOf(prefs.getInt("meta_diaria_ml", 2000)) }
    var metaManual by remember { mutableStateOf(prefs.getBoolean("meta_manual", false)) }

    // Aplicar meta auto si no es manual
    LaunchedEffect(metaAutoPorPeso, metaManual) {
        if (!metaManual && metaAutoPorPeso > 0 && metaDiariaMl != metaAutoPorPeso) {
            metaDiariaMl = metaAutoPorPeso
            prefs.edit().putInt("meta_diaria_ml", metaAutoPorPeso).apply()
        }
    }

    // Recordatorios
    val scheduler = remember { HidratacionScheduler(context) }
    val settings = remember { HidratacionScheduler.loadSettings(context) }
    var recordatoriosActivados by remember { mutableStateOf(settings.enabled) }
    var intervaloHoras by remember { mutableStateOf(settings.intervalHours) }
    var horaInicio by remember { mutableStateOf(settings.startHour) }
    var horaFin by remember { mutableStateOf(settings.endHour) }

    // Tipo de bebida seleccionado
    var tipoBebidaSeleccionado by remember { mutableStateOf("Agua") }

    // UI states
    var showMeta by remember { mutableStateOf(false) }
    var metaTemp by remember { mutableStateOf(metaDiariaMl.toString()) }
    var showConfig by remember { mutableStateOf(false) }

    val totalConsumo = totalHoy ?: 0
    val progreso = (totalConsumo.toFloat() / metaDiariaMl.toFloat()).coerceIn(0f, 1f)
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 800),
        label = "progreso"
    )

    val colorAgua = Color(0xFF29B6F6)
    val colorElectrolitos = Color(0xFF66BB6A)
    val colorTe = Color(0xFFFFB74D)
    val colorFondo = Color(0xFF0D1B2A)

    fun colorParaTipo(tipo: String) = when (tipo) {
        "Electrolitos" -> colorElectrolitos
        "Té / Infusiones" -> colorTe
        else -> colorAgua
    }

    fun iconoParaTipo(tipo: String) = when (tipo) {
        "Electrolitos" -> "⚡"
        "Té / Infusiones" -> "🍵"
        else -> "💧"
    }

    fun guardarConfig() {
        HidratacionScheduler.saveSettings(context, recordatoriosActivados, intervaloHoras, horaInicio, horaFin)
        HidratacionScheduler.savePatientInfo(context, patientId, paciente?.nombre ?: "Usuario")
        if (recordatoriosActivados) {
            scheduler.programar(patientId, paciente?.nombre ?: "Usuario")
        } else {
            scheduler.cancelar(patientId)
        }
    }

    fun registrarToma(ml: Int) {
        scope.launch {
            hidratacionDao.registrarToma(
                RegistroHidratacion(patientId = patientId, cantidadMl = ml, tipoBebida = tipoBebidaSeleccionado)
            )
            // Reiniciar temporizador de alarma tras beber
            if (recordatoriosActivados) {
                scheduler.programar(patientId, paciente?.nombre ?: "Usuario")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidratación", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showConfig = !showConfig }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configuración", tint = colorAgua)
                    }
                    TextButton(onClick = { metaTemp = metaDiariaMl.toString(); showMeta = true }) {
                        Text("Meta", color = colorAgua)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1628))
            )
        },
        containerColor = colorFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF0D2137), Color(0xFF0A1628))))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ====== SECCIÓN A: Dashboard ======
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 18f, cap = StrokeCap.Round)
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter = false, style = stroke
                    )
                    if (progresoAnimado > 0f) drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF0288D1), Color(0xFF29B6F6), Color(0xFF4FC3F7))
                        ),
                        startAngle = 135f,
                        sweepAngle = (270f * progresoAnimado).coerceAtLeast(1f),
                        useCenter = false, style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = colorAgua, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalConsumo ml", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("de $metaDiariaMl ml", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text(
                        "${(progreso * 100).toInt()}%",
                        color = if (progreso >= 1f) Color(0xFF66BB6A) else colorAgua,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (totalConsumo >= metaDiariaMl) {
                Text("¡Meta diaria alcanzada!", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Text("Te faltan ${metaDiariaMl - totalConsumo} ml para tu meta", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }

            if (pesoKg > 0 && !metaManual) {
                Text(
                    "Meta auto: ${pesoKg.toInt()} kg × 35 ml = $metaAutoPorPeso ml",
                    color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ====== SECCIÓN B: Tipo de Bebida ======
            Text("Tipo de bebida (puedes combinar varios)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Triple("Agua", "💧", colorAgua),
                    Triple("Electrolitos", "⚡", colorElectrolitos),
                    Triple("Té / Infusiones", "🍵", colorTe)
                ).forEach { (tipo, emoji, color) ->
                    val selected = tipoBebidaSeleccionado == tipo
                    FilterChip(
                        selected = selected,
                        onClick = { tipoBebidaSeleccionado = tipo },
                        label = { Text("$emoji $tipo", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.25f),
                            selectedLabelColor = color,
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color.White.copy(alpha = 0.15f),
                            selectedBorderColor = color.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ====== Botones rápidos ======
            Text("Registrar toma rápida", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val colorBtn = colorParaTipo(tipoBebidaSeleccionado)
                listOf(150 to "Taza\n150ml", 250 to "Vaso\n250ml", 500 to "Botella\n500ml", 1000 to "Litro\n1000ml").forEach { (ml, label) ->
                    Button(
                        onClick = { registrarToma(ml) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorBtn.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(62.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(iconoParaTipo(tipoBebidaSeleccionado), fontSize = 14.sp)
                            Text(label, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo personalizado
            var cantidadPersonalizada by remember { mutableStateOf("") }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cantidadPersonalizada,
                    onValueChange = { cantidadPersonalizada = it.filter { c -> c.isDigit() } },
                    label = { Text("ml personalizado", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorAgua, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = {
                        val ml = cantidadPersonalizada.toIntOrNull()
                        if (ml != null && ml > 0) { registrarToma(ml); cantidadPersonalizada = "" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorParaTipo(tipoBebidaSeleccionado)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = cantidadPersonalizada.isNotBlank()
                ) {
                    Text("+ Añadir", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ====== SECCIÓN C: Configuración (plegable) ======
            AnimatedVisibility(visible = showConfig) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Recordatorios de Hidratación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(10.dp))

                        // Switch maestro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Activar recordatorios", color = Color.White, fontSize = 13.sp)
                                Text("Te aviso para que no olvides beber", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = recordatoriosActivados,
                                onCheckedChange = { recordatoriosActivados = it; guardarConfig() },
                                colors = SwitchDefaults.colors(checkedThumbColor = colorAgua, checkedTrackColor = colorAgua.copy(0.4f))
                            )
                        }

                        AnimatedVisibility(visible = recordatoriosActivados) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                // Frecuencia
                                Text("Frecuencia:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1 to "Cada hora", 2 to "Cada 2h", 3 to "Cada 3h", 4 to "Cada 4h").forEach { (h, label) ->
                                        FilterChip(
                                            selected = intervaloHoras == h,
                                            onClick = { intervaloHoras = h; guardarConfig() },
                                            label = { Text(label, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colorAgua.copy(0.3f), selectedLabelColor = colorAgua
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                // Horas activas
                                Text("Horas activas:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Desde", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    // Hora inicio
                                    var expandedInicio by remember { mutableStateOf(false) }
                                    Box {
                                        FilterChip(
                                            selected = true,
                                            onClick = { expandedInicio = true },
                                            label = { Text(String.format("%02d:00", horaInicio), fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colorAgua.copy(0.2f), selectedLabelColor = colorAgua
                                            )
                                        )
                                        DropdownMenu(expanded = expandedInicio, onDismissRequest = { expandedInicio = false }) {
                                            (5..12).forEach { h ->
                                                DropdownMenuItem(
                                                    text = { Text(String.format("%02d:00", h)) },
                                                    onClick = { horaInicio = h; expandedInicio = false; guardarConfig() }
                                                )
                                            }
                                        }
                                    }
                                    Text("hasta", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    // Hora fin
                                    var expandedFin by remember { mutableStateOf(false) }
                                    Box {
                                        FilterChip(
                                            selected = true,
                                            onClick = { expandedFin = true },
                                            label = { Text(String.format("%02d:00", horaFin), fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colorAgua.copy(0.2f), selectedLabelColor = colorAgua
                                            )
                                        )
                                        DropdownMenu(expanded = expandedFin, onDismissRequest = { expandedFin = false }) {
                                            (18..23).forEach { h ->
                                                DropdownMenuItem(
                                                    text = { Text(String.format("%02d:00", h)) },
                                                    onClick = { horaFin = h; expandedFin = false; guardarConfig() }
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

            Spacer(modifier = Modifier.height(16.dp))

            // ====== Resumen por tipo de bebida ======
            if (tomasDeHoy.isNotEmpty()) {
                val desglose = tomasDeHoy.groupBy { it.tipoBebida }
                    .mapValues { entry -> entry.value.sumOf { it.cantidadMl } }
                    .toList()
                    .sortedByDescending { it.second }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Desglose del día", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        desglose.forEach { (tipo, ml) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(iconoParaTipo(tipo), fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(tipo, color = colorParaTipo(tipo), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("$ml ml", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ====== Historial del día ======
            Text(
                "Tomas de hoy (${tomasDeHoy.size})",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (tomasDeHoy.isEmpty()) {
                Text(
                    "No has registrado ninguna toma hoy.\n¡Empieza a hidratarte!",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                tomasDeHoy.forEach { toma ->
                    val tomaColor = colorParaTipo(toma.tipoBebida)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137).copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(tomaColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(iconoParaTipo(toma.tipoBebida), fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("${toma.cantidadMl} ml", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(
                                        "${toma.tipoBebida} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(toma.timestamp))}",
                                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = { scope.launch { hidratacionDao.eliminarToma(toma.id) } }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ====== Diálogo de Meta ======
    if (showMeta) {
        AlertDialog(
            onDismissRequest = { showMeta = false },
            title = { Text("Meta diaria de hidratación") },
            text = {
                Column {
                    if (pesoKg > 0) {
                        Text(
                            "Recomendación automática: $metaAutoPorPeso ml (${pesoKg.toInt()} kg × 35 ml/kg)",
                            color = colorAgua, fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = metaManual,
                                onCheckedChange = { metaManual = it; prefs.edit().putBoolean("meta_manual", it).apply() },
                                colors = CheckboxDefaults.colors(checkedColor = colorAgua)
                            )
                            Text("Usar meta manual personalizada", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = metaTemp,
                        onValueChange = { metaTemp = it.filter { c -> c.isDigit() } },
                        label = { Text("Mililitros (ml)") },
                        singleLine = true,
                        enabled = metaManual || pesoKg <= 0,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorAgua, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedLabelColor = colorAgua, unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            disabledBorderColor = Color.White.copy(alpha = 0.15f),
                            disabledTextColor = Color.White.copy(alpha = 0.4f),
                            disabledLabelColor = Color.White.copy(alpha = 0.3f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1500, 2000, 2500, 3000).forEach { ml ->
                            FilterChip(
                                selected = metaTemp == ml.toString(),
                                onClick = { metaTemp = ml.toString() },
                                label = { Text("${ml}ml", fontSize = 11.sp) },
                                enabled = metaManual || pesoKg <= 0
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ml = metaTemp.toIntOrNull()
                    if (ml != null && ml >= 500) {
                        metaDiariaMl = ml
                        prefs.edit().putInt("meta_diaria_ml", ml).apply()
                        if (ml != metaAutoPorPeso) {
                            metaManual = true
                            prefs.edit().putBoolean("meta_manual", true).apply()
                        }
                    }
                    showMeta = false
                }) { Text("Guardar", color = colorAgua) }
            },
            dismissButton = {
                TextButton(onClick = { showMeta = false }) { Text("Cancelar") }
            },
            containerColor = Color(0xFF0D2137)
        )
    }
}
