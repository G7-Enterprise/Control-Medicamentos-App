package com.carlos.controlmedicamentos

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.ControlEmbarazo
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.NinoEntity
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.SignosVitales
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import kotlinx.coroutines.CoroutineScope

internal class MedicamentoFormUIListParams(
    val perfilesPacientes: List<PatientProfile>,
    val pacienteActivo: PatientProfile?,
    val insumosGuardados: List<Medication>,
    val carritoItems: List<CarritoItem>,
    val monedaActiva: String,
    val profesionalesHabituales: List<MedicalPractitioner>,
    val reportesSalud: List<MedicalReport>,
    val citasMedicas: List<MedicalAppointment>,
    val signosVitales: List<SignosVitales>,
    val embarazoActivo: ControlEmbarazo?,
    val ninosDelPaciente: List<NinoEntity>,
    val edadCalculadaPaciente: String,
    val ultimoBackupAutomatico: java.io.File?,
    val sugerencias: List<VademecumMedication>,
    val signosVitalesSeleccionados: List<SignosVitales>,
    val mesesExpandidosSignos: SnapshotStateList<String>,
    val registrosSignosSeleccionados: SnapshotStateList<Int>,
)

internal class MedicamentoFormUIScrollParams(
    val panelUsaScrollInterno: Boolean,
    val scrollState: ScrollState,
    val panelInternoScrollState: ScrollState,
    val inventarioLazyRowState: androidx.compose.foundation.lazy.LazyListState,
)

internal class MedicamentoFormUIConfigParams(
    val opcionesToma: List<String>,
    val ciclos: List<String>,
    val opcionesPesoUnidad: List<String>,
    val opcionesEstaturaUnidad: List<String>,
    val opcionesFrecuenciaBackup: List<String>,
    val opcionesHoraBackup: List<String>,
    val opcionesReintentoCritico: List<Int>,
    val opcionesIntentosCriticos: List<Int>,
    val opcionesRecordatorioCita: List<Int>,
    val frecuenciaBackupSeleccionada: String,
    val horaBackupSeleccionada: Int,
    val minutoBackupSeleccionado: Int,
    val expandedHoraBackup: Boolean,
    val urlServicioIa: String,
    val modeloServicioIa: String,
    val recordatorioSignosActivo: Boolean,
    val recordatorioSignosHora: Int,
    val recordatorioSignosMinuto: Int,
    val mostrarTimePickerSignos: Boolean,
    val fechaEscritorioSeleccionada: Long,
)

internal class MedicamentoFormUIEscritorioParams(
    val mostrarEscritorio: Boolean,
    val mostrarListadoSignosPanel: Boolean,
    val mostrarListadoSignosGuardados: Boolean,
    val pagerEscritorioState: androidx.compose.foundation.pager.PagerState,
    val paginaBaseEscritorio: Int,
    val fechaBaseEscritorio: Long,
    val fechaResumenEscritorioTexto: String,
    val escritorioEsHoy: Boolean,
)

internal class MedicamentoFormUIStateParams(
    val exportandoTomasState: MutableState<Boolean>,
    val periodoExportacionPendienteState: MutableState<IntakeExportPeriod?>,
    val fallAlertPanelState: MutableState<Boolean>,
    val database: AppDatabase,
    val coroutineScope: CoroutineScope,
)
