package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.backup.AutoBackupScheduler
import com.carlos.controlmedicamentos.backup.BackupSelection
import com.carlos.controlmedicamentos.notifications.AnticonceptivoScheduler
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.notifications.VaccinationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DialogosPrincipalesPanel(
    perfilPendienteDeEliminarState: MutableState<PatientProfile?>,
    mostrarDialogoBackupManualState: MutableState<Boolean>,
    mostrarDialogoRestoreSeleccionState: MutableState<Boolean>,
    mostrarDialogoProgramarBackupState: MutableState<Boolean>,
    backupSelectionState: MutableState<BackupSelection>,
    restoreSelectionState: MutableState<BackupSelection>,
    backupPatientIdState: MutableState<Int?>,
    restorePatientIdState: MutableState<Int?>,
    ejecutandoBackupManualState: MutableState<Boolean>,
    restaurandoBackupState: MutableState<Boolean>,
    frecuenciaBackupSeleccionada: String,
    horaBackupSeleccionada: Int,
    minutoBackupSeleccionado: Int,
    pacienteActivoNombre: String?,
    pacienteActivoId: Int?,
    editingPatientId: Int?,
    database: AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onCerrarPanelesSecundarios: () -> Unit,
    onMostrarPanelBackups: () -> Unit,
    onResetFichaPaciente: () -> Unit,
    onMostrarFichaPacienteChange: (Boolean) -> Unit,
    onMensajeBackupChange: (String) -> Unit,
    onLanzarCreateBackup: (String) -> Unit,
    onLanzarRestoreBackup: () -> Unit,
    onTimestampArchivo: () -> String
) {
    val context = LocalContext.current
    var perfilPendienteDeEliminar by perfilPendienteDeEliminarState
    var mostrarDialogoBackupManual by mostrarDialogoBackupManualState
    var mostrarDialogoRestoreSeleccion by mostrarDialogoRestoreSeleccionState
    var mostrarDialogoProgramarBackup by mostrarDialogoProgramarBackupState
    var backupSelection by backupSelectionState
    var restoreSelection by restoreSelectionState
    var backupPatientId by backupPatientIdState
    var restorePatientId by restorePatientIdState
    var ejecutandoBackupManual by ejecutandoBackupManualState
    var restaurandoBackup by restaurandoBackupState

    perfilPendienteDeEliminar?.let { perfil ->
        AlertDialog(
            onDismissRequest = { perfilPendienteDeEliminar = null },
            title = { Text("Eliminar perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("La eliminacion del perfil es definitiva y no se puede recuperar.")
                    Text("Solo seria posible la recuperacion si antes se hizo un backup.")
                    Text("Perfil: ${perfil.nombre} ${perfil.apellidos}")
                    Button(
                        onClick = {
                            perfilPendienteDeEliminar = null
                            onCerrarPanelesSecundarios()
                            onMostrarPanelBackups()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Ir a backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ir a backup")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val perfilAEliminar = perfil
                        perfilPendienteDeEliminar = null
                        coroutineScope.launch(Dispatchers.IO) {
                            val medicationScheduler = MedicationScheduler(context)
                            val appointmentScheduler = MedicalAppointmentScheduler(context)
                            val vaccinationScheduler = VaccinationScheduler(context)
                            val medications = database.medicationDao().obtenerTodosPorPacienteLista(perfilAEliminar.id)
                            val appointments = database.medicalAppointmentDao().obtenerTodosLista()
                                .filter { it.patientId == perfilAEliminar.id }
                            val vaccinations = database.vaccinationRecordDao().obtenerTodosLista()
                                .filter { it.patientId == perfilAEliminar.id }

                            medications.forEach { medicationScheduler.cancelarAlarma(it.id) }
                            appointments.forEach { appointmentScheduler.cancelar(it.id) }
                            vaccinations.forEach { vaccinationScheduler.cancelar(it.id) }

                            database.medicationIntakeDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.medicalReportDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.medicalAppointmentDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.medicalPractitionerDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.vaccinationRecordDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.medicationOrderDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.medicationDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.signosVitalesDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.physicalActivityDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.cicloMenstrualDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.controlEmbarazoDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.carritoPendienteDao().limpiarPorPaciente(perfilAEliminar.id)
                            val metodos = database.metodoAnticonceptivoDao().obtenerActivos(perfilAEliminar.id)
                            val anticonceptivoScheduler = AnticonceptivoScheduler(context)
                            metodos.forEach {
                                database.anticonceptivoIntakeDao().eliminarPorMetodoId(it.id)
                                anticonceptivoScheduler.cancelar(it.id)
                            }
                            database.metodoAnticonceptivoDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.bebeRecienNacidoDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.ninoDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.diarioEntryDao().eliminarPorPaciente(perfilAEliminar.id)
                            database.patientProfileDao().eliminarPorId(perfilAEliminar.id)
                            clearPersistedBirthday(context, perfilAEliminar.id)
                            NotificacionHelper.cancelarTomasPerdidas(context)

                            val perfilesRestantes = database.patientProfileDao().obtenerTodosLista()
                            if (perfilesRestantes.isNotEmpty()) {
                                database.patientProfileDao().desactivarTodos()
                                database.patientProfileDao().activarPaciente(perfilesRestantes.first().id)
                            }

                            withContext(Dispatchers.Main) {
                                if (editingPatientId == perfilAEliminar.id) {
                                    onResetFichaPaciente()
                                    onMostrarFichaPacienteChange(false)
                                }
                                Toast.makeText(context, "Perfil eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F1C28))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Confirmar eliminacion")
                }
            },
            dismissButton = {
                Button(
                    onClick = { perfilPendienteDeEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34507A))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoBackupManual) {
        val allPatients by database.patientProfileDao().observarTodos().collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { mostrarDialogoBackupManual = false },
            title = { Text("Crear backup manual") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Selecciona el paciente:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = backupPatientId == null,
                            onClick = { backupPatientId = null }
                        )
                        Text("Todos los pacientes", modifier = Modifier.padding(start = 8.dp))
                    }
                    allPatients.forEach { patient ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = backupPatientId == patient.id,
                                onClick = { backupPatientId = patient.id }
                            )
                            Text(patient.nombre, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selecciona los datos a incluir en el backup:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.patients, onCheckedChange = { backupSelection = backupSelection.copy(patients = it) })
                        Text("Perfiles de pacientes")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.medications, onCheckedChange = { backupSelection = backupSelection.copy(medications = it) })
                        Text("Medicamentos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.vitalSigns, onCheckedChange = { backupSelection = backupSelection.copy(vitalSigns = it) })
                        Text("Signos vitales")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.intakes, onCheckedChange = { backupSelection = backupSelection.copy(intakes = it) })
                        Text("Tomas de medicamentos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.appointments, onCheckedChange = { backupSelection = backupSelection.copy(appointments = it) })
                        Text("Citas médicas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.reports, onCheckedChange = { backupSelection = backupSelection.copy(reports = it) })
                        Text("Informes médicos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.vaccinations, onCheckedChange = { backupSelection = backupSelection.copy(vaccinations = it) })
                        Text("Vacunas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.physicalActivities, onCheckedChange = { backupSelection = backupSelection.copy(physicalActivities = it) })
                        Text("Actividades físicas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.registrosSedentarismo, onCheckedChange = { backupSelection = backupSelection.copy(registrosSedentarismo = it) })
                        Text("Sedentarismo (registros)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.configSedentarismo, onCheckedChange = { backupSelection = backupSelection.copy(configSedentarismo = it) })
                        Text("Sedentarismo (configuración)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.registrosHidratacion, onCheckedChange = { backupSelection = backupSelection.copy(registrosHidratacion = it) })
                        Text("Hidratación")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupSelection.fallAlerts, onCheckedChange = { backupSelection = backupSelection.copy(fallAlerts = it) })
                        Text("Alertas de caídas")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { backupSelection = BackupSelection.all() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Seleccionar todo") }
                    Button(
                        onClick = { backupSelection = BackupSelection.none() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Deseleccionar todo") }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoBackupManual = false
                        ejecutandoBackupManual = true
                        onLanzarCreateBackup("controlmedicamentos-backup-${onTimestampArchivo()}.json")
                    }
                ) { Text("Hacer backup") }
            },
            dismissButton = {
                IconButton(onClick = { mostrarDialogoBackupManual = false }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoRestoreSeleccion) {
        val allPatients by database.patientProfileDao().observarTodos().collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { mostrarDialogoRestoreSeleccion = false },
            title = { Text("Restaurar backup") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Selecciona el paciente:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = restorePatientId == null,
                            onClick = { restorePatientId = pacienteActivoId }
                        )
                        Text("Paciente actual (${pacienteActivoNombre ?: "Sin nombre"})", modifier = Modifier.padding(start = 8.dp))
                    }
                    allPatients.forEach { patient ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = restorePatientId == patient.id,
                                onClick = { restorePatientId = patient.id }
                            )
                            Text(patient.nombre, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selecciona los datos a restaurar:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.patients, onCheckedChange = { restoreSelection = restoreSelection.copy(patients = it) })
                        Text("Perfiles de pacientes")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.medications, onCheckedChange = { restoreSelection = restoreSelection.copy(medications = it) })
                        Text("Medicamentos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.vitalSigns, onCheckedChange = { restoreSelection = restoreSelection.copy(vitalSigns = it) })
                        Text("Signos vitales")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.intakes, onCheckedChange = { restoreSelection = restoreSelection.copy(intakes = it) })
                        Text("Tomas de medicamentos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.appointments, onCheckedChange = { restoreSelection = restoreSelection.copy(appointments = it) })
                        Text("Citas médicas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.reports, onCheckedChange = { restoreSelection = restoreSelection.copy(reports = it) })
                        Text("Informes médicos")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.vaccinations, onCheckedChange = { restoreSelection = restoreSelection.copy(vaccinations = it) })
                        Text("Vacunas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.physicalActivities, onCheckedChange = { restoreSelection = restoreSelection.copy(physicalActivities = it) })
                        Text("Actividades físicas")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.registrosSedentarismo, onCheckedChange = { restoreSelection = restoreSelection.copy(registrosSedentarismo = it) })
                        Text("Sedentarismo (registros)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.configSedentarismo, onCheckedChange = { restoreSelection = restoreSelection.copy(configSedentarismo = it) })
                        Text("Sedentarismo (configuración)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.registrosHidratacion, onCheckedChange = { restoreSelection = restoreSelection.copy(registrosHidratacion = it) })
                        Text("Hidratación")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSelection.fallAlerts, onCheckedChange = { restoreSelection = restoreSelection.copy(fallAlerts = it) })
                        Text("Alertas de caídas")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { restoreSelection = BackupSelection.all() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Seleccionar todo") }
                    Button(
                        onClick = { restoreSelection = BackupSelection.none() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Deseleccionar todo") }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoRestoreSeleccion = false
                        restaurandoBackup = true
                        onLanzarRestoreBackup()
                    }
                ) { Text("Seleccionar archivo") }
            },
            dismissButton = {
                IconButton(onClick = { mostrarDialogoRestoreSeleccion = false }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoProgramarBackup) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoProgramarBackup = false },
            title = { Text("Programar backup") },
            text = {
                Text(
                    when (frecuenciaBackupSeleccionada) {
                        AutoBackupScheduler.FREQUENCY_DAILY -> "Se programara un backup automatico diario a las ${String.format("%02d:%02d", horaBackupSeleccionada, minutoBackupSeleccionado)}."
                        AutoBackupScheduler.FREQUENCY_WEEKLY -> "Se programara un backup automatico semanal a las ${String.format("%02d:%02d", horaBackupSeleccionada, minutoBackupSeleccionado)}."
                        else -> "Se desactivaran los backups automaticos."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoProgramarBackup = false
                        AutoBackupScheduler.applySchedule(context, frecuenciaBackupSeleccionada, horaBackupSeleccionada, minutoBackupSeleccionado)
                        onMensajeBackupChange("Programacion de backup actualizada")
                        Toast.makeText(context, "Programacion de backup actualizada", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                Button(onClick = { mostrarDialogoProgramarBackup = false }) { Text("Cancelar") }
            }
        )
    }
}
