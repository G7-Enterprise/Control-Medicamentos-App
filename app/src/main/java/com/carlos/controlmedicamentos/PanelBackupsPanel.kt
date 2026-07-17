package com.carlos.controlmedicamentos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.backup.AutoBackupScheduler
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelBackupsPanel(
    mostrarPanelBackups: Boolean,
    frecuenciaBackupSeleccionada: String,
    expandedFrecuenciaBackup: Boolean,
    expandedHoraBackup: Boolean,
    horaBackupSeleccionada: Int,
    minutoBackupSeleccionado: Int,
    ultimoBackupAutomatico: File?,
    mensajeBackup: String,
    opcionesFrecuenciaBackup: List<String>,
    opcionesHoraBackup: List<String>,
    ejecutandoBackupManual: Boolean,
    restaurandoBackup: Boolean,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onFrecuenciaBackupSeleccionadaChange: (String) -> Unit,
    onExpandedFrecuenciaBackupChange: (Boolean) -> Unit,
    onExpandedHoraBackupChange: (Boolean) -> Unit,
    onHoraBackupSeleccionadaChange: (Int) -> Unit,
    onMinutoBackupSeleccionadoChange: (Int) -> Unit,
    onMostrarDialogoProgramarBackupChange: (Boolean) -> Unit,
    onMostrarDialogoBackupManualChange: (Boolean) -> Unit,
    onMostrarDialogoRestoreSeleccionChange: (Boolean) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit
) {
    if (!mostrarPanelBackups) return

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
                "Copias de seguridad",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                when (frecuenciaBackupSeleccionada) {
                    AutoBackupScheduler.FREQUENCY_DAILY -> "Backup automatico: diario"
                    AutoBackupScheduler.FREQUENCY_WEEKLY -> "Backup automatico: semanal"
                    else -> "Backup automatico: desactivado"
                }
            )
            if (ultimoBackupAutomatico != null) {
                Text("Ultimo backup automatico: ${formatDateTimeMain(ultimoBackupAutomatico.lastModified())}")
                Text("Ruta interna: ${ultimoBackupAutomatico.absolutePath}")
            } else {
                Text("Todavia no hay backup automatico generado.")
            }
            if (mensajeBackup.isNotBlank()) {
                Text(mensajeBackup)
            }

            VademecumDropdown(
                label = "Frecuencia programada",
                options = opcionesFrecuenciaBackup,
                selectedValue = frecuenciaBackupLabelMain(frecuenciaBackupSeleccionada),
                expanded = expandedFrecuenciaBackup,
                onExpandedChange = { onExpandedFrecuenciaBackupChange(!expandedFrecuenciaBackup) },
                onDismiss = { onExpandedFrecuenciaBackupChange(false) },
                onSelect = {
                    onFrecuenciaBackupSeleccionadaChange(
                        when (it) {
                            "Diario" -> AutoBackupScheduler.FREQUENCY_DAILY
                            "Semanal" -> AutoBackupScheduler.FREQUENCY_WEEKLY
                            else -> AutoBackupScheduler.FREQUENCY_MANUAL
                        }
                    )
                    onExpandedFrecuenciaBackupChange(false)
                }
            )

            VademecumDropdown(
                label = "Hora del backup",
                options = opcionesHoraBackup,
                selectedValue = String.format("%02d:%02d", horaBackupSeleccionada, minutoBackupSeleccionado),
                expanded = expandedHoraBackup,
                onExpandedChange = { onExpandedHoraBackupChange(!expandedHoraBackup) },
                onDismiss = { onExpandedHoraBackupChange(false) },
                onSelect = {
                    val parts = it.split(":")
                    onHoraBackupSeleccionadaChange(parts[0].toInt())
                    onMinutoBackupSeleccionadoChange(parts[1].toInt())
                    onExpandedHoraBackupChange(false)
                }
            )

            Button(
                onClick = { onMostrarDialogoProgramarBackupChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar programacion")
            }
            Button(
                onClick = { onMostrarDialogoBackupManualChange(true) },
                enabled = !ejecutandoBackupManual,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (ejecutandoBackupManual) "Creando backup..." else "Crear backup manual")
            }
            Button(
                onClick = { onMostrarDialogoRestoreSeleccionChange(true) },
                enabled = !restaurandoBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (restaurandoBackup) "Restaurando backup..." else "Restaurar backup manual")
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
