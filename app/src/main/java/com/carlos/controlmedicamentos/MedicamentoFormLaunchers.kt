package com.carlos.controlmedicamentos

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.carlos.controlmedicamentos.backup.BackupManager
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
internal class FormLaunchers(
    val notificationPermissionLauncher: ActivityResultLauncher<String>,
    val exactAlarmPermissionLauncher: ActivityResultLauncher<android.content.Intent>,
    val fullScreenIntentPermissionLauncher: ActivityResultLauncher<android.content.Intent>,
    val notificationPolicyAccessLauncher: ActivityResultLauncher<android.content.Intent>,
    val pickStudyImagesLauncher: ActivityResultLauncher<String>,
    val documentScanner: GmsDocumentScanner?,
    val scannerLauncher: ActivityResultLauncher<IntentSenderRequest>,
    val takeStudyPictureLauncher: ActivityResultLauncher<Void?>,
    val cameraPermissionLauncher: ActivityResultLauncher<String>,
    val pickFotoPerfilLauncher: ActivityResultLauncher<String>,
    val pickRestoreSignosLauncher: ActivityResultLauncher<String>,
    val takeFotoPerfilLauncher: ActivityResultLauncher<Void?>,
    val cameraPermissionPerfilLauncher: ActivityResultLauncher<String>,
    val createBackupDocumentLauncher: ActivityResultLauncher<String>,
    val restoreBackupDocumentLauncher: ActivityResultLauncher<Array<String>>,
    val exportMedicationReportLauncher: ActivityResultLauncher<String>,
    val exportVitalSignsReportLauncher: ActivityResultLauncher<String>,
    val exportClinicalReportLauncher: ActivityResultLauncher<String>,
    val ringtonePickerLauncher: ActivityResultLauncher<android.content.Intent>
)

@Composable
internal fun rememberMedicamentoFormLaunchers(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    // Permission state
    tienePermisoNotificacionesState: MutableState<Boolean>,
    tienePermisoAlarmaExactaState: MutableState<Boolean>,
    tienePermisoPantallaCompletaState: MutableState<Boolean>,
    tienePermisoCamaraState: MutableState<Boolean>,
    tieneAccesoNoMolestarState: MutableState<Boolean>,
    // Camera/photo state
    fotoPerfilPacienteState: MutableState<String?>,
    cameraPermissionPendingState: MutableState<Boolean>,
    cameraPermissionPerfilPendingState: MutableState<Boolean>,
    // Attachment state
    estudiosAdjuntos: SnapshotStateList<String>,
    adjuntosPendientesReemplazo: SnapshotStateList<PendingAttachmentReplacement>,
    // Backup state
    ejecutandoBackupManualState: MutableState<Boolean>,
    restaurandoBackupState: MutableState<Boolean>,
    backupSelectionState: MutableState<com.carlos.controlmedicamentos.backup.BackupSelection>,
    backupPatientIdState: MutableState<Int?>,
    restoreSelectionState: MutableState<com.carlos.controlmedicamentos.backup.BackupSelection>,
    restorePatientIdState: MutableState<Int?>,
    refrescoBackupState: MutableState<Int>,
    mensajeBackupState: MutableState<String>,
    // Panel visibility after restore
    mostrarFichaPacienteState: MutableState<Boolean>,
    mostrarFormularioInformeState: MutableState<Boolean>,
    formularioInformeAutoAbiertoState: MutableState<Boolean>,
    mostrarPanelPacientesState: MutableState<Boolean>,
    mostrarPanelInformesState: MutableState<Boolean>,
    mostrarListaInsumosState: MutableState<Boolean>,
    mostrarPanelBackupsState: MutableState<Boolean>,
    mostrarPanelConfiguracionAlertasState: MutableState<Boolean>,
    mostrarPanelSignosVitalesState: MutableState<Boolean>,
    mostrarPanelConfiguracionIaState: MutableState<Boolean>,
    mostrarFormularioState: MutableState<Boolean>,
    // Alert config state
    intervaloReintentoSeleccionadoState: MutableState<Int>,
    numeroIntentosCriticosSeleccionadoState: MutableState<Int>,
    alarmaSonidoUriState: MutableState<String>,
    alarmaSonidoNombreState: MutableState<String>,
    // Export state
    periodoExportacionPendienteState: MutableState<IntakeExportPeriod?>,
    exportandoTomasState: MutableState<Boolean>,
    exportacionSignosPendienteState: MutableState<VitalSignsExportRequest?>,
    exportandoSignosVitalesState: MutableState<Boolean>,
    exportandoReporteClinicoState: MutableState<Boolean>,
    restaurandoSignosVitalesState: MutableState<Boolean>,
    // Data
    pacienteActivo: PatientProfile?,
    database: AppDatabase,
    fechaEscritorioSeleccionada: Long,
    documentScannerInstance: GmsDocumentScanner?
): FormLaunchers {
    var tienePermisoNotificaciones by tienePermisoNotificacionesState
    var tienePermisoAlarmaExacta by tienePermisoAlarmaExactaState
    var tienePermisoPantallaCompleta by tienePermisoPantallaCompletaState
    var tienePermisoCamara by tienePermisoCamaraState
    var tieneAccesoNoMolestar by tieneAccesoNoMolestarState
    var fotoPerfilPaciente by fotoPerfilPacienteState
    var cameraPermissionPending by cameraPermissionPendingState
    var cameraPermissionPerfilPending by cameraPermissionPerfilPendingState
    var ejecutandoBackupManual by ejecutandoBackupManualState
    var restaurandoBackup by restaurandoBackupState
    var backupSelection by backupSelectionState
    var backupPatientId by backupPatientIdState
    var restoreSelection by restoreSelectionState
    var restorePatientId by restorePatientIdState
    var refrescoBackup by refrescoBackupState
    var mensajeBackup by mensajeBackupState
    var mostrarFichaPaciente by mostrarFichaPacienteState
    var mostrarFormularioInforme by mostrarFormularioInformeState
    var formularioInformeAutoAbierto by formularioInformeAutoAbiertoState
    var mostrarPanelPacientes by mostrarPanelPacientesState
    var mostrarPanelInformes by mostrarPanelInformesState
    var mostrarListaInsumos by mostrarListaInsumosState
    var mostrarPanelBackups by mostrarPanelBackupsState
    var mostrarPanelConfiguracionAlertas by mostrarPanelConfiguracionAlertasState
    var mostrarPanelSignosVitales by mostrarPanelSignosVitalesState
    var mostrarPanelConfiguracionIa by mostrarPanelConfiguracionIaState
    var mostrarFormulario by mostrarFormularioState
    var intervaloReintentoSeleccionado by intervaloReintentoSeleccionadoState
    var numeroIntentosCriticosSeleccionado by numeroIntentosCriticosSeleccionadoState
    var alarmaSonidoUri by alarmaSonidoUriState
    var alarmaSonidoNombre by alarmaSonidoNombreState
    var periodoExportacionPendiente by periodoExportacionPendienteState
    var exportandoTomas by exportandoTomasState
    var exportacionSignosPendiente by exportacionSignosPendienteState
    var exportandoSignosVitales by exportandoSignosVitalesState
    var exportandoReporteClinico by exportandoReporteClinicoState
    var restaurandoSignosVitales by restaurandoSignosVitalesState

    val activity = context as? androidx.activity.ComponentActivity

    fun launchDocumentScanner() {
        if (activity != null && documentScannerInstance != null) {
            documentScannerInstance.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    // scannerLauncher referenced below - use a holder
                }
                .addOnFailureListener { _ ->
                    Toast.makeText(context, "Escaner no disponible", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        tienePermisoNotificaciones = granted || notificationPermissionGranted(context)
    }
    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        tienePermisoAlarmaExacta = exactAlarmPermissionGranted(context)
    }
    val fullScreenIntentPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        tienePermisoPantallaCompleta = fullScreenIntentPermissionGranted(context)
    }
    val notificationPolicyAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        tieneAccesoNoMolestar = notificationPolicyAccessGranted(context)
    }
    val pickStudyImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val previousCount = estudiosAdjuntos.size
        uris.forEach { uri ->
            copyUriToInternalStorage(context, uri)?.let { savedUri ->
                enqueueAttachmentForReport(
                    context = context,
                    currentAttachments = estudiosAdjuntos,
                    pendingReplacements = adjuntosPendientesReemplazo,
                    newAttachmentPath = savedUri
                )
            }
        }
        if (formularioInformeAutoAbierto) {
            formularioInformeAutoAbierto = false
            if (estudiosAdjuntos.isEmpty() || estudiosAdjuntos.size == previousCount) {
                mostrarFormularioInforme = false
            }
        }
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.forEach { page ->
                val imageUri = page.imageUri
                copyUriToInternalStorage(context, imageUri)?.let { savedUri ->
                    enqueueAttachmentForReport(
                        context = context,
                        currentAttachments = estudiosAdjuntos,
                        pendingReplacements = adjuntosPendientesReemplazo,
                        newAttachmentPath = savedUri
                    )
                }
            }
        }
    }
    val takeStudyPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            saveBitmapToInternalStorage(context, it)?.let { savedUri ->
                enqueueAttachmentForReport(
                    context = context,
                    currentAttachments = estudiosAdjuntos,
                    pendingReplacements = adjuntosPendientesReemplazo,
                    newAttachmentPath = savedUri
                )
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        tienePermisoCamara = granted || cameraPermissionGranted(context)
        if (granted && cameraPermissionPending) {
            cameraPermissionPending = false
            if (activity != null && documentScannerInstance != null) {
                documentScannerInstance.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener { _ ->
                        Toast.makeText(context, "Escaner no disponible", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            cameraPermissionPending = false
        }
    }
    val pickFotoPerfilLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val ruta = guardarFotoPerfil(context, uri = it, bitmap = null)
                withContext(Dispatchers.Main) { fotoPerfilPaciente = ruta }
            }
        }
    }
    val pickRestoreSignosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            restaurandoSignosVitales = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val restaurados = BackupManager.restoreOnlyVitalSigns(context, it)
                    withContext(Dispatchers.Main) {
                        restaurandoSignosVitales = false
                        Toast.makeText(context, "Se restauraron $restaurados registros de signos vitales", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        restaurandoSignosVitales = false
                        Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    val takeFotoPerfilLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val ruta = guardarFotoPerfil(context, uri = null, bitmap = it)
                withContext(Dispatchers.Main) { fotoPerfilPaciente = ruta }
            }
        }
    }
    val cameraPermissionPerfilLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && cameraPermissionPerfilPending) {
            cameraPermissionPerfilPending = false
            takeFotoPerfilLauncher.launch(null)
        } else {
            cameraPermissionPerfilPending = false
        }
    }
    val createBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            ejecutandoBackupManual = false
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val summary = BackupManager.exportManualBackup(context, uri, backupSelection, backupPatientId)
                withContext(Dispatchers.Main) {
                    ejecutandoBackupManual = false
                    refrescoBackup += 1
                    mensajeBackup = "Backup manual creado: ${summary.medications} medicamentos, ${summary.patients} perfiles"
                    Toast.makeText(context, mensajeBackup, Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    ejecutandoBackupManual = false
                    mensajeBackup = "No se pudo crear el backup: ${exception.message ?: "error desconocido"}"
                    Toast.makeText(context, mensajeBackup, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val restoreBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            restaurandoBackup = false
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val summary = BackupManager.importManualBackup(context, uri, restoreSelection, restorePatientId)
                withContext(Dispatchers.Main) {
                    restaurandoBackup = false
                    refrescoBackup += 1
                    mensajeBackup = "Backup restaurado: ${summary.medications} medicamentos, ${summary.patients} perfiles"
                    val restoredConfig = CriticalAlertSettings.load(context)
                    intervaloReintentoSeleccionado = restoredConfig.retryIntervalMinutes
                    numeroIntentosCriticosSeleccionado = restoredConfig.maxRetryCount
                    alarmaSonidoUri = restoredConfig.soundUri
                    alarmaSonidoNombre = resolveAlarmSoundLabel(context, restoredConfig.soundUri)
                    mostrarFichaPaciente = false
                    mostrarFormularioInforme = false
                    mostrarPanelPacientes = false
                    mostrarPanelInformes = false
                    mostrarListaInsumos = false
                    mostrarPanelBackups = false
                    mostrarPanelConfiguracionAlertas = false
                    mostrarPanelSignosVitales = false
                    mostrarPanelConfiguracionIa = false
                    mostrarFormulario = false
                    Toast.makeText(context, mensajeBackup, Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    restaurandoBackup = false
                    mensajeBackup = "No se pudo restaurar el backup: ${exception.message ?: "error desconocido"}"
                    Toast.makeText(context, mensajeBackup, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportMedicationReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/rtf")
    ) { uri ->
        val periodo = periodoExportacionPendiente
        val paciente = pacienteActivo
        if (uri == null || periodo == null || paciente == null) {
            exportandoTomas = false
            periodoExportacionPendiente = null
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val rango = calcularRangoExportacion(periodo, fechaEscritorioSeleccionada)
                val medications = database.medicationDao().obtenerTodosPorPacienteLista(paciente.id)
                val intakes = database.medicationIntakeDao().obtenerEnRango(rango.start, rango.end)
                val payload = buildMedicationIntakeRtfDocument(
                    patient = paciente,
                    medications = medications,
                    intakes = intakes,
                    range = rango
                )
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo de destino")
                withContext(Dispatchers.Main) {
                    exportandoTomas = false
                    periodoExportacionPendiente = null
                    Toast.makeText(context, "Informe de tomas exportado correctamente en formato Word compatible", Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exportandoTomas = false
                    periodoExportacionPendiente = null
                    Toast.makeText(context, "No se pudo exportar el informe: ${exception.message ?: "error desconocido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportVitalSignsReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        val request = exportacionSignosPendiente
        if (uri == null || request == null) {
            exportandoSignosVitales = false
            exportacionSignosPendiente = null
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    writeVitalSignsDocxDocument(
                        output = output,
                        report = buildVitalSignsExportReport(
                            records = request.records,
                            patient = pacienteActivo,
                            rangeLabel = request.label
                        )
                    )
                } ?: error("No se pudo abrir el archivo de destino")
                withContext(Dispatchers.Main) {
                    exportandoSignosVitales = false
                    exportacionSignosPendiente = null
                    Toast.makeText(context, "Reporte de seguimiento diario exportado en formato Word", Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exportandoSignosVitales = false
                    exportacionSignosPendiente = null
                    Toast.makeText(context, "No se pudo exportar el reporte: ${exception.message ?: "error desconocido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportClinicalReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        val paciente = pacienteActivo
        if (uri == null || paciente == null) {
            exportandoReporteClinico = false
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val payload = compilarReporteClinico(
                    database = database,
                    paciente = paciente,
                    mesesAtras = 0,
                    incluirAlertas = true,
                    incluirAnticonceptivos = paciente.sexo.equals("Mujer", ignoreCase = true),
                    incluirSignosVitales = true,
                    incluirMedicamentos = true,
                    incluirActividad = true
                )
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    escribirReporteClinicoDocx(output, payload)
                } ?: error("No se pudo abrir el archivo de destino")
                withContext(Dispatchers.Main) {
                    exportandoReporteClinico = false
                    Toast.makeText(context, "Resumen exportado en formato Word", Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exportandoReporteClinico = false
                    Toast.makeText(context, "No se pudo exportar el resumen: ${exception.message ?: "error desconocido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (pickedUri == null) {
            alarmaSonidoUri = ""
            alarmaSonidoNombre = "Alarma predeterminada"
        } else {
            alarmaSonidoUri = pickedUri.toString()
            alarmaSonidoNombre = resolveRingtoneTitle(context, pickedUri)
        }
    }

    return remember(
        notificationPermissionLauncher, exactAlarmPermissionLauncher,
        fullScreenIntentPermissionLauncher, notificationPolicyAccessLauncher,
        pickStudyImagesLauncher, documentScannerInstance, scannerLauncher,
        takeStudyPictureLauncher, cameraPermissionLauncher, pickFotoPerfilLauncher,
        pickRestoreSignosLauncher, takeFotoPerfilLauncher, cameraPermissionPerfilLauncher,
        createBackupDocumentLauncher, restoreBackupDocumentLauncher,
        exportMedicationReportLauncher, exportVitalSignsReportLauncher,
        exportClinicalReportLauncher, ringtonePickerLauncher
    ) {
        FormLaunchers(
            notificationPermissionLauncher = notificationPermissionLauncher,
            exactAlarmPermissionLauncher = exactAlarmPermissionLauncher,
            fullScreenIntentPermissionLauncher = fullScreenIntentPermissionLauncher,
            notificationPolicyAccessLauncher = notificationPolicyAccessLauncher,
            pickStudyImagesLauncher = pickStudyImagesLauncher,
            documentScanner = documentScannerInstance,
            scannerLauncher = scannerLauncher,
            takeStudyPictureLauncher = takeStudyPictureLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            pickFotoPerfilLauncher = pickFotoPerfilLauncher,
            pickRestoreSignosLauncher = pickRestoreSignosLauncher,
            takeFotoPerfilLauncher = takeFotoPerfilLauncher,
            cameraPermissionPerfilLauncher = cameraPermissionPerfilLauncher,
            createBackupDocumentLauncher = createBackupDocumentLauncher,
            restoreBackupDocumentLauncher = restoreBackupDocumentLauncher,
            exportMedicationReportLauncher = exportMedicationReportLauncher,
            exportVitalSignsReportLauncher = exportVitalSignsReportLauncher,
            exportClinicalReportLauncher = exportClinicalReportLauncher,
            ringtonePickerLauncher = ringtonePickerLauncher
        )
    }
}
