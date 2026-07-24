package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalReport
import kotlinx.coroutines.flow.flowOf
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormularioCitaMedicaPanel(
    mostrarFormularioCitaMedica: Boolean,
    editingAppointmentId: Int?,
    profesionalCitaMedicaState: MutableState<String>,
    fechaCitaMedicaState: MutableState<Long>,
    notasCitaMedicaState: MutableState<String>,
    lugarCitaMedicaState: MutableState<String>,
    alarmaCitaMedicaActivaState: MutableState<Boolean>,
    alarmaSonidoNombre: String,
    recordatorioCitaMinutosState: MutableState<Int>,
    expandedRecordatorioCitaState: MutableState<Boolean>,
    opcionesRecordatorioCita: List<Int>,
    onFormatReminderMinutesLabel: (Int) -> String,
    onGuardarCitaMedica: () -> Unit,
    onResetCitaMedica: () -> Unit,
    mostrarPanelCitasMedicasState: MutableState<Boolean>,
    mostrarFormularioCitaMedicaState: MutableState<Boolean>,
    citaPendienteDeEliminarState: MutableState<MedicalAppointment?>,
    citaMedicaSeleccionadaIdState: MutableState<Int?>,
    reportePendienteDeEliminarState: MutableState<MedicalReport?>,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onFormatDateTime: (Long) -> String,
    database: com.carlos.controlmedicamentos.data.local.AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    var profesionalCitaMedica by profesionalCitaMedicaState
    var fechaCitaMedica by fechaCitaMedicaState
    var notasCitaMedica by notasCitaMedicaState
    var lugarCitaMedica by lugarCitaMedicaState
    var alarmaCitaMedicaActiva by alarmaCitaMedicaActivaState
    var recordatorioCitaMinutos by recordatorioCitaMinutosState
    var expandedRecordatorioCita by expandedRecordatorioCitaState
    var citaPendienteDeEliminar by citaPendienteDeEliminarState
    var citaMedicaSeleccionadaId by citaMedicaSeleccionadaIdState
    var reportePendienteDeEliminar by reportePendienteDeEliminarState
    var mostrarPanelCitasMedicas by mostrarPanelCitasMedicasState
    var mostrarFormularioCitaMedicaLocal by mostrarFormularioCitaMedicaState

    val pacienteActivo by database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)
    val profesionalesHabituales by remember(pacienteActivo?.id) {
        val id = pacienteActivo?.id
        if (id != null) database.medicalPractitionerDao().observarPorPaciente(id)
        else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var expandedProfesionalCita by remember { mutableStateOf(false) }

    val formatoMedico: (com.carlos.controlmedicamentos.data.local.MedicalPractitioner) -> String = {
        if (it.specialty.isNotBlank()) "${it.name} — ${it.specialty}" else it.name
    }

    if (mostrarFormularioCitaMedica) {
        MetallicMedicationCard(
            modifier = Modifier.fillMaxSize(),
            contentPadding = 16,
            verticalSpacing = 8,
            expandVertically = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(panelInternoScrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (editingAppointmentId == null) "Nueva cita" else "Editar cita")
                val opcionesProfesionalesCita = profesionalesHabituales.map(formatoMedico)
                val profesionalSeleccionadoCita = profesionalesHabituales.find { it.name == profesionalCitaMedica }?.let(formatoMedico) ?: profesionalCitaMedica
                VademecumDropdown(
                    label = "Profesional",
                    options = opcionesProfesionalesCita,
                    selectedValue = profesionalSeleccionadoCita,
                    expanded = expandedProfesionalCita,
                    onExpandedChange = { expandedProfesionalCita = !expandedProfesionalCita },
                    onDismiss = { expandedProfesionalCita = false },
                    onSelect = { selected ->
                        profesionalesHabituales.find { formatoMedico(it) == selected }?.let { profesionalCitaMedica = it.name }
                        expandedProfesionalCita = false
                    },
                    emptyOptionsText = "No hay médicos guardados"
                )
                OutlinedTextField(
                    value = onFormatDateTime(fechaCitaMedica),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dia mes año") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val calendario = Calendar.getInstance().apply { timeInMillis = fechaCitaMedica }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val baseSeleccionada = Calendar.getInstance().apply {
                                            timeInMillis = fechaCitaMedica
                                            set(year, month, dayOfMonth)
                                        }
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val fechaActualizada = Calendar.getInstance().apply {
                                                    timeInMillis = baseSeleccionada.timeInMillis
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                fechaCitaMedica = fechaActualizada.timeInMillis
                                            },
                                            calendario.get(Calendar.HOUR_OF_DAY),
                                            calendario.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    calendario.get(Calendar.YEAR),
                                    calendario.get(Calendar.MONTH),
                                    calendario.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar fecha de cita")
                        }
                    }
                )
                OutlinedTextField(
                    value = notasCitaMedica,
                    onValueChange = { notasCitaMedica = it },
                    label = { Text("Documentacion o pruebas a llevar") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                OutlinedTextField(
                    value = lugarCitaMedica,
                    onValueChange = { lugarCitaMedica = it },
                    label = { Text("Lugar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Alarma critica")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (alarmaCitaMedicaActiva) "Activada" else "Desactivada")
                    Switch(
                        checked = alarmaCitaMedicaActiva,
                        onCheckedChange = { alarmaCitaMedicaActiva = it }
                    )
                }
                Text("Sonido critico actual: $alarmaSonidoNombre")
                VademecumDropdown(
                    label = "Antelacion del recordatorio",
                    options = opcionesRecordatorioCita.map { onFormatReminderMinutesLabel(it) },
                    selectedValue = onFormatReminderMinutesLabel(recordatorioCitaMinutos),
                    expanded = expandedRecordatorioCita,
                    onExpandedChange = { expandedRecordatorioCita = !expandedRecordatorioCita },
                    onDismiss = { expandedRecordatorioCita = false },
                    onSelect = { seleccionado ->
                        recordatorioCitaMinutos = opcionesRecordatorioCita.firstOrNull {
                            onFormatReminderMinutesLabel(it) == seleccionado
                        } ?: 60
                        expandedRecordatorioCita = false
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onGuardarCitaMedica() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                    Button(
                        onClick = {
                            onResetCitaMedica()
                            mostrarFormularioCitaMedicaLocal = false
                            mostrarPanelCitasMedicas = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    citaPendienteDeEliminar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaPendienteDeEliminar = null },
            title = { Text("Eliminar cita") },
            text = {
                Text("Si eliminas la cita perderas todos los datos con referencia a esta cita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        citaPendienteDeEliminar = null
                        coroutineScope.launch(Dispatchers.IO) {
                            MedicalAppointmentScheduler(context).cancelar(cita.id)
                            database.medicalAppointmentDao().eliminar(cita)
                            withContext(Dispatchers.Main) {
                                if (citaMedicaSeleccionadaId == cita.id) {
                                    citaMedicaSeleccionadaId = null
                                }
                                Toast.makeText(context, "Cita eliminada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { citaPendienteDeEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    reportePendienteDeEliminar?.let { reporte ->
        AlertDialog(
            onDismissRequest = { reportePendienteDeEliminar = null },
            title = { Text("Eliminar documento") },
            text = { Text("¿Estas seguro de eliminar \"${reporte.titulo}\"? Esta accion no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        reportePendienteDeEliminar = null
                        coroutineScope.launch(Dispatchers.IO) {
                            database.medicalReportDao().eliminar(reporte)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Documento eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Confirmar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { reportePendienteDeEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                }
            }
        )
    }
}
