package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
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
import com.carlos.controlmedicamentos.data.local.VaccinationRecord
import com.carlos.controlmedicamentos.notifications.VaccinationScheduler
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Paleta violeta metálica
private val PurpleMetalDark   = Color(0xFF1A0030)
private val PurpleMetalMid    = Color(0xFF6B0FAD)
private val PurpleMetalLight  = Color(0xFFB44FE8)
private val PurpleMetalSheen  = Color(0xFFD68FFF)
private val PurpleMetalBg = Brush.horizontalGradient(
    colors = listOf(
        PurpleMetalDark,
        PurpleMetalMid,
        PurpleMetalSheen,
        PurpleMetalLight,
        PurpleMetalMid,
        PurpleMetalDark
    )
)
private val PurpleCardBg      = Color(0xFF2D0050).copy(alpha = 0.85f)
private val PurpleCardDetail  = Color(0xFF3A006A).copy(alpha = 0.90f)
private val PurpleTextMain    = Color(0xFFFFFFFF)
private val PurpleTextSub     = Color(0xFFE0BBFF)
private val PurpleAccent      = Color(0xFFD68FFF)
private val PurpleFieldBorder = Color(0xFFB44FE8)

data class VacunaInfo(
    val nombre: String,
    val edadMinMeses: Int,
    val edadMaxMeses: Int
)

private val ESQUEMA_VACUNAS = listOf(
    VacunaInfo("BCG (Tuberculosis) - Recién Nacido",                  0,   3),
    VacunaInfo("Hepatitis B - Recién Nacido",                         0,   3),
    VacunaInfo("Pentavalente 1ra Dosis - 2 Meses",                    2,   6),
    VacunaInfo("Pentavalente 2da Dosis - 4 Meses",                    4,   8),
    VacunaInfo("Pentavalente 3ra Dosis - 6 Meses",                    6,  12),
    VacunaInfo("Polio 1ra Dosis - 2 Meses",                           2,   6),
    VacunaInfo("Polio 2da Dosis - 4 Meses",                           4,   8),
    VacunaInfo("Polio 3ra Dosis - 6 Meses",                           6,  12),
    VacunaInfo("Rotavirus 1ra Dosis - 2 Meses",                       2,   6),
    VacunaInfo("Rotavirus 2da Dosis - 4 Meses",                       4,   8),
    VacunaInfo("Neumococo 1ra Dosis - 2 Meses",                       2,   6),
    VacunaInfo("Neumococo 2da Dosis - 4 Meses",                       4,   8),
    VacunaInfo("Neumococo 3ra Dosis - 12 Meses",                     12,  18),
    VacunaInfo("Varicela - 15 Meses",                                15,  24),
    VacunaInfo("MMR / SRP (Sarampión, Rubeola, Paperas) - 12 Meses", 12,  24),
    VacunaInfo("Hepatitis A - 12 Meses",                             12,  24),
    VacunaInfo("Fiebre Amarilla - 12 Meses",                         12, Int.MAX_VALUE),
    VacunaInfo("DPT Refuerzo - 18 Meses",                            18,  36),
    VacunaInfo("VPH (Virus Papiloma Humano) - 9 años",              108, 180),
    VacunaInfo("Td (Tétanos/Difteria) Adolescente",                 144, Int.MAX_VALUE),
    VacunaInfo("Influenza Estacional (Anual)",                         6, Int.MAX_VALUE),
    VacunaInfo("COVID-19 (Sinopharm/AstraZeneca) 1ra Dosis",         72, Int.MAX_VALUE),
    VacunaInfo("COVID-19 (Sinopharm/AstraZeneca) 2da Dosis",         72, Int.MAX_VALUE),
    VacunaInfo("COVID-19 - Refuerzo Anual",                          72, Int.MAX_VALUE)
)

private fun calcularEdadEnMeses(fechaNacimiento: Long): Int {
    if (fechaNacimiento == 0L) return Int.MAX_VALUE
    val nacimiento = Calendar.getInstance().apply { timeInMillis = fechaNacimiento }
    val hoy = Calendar.getInstance()
    val anios = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)
    val meses = hoy.get(Calendar.MONTH) - nacimiento.get(Calendar.MONTH)
    return (anios * 12 + meses).coerceAtLeast(0)
}

private fun formatFecha(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return ""
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "NI")).format(timestamp)
}

class NuevaVacunaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControlMedicamentosTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PurpleMetalBg)
                ) {
                    FormularioVacunaScreen(
                        onCerrar = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioVacunaScreen(onCerrar: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val usuarioActivo by database.patientProfileDao().observarPerfilActivo()
        .collectAsState(initial = null)

    val edadMeses = remember(usuarioActivo) {
        usuarioActivo?.let { calcularEdadEnMeses(it.fechaNacimiento) } ?: Int.MAX_VALUE
    }

    val vacunasFiltradas = remember(edadMeses) {
        if (edadMeses == Int.MAX_VALUE) {
            ESQUEMA_VACUNAS.map { it.nombre }
        } else {
            ESQUEMA_VACUNAS
                .filter { edadMeses >= it.edadMinMeses && edadMeses <= it.edadMaxMeses }
                .map { it.nombre }
                .ifEmpty {
                    ESQUEMA_VACUNAS.filter { it.edadMaxMeses == Int.MAX_VALUE }.map { it.nombre }
                }
        }
    }

    // Estado principal del formulario
    val coroutineScope = rememberCoroutineScope()
    var mostrarFormulario by remember { mutableStateOf(false) }
    var expandedVacuna by remember { mutableStateOf(false) }
    var vacunaSeleccionada by remember(vacunasFiltradas) {
        mutableStateOf(vacunasFiltradas.firstOrNull() ?: "")
    }
    var yaRecibida by remember { mutableStateOf(false) }
    var activarAlarma by remember { mutableStateOf(true) }

    // Historial de registros del usuario activo
    val registrosGuardados by remember(usuarioActivo?.id) {
        if (usuarioActivo == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else database.vaccinationRecordDao().observarPorPaciente(usuarioActivo!!.id)
    }.collectAsState(initial = emptyList())

    // Estado del detalle (sección expandible)
    var fechaAplicacion by remember { mutableStateOf<Long?>(null) }
    var medicoAplicador by remember { mutableStateOf("") }
    var tipoDosis by remember { mutableStateOf("Dosis única") }
    var expandedTipoDosis by remember { mutableStateOf(false) }
    var fechaRefuerzo by remember { mutableStateOf<Long?>(null) }
    var loteVacuna by remember { mutableStateOf("") }
    var lugarAplicacion by remember { mutableStateOf("") }
    var notasVacuna by remember { mutableStateOf("") }

    // Estado de edición y confirmación de eliminación
    var editingRecordId by remember { mutableStateOf<Int?>(null) }
    var recordToDelete by remember { mutableStateOf<VaccinationRecord?>(null) }

    val tiposDosis = listOf("Dosis única", "Requiere refuerzo", "Anual", "Serie de dosis")

    val cal = Calendar.getInstance()

    val nombreUsuario = usuarioActivo?.let {
        "${it.nombre} ${it.apellidos}".trim().ifBlank { "Perfil activo" }
    }
    val edadTexto = usuarioActivo?.let {
        if (it.fechaNacimiento != 0L) {
            val m = calcularEdadEnMeses(it.fechaNacimiento)
            if (m < 24) "$m meses" else "${m / 12} años"
        } else it.edad.ifBlank { null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(top = 16.dp)
            ) {
                Text(
                    text = "Vacunas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    editingRecordId = null
                    vacunaSeleccionada = vacunasFiltradas.firstOrNull() ?: ""
                    yaRecibida = false
                    activarAlarma = true
                    fechaAplicacion = null
                    medicoAplicador = ""
                    tipoDosis = "Dosis única"
                    fechaRefuerzo = null
                    loteVacuna = ""
                    lugarAplicacion = ""
                    notasVacuna = ""
                    mostrarFormulario = true
                }) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Nueva vacuna",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(onClick = onCerrar) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PurpleTextSub)
            }
        }

        // Tarjeta de perfil activo
        if (nombreUsuario != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PurpleCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Usuario: $nombreUsuario", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PurpleTextMain)
                    if (edadTexto != null) {
                        Text(
                            "Edad: $edadTexto — ${vacunasFiltradas.size} dosis correspondientes",
                            fontSize = 12.sp,
                            color = PurpleTextSub
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0000).copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No hay perfil activo. Se muestran todos los registros.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFFFBBBB)
                )
            }
        }

        if (vacunasFiltradas.isEmpty()) {
            Text(
                "No hay registros pendientes para la edad de este usuario.",
                color = PurpleTextSub
            )
            return@Column
        }

        // ── HISTORIAL DE VACUNAS GUARDADAS ───────────────────────────────────
        if (registrosGuardados.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PurpleCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Historial de dosis guardadas (${registrosGuardados.size})",
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        fontSize = 14.sp
                    )
                    registrosGuardados.forEach { record ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PurpleCardDetail),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val ahora = System.currentTimeMillis()
                                    val citaVencida = record.alarmEnabled &&
                                        record.doseLabel == "Cita pendiente" &&
                                        record.appliedAt > 0L &&
                                        record.appliedAt < ahora

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(record.vaccineName, fontWeight = FontWeight.SemiBold, color = PurpleTextMain, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
                                        if (citaVencida) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFFF6B00)
                                            ) {
                                                Text(
                                                    text = "⚠ Cita vencida",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(record.doseLabel, color = PurpleAccent, fontSize = 12.sp)
                                    if (record.appliedAt > 0L) {
                                        Text(
                                            if (record.alarmEnabled && record.appliedAt > ahora)
                                                "Cita programada: ${formatFecha(record.appliedAt)}"
                                            else if (citaVencida)
                                                "Cita no realizada: ${formatFecha(record.appliedAt)}"
                                            else
                                                "Aplicada: ${formatFecha(record.appliedAt)}",
                                            color = if (citaVencida) Color(0xFFFFAA55) else PurpleTextSub,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (record.nextDoseAt != null && record.nextDoseAt > 0L) {
                                        Text("Próximo refuerzo: ${formatFecha(record.nextDoseAt)}", color = PurpleAccent, fontSize = 12.sp)
                                    }
                                    // Parsear campos guardados en notes
                                    if (record.notes.isNotBlank()) {
                                        record.notes.split(" | ").forEach { parte ->
                                            Text(parte, color = PurpleTextSub, fontSize = 11.sp)
                                        }
                                    }
                                    if (record.alarmEnabled && !citaVencida) {
                                        Text("Recordatorio: activado", color = Color(0xFF88FF88), fontSize = 11.sp)
                                    }
                                }
                                Column {
                                    IconButton(
                                        onClick = {
                                            editingRecordId = record.id
                                            vacunaSeleccionada = record.vaccineName
                                            yaRecibida = record.appliedAt > 0L && !record.alarmEnabled
                                            activarAlarma = record.alarmEnabled
                                            fechaAplicacion = if (record.appliedAt > 0L) record.appliedAt else null
                                            fechaRefuerzo = record.nextDoseAt
                                            tipoDosis = record.doseLabel
                                            val partes = record.notes.split(" | ")
                                            medicoAplicador = partes.find { it.startsWith("Profesional:") }?.removePrefix("Profesional: ")?.trim() ?: ""
                                            loteVacuna = partes.find { it.startsWith("Lote:") }?.removePrefix("Lote: ")?.trim() ?: ""
                                            lugarAplicacion = partes.find { it.startsWith("Lugar:") }?.removePrefix("Lugar: ")?.trim() ?: ""
                                            notasVacuna = partes.find { it.startsWith("Nota:") }?.removePrefix("Nota: ")?.trim() ?: ""
                                            mostrarFormulario = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PurpleAccent)
                                    }
                                    IconButton(
                                        onClick = { recordToDelete = record }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF6B6B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación de eliminación
        if (recordToDelete != null) {
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text("¿Eliminar registro?", color = PurpleTextMain) },
                text = { Text("Se eliminará el registro de \"${recordToDelete!!.vaccineName}\". Esta acción no se puede deshacer.", color = PurpleTextSub) },
                confirmButton = {
                    TextButton(onClick = {
                        val rec = recordToDelete!!
                        recordToDelete = null
                        coroutineScope.launch(Dispatchers.IO) {
                            database.vaccinationRecordDao().eliminar(rec)
                        }
                    }) {
                        Text("Eliminar", color = Color(0xFFFF6B6B))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text("Cancelar", color = PurpleAccent)
                    }
                },
                containerColor = PurpleCardBg
            )
        }


        // ── DIÁLOGO FORMULARIO DE VACUNA ──────────────────────────────────────
        if (mostrarFormulario) {
            val purpleFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = PurpleFieldBorder,
                focusedLabelColor = PurpleAccent,
                unfocusedLabelColor = PurpleTextSub,
                cursorColor = PurpleAccent,
                focusedTextColor = PurpleTextMain,
                unfocusedTextColor = PurpleTextMain,
                focusedTrailingIconColor = PurpleAccent,
                unfocusedTrailingIconColor = PurpleFieldBorder
            )
            AlertDialog(
                onDismissRequest = {
                    mostrarFormulario = false
                    editingRecordId = null
                },
                containerColor = PurpleCardBg,
                title = {
                    Text(
                        if (editingRecordId != null) "Editar vacuna" else "Nueva vacuna",
                        color = PurpleTextMain,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Selector de vacuna
                        ExposedDropdownMenuBox(
                            expanded = expandedVacuna,
                            onExpandedChange = { expandedVacuna = it }
                        ) {
                            OutlinedTextField(
                                value = vacunaSeleccionada,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Seleccione la vacuna") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVacuna) },
                                colors = purpleFieldColors,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedVacuna,
                                onDismissRequest = { expandedVacuna = false },
                                modifier = Modifier.background(PurpleCardBg)
                            ) {
                                vacunasFiltradas.forEach { vacuna ->
                                    DropdownMenuItem(
                                        text = { Text(vacuna, color = PurpleTextMain) },
                                        onClick = { vacunaSeleccionada = vacuna; expandedVacuna = false }
                                    )
                                }
                            }
                        }

                        // Checkbox principal
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = yaRecibida,
                                onCheckedChange = {
                                    yaRecibida = it
                                    if (it) activarAlarma = false else activarAlarma = true
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PurpleMetalLight,
                                    uncheckedColor = PurpleFieldBorder,
                                    checkmarkColor = PurpleTextMain
                                )
                            )
                            Text("Ya recibí esta vacuna", fontWeight = FontWeight.Medium, color = PurpleTextMain)
                        }

                        // ── SECCIÓN DETALLE (visible solo si yaRecibida = true) ──
                        if (yaRecibida) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PurpleCardDetail),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Detalle del registro (opcional)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = PurpleAccent
                                    )

                                    OutlinedTextField(
                                        value = formatFecha(fechaAplicacion),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Fecha en que se registró") },
                                        colors = purpleFieldColors,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                DatePickerDialog(
                                                    context,
                                                    { _, year, month, day ->
                                                        fechaAplicacion = Calendar.getInstance()
                                                            .apply { set(year, month, day, 0, 0, 0) }.timeInMillis
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = medicoAplicador,
                                        onValueChange = { medicoAplicador = it },
                                        label = { Text("Profesional que lo registró") },
                                        colors = purpleFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = lugarAplicacion,
                                        onValueChange = { lugarAplicacion = it },
                                        label = { Text("Lugar / Centro de atención") },
                                        colors = purpleFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = expandedTipoDosis,
                                        onExpandedChange = { expandedTipoDosis = it }
                                    ) {
                                        OutlinedTextField(
                                            value = tipoDosis,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Tipo de dosis") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipoDosis) },
                                            colors = purpleFieldColors,
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedTipoDosis,
                                            onDismissRequest = { expandedTipoDosis = false },
                                            modifier = Modifier.background(PurpleCardBg)
                                        ) {
                                            tiposDosis.forEach { tipo ->
                                                DropdownMenuItem(
                                                    text = { Text(tipo, color = PurpleTextMain) },
                                                    onClick = { tipoDosis = tipo; expandedTipoDosis = false }
                                                )
                                            }
                                        }
                                    }

                                    if (tipoDosis != "Dosis única") {
                                        OutlinedTextField(
                                            value = formatFecha(fechaRefuerzo),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Fecha próximo refuerzo / siguiente dosis") },
                                            colors = purpleFieldColors,
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            fechaRefuerzo = Calendar.getInstance()
                                                                .apply { set(year, month, day, 0, 0, 0) }.timeInMillis
                                                        },
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH),
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha refuerzo", tint = PurpleAccent)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    OutlinedTextField(
                                        value = loteVacuna,
                                        onValueChange = { loteVacuna = it },
                                        label = { Text("Número de lote") },
                                        colors = purpleFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = notasVacuna,
                                        onValueChange = { notasVacuna = it },
                                        label = { Text("Notas / observaciones") },
                                        colors = purpleFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            }
                        }

                        // ── SECCIÓN PRÓXIMA CITA (solo si NO ya recibida) ────
                        if (!yaRecibida) {
                            OutlinedButton(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            fechaAplicacion = Calendar.getInstance()
                                                .apply { set(year, month, day, 0, 0, 0) }.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleFieldBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleTextMain),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PurpleAccent)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (fechaAplicacion != null) "Cita: ${formatFecha(fechaAplicacion)}"
                                    else "Seleccionar fecha de cita"
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Activar Alarma / Recordatorio", color = PurpleTextMain)
                                Switch(
                                    checked = activarAlarma,
                                    onCheckedChange = { activarAlarma = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PurpleTextMain,
                                        checkedTrackColor = PurpleMetalLight,
                                        uncheckedThumbColor = PurpleTextSub,
                                        uncheckedTrackColor = PurpleCardBg
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    IconButton(
                        onClick = {
                            val activePatient = usuarioActivo
                            if (activePatient == null) {
                                Toast.makeText(context, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val isEditing = editingRecordId != null
                            val record = VaccinationRecord(
                                id = editingRecordId ?: 0,
                                patientId = activePatient.id,
                                vaccineName = vacunaSeleccionada,
                                doseLabel = if (yaRecibida) tipoDosis else "Cita pendiente",
                                appliedAt = fechaAplicacion ?: System.currentTimeMillis(),
                                nextDoseAt = fechaRefuerzo,
                                alarmEnabled = activarAlarma,
                                notes = listOf(
                                    medicoAplicador.trim().let { if (it.isNotBlank()) "Profesional: $it" else "" },
                                    lugarAplicacion.trim().let { if (it.isNotBlank()) "Lugar: $it" else "" },
                                    loteVacuna.trim().let { if (it.isNotBlank()) "Lote: $it" else "" },
                                    notasVacuna.trim()
                                ).filter { it.isNotBlank() }.joinToString(" | ")
                            )
                            coroutineScope.launch(Dispatchers.IO) {
                                val id = database.vaccinationRecordDao().guardar(record)
                                if (activarAlarma && fechaAplicacion != null) {
                                    VaccinationScheduler(context).programar(record.copy(id = id.toInt()))
                                }
                            }
                            editingRecordId = null
                            mostrarFormulario = false
                            Toast.makeText(context, if (isEditing) "Registro actualizado" else "Registro guardado", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PurpleMetalLight)
                    }
                },
                dismissButton = {
                    IconButton(onClick = {
                        mostrarFormulario = false
                        editingRecordId = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = PurpleTextSub)
                    }
                }
            )
        }
    }
}
