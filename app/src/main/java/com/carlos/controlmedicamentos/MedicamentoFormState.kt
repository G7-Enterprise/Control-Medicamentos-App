package com.carlos.controlmedicamentos

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.carlos.controlmedicamentos.backup.BackupSelection
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings

@Stable
internal class MedicamentoFormState(
    val nombreState: MutableState<String>,
    val cantidadState: MutableState<String>,
    val alarmaActivaState: MutableState<Boolean>,
    val esCicloCortoState: MutableState<Boolean>,
    val estaActivoState: MutableState<Boolean>,
    val editingMedicationIdState: MutableState<Int?>,
    val controlarExistenciasState: MutableState<Boolean>,
    val stockActualState: MutableState<String>,
    val stockMinimoState: MutableState<String>,
    val precioPorUnidadState: MutableState<String>,
    val telefonoPedidoWhatsappState: MutableState<String>,
    val dispensacionGratuitaState: MutableState<Boolean>,
    val origenReposicionState: MutableState<String>,
    val expandedOrigenReposicionState: MutableState<Boolean>,
    val selectedMedicationState: MutableState<VademecumMedication?>,
    val fechaInicioState: MutableState<Long>,
    val fechaFinState: MutableState<Long>,
    val horaTomaSeleccionadaState: MutableState<String>,
    val medicationToDeleteState: MutableState<Medication?>,
    val insumoARecargarState: MutableState<Medication?>,
    val inputRecargarStockState: MutableState<String>,
    val insumoAPedirState: MutableState<Medication?>,
    val inputUnidadesPedidoState: MutableState<String>,
    val itemCarritoAConfirmarState: MutableState<CarritoItem?>,
    val itemCarritoAEliminarState: MutableState<CarritoItem?>,
    val confirmarRecepcionTotalState: MutableState<Boolean>,
    val pedidoAEliminarState: MutableState<MedicationOrder?>,
    val pedidoAEditarState: MutableState<MedicationOrder?>,
    val inputEditarResumenState: MutableState<String>,
    val inputEditarTotalState: MutableState<String>,
    val inputUnidadesRecibidasState: MutableState<String>,
    val inputPrecioActualizadoState: MutableState<String>,
    val duplicateMedicationState: MutableState<Medication?>,
    val mostrarFormularioState: MutableState<Boolean>,
    val mostrarFichaPacienteState: MutableState<Boolean>,
    val mostrarMenuHamburguesaState: MutableState<Boolean>,
    val mostrarFormularioInformeState: MutableState<Boolean>,
    val formularioInformeAutoAbiertoState: MutableState<Boolean>,
    val mostrarFormularioProfesionalState: MutableState<Boolean>,
    val mostrarPanelPacientesState: MutableState<Boolean>,
    val mostrarPanelProfesionalesState: MutableState<Boolean>,
    val mostrarPanelInformesState: MutableState<Boolean>,
    val mostrarListaInsumosState: MutableState<Boolean>,
    val mostrarPanelBackupsState: MutableState<Boolean>,
    val mostrarPanelPedidosState: MutableState<Boolean>,
    val mostrarPanelPodometroState: MutableState<Boolean>,
    val mostrarPanelConfiguracionAlertasState: MutableState<Boolean>,
    val mostrarPanelSignosVitalesState: MutableState<Boolean>,
    val mostrarPanelConfiguracionIaState: MutableState<Boolean>,
    val mostrarPanelAsistenteIaState: MutableState<Boolean>,
    val mostrarPanelCicloMenstrualState: MutableState<Boolean>,
    val mostrarPanelEmbarazoState: MutableState<Boolean>,
    val mostrarPanelAnticonceptivosState: MutableState<Boolean>,
    val mostrarPanelPediatricoState: MutableState<Boolean>,
    val mostrarPanelReporteClinicoState: MutableState<Boolean>,
    val mostrarPanelEstadisticasState: MutableState<Boolean>,
    val mostrarPanelDiarioState: MutableState<Boolean>,
    val mostrarPanelVerificadorTomasState: MutableState<Boolean>,
    val mostrarPanelHidratacionState: MutableState<Boolean>,
    val mostrarPanelSedentarismoState: MutableState<Boolean>,
    val mostrarPanelDentistaState: MutableState<Boolean>,
    val insumoSeleccionadoEnInventarioState: MutableState<Int?>,
    val editingPatientIdState: MutableState<Int?>,
    val editingReportIdState: MutableState<Int?>,
    val practitionerIdInformeState: MutableState<Int?>,
    val editingPractitionerIdState: MutableState<Int?>,
    val profesionalSeleccionadoIdState: MutableState<Int?>,
    val citaMedicaSeleccionadaIdState: MutableState<Int?>,
    val editandoFichaPacienteState: MutableState<Boolean>,
    val editingAppointmentIdState: MutableState<Int?>,
    val nombrePacienteState: MutableState<String>,
    val apellidosPacienteState: MutableState<String>,
    val fechaNacimientoPacienteState: MutableState<Long?>,
    val edadPacienteState: MutableState<String>,
    val pesoPacienteState: MutableState<String>,
    val pesoUnidadPacienteState: MutableState<String>,
    val estaturaPacienteState: MutableState<String>,
    val estaturaUnidadPacienteState: MutableState<String>,
    val sexoPacienteState: MutableState<String>,
    val paisPacienteState: MutableState<String>,
    val monedaPacienteState: MutableState<String>,
    val enfermedadesPacienteState: MutableState<String>,
    val prescripcionesPacienteState: MutableState<String>,
    val fotoPerfilPacienteState: MutableState<String?>,
    val cameraPermissionPerfilPendingState: MutableState<Boolean>,
    val estudiosAdjuntos: SnapshotStateList<String>,
    val tituloInformeState: MutableState<String>,
    val descripcionInformeState: MutableState<String>,
    val nombreProfesionalState: MutableState<String>,
    val especialidadProfesionalState: MutableState<String>,
    val tituloCitaMedicaState: MutableState<String>,
    val profesionalCitaMedicaState: MutableState<String>,
    val lugarCitaMedicaState: MutableState<String>,
    val notasCitaMedicaState: MutableState<String>,
    val fechaCitaMedicaState: MutableState<Long>,
    val recordatorioCitaMinutosState: MutableState<Int>,
    val alarmaCitaMedicaActivaState: MutableState<Boolean>,
    val ejecutandoBackupManualState: MutableState<Boolean>,
    val restaurandoBackupState: MutableState<Boolean>,
    val backupSelectionState: MutableState<BackupSelection>,
    val restoreSelectionState: MutableState<BackupSelection>,
    val backupPatientIdState: MutableState<Int?>,
    val restorePatientIdState: MutableState<Int?>,
    val backupPatientDropdownExpandedState: MutableState<Boolean>,
    val restorePatientDropdownExpandedState: MutableState<Boolean>,
    val cameraPermissionPendingState: MutableState<Boolean>,
    val mostrarDialogoBackupManualState: MutableState<Boolean>,
    val mostrarDialogoRestoreSeleccionState: MutableState<Boolean>,
    val mostrarDialogoProgramarBackupState: MutableState<Boolean>,
    val mostrarDialogoCerrarInformeSinGuardarState: MutableState<Boolean>,
    val mensajeBackupState: MutableState<String>,
    val visorAdjuntosState: MutableState<AttachmentViewerState?>,
    val adjuntosPendientesReemplazo: SnapshotStateList<PendingAttachmentReplacement>,
    val borradorInformeInicialState: MutableState<ReportDraftSnapshot>,
    val sistolicaInputState: MutableState<String>,
    val diastolicaInputState: MutableState<String>,
    val comentarioPresionInputState: MutableState<String>,
    val latidosInputState: MutableState<String>,
    val comentarioLatidosInputState: MutableState<String>,
    val glucemiaInputState: MutableState<String>,
    val comentarioGlucemiaInputState: MutableState<String>,
    val temperaturaInputState: MutableState<String>,
    val comentarioTemperaturaInputState: MutableState<String>,
    val pesoInputState: MutableState<String>,
    val pesoUnidadKgState: MutableState<Boolean>,
    val tomaPendienteDeEliminarState: MutableState<IntakeRemovalConfirmation?>,
    val perfilPendienteDeEliminarState: MutableState<PatientProfile?>,
    val mostrarFormularioCitaMedicaState: MutableState<Boolean>,
    val mostrarPanelCitasMedicasState: MutableState<Boolean>,
    val citaPendienteDeEliminarState: MutableState<MedicalAppointment?>,
    val reportePendienteDeEliminarState: MutableState<MedicalReport?>,
    val mostrarDialogoPesoSincronizadoState: MutableState<Boolean>,
    val mostrarDialogoMediaState: MutableState<Boolean>,
    val expandedNombreState: MutableState<Boolean>,
    val expandedFormatoState: MutableState<Boolean>,
    val expandedConcentracionState: MutableState<Boolean>,
    val mostrarConcentracionLibreState: MutableState<Boolean>,
    val expandedCicloState: MutableState<Boolean>,
    val expandedTomaState: MutableState<Boolean>,
    val expandedPesoUnidadState: MutableState<Boolean>,
    val expandedEstaturaUnidadState: MutableState<Boolean>,
    val expandedPaisPacienteState: MutableState<Boolean>,
    val expandedFrecuenciaBackupState: MutableState<Boolean>,
    val expandedReintentoCriticoState: MutableState<Boolean>,
    val expandedIntentosCriticosState: MutableState<Boolean>,
    val expandedRecordatorioCitaState: MutableState<Boolean>,
    val expandedProfesionalInformeState: MutableState<Boolean>,
    val expandedFiltroProfesionalInformesState: MutableState<Boolean>,
    val filtroProfesionalInformesIdState: MutableState<Int?>,
    val formatoSeleccionadoState: MutableState<String>,
    val formaInsumoSeleccionadaState: MutableState<String>,
    val colorInsumoSeleccionadoState: MutableState<Color>,
    val colorInsumo2SeleccionadoState: MutableState<Color>,
    val presentacionPersistidaState: MutableState<String>,
    val concentracionSeleccionadaState: MutableState<String>,
    val cicloSeleccionadoState: MutableState<String>,
    val tomaSeleccionadaState: MutableState<String>,
    val horasTomas: SnapshotStateList<String>,
    val filtroExportacionSignosState: MutableState<VitalSignsExportFilter>,
    val fechaInicioExportacionSignosState: MutableState<Long>,
    val fechaFinExportacionSignosState: MutableState<Long>,
    val expandedFiltroExportacionSignosState: MutableState<Boolean>,
    val mostrarVistaPreviaSignosSeleccionadosState: MutableState<Boolean>,
    val exportandoSignosVitalesState: MutableState<Boolean>,
    val intervaloReintentoSeleccionadoState: MutableState<Int>,
    val numeroIntentosCriticosSeleccionadoState: MutableState<Int>,
    val alarmaSonidoUriState: MutableState<String>,
    val alarmaSonidoNombreState: MutableState<String>,
    val tienePermisoNotificacionesState: MutableState<Boolean>,
    val tienePermisoAlarmaExactaState: MutableState<Boolean>,
    val tienePermisoPantallaCompletaState: MutableState<Boolean>,
    val tieneAccesoNoMolestarState: MutableState<Boolean>,
    val tienePermisoCamaraState: MutableState<Boolean>
)

internal val MedicamentoFormState.mostrarFormulario get() = mostrarFormularioState.value
internal val MedicamentoFormState.mostrarFichaPaciente get() = mostrarFichaPacienteState.value
internal val MedicamentoFormState.mostrarFormularioInforme get() = mostrarFormularioInformeState.value
internal val MedicamentoFormState.mostrarFormularioProfesional get() = mostrarFormularioProfesionalState.value
internal val MedicamentoFormState.mostrarPanelPacientes get() = mostrarPanelPacientesState.value
internal val MedicamentoFormState.mostrarPanelProfesionales get() = mostrarPanelProfesionalesState.value
internal val MedicamentoFormState.mostrarPanelInformes get() = mostrarPanelInformesState.value
internal val MedicamentoFormState.mostrarListaInsumos get() = mostrarListaInsumosState.value
internal val MedicamentoFormState.mostrarPanelBackups get() = mostrarPanelBackupsState.value
internal val MedicamentoFormState.mostrarPanelPedidos get() = mostrarPanelPedidosState.value
internal val MedicamentoFormState.mostrarPanelPodometro get() = mostrarPanelPodometroState.value
internal val MedicamentoFormState.mostrarPanelConfiguracionAlertas get() = mostrarPanelConfiguracionAlertasState.value
internal val MedicamentoFormState.mostrarPanelSignosVitales get() = mostrarPanelSignosVitalesState.value
internal val MedicamentoFormState.mostrarPanelConfiguracionIa get() = mostrarPanelConfiguracionIaState.value
internal val MedicamentoFormState.mostrarPanelAsistenteIa get() = mostrarPanelAsistenteIaState.value
internal val MedicamentoFormState.mostrarPanelCicloMenstrual get() = mostrarPanelCicloMenstrualState.value
internal val MedicamentoFormState.mostrarPanelEmbarazo get() = mostrarPanelEmbarazoState.value
internal val MedicamentoFormState.mostrarPanelAnticonceptivos get() = mostrarPanelAnticonceptivosState.value
internal val MedicamentoFormState.mostrarPanelPediatrico get() = mostrarPanelPediatricoState.value
internal val MedicamentoFormState.mostrarPanelReporteClinico get() = mostrarPanelReporteClinicoState.value
internal val MedicamentoFormState.mostrarPanelEstadisticas get() = mostrarPanelEstadisticasState.value
internal val MedicamentoFormState.mostrarPanelDiario get() = mostrarPanelDiarioState.value
internal val MedicamentoFormState.mostrarPanelVerificadorTomas get() = mostrarPanelVerificadorTomasState.value
internal val MedicamentoFormState.mostrarPanelHidratacion get() = mostrarPanelHidratacionState.value
internal val MedicamentoFormState.mostrarPanelSedentarismo get() = mostrarPanelSedentarismoState.value
internal val MedicamentoFormState.mostrarPanelDentista get() = mostrarPanelDentistaState.value
internal val MedicamentoFormState.mostrarPanelCitasMedicas get() = mostrarPanelCitasMedicasState.value
internal val MedicamentoFormState.mostrarFormularioCitaMedica get() = mostrarFormularioCitaMedicaState.value
internal val MedicamentoFormState.mostrarDialogoCerrarInformeSinGuardar get() = mostrarDialogoCerrarInformeSinGuardarState.value
internal val MedicamentoFormState.editingMedicationId get() = editingMedicationIdState.value
internal val MedicamentoFormState.editingPractitionerId get() = editingPractitionerIdState.value
internal val MedicamentoFormState.profesionalSeleccionadoId get() = profesionalSeleccionadoIdState.value
internal val MedicamentoFormState.citaMedicaSeleccionadaId get() = citaMedicaSeleccionadaIdState.value
internal val MedicamentoFormState.alarmaSonidoNombre get() = alarmaSonidoNombreState.value
internal val MedicamentoFormState.alarmaSonidoUri get() = alarmaSonidoUriState.value
internal val MedicamentoFormState.filtroExportacionSignos get() = filtroExportacionSignosState.value
internal val MedicamentoFormState.fechaInicioExportacionSignos get() = fechaInicioExportacionSignosState.value
internal val MedicamentoFormState.fechaFinExportacionSignos get() = fechaFinExportacionSignosState.value
internal val MedicamentoFormState.expandedFiltroExportacionSignos get() = expandedFiltroExportacionSignosState.value
internal val MedicamentoFormState.mostrarVistaPreviaSignosSeleccionados get() = mostrarVistaPreviaSignosSeleccionadosState.value
internal val MedicamentoFormState.exportandoSignosVitales get() = exportandoSignosVitalesState.value
internal val MedicamentoFormState.intervaloReintentoSeleccionado get() = intervaloReintentoSeleccionadoState.value
internal val MedicamentoFormState.numeroIntentosCriticosSeleccionado get() = numeroIntentosCriticosSeleccionadoState.value
internal val MedicamentoFormState.tienePermisoNotificaciones get() = tienePermisoNotificacionesState.value
internal val MedicamentoFormState.tienePermisoAlarmaExacta get() = tienePermisoAlarmaExactaState.value
internal val MedicamentoFormState.tienePermisoPantallaCompleta get() = tienePermisoPantallaCompletaState.value
internal val MedicamentoFormState.tieneAccesoNoMolestar get() = tieneAccesoNoMolestarState.value
internal val MedicamentoFormState.tienePermisoCamara get() = tienePermisoCamaraState.value
internal val MedicamentoFormState.sistolicaInput get() = sistolicaInputState.value
internal val MedicamentoFormState.diastolicaInput get() = diastolicaInputState.value
internal val MedicamentoFormState.comentarioPresionInput get() = comentarioPresionInputState.value
internal val MedicamentoFormState.latidosInput get() = latidosInputState.value
internal val MedicamentoFormState.comentarioLatidosInput get() = comentarioLatidosInputState.value
internal val MedicamentoFormState.glucemiaInput get() = glucemiaInputState.value
internal val MedicamentoFormState.comentarioGlucemiaInput get() = comentarioGlucemiaInputState.value
internal val MedicamentoFormState.temperaturaInput get() = temperaturaInputState.value
internal val MedicamentoFormState.comentarioTemperaturaInput get() = comentarioTemperaturaInputState.value
internal val MedicamentoFormState.expandedFrecuenciaBackup get() = expandedFrecuenciaBackupState.value
internal val MedicamentoFormState.mensajeBackup get() = mensajeBackupState.value
internal val MedicamentoFormState.ejecutandoBackupManual get() = ejecutandoBackupManualState.value
internal val MedicamentoFormState.restaurandoBackup get() = restaurandoBackupState.value
internal val MedicamentoFormState.insumoSeleccionadoEnInventario get() = insumoSeleccionadoEnInventarioState.value
internal val MedicamentoFormState.expandedReintentoCritico get() = expandedReintentoCriticoState.value
internal val MedicamentoFormState.expandedIntentosCriticos get() = expandedIntentosCriticosState.value
internal val MedicamentoFormState.nombre get() = nombreState.value
internal val MedicamentoFormState.cantidad get() = cantidadState.value
internal val MedicamentoFormState.horaTomaSeleccionada get() = horaTomaSeleccionadaState.value
internal val MedicamentoFormState.formatoSeleccionado get() = formatoSeleccionadoState.value
internal val MedicamentoFormState.formaInsumoSeleccionada get() = formaInsumoSeleccionadaState.value
internal val MedicamentoFormState.colorInsumoSeleccionado get() = colorInsumoSeleccionadoState.value
internal val MedicamentoFormState.colorInsumo2Seleccionado get() = colorInsumo2SeleccionadoState.value
internal val MedicamentoFormState.concentracionSeleccionada get() = concentracionSeleccionadaState.value
internal val MedicamentoFormState.cicloSeleccionado get() = cicloSeleccionadoState.value
internal val MedicamentoFormState.fechaInicio get() = fechaInicioState.value
internal val MedicamentoFormState.fechaFin get() = fechaFinState.value
internal val MedicamentoFormState.stockActual get() = stockActualState.value
internal val MedicamentoFormState.stockMinimo get() = stockMinimoState.value
internal val MedicamentoFormState.precioPorUnidad get() = precioPorUnidadState.value
internal val MedicamentoFormState.telefonoPedidoWhatsapp get() = telefonoPedidoWhatsappState.value
internal val MedicamentoFormState.presentacionPersistida get() = presentacionPersistidaState.value
internal val MedicamentoFormState.esCicloCorto get() = esCicloCortoState.value
internal val MedicamentoFormState.estaActivo get() = estaActivoState.value
internal val MedicamentoFormState.alarmaActiva get() = alarmaActivaState.value
internal val MedicamentoFormState.controlarExistencias get() = controlarExistenciasState.value
internal val MedicamentoFormState.dispensacionGratuita get() = dispensacionGratuitaState.value
internal val MedicamentoFormState.expandedNombre get() = expandedNombreState.value
internal val MedicamentoFormState.expandedToma get() = expandedTomaState.value
internal val MedicamentoFormState.expandedConcentracion get() = expandedConcentracionState.value
internal val MedicamentoFormState.expandedCiclo get() = expandedCicloState.value
internal val MedicamentoFormState.expandedOrigenReposicion get() = expandedOrigenReposicionState.value
internal val MedicamentoFormState.selectedMedication get() = selectedMedicationState.value
internal val MedicamentoFormState.tomaSeleccionada get() = tomaSeleccionadaState.value
internal val MedicamentoFormState.origenReposicion get() = origenReposicionState.value
internal val MedicamentoFormState.medicationToDelete get() = medicationToDeleteState.value
internal val MedicamentoFormState.duplicateMedication get() = duplicateMedicationState.value
internal val MedicamentoFormState.insumoARecargar get() = insumoARecargarState.value
internal val MedicamentoFormState.insumoAPedir get() = insumoAPedirState.value
internal val MedicamentoFormState.itemCarritoAConfirmar get() = itemCarritoAConfirmarState.value
internal val MedicamentoFormState.itemCarritoAEliminar get() = itemCarritoAEliminarState.value
internal val MedicamentoFormState.confirmarRecepcionTotal get() = confirmarRecepcionTotalState.value
internal val MedicamentoFormState.pedidoAEliminar get() = pedidoAEliminarState.value
internal val MedicamentoFormState.pedidoAEditar get() = pedidoAEditarState.value
internal val MedicamentoFormState.tomaPendienteDeEliminar get() = tomaPendienteDeEliminarState.value
internal val MedicamentoFormState.inputRecargarStock get() = inputRecargarStockState.value
internal val MedicamentoFormState.inputUnidadesPedido get() = inputUnidadesPedidoState.value
internal val MedicamentoFormState.inputUnidadesRecibidas get() = inputUnidadesRecibidasState.value
internal val MedicamentoFormState.inputPrecioActualizado get() = inputPrecioActualizadoState.value
internal val MedicamentoFormState.inputEditarResumen get() = inputEditarResumenState.value
internal val MedicamentoFormState.inputEditarTotal get() = inputEditarTotalState.value
internal val MedicamentoFormState.visorAdjuntos get() = visorAdjuntosState.value
internal val MedicamentoFormState.nombreProfesional get() = nombreProfesionalState.value
internal val MedicamentoFormState.especialidadProfesional get() = especialidadProfesionalState.value
internal val MedicamentoFormState.tituloInforme get() = tituloInformeState.value
internal val MedicamentoFormState.descripcionInforme get() = descripcionInformeState.value
internal val MedicamentoFormState.expandedProfesionalInforme get() = expandedProfesionalInformeState.value
internal val MedicamentoFormState.practitionerIdInforme get() = practitionerIdInformeState.value
internal val MedicamentoFormState.cameraPermissionPending get() = cameraPermissionPendingState.value

@Composable
internal fun rememberMedicamentoFormState(
    alarmaSonidoUriState: MutableState<String> = remember { mutableStateOf("") },
    alarmaSonidoNombreState: MutableState<String> = remember { mutableStateOf("Alarma predeterminada") },
    intervaloReintentoSeleccionadoState: MutableState<Int> = remember { mutableStateOf(CriticalAlertSettings.DEFAULT_RETRY_INTERVAL_MINUTES) },
    numeroIntentosCriticosSeleccionadoState: MutableState<Int> = remember { mutableStateOf(CriticalAlertSettings.DEFAULT_MAX_RETRY_COUNT) },
    tienePermisoNotificacionesState: MutableState<Boolean> = remember { mutableStateOf(false) },
    tienePermisoAlarmaExactaState: MutableState<Boolean> = remember { mutableStateOf(false) },
    tienePermisoPantallaCompletaState: MutableState<Boolean> = remember { mutableStateOf(false) },
    tieneAccesoNoMolestarState: MutableState<Boolean> = remember { mutableStateOf(false) },
    tienePermisoCamaraState: MutableState<Boolean> = remember { mutableStateOf(false) }
): MedicamentoFormState {
    val nombreState = remember { mutableStateOf("") }
    val cantidadState = remember { mutableStateOf("") }
    val alarmaActivaState = remember { mutableStateOf(true) }
    val esCicloCortoState = remember { mutableStateOf(false) }
    val estaActivoState = remember { mutableStateOf(true) }
    val editingMedicationIdState = remember { mutableStateOf<Int?>(null) }
    val controlarExistenciasState = remember { mutableStateOf(false) }
    val stockActualState = remember { mutableStateOf("") }
    val stockMinimoState = remember { mutableStateOf("") }
    val precioPorUnidadState = remember { mutableStateOf("") }
    val telefonoPedidoWhatsappState = remember { mutableStateOf("") }
    val dispensacionGratuitaState = remember { mutableStateOf(false) }
    val origenReposicionState = remember { mutableStateOf(RestockSource.WHATSAPP_NUMBER) }
    val expandedOrigenReposicionState = remember { mutableStateOf(false) }
    val selectedMedicationState = remember { mutableStateOf<VademecumMedication?>(null) }
    val fechaInicioState = remember { mutableStateOf(System.currentTimeMillis()) }
    val fechaFinState = remember { mutableStateOf(System.currentTimeMillis()) }
    val horaTomaSeleccionadaState = remember { mutableStateOf("") }
    val medicationToDeleteState = remember { mutableStateOf<Medication?>(null) }
    val insumoARecargarState = remember { mutableStateOf<Medication?>(null) }
    val inputRecargarStockState = remember { mutableStateOf("") }
    val insumoAPedirState = remember { mutableStateOf<Medication?>(null) }
    val inputUnidadesPedidoState = remember { mutableStateOf("") }
    val itemCarritoAConfirmarState = remember { mutableStateOf<CarritoItem?>(null) }
    val itemCarritoAEliminarState = remember { mutableStateOf<CarritoItem?>(null) }
    val confirmarRecepcionTotalState = remember { mutableStateOf(false) }
    val pedidoAEliminarState = remember { mutableStateOf<MedicationOrder?>(null) }
    val pedidoAEditarState = remember { mutableStateOf<MedicationOrder?>(null) }
    val inputEditarResumenState = remember { mutableStateOf("") }
    val inputEditarTotalState = remember { mutableStateOf("") }
    val inputUnidadesRecibidasState = remember { mutableStateOf("") }
    val inputPrecioActualizadoState = remember { mutableStateOf("") }
    val duplicateMedicationState = remember { mutableStateOf<Medication?>(null) }
    val mostrarFormularioState = remember { mutableStateOf(false) }
    val mostrarFichaPacienteState = remember { mutableStateOf(false) }
    val mostrarMenuHamburguesaState = remember { mutableStateOf(false) }
    val mostrarFormularioInformeState = remember { mutableStateOf(false) }
    val formularioInformeAutoAbiertoState = remember { mutableStateOf(false) }
    val mostrarFormularioProfesionalState = remember { mutableStateOf(false) }
    val mostrarPanelPacientesState = remember { mutableStateOf(false) }
    val mostrarPanelProfesionalesState = remember { mutableStateOf(false) }
    val mostrarPanelInformesState = remember { mutableStateOf(false) }
    val mostrarListaInsumosState = remember { mutableStateOf(false) }
    val mostrarPanelBackupsState = remember { mutableStateOf(false) }
    val mostrarPanelPedidosState = remember { mutableStateOf(false) }
    val mostrarPanelPodometroState = remember { mutableStateOf(false) }
    val mostrarPanelConfiguracionAlertasState = remember { mutableStateOf(false) }
    val mostrarPanelSignosVitalesState = remember { mutableStateOf(false) }
    val mostrarPanelConfiguracionIaState = remember { mutableStateOf(false) }
    val mostrarPanelAsistenteIaState = remember { mutableStateOf(false) }
    val mostrarPanelCicloMenstrualState = remember { mutableStateOf(false) }
    val mostrarPanelEmbarazoState = remember { mutableStateOf(false) }
    val mostrarPanelAnticonceptivosState = remember { mutableStateOf(false) }
    val mostrarPanelPediatricoState = remember { mutableStateOf(false) }
    val mostrarPanelReporteClinicoState = remember { mutableStateOf(false) }
    val mostrarPanelEstadisticasState = remember { mutableStateOf(false) }
    val mostrarPanelDiarioState = remember { mutableStateOf(false) }
    val mostrarPanelVerificadorTomasState = remember { mutableStateOf(false) }
    val mostrarPanelHidratacionState = remember { mutableStateOf(false) }
    val mostrarPanelSedentarismoState = remember { mutableStateOf(false) }
    val mostrarPanelDentistaState = remember { mutableStateOf(false) }
    val insumoSeleccionadoEnInventarioState = remember { mutableStateOf<Int?>(null) }
    val editingPatientIdState = remember { mutableStateOf<Int?>(null) }
    val editingReportIdState = remember { mutableStateOf<Int?>(null) }
    val practitionerIdInformeState = remember { mutableStateOf<Int?>(null) }
    val editingPractitionerIdState = remember { mutableStateOf<Int?>(null) }
    val profesionalSeleccionadoIdState = remember { mutableStateOf<Int?>(null) }
    val citaMedicaSeleccionadaIdState = remember { mutableStateOf<Int?>(null) }
    val editandoFichaPacienteState = remember { mutableStateOf(false) }
    val editingAppointmentIdState = remember { mutableStateOf<Int?>(null) }
    val nombrePacienteState = remember { mutableStateOf("") }
    val apellidosPacienteState = remember { mutableStateOf("") }
    val fechaNacimientoPacienteState = remember { mutableStateOf<Long?>(null) }
    val edadPacienteState = remember { mutableStateOf("") }
    val pesoPacienteState = remember { mutableStateOf("") }
    val pesoUnidadPacienteState = remember { mutableStateOf("kg") }
    val estaturaPacienteState = remember { mutableStateOf("") }
    val estaturaUnidadPacienteState = remember { mutableStateOf("cm") }
    val sexoPacienteState = remember { mutableStateOf("") }
    val paisPacienteState = remember { mutableStateOf(CountryCurrencyCatalog.DEFAULT_COUNTRY) }
    val monedaPacienteState = remember { mutableStateOf(CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL) }
    val enfermedadesPacienteState = remember { mutableStateOf("") }
    val prescripcionesPacienteState = remember { mutableStateOf("") }
    val fotoPerfilPacienteState = remember { mutableStateOf<String?>(null) }
    val cameraPermissionPerfilPendingState = remember { mutableStateOf(false) }
    val estudiosAdjuntos = remember { mutableStateListOf<String>() }
    val tituloInformeState = remember { mutableStateOf("") }
    val descripcionInformeState = remember { mutableStateOf("") }
    val nombreProfesionalState = remember { mutableStateOf("") }
    val especialidadProfesionalState = remember { mutableStateOf("") }
    val tituloCitaMedicaState = remember { mutableStateOf("") }
    val profesionalCitaMedicaState = remember { mutableStateOf("") }
    val lugarCitaMedicaState = remember { mutableStateOf("") }
    val notasCitaMedicaState = remember { mutableStateOf("") }
    val fechaCitaMedicaState = remember { mutableStateOf(siguienteHoraDisponible()) }
    val recordatorioCitaMinutosState = remember { mutableStateOf(60) }
    val alarmaCitaMedicaActivaState = remember { mutableStateOf(true) }
    val ejecutandoBackupManualState = remember { mutableStateOf(false) }
    val restaurandoBackupState = remember { mutableStateOf(false) }
    val backupSelectionState = remember { mutableStateOf(BackupSelection.all()) }
    val restoreSelectionState = remember { mutableStateOf(BackupSelection.all()) }
    val backupPatientIdState = remember { mutableStateOf<Int?>(null) }
    val restorePatientIdState = remember { mutableStateOf<Int?>(null) }
    val backupPatientDropdownExpandedState = remember { mutableStateOf(false) }
    val restorePatientDropdownExpandedState = remember { mutableStateOf(false) }
    val cameraPermissionPendingState = remember { mutableStateOf(false) }
    val mostrarDialogoBackupManualState = remember { mutableStateOf(false) }
    val mostrarDialogoRestoreSeleccionState = remember { mutableStateOf(false) }
    val mostrarDialogoProgramarBackupState = remember { mutableStateOf(false) }
    val mostrarDialogoCerrarInformeSinGuardarState = remember { mutableStateOf(false) }
    val mensajeBackupState = remember { mutableStateOf("") }
    val visorAdjuntosState = remember { mutableStateOf<AttachmentViewerState?>(null) }
    val adjuntosPendientesReemplazo = remember { mutableStateListOf<PendingAttachmentReplacement>() }
    val borradorInformeInicialState = remember { mutableStateOf(ReportDraftSnapshot()) }
    val sistolicaInputState = remember { mutableStateOf("") }
    val diastolicaInputState = remember { mutableStateOf("") }
    val comentarioPresionInputState = remember { mutableStateOf("") }
    val latidosInputState = remember { mutableStateOf("") }
    val comentarioLatidosInputState = remember { mutableStateOf("") }
    val glucemiaInputState = remember { mutableStateOf("") }
    val comentarioGlucemiaInputState = remember { mutableStateOf("") }
    val temperaturaInputState = remember { mutableStateOf("") }
    val comentarioTemperaturaInputState = remember { mutableStateOf("") }
    val pesoInputState = remember { mutableStateOf("") }
    val pesoUnidadKgState = remember { mutableStateOf(true) }
    val tomaPendienteDeEliminarState = remember { mutableStateOf<IntakeRemovalConfirmation?>(null) }
    val perfilPendienteDeEliminarState = remember { mutableStateOf<PatientProfile?>(null) }
    val mostrarFormularioCitaMedicaState = remember { mutableStateOf(false) }
    val mostrarPanelCitasMedicasState = remember { mutableStateOf(false) }
    val citaPendienteDeEliminarState = remember { mutableStateOf<MedicalAppointment?>(null) }
    val reportePendienteDeEliminarState = remember { mutableStateOf<MedicalReport?>(null) }
    val mostrarDialogoPesoSincronizadoState = remember { mutableStateOf(false) }
    val mostrarDialogoMediaState = remember { mutableStateOf(false) }
    val expandedNombreState = remember { mutableStateOf(false) }
    val expandedFormatoState = remember { mutableStateOf(false) }
    val expandedConcentracionState = remember { mutableStateOf(false) }
    val mostrarConcentracionLibreState = remember { mutableStateOf(false) }
    val expandedCicloState = remember { mutableStateOf(false) }
    val expandedTomaState = remember { mutableStateOf(false) }
    val expandedPesoUnidadState = remember { mutableStateOf(false) }
    val expandedEstaturaUnidadState = remember { mutableStateOf(false) }
    val expandedPaisPacienteState = remember { mutableStateOf(false) }
    val expandedFrecuenciaBackupState = remember { mutableStateOf(false) }
    val expandedReintentoCriticoState = remember { mutableStateOf(false) }
    val expandedIntentosCriticosState = remember { mutableStateOf(false) }
    val expandedRecordatorioCitaState = remember { mutableStateOf(false) }
    val expandedProfesionalInformeState = remember { mutableStateOf(false) }
    val expandedFiltroProfesionalInformesState = remember { mutableStateOf(false) }
    val filtroProfesionalInformesIdState = remember { mutableStateOf<Int?>(null) }
    val formatoSeleccionadoState = remember { mutableStateOf("") }
    val formaInsumoSeleccionadaState = remember { mutableStateOf("") }
    val colorInsumoSeleccionadoState = remember { mutableStateOf(Color(0xFFFFFF00)) }
    val colorInsumo2SeleccionadoState = remember { mutableStateOf(Color(0xFFFFFFFF)) }
    val presentacionPersistidaState = remember { mutableStateOf("") }
    val concentracionSeleccionadaState = remember { mutableStateOf("") }
    val cicloSeleccionadoState = remember { mutableStateOf("Diario") }
    val tomaSeleccionadaState = remember { mutableStateOf("En una sola toma") }
    val horasTomas = remember { mutableStateListOf<String>() }
    val filtroExportacionSignosState = remember { mutableStateOf(VitalSignsExportFilter.TODAY) }
    val fechaInicioExportacionSignosState = remember { mutableStateOf(inicioDelDia(System.currentTimeMillis())) }
    val fechaFinExportacionSignosState = remember { mutableStateOf(finDelDia(System.currentTimeMillis())) }
    val expandedFiltroExportacionSignosState = remember { mutableStateOf(false) }
    val mostrarVistaPreviaSignosSeleccionadosState = remember { mutableStateOf(false) }
    val exportandoSignosVitalesState = remember { mutableStateOf(false) }

    return MedicamentoFormState(
        nombreState = nombreState,
        cantidadState = cantidadState,
        alarmaActivaState = alarmaActivaState,
        esCicloCortoState = esCicloCortoState,
        estaActivoState = estaActivoState,
        editingMedicationIdState = editingMedicationIdState,
        controlarExistenciasState = controlarExistenciasState,
        stockActualState = stockActualState,
        stockMinimoState = stockMinimoState,
        precioPorUnidadState = precioPorUnidadState,
        telefonoPedidoWhatsappState = telefonoPedidoWhatsappState,
        dispensacionGratuitaState = dispensacionGratuitaState,
        origenReposicionState = origenReposicionState,
        expandedOrigenReposicionState = expandedOrigenReposicionState,
        selectedMedicationState = selectedMedicationState,
        fechaInicioState = fechaInicioState,
        fechaFinState = fechaFinState,
        horaTomaSeleccionadaState = horaTomaSeleccionadaState,
        medicationToDeleteState = medicationToDeleteState,
        insumoARecargarState = insumoARecargarState,
        inputRecargarStockState = inputRecargarStockState,
        insumoAPedirState = insumoAPedirState,
        inputUnidadesPedidoState = inputUnidadesPedidoState,
        itemCarritoAConfirmarState = itemCarritoAConfirmarState,
        itemCarritoAEliminarState = itemCarritoAEliminarState,
        confirmarRecepcionTotalState = confirmarRecepcionTotalState,
        pedidoAEliminarState = pedidoAEliminarState,
        pedidoAEditarState = pedidoAEditarState,
        inputEditarResumenState = inputEditarResumenState,
        inputEditarTotalState = inputEditarTotalState,
        inputUnidadesRecibidasState = inputUnidadesRecibidasState,
        inputPrecioActualizadoState = inputPrecioActualizadoState,
        duplicateMedicationState = duplicateMedicationState,
        mostrarFormularioState = mostrarFormularioState,
        mostrarFichaPacienteState = mostrarFichaPacienteState,
        mostrarMenuHamburguesaState = mostrarMenuHamburguesaState,
        mostrarFormularioInformeState = mostrarFormularioInformeState,
        formularioInformeAutoAbiertoState = formularioInformeAutoAbiertoState,
        mostrarFormularioProfesionalState = mostrarFormularioProfesionalState,
        mostrarPanelPacientesState = mostrarPanelPacientesState,
        mostrarPanelProfesionalesState = mostrarPanelProfesionalesState,
        mostrarPanelInformesState = mostrarPanelInformesState,
        mostrarListaInsumosState = mostrarListaInsumosState,
        mostrarPanelBackupsState = mostrarPanelBackupsState,
        mostrarPanelPedidosState = mostrarPanelPedidosState,
        mostrarPanelPodometroState = mostrarPanelPodometroState,
        mostrarPanelConfiguracionAlertasState = mostrarPanelConfiguracionAlertasState,
        mostrarPanelSignosVitalesState = mostrarPanelSignosVitalesState,
        mostrarPanelConfiguracionIaState = mostrarPanelConfiguracionIaState,
        mostrarPanelAsistenteIaState = mostrarPanelAsistenteIaState,
        mostrarPanelCicloMenstrualState = mostrarPanelCicloMenstrualState,
        mostrarPanelEmbarazoState = mostrarPanelEmbarazoState,
        mostrarPanelAnticonceptivosState = mostrarPanelAnticonceptivosState,
        mostrarPanelPediatricoState = mostrarPanelPediatricoState,
        mostrarPanelReporteClinicoState = mostrarPanelReporteClinicoState,
        mostrarPanelEstadisticasState = mostrarPanelEstadisticasState,
        mostrarPanelDiarioState = mostrarPanelDiarioState,
        mostrarPanelVerificadorTomasState = mostrarPanelVerificadorTomasState,
        mostrarPanelHidratacionState = mostrarPanelHidratacionState,
        mostrarPanelSedentarismoState = mostrarPanelSedentarismoState,
        mostrarPanelDentistaState = mostrarPanelDentistaState,
        insumoSeleccionadoEnInventarioState = insumoSeleccionadoEnInventarioState,
        editingPatientIdState = editingPatientIdState,
        editingReportIdState = editingReportIdState,
        practitionerIdInformeState = practitionerIdInformeState,
        editingPractitionerIdState = editingPractitionerIdState,
        profesionalSeleccionadoIdState = profesionalSeleccionadoIdState,
        citaMedicaSeleccionadaIdState = citaMedicaSeleccionadaIdState,
        editandoFichaPacienteState = editandoFichaPacienteState,
        editingAppointmentIdState = editingAppointmentIdState,
        nombrePacienteState = nombrePacienteState,
        apellidosPacienteState = apellidosPacienteState,
        fechaNacimientoPacienteState = fechaNacimientoPacienteState,
        edadPacienteState = edadPacienteState,
        pesoPacienteState = pesoPacienteState,
        pesoUnidadPacienteState = pesoUnidadPacienteState,
        estaturaPacienteState = estaturaPacienteState,
        estaturaUnidadPacienteState = estaturaUnidadPacienteState,
        sexoPacienteState = sexoPacienteState,
        paisPacienteState = paisPacienteState,
        monedaPacienteState = monedaPacienteState,
        enfermedadesPacienteState = enfermedadesPacienteState,
        prescripcionesPacienteState = prescripcionesPacienteState,
        fotoPerfilPacienteState = fotoPerfilPacienteState,
        cameraPermissionPerfilPendingState = cameraPermissionPerfilPendingState,
        estudiosAdjuntos = estudiosAdjuntos,
        tituloInformeState = tituloInformeState,
        descripcionInformeState = descripcionInformeState,
        nombreProfesionalState = nombreProfesionalState,
        especialidadProfesionalState = especialidadProfesionalState,
        tituloCitaMedicaState = tituloCitaMedicaState,
        profesionalCitaMedicaState = profesionalCitaMedicaState,
        lugarCitaMedicaState = lugarCitaMedicaState,
        notasCitaMedicaState = notasCitaMedicaState,
        fechaCitaMedicaState = fechaCitaMedicaState,
        recordatorioCitaMinutosState = recordatorioCitaMinutosState,
        alarmaCitaMedicaActivaState = alarmaCitaMedicaActivaState,
        ejecutandoBackupManualState = ejecutandoBackupManualState,
        restaurandoBackupState = restaurandoBackupState,
        backupSelectionState = backupSelectionState,
        restoreSelectionState = restoreSelectionState,
        backupPatientIdState = backupPatientIdState,
        restorePatientIdState = restorePatientIdState,
        backupPatientDropdownExpandedState = backupPatientDropdownExpandedState,
        restorePatientDropdownExpandedState = restorePatientDropdownExpandedState,
        cameraPermissionPendingState = cameraPermissionPendingState,
        mostrarDialogoBackupManualState = mostrarDialogoBackupManualState,
        mostrarDialogoRestoreSeleccionState = mostrarDialogoRestoreSeleccionState,
        mostrarDialogoProgramarBackupState = mostrarDialogoProgramarBackupState,
        mostrarDialogoCerrarInformeSinGuardarState = mostrarDialogoCerrarInformeSinGuardarState,
        mensajeBackupState = mensajeBackupState,
        visorAdjuntosState = visorAdjuntosState,
        adjuntosPendientesReemplazo = adjuntosPendientesReemplazo,
        borradorInformeInicialState = borradorInformeInicialState,
        sistolicaInputState = sistolicaInputState,
        diastolicaInputState = diastolicaInputState,
        comentarioPresionInputState = comentarioPresionInputState,
        latidosInputState = latidosInputState,
        comentarioLatidosInputState = comentarioLatidosInputState,
        glucemiaInputState = glucemiaInputState,
        comentarioGlucemiaInputState = comentarioGlucemiaInputState,
        temperaturaInputState = temperaturaInputState,
        comentarioTemperaturaInputState = comentarioTemperaturaInputState,
        pesoInputState = pesoInputState,
        pesoUnidadKgState = pesoUnidadKgState,
        tomaPendienteDeEliminarState = tomaPendienteDeEliminarState,
        perfilPendienteDeEliminarState = perfilPendienteDeEliminarState,
        mostrarFormularioCitaMedicaState = mostrarFormularioCitaMedicaState,
        mostrarPanelCitasMedicasState = mostrarPanelCitasMedicasState,
        citaPendienteDeEliminarState = citaPendienteDeEliminarState,
        reportePendienteDeEliminarState = reportePendienteDeEliminarState,
        mostrarDialogoPesoSincronizadoState = mostrarDialogoPesoSincronizadoState,
        mostrarDialogoMediaState = mostrarDialogoMediaState,
        expandedNombreState = expandedNombreState,
        expandedFormatoState = expandedFormatoState,
        expandedConcentracionState = expandedConcentracionState,
        mostrarConcentracionLibreState = mostrarConcentracionLibreState,
        expandedCicloState = expandedCicloState,
        expandedTomaState = expandedTomaState,
        expandedPesoUnidadState = expandedPesoUnidadState,
        expandedEstaturaUnidadState = expandedEstaturaUnidadState,
        expandedPaisPacienteState = expandedPaisPacienteState,
        expandedFrecuenciaBackupState = expandedFrecuenciaBackupState,
        expandedReintentoCriticoState = expandedReintentoCriticoState,
        expandedIntentosCriticosState = expandedIntentosCriticosState,
        expandedRecordatorioCitaState = expandedRecordatorioCitaState,
        expandedProfesionalInformeState = expandedProfesionalInformeState,
        expandedFiltroProfesionalInformesState = expandedFiltroProfesionalInformesState,
        filtroProfesionalInformesIdState = filtroProfesionalInformesIdState,
        formatoSeleccionadoState = formatoSeleccionadoState,
        formaInsumoSeleccionadaState = formaInsumoSeleccionadaState,
        colorInsumoSeleccionadoState = colorInsumoSeleccionadoState,
        colorInsumo2SeleccionadoState = colorInsumo2SeleccionadoState,
        presentacionPersistidaState = presentacionPersistidaState,
        concentracionSeleccionadaState = concentracionSeleccionadaState,
        cicloSeleccionadoState = cicloSeleccionadoState,
        tomaSeleccionadaState = tomaSeleccionadaState,
        horasTomas = horasTomas,
        filtroExportacionSignosState = filtroExportacionSignosState,
        fechaInicioExportacionSignosState = fechaInicioExportacionSignosState,
        fechaFinExportacionSignosState = fechaFinExportacionSignosState,
        expandedFiltroExportacionSignosState = expandedFiltroExportacionSignosState,
        mostrarVistaPreviaSignosSeleccionadosState = mostrarVistaPreviaSignosSeleccionadosState,
        exportandoSignosVitalesState = exportandoSignosVitalesState,
        intervaloReintentoSeleccionadoState = intervaloReintentoSeleccionadoState,
        numeroIntentosCriticosSeleccionadoState = numeroIntentosCriticosSeleccionadoState,
        alarmaSonidoUriState = alarmaSonidoUriState,
        alarmaSonidoNombreState = alarmaSonidoNombreState,
        tienePermisoNotificacionesState = tienePermisoNotificacionesState,
        tienePermisoAlarmaExactaState = tienePermisoAlarmaExactaState,
        tienePermisoPantallaCompletaState = tienePermisoPantallaCompletaState,
        tieneAccesoNoMolestarState = tieneAccesoNoMolestarState,
        tienePermisoCamaraState = tienePermisoCamaraState
    )
}
