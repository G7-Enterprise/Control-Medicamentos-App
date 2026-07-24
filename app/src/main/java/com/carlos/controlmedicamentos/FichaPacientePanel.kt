package com.carlos.controlmedicamentos

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.carlos.controlmedicamentos.BirthdayCelebrationRequest
import com.carlos.controlmedicamentos.IntakeExportPeriod
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FichaPacientePanel(
    mostrarFichaPaciente: Boolean,
    editandoFichaPacienteState: MutableState<Boolean>,
    nombrePacienteState: MutableState<String>,
    apellidosPacienteState: MutableState<String>,
    fechaNacimientoPacienteState: MutableState<Long?>,
    edadPacienteState: MutableState<String>,
    pesoPacienteState: MutableState<String>,
    pesoUnidadPacienteState: MutableState<String>,
    estaturaPacienteState: MutableState<String>,
    estaturaUnidadPacienteState: MutableState<String>,
    sexoPacienteState: MutableState<String>,
    paisPacienteState: MutableState<String>,
    monedaPacienteState: MutableState<String>,
    enfermedadesPacienteState: MutableState<String>,
    prescripcionesPacienteState: MutableState<String>,
    fotoPerfilPacienteState: MutableState<String?>,
    cameraPermissionPerfilPendingState: MutableState<Boolean>,
    expandedPesoUnidadState: MutableState<Boolean>,
    expandedEstaturaUnidadState: MutableState<Boolean>,
    expandedPaisPacienteState: MutableState<Boolean>,
    pesoInputState: MutableState<String>,
    pesoUnidadKgState: MutableState<Boolean>,
    mostrarDialogoPesoSincronizadoState: MutableState<Boolean>,
    perfilPendienteDeEliminarState: MutableState<PatientProfile?>,
    editingPatientId: Int?,
    pacienteActivo: PatientProfile?,
    edadCalculadaPaciente: String,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    pickFotoPerfilLauncher: ActivityResultLauncher<String>,
    takeFotoPerfilLauncher: ActivityResultLauncher<Void?>,
    cameraPermissionPerfilLauncher: ActivityResultLauncher<String>,
    onCargarFichaPaciente: (PatientProfile) -> Unit,
    onVolverPantallaAnteriorDesdeFichaPaciente: () -> Unit,
    onSavePersistedBirthday: (Context, Int, Long) -> Unit,
    onCalcularEdadDesdeNacimiento: (Long) -> Int,
    onLoadPersistedBirthday: (Context, Int) -> Long?,
    onClearPersistedBirthday: (Context, Int) -> Unit,
    onRequestBirthdayPreview: (BirthdayCelebrationRequest) -> Unit,
    profesionalesHabituales: List<MedicalPractitioner>,
    reportesSalud: List<MedicalReport>,
    visorAdjuntosState: MutableState<AttachmentViewerState?>,
    exportandoTomasState: MutableState<Boolean>,
    periodoExportacionPendienteState: MutableState<IntakeExportPeriod?>,
    reportePendienteDeEliminarState: MutableState<MedicalReport?>,
    fechaActualTexto: String,
    exportMedicationReportLauncher: ActivityResultLauncher<String>,
    onAbrirNuevaFichaPaciente: () -> Unit,
    onResetInformeMedico: () -> Unit,
    onMostrarPanelInformesChange: (Boolean) -> Unit,
    onMostrarFormularioInformeChange: (Boolean) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit,
    onCargarInformeMedico: (MedicalReport) -> Unit,
    mostrarPanelCitasMedicas: Boolean,
    mostrarFormularioCitaMedica: Boolean,
    mostrarPanelInformes: Boolean,
    mostrarFormularioInforme: Boolean,
    citasMedicas: List<MedicalAppointment>,
    citaMedicaSeleccionadaId: Int?,
    citaMedicaSeleccionada: MedicalAppointment?,
    citaPendienteDeEliminarState: MutableState<MedicalAppointment?>,
    alarmaSonidoNombre: String,
    filtroProfesionalInformesIdState: MutableState<Int?>,
    expandedFiltroProfesionalInformesState: MutableState<Boolean>,
    citaMedicaSeleccionadaIdState: MutableState<Int?>,
    onAbrirFormularioCitaMedica: (MedicalAppointment?) -> Unit,
    panelInternoScrollState: ScrollState,
    onFormatReminderMinutesLabel: (Int) -> String,
    editingPatientIdState: MutableState<Int?>,
    onFormatDate: (Long) -> String
) {
    val context = LocalContext.current
    var editandoFichaPaciente by editandoFichaPacienteState
    var nombrePaciente by nombrePacienteState
    var apellidosPaciente by apellidosPacienteState
    var fechaNacimientoPaciente by fechaNacimientoPacienteState
    var edadPaciente by edadPacienteState
    var pesoPaciente by pesoPacienteState
    var pesoUnidadPaciente by pesoUnidadPacienteState
    var estaturaPaciente by estaturaPacienteState
    var estaturaUnidadPaciente by estaturaUnidadPacienteState
    var sexoPaciente by sexoPacienteState
    var paisPaciente by paisPacienteState
    var monedaPaciente by monedaPacienteState
    var enfermedadesPaciente by enfermedadesPacienteState
    var prescripcionesPaciente by prescripcionesPacienteState
    var fotoPerfilPaciente by fotoPerfilPacienteState
    var cameraPermissionPerfilPending by cameraPermissionPerfilPendingState
    var expandedPesoUnidad by expandedPesoUnidadState
    var expandedEstaturaUnidad by expandedEstaturaUnidadState
    var expandedPaisPaciente by expandedPaisPacienteState
    var pesoInput by pesoInputState
    var pesoUnidadKg by pesoUnidadKgState
    var mostrarDialogoPesoSincronizado by mostrarDialogoPesoSincronizadoState
    var perfilPendienteDeEliminar by perfilPendienteDeEliminarState
    var visorAdjuntos by visorAdjuntosState
    var exportandoTomas by exportandoTomasState
    var periodoExportacionPendiente by periodoExportacionPendienteState
    var reportePendienteDeEliminar by reportePendienteDeEliminarState
    var citaPendienteDeEliminar by citaPendienteDeEliminarState
    var filtroProfesionalInformesId by filtroProfesionalInformesIdState
    var expandedFiltroProfesionalInformes by expandedFiltroProfesionalInformesState
    var citaMedicaSeleccionadaIdMutable by citaMedicaSeleccionadaIdState
    var editingPatientId by editingPatientIdState

    val fechaNacimientoTexto by derivedStateOf {
        fechaNacimientoPaciente?.let { onFormatDate(it) }.orEmpty()
    }
    val opcionesEstaturaUnidad = listOf("cm", "in")
    val opcionesPesoUnidad = listOf("kg", "lb")

    if (!mostrarFichaPaciente && !mostrarPanelInformes && !mostrarFormularioInforme) return

    // Form fields go below
    if (mostrarFichaPaciente) {
            val perfilCardShape = RoundedCornerShape(24.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = perfilCardShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFF3FBFF)
                )
            ) {
                Column(
                    modifier = Modifier
                        .clip(perfilCardShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC163F7A),
                                    Color(0xCC0D4FA8),
                                    Color(0xB80A2D63)
                                )
                            )
                        )
                        .padding(16.dp)
                        .verticalScroll(panelInternoScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingPatientId == null) "Nuevo perfil" else "Perfil",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (editingPatientId != null && !editandoFichaPaciente) {
                                IconButton(onClick = { editandoFichaPaciente = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = Color.White)
                                }
                            }
                            if (editandoFichaPaciente) {
                                IconButton(
                                    onClick = {
                                        if (nombrePaciente.isBlank() || apellidosPaciente.isBlank()) {
                                            Toast.makeText(context, "Completa nombre y apellidos del paciente", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }

                                        // Para perfiles nuevos, usar solo la fecha seleccionada por el usuario
                                        // No usar persisted birthday que podría traer datos de otro perfil
                                        val fechaNacimientoGuardada = fechaNacimientoPaciente ?: 0L
                                        val edadGuardada = edadPaciente.ifBlank { edadCalculadaPaciente }
                                        val monedaGuardada = CountryCurrencyCatalog.forCountry(paisPaciente).currencySymbol

                                        val profile = PatientProfile(
                                            id = editingPatientId ?: 0,
                                            nombre = nombrePaciente,
                                            apellidos = apellidosPaciente,
                                            fechaNacimiento = fechaNacimientoGuardada ?: 0L,
                                            edad = edadGuardada,
                                            peso = pesoPaciente,
                                            pesoUnidad = pesoUnidadPaciente,
                                            estatura = estaturaPaciente,
                                            estaturaUnidad = estaturaUnidadPaciente,
                                            sexo = sexoPaciente,
                                            pais = paisPaciente,
                                            moneda = monedaGuardada,
                                            enfermedades = enfermedadesPaciente,
                                            prescripciones = prescripcionesPaciente,
                                            isActive = true,
                                            fotoPerfil = fotoPerfilPaciente
                                        )

                                        coroutineScope.launch(Dispatchers.IO) {
                                            val creandoNuevoPerfil = editingPatientId == null
                                            database.patientProfileDao().desactivarTodos()
                                            val savedId = if (creandoNuevoPerfil) {
                                                database.patientProfileDao().guardar(profile).toInt()
                                            } else {
                                                database.patientProfileDao().actualizar(profile)
                                                profile.id
                                            }
                                            val finalPatientId = when {
                                                savedId > 0 -> savedId
                                                editingPatientId != null -> editingPatientId!!
                                                else -> 0
                                            }
                                            if (finalPatientId > 0) {
                                                database.patientProfileDao().activarPaciente(finalPatientId)
                                                // Guardar la fecha correcta en SharedPreferences para sincronización
                                                if (fechaNacimientoGuardada > 0L) {
                                                    onSavePersistedBirthday(context, finalPatientId, fechaNacimientoGuardada)
                                                }
                                            }
                                            val perfilPersistido = finalPatientId
                                                .takeIf { it > 0 }
                                                ?.let { database.patientProfileDao().buscarPorId(it) }
                                            withContext(Dispatchers.Main) {
                                                editingPatientId = finalPatientId.takeIf { it > 0 }
                                                val perfilBase = perfilPersistido ?: profile.copy(
                                                    id = finalPatientId,
                                                    edad = edadGuardada,
                                                    fechaNacimiento = fechaNacimientoGuardada ?: 0L,
                                                    pais = paisPaciente,
                                                    moneda = monedaGuardada,
                                                    isActive = true
                                                )
                                                val perfilParaMostrar = perfilBase.copy(
                                                    fechaNacimiento = fechaNacimientoGuardada ?: perfilBase.fechaNacimiento,
                                                    edad = edadGuardada.ifBlank { perfilBase.edad }
                                                )
                                                if (finalPatientId > 0) {
                                                    onCargarFichaPaciente(perfilParaMostrar)
                                                } else {
                                                    fechaNacimientoPaciente = fechaNacimientoGuardada
                                                    edadPaciente = edadGuardada
                                                }
                                                editandoFichaPaciente = false
                                                // panel stays shown (mostrarFichaPaciente is controlled by caller)
                                                Toast.makeText(context, if (creandoNuevoPerfil) "Perfil guardado" else "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Guardar perfil", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { onVolverPantallaAnteriorDesdeFichaPaciente() }) {
                                Icon(Icons.Default.Close, contentDescription = "Volver", tint = Color.White)
                            }
                        }
                    }

                    val mostrarDatePickerNacimiento = {
                        if (editandoFichaPaciente) {
                            val calendarioNacimiento = Calendar.getInstance().apply {
                                timeInMillis = fechaNacimientoPaciente ?: System.currentTimeMillis()
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val fechaSeleccionada = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    fechaNacimientoPaciente = fechaSeleccionada
                                    edadPaciente = onCalcularEdadDesdeNacimiento(fechaSeleccionada).toString()
                                    (editingPatientId ?: pacienteActivo?.id)?.takeIf { it > 0 }?.let { patientId ->
                                        onSavePersistedBirthday(context, patientId, fechaSeleccionada)
                                    }
                                },
                                calendarioNacimiento.get(Calendar.YEAR),
                                calendarioNacimiento.get(Calendar.MONTH),
                                calendarioNacimiento.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val fotoBitmap: Bitmap? = remember(fotoPerfilPaciente) {
                            fotoPerfilPaciente?.let { ruta ->
                                try { BitmapFactory.decodeFile(ruta) } catch (_: Exception) { null }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoBitmap != null) {
                                Image(
                                    bitmap = fotoBitmap.asImageBitmap(),
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Sin foto",
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xAAFFFFFF)
                                )
                            }
                        }
                        if (editandoFichaPaciente) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { pickFotoPerfilLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Galería", fontSize = 13.sp)
                                }
                                OutlinedButton(onClick = {
                                    if (cameraPermissionGranted(context)) {
                                        takeFotoPerfilLauncher.launch(null)
                                    } else {
                                        cameraPermissionPerfilPending = true
                                        cameraPermissionPerfilLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cámara", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nombrePaciente,
                        onValueChange = { if (editandoFichaPaciente) nombrePaciente = it },
                        readOnly = !editandoFichaPaciente,
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = apellidosPaciente,
                        onValueChange = { if (editandoFichaPaciente) apellidosPaciente = it },
                        readOnly = !editandoFichaPaciente,
                        label = { Text("Apellidos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fechaNacimientoTexto,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de nacimiento") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = editandoFichaPaciente
                            ) {
                                mostrarDatePickerNacimiento()
                            },
                        trailingIcon = if (editandoFichaPaciente) {
                            {
                                IconButton(onClick = { mostrarDatePickerNacimiento() }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar fecha de nacimiento"
                                    )
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = edadPaciente,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Edad") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val fechaNacimientoPreview = resolvePersistedBirthday(
                                context = context,
                                patientId = editingPatientId ?: pacienteActivo?.id,
                                inMemoryBirthday = fechaNacimientoPaciente
                            )
                            if (fechaNacimientoPreview == null || fechaNacimientoPreview <= 0L) {
                                Toast.makeText(context, "Primero indica la fecha de nacimiento", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onRequestBirthdayPreview(
                                BirthdayCelebrationRequest(
                                    patientName = listOf(nombrePaciente, apellidosPaciente)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ")
                                        .ifBlank { "Perfil" },
                                    age = edadPaciente.ifBlank { edadCalculadaPaciente }.toIntOrNull()
                                        ?: onCalcularEdadDesdeNacimiento(fechaNacimientoPreview),
                                    seed = (editingPatientId ?: 1).coerceAtLeast(1)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Probar felicitacion")
                    }
                    OutlinedTextField(
                        value = pesoPaciente,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Peso") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarDialogoPesoSincronizado = true },
                        singleLine = true,
                        enabled = false
                    )
                    OutlinedTextField(
                        value = pesoUnidadPaciente,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad de peso") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarDialogoPesoSincronizado = true },
                        singleLine = true,
                        enabled = false
                    )
                    if (mostrarDialogoPesoSincronizado) {
                        AlertDialog(
                            onDismissRequest = { mostrarDialogoPesoSincronizado = false },
                            title = { Text("Campo sincronizado") },
                            text = { Text("El dato de este campo se sincroniza desde Seguimiento diario.") },
                            confirmButton = {
                                IconButton(onClick = { mostrarDialogoPesoSincronizado = false }) {
                                    Icon(Icons.Filled.Save, contentDescription = "Aceptar")
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = estaturaPaciente,
                        onValueChange = {
                            if (editandoFichaPaciente && (it.isEmpty() || it.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$")))) estaturaPaciente = it
                        },
                        readOnly = !editandoFichaPaciente,
                        label = { Text("Estatura") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (editandoFichaPaciente) {
                        VademecumDropdown(
                            label = "Unidad de estatura",
                            options = opcionesEstaturaUnidad,
                            selectedValue = estaturaUnidadPaciente,
                            expanded = expandedEstaturaUnidad,
                            onExpandedChange = { expandedEstaturaUnidad = !expandedEstaturaUnidad },
                            onDismiss = { expandedEstaturaUnidad = false },
                            onSelect = {
                                estaturaUnidadPaciente = it
                                expandedEstaturaUnidad = false
                            }
                        )
                    } else {
                        OutlinedTextField(
                            value = estaturaUnidadPaciente,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad de estatura") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    val imcPerfil = calcularIMC(
                        pesoStr = pesoPaciente,
                        pesoUnidad = pesoUnidadPaciente,
                        estaturaStr = estaturaPaciente,
                        estaturaUnidad = estaturaUnidadPaciente
                    )
                    if (imcPerfil != null) {
                        OutlinedTextField(
                            value = "${"%.1f".format(imcPerfil)} — ${etiquetaIMC(imcPerfil)}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("IMC (Índice de masa corporal)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = false
                        )
                    }

                    Text(
                        text = "Sexo",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (editandoFichaPaciente) {
                                    sexoPaciente = if (sexoPaciente == "Mujer") "" else "Mujer"
                                }
                            }
                        ) {
                            Checkbox(
                                checked = sexoPaciente == "Mujer",
                                onCheckedChange = {
                                    if (editandoFichaPaciente) {
                                        sexoPaciente = if (it) "Mujer" else ""
                                    }
                                },
                                enabled = editandoFichaPaciente
                            )
                            Text(
                                text = "Mujer",
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (editandoFichaPaciente) {
                                    sexoPaciente = if (sexoPaciente == "Hombre") "" else "Hombre"
                                }
                            }
                        ) {
                            Checkbox(
                                checked = sexoPaciente == "Hombre",
                                onCheckedChange = {
                                    if (editandoFichaPaciente) {
                                        sexoPaciente = if (it) "Hombre" else ""
                                    }
                                },
                                enabled = editandoFichaPaciente
                            )
                            Text(
                                text = "Hombre",
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (editandoFichaPaciente) {
                        VademecumDropdown(
                            label = "País",
                            options = CountryCurrencyCatalog.spanishSpeakingCountries.map { it.country },
                            selectedValue = paisPaciente,
                            expanded = expandedPaisPaciente,
                            onExpandedChange = { expandedPaisPaciente = !expandedPaisPaciente },
                            onDismiss = { expandedPaisPaciente = false },
                            onSelect = { pais ->
                                val currency = CountryCurrencyCatalog.forCountry(pais)
                                paisPaciente = currency.country
                                monedaPaciente = currency.currencySymbol
                                expandedPaisPaciente = false
                            }
                        )
                    } else {
                        OutlinedTextField(
                            value = paisPaciente,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("País") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = monedaPaciente,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moneda") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false
                    )

                    OutlinedTextField(
                        value = enfermedadesPaciente,
                        onValueChange = { if (editandoFichaPaciente) enfermedadesPaciente = it },
                        readOnly = !editandoFichaPaciente,
                        label = { Text("Notas adicionales u Objetivos personales") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = prescripcionesPaciente,
                        onValueChange = { if (editandoFichaPaciente) prescripcionesPaciente = it },
                        readOnly = !editandoFichaPaciente,
                        label = { Text("Notas del usuario o Apuntes generales") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
                }

        if (mostrarPanelCitasMedicas && !mostrarFormularioCitaMedica) {
            MetallicMedicationCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16,
                verticalSpacing = 8
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Citas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    if (pacienteActivo == null) {
                        Text("Selecciona un perfil para gestionar tus citas.")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onAbrirFormularioCitaMedica(null) }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Nueva cita")
                            }
                            IconButton(
                                onClick = {
                                    val cita = citaMedicaSeleccionada
                                    if (cita == null) {
                                        Toast.makeText(context, "Selecciona una cita primero", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onAbrirFormularioCitaMedica(cita)
                                    }
                                },
                                enabled = citaMedicaSeleccionada != null
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    val cita = citaMedicaSeleccionada
                                    if (cita == null) {
                                        Toast.makeText(context, "Selecciona una cita primero", Toast.LENGTH_SHORT).show()
                                    } else {
                                        citaPendienteDeEliminar = cita
                                    }
                                },
                                enabled = citaMedicaSeleccionada != null
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                            }
                        }
                        val citaSelecParaRealizar = citaMedicaSeleccionada
                        if (citaSelecParaRealizar != null && !citaSelecParaRealizar.isCompleted) {
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        MedicalAppointmentScheduler(context).cancelar(citaSelecParaRealizar.id)
                                        database.medicalAppointmentDao().actualizar(citaSelecParaRealizar.copy(isCompleted = true))
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Cita marcada como realizada", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B5E20)
                                )
                            ) {
                                Text("Marcar como realizada")
                            }
                        }

                        if (citasMedicas.isEmpty()) {
                            Text("Todavia no hay citas guardadas para este perfil.")
                        } else {
                            citasMedicas.forEach { cita ->
                                val estaSeleccionada = cita.id == citaMedicaSeleccionadaId
                                val containerColor = when {
                                    cita.isCompleted -> Color(0xFF1B4D2B)
                                    estaSeleccionada -> Color(0xCC1E61B5)
                                    else -> Color(0xB4142D59)
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { citaMedicaSeleccionadaIdMutable = cita.id },
                                    colors = CardDefaults.cardColors(
                                        containerColor = containerColor,
                                        contentColor = Color(0xFFF3FBFF)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(cita.doctorName.ifBlank { "Visita" }, fontWeight = FontWeight.SemiBold)
                                            if (cita.isCompleted) {
                                                Text(
                                                    "\u2713 Realizada",
                                                    color = Color(0xFF80E27E),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(formatDateTimeMain(cita.scheduledAt))
                                        if (cita.notes.isNotBlank()) {
                                            Text(cita.notes.take(140))
                                        }
                                        if (!cita.isCompleted) {
                                            Text(
                                                if (cita.alarmEnabled) {
                                                    "Alarma critica: activa | Sonido: ${alarmaSonidoNombre} | Antelacion: ${onFormatReminderMinutesLabel(cita.reminderMinutes)}"
                                                } else {
                                                    "Alarma critica: desactivada"
                                                }
                                            )
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
    } // end if (mostrarFichaPaciente)

        if (mostrarPanelInformes && !mostrarFormularioInforme) {
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
                    "Documentos",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (pacienteActivo == null) {
                    Text("Selecciona un usuario para ver sus documentos.")
                } else if (reportesSalud.isEmpty()) {
                    Text("Este usuario aun no tiene documentos registrados.")
                } else {
                    // Filtro por contacto
                    val profesionalesConInformes = profesionalesHabituales.filter { m ->
                        reportesSalud.any { r ->
                            r.practitionerId == m.id ||
                            (r.practitionerId == null && m.name.isNotBlank() && (
                                r.titulo.trim().equals(m.name.trim(), ignoreCase = true) ||
                                r.titulo.trim().contains(m.name.trim(), ignoreCase = true) ||
                                m.name.trim().contains(r.titulo.trim(), ignoreCase = true)
                            ))
                        }
                    }
                    if (profesionalesConInformes.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedFiltroProfesionalInformes,
                            onExpandedChange = { expandedFiltroProfesionalInformes = !expandedFiltroProfesionalInformes }
                        ) {
                            val nombreFiltro = profesionalesHabituales.find { it.id == filtroProfesionalInformesId }
                                ?.let { "${it.name} — ${it.specialty}" }
                                ?: "Todos los médicos y especialistas"
                            OutlinedTextField(
                                value = nombreFiltro,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filtrar por médico/especialista") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFiltroProfesionalInformes) }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedFiltroProfesionalInformes,
                                onDismissRequest = { expandedFiltroProfesionalInformes = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos los médicos y especialistas") },
                                    onClick = { filtroProfesionalInformesId = null; expandedFiltroProfesionalInformes = false }
                                )
                                profesionalesConInformes.forEach { profesional ->
                                    DropdownMenuItem(
                                        text = { Text("${profesional.name} — ${profesional.specialty}") },
                                        onClick = { filtroProfesionalInformesId = profesional.id; expandedFiltroProfesionalInformes = false }
                                    )
                                }
                            }
                        }
                    }
                    val reportesFiltrados = if (filtroProfesionalInformesId != null) {
                        val filtroNombre = profesionalesHabituales.find { it.id == filtroProfesionalInformesId }?.name?.trim() ?: ""
                        reportesSalud.filter { r ->
                            r.practitionerId == filtroProfesionalInformesId ||
                            (r.practitionerId == null && filtroNombre.isNotBlank() && (
                                r.titulo.trim().equals(filtroNombre, ignoreCase = true) ||
                                r.titulo.trim().contains(filtroNombre, ignoreCase = true) ||
                                filtroNombre.contains(r.titulo.trim(), ignoreCase = true)
                            ))
                        }
                    } else {
                        reportesSalud
                    }
                    if (reportesFiltrados.isEmpty()) {
                        Text("No hay informes para el profesional seleccionado.")
                    } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reportesFiltrados, key = { it.id }) { reporte ->
                            MetallicMedicationCard(
                                modifier = Modifier.width(300.dp),
                                contentPadding = 12,
                                verticalSpacing = 6
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.LocalHospital, contentDescription = null)
                                        Text(reporte.titulo, fontWeight = FontWeight.SemiBold)
                                    }
                                    val medicoReporte = profesionalesHabituales.find { it.id == reporte.practitionerId }
                                    if (medicoReporte != null) {
                                        Text(medicoReporte.name, fontSize = 12.sp, color = Color(0xFFB0D4FF))
                                    }
                                    Text(reporte.descripcion)
                                    Text("Adjuntos: ${decodeAttachmentPaths(reporte.adjuntos).size}")
                                    val adjuntosReporte = decodeAttachmentPaths(reporte.adjuntos)
                                    if (adjuntosReporte.isNotEmpty()) {
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(adjuntosReporte.size, key = { index -> adjuntosReporte[index] }) { index ->
                                                val path = adjuntosReporte[index]
                                                AttachmentThumbnail(
                                                    path = path,
                                                    onClick = {
                                                        visorAdjuntos = AttachmentViewerState(
                                                            paths = adjuntosReporte,
                                                            currentIndex = index
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onCargarInformeMedico(reporte) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                                        ) {
                                            Text("Editar", color = Color.Black)
                                        }
                                        Button(
                                            onClick = { reportePendienteDeEliminar = reporte },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                                        ) {
                                            Text("Eliminar", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } // end if reportesFiltrados not empty
                }
                if (pacienteActivo != null) {
                    TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Exportar registros del usuario")
                            Text("Periodo basado en la fecha seleccionada: $fechaActualTexto")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IntakeExportPeriod.entries.forEach { periodo ->
                                    Button(
                                        onClick = {
                                            periodoExportacionPendiente = periodo
                                            exportandoTomas = true
                                            exportMedicationReportLauncher.launch(
                                                "tomas-${periodo.fileSuffix}-${timestampArchivo()}.rtf"
                                            )
                                        },
                                        enabled = !exportandoTomas,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                                    ) {
                                        Text(periodo.label, fontSize = 12.sp, maxLines = 1, color = Color.Black)
                                    }
                                }
                            }
                            if (exportandoTomas) {
                                Text("Generando informe exportable...")
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (pacienteActivo == null) {
                            onAbrirNuevaFichaPaciente()
                            Toast.makeText(context, "Primero selecciona o crea un paciente", Toast.LENGTH_SHORT).show()
                        } else {
                            onResetInformeMedico()
                            onMostrarPanelInformesChange(false)
                            onMostrarFormularioInformeChange(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                ) {
                    Text("Nuevo informe", color = Color.Black)
                }
                Button(
                    onClick = { onCerrarPanelesSecundarios() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                ) {
                    Text("Volver al escritorio", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    reportePendienteDeEliminar?.let { reporte ->
        AlertDialog(
            onDismissRequest = { reportePendienteDeEliminar = null },
            title = { Text("Eliminar documento") },
            text = { Text("¿Seguro que quieres eliminar \"${reporte.titulo}\"? Esta acción no se puede deshacer.") },
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
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                ) { Text("Eliminar", color = Color.Black) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { reportePendienteDeEliminar = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) { Text("Cancelar", color = Color.Black) }
            }
        )
    }

    citaPendienteDeEliminar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaPendienteDeEliminar = null },
            title = { Text("Eliminar cita") },
            text = { Text("¿Seguro que quieres eliminar la cita \"${cita.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        citaPendienteDeEliminar = null
                        citaMedicaSeleccionadaIdMutable = null
                        coroutineScope.launch(Dispatchers.IO) {
                            try { MedicalAppointmentScheduler(context).cancelar(cita.id) } catch (_: Exception) {}
                            database.medicalAppointmentDao().eliminar(cita)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Cita eliminada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                ) { Text("Eliminar", color = Color.Black) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { citaPendienteDeEliminar = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) { Text("Cancelar", color = Color.Black) }
            }
        )
    }
}
