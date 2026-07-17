package com.carlos.controlmedicamentos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalReport

@Composable
internal fun FormularioProfesionalPanel(
    mostrarFormularioProfesional: Boolean,
    editingPractitionerId: Int?,
    nombreProfesional: String,
    especialidadProfesional: String,
    proximaCitaMedico: MedicalAppointment?,
    informesSincronizadosMedico: List<MedicalReport>,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onNombreProfesionalChange: (String) -> Unit,
    onEspecialidadProfesionalChange: (String) -> Unit,
    onGuardarMedicoHabitualActual: () -> Unit,
    onResetMedicoHabitual: () -> Unit,
    onMostrarFormularioProfesionalChange: (Boolean) -> Unit,
    onMostrarPanelProfesionalesChange: (Boolean) -> Unit
) {
    if (!mostrarFormularioProfesional) return

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
            Text(if (editingPractitionerId == null) "Nuevo médico" else "Editar médico")
            OutlinedTextField(
                value = nombreProfesional,
                onValueChange = { onNombreProfesionalChange(it) },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = especialidadProfesional,
                onValueChange = { onEspecialidadProfesionalChange(it) },
                label = { Text("Especialidad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                Text("Próxima cita")
                Text(
                    proximaCitaMedico?.let {
                        buildString {
                            append(formatDateTimeMain(it.scheduledAt))
                            if (it.title.isNotBlank()) append(" | ${it.title}")
                            if (it.location.isNotBlank()) append(" | ${it.location}")
                        }
                    } ?: "No hay una cita futura sincronizada para este médico."
                )
            }

            TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                Text("Vista de informes")
                if (informesSincronizadosMedico.isEmpty()) {
                    Text("No hay informes sincronizados para mostrar desde el panel de informes médicos.")
                } else {
                    informesSincronizadosMedico.forEach { reporte ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(reporte.titulo, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (reporte.descripcion.isBlank()) {
                                    "Sin descripcion"
                                } else {
                                    reporte.descripcion.take(120)
                                }
                            )
                            Text("Registro: ${formatDateTimeMain(reporte.createdAt)}")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onGuardarMedicoHabitualActual() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
                Button(
                    onClick = {
                        onResetMedicoHabitual()
                        onMostrarFormularioProfesionalChange(false)
                        onMostrarPanelProfesionalesChange(true)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}
