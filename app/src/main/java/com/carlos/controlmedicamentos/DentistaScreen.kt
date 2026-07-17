package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.notifications.DentistaScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val COLOR_DENTAL = Color(0xFF00BCD4)
private val COLOR_FONDO  = Color(0xFF050F12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentistaScreen(
    patientId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val scheduler = remember { DentistaScheduler(context) }

    val dentistas  by db.dentistaDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val visitas    by db.visitaDentistaDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val pendientes by db.procedimientoDentalDao().observarPendientes(patientId).collectAsState(initial = emptyList())
    val transacciones by db.transaccionDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val patientProfile by db.patientProfileDao().observeById(patientId).collectAsState(initial = null)

    val patientName = patientProfile?.let { "${it.nombre} ${it.apellidos}" } ?: "Paciente"

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val ok = DentistaPdfExporter.exportarFinanzasAPdf(context, it, patientName, transacciones)
            Toast.makeText(context, if (ok) "PDF exportado" else "Error al exportar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    var pestana by remember { mutableStateOf(0) }   // 0=Panel, 1=Citas, 2=Odontograma, 3=Ortodoncia, 4=Sonrisa, 5=Finanzas, 6=Directorio
    var mostrarFormVisita    by remember { mutableStateOf(false) }
    var mostrarFormDentista  by remember { mutableStateOf(false) }
    var visitaSeleccionada   by remember { mutableStateOf<VisitaDentista?>(null) }
    var dentistaSeleccionado by remember { mutableStateOf<Dentista?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modulo Dental", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { mostrarFormVisita = true }) {
                        Icon(Icons.Filled.Add, null, tint = COLOR_DENTAL)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF041015))
            )
        },
        containerColor = COLOR_FONDO
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF041015), Color(0xFF071C24), Color(0xFF041015))))
                .padding(padding)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pestana,
                containerColor = Color(0xFF041015),
                contentColor = COLOR_DENTAL,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pestana]),
                        color = COLOR_DENTAL
                    )
                }
            ) {
                listOf("Panel", "Citas", "Odontograma", "Ortodoncia", "Sonrisa", "Finanzas", "Directorio").forEachIndexed { i, label ->
                    Tab(
                        selected = pestana == i,
                        onClick = { pestana = i },
                        text = { Text(label, fontSize = 12.sp, color = if (pestana == i) COLOR_DENTAL else Color.White.copy(0.5f)) }
                    )
                }
            }

            when (pestana) {
                0 -> PanelDental(visitas, pendientes, dentistas, onNuevaCita = { mostrarFormVisita = true })
                1 -> CitasDentales(visitas, dentistas, db, scope, scheduler, onEditarVisita = { visitaSeleccionada = it; mostrarFormVisita = true })
                2 -> OdontogramaInteractivoTab(patientId, db)
                3 -> OrtodonciaTab(patientId, db)
                4 -> SonrisaTab(patientId, db)
                5 -> FinanzasTab(patientId, db, onExportarPdf = { pdfLauncher.launch("resumen_dental_${patientId}.pdf") })
                6 -> DirectorioDentistas(dentistas, onNuevo = { mostrarFormDentista = true }, onEliminar = { d ->
                    scope.launch { db.dentistaDao().eliminar(d) }
                })
            }
        }
    }

    if (mostrarFormVisita) {
        FormVisitaDentista(
            patientId = patientId,
            dentistas = dentistas,
            visitaEditar = visitaSeleccionada,
            onGuardar = { visita ->
                scope.launch {
                    val id = db.visitaDentistaDao().insertar(visita.copy(patientId = patientId)).toInt()
                    scheduler.programarCita(visita.copy(id = if (visita.id == 0) id else visita.id))
                }
                visitaSeleccionada = null
                mostrarFormVisita = false
            },
            onCancelar = { visitaSeleccionada = null; mostrarFormVisita = false }
        )
    }

    if (mostrarFormDentista) {
        FormDentista(
            patientId = patientId,
            dentistaEditar = dentistaSeleccionado,
            onGuardar = { d ->
                scope.launch { db.dentistaDao().insertar(d.copy(patientId = patientId)) }
                dentistaSeleccionado = null
                mostrarFormDentista = false
            },
            onCancelar = { dentistaSeleccionado = null; mostrarFormDentista = false }
        )
    }
}

// ── Panel principal ───────────────────────────────────────────────────────────
@Composable
private fun PanelDental(
    visitas: List<VisitaDentista>,
    pendientes: List<ProcedimientoDental>,
    dentistas: List<Dentista>,
    onNuevaCita: () -> Unit
) {
    val proxima = visitas.filter { it.estado == "PENDIENTE" && it.fechaHora > System.currentTimeMillis() }
        .minByOrNull { it.fechaHora }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Proxima cita", color = COLOR_DENTAL, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    if (proxima != null) {
                        Text(
                            SimpleDateFormat("EEEE dd 'de' MMMM yyyy, HH:mm", Locale("es")).format(Date(proxima.fechaHora)),
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                        )
                        if (proxima.motivo.isNotBlank())
                            Text("Motivo: ${proxima.motivo}", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("No hay citas programadas", color = Color.White.copy(0.5f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Button(onClick = onNuevaCita, colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Agendar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Visitas", visitas.size.toString(), Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
                StatCard("Pendientes", pendientes.size.toString(), Color(0xFFFFA726), modifier = Modifier.weight(1f))
                StatCard("Dentistas", dentistas.size.toString(), Color(0xFF81C784), modifier = Modifier.weight(1f))
            }
        }

        if (pendientes.isNotEmpty()) {
            item { Text("Tratamientos pendientes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            items(pendientes.take(5)) { proc ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24).copy(0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MedicalServices, null, tint = Color(0xFFFFA726), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(proc.tipo, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (proc.descripcion.isNotBlank())
                                Text(proc.descripcion, color = Color.White.copy(0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

// ── Citas ─────────────────────────────────────────────────────────────────────
@Composable
private fun CitasDentales(
    visitas: List<VisitaDentista>,
    dentistas: List<Dentista>,
    db: AppDatabase,
    scope: kotlinx.coroutines.CoroutineScope,
    scheduler: DentistaScheduler,
    onEditarVisita: (VisitaDentista) -> Unit
) {
    val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es"))
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (visitas.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No hay citas registradas", color = Color.White.copy(0.4f), textAlign = TextAlign.Center)
                }
            }
        }
        items(visitas) { visita ->
            val dentista = dentistas.find { it.id == visita.dentistaId }
            val colorEstado = when (visita.estado) {
                "COMPLETADA" -> Color(0xFF66BB6A)
                "CANCELADA"  -> Color(0xFFEF5350)
                else         -> COLOR_DENTAL
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fmt.format(Date(visita.fechaHora)), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (visita.motivo.isNotBlank()) Text(visita.motivo, color = Color.White.copy(0.7f), fontSize = 12.sp)
                            if (dentista != null) Text("Dr. ${dentista.nombre}", color = COLOR_DENTAL.copy(0.8f), fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = colorEstado.copy(0.2f)) {
                            Text(visita.estado, color = colorEstado, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (visita.estado == "PENDIENTE") {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        db.visitaDentistaDao().actualizar(visita.copy(estado = "COMPLETADA", seguimientoPostConsulta = true))
                                        scheduler.programarCita(visita.copy(estado = "COMPLETADA", seguimientoPostConsulta = true))
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF66BB6A)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("Completar", fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = { onEditarVisita(visita) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = COLOR_DENTAL),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("Editar", fontSize = 11.sp) }
                        }
                        OutlinedButton(
                            onClick = { scope.launch { scheduler.cancelarCita(visita.id); db.visitaDentistaDao().eliminar(visita) } },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("Eliminar", fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

// ── Odontograma ───────────────────────────────────────────────────────────────
@Composable
private fun OdontogramaTab(
    patientId: Int,
    db: AppDatabase,
    dienteSeleccionado: Int?,
    onDienteClick: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var historalDiente by remember(dienteSeleccionado) { mutableStateOf<List<DiagnosticoDental>>(emptyList()) }

    LaunchedEffect(dienteSeleccionado) {
        if (dienteSeleccionado != null) {
            historalDiente = db.diagnosticoDentalDao().obtenerPorDiente(patientId, dienteSeleccionado)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Toca un diente para ver su historial", color = Color.White.copy(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        item {
            OdontogramaVisual(dienteSeleccionado, onDienteClick)
        }
        if (dienteSeleccionado != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Diente #$dienteSeleccionado — Historial", color = COLOR_DENTAL, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        if (historalDiente.isEmpty()) {
                            Text("Sin registros para este diente", color = Color.White.copy(0.4f), fontSize = 13.sp)
                        } else {
                            historalDiente.forEach { dx ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Icon(Icons.Filled.Circle, null, tint = when (dx.estado) {
                                        "RESUELTO" -> Color(0xFF66BB6A)
                                        "EN_TRATAMIENTO" -> Color(0xFFFFA726)
                                        else -> Color(0xFFEF5350)
                                    }, modifier = Modifier.size(10.dp).padding(top = 3.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(dx.descripcion, color = Color.White, fontSize = 13.sp)
                                        Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dx.fechaRegistro)), color = Color.White.copy(0.4f), fontSize = 11.sp)
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

@Composable
private fun OdontogramaVisual(seleccionado: Int?, onDienteClick: (Int) -> Unit) {
    // Notación FDI: superior derecho 11-18, superior izquierdo 21-28
    //               inferior izquierdo 31-38, inferior derecho 41-48
    val cuadrantes = listOf(
        (18 downTo 11).toList(),    // Superior derecho (der → izq visualmente)
        (21..28).toList(),          // Superior izquierdo
        (38 downTo 31).toList(),    // Inferior izquierdo (der → izq visualmente)
        (41..48).toList()           // Inferior derecho
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SUPERIOR", color = Color.White.copy(0.4f), fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        // Fila superior
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            cuadrantes[0].forEach { num -> DienteBoton(num, seleccionado, onDienteClick) }
            Spacer(Modifier.width(4.dp))
            cuadrantes[1].forEach { num -> DienteBoton(num, seleccionado, onDienteClick) }
        }
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(color = Color.White.copy(0.15f))
        Spacer(Modifier.height(2.dp))
        // Fila inferior
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            cuadrantes[2].forEach { num -> DienteBoton(num, seleccionado, onDienteClick) }
            Spacer(Modifier.width(4.dp))
            cuadrantes[3].forEach { num -> DienteBoton(num, seleccionado, onDienteClick) }
        }
        Spacer(Modifier.height(4.dp))
        Text("INFERIOR", color = Color.White.copy(0.4f), fontSize = 10.sp)
    }
}

@Composable
private fun DienteBoton(numero: Int, seleccionado: Int?, onClick: (Int) -> Unit) {
    val esMolar = numero % 10 >= 6
    val size = if (esMolar) 32.dp else 28.dp
    val selected = seleccionado == numero
    Box(
        modifier = Modifier
            .padding(1.dp)
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) COLOR_DENTAL.copy(0.4f) else Color(0xFF0A2530))
            .border(1.dp, if (selected) COLOR_DENTAL else Color.White.copy(0.15f), RoundedCornerShape(6.dp))
            .clickable { onClick(numero) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (numero % 10).toString(),
            color = if (selected) COLOR_DENTAL else Color.White.copy(0.7f),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Directorio dentistas ──────────────────────────────────────────────────────
@Composable
private fun DirectorioDentistas(
    dentistas: List<Dentista>,
    onNuevo: () -> Unit,
    onEliminar: (Dentista) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onNuevo, colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Agregar dentista")
            }
        }
        if (dentistas.isEmpty()) {
            item { Text("No hay dentistas registrados", color = Color.White.copy(0.4f), modifier = Modifier.padding(16.dp)) }
        }
        items(dentistas) { d ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(COLOR_DENTAL.copy(0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = COLOR_DENTAL, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(d.nombre, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (d.especialidad.isNotBlank()) Text(d.especialidad, color = COLOR_DENTAL.copy(0.7f), fontSize = 12.sp)
                        if (d.telefono.isNotBlank()) Text(d.telefono, color = Color.White.copy(0.5f), fontSize = 11.sp)
                    }
                    IconButton(onClick = { onEliminar(d) }) {
                        Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350).copy(0.6f))
                    }
                }
            }
        }
    }
}

// ── Formulario nueva visita ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormVisitaDentista(
    patientId: Int,
    dentistas: List<Dentista>,
    visitaEditar: VisitaDentista?,
    onGuardar: (VisitaDentista) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var motivo       by remember { mutableStateOf(visitaEditar?.motivo ?: "") }
    var notas        by remember { mutableStateOf(visitaEditar?.notas ?: "") }
    var dentistaId   by remember { mutableStateOf(visitaEditar?.dentistaId) }
    var r24h         by remember { mutableStateOf(visitaEditar?.recordatorio24h ?: true) }
    var r2h          by remember { mutableStateOf(visitaEditar?.recordatorio2h ?: true) }
    var seguimiento  by remember { mutableStateOf(visitaEditar?.seguimientoPostConsulta ?: false) }
    var fechaHora    by remember { mutableStateOf(visitaEditar?.fechaHora ?: (System.currentTimeMillis() + 86400_000L)) }
    var expandedDentista by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance().apply { timeInMillis = fechaHora }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color(0xFF071C24),
        title = { Text(if (visitaEditar == null) "Nueva cita dental" else "Editar cita", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo de la visita") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                }
                item {
                    // Fecha
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                cal.set(y, m, d)
                                TimePickerDialog(context, { _, h, min -> cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); fechaHora = cal.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = COLOR_DENTAL)
                    ) {
                        Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaHora)))
                    }
                }
                item {
                    // Dentista
                    ExposedDropdownMenuBox(expanded = expandedDentista, onExpandedChange = { expandedDentista = it }) {
                        OutlinedTextField(
                            value = dentistas.find { it.id == dentistaId }?.nombre ?: "Sin asignar",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dentista") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDentista) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                        )
                        ExposedDropdownMenu(expanded = expandedDentista, onDismissRequest = { expandedDentista = false }, containerColor = Color(0xFF071C24)) {
                            DropdownMenuItem(text = { Text("Sin asignar", color = Color.White) }, onClick = { dentistaId = null; expandedDentista = false })
                            dentistas.forEach { d ->
                                DropdownMenuItem(text = { Text(d.nombre, color = Color.White) }, onClick = { dentistaId = d.id; expandedDentista = false })
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("Notas adicionales") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                }
                item {
                    Text("Recordatorios", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = r24h, onCheckedChange = { r24h = it }, colors = CheckboxDefaults.colors(checkedColor = COLOR_DENTAL))
                        Text("24 horas antes", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = r2h, onCheckedChange = { r2h = it }, colors = CheckboxDefaults.colors(checkedColor = COLOR_DENTAL))
                        Text("2 horas antes", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seguimiento, onCheckedChange = { seguimiento = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA726)))
                        Text("Seguimiento post-consulta (al dia siguiente)", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(VisitaDentista(
                        id = visitaEditar?.id ?: 0,
                        patientId = patientId,
                        dentistaId = dentistaId,
                        fechaHora = fechaHora,
                        motivo = motivo.trim(),
                        notas = notas.trim(),
                        recordatorio24h = r24h,
                        recordatorio2h = r2h,
                        seguimientoPostConsulta = seguimiento
                    ))
                },
                enabled = motivo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

// ── Formulario dentista ───────────────────────────────────────────────────────
@Composable
private fun FormDentista(
    patientId: Int,
    dentistaEditar: Dentista?,
    onGuardar: (Dentista) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre       by remember { mutableStateOf(dentistaEditar?.nombre ?: "") }
    var especialidad by remember { mutableStateOf(dentistaEditar?.especialidad ?: "") }
    var telefono     by remember { mutableStateOf(dentistaEditar?.telefono ?: "") }
    var direccion    by remember { mutableStateOf(dentistaEditar?.direccion ?: "") }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color(0xFF071C24),
        title = { Text("Datos del dentista", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Nombre *", nombre) { v: String -> nombre = v },
                    Triple("Especialidad", especialidad) { v: String -> especialidad = v },
                    Triple("Telefono", telefono) { v: String -> telefono = v },
                    Triple("Clinica / Direccion", direccion) { v: String -> direccion = v }
                ).forEach { (label, valor, onValue) ->
                    OutlinedTextField(
                        value = valor,
                        onValueChange = onValue,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(Dentista(id = dentistaEditar?.id ?: 0, patientId = patientId, nombre = nombre.trim(), especialidad = especialidad.trim(), telefono = telefono.trim(), direccion = direccion.trim())) },
                enabled = nombre.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}
