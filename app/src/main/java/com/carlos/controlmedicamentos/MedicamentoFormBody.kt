package com.carlos.controlmedicamentos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.carlos.controlmedicamentos.data.local.ControlEmbarazo
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.NinoEntity
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.SignosVitales
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.data.local.AppDatabase
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MedicamentoFormBody(
    modifier: Modifier,
    s: MedicamentoFormState,
    fallAlertPanelState: MutableState<Boolean>,
    mostrarListadoSignosPanelState: MutableState<Boolean>,
    mesesExpandidosSignos: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    registrosSignosSeleccionados: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    mostrarListadoSignosGuardadosState: MutableState<Boolean>,
    perfilesPacientes: List<PatientProfile>,
    pacienteActivo: PatientProfile?,
    insumosGuardados: List<Medication>,
    carritoItems: List<CarritoItem>,
    reportesSalud: List<MedicalReport>,
    citasMedicas: List<MedicalAppointment>,
    profesionalesHabituales: List<MedicalPractitioner>,
    signosVitales: List<SignosVitales>,
    embarazoActivo: ControlEmbarazo?,
    ninosDelPaciente: List<NinoEntity>,
    monedaActiva: String,
    sugerencias: List<VademecumMedication>,
    pagerEscritorioState: androidx.compose.foundation.pager.PagerState,
    paginaBaseEscritorio: Int,
    fechaBaseEscritorio: Long,
    fechaResumenEscritorioTexto: String,
    escritorioEsHoy: Boolean,
    edadCalculadaPaciente: String,
    ultimoBackupAutomatico: java.io.File?,
    opcionesToma: List<String>,
    ciclos: List<String>,
    opcionesPesoUnidad: List<String>,
    opcionesEstaturaUnidad: List<String>,
    opcionesFrecuenciaBackup: List<String>,
    opcionesHoraBackup: List<String>,
    opcionesReintentoCritico: List<Int>,
    opcionesIntentosCriticos: List<Int>,
    opcionesRecordatorioCita: List<Int>,
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
    fechaEscritorioSeleccionada: Long,
    signosVitalesSeleccionados: List<SignosVitales>,
    exportandoTomasState: MutableState<Boolean>,
    periodoExportacionPendienteState: MutableState<IntakeExportPeriod?>,
    exportacionSignosPendienteState: MutableState<VitalSignsExportRequest?>,
    database: AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    launchers: FormLaunchers,
    callbacks: MedicamentoFormCallbacks,
) {
    var mostrarListadoSignosPanel by mostrarListadoSignosPanelState
    var mostrarListadoSignosGuardados by mostrarListadoSignosGuardadosState
    val mostrarFormulario = s.mostrarFormulario
    val mostrarListaInsumos = s.mostrarListaInsumos
    val mostrarPanelPacientes = s.mostrarPanelPacientes
    val mostrarPanelCitasMedicas = s.mostrarPanelCitasMedicas
    val mostrarPanelProfesionales = s.mostrarPanelProfesionales
    val mostrarFormularioProfesional = s.mostrarFormularioProfesional
    val mostrarFormularioCitaMedica = s.mostrarFormularioCitaMedica
    val mostrarFichaPaciente = s.mostrarFichaPaciente
    val mostrarPanelInformes = s.mostrarPanelInformes
    val mostrarFormularioInforme = s.mostrarFormularioInforme
    val mostrarPanelBackups = s.mostrarPanelBackups
    val mostrarPanelPedidos = s.mostrarPanelPedidos
    val mostrarPanelPodometro = s.mostrarPanelPodometro
    val mostrarPanelConfiguracionAlertas = s.mostrarPanelConfiguracionAlertas
    val mostrarPanelSignosVitales = s.mostrarPanelSignosVitales
    val mostrarPanelConfiguracionIa = s.mostrarPanelConfiguracionIa
    val mostrarPanelCicloMenstrual = s.mostrarPanelCicloMenstrual
    val mostrarPanelEmbarazo = s.mostrarPanelEmbarazo
    val mostrarPanelAnticonceptivos = s.mostrarPanelAnticonceptivos
    val mostrarPanelPediatrico = s.mostrarPanelPediatrico
    val mostrarPanelReporteClinico = s.mostrarPanelReporteClinico
    val mostrarPanelDiario = s.mostrarPanelDiario
    val mostrarPanelAsistenteIa = s.mostrarPanelAsistenteIa
    val mostrarPanelEstadisticas = s.mostrarPanelEstadisticas
    val mostrarPanelVerificadorTomas = s.mostrarPanelVerificadorTomas
    val mostrarPanelHidratacion = s.mostrarPanelHidratacion
    val mostrarPanelSedentarismo = s.mostrarPanelSedentarismo
    val mostrarPanelDentista = s.mostrarPanelDentista
    val mostrarEscritorio = !mostrarFormulario && !mostrarListaInsumos && !mostrarPanelPacientes && !mostrarPanelCitasMedicas && !mostrarPanelProfesionales && !mostrarFormularioProfesional && !mostrarFormularioCitaMedica && !mostrarFichaPaciente && !mostrarPanelInformes && !mostrarFormularioInforme && !mostrarPanelBackups && !mostrarPanelPedidos && !mostrarPanelPodometro && !mostrarPanelConfiguracionAlertas && !mostrarPanelSignosVitales && !mostrarPanelConfiguracionIa && !mostrarPanelCicloMenstrual && !mostrarPanelEmbarazo && !mostrarPanelAnticonceptivos && !mostrarPanelPediatrico && !mostrarPanelReporteClinico && !mostrarPanelDiario && !mostrarPanelAsistenteIa && !mostrarPanelEstadisticas && !mostrarPanelVerificadorTomas && !mostrarPanelHidratacion && !mostrarPanelSedentarismo && !mostrarPanelDentista
    val citaMedicaSeleccionada = citasMedicas.firstOrNull { it.id == s.citaMedicaSeleccionadaId }
    val medicoSeleccionado = profesionalesHabituales.firstOrNull { it.id == s.profesionalSeleccionadoId }
    val nombreProfesionalActivo = s.nombreProfesional.trim()
    val citasSincronizadasMedico = citasMedicas.filter { cita ->
        when {
            s.editingPractitionerId != null && cita.practitionerId == s.editingPractitionerId -> true
            nombreProfesionalActivo.isNotBlank() -> {
                val citaNombre = cita.doctorName.trim()
                citaNombre.equals(nombreProfesionalActivo, ignoreCase = true) ||
                    citaNombre.contains(nombreProfesionalActivo, ignoreCase = true) ||
                    nombreProfesionalActivo.contains(citaNombre, ignoreCase = true)
            }
            else -> false
        }
    }.sortedBy { it.scheduledAt }
    val proximaCitaMedico = citasSincronizadasMedico
        .filter { !it.isCompleted && it.scheduledAt >= System.currentTimeMillis() }
        .minByOrNull { it.scheduledAt }
    val informesSincronizadosMedico = reportesSalud
        .filter { reporte ->
            val pracId = s.editingPractitionerId
            val pracNombre = profesionalesHabituales.find { it.id == pracId }?.name?.trim() ?: ""
            pracId != null && (
                reporte.practitionerId == pracId ||
                (reporte.practitionerId == null && pracNombre.isNotBlank() && (
                    reporte.titulo.trim().equals(pracNombre, ignoreCase = true) ||
                    reporte.titulo.trim().contains(pracNombre, ignoreCase = true) ||
                    pracNombre.contains(reporte.titulo.trim(), ignoreCase = true)
                ))
            )
        }
        .ifEmpty {
            if (s.editingPractitionerId != null) emptyList()
            else reportesSalud.take(5)
        }
    val scrollState = rememberScrollState()
    val panelInternoScrollState = rememberScrollState()
    val inventarioLazyRowState = rememberLazyListState()
    val panelUsaScrollInterno =
        mostrarFormulario ||
            mostrarPanelProfesionales ||
            mostrarFormularioProfesional ||
            mostrarFormularioCitaMedica ||
            mostrarPanelBackups ||
            mostrarPanelPedidos ||
            (mostrarPanelInformes && !mostrarFormularioInforme) ||
            mostrarFormularioInforme ||
            mostrarListaInsumos ||
            mostrarPanelConfiguracionAlertas ||
            mostrarPanelPodometro ||
            mostrarPanelAnticonceptivos ||
            mostrarPanelPediatrico ||
            mostrarPanelEstadisticas ||
            mostrarPanelVerificadorTomas ||
            mostrarPanelSignosVitales ||
            mostrarPanelHidratacion ||
            mostrarPanelSedentarismo ||
            mostrarPanelDentista ||
            mostrarPanelCicloMenstrual ||
            mostrarPanelEmbarazo ||
            mostrarPanelReporteClinico ||
            mostrarPanelDiario ||
            mostrarPanelAsistenteIa ||
            mostrarFichaPaciente ||
            mostrarPanelPacientes ||
            fallAlertPanelState.value
    LaunchedEffect(
        mostrarFormulario,
        mostrarPanelProfesionales,
        mostrarFormularioProfesional,
        mostrarPanelCitasMedicas,
        mostrarFormularioCitaMedica
    ) {
        if (
            mostrarFormulario ||
                mostrarPanelProfesionales ||
                mostrarFormularioProfesional ||
                mostrarPanelCitasMedicas ||
                mostrarFormularioCitaMedica
        ) {
            scrollState.scrollTo(0)
            panelInternoScrollState.scrollTo(0)
        }
    }
    // BackHandlers: cualquier panel abierto debe cerrarse con el botón nativo "atrás"
    BackHandler(enabled = !mostrarEscritorio) {
        callbacks.onCerrarPanelesSecundarios()
    }
    MedicamentoFormGradientWrapper(
        modifier = modifier,
        mostrarEscritorio = mostrarEscritorio,
        panelUsaScrollInterno = panelUsaScrollInterno,
        scrollState = scrollState
    ) {
        if (mostrarEscritorio) {
            MedicamentoFormBodyEscritorio(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                s = s,
                callbacks = callbacks,
                launchers = launchers,
                pacienteActivo = pacienteActivo,
                perfilesPacientes = perfilesPacientes,
                insumosGuardados = insumosGuardados,
                embarazoActivo = embarazoActivo,
                ninosDelPaciente = ninosDelPaciente,
                pagerEscritorioState = pagerEscritorioState,
                paginaBaseEscritorio = paginaBaseEscritorio,
                fechaBaseEscritorio = fechaBaseEscritorio,
                fechaResumenEscritorioTexto = fechaResumenEscritorioTexto,
                escritorioEsHoy = escritorioEsHoy,
                database = database,
                fallAlertPanelState = fallAlertPanelState
            )
        }

        MedicamentoFormBodyPaneles(
            s = s,
            callbacks = callbacks,
            launchers = launchers,
            pacienteActivo = pacienteActivo,
            perfilesPacientes = perfilesPacientes,
            insumosGuardados = insumosGuardados,
            carritoItems = carritoItems,
            monedaActiva = monedaActiva,
            profesionalesHabituales = profesionalesHabituales,
            reportesSalud = reportesSalud,
            citasMedicas = citasMedicas,
            signosVitales = signosVitales,
            edadCalculadaPaciente = edadCalculadaPaciente,
            ultimoBackupAutomatico = ultimoBackupAutomatico,
            sugerencias = sugerencias,
            signosVitalesSeleccionados = signosVitalesSeleccionados,
            mesesExpandidosSignos = mesesExpandidosSignos,
            registrosSignosSeleccionados = registrosSignosSeleccionados,
            opcionesFrecuenciaBackup = opcionesFrecuenciaBackup,
            opcionesHoraBackup = opcionesHoraBackup,
            opcionesReintentoCritico = opcionesReintentoCritico,
            opcionesIntentosCriticos = opcionesIntentosCriticos,
            frecuenciaBackupSeleccionada = frecuenciaBackupSeleccionada,
            horaBackupSeleccionada = horaBackupSeleccionada,
            minutoBackupSeleccionado = minutoBackupSeleccionado,
            expandedHoraBackup = expandedHoraBackup,
            urlServicioIa = urlServicioIa,
            modeloServicioIa = modeloServicioIa,
            recordatorioSignosActivo = recordatorioSignosActivo,
            recordatorioSignosHora = recordatorioSignosHora,
            recordatorioSignosMinuto = recordatorioSignosMinuto,
            mostrarTimePickerSignos = mostrarTimePickerSignos,
            mostrarListadoSignosPanel = mostrarListadoSignosPanel,
            mostrarListadoSignosGuardados = mostrarListadoSignosGuardados,
            exportandoTomasState = exportandoTomasState,
            periodoExportacionPendienteState = periodoExportacionPendienteState,
            database = database,
            coroutineScope = coroutineScope,
            panelInternoScrollState = panelInternoScrollState,
            inventarioLazyRowState = inventarioLazyRowState
        )

        FallAlertPanelManager(
            mostrar = fallAlertPanelState,
            patientId = pacienteActivo?.id ?: 0,
            database = database,
            onVolver = callbacks.onCerrarPanelesSecundarios
        )

        // Menú hamburguesa flotante (visible en todas las pantallas secundarias)
        MenuHamburguesaFlotante(
            mostrarEscritorio = mostrarEscritorio,
            mostrarMenuHamburguesaState = s.mostrarMenuHamburguesaState,
            pacienteActivo = pacienteActivo,
            alarmaSonidoUriState = s.alarmaSonidoUriState,
            alarmaSonidoNombreState = s.alarmaSonidoNombreState,
            fallAlertPanelState = fallAlertPanelState,
            intervaloReintentoSeleccionadoState = s.intervaloReintentoSeleccionadoState,
            numeroIntentosCriticosSeleccionadoState = s.numeroIntentosCriticosSeleccionadoState,
            onCerrarPanelesSecundarios = callbacks.onCerrarPanelesSecundarios,
            onAbrirNuevaFichaPaciente = callbacks.onAbrirNuevaFichaPaciente,
            onResetForm = callbacks.onResetForm,
            onMostrarFormulario = { s.mostrarFormularioState.value = it },
            onMostrarPanelPacientes = { s.mostrarPanelPacientesState.value = it },
            onMostrarPanelProfesionales = { s.mostrarPanelProfesionalesState.value = it },
            onMostrarPanelInformes = { s.mostrarPanelInformesState.value = it },
            onMostrarListaInsumos = { s.mostrarListaInsumosState.value = it },
            onMostrarDialogoMedia = { s.mostrarDialogoMediaState.value = it },
            onMostrarFormularioInformeChange = { s.mostrarFormularioInformeState.value = it },
            onMostrarPanelSignosVitales = callbacks.onMostrarPanelSignosVitalesInit,
            onMostrarPanelConfiguracionAlertas = { s.mostrarPanelConfiguracionAlertasState.value = it },
            onMostrarPanelAsistenteIa = { s.mostrarPanelAsistenteIaState.value = it },
            onMostrarPanelPodometro = { s.mostrarPanelPodometroState.value = it },
            onMostrarPanelPedidos = { s.mostrarPanelPedidosState.value = it },
            onMostrarPanelBackups = { s.mostrarPanelBackupsState.value = it },
            onMostrarPanelHidratacion = { s.mostrarPanelHidratacionState.value = it },
            onMostrarPanelSedentarismo = { s.mostrarPanelSedentarismoState.value = it },
            onMostrarPanelDentista = { s.mostrarPanelDentistaState.value = it },
            onMostrarPanelReporteClinico = { s.mostrarPanelReporteClinicoState.value = it },
            onMostrarPanelEstadisticas = { s.mostrarPanelEstadisticasState.value = it },
            onMostrarPanelVerificadorTomas = { s.mostrarPanelVerificadorTomasState.value = it },
            onMostrarPanelDiario = { s.mostrarPanelDiarioState.value = it },
            onMostrarPanelCicloMenstrual = { s.mostrarPanelCicloMenstrualState.value = it },
            onResolveAlarmSoundLabel = callbacks.onResolveAlarmSoundLabel
        )

        ListadoSignosVitalesPanel(
            mostrarListadoSignosPanel = mostrarListadoSignosPanel,
            signosVitales = signosVitales,
            filtroExportacionSignos = s.filtroExportacionSignosState.value,
            fechaInicioExportacionSignos = s.fechaInicioExportacionSignosState.value,
            fechaFinExportacionSignos = s.fechaFinExportacionSignosState.value,
            expandedFiltroExportacionSignos = s.expandedFiltroExportacionSignosState.value,
            registrosSignosSeleccionados = registrosSignosSeleccionados,
            mesesExpandidosSignos = mesesExpandidosSignos,
            mostrarVistaPreviaSignosSeleccionados = s.mostrarVistaPreviaSignosSeleccionadosState.value,
            signosVitalesSeleccionados = signosVitalesSeleccionados,
            exportandoSignosVitales = s.exportandoSignosVitalesState.value,
            onFiltroExportacionSignosChange = { s.filtroExportacionSignosState.value = it },
            onFechaInicioExportacionSignosChange = { s.fechaInicioExportacionSignosState.value = it },
            onFechaFinExportacionSignosChange = { s.fechaFinExportacionSignosState.value = it },
            onExpandedFiltroExportacionSignosChange = { s.expandedFiltroExportacionSignosState.value = it },
            onMostrarVistaPreviaSignosSeleccionadosChange = { s.mostrarVistaPreviaSignosSeleccionadosState.value = it },
            onExportandoSignosVitalesChange = { s.exportandoSignosVitalesState.value = it },
            onExportacionSignosPendienteChange = { exportacionSignosPendienteState.value = it },
            onMostrarListadoSignosPanelChange = { mostrarListadoSignosPanelState.value = it },
            onMostrarPanelSignosVitalesChange = { s.mostrarPanelSignosVitalesState.value = it },
            onLanzarExportVitalSigns = { launchers.exportVitalSignsReportLauncher.launch(it) }
        )
    }
}
