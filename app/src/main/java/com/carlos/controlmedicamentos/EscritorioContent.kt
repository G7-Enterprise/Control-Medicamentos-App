package com.carlos.controlmedicamentos

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.ControlEmbarazo
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.VaccinationRecord
import com.carlos.controlmedicamentos.data.local.VisitaDentista
import com.carlos.controlmedicamentos.data.local.unidadesPorToma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ProximaCitaInfo(
    val titulo: String,
    val fechaHora: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EscritorioContent(
    modifier: Modifier = Modifier,
    pacienteActivo: PatientProfile?,
    insumosGuardados: List<Medication>,
    perfilesPacientes: List<PatientProfile>,
    pagerEscritorioState: PagerState,
    paginaBaseEscritorio: Int,
    fechaBaseEscritorio: Long,
    fechaResumenEscritorioTexto: String,
    escritorioEsHoy: Boolean,
    tomaPendienteDeEliminarState: MutableState<IntakeRemovalConfirmation?>,
    mostrarDialogoMediaState: MutableState<Boolean>,
    formularioInformeAutoAbiertoState: MutableState<Boolean>,
    database: AppDatabase,
    mostrarMenuHamburguesaState: MutableState<Boolean>,
    embarazoActivo: com.carlos.controlmedicamentos.data.local.ControlEmbarazo?,
    ninosDelPaciente: List<com.carlos.controlmedicamentos.data.local.NinoEntity>,
    alarmaSonidoUriState: MutableState<String>,
    alarmaSonidoNombreState: MutableState<String>,
    fallAlertPanelState: MutableState<Boolean>,
    intervaloReintentoSeleccionadoState: MutableState<Int>,
    numeroIntentosCriticosSeleccionadoState: MutableState<Int>,
    tienePermisoCamara: Boolean,
    cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    pickStudyImagesLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onAbrirNuevaFichaPaciente: () -> Unit,
    onAbrirFichaPaciente: (PatientProfile, Boolean) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit,
    onResetForm: () -> Unit,
    onMostrarFormulario: (Boolean) -> Unit,
    onMostrarPanelPacientes: (Boolean) -> Unit,
    onMostrarPanelProfesionales: (Boolean) -> Unit,
    onMostrarPanelInformes: (Boolean) -> Unit,
    onMostrarListaInsumos: (Boolean) -> Unit,
    onMostrarDialogoMedia: (Boolean) -> Unit,
    onMostrarPanelSignosVitales: () -> Unit,
    onMostrarPanelConfiguracionAlertas: (Boolean) -> Unit,
    onMostrarPanelAsistenteIa: (Boolean) -> Unit,
    onMostrarPanelPodometro: (Boolean) -> Unit,
    onMostrarPanelPedidos: (Boolean) -> Unit,
    onMostrarPanelBackups: (Boolean) -> Unit,
    onMostrarPanelHidratacion: (Boolean) -> Unit,
    onMostrarPanelCicloMenstrual: (Boolean) -> Unit,
    onMostrarPanelAnticonceptivos: (Boolean) -> Unit,
    onMostrarPanelEmbarazo: (Boolean) -> Unit,
    onMostrarPanelPediatrico: (Boolean) -> Unit,
    onMostrarPanelReporteClinico: (Boolean) -> Unit,
    onMostrarPanelEstadisticas: (Boolean) -> Unit,
    onMostrarPanelVerificadorTomas: (Boolean) -> Unit,
    onMostrarPanelDiario: (Boolean) -> Unit,
    onMostrarPanelSedentarismo: (Boolean) -> Unit,
    onMostrarPanelDentista: (Boolean) -> Unit,
    onResolveAlarmSoundLabel: (android.content.Context, String) -> String,
    onInsumoARecargar: (Medication) -> Unit,
    onInsumoAPedir: (Medication) -> Unit,
    onInsumoSeleccionado: (Int) -> Unit,
    onMostrarListaInsumosView: (Boolean) -> Unit,
    onCameraPermissionPendingChange: (Boolean) -> Unit,
    onMostrarPanelInformesChange: (Boolean) -> Unit,
    onMostrarFormularioInformeChange: (Boolean) -> Unit,
    onFormatHour: (Long) -> String,
    onMoverFecha: (Long, Int) -> Long
) {
    val coroutineScope = rememberCoroutineScope()
    var tomaPendienteDeEliminar by tomaPendienteDeEliminarState
    var mostrarDialogoMedia by mostrarDialogoMediaState
    var formularioInformeAutoAbierto by formularioInformeAutoAbiertoState

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetallicGreenHeaderCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pacienteActivo != null) {
                        MetallicProfileCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onAbrirFichaPaciente(pacienteActivo, false) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val fotoFile = pacienteActivo.fotoPerfil?.let { File(it) }
                                if (fotoFile != null && fotoFile.exists()) {
                                    AsyncImage(
                                        model = fotoFile,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(48.dp).clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF29B7FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pacienteActivo.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${pacienteActivo.nombre} ${pacienteActivo.apellidos}".trim(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFF3FBFF)
                                    )
                                    Text(
                                        text = "Edad: ${pacienteActivo.edad}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFAEEBFF)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = { mostrarMenuHamburguesaState.value = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu principal"
                            )
                        }
                        MenuHamburguesaEscritorio(
                            mostrarMenuHamburguesaState = mostrarMenuHamburguesaState,
                            pacienteActivo = pacienteActivo,
                            embarazoActivo = embarazoActivo,
                            ninosDelPaciente = ninosDelPaciente,
                            alarmaSonidoUriState = alarmaSonidoUriState,
                            alarmaSonidoNombreState = alarmaSonidoNombreState,
                            fallAlertPanelState = fallAlertPanelState,
                            intervaloReintentoSeleccionadoState = intervaloReintentoSeleccionadoState,
                            numeroIntentosCriticosSeleccionadoState = numeroIntentosCriticosSeleccionadoState,
                            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
                            onAbrirNuevaFichaPaciente = onAbrirNuevaFichaPaciente,
                            onResetForm = onResetForm,
                            onMostrarFormulario = onMostrarFormulario,
                            onMostrarPanelPacientes = onMostrarPanelPacientes,
                            onMostrarPanelProfesionales = onMostrarPanelProfesionales,
                            onMostrarPanelInformes = onMostrarPanelInformes,
                            onMostrarListaInsumos = onMostrarListaInsumos,
                            onMostrarDialogoMedia = onMostrarDialogoMedia,
                            onMostrarPanelSignosVitales = { onMostrarPanelSignosVitales() },
                            onMostrarPanelConfiguracionAlertas = onMostrarPanelConfiguracionAlertas,
                            onMostrarPanelAsistenteIa = onMostrarPanelAsistenteIa,
                            onMostrarPanelPodometro = onMostrarPanelPodometro,
                            onMostrarPanelPedidos = onMostrarPanelPedidos,
                            onMostrarPanelBackups = onMostrarPanelBackups,
                            onMostrarPanelHidratacion = onMostrarPanelHidratacion,
                            onMostrarPanelCicloMenstrual = onMostrarPanelCicloMenstrual,
                            onMostrarPanelAnticonceptivos = onMostrarPanelAnticonceptivos,
                            onMostrarPanelEmbarazo = onMostrarPanelEmbarazo,
                            onMostrarPanelPediatrico = onMostrarPanelPediatrico,
                            onMostrarPanelReporteClinico = onMostrarPanelReporteClinico,
                            onMostrarPanelEstadisticas = onMostrarPanelEstadisticas,
                            onMostrarPanelVerificadorTomas = onMostrarPanelVerificadorTomas,
                            onMostrarPanelDiario = onMostrarPanelDiario,
                            onMostrarPanelSedentarismo = onMostrarPanelSedentarismo,
                            onMostrarPanelDentista = onMostrarPanelDentista,
                            onResolveAlarmSoundLabel = onResolveAlarmSoundLabel
                        )
                    }
                }
            }
            DashboardWeekSelector(
                selectedDate = fechaBaseEscritorio,
                onSelectDate = { fechaObjetivo ->
                    coroutineScope.launch {
                        pagerEscritorioState.animateScrollToPage(
                            paginaBaseEscritorio + calcularDiferenciaDias(fechaBaseEscritorio, fechaObjetivo)
                        )
                    }
                },
                onPreviousDay = { coroutineScope.launch { pagerEscritorioState.animateScrollToPage(pagerEscritorioState.currentPage - 1) } },
                onNextDay = { coroutineScope.launch { pagerEscritorioState.animateScrollToPage(pagerEscritorioState.currentPage + 1) } }
            )
            AnimatedDashboardDateSummary(
                text = fechaResumenEscritorioTexto,
                isToday = escritorioEsHoy
            )
            val proximaCitaState = remember { mutableStateOf<ProximaCitaInfo?>(null) }
            LaunchedEffect(pacienteActivo?.id) {
                val pid = pacienteActivo?.id
                if (pid != null) {
                    proximaCitaState.value = withContext(Dispatchers.IO) {
                        val ahora = System.currentTimeMillis()
                        val citaMedica = database.medicalAppointmentDao().obtenerProximaNoCompletada(pid, ahora)
                        val citaDental = database.visitaDentistaDao().proximaCitaPendiente(pid, ahora)
                        val dosisVacuna = database.vaccinationRecordDao().obtenerProximaDosis(pid, ahora)
                        val embarazo = database.controlEmbarazoDao().obtenerEmbarazoActivo(pid)
                        listOfNotNull(
                            citaMedica?.let { ProximaCitaInfo(it.title, it.scheduledAt) },
                            citaDental?.let { ProximaCitaInfo(it.motivo, it.fechaHora) },
                            dosisVacuna?.let { ProximaCitaInfo("Vacuna: ${it.vaccineName} (${it.doseLabel})", it.nextDoseAt!!) },
                            embarazo?.let { ProximaCitaInfo("Fecha probable de parto", it.fechaProbableParto) }
                        ).minByOrNull { it.fechaHora }
                    }
                } else {
                    proximaCitaState.value = null
                }
            }
            proximaCitaState.value?.let { cita ->
                val fechaCita = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-ES"))
                    .format(Date(cita.fechaHora))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEB3B),
                        contentColor = Color.Black
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📅",
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Próx. cita: ${cita.titulo} - $fechaCita",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 60.dp)
                        )
                    }
                }
            }
            if (perfilesPacientes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Bienvenido", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Todavia no hay ningun perfil creado. Crea el primero para empezar a registrar tus medicamentos, citas, notas y mucho mas.", color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Button(onClick = onAbrirNuevaFichaPaciente, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4CAF50)), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("Crear primer perfil", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else if (insumosGuardados.isEmpty()) {
                Text("Aún no hay medicamentos en la base de datos.", color = Color.White)
            }
        }

        HorizontalPager(
            state = pagerEscritorioState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            val fechaPagina = onMoverFecha(fechaBaseEscritorio, page - paginaBaseEscritorio)
            DashboardMedicationPage(
                pageDate = fechaPagina,
                medications = insumosGuardados.filter { it.estaActivo },
                patientId = pacienteActivo?.id ?: 0,
                database = database,
                onRequestRemoveIntake = { tomaPendienteDeEliminar = it },
                onRecargarStock = { med -> onInsumoARecargar(med) },
                onAnadirAPedido = { med -> onInsumoAPedir(med) },
                onViewStock = { med -> onInsumoSeleccionado(med.id); onMostrarListaInsumosView(true) }
            )
        }

        tomaPendienteDeEliminar?.let { intake ->
            AlertDialog(
                onDismissRequest = { tomaPendienteDeEliminar = null },
                title = { Text("Eliminar registro de toma") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("La modificacion del registro real es irreversible. Desea continuar?")
                        Text("Medicamento: ${intake.medicationName}")
                        Text("Hora programada: ${onFormatHour(intake.scheduledAt)}")
                        Text("Hora real de toma: ${onFormatHour(intake.acceptedAt)}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pendingIntake = intake
                            tomaPendienteDeEliminar = null
                            coroutineScope.launch(Dispatchers.IO) {
                                database.medicationIntakeDao().eliminarPorMedicamentoYHorario(pendingIntake.medicationId, pendingIntake.scheduledAt)
                                val med = database.medicationDao().findById(pendingIntake.medicationId)
                                if (med?.stockActual != null) {
                                    val unidadesToma = med.unidadesPorToma()
                                    database.medicationDao().actualizarStock(med.id, med.stockActual + unidadesToma)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("✓") }
                },
                dismissButton = {
                    Button(onClick = { tomaPendienteDeEliminar = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("X") }
                }
            )
        }

        if (mostrarDialogoMedia) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoMedia = false },
                title = { Text("Adjuntar media") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                mostrarDialogoMedia = false
                                onCerrarPanelesSecundarios()
                                onMostrarPanelInformesChange(false)
                                formularioInformeAutoAbierto = true
                                onMostrarFormularioInformeChange(true)
                                if (tienePermisoCamara) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    onCameraPermissionPendingChange(true)
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escanear documento")
                        }
                        Button(
                            onClick = {
                                mostrarDialogoMedia = false
                                onCerrarPanelesSecundarios()
                                onMostrarPanelInformesChange(false)
                                formularioInformeAutoAbierto = true
                                onMostrarFormularioInformeChange(true)
                                pickStudyImagesLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galeria o captura")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    IconButton(onClick = { mostrarDialogoMedia = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                    }
                }
            )
        }
    }
}
