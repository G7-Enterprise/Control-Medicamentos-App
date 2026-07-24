package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Paleta violeta metálica (igual que NuevaVacunaActivity)
private val CApptDark   = Color(0xFF1A0030)
private val CApptMid    = Color(0xFF6B0FAD)
private val CApptLight  = Color(0xFFB44FE8)
private val CApptSheen  = Color(0xFFD68FFF)
private val CApptBg = Brush.horizontalGradient(
    colors = listOf(CApptDark, CApptMid, CApptSheen, CApptLight, CApptMid, CApptDark)
)
private val CApptCardBg     = Color(0xFF2D0050).copy(alpha = 0.85f)
private val CApptCardDetail = Color(0xFF3A006A).copy(alpha = 0.90f)
private val CApptTextMain   = Color(0xFFFFFFFF)
private val CApptTextSub    = Color(0xFFE0BBFF)
private val CApptAccent     = Color(0xFFD68FFF)
private val CApptFieldBdr   = Color(0xFFB44FE8)

private fun fmtFechaCita(ts: Long): String {
    if (ts == 0L) return ""
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "NI")).format(ts)
}

class CitasMedicasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControlMedicamentosTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CApptBg)
                ) {
                    CitasMedicasScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasMedicasScreen() {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val pacienteActivo by database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)

    val todasLasCitas by remember(pacienteActivo?.id) {
        val id = pacienteActivo?.id
        if (id != null) database.medicalAppointmentDao().observarPorPaciente(id)
        else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val citasPendientes = remember(todasLasCitas) {
        todasLasCitas.filter { !it.isCompleted }.sortedBy { it.scheduledAt }
    }

    val citasRealizadas = remember(todasLasCitas) {
        todasLasCitas.filter { it.isCompleted }.sortedByDescending { it.scheduledAt }
    }

    val profesionalesHabituales by remember(pacienteActivo?.id) {
        val id = pacienteActivo?.id
        if (id != null) database.medicalPractitionerDao().observarPorPaciente(id)
        else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    // Estado del formulario nueva cita
    var mostrarFormNuevaCita by remember { mutableStateOf(false) }
    var mostrarHistorial by remember { mutableStateOf(false) }
    var motivoCita by remember { mutableStateOf("") }
    var doctorCita by remember { mutableStateOf("") }
    var lugarCita by remember { mutableStateOf("") }
    var notasCita by remember { mutableStateOf("") }
    var fechaCita by remember { mutableStateOf<Long?>(null) }
    var alarmaActiva by remember { mutableStateOf(true) }

    // Estado editar / eliminar cita
    var citaEditando by remember { mutableStateOf<MedicalAppointment?>(null) }
    var editMotivo by remember { mutableStateOf("") }
    var editDoctor by remember { mutableStateOf("") }
    var editLugar by remember { mutableStateOf("") }
    var editNotas by remember { mutableStateOf("") }
    var editFecha by remember { mutableStateOf<Long?>(null) }
    var editAlarma by remember { mutableStateOf(true) }
    var citaParaEliminar by remember { mutableStateOf<MedicalAppointment?>(null) }
    var expandedMedicoCita by remember { mutableStateOf(false) }
    var expandedEditMedico by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()

    val nombrePaciente = pacienteActivo?.let {
        "${it.nombre} ${it.apellidos}".trim().ifBlank { "Perfil activo" }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CApptAccent,
        unfocusedBorderColor = CApptFieldBdr,
        focusedLabelColor = CApptAccent,
        unfocusedLabelColor = CApptTextSub,
        cursorColor = CApptAccent,
        focusedTextColor = CApptTextMain,
        unfocusedTextColor = CApptTextMain,
        focusedTrailingIconColor = CApptAccent,
        unfocusedTrailingIconColor = CApptFieldBdr
    )

    val formatoMedico: (com.carlos.controlmedicamentos.data.local.MedicalPractitioner) -> String = {
        if (it.specialty.isNotBlank()) "${it.name} — ${it.specialty}" else it.name
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Citas médicas",
            style = MaterialTheme.typography.headlineMedium,
            color = CApptTextMain,
            fontWeight = FontWeight.Bold
        )

        // ── Tarjeta de perfil activo ───────────────────────────────────────────
        if (nombrePaciente != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CApptCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Usuario: $nombrePaciente",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = CApptTextMain
                    )
                    Text(
                        "${citasPendientes.size} cita(s) pendiente(s)",
                        fontSize = 12.sp,
                        color = CApptTextSub
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0000).copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No hay perfil activo. Selecciona un paciente primero.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFFFBBBB)
                )
            }
        }

        // ── CITAS PENDIENTES ──────────────────────────────────────────────────
        Text(
            "Eventos Pendientes",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = CApptAccent
        )

        if (citasPendientes.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CApptCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No hay citas pendientes.",
                    modifier = Modifier.padding(16.dp),
                    color = CApptTextSub,
                    fontSize = 13.sp
                )
            }
        } else {
            citasPendientes.forEach { cita ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CApptCardDetail),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            cita.title,
                            fontWeight = FontWeight.SemiBold,
                            color = CApptTextMain,
                            fontSize = 14.sp
                        )
                        if (cita.doctorName.isNotBlank())
                            Text("Contacto: ${cita.doctorName}", fontSize = 12.sp, color = CApptTextSub)
                        if (cita.location.isNotBlank())
                            Text("Lugar: ${cita.location}", fontSize = 12.sp, color = CApptTextSub)
                        if (cita.scheduledAt != 0L)
                            Text(
                                "Fecha: ${fmtFechaCita(cita.scheduledAt)}",
                                fontSize = 12.sp,
                                color = CApptAccent
                            )
                        if (cita.notes.isNotBlank())
                            Text("Notas: ${cita.notes}", fontSize = 11.sp, color = CApptTextSub)

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        MedicalAppointmentScheduler(context).cancelar(cita.id)
                                    } catch (_: Exception) {}
                                    database.medicalAppointmentDao().actualizar(cita.copy(isCompleted = true))
                                    Toast.makeText(context, "Cita marcada como realizada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32).copy(alpha = 0.85f),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Ya realizada", fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editMotivo = cita.title
                                    editDoctor = cita.doctorName
                                    editLugar = cita.location
                                    editNotas = cita.notes
                                    editFecha = if (cita.scheduledAt != 0L) cita.scheduledAt else null
                                    editAlarma = cita.alarmEnabled
                                    citaEditando = cita
                                },
                                border = BorderStroke(1.dp, CApptAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = CApptAccent)
                                Spacer(Modifier.width(4.dp))
                                Text("Editar", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { citaParaEliminar = cita },
                                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF5252))
                                Spacer(Modifier.width(4.dp))
                                Text("Eliminar", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = CApptFieldBdr.copy(alpha = 0.4f), thickness = 1.dp)

        // ── BOTÓN HISTORIAL DE CITAS REALIZADAS ───────────────────────────────
        Button(
            onClick = { mostrarHistorial = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = CApptMid,
                contentColor = CApptTextMain
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ver historial de citas realizadas (${citasRealizadas.size})", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = CApptFieldBdr.copy(alpha = 0.4f), thickness = 1.dp)

        // ── BOTÓN NUEVA CITA ─────────────────────────────────────────────────
        Button(
            onClick = { mostrarFormNuevaCita = !mostrarFormNuevaCita },
            colors = ButtonDefaults.buttonColors(
                containerColor = CApptMid,
                contentColor = CApptTextMain
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (mostrarFormNuevaCita) "Cancelar" else "Nuevo Evento",
                fontWeight = FontWeight.Bold
            )
        }

        // ── FORMULARIO NUEVA CITA (expandible) ───────────────────────────────
        AnimatedVisibility(
            visible = mostrarFormNuevaCita,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CApptCardDetail),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Datos de la nueva cita",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = CApptAccent
                    )

                    OutlinedTextField(
                        value = motivoCita,
                        onValueChange = { motivoCita = it },
                        label = { Text("Motivo / Título *") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val opcionesDoctoresNueva = profesionalesHabituales.map(formatoMedico)
                    val doctorSeleccionadoNueva = profesionalesHabituales.find { it.name == doctorCita }?.let(formatoMedico) ?: doctorCita
                    VademecumDropdown(
                        label = "Doctor / Especialista",
                        options = opcionesDoctoresNueva,
                        selectedValue = doctorSeleccionadoNueva,
                        expanded = expandedMedicoCita,
                        onExpandedChange = { expandedMedicoCita = !expandedMedicoCita },
                        onDismiss = { expandedMedicoCita = false },
                        onSelect = { selected ->
                            profesionalesHabituales.find { formatoMedico(it) == selected }?.let { doctorCita = it.name }
                            expandedMedicoCita = false
                        },
                        colors = fieldColors,
                        emptyOptionsText = "No hay médicos guardados"
                    )

                    OutlinedTextField(
                        value = lugarCita,
                        onValueChange = { lugarCita = it },
                        label = { Text("Lugar / Centro de atencion") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notasCita,
                        onValueChange = { notasCita = it },
                        label = { Text("Notas / Observaciones") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Botón seleccionar fecha y hora
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            fechaCita = Calendar.getInstance()
                                                .apply { set(year, month, day, hour, minute, 0) }
                                                .timeInMillis
                                        },
                                        9, 0, true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        border = BorderStroke(1.dp, CApptFieldBdr),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CApptTextMain),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CApptAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (fechaCita != null) "Cita: ${fmtFechaCita(fechaCita!!)}"
                            else "Seleccionar fecha y hora"
                        )
                    }

                    // Switch alarma
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activar alarma / recordatorio", color = CApptTextMain)
                        Switch(
                            checked = alarmaActiva,
                            onCheckedChange = { alarmaActiva = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CApptTextMain,
                                checkedTrackColor = CApptLight,
                                uncheckedThumbColor = CApptTextSub,
                                uncheckedTrackColor = CApptCardBg
                            )
                        )
                    }

                    // Botón guardar
                    Button(
                        onClick = {
                            if (motivoCita.isBlank()) {
                                Toast.makeText(context, "Escribe el motivo de la cita", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (fechaCita == null) {
                                Toast.makeText(context, "Selecciona la fecha de la cita", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val pacId = pacienteActivo?.id ?: run {
                                Toast.makeText(context, "No hay paciente activo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val appointment = MedicalAppointment(
                                    patientId = pacId,
                                    title = motivoCita.trim(),
                                    doctorName = doctorCita.trim(),
                                    location = lugarCita.trim(),
                                    notes = notasCita.trim(),
                                    scheduledAt = fechaCita!!,
                                    alarmEnabled = alarmaActiva,
                                    isCompleted = false
                                )
                                val savedId = database.medicalAppointmentDao().guardar(appointment)
                                if (alarmaActiva) {
                                    try {
                                        val saved = database.medicalAppointmentDao().buscarPorId(savedId.toInt())
                                        if (saved != null) MedicalAppointmentScheduler(context).programar(saved)
                                    } catch (_: Exception) {}
                                }
                                motivoCita = ""
                                doctorCita = ""
                                lugarCita = ""
                                notasCita = ""
                                fechaCita = null
                                alarmaActiva = true
                                mostrarFormNuevaCita = false
                                Toast.makeText(context, "Cita guardada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CApptLight,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Guardar Cita", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // ── DIÁLOGO EDITAR CITA ──────────────────────────────────────────────────
    if (citaEditando != null) {
        Dialog(onDismissRequest = { citaEditando = null }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = CApptCardDetail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Editar Cita", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CApptAccent)

                    OutlinedTextField(
                        value = editMotivo,
                        onValueChange = { editMotivo = it },
                        label = { Text("Motivo / Título *") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    val opcionesDoctoresEdit = profesionalesHabituales.map(formatoMedico)
                    val doctorSeleccionadoEdit = profesionalesHabituales.find { it.name == editDoctor }?.let(formatoMedico) ?: editDoctor
                    VademecumDropdown(
                        label = "Doctor / Especialista",
                        options = opcionesDoctoresEdit,
                        selectedValue = doctorSeleccionadoEdit,
                        expanded = expandedEditMedico,
                        onExpandedChange = { expandedEditMedico = !expandedEditMedico },
                        onDismiss = { expandedEditMedico = false },
                        onSelect = { selected ->
                            profesionalesHabituales.find { formatoMedico(it) == selected }?.let { editDoctor = it.name }
                            expandedEditMedico = false
                        },
                        colors = fieldColors,
                        emptyOptionsText = "No hay médicos guardados"
                    )
                    OutlinedTextField(
                        value = editLugar,
                        onValueChange = { editLugar = it },
                        label = { Text("Lugar / Centro de atencion") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editNotas,
                        onValueChange = { editNotas = it },
                        label = { Text("Notas / Observaciones") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            editFecha = Calendar.getInstance()
                                                .apply { set(year, month, day, hour, minute, 0) }
                                                .timeInMillis
                                        },
                                        9, 0, true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        border = BorderStroke(1.dp, CApptFieldBdr),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CApptTextMain),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CApptAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (editFecha != null) "Cita: ${fmtFechaCita(editFecha!!)}"
                            else "Seleccionar fecha y hora"
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activar alarma / recordatorio", color = CApptTextMain)
                        Switch(
                            checked = editAlarma,
                            onCheckedChange = { editAlarma = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CApptTextMain,
                                checkedTrackColor = CApptLight,
                                uncheckedThumbColor = CApptTextSub,
                                uncheckedTrackColor = CApptCardBg
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { citaEditando = null },
                            border = BorderStroke(1.dp, CApptFieldBdr),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CApptTextMain),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar") }
                        Button(
                            onClick = {
                                if (editMotivo.isBlank()) {
                                    Toast.makeText(context, "Escribe el motivo de la cita", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (editFecha == null) {
                                    Toast.makeText(context, "Selecciona la fecha de la cita", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val cita = citaEditando!!
                                scope.launch {
                                    try { MedicalAppointmentScheduler(context).cancelar(cita.id) } catch (_: Exception) {}
                                    val updated = cita.copy(
                                        title = editMotivo.trim(),
                                        doctorName = editDoctor.trim(),
                                        location = editLugar.trim(),
                                        notes = editNotas.trim(),
                                        scheduledAt = editFecha!!,
                                        alarmEnabled = editAlarma
                                    )
                                    database.medicalAppointmentDao().actualizar(updated)
                                    if (editAlarma) {
                                        try { MedicalAppointmentScheduler(context).programar(updated) } catch (_: Exception) {}
                                    }
                                    citaEditando = null
                                    Toast.makeText(context, "Cita actualizada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CApptLight,
                                contentColor = CApptTextMain
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Guardar", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // ── DIÁLOGO CONFIRMAR ELIMINAR ───────────────────────────────────────────
    if (citaParaEliminar != null) {
        AlertDialog(
            onDismissRequest = { citaParaEliminar = null },
            title = { Text("Eliminar cita", color = CApptTextMain) },
            text = {
                Text(
                    "¿Seguro que quieres eliminar la cita \"${citaParaEliminar!!.title}\"?",
                    color = CApptTextSub
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cita = citaParaEliminar!!
                        scope.launch {
                            try { MedicalAppointmentScheduler(context).cancelar(cita.id) } catch (_: Exception) {}
                            database.medicalAppointmentDao().eliminar(cita)
                            Toast.makeText(context, "Cita eliminada", Toast.LENGTH_SHORT).show()
                        }
                        citaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.Black)
                ) { Text("Eliminar", color = Color.Black) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { citaParaEliminar = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) { Text("Cancelar", color = Color.Black) }
            },
            containerColor = CApptCardDetail,
            titleContentColor = CApptTextMain,
            textContentColor = CApptTextSub
        )
    }

    // ── DIÁLOGO HISTORIAL DE CITAS REALIZADAS ─────────────────────────────────
    if (mostrarHistorial) {
        Dialog(onDismissRequest = { mostrarHistorial = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = CApptCardDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Historial de Citas Realizadas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CApptAccent
                    )

                    if (citasRealizadas.isEmpty()) {
                        Text(
                            "No hay citas realizadas registradas.",
                            color = CApptTextSub,
                            fontSize = 13.sp
                        )
                    } else {
                        // Estadísticas básicas
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CApptCardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Total de citas realizadas: ${citasRealizadas.size}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = CApptTextMain
                                )
                                // Contar visitas por médico/especialista
                                val visitasPorMedico = citasRealizadas
                                    .filter { it.doctorName.isNotBlank() }
                                    .groupBy { it.doctorName }
                                    .mapValues { it.value.size }
                                    .toList()
                                    .sortedByDescending { it.second }

                                if (visitasPorMedico.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Visitas por profesional:",
                                        fontSize = 12.sp,
                                        color = CApptTextSub,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    visitasPorMedico.forEach { (medico, count) ->
                                        Text(
                                            "• $medico: $count visita(s)",
                                            fontSize = 11.sp,
                                            color = CApptTextSub
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lista de citas realizadas agrupadas por año
                        val citasPorAnio = citasRealizadas
                            .filter { it.scheduledAt != 0L }
                            .groupBy { cita ->
                                val cal = java.util.Calendar.getInstance()
                                cal.timeInMillis = cita.scheduledAt
                                cal.get(java.util.Calendar.YEAR)
                            }
                            .toSortedMap()
                            .toSortedMap(reverseOrder())

                        if (citasPorAnio.isEmpty()) {
                            Text(
                                "No hay citas con fecha registrada.",
                                color = CApptTextSub,
                                fontSize = 13.sp
                            )
                        } else {
                            citasPorAnio.forEach { (anio, citasDelAnio) ->
                                Text(
                                    "Año $anio (${citasDelAnio.size} cita(s))",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CApptAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                citasDelAnio.forEach { cita ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CApptCardBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                cita.title,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CApptTextMain,
                                                fontSize = 14.sp
                                            )
                                            if (cita.doctorName.isNotBlank())
                                                Text("Contacto: ${cita.doctorName}", fontSize = 12.sp, color = CApptTextSub)
                                            if (cita.location.isNotBlank())
                                                Text("Lugar: ${cita.location}", fontSize = 12.sp, color = CApptTextSub)
                                            if (cita.scheduledAt != 0L)
                                                Text(
                                                    "Fecha: ${fmtFechaCita(cita.scheduledAt)}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            if (cita.notes.isNotBlank())
                                                Text("Notas: ${cita.notes}", fontSize = 11.sp, color = CApptTextSub)

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        editMotivo = cita.title
                                                        editDoctor = cita.doctorName
                                                        editLugar = cita.location
                                                        editNotas = cita.notes
                                                        editFecha = if (cita.scheduledAt != 0L) cita.scheduledAt else null
                                                        editAlarma = cita.alarmEnabled
                                                        citaEditando = cita
                                                    },
                                                    border = BorderStroke(1.dp, CApptAccent),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = CApptAccent)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Editar", fontSize = 13.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { citaParaEliminar = cita },
                                                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF5252))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Eliminar", fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón exportar a PDF
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        exportarHistorialPDF(context, citasRealizadas, nombrePaciente ?: "Usuario")
                                        Toast.makeText(context, "PDF exportado correctamente", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al exportar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CApptLight,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Exportar a PDF", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { mostrarHistorial = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CApptMid,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

private fun exportarHistorialPDF(context: android.content.Context, citas: List<MedicalAppointment>, nombrePaciente: String) {
    val outputFile = java.io.File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
        "historial_citas_${System.currentTimeMillis()}.pdf"
    )

    val pdfDoc = android.graphics.pdf.PdfDocument()
    try {
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint()
        titlePaint.textSize = 18f
        titlePaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        val textPaint = android.graphics.Paint()
        textPaint.textSize = 12f
        val smallPaint = android.graphics.Paint()
        smallPaint.textSize = 10f

        var yPosition = 50f
        canvas.drawText("Historial de Citas Médicas", 50f, yPosition, titlePaint)
        yPosition += 30f
        canvas.drawText("Paciente: $nombrePaciente", 50f, yPosition, textPaint)
        yPosition += 20f
        canvas.drawText("Total de citas: ${citas.size}", 50f, yPosition, textPaint)
        yPosition += 20f
        canvas.drawText("Fecha de generación: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es")).format(java.util.Date())}", 50f, yPosition, smallPaint)
        yPosition += 40f

        // Agrupar por año
        val citasPorAnio = citas
            .filter { it.scheduledAt != 0L }
            .groupBy { cita ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = cita.scheduledAt
                cal.get(java.util.Calendar.YEAR)
            }
            .toSortedMap()
            .toSortedMap(reverseOrder())

        var currentPage = page
        var currentCanvas = canvas

        citasPorAnio.forEach { (anio, citasDelAnio) ->
            currentCanvas.drawText("Año $anio (${citasDelAnio.size} cita(s))", 50f, yPosition, titlePaint)
            yPosition += 25f

            citasDelAnio.forEach { cita ->
                currentCanvas.drawText("• ${cita.title}", 60f, yPosition, textPaint)
                yPosition += 15f
                if (cita.doctorName.isNotBlank()) {
                    currentCanvas.drawText("  Contacto: ${cita.doctorName}", 70f, yPosition, smallPaint)
                    yPosition += 15f
                }
                if (cita.location.isNotBlank()) {
                    currentCanvas.drawText("  Lugar: ${cita.location}", 70f, yPosition, smallPaint)
                    yPosition += 15f
                }
                if (cita.scheduledAt != 0L) {
                    currentCanvas.drawText("  Fecha: ${fmtFechaCita(cita.scheduledAt)}", 70f, yPosition, smallPaint)
                    yPosition += 15f
                }
                if (cita.notes.isNotBlank()) {
                    currentCanvas.drawText("  Notas: ${cita.notes}", 70f, yPosition, smallPaint)
                    yPosition += 15f
                }
                yPosition += 10f

                // Crear nueva página si nos acercamos al final
                if (yPosition > 750f) {
                    pdfDoc.finishPage(currentPage)
                    val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDoc.pages.size + 1).create()
                    currentPage = pdfDoc.startPage(newPageInfo)
                    currentCanvas = currentPage.canvas
                    yPosition = 50f
                }
            }
            yPosition += 20f
        }

        pdfDoc.finishPage(currentPage)
        pdfDoc.writeTo(java.io.FileOutputStream(outputFile))
    } finally {
        pdfDoc.close()
    }
}
