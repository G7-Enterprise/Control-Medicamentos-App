package com.carlos.controlmedicamentos

import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.PatientProfile

internal class MedicamentoFormCallbacks(
    val onCerrarPanelesSecundarios: () -> Unit,
    val onAbrirNuevaFichaPaciente: () -> Unit,
    val onAbrirFichaPaciente: (PatientProfile, Boolean) -> Unit,
    val onCargarFichaPaciente: (PatientProfile) -> Unit,
    val onVolverPantallaAnteriorDesdeFichaPaciente: () -> Unit,
    val onResetForm: () -> Unit,
    val onResetFichaPaciente: () -> Unit,
    val onCargarMedicamentoEnFormulario: (Medication) -> Unit,
    val onGuardarInformeMedicoActual: () -> Unit,
    val onCerrarFormularioInforme: () -> Unit,
    val onDeleteAttachmentFile: (String) -> Unit,
    val onGuardarConfiguracionAlertasCriticas: () -> Unit,
    val onGuardarConfiguracionIa: () -> Unit,
    val onTimestampArchivo: () -> String,
    val onCalcularEdadDesdeNacimiento: (Long) -> Int,
    val onSavePersistedBirthday: (android.content.Context, Int, Long) -> Unit,
    val onLoadPersistedBirthday: (android.content.Context, Int) -> Long?,
    val onClearPersistedBirthday: (android.content.Context, Int) -> Unit,
    val onRequestBirthdayPreview: (BirthdayCelebrationRequest) -> Unit,
    val onResetInformeMedico: () -> Unit,
    val onCargarInformeMedico: (MedicalReport) -> Unit,
    val onGuardarMedicoHabitualActual: () -> Unit,
    val onResetMedicoHabitual: () -> Unit,
    val onAbrirFormularioCitaMedica: (MedicalAppointment?) -> Unit,
    val onFormatDate: (Long) -> String,
    val onFormatReminderMinutesLabel: (Int) -> String,
    val onInformeMedicoTieneCambiosSinGuardar: () -> Boolean,
    val onLaunchDocumentScanner: () -> Unit,
    val onFormatHour: (Long) -> String,
    val onMoverFecha: (Long, Int) -> Long,
    val onResolveAlarmSoundLabel: (android.content.Context, String) -> String,
    val onMostrarPanelSignosVitalesInit: () -> Unit,
    val onFrecuenciaBackupChange: (String) -> Unit,
    val onExpandedHoraBackupChange: (Boolean) -> Unit,
    val onHoraBackupChange: (Int) -> Unit,
    val onMinutoBackupChange: (Int) -> Unit,
    val onUrlServicioIaChange: (String) -> Unit,
    val onModeloServicioIaChange: (String) -> Unit,
    val onRecordatorioSignosActivoChange: (Boolean) -> Unit,
    val onRecordatorioSignosHoraChange: (Int) -> Unit,
    val onRecordatorioSignosMinutoChange: (Int) -> Unit,
    val onMostrarTimePickerSignosChange: (Boolean) -> Unit
)
