package com.carlos.controlmedicamentos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import android.media.MediaPlayer
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.HidratacionDao
import com.carlos.controlmedicamentos.data.local.RegistroHidratacion
import com.carlos.controlmedicamentos.notifications.HidratacionScheduler
import kotlinx.coroutines.delay
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
    var tomaAEliminar by remember { mutableStateOf<RegistroHidratacion?>(null) }
    var showAvisoElectrolitos by remember { mutableStateOf(false) }
    var litrosAviso by remember { mutableStateOf(0) }
    var sonidoActivado by remember { mutableStateOf(HidratacionScheduler.loadSoundEnabled(context)) }
    var notaTexto by remember { mutableStateOf("") }
    val notasPrefs = remember { context.getSharedPreferences("hidratacion_notas", android.content.Context.MODE_PRIVATE) }
    val mesesConHistorial by hidratacionDao
        .observarMesesConHistorial(patientId)
        .collectAsState(initial = emptyList())
    val mesActual = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val mesesExpandidos = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(mesesConHistorial) {
        mesesConHistorial.forEach { mes ->
            if (mes !in mesesExpandidos) mesesExpandidos[mes] = mes == mesActual
        }
    }

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

    fun probarSonido() {
        try {
            val mp = MediaPlayer.create(context, R.raw.water_sound)?.apply {
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

    fun registrarToma(ml: Int) {
        // Aviso de hidratación al alcanzar cada nuevo litro
        val litrosAntes = totalConsumo / 1000
        val litrosDespues = (totalConsumo + ml) / 1000
        if (litrosDespues > litrosAntes) {
            litrosAviso = litrosDespues
            showAvisoElectrolitos = true
        }
        scope.launch {
            val nota = notaTexto.trim()
            val id = hidratacionDao.registrarToma(
                RegistroHidratacion(patientId = patientId, cantidadMl = ml, tipoBebida = tipoBebidaSeleccionado)
            )
            if (nota.isNotBlank()) {
                notasPrefs.edit().putString("nota_${id}", nota).apply()
            }
            notaTexto = ""
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

            Spacer(modifier = Modifier.height(8.dp))

            // Nota opcional para la toma
            OutlinedTextField(
                value = notaTexto,
                onValueChange = { notaTexto = it },
                label = { Text("Nota (opcional)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorAgua, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )

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

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sonido de alarma", color = Color.White, fontSize = 13.sp)
                                Text("Escuchar tono al llegar el recordatorio", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = sonidoActivado,
                                    onCheckedChange = { sonidoActivado = it; HidratacionScheduler.saveSoundEnabled(context, sonidoActivado) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colorAgua, checkedTrackColor = colorAgua.copy(0.4f))
                                )
                                Spacer(Modifier.width(8.dp))
                                TextButton(
                                    onClick = { probarSonido() },
                                    enabled = sonidoActivado
                                ) {
                                    Text("Probar sonido", color = if (sonidoActivado) colorAgua else Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                                }
                            }
                        }

                        AnimatedVisibility(visible = recordatoriosActivados) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                // Frecuencia
                                Text("Frecuencia:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
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

            Text(
                "Historial",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (mesesConHistorial.isEmpty()) {
                Text(
                    "No hay registros todavía.\n¡Empieza a hidratarte!",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                mesesConHistorial.forEach { mes ->
                    HistorialMesHidratacion(
                        patientId = patientId,
                        mes = mes,
                        expandido = mesesExpandidos[mes] == true,
                        hidratacionDao = hidratacionDao,
                        notasPrefs = notasPrefs,
                        inicioDelDia = inicioDelDia,
                        onToggle = { mesesExpandidos[mes] = !(mesesExpandidos[mes] == true) },
                        onRequestDelete = { tomaAEliminar = it },
                        colorParaTipo = ::colorParaTipo,
                        iconoParaTipo = ::iconoParaTipo
                    )
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1500, 2000).forEach { ml ->
                                FilterChip(
                                    selected = metaTemp == ml.toString(),
                                    onClick = { metaTemp = ml.toString() },
                                    label = { Text("${ml}ml", fontSize = 11.sp) },
                                    enabled = metaManual || pesoKg <= 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2500, 3000).forEach { ml ->
                                FilterChip(
                                    selected = metaTemp == ml.toString(),
                                    onClick = { metaTemp = ml.toString() },
                                    label = { Text("${ml}ml", fontSize = 11.sp) },
                                    enabled = metaManual || pesoKg <= 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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

    // ====== Diálogo de eliminación ======
    if (tomaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { tomaAEliminar = null },
            title = { Text("Eliminar toma") },
            text = { Text("¿Seguro que quieres eliminar esta toma de ${tomaAEliminar?.cantidadMl} ml de ${tomaAEliminar?.tipoBebida}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tomaAEliminar?.let { scope.launch { hidratacionDao.eliminarToma(it.id); notasPrefs.edit().remove("nota_${it.id}").apply() } }
                        tomaAEliminar = null
                    }
                ) { Text("Eliminar", color = colorAgua) }
            },
            dismissButton = {
                TextButton(onClick = { tomaAEliminar = null }) { Text("Cancelar") }
            },
            containerColor = Color(0xFF0D2137)
        )
    }

    // ====== Aviso de hidratación ======
    if (showAvisoElectrolitos) {
        AlertDialog(
            onDismissRequest = { showAvisoElectrolitos = false },
            title = { Text("¡Excelente hidratación! 💧") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Has registrado ${litrosAviso} litro${if (litrosAviso > 1) "s" else ""} de líquidos.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorAgua
                    )
                    HorizontalDivider(color = colorAgua.copy(alpha = 0.4f))
                    Text(
                        text = "Considera incluir una bebida con electrolitos para reponer minerales como sodio, potasio y magnesio.",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Si es posible, elige opciones sin azúcar añadida. El consumo frecuente de azúcar refinada puede afectar tu salud y contribuir a la deshidratación.",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Consejos:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("• Agua natural es la mejor opción.", color = Color.White.copy(alpha = 0.85f))
                        Text("• Evita bebidas azucaradas y refrescos.", color = Color.White.copy(alpha = 0.85f))
                        Text("• Reponer electrolitos si sudas mucho.", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvisoElectrolitos = false }) { Text("Entendido", color = colorAgua) }
            },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = colorAgua) },
            containerColor = Color(0xFF0D2137)
        )
    }
}

@Composable
private fun HistorialMesHidratacion(
    patientId: Int,
    mes: String,
    expandido: Boolean,
    hidratacionDao: HidratacionDao,
    notasPrefs: android.content.SharedPreferences,
    inicioDelDia: Long,
    onToggle: () -> Unit,
    onRequestDelete: (RegistroHidratacion) -> Unit,
    colorParaTipo: (String) -> Color,
    iconoParaTipo: (String) -> String
) {
    val limitesMes = remember(mes) { limitesMesHidratacion(mes) }
    val tomasDelMes by hidratacionDao
        .observarEnRango(patientId = patientId, desde = limitesMes.first, hasta = limitesMes.second)
        .collectAsState(initial = emptyList())
    val diasExpandidos = remember(mes) { mutableStateMapOf<Long, Boolean>() }
    val tituloMes = remember(mes) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { isLenient = false }
            .format(SimpleDateFormat("yyyy-MM", Locale.US).parse(mes) ?: Date())
            .replaceFirstChar { it.uppercase() }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137).copy(alpha = 0.78f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF29B6F6))
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
                    if (tomasDelMes.isEmpty()) {
                        Text(
                            "No hay tomas en este mes.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    } else {
                        tomasDelMes.groupBy(::inicioDelDiaHidratacion)
                            .toList()
                            .sortedByDescending { it.first }
                            .forEach { (dia, tomasDelDia) ->
                                val diaExpandido = diasExpandidos[dia] == true
                                val totalDia = tomasDelDia.sumOf { it.cantidadMl }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102A45)),
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
                                            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${tituloDiaHidratacion(dia, inicioDelDia)} · $totalDia ml",
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
                                            Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                                                tomasDelDia.forEach { toma ->
                                                    val nota = notasPrefs.getString("nota_${toma.id}", "")?.takeIf { it.isNotBlank() }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 7.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .clip(CircleShape)
                                                                .background(colorParaTipo(toma.tipoBebida).copy(alpha = 0.2f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(iconoParaTipo(toma.tipoBebida), fontSize = 15.sp)
                                                        }
                                                        Spacer(Modifier.width(9.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("${toma.cantidadMl} ml", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                            Text(
                                                                "${toma.tipoBebida} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(toma.timestamp))}",
                                                                color = Color.White.copy(alpha = 0.55f),
                                                                fontSize = 12.sp
                                                            )
                                                            nota?.let {
                                                                Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                                            }
                                                        }
                                                        IconButton(onClick = { onRequestDelete(toma) }) {
                                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
        }
    }
}

private fun limitesMesHidratacion(mes: String): Pair<Long, Long> {
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

private fun inicioDelDiaHidratacion(toma: RegistroHidratacion): Long {
    return Calendar.getInstance().apply {
        timeInMillis = toma.timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun tituloDiaHidratacion(dia: Long, inicioDelDia: Long): String {
    return when (dia) {
        inicioDelDia -> "Hoy"
        inicioDelDia - 86_400_000L -> "Ayer"
        else -> SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(Date(dia)).replaceFirstChar { it.uppercase() }
    }
}
