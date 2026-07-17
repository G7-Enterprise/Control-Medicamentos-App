package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PanelProfesionalesPanel(
    mostrarPanelProfesionales: Boolean,
    pacienteActivo: com.carlos.controlmedicamentos.data.local.PatientProfile?,
    profesionalSeleccionadoId: Int?,
    medicoSeleccionado: MedicalPractitioner?,
    profesionalesHabituales: List<MedicalPractitioner>,
    citasMedicas: List<MedicalAppointment>,
    reportesSalud: List<MedicalReport>,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onProfesionalSeleccionadoIdChange: (Int?) -> Unit,
    onMostrarPanelProfesionalesChange: (Boolean) -> Unit,
    onMostrarFormularioProfesionalChange: (Boolean) -> Unit,
    onMostrarFormularioChange: (Boolean) -> Unit,
    onMostrarPanelInformesChange: (Boolean) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit,
    onAbrirFormularioMedico: (MedicalPractitioner?) -> Unit
) {
    val context = LocalContext.current

    if (!mostrarPanelProfesionales) return

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
            Text(
                "Médicos y especialistas",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            if (pacienteActivo == null) {
                Text("Selecciona un paciente para gestionar sus médicos habituales.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onProfesionalSeleccionadoIdChange(null)
                            onAbrirFormularioMedico(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nuevo")
                    }
                    Button(
                        onClick = {
                            val seleccionado = medicoSeleccionado
                            if (seleccionado == null) {
                                Toast.makeText(context, "Selecciona un médico primero", Toast.LENGTH_SHORT).show()
                            } else {
                                onAbrirFormularioMedico(seleccionado)
                            }
                        },
                        enabled = medicoSeleccionado != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar")
                    }
                    Button(
                        onClick = {
                            val seleccionado = medicoSeleccionado
                            if (seleccionado == null) {
                                Toast.makeText(context, "Selecciona un médico primero", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch(Dispatchers.IO) {
                                database.medicalPractitionerDao().eliminar(seleccionado)
                                withContext(Dispatchers.Main) {
                                    onProfesionalSeleccionadoIdChange(null)
                                    Toast.makeText(context, "Médico eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = medicoSeleccionado != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Eliminar")
                    }
                }

                if (profesionalesHabituales.isEmpty()) {
                    Text("Todavía no hay médicos ni especialistas guardados para este usuario.")
                } else {
                    profesionalesHabituales.forEach { practitioner ->
                        val estaSeleccionado = practitioner.id == profesionalSeleccionadoId
                        val proximaCita = citasMedicas
                            .filter { cita ->
                                val citaNombre = cita.doctorName.trim()
                                val pracNombre = practitioner.name.trim()
                                cita.practitionerId == practitioner.id ||
                                    citaNombre.equals(pracNombre, ignoreCase = true) ||
                                    citaNombre.contains(pracNombre, ignoreCase = true) ||
                                    pracNombre.contains(citaNombre, ignoreCase = true)
                            }
                            .filter { !it.isCompleted && it.scheduledAt >= System.currentTimeMillis() }
                            .minByOrNull { it.scheduledAt }
                        val totalInformesSincronizados = reportesSalud.count { reporte ->
                            val pracNombre = practitioner.name.trim()
                            reporte.practitionerId == practitioner.id ||
                            (reporte.practitionerId == null && pracNombre.isNotBlank() && (
                                reporte.titulo.trim().equals(pracNombre, ignoreCase = true) ||
                                reporte.titulo.trim().contains(pracNombre, ignoreCase = true) ||
                                pracNombre.contains(reporte.titulo.trim(), ignoreCase = true)
                            ))
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProfesionalSeleccionadoIdChange(practitioner.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (estaSeleccionado) Color(0xCC1E61B5) else Color(0xB4142D59),
                                contentColor = Color(0xFFF3FBFF)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(practitioner.name, fontWeight = FontWeight.SemiBold)
                                Text("Especialidad: ${practitioner.specialty}")
                                Text(
                                    if (proximaCita != null) {
                                        "Próxima cita: ${formatDateTimeMain(proximaCita.scheduledAt)}"
                                    } else {
                                        "Próxima cita: no programada"
                                    }
                                )
                                Text("Informes sincronizados: $totalInformesSincronizados")
                                if (totalInformesSincronizados > 0) {
                                    Button(
                                        onClick = {
                                            onCerrarPanelesSecundarios()
                                            onMostrarPanelInformesChange(true)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Ver informes")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al escritorio", color = Color.Black)
            }
        }
    }
}
