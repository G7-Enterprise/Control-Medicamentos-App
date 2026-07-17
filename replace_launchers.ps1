$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)

# Lines 2269-2632 (0-indexed: 2268-2631) = the launcher block to replace
$startRemove = 2268  # 0-indexed
$endRemove   = 2631  # 0-indexed (inclusive)

$replacement = @(
'    val documentScannerOptions = remember {',
'        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()',
'            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)',
'            .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)',
'            .setGalleryImportAllowed(false)',
'            .build()',
'    }',
'    val documentScanner = remember { com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(documentScannerOptions) }',
'    val launchers = rememberMedicamentoFormLaunchers(',
'        context = context,',
'        coroutineScope = coroutineScope,',
'        tienePermisoNotificacionesState = _tienePermisoNotificaciones,',
'        tienePermisoAlarmaExactaState = _tienePermisoAlarmaExacta,',
'        tienePermisoPantallaCompletaState = _tienePermisoPantallaCompleta,',
'        tienePermisoCamaraState = _tienePermisoCamara,',
'        tieneAccesoNoMolestarState = _tieneAccesoNoMolestar,',
'        fotoPerfilPacienteState = _fotoPerfilPaciente,',
'        cameraPermissionPendingState = _cameraPermissionPending,',
'        cameraPermissionPerfilPendingState = _cameraPermissionPerfilPending,',
'        estudiosAdjuntos = estudiosAdjuntos,',
'        adjuntosPendientesReemplazo = adjuntosPendientesReemplazo,',
'        ejecutandoBackupManualState = _ejecutandoBackupManual,',
'        restaurandoBackupState = _restaurandoBackup,',
'        backupSelectionState = _backupSelection,',
'        backupPatientIdState = _backupPatientId,',
'        restoreSelectionState = _restoreSelection,',
'        restorePatientIdState = _restorePatientId,',
'        refrescoBackupState = _refrescoBackup,',
'        mensajeBackupState = _mensajeBackup,',
'        mostrarFichaPacienteState = _mostrarFichaPaciente,',
'        mostrarFormularioInformeState = _mostrarFormularioInforme,',
'        mostrarPanelPacientesState = _mostrarPanelPacientes,',
'        mostrarPanelInformesState = _mostrarPanelInformes,',
'        mostrarListaInsumosState = _mostrarListaInsumos,',
'        mostrarPanelBackupsState = _mostrarPanelBackups,',
'        mostrarPanelConfiguracionAlertasState = _mostrarPanelConfiguracionAlertas,',
'        mostrarPanelSignosVitalesState = _mostrarPanelSignosVitales,',
'        mostrarPanelConfiguracionIaState = _mostrarPanelConfiguracionIa,',
'        mostrarFormularioState = _mostrarFormulario,',
'        intervaloReintentoSeleccionadoState = _intervaloReintentoSeleccionado,',
'        numeroIntentosCriticosSeleccionadoState = _numeroIntentosCriticosSeleccionado,',
'        alarmaSonidoUriState = _alarmaSonidoUri,',
'        alarmaSonidoNombreState = _alarmaSonidoNombre,',
'        periodoExportacionPendienteState = _periodoExportacionPendiente,',
'        exportandoTomasState = _exportandoTomas,',
'        exportacionSignosPendienteState = _exportacionSignosPendiente,',
'        exportandoSignosVitalesState = _exportandoSignosVitales,',
'        exportandoReporteClinicoState = _exportandoReporteClinico,',
'        restaurandoSignosVitalesState = _restaurandoSignosVitales,',
'        pacienteActivo = pacienteActivo,',
'        database = database,',
'        fechaEscritorioSeleccionada = fechaEscritorioSeleccionada,',
'        documentScannerInstance = documentScanner',
'    )',
'    val notificationPermissionLauncher = launchers.notificationPermissionLauncher',
'    val exactAlarmPermissionLauncher = launchers.exactAlarmPermissionLauncher',
'    val fullScreenIntentPermissionLauncher = launchers.fullScreenIntentPermissionLauncher',
'    val notificationPolicyAccessLauncher = launchers.notificationPolicyAccessLauncher',
'    val pickStudyImagesLauncher = launchers.pickStudyImagesLauncher',
'    val scannerLauncher = launchers.scannerLauncher',
'    val takeStudyPictureLauncher = launchers.takeStudyPictureLauncher',
'    val cameraPermissionLauncher = launchers.cameraPermissionLauncher',
'    val pickFotoPerfilLauncher = launchers.pickFotoPerfilLauncher',
'    val pickRestoreSignosLauncher = launchers.pickRestoreSignosLauncher',
'    val takeFotoPerfilLauncher = launchers.takeFotoPerfilLauncher',
'    val cameraPermissionPerfilLauncher = launchers.cameraPermissionPerfilLauncher',
'    val createBackupDocumentLauncher = launchers.createBackupDocumentLauncher',
'    val restoreBackupDocumentLauncher = launchers.restoreBackupDocumentLauncher',
'    val exportMedicationReportLauncher = launchers.exportMedicationReportLauncher',
'    val exportVitalSignsReportLauncher = launchers.exportVitalSignsReportLauncher',
'    val exportClinicalReportLauncher = launchers.exportClinicalReportLauncher',
'    val ringtonePickerLauncher = launchers.ringtonePickerLauncher'
)

$newLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($i -eq $startRemove) {
        foreach ($r in $replacement) { $newLines.Add($r) }
    } elseif ($i -gt $startRemove -and $i -le $endRemove) {
        # skip
    } else {
        $newLines.Add($lines[$i])
    }
}

[System.IO.File]::WriteAllLines($f, $newLines, [System.Text.Encoding]::UTF8)
Write-Host "Done. File now has $($newLines.Count) lines."
