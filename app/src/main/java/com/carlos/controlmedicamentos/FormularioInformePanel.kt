package com.carlos.controlmedicamentos

import android.Manifest
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormularioInformePanel(
    mostrarFormularioInforme: Boolean,
    tituloInforme: String,
    descripcionInforme: String,
    expandedProfesionalInforme: Boolean,
    practitionerIdInforme: Int?,
    profesionalesHabituales: List<MedicalPractitioner>,
    estudiosAdjuntos: SnapshotStateList<String>,
    visorAdjuntos: AttachmentViewerState?,
    tienePermisoCamara: Boolean,
    cameraPermissionPending: Boolean,
    onTituloInformeChange: (String) -> Unit,
    onDescripcionInformeChange: (String) -> Unit,
    onExpandedProfesionalInformeChange: (Boolean) -> Unit,
    onPractitionerIdInformeChange: (Int?) -> Unit,
    onVisorAdjuntosChange: (AttachmentViewerState?) -> Unit,
    onCameraPermissionPendingChange: (Boolean) -> Unit,
    onGuardarInformeMedicoActual: () -> Unit,
    onInformeMedicoTieneCambiosSinGuardar: () -> Boolean,
    onCerrarFormularioInforme: () -> Unit,
    onMostrarDialogoCerrarInformeSinGuardarChange: (Boolean) -> Unit,
    onLaunchDocumentScanner: () -> Unit,
    cameraPermissionLauncher: ActivityResultLauncher<String>,
    pickStudyImagesLauncher: ActivityResultLauncher<String>
) {
    if (!mostrarFormularioInforme) return

    val context = LocalContext.current
    val formularioScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val bg = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF030D1F), Color(0xFF0A2A4B),
                        Color(0xFF1768A3), Color(0xFF0C3451), Color(0xFF030D1F)
                    )
                )
                onDrawBehind { drawRect(brush = bg) }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(formularioScrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Formulario de documento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { onGuardarInformeMedicoActual() }) {
                Icon(Icons.Filled.Save, contentDescription = "Guardar informe", tint = Color.White)
            }
            IconButton(
                onClick = {
                    if (onInformeMedicoTieneCambiosSinGuardar()) {
                        onMostrarDialogoCerrarInformeSinGuardarChange(true)
                    } else {
                        onCerrarFormularioInforme()
                    }
                }
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
        OutlinedTextField(
            value = tituloInforme,
            onValueChange = { onTituloInformeChange(it) },
            label = { Text("Titulo del informe") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenuBox(
            expanded = expandedProfesionalInforme,
            onExpandedChange = { onExpandedProfesionalInformeChange(!expandedProfesionalInforme) }
        ) {
            val medicoInformeNombre = profesionalesHabituales.find { it.id == practitionerIdInforme }
                ?.let { "${it.name} — ${it.specialty}" }
                ?: "Sin profesional asociado"
            OutlinedTextField(
                value = medicoInformeNombre,
                onValueChange = {},
                readOnly = true,
                label = { Text("Profesional") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProfesionalInforme) }
            )
            ExposedDropdownMenu(
                expanded = expandedProfesionalInforme,
                onDismissRequest = { onExpandedProfesionalInformeChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Sin profesional asociado") },
                    onClick = { onPractitionerIdInformeChange(null); onExpandedProfesionalInformeChange(false) }
                )
                profesionalesHabituales.forEach { profesional ->
                    DropdownMenuItem(
                        text = { Text("${profesional.name} — ${profesional.specialty}") },
                        onClick = { onPractitionerIdInformeChange(profesional.id); onExpandedProfesionalInformeChange(false) }
                    )
                }
            }
        }
        OutlinedTextField(
            value = descripcionInforme,
            onValueChange = { onDescripcionInformeChange(it) },
            label = { Text("Descripcion o hallazgos") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (tienePermisoCamara) {
                        onLaunchDocumentScanner()
                    } else {
                        onCameraPermissionPendingChange(true)
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Escanear")
            }
            Button(
                onClick = { pickStudyImagesLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Galeria")
            }
        }
        if (estudiosAdjuntos.isNotEmpty()) {
            estudiosAdjuntos.forEachIndexed { index, path ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttachmentThumbnail(
                        path = path,
                        modifier = Modifier.size(72.dp),
                        onClick = {
                            onVisorAdjuntosChange(
                                AttachmentViewerState(
                                    paths = estudiosAdjuntos.toList(),
                                    currentIndex = index
                                )
                            )
                        }
                    )
                    Text(attachmentDisplayName(path, index), modifier = Modifier.weight(1f), color = Color.White)
                    Button(onClick = { estudiosAdjuntos.remove(path) }) {
                        Text("Quitar")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
