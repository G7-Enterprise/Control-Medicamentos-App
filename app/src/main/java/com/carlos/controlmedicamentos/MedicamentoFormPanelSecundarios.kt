package com.carlos.controlmedicamentos

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.backup.BackupSelection
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun MedicamentoFormPanelSecundarios(
    modifier: Modifier = Modifier,
    s: MedicamentoFormState,
    callbacks: MedicamentoFormCallbacks,
    launchers: FormLaunchers,
    pacienteActivo: PatientProfile?,
    perfilesPacientes: List<PatientProfile>,
    insumosGuardados: List<Medication>,
    carritoItems: List<CarritoItem>,
    monedaActiva: String,
    profesionalesHabituales: List<MedicalPractitioner>,
    reportesSalud: List<MedicalReport>,
    citasMedicas: List<MedicalAppointment>,
    signosVitales: List<SignosVitales>,
    edadCalculadaPaciente: String,
    ultimoBackupAutomatico: java.io.File?,
    sugerencias: List<VademecumMedication>,
    signosVitalesSeleccionados: List<SignosVitales>,
    mesesExpandidosSignos: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    registrosSignosSeleccionados: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    opcionesFrecuenciaBackup: List<String>,
    opcionesHoraBackup: List<String>,
    opcionesReintentoCritico: List<Int>,
    opcionesIntentosCriticos: List<Int>,
    frecuenciaBackupSeleccionada: String,
    horaBackupSeleccionada: Int,
    minutoBackupSeleccionado: Int,
    expandedHoraBackup: Boolean,
    urlServicioIa: String,
    modeloServicioIa: String,
    recordatorioSignosActivo: Boolean,
    recordatorioSignosHora: Int,
    recordatorioSignosMinuto: Int,
    mostrarTimePickerSignos: Boolean,
    mostrarListadoSignosPanel: Boolean,
    mostrarListadoSignosGuardados: Boolean,
    exportandoTomasState: MutableState<Boolean>,
    periodoExportacionPendienteState: MutableState<IntakeExportPeriod?>,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    panelInternoScrollState: ScrollState,
    inventarioLazyRowState: LazyListState
) {
    // Derive values from state
    val mostrarFormulario = s.mostrarFormulario
    val mostrarFormularioInforme = s.mostrarFormularioInforme
    val mostrarListaInsumos = s.mostrarListaInsumos
    val mostrarPanelPacientes = s.mostrarPanelPacientes
    val mostrarPanelProfesionales = s.mostrarPanelProfesionales
    val mostrarPanelInformes = s.mostrarPanelInformes
    val mostrarFormularioProfesional = s.mostrarFormularioProfesional
    val mostrarPanelSignosVitales = s.mostrarPanelSignosVitales
    val mostrarPanelPodometro = s.mostrarPanelPodometro
    val mostrarPanelCicloMenstrual = s.mostrarPanelCicloMenstrual
    val mostrarPanelEmbarazo = s.mostrarPanelEmbarazo
    val mostrarPanelAnticonceptivos = s.mostrarPanelAnticonceptivos
    val mostrarPanelPediatrico = s.mostrarPanelPediatrico
    val mostrarPanelReporteClinico = s.mostrarPanelReporteClinico
    val mostrarPanelDiario = s.mostrarPanelDiario
    val mostrarPanelVerificadorTomas = s.mostrarPanelVerificadorTomas
    val mostrarPanelHidratacion = s.mostrarPanelHidratacion
    val mostrarPanelSedentarismo = s.mostrarPanelSedentarismo
    val mostrarPanelDentista = s.mostrarPanelDentista
    val mostrarPanelPedidos = s.mostrarPanelPedidos
    val mostrarPanelConfiguracionAlertas = s.mostrarPanelConfiguracionAlertas
    val mostrarPanelConfiguracionIa = s.mostrarPanelConfiguracionIa
    val mostrarPanelAsistenteIa = s.mostrarPanelAsistenteIa
    val mostrarPanelBackups = s.mostrarPanelBackups
    val mostrarFichaPaciente = s.mostrarFichaPaciente
    val mostrarDialogoCerrarInformeSinGuardar = s.mostrarDialogoCerrarInformeSinGuardarState.value
    val mostrarPanelCitasMedicas = s.mostrarPanelCitasMedicas
    val mostrarFormularioCitaMedica = s.mostrarFormularioCitaMedica
    val editingMedicationId = s.editingMedicationIdState.value
    val editingPractitionerId = s.editingPractitionerIdState.value
    val alarmaSonidoNombre = s.alarmaSonidoNombreState.value
    val sistolicaInput = s.sistolicaInputState.value
    val diastolicaInput = s.diastolicaInputState.value
    val comentarioPresionInput = s.comentarioPresionInputState.value
    val latidosInput = s.latidosInputState.value
    val comentarioLatidosInput = s.comentarioLatidosInputState.value
    val glucemiaInput = s.glucemiaInputState.value
    val comentarioGlucemiaInput = s.comentarioGlucemiaInputState.value
    val temperaturaInput = s.temperaturaInputState.value
    val comentarioTemperaturaInput = s.comentarioTemperaturaInputState.value
    val filtroExportacionSignos = s.filtroExportacionSignosState.value
    val fechaInicioExportacionSignos = s.fechaInicioExportacionSignosState.value
    val fechaFinExportacionSignos = s.fechaFinExportacionSignosState.value
    val expandedFiltroExportacionSignos = s.expandedFiltroExportacionSignosState.value
    val mostrarVistaPreviaSignosSeleccionados = s.mostrarVistaPreviaSignosSeleccionadosState.value
    val exportandoSignosVitales = s.exportandoSignosVitalesState.value
    val expandedFrecuenciaBackup = s.expandedFrecuenciaBackupState.value
    val mensajeBackup = s.mensajeBackupState.value
    val ejecutandoBackupManual = s.ejecutandoBackupManualState.value
    val restaurandoBackup = s.restaurandoBackupState.value
    val insumoSeleccionadoEnInventario = s.insumoSeleccionadoEnInventarioState.value
    val intervaloReintentoSeleccionado = s.intervaloReintentoSeleccionadoState.value
    val numeroIntentosCriticosSeleccionado = s.numeroIntentosCriticosSeleccionadoState.value
    val expandedReintentoCritico = s.expandedReintentoCriticoState.value
    val expandedIntentosCriticos = s.expandedIntentosCriticosState.value
    val alarmaSonidoUri = s.alarmaSonidoUriState.value
    val tienePermisoNotificaciones = s.tienePermisoNotificaciones
    val tienePermisoAlarmaExacta = s.tienePermisoAlarmaExacta
    val tienePermisoPantallaCompleta = s.tienePermisoPantallaCompleta
    val tieneAccesoNoMolestar = s.tieneAccesoNoMolestar
    val nombre = s.nombreState.value
    val cantidad = s.cantidadState.value
    val horaTomaSeleccionada = s.horaTomaSeleccionadaState.value
    val formatoSeleccionado = s.formatoSeleccionadoState.value
    val formaInsumoSeleccionada = s.formaInsumoSeleccionadaState.value
    val colorInsumoSeleccionado = s.colorInsumoSeleccionadoState.value
    val colorInsumo2Seleccionado = s.colorInsumo2SeleccionadoState.value
    val concentracionSeleccionada = s.concentracionSeleccionadaState.value
    val cicloSeleccionado = s.cicloSeleccionadoState.value
    val fechaInicio = s.fechaInicioState.value
    val fechaFin = s.fechaFinState.value
    val stockActual = s.stockActualState.value
    val stockMinimo = s.stockMinimoState.value
    val precioPorUnidad = s.precioPorUnidadState.value
    val telefonoPedidoWhatsapp = s.telefonoPedidoWhatsappState.value
    val presentacionPersistida = s.presentacionPersistidaState.value
    val esCicloCorto = s.esCicloCortoState.value
    val estaActivo = s.estaActivoState.value
    val alarmaActiva = s.alarmaActivaState.value
    val controlarExistencias = s.controlarExistenciasState.value
    val dispensacionGratuita = s.dispensacionGratuitaState.value
    val expandedNombre = s.expandedNombreState.value
    val expandedToma = s.expandedTomaState.value
    val expandedConcentracion = s.expandedConcentracionState.value
    val expandedCiclo = s.expandedCicloState.value
    val expandedOrigenReposicion = s.expandedOrigenReposicionState.value
    val horasTomas = s.horasTomas
    val selectedMedication = s.selectedMedicationState.value
    val tomaSeleccionada = s.tomaSeleccionadaState.value
    val origenReposicion = s.origenReposicionState.value
    val adjuntosPendientesReemplazo = s.adjuntosPendientesReemplazo
    val medicationToDelete = s.medicationToDeleteState.value
    val duplicateMedication = s.duplicateMedicationState.value
    val insumoARecargar = s.insumoARecargarState.value
    val insumoAPedir = s.insumoAPedirState.value
    val itemCarritoAConfirmar = s.itemCarritoAConfirmarState.value
    val itemCarritoAEliminar = s.itemCarritoAEliminarState.value
    val confirmarRecepcionTotal = s.confirmarRecepcionTotalState.value
    val pedidoAEliminar = s.pedidoAEliminarState.value
    val pedidoAEditar = s.pedidoAEditarState.value
    val tomaPendienteDeEliminar = s.tomaPendienteDeEliminarState.value
    val inputRecargarStock = s.inputRecargarStockState.value
    val inputUnidadesPedido = s.inputUnidadesPedidoState.value
    val inputUnidadesRecibidas = s.inputUnidadesRecibidasState.value
    val inputPrecioActualizado = s.inputPrecioActualizadoState.value
    val inputEditarResumen = s.inputEditarResumenState.value
    val inputEditarTotal = s.inputEditarTotalState.value
    val estudiosAdjuntos = s.estudiosAdjuntos
    val visorAdjuntos = s.visorAdjuntosState.value
    val nombreProfesional = s.nombreProfesionalState.value
    val especialidadProfesional = s.especialidadProfesionalState.value

    // Derive callbacks
    val onCerrarPanelesSecundarios = callbacks.onCerrarPanelesSecundarios
    val onAbrirNuevaFichaPaciente = callbacks.onAbrirNuevaFichaPaciente
    val onAbrirFichaPaciente = callbacks.onAbrirFichaPaciente
    val onCargarFichaPaciente = callbacks.onCargarFichaPaciente
    val onVolverPantallaAnteriorDesdeFichaPaciente = callbacks.onVolverPantallaAnteriorDesdeFichaPaciente
    val onResetForm = callbacks.onResetForm
    val onResetFichaPaciente = callbacks.onResetFichaPaciente
    val onCargarMedicamentoEnFormulario = callbacks.onCargarMedicamentoEnFormulario
    val onGuardarInformeMedicoActual = callbacks.onGuardarInformeMedicoActual
    val onCerrarFormularioInforme = callbacks.onCerrarFormularioInforme
    val onDeleteAttachmentFile = callbacks.onDeleteAttachmentFile
    val onGuardarConfiguracionAlertasCriticas = callbacks.onGuardarConfiguracionAlertasCriticas
    val onGuardarConfiguracionIa = callbacks.onGuardarConfiguracionIa
    val onTimestampArchivo = callbacks.onTimestampArchivo
    val onCalcularEdadDesdeNacimiento = callbacks.onCalcularEdadDesdeNacimiento
    val onSavePersistedBirthday = callbacks.onSavePersistedBirthday
    val onLoadPersistedBirthday = callbacks.onLoadPersistedBirthday
    val onClearPersistedBirthday = callbacks.onClearPersistedBirthday
    val onRequestBirthdayPreview = callbacks.onRequestBirthdayPreview
    val onResetInformeMedico = callbacks.onResetInformeMedico
    val onCargarInformeMedico = callbacks.onCargarInformeMedico
    val onGuardarMedicoHabitualActual = callbacks.onGuardarMedicoHabitualActual
    val onResetMedicoHabitual = callbacks.onResetMedicoHabitual
    val onAbrirFormularioCitaMedica = callbacks.onAbrirFormularioCitaMedica
    val onFormatDate = callbacks.onFormatDate
    val onFormatReminderMinutesLabel = callbacks.onFormatReminderMinutesLabel

    // Derive state-change lambdas
    val onMostrarFichaPacienteChange: (Boolean) -> Unit = { s.mostrarFichaPacienteState.value = it }
    val onMostrarFormularioChange: (Boolean) -> Unit = { s.mostrarFormularioState.value = it }
    val onMostrarListaInsumosChange: (Boolean) -> Unit = { s.mostrarListaInsumosState.value = it }
    val onMostrarPanelPacientesChange: (Boolean) -> Unit = { s.mostrarPanelPacientesState.value = it }
    val onMostrarPanelProfesionalesChange: (Boolean) -> Unit = { s.mostrarPanelProfesionalesState.value = it }
    val onMostrarPanelInformesChange: (Boolean) -> Unit = { s.mostrarPanelInformesState.value = it }
    val onMostrarFormularioProfesionalChange: (Boolean) -> Unit = { s.mostrarFormularioProfesionalState.value = it }
    val onMostrarPanelPedidosChange: (Boolean) -> Unit = { s.mostrarPanelPedidosState.value = it }
    val onMostrarPanelBackupsChange: (Boolean) -> Unit = { s.mostrarPanelBackupsState.value = it }
    val onMostrarFormularioInformeChange: (Boolean) -> Unit = { s.mostrarFormularioInformeState.value = it }
    val onMostrarListadoSignosPanelChange: (Boolean) -> Unit = { /* handled externally */ }
    val onMostrarPanelSignosVitalesChange: (Boolean) -> Unit = { s.mostrarPanelSignosVitalesState.value = it }
    val onMostrarPanelEmbarazoChange: (Boolean) -> Unit = { s.mostrarPanelEmbarazoState.value = it }
    val onMostrarPanelPediatricoChange: (Boolean) -> Unit = { s.mostrarPanelPediatricoState.value = it }
    val onMostrarPanelCitasMedicasChange: (Boolean) -> Unit = { s.mostrarPanelCitasMedicasState.value = it }
    val onMostrarFormularioCitaMedicaChange: (Boolean) -> Unit = { s.mostrarFormularioCitaMedicaState.value = it }
    val onMensajeBackupChange: (String) -> Unit = { s.mensajeBackupState.value = it }
    val onSistolicaChange: (String) -> Unit = { s.sistolicaInputState.value = it }
    val onDiastolicaChange: (String) -> Unit = { s.diastolicaInputState.value = it }
    val onComentarioPresionChange: (String) -> Unit = { s.comentarioPresionInputState.value = it }
    val onLatidosChange: (String) -> Unit = { s.latidosInputState.value = it }
    val onComentarioLatidosChange: (String) -> Unit = { s.comentarioLatidosInputState.value = it }
    val onGlucemiaChange: (String) -> Unit = { s.glucemiaInputState.value = it }
    val onComentarioGlucemiaChange: (String) -> Unit = { s.comentarioGlucemiaInputState.value = it }
    val onTemperaturaChange: (String) -> Unit = { s.temperaturaInputState.value = it }
    val onComentarioTemperaturaChange: (String) -> Unit = { s.comentarioTemperaturaInputState.value = it }
    val onPesoInputChange: (String) -> Unit = { s.pesoInputState.value = it }
    val onRecordatorioSignosActivoChange = callbacks.onRecordatorioSignosActivoChange
    val onRecordatorioSignosHoraChange = callbacks.onRecordatorioSignosHoraChange
    val onRecordatorioSignosMinutoChange = callbacks.onRecordatorioSignosMinutoChange
    val onMostrarTimePickerSignosChange = callbacks.onMostrarTimePickerSignosChange
    val onPesoPacienteChange: (String) -> Unit = { s.pesoPacienteState.value = it }
    val onPesoUnidadPacienteChange: (String) -> Unit = { s.pesoUnidadPacienteState.value = it }
    val onFiltroExportacionSignosChange: (VitalSignsExportFilter) -> Unit = { s.filtroExportacionSignosState.value = it }
    val onFechaInicioExportacionSignosChange: (Long) -> Unit = { s.fechaInicioExportacionSignosState.value = it }
    val onFechaFinExportacionSignosChange: (Long) -> Unit = { s.fechaFinExportacionSignosState.value = it }
    val onExpandedFiltroExportacionSignosChange: (Boolean) -> Unit = { s.expandedFiltroExportacionSignosState.value = it }
    val onMostrarVistaPreviaSignosSeleccionadosChange: (Boolean) -> Unit = { s.mostrarVistaPreviaSignosSeleccionadosState.value = it }
    val onExportandoSignosVitalesChange: (Boolean) -> Unit = { s.exportandoSignosVitalesState.value = it }
    val onExportacionSignosPendienteChange: (VitalSignsExportRequest?) -> Unit = { /* handled via launcher */ }
    val onMostrarListadoSignosGuardadosChange: (Boolean) -> Unit = { /* handled externally */ }
    val onNombreChange: (String) -> Unit = { s.nombreState.value = it }
    val onCantidadChange: (String) -> Unit = { s.cantidadState.value = it }
    val onHoraTomaChange: (String) -> Unit = { s.horaTomaSeleccionadaState.value = it }
    val onFormaChange: (String) -> Unit = { s.formaInsumoSeleccionadaState.value = it }
    val onColorChange: (Color) -> Unit = { s.colorInsumoSeleccionadoState.value = it }
    val onColor2Change: (Color) -> Unit = { s.colorInsumo2SeleccionadoState.value = it }
    val onConcentracionChange: (String) -> Unit = { s.concentracionSeleccionadaState.value = it }
    val onCicloChange: (String) -> Unit = { s.cicloSeleccionadoState.value = it }
    val onFechaInicioChange: (Long) -> Unit = { s.fechaInicioState.value = it }
    val onFechaFinChange: (Long) -> Unit = { s.fechaFinState.value = it }
    val onStockActualChange: (String) -> Unit = { s.stockActualState.value = it }
    val onStockMinimoChange: (String) -> Unit = { s.stockMinimoState.value = it }
    val onPrecioChange: (String) -> Unit = { s.precioPorUnidadState.value = it }
    val onTelefonoChange: (String) -> Unit = { s.telefonoPedidoWhatsappState.value = it }
    val onEsCicloCortoChange: (Boolean) -> Unit = { s.esCicloCortoState.value = it }
    val onEstaActivoChange: (Boolean) -> Unit = { s.estaActivoState.value = it }
    val onAlarmaActivaChange: (Boolean) -> Unit = { s.alarmaActivaState.value = it }
    val onControlarExistenciasChange: (Boolean) -> Unit = { s.controlarExistenciasState.value = it }
    val onDispensacionGratuitaChange: (Boolean) -> Unit = { s.dispensacionGratuitaState.value = it }
    val onExpandedNombreChange: (Boolean) -> Unit = { s.expandedNombreState.value = it }
    val onExpandedTomaChange: (Boolean) -> Unit = { s.expandedTomaState.value = it }
    val onExpandedConcentracionChange: (Boolean) -> Unit = { s.expandedConcentracionState.value = it }
    val onExpandedCicloChange: (Boolean) -> Unit = { s.expandedCicloState.value = it }
    val onExpandedOrigenReposicionChange: (Boolean) -> Unit = { s.expandedOrigenReposicionState.value = it }
    val onSelectedMedicationChange: (VademecumMedication?) -> Unit = { s.selectedMedicationState.value = it }
    val onTomaSeleccionadaChange: (String) -> Unit = { s.tomaSeleccionadaState.value = it }
    val onOrigenReposicionChange: (String) -> Unit = { s.origenReposicionState.value = it }
    val onMostrarDialogoCerrarInformeSinGuardarChange: (Boolean) -> Unit = { s.mostrarDialogoCerrarInformeSinGuardarState.value = it }
    val onMedicationToDeleteChange: (Medication?) -> Unit = { s.medicationToDeleteState.value = it }
    val onDuplicateMedicationChange: (Medication?) -> Unit = { s.duplicateMedicationState.value = it }
    val onInsumoARecargarChange: (Medication?) -> Unit = { s.insumoARecargarState.value = it }
    val onInsumoAPedirChange: (Medication?) -> Unit = { s.insumoAPedirState.value = it }
    val onItemCarritoAConfirmarChange: (CarritoItem?) -> Unit = { s.itemCarritoAConfirmarState.value = it }
    val onItemCarritoAEliminarChange: (CarritoItem?) -> Unit = { s.itemCarritoAEliminarState.value = it }
    val onConfirmarRecepcionTotalChange: (Boolean) -> Unit = { s.confirmarRecepcionTotalState.value = it }
    val onPedidoAEliminarChange: (MedicationOrder?) -> Unit = { s.pedidoAEliminarState.value = it }
    val onPedidoAEditarChange: (MedicationOrder?) -> Unit = { s.pedidoAEditarState.value = it }
    val onTomaPendienteDeEliminarChange: (IntakeRemovalConfirmation?) -> Unit = { s.tomaPendienteDeEliminarState.value = it }
    val onInputRecargarStockChange: (String) -> Unit = { s.inputRecargarStockState.value = it }
    val onInputUnidadesPedidoChange: (String) -> Unit = { s.inputUnidadesPedidoState.value = it }
    val onInputUnidadesRecibidasChange: (String) -> Unit = { s.inputUnidadesRecibidasState.value = it }
    val onInputPrecioActualizadoChange: (String) -> Unit = { s.inputPrecioActualizadoState.value = it }
    val onInputEditarResumenChange: (String) -> Unit = { s.inputEditarResumenState.value = it }
    val onInputEditarTotalChange: (String) -> Unit = { s.inputEditarTotalState.value = it }
    val onVisorAdjuntosChange: (AttachmentViewerState?) -> Unit = { s.visorAdjuntosState.value = it }
    val onFrecuenciaBackupSeleccionadaChange = callbacks.onFrecuenciaBackupChange
    val onExpandedFrecuenciaBackupChange: (Boolean) -> Unit = { s.expandedFrecuenciaBackupState.value = it }
    val onExpandedHoraBackupChange = callbacks.onExpandedHoraBackupChange
    val onHoraBackupSeleccionadaChange = callbacks.onHoraBackupChange
    val onMinutoBackupSeleccionadoChange = callbacks.onMinutoBackupChange
    val onIntervaloReintentoSeleccionadoChange: (Int) -> Unit = { s.intervaloReintentoSeleccionadoState.value = it }
    val onNumeroIntentosCriticosSeleccionadoChange: (Int) -> Unit = { s.numeroIntentosCriticosSeleccionadoState.value = it }
    val onExpandedReintentoCriticoChange: (Boolean) -> Unit = { s.expandedReintentoCriticoState.value = it }
    val onExpandedIntentosCriticosChange: (Boolean) -> Unit = { s.expandedIntentosCriticosState.value = it }
    val onAlarmaSonidoUriChange: (String) -> Unit = { s.alarmaSonidoUriState.value = it }
    val onAlarmaSonidoNombreChange: (String) -> Unit = { s.alarmaSonidoNombreState.value = it }
    val onUrlServicioIaChange = callbacks.onUrlServicioIaChange
    val onModeloServicioIaChange = callbacks.onModeloServicioIaChange
    val onInsumoSeleccionadoEnInventarioChange: (Int?) -> Unit = { s.insumoSeleccionadoEnInventarioState.value = it }
    val onMostrarDialogoBackupManualChange: (Boolean) -> Unit = { s.mostrarDialogoBackupManualState.value = it }
    val onMostrarDialogoRestoreSeleccionChange: (Boolean) -> Unit = { s.mostrarDialogoRestoreSeleccionState.value = it }
    val onMostrarDialogoProgramarBackupChange: (Boolean) -> Unit = { s.mostrarDialogoProgramarBackupState.value = it }
    val onLanzarCreateBackup: (String) -> Unit = { launchers.createBackupDocumentLauncher.launch(it) }
    val onLanzarRestoreBackup: () -> Unit = { launchers.restoreBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain")) }
    val onLanzarExportVitalSigns: (String) -> Unit = { launchers.exportVitalSignsReportLauncher.launch(it) }
    val onNombreProfesionalChange: (String) -> Unit = { s.nombreProfesionalState.value = it }
    val onEspecialidadProfesionalChange: (String) -> Unit = { s.especialidadProfesionalState.value = it }
    val onProfesionalSeleccionadoIdChange: (Int?) -> Unit = { s.profesionalSeleccionadoIdState.value = it }

    // Launchers
    val notificationPermissionLauncher = launchers.notificationPermissionLauncher
    val exactAlarmPermissionLauncher = launchers.exactAlarmPermissionLauncher
    val fullScreenIntentPermissionLauncher = launchers.fullScreenIntentPermissionLauncher
    val notificationPolicyAccessLauncher = launchers.notificationPolicyAccessLauncher
    val ringtonePickerLauncher = launchers.ringtonePickerLauncher
    val pickFotoPerfilLauncher = launchers.pickFotoPerfilLauncher
    val takeFotoPerfilLauncher = launchers.takeFotoPerfilLauncher
    val cameraPermissionPerfilLauncher = launchers.cameraPermissionPerfilLauncher
    val exportMedicationReportLauncher = launchers.exportMedicationReportLauncher
    val exportVitalSignsReportLauncher = launchers.exportVitalSignsReportLauncher
    val createBackupDocumentLauncher = launchers.createBackupDocumentLauncher
    val restoreBackupDocumentLauncher = launchers.restoreBackupDocumentLauncher

    // MutableState refs from s
    val editingPatientIdState = s.editingPatientIdState
    val perfilPendienteDeEliminarState = s.perfilPendienteDeEliminarState
    val mostrarDialogoBackupManualState = s.mostrarDialogoBackupManualState
    val mostrarDialogoRestoreSeleccionState = s.mostrarDialogoRestoreSeleccionState
    val mostrarDialogoProgramarBackupState = s.mostrarDialogoProgramarBackupState
    val backupSelectionState = s.backupSelectionState
    val restoreSelectionState = s.restoreSelectionState
    val backupPatientIdState = s.backupPatientIdState
    val restorePatientIdState = s.restorePatientIdState
    val ejecutandoBackupManualState = s.ejecutandoBackupManualState
    val restaurandoBackupState = s.restaurandoBackupState
    val editandoFichaPacienteState = s.editandoFichaPacienteState
    val nombrePacienteState = s.nombrePacienteState
    val apellidosPacienteState = s.apellidosPacienteState
    val fechaNacimientoPacienteState = s.fechaNacimientoPacienteState
    val edadPacienteState = s.edadPacienteState
    val pesoPacienteState = s.pesoPacienteState
    val pesoUnidadPacienteState = s.pesoUnidadPacienteState
    val estaturaPacienteState = s.estaturaPacienteState
    val estaturaUnidadPacienteState = s.estaturaUnidadPacienteState
    val sexoPacienteState = s.sexoPacienteState
    val paisPacienteState = s.paisPacienteState
    val monedaPacienteState = s.monedaPacienteState
    val enfermedadesPacienteState = s.enfermedadesPacienteState
    val prescripcionesPacienteState = s.prescripcionesPacienteState
    val fotoPerfilPacienteState = s.fotoPerfilPacienteState
    val cameraPermissionPerfilPendingState = s.cameraPermissionPerfilPendingState
    val expandedPesoUnidadState = s.expandedPesoUnidadState
    val expandedEstaturaUnidadState = s.expandedEstaturaUnidadState
    val expandedPaisPacienteState = s.expandedPaisPacienteState
    val pesoInputState = s.pesoInputState
    val pesoUnidadKgState = s.pesoUnidadKgState
    val mostrarDialogoPesoSincronizadoState = s.mostrarDialogoPesoSincronizadoState
    val reportePendienteDeEliminarState = s.reportePendienteDeEliminarState
    val citaPendienteDeEliminarState = s.citaPendienteDeEliminarState
    val citaMedicaSeleccionadaIdState = s.citaMedicaSeleccionadaIdState
    val filtroProfesionalInformesIdState = s.filtroProfesionalInformesIdState
    val expandedFiltroProfesionalInformesState = s.expandedFiltroProfesionalInformesState
    val visorAdjuntosState = s.visorAdjuntosState

    val pedidosPacienteFlow: () -> Flow<List<MedicationOrder>> = { if (pacienteActivo == null) flowOf(emptyList()) else database.medicationOrderDao().observarPorPaciente(pacienteActivo.id) }
    val pedidosPaciente by pedidosPacienteFlow().collectAsState(initial = emptyList())

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        PanelPacientesContent(
            mostrarPanelPacientes = mostrarPanelPacientes,
            perfilesPacientes = perfilesPacientes,
            pacienteActivo = pacienteActivo,
            perfilPendienteDeEliminarState = perfilPendienteDeEliminarState,
            database = database,
            coroutineScope = coroutineScope,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
            onAbrirNuevaFichaPaciente = onAbrirNuevaFichaPaciente,
            onAbrirFichaPaciente = onAbrirFichaPaciente,
            onMostrarFormularioInformeChange = onMostrarFormularioInformeChange
        )

        FichaPacientePanel(
            mostrarFichaPaciente = mostrarFichaPaciente,
            editandoFichaPacienteState = editandoFichaPacienteState,
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
            expandedPesoUnidadState = expandedPesoUnidadState,
            expandedEstaturaUnidadState = expandedEstaturaUnidadState,
            expandedPaisPacienteState = expandedPaisPacienteState,
            pesoInputState = pesoInputState,
            pesoUnidadKgState = pesoUnidadKgState,
            mostrarDialogoPesoSincronizadoState = mostrarDialogoPesoSincronizadoState,
            perfilPendienteDeEliminarState = perfilPendienteDeEliminarState,
            editingPatientId = editingPatientIdState.value,
            pacienteActivo = pacienteActivo,
            edadCalculadaPaciente = edadCalculadaPaciente,
            database = database,
            coroutineScope = coroutineScope,
            pickFotoPerfilLauncher = pickFotoPerfilLauncher,
            takeFotoPerfilLauncher = takeFotoPerfilLauncher,
            cameraPermissionPerfilLauncher = cameraPermissionPerfilLauncher,
            onCargarFichaPaciente = onCargarFichaPaciente,
            onVolverPantallaAnteriorDesdeFichaPaciente = onVolverPantallaAnteriorDesdeFichaPaciente,
            onSavePersistedBirthday = onSavePersistedBirthday,
            onCalcularEdadDesdeNacimiento = onCalcularEdadDesdeNacimiento,
            onLoadPersistedBirthday = onLoadPersistedBirthday,
            onClearPersistedBirthday = onClearPersistedBirthday,
            onRequestBirthdayPreview = onRequestBirthdayPreview,
            profesionalesHabituales = profesionalesHabituales,
            reportesSalud = reportesSalud,
            visorAdjuntosState = visorAdjuntosState,
            exportandoTomasState = exportandoTomasState,
            periodoExportacionPendienteState = periodoExportacionPendienteState,
            reportePendienteDeEliminarState = reportePendienteDeEliminarState,
            fechaActualTexto = onFormatDate(System.currentTimeMillis()),
            exportMedicationReportLauncher = exportMedicationReportLauncher,
            onAbrirNuevaFichaPaciente = onAbrirNuevaFichaPaciente,
            onResetInformeMedico = onResetInformeMedico,
            onMostrarPanelInformesChange = onMostrarPanelInformesChange,
            onMostrarFormularioInformeChange = onMostrarFormularioInformeChange,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
            onCargarInformeMedico = onCargarInformeMedico,
            mostrarPanelCitasMedicas = mostrarPanelCitasMedicas,
            mostrarFormularioCitaMedica = mostrarFormularioCitaMedica,
            mostrarPanelInformes = mostrarPanelInformes,
            mostrarFormularioInforme = mostrarFormularioInforme,
            citasMedicas = citasMedicas,
            citaMedicaSeleccionadaId = citaMedicaSeleccionadaIdState.value,
            citaMedicaSeleccionada = citasMedicas.firstOrNull { it.id == citaMedicaSeleccionadaIdState.value },
            citaPendienteDeEliminarState = citaPendienteDeEliminarState,
            alarmaSonidoNombre = alarmaSonidoNombre,
            filtroProfesionalInformesIdState = filtroProfesionalInformesIdState,
            expandedFiltroProfesionalInformesState = expandedFiltroProfesionalInformesState,
            citaMedicaSeleccionadaIdState = citaMedicaSeleccionadaIdState,
            onAbrirFormularioCitaMedica = onAbrirFormularioCitaMedica,
            panelInternoScrollState = panelInternoScrollState,
            onFormatReminderMinutesLabel = onFormatReminderMinutesLabel,
            editingPatientIdState = editingPatientIdState,
            onFormatDate = onFormatDate
        )

        PanelProfesionalesPanel(
            mostrarPanelProfesionales = mostrarPanelProfesionales,
            pacienteActivo = pacienteActivo,
            profesionalSeleccionadoId = null,
            medicoSeleccionado = null,
            profesionalesHabituales = profesionalesHabituales,
            citasMedicas = citasMedicas,
            reportesSalud = reportesSalud,
            database = database,
            coroutineScope = coroutineScope,
            panelInternoScrollState = panelInternoScrollState,
            onProfesionalSeleccionadoIdChange = onProfesionalSeleccionadoIdChange,
            onMostrarPanelProfesionalesChange = onMostrarPanelProfesionalesChange,
            onMostrarFormularioProfesionalChange = onMostrarFormularioProfesionalChange,
            onMostrarFormularioChange = onMostrarFormularioChange,
            onMostrarPanelInformesChange = onMostrarPanelInformesChange,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
            onAbrirFormularioMedico = { practitioner ->
                if (practitioner != null) {
                    onNombreProfesionalChange(practitioner.name)
                    onEspecialidadProfesionalChange(practitioner.specialty)
                } else {
                    onNombreProfesionalChange("")
                    onEspecialidadProfesionalChange("")
                }
                onMostrarFormularioProfesionalChange(true)
                onMostrarPanelProfesionalesChange(false)
            }
        )

        FormularioProfesionalPanel(
            mostrarFormularioProfesional = mostrarFormularioProfesional,
            editingPractitionerId = editingPractitionerId,
            nombreProfesional = nombreProfesional,
            especialidadProfesional = especialidadProfesional,
            proximaCitaMedico = null,
            informesSincronizadosMedico = emptyList(),
            panelInternoScrollState = panelInternoScrollState,
            onNombreProfesionalChange = onNombreProfesionalChange,
            onEspecialidadProfesionalChange = onEspecialidadProfesionalChange,
            onGuardarMedicoHabitualActual = onGuardarMedicoHabitualActual,
            onResetMedicoHabitual = onResetMedicoHabitual,
            onMostrarFormularioProfesionalChange = onMostrarFormularioProfesionalChange,
            onMostrarPanelProfesionalesChange = onMostrarPanelProfesionalesChange
        )

        if (mostrarPanelSignosVitales) {
            PanelSignosVitalesContent(
                sistolicaInput = sistolicaInput, onSistolicaChange = onSistolicaChange,
                diastolicaInput = diastolicaInput, onDiastolicaChange = onDiastolicaChange,
                comentarioPresionInput = comentarioPresionInput, onComentarioPresionChange = onComentarioPresionChange,
                latidosInput = latidosInput, onLatidosChange = onLatidosChange,
                comentarioLatidosInput = comentarioLatidosInput, onComentarioLatidosChange = onComentarioLatidosChange,
                glucemiaInput = glucemiaInput, onGlucemiaChange = onGlucemiaChange,
                comentarioGlucemiaInput = comentarioGlucemiaInput, onComentarioGlucemiaChange = onComentarioGlucemiaChange,
                temperaturaInput = temperaturaInput, onTemperaturaChange = onTemperaturaChange,
                comentarioTemperaturaInput = comentarioTemperaturaInput, onComentarioTemperaturaChange = onComentarioTemperaturaChange,
                pesoInput = pesoInputState.value, onPesoChange = onPesoInputChange,
                pesoUnidadKg = pesoUnidadKgState.value, onPesoUnidadToggle = { pesoUnidadKgState.value = !pesoUnidadKgState.value },
                estaturaPaciente = estaturaPacienteState.value,
                estaturaUnidadPaciente = estaturaUnidadPacienteState.value,
                pacienteActivo = pacienteActivo,
                database = database,
                coroutineScope = coroutineScope,
                recordatorioSignosActivo = recordatorioSignosActivo,
                recordatorioSignosHora = recordatorioSignosHora,
                recordatorioSignosMinuto = recordatorioSignosMinuto,
                mostrarTimePickerSignos = mostrarTimePickerSignos,
                onRecordatorioActivoChange = onRecordatorioSignosActivoChange,
                onRecordatorioHoraChange = onRecordatorioSignosHoraChange,
                onRecordatorioMinutoChange = onRecordatorioSignosMinutoChange,
                onMostrarTimePickerChange = onMostrarTimePickerSignosChange,
                onPesoPacienteChange = onPesoPacienteChange,
                onPesoUnidadPacienteChange = onPesoUnidadPacienteChange,
                onSistolicaInputClear = { onSistolicaChange("") },
                onDiastolicaInputClear = { onDiastolicaChange("") },
                onComentarioPresionClear = { onComentarioPresionChange("") },
                onLatidosInputClear = { onLatidosChange("") },
                onComentarioLatidosClear = { onComentarioLatidosChange("") },
                onGlucemiaInputClear = { onGlucemiaChange("") },
                onComentarioGlucemiaClear = { onComentarioGlucemiaChange("") },
                onTemperaturaInputClear = { onTemperaturaChange("") },
                onComentarioTemperaturaClear = { onComentarioTemperaturaChange("") },
                onPesoInputClear = { onPesoInputChange("") },
                onVerListado = { onMostrarPanelSignosVitalesChange(false); onMostrarListadoSignosPanelChange(true) },
                onVolver = onCerrarPanelesSecundarios
            )
        }

        if (mostrarPanelPodometro) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                PodometroScreen(pacienteId = pacienteActivo?.id ?: 0, database = database, onVolver = onCerrarPanelesSecundarios)
            }
        }
        if (mostrarPanelCicloMenstrual) {
            CicloMenstrualScreen(pacienteId = pacienteActivo?.id ?: 0, database = database, onVolver = onCerrarPanelesSecundarios, onIrAEmbarazo = { onCerrarPanelesSecundarios(); onMostrarPanelEmbarazoChange(true) })
        }
        if (mostrarPanelEmbarazo) {
            ControlEmbarazoScreen(
                pacienteId = pacienteActivo?.id ?: 0,
                nombrePaciente = "${pacienteActivo?.nombre.orEmpty()} ${pacienteActivo?.apellidos.orEmpty()}".trim(),
                pesoPaciente = pacienteActivo?.peso.orEmpty(),
                pesoUnidadPaciente = pacienteActivo?.pesoUnidad.orEmpty(),
                estaturaPaciente = pacienteActivo?.estatura.orEmpty(),
                estaturaUnidadPaciente = pacienteActivo?.estaturaUnidad.orEmpty(),
                database = database, onVolver = onCerrarPanelesSecundarios,
                onIrAPediatrico = { onCerrarPanelesSecundarios(); onMostrarPanelPediatricoChange(true) }
            )
        }
        if (mostrarPanelAnticonceptivos) {
            AnticonceptivosScreen(pacienteId = pacienteActivo?.id ?: 0, database = database, onVolver = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelPediatrico) {
            ControlPediatricoMainScreen(pacienteId = pacienteActivo?.id ?: 0, database = database, onVolver = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelReporteClinico) {
            ReporteClinicoScreen(pacienteActivo = pacienteActivo, database = database, onVolver = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelDiario) {
            DiarioScreen(patientId = pacienteActivo?.id ?: 0, database = database, onVolver = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelVerificadorTomas) {
            VerificadorTomasPasadasScreen(database = database, onVolver = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelHidratacion) {
            HidratacionScreen(patientId = pacienteActivo?.id ?: 0, onBack = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelSedentarismo) {
            SedentarismoScreen(patientId = pacienteActivo?.id ?: 0, onBack = onCerrarPanelesSecundarios)
        }
        if (mostrarPanelDentista) {
            DentistaScreen(patientId = pacienteActivo?.id ?: 0, onBack = onCerrarPanelesSecundarios)
        }

        PanelPedidosPanel(
            mostrarPanelPedidos = mostrarPanelPedidos,
            pedidosPaciente = pedidosPaciente,
            carritoItems = carritoItems,
            monedaActiva = monedaActiva,
            pacienteActivoId = pacienteActivo?.id,
            database = database,
            coroutineScope = coroutineScope,
            inputUnidadesRecibidas = inputUnidadesRecibidas,
            inputPrecioActualizado = inputPrecioActualizado,
            itemCarritoAConfirmar = itemCarritoAConfirmar,
            itemCarritoAEliminar = itemCarritoAEliminar,
            confirmarRecepcionTotal = confirmarRecepcionTotal,
            pedidoAEditar = pedidoAEditar,
            inputEditarResumen = inputEditarResumen,
            inputEditarTotal = inputEditarTotal,
            pedidoAEliminar = pedidoAEliminar,
            panelInternoScrollState = panelInternoScrollState,
            onInputUnidadesRecibidasChange = onInputUnidadesRecibidasChange,
            onInputPrecioActualizadoChange = onInputPrecioActualizadoChange,
            onItemCarritoAConfirmarChange = onItemCarritoAConfirmarChange,
            onItemCarritoAEliminarChange = onItemCarritoAEliminarChange,
            onConfirmarRecepcionTotalChange = onConfirmarRecepcionTotalChange,
            onMostrarPanelPedidosChange = onMostrarPanelPedidosChange,
            onMostrarListaInsumosChange = onMostrarListaInsumosChange,
            onPedidoAEditarChange = onPedidoAEditarChange,
            onInputEditarResumenChange = onInputEditarResumenChange,
            onInputEditarTotalChange = onInputEditarTotalChange,
            onPedidoAEliminarChange = onPedidoAEliminarChange,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios
        )

        ConfiguracionAlertasPanel(
            mostrarPanelConfiguracionAlertas = mostrarPanelConfiguracionAlertas,
            intervaloReintentoSeleccionado = intervaloReintentoSeleccionado,
            numeroIntentosCriticosSeleccionado = numeroIntentosCriticosSeleccionado,
            expandedReintentoCritico = expandedReintentoCritico,
            expandedIntentosCriticos = expandedIntentosCriticos,
            alarmaSonidoUri = alarmaSonidoUri,
            alarmaSonidoNombre = alarmaSonidoNombre,
            opcionesReintentoCritico = opcionesReintentoCritico,
            opcionesIntentosCriticos = opcionesIntentosCriticos,
            tienePermisoNotificaciones = tienePermisoNotificaciones,
            tienePermisoAlarmaExacta = tienePermisoAlarmaExacta,
            tienePermisoPantallaCompleta = tienePermisoPantallaCompleta,
            tieneAccesoNoMolestar = tieneAccesoNoMolestar,
            notificationPermissionLauncher = notificationPermissionLauncher,
            exactAlarmPermissionLauncher = exactAlarmPermissionLauncher,
            fullScreenIntentPermissionLauncher = fullScreenIntentPermissionLauncher,
            notificationPolicyAccessLauncher = notificationPolicyAccessLauncher,
            ringtonePickerLauncher = ringtonePickerLauncher,
            panelInternoScrollState = panelInternoScrollState,
            onIntervaloReintentoSeleccionadoChange = onIntervaloReintentoSeleccionadoChange,
            onNumeroIntentosCriticosSeleccionadoChange = onNumeroIntentosCriticosSeleccionadoChange,
            onExpandedReintentoCriticoChange = onExpandedReintentoCriticoChange,
            onExpandedIntentosCriticosChange = onExpandedIntentosCriticosChange,
            onAlarmaSonidoUriChange = onAlarmaSonidoUriChange,
            onAlarmaSonidoNombreChange = onAlarmaSonidoNombreChange,
            onGuardarConfiguracionAlertasCriticas = onGuardarConfiguracionAlertasCriticas,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios
        )

        PanelConfiguracionIaPanel(
            mostrarPanelConfiguracionIa = mostrarPanelConfiguracionIa,
            urlServicioIa = urlServicioIa,
            modeloServicioIa = modeloServicioIa,
            onUrlServicioIaChange = onUrlServicioIaChange,
            onModeloServicioIaChange = onModeloServicioIaChange,
            onGuardarConfiguracionIa = onGuardarConfiguracionIa,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios
        )

        PanelAsistenteIaPanel(
            mostrarPanelAsistenteIa = mostrarPanelAsistenteIa,
            perfilActivoNombre = pacienteActivo?.let { p -> "${p.nombre} ${p.apellidos}".trim() }.takeIf { it?.isNotEmpty() == true },
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios
        )

        PanelBackupsPanel(
            mostrarPanelBackups = mostrarPanelBackups,
            frecuenciaBackupSeleccionada = frecuenciaBackupSeleccionada,
            expandedFrecuenciaBackup = expandedFrecuenciaBackup,
            expandedHoraBackup = expandedHoraBackup,
            horaBackupSeleccionada = horaBackupSeleccionada,
            minutoBackupSeleccionado = minutoBackupSeleccionado,
            ultimoBackupAutomatico = ultimoBackupAutomatico,
            mensajeBackup = mensajeBackup,
            opcionesFrecuenciaBackup = opcionesFrecuenciaBackup,
            opcionesHoraBackup = opcionesHoraBackup,
            ejecutandoBackupManual = ejecutandoBackupManual,
            restaurandoBackup = restaurandoBackup,
            panelInternoScrollState = panelInternoScrollState,
            onFrecuenciaBackupSeleccionadaChange = onFrecuenciaBackupSeleccionadaChange,
            onExpandedFrecuenciaBackupChange = onExpandedFrecuenciaBackupChange,
            onExpandedHoraBackupChange = onExpandedHoraBackupChange,
            onHoraBackupSeleccionadaChange = onHoraBackupSeleccionadaChange,
            onMinutoBackupSeleccionadoChange = onMinutoBackupSeleccionadoChange,
            onMostrarDialogoProgramarBackupChange = onMostrarDialogoProgramarBackupChange,
            onMostrarDialogoBackupManualChange = onMostrarDialogoBackupManualChange,
            onMostrarDialogoRestoreSeleccionChange = onMostrarDialogoRestoreSeleccionChange,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios
        )

        DialogosOperacionesPanel(
            adjuntosPendientesReemplazo = adjuntosPendientesReemplazo,
            mostrarDialogoCerrarInformeSinGuardar = mostrarDialogoCerrarInformeSinGuardar,
            medicationToDelete = medicationToDelete,
            duplicateMedication = duplicateMedication,
            insumoARecargar = insumoARecargar,
            insumoAPedir = insumoAPedir,
            itemCarritoAConfirmar = itemCarritoAConfirmar,
            itemCarritoAEliminar = itemCarritoAEliminar,
            confirmarRecepcionTotal = confirmarRecepcionTotal,
            pedidoAEliminar = pedidoAEliminar,
            pedidoAEditar = pedidoAEditar,
            tomaPendienteDeEliminar = tomaPendienteDeEliminar,
            inputRecargarStock = inputRecargarStock,
            inputUnidadesPedido = inputUnidadesPedido,
            inputUnidadesRecibidas = inputUnidadesRecibidas,
            inputPrecioActualizado = inputPrecioActualizado,
            inputEditarResumen = inputEditarResumen,
            inputEditarTotal = inputEditarTotal,
            monedaActiva = monedaActiva,
            carritoItems = carritoItems,
            pacienteActivoId = pacienteActivo?.id,
            editingMedicationId = editingMedicationId,
            estudiosAdjuntos = estudiosAdjuntos,
            database = database,
            onMostrarDialogoCerrarInformeSinGuardarChange = onMostrarDialogoCerrarInformeSinGuardarChange,
            onMedicationToDeleteChange = onMedicationToDeleteChange,
            onDuplicateMedicationChange = onDuplicateMedicationChange,
            onInsumoARecargarChange = onInsumoARecargarChange,
            onInsumoAPedirChange = onInsumoAPedirChange,
            onItemCarritoAConfirmarChange = onItemCarritoAConfirmarChange,
            onItemCarritoAEliminarChange = onItemCarritoAEliminarChange,
            onConfirmarRecepcionTotalChange = onConfirmarRecepcionTotalChange,
            onPedidoAEliminarChange = onPedidoAEliminarChange,
            onPedidoAEditarChange = onPedidoAEditarChange,
            onTomaPendienteDeEliminarChange = onTomaPendienteDeEliminarChange,
            onInputRecargarStockChange = onInputRecargarStockChange,
            onInputUnidadesPedidoChange = onInputUnidadesPedidoChange,
            onInputUnidadesRecibidasChange = onInputUnidadesRecibidasChange,
            onInputPrecioActualizadoChange = onInputPrecioActualizadoChange,
            onInputEditarResumenChange = onInputEditarResumenChange,
            onInputEditarTotalChange = onInputEditarTotalChange,
            onCargarMedicamentoEnFormulario = onCargarMedicamentoEnFormulario,
            onGuardarInformeMedicoActual = onGuardarInformeMedicoActual,
            onCerrarFormularioInforme = onCerrarFormularioInforme,
            onResetForm = onResetForm,
            onDeleteAttachmentFile = onDeleteAttachmentFile,
            onFormatDate = onFormatDate,
            onFormatHour = { android.text.format.DateFormat.format("HH:mm", it).toString() },
            onFormatMoney = { amount, moneda -> "$moneda ${"%.2f".format(amount)}" }
        )

        DialogosPrincipalesPanel(
            perfilPendienteDeEliminarState = perfilPendienteDeEliminarState,
            mostrarDialogoBackupManualState = mostrarDialogoBackupManualState,
            mostrarDialogoRestoreSeleccionState = mostrarDialogoRestoreSeleccionState,
            mostrarDialogoProgramarBackupState = mostrarDialogoProgramarBackupState,
            backupSelectionState = backupSelectionState,
            restoreSelectionState = restoreSelectionState,
            backupPatientIdState = backupPatientIdState,
            restorePatientIdState = restorePatientIdState,
            ejecutandoBackupManualState = ejecutandoBackupManualState,
            restaurandoBackupState = restaurandoBackupState,
            frecuenciaBackupSeleccionada = frecuenciaBackupSeleccionada,
            horaBackupSeleccionada = horaBackupSeleccionada,
            minutoBackupSeleccionado = minutoBackupSeleccionado,
            pacienteActivoNombre = pacienteActivo?.nombre,
            pacienteActivoId = pacienteActivo?.id,
            editingPatientId = editingPatientIdState.value,
            database = database,
            coroutineScope = coroutineScope,
            onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
            onMostrarPanelBackups = { onCerrarPanelesSecundarios(); onMostrarPanelBackupsChange(true) },
            onResetFichaPaciente = onResetFichaPaciente,
            onMostrarFichaPacienteChange = onMostrarFichaPacienteChange,
            onMensajeBackupChange = onMensajeBackupChange,
            onLanzarCreateBackup = onLanzarCreateBackup,
            onLanzarRestoreBackup = onLanzarRestoreBackup,
            onTimestampArchivo = onTimestampArchivo
        )

        if (mostrarFormulario) {
            NuevoRegistroForm(
                nombre = nombre, onNombreChange = onNombreChange,
                cantidad = cantidad, onCantidadChange = onCantidadChange,
                horaTomaSeleccionada = horaTomaSeleccionada, onHoraTomaChange = onHoraTomaChange,
                formatoSeleccionado = formatoSeleccionado,
                formaInsumoSeleccionada = formaInsumoSeleccionada, onFormaChange = onFormaChange,
                colorInsumoSeleccionado = colorInsumoSeleccionado, onColorChange = onColorChange,
                colorInsumo2Seleccionado = colorInsumo2Seleccionado, onColor2Change = onColor2Change,
                concentracionSeleccionada = concentracionSeleccionada, onConcentracionChange = onConcentracionChange,
                cicloSeleccionado = cicloSeleccionado, onCicloChange = onCicloChange,
                fechaInicio = fechaInicio, onFechaInicioChange = onFechaInicioChange,
                fechaFin = fechaFin, onFechaFinChange = onFechaFinChange,
                stockActual = stockActual, onStockActualChange = onStockActualChange,
                stockMinimo = stockMinimo, onStockMinimoChange = onStockMinimoChange,
                precioPorUnidad = precioPorUnidad, onPrecioChange = onPrecioChange,
                telefonoPedidoWhatsapp = telefonoPedidoWhatsapp, onTelefonoChange = onTelefonoChange,
                presentacionPersistida = presentacionPersistida,
                esCicloCorto = esCicloCorto, onEsCicloCortoChange = onEsCicloCortoChange,
                estaActivo = estaActivo, onEstaActivoChange = onEstaActivoChange,
                alarmaActiva = alarmaActiva, onAlarmaActivaChange = onAlarmaActivaChange,
                controlarExistencias = controlarExistencias, onControlarExistenciasChange = onControlarExistenciasChange,
                dispensacionGratuita = dispensacionGratuita, onDispensacionGratuitaChange = onDispensacionGratuitaChange,
                expandedNombre = expandedNombre, onExpandedNombreChange = onExpandedNombreChange,
                expandedToma = expandedToma, onExpandedTomaChange = onExpandedTomaChange,
                expandedConcentracion = expandedConcentracion, onExpandedConcentracionChange = onExpandedConcentracionChange,
                expandedCiclo = expandedCiclo, onExpandedCicloChange = onExpandedCicloChange,
                expandedOrigenReposicion = expandedOrigenReposicion, onExpandedOrigenReposicionChange = onExpandedOrigenReposicionChange,
                horasTomas = horasTomas,
                sugerencias = sugerencias,
                selectedMedication = selectedMedication, onSelectedMedicationChange = onSelectedMedicationChange,
                tomaSeleccionada = tomaSeleccionada, onTomaSeleccionadaChange = onTomaSeleccionadaChange,
                origenReposicion = origenReposicion, onOrigenReposicionChange = onOrigenReposicionChange,
                editingMedicationId = editingMedicationId,
                pacienteActivo = pacienteActivo,
                insumosGuardados = insumosGuardados,
                monedaActiva = monedaActiva,
                database = database,
                coroutineScope = coroutineScope,
                panelInternoScrollState = panelInternoScrollState,
                tienePermisoNotificaciones = tienePermisoNotificaciones,
                tienePermisoAlarmaExacta = tienePermisoAlarmaExacta,
                tienePermisoPantallaCompleta = tienePermisoPantallaCompleta,
                tieneAccesoNoMolestar = tieneAccesoNoMolestar,
                notificationPermissionLauncher = notificationPermissionLauncher,
                exactAlarmPermissionLauncher = exactAlarmPermissionLauncher,
                fullScreenIntentPermissionLauncher = fullScreenIntentPermissionLauncher,
                notificationPolicyAccessLauncher = notificationPolicyAccessLauncher,
                duplicateMedication = duplicateMedication, onDuplicateMedicationChange = onDuplicateMedicationChange,
                onAbrirNuevaFichaPaciente = onAbrirNuevaFichaPaciente,
                onResetForm = onResetForm,
                onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
                onMostrarFormularioChange = onMostrarFormularioChange
            )
        }

        if (mostrarListaInsumos) {
            ListaInsumosPanel(
                insumosGuardados = insumosGuardados,
                inventarioLazyRowState = inventarioLazyRowState,
                monedaActiva = monedaActiva,
                carritoItems = carritoItems,
                insumoSeleccionadoEnInventario = insumoSeleccionadoEnInventario,
                mostrarListaInsumos = mostrarListaInsumos,
                database = database,
                coroutineScope = coroutineScope,
                editingMedicationId = editingMedicationId,
                onInputRecargarStockChange = onInputRecargarStockChange,
                onInsumoARecargarChange = onInsumoARecargarChange,
                onInputUnidadesPedidoChange = onInputUnidadesPedidoChange,
                onInsumoAPedirChange = onInsumoAPedirChange,
                onMostrarPanelPedidosChange = onMostrarPanelPedidosChange,
                onMostrarFormularioChange = onMostrarFormularioChange,
                onCerrarPanelesSecundarios = onCerrarPanelesSecundarios,
                onCargarMedicamentoEnFormulario = onCargarMedicamentoEnFormulario,
                onAlarmaActivaChange = onAlarmaActivaChange,
                onMedicationToDeleteChange = onMedicationToDeleteChange,
                panelInternoScrollState = panelInternoScrollState
            )
        }

        visorAdjuntos?.let { visor ->
            AttachmentFullscreenViewer(
                visor = visor,
                onDismiss = { onVisorAdjuntosChange(null) },
                onPrevious = { onVisorAdjuntosChange(visor.previous()) },
                onNext = { onVisorAdjuntosChange(visor.next()) }
            )
        }
    }
}
