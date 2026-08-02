package com.carlos.controlmedicamentos.backup

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.room.withTransaction
import com.carlos.controlmedicamentos.BuildConfig
import com.carlos.controlmedicamentos.CountryCurrencyCatalog
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MedicationIntake
import com.carlos.controlmedicamentos.data.local.MedicationOrder
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.data.local.SignosVitales
import com.carlos.controlmedicamentos.data.local.VaccinationRecord
import com.carlos.controlmedicamentos.data.local.PhysicalActivity
import com.carlos.controlmedicamentos.data.local.CarritoPendienteItem
import com.carlos.controlmedicamentos.data.local.CicloMenstrual
import com.carlos.controlmedicamentos.data.local.RegistroDiarioCiclo
import com.carlos.controlmedicamentos.data.local.ControlEmbarazo
import com.carlos.controlmedicamentos.data.local.VisitaPrenatal
import com.carlos.controlmedicamentos.data.local.MetodoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.AnticonceptivoIntake
import com.carlos.controlmedicamentos.data.local.BebeRecienNacido
import com.carlos.controlmedicamentos.data.local.NinoEntity
import com.carlos.controlmedicamentos.data.local.VacunaEntity
import com.carlos.controlmedicamentos.data.local.ControlPediatricoEntity
import com.carlos.controlmedicamentos.data.local.EnfermedadEntity
import com.carlos.controlmedicamentos.data.local.DiarioEntry
import com.carlos.controlmedicamentos.data.local.Dentista
import com.carlos.controlmedicamentos.data.local.VisitaDentista
import com.carlos.controlmedicamentos.data.local.DiagnosticoDental
import com.carlos.controlmedicamentos.data.local.ProcedimientoDental
import com.carlos.controlmedicamentos.data.local.PrescripcionDental
import com.carlos.controlmedicamentos.data.local.DienteEstado
import com.carlos.controlmedicamentos.data.local.ImagenDental
import com.carlos.controlmedicamentos.data.local.TransaccionDental
import com.carlos.controlmedicamentos.data.local.Ortodoncia
import com.carlos.controlmedicamentos.data.local.AjusteOrtodoncia
import com.carlos.controlmedicamentos.data.local.IncidenciaOrtodoncia
import com.carlos.controlmedicamentos.data.local.ElasticoOrtodoncia
import com.carlos.controlmedicamentos.data.local.RegistroSedentarismo
import com.carlos.controlmedicamentos.data.local.ConfigSedentarismo
import com.carlos.controlmedicamentos.data.local.RegistroHidratacion
import com.carlos.controlmedicamentos.data.local.FallAlert
import com.carlos.controlmedicamentos.data.local.FALL_STATUS_DETECTED
import com.carlos.controlmedicamentos.notifications.CriticalAlertConfig
import com.carlos.controlmedicamentos.data.local.TipoAnticonceptivo
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import com.carlos.controlmedicamentos.notifications.VaccinationScheduler
import com.carlos.controlmedicamentos.sync.buildAndroidSyncSnapshot
import com.carlos.controlmedicamentos.sync.model.SyncDeviceInfo
import com.carlos.controlmedicamentos.sync.model.SyncMedicalAppointment
import com.carlos.controlmedicamentos.sync.model.SyncMedicalPractitioner
import com.carlos.controlmedicamentos.sync.model.SyncMedicalReport
import com.carlos.controlmedicamentos.sync.model.SyncMedication
import com.carlos.controlmedicamentos.sync.model.SyncMedicationIntake
import com.carlos.controlmedicamentos.sync.model.SyncPatient
import com.carlos.controlmedicamentos.sync.model.SyncSnapshot
import com.carlos.controlmedicamentos.sync.model.SyncVaccinationRecord
import com.carlos.controlmedicamentos.sync.model.SyncVitalSigns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

private const val RESTORED_ATTACHMENTS_DIR = "restored_report_attachments"

data class BackupSummary(
    val patients: Int,
    val medications: Int,
    val reports: Int,
    val vitalSigns: Int,
    val intakes: Int = 0,
    val appointments: Int = 0,
    val practitioners: Int = 0,
    val vaccinations: Int = 0,
    val medicationOrders: Int = 0,
    val physicalActivities: Int = 0,
    val carritoPendiente: Int = 0,
    val ciclosMenstruales: Int = 0,
    val registrosDiarioCiclo: Int = 0,
    val controlesEmbarazo: Int = 0,
    val visitasPrenatales: Int = 0,
    val metodosAnticonceptivos: Int = 0,
    val anticonceptivoIntakes: Int = 0,
    val bebesRecienNacidos: Int = 0,
    val ninos: Int = 0,
    val vacunas: Int = 0,
    val controlesPediatricos: Int = 0,
    val enfermedades: Int = 0,
    val diarioEntries: Int = 0,
    val dentistas: Int = 0,
    val visitasDentista: Int = 0,
    val diagnosticosDentales: Int = 0,
    val procedimientosDentales: Int = 0,
    val prescripcionesDentales: Int = 0,
    val dientesEstado: Int = 0,
    val imagenesDentales: Int = 0,
    val transaccionesDentales: Int = 0,
    val ortodoncias: Int = 0,
    val ajustesOrtodoncia: Int = 0,
    val incidenciasOrtodoncia: Int = 0,
    val elasticosOrtodoncia: Int = 0,
    val registrosSedentarismo: Int = 0,
    val configSedentarismo: Int = 0,
    val registrosHidratacion: Int = 0,
    val fallAlerts: Int = 0
)

data class BackupSelection(
    val patients: Boolean = true,
    val medications: Boolean = true,
    val reports: Boolean = true,
    val vitalSigns: Boolean = true,
    val intakes: Boolean = true,
    val appointments: Boolean = true,
    val practitioners: Boolean = true,
    val vaccinations: Boolean = true,
    val medicationOrders: Boolean = true,
    val physicalActivities: Boolean = true,
    val carritoPendiente: Boolean = true,
    val ciclosMenstruales: Boolean = true,
    val registrosDiarioCiclo: Boolean = true,
    val controlesEmbarazo: Boolean = true,
    val visitasPrenatales: Boolean = true,
    val metodosAnticonceptivos: Boolean = true,
    val anticonceptivoIntakes: Boolean = true,
    val bebesRecienNacidos: Boolean = true,
    val ninos: Boolean = true,
    val vacunas: Boolean = true,
    val controlesPediatricos: Boolean = true,
    val enfermedades: Boolean = true,
    val diarioEntries: Boolean = true,
    val dentistas: Boolean = true,
    val visitasDentista: Boolean = true,
    val diagnosticosDentales: Boolean = true,
    val procedimientosDentales: Boolean = true,
    val prescripcionesDentales: Boolean = true,
    val dientesEstado: Boolean = true,
    val imagenesDentales: Boolean = true,
    val transaccionesDentales: Boolean = true,
    val ortodoncias: Boolean = true,
    val ajustesOrtodoncia: Boolean = true,
    val incidenciasOrtodoncia: Boolean = true,
    val elasticosOrtodoncia: Boolean = true,
    val registrosSedentarismo: Boolean = true,
    val configSedentarismo: Boolean = true,
    val registrosHidratacion: Boolean = true,
    val fallAlerts: Boolean = true
) {
    companion object {
        fun all() = BackupSelection()
        fun none() = BackupSelection(
            patients = false,
            medications = false,
            reports = false,
            vitalSigns = false,
            intakes = false,
            appointments = false,
            practitioners = false,
            vaccinations = false,
            medicationOrders = false,
            physicalActivities = false,
            carritoPendiente = false,
            ciclosMenstruales = false,
            registrosDiarioCiclo = false,
            controlesEmbarazo = false,
            visitasPrenatales = false,
            metodosAnticonceptivos = false,
            anticonceptivoIntakes = false,
            bebesRecienNacidos = false,
            ninos = false,
            vacunas = false,
            controlesPediatricos = false,
            enfermedades = false,
            diarioEntries = false,
            dentistas = false,
            visitasDentista = false,
            diagnosticosDentales = false,
            procedimientosDentales = false,
            prescripcionesDentales = false,
            dientesEstado = false,
            imagenesDentales = false,
            transaccionesDentales = false,
            ortodoncias = false,
            ajustesOrtodoncia = false,
            incidenciasOrtodoncia = false,
            elasticosOrtodoncia = false,
            registrosSedentarismo = false,
            configSedentarismo = false,
            registrosHidratacion = false,
            fallAlerts = false
        )
    }
}

object BackupManager {
    private const val SCHEMA_VERSION = 8
    private const val AUTO_BACKUP_DIR = "backups"
    private const val AUTO_BACKUP_FILE = "controlmedicamentos-auto-backup.json"

    suspend fun exportManualBackup(context: Context, backupUri: Uri, selection: BackupSelection = BackupSelection.all(), patientId: Int? = null): BackupSummary = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val payload = buildBackupJson(context, database, selection, patientId)
        context.contentResolver.openOutputStream(backupUri)?.use { output ->
            output.write(payload.toString(2).toByteArray())
        } ?: error("No se pudo abrir el destino del backup")
        summarize(payload)
    }

    suspend fun importManualBackup(context: Context, backupUri: Uri, selection: BackupSelection = BackupSelection.all(), patientId: Int? = null): BackupSummary = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val content = context.contentResolver.openInputStream(backupUri)?.bufferedReader()?.use { it.readText() }
            ?: error("No se pudo leer el archivo de backup")
        val json = JSONObject(content)
        restoreFromJson(context, database, json, selection, patientId)
    }

    suspend fun createAutomaticBackup(context: Context): File = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val payload = buildBackupJson(context, database)
        val directory = File(context.filesDir, AUTO_BACKUP_DIR).apply { mkdirs() }
        val file = File(directory, AUTO_BACKUP_FILE)
        file.writeText(payload.toString(2))
        file
    }

    fun latestAutomaticBackupFile(context: Context): File? {
        val file = File(File(context.filesDir, AUTO_BACKUP_DIR), AUTO_BACKUP_FILE)
        return file.takeIf { it.exists() }
    }

    private suspend fun buildBackupJson(context: Context, database: AppDatabase, selection: BackupSelection = BackupSelection.all(), patientId: Int? = null): JSONObject {
        val patients = if (selection.patients) {
            if (patientId != null) database.patientProfileDao().obtenerTodosLista().filter { it.id == patientId }
            else database.patientProfileDao().obtenerTodosLista()
        } else emptyList()
        val medications = if (selection.medications) {
            if (patientId != null) database.medicationDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.medicationDao().obtenerTodosLista()
        } else emptyList()
        val intakes = if (selection.intakes) {
            if (patientId != null) database.medicationIntakeDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.medicationIntakeDao().obtenerTodosLista()
        } else emptyList()
        val reports = if (selection.reports) {
            if (patientId != null) database.medicalReportDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.medicalReportDao().obtenerTodosLista()
        } else emptyList()
        val vitalSigns = if (selection.vitalSigns) {
            if (patientId != null) database.signosVitalesDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.signosVitalesDao().obtenerTodosLista()
        } else emptyList()
        val appointments = if (selection.appointments) {
            if (patientId != null) database.medicalAppointmentDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.medicalAppointmentDao().obtenerTodosLista()
        } else emptyList()
        val practitioners = if (selection.practitioners) database.medicalPractitionerDao().obtenerTodosLista() else emptyList()
        val vaccinations = if (selection.vaccinations) {
            if (patientId != null) database.vaccinationRecordDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.vaccinationRecordDao().obtenerTodosLista()
        } else emptyList()
        val medicationOrders = if (selection.medicationOrders) {
            if (patientId != null) database.medicationOrderDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.medicationOrderDao().obtenerTodosLista()
        } else emptyList()
        val physicalActivities = if (selection.physicalActivities) {
            if (patientId != null) database.physicalActivityDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.physicalActivityDao().obtenerTodosLista()
        } else emptyList()
        val carritoItems = if (selection.carritoPendiente) database.carritoPendienteDao().obtenerTodosLista() else emptyList()
        val ciclos = if (selection.ciclosMenstruales) database.cicloMenstrualDao().obtenerTodosLista() else emptyList()
        val registrosCiclo = if (selection.registrosDiarioCiclo) database.registroDiarioCicloDao().obtenerTodosLista() else emptyList()
        val embarazos = if (selection.controlesEmbarazo) {
            if (patientId != null) database.controlEmbarazoDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.controlEmbarazoDao().obtenerTodosLista()
        } else emptyList()
        val visitas = if (selection.visitasPrenatales) database.visitaPrenatalDao().obtenerTodosLista() else emptyList()
        val metodos = if (selection.metodosAnticonceptivos) database.metodoAnticonceptivoDao().obtenerTodosLista() else emptyList()
        val anticonceptivoIntakes = if (selection.anticonceptivoIntakes) database.anticonceptivoIntakeDao().obtenerTodosLista() else emptyList()
        val bebes = if (selection.bebesRecienNacidos) {
            if (patientId != null) database.bebeRecienNacidoDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.bebeRecienNacidoDao().obtenerTodosLista()
        } else emptyList()
        val ninos = if (selection.ninos) {
            if (patientId != null) database.ninoDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.ninoDao().obtenerTodosLista()
        } else emptyList()
        val vacunas = if (selection.vacunas) database.vacunaDao().obtenerTodosLista() else emptyList()
        val controlesPediatricos = if (selection.controlesPediatricos) database.controlPediatricoDao().obtenerTodosLista() else emptyList()
        val enfermedades = if (selection.enfermedades) database.enfermedadDao().obtenerTodosLista() else emptyList()
        val diarioEntries = if (selection.diarioEntries) {
            if (patientId != null) database.diarioEntryDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.diarioEntryDao().obtenerTodosLista()
        } else emptyList()
        val dentistas = if (selection.dentistas) {
            if (patientId != null) database.dentistaDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.dentistaDao().obtenerTodosLista()
        } else emptyList()
        val visitasDentista = if (selection.visitasDentista) {
            if (patientId != null) database.visitaDentistaDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.visitaDentistaDao().obtenerTodosLista()
        } else emptyList()
        val diagnosticosDentales = if (selection.diagnosticosDentales) {
            if (patientId != null) database.diagnosticoDentalDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.diagnosticoDentalDao().obtenerTodosLista()
        } else emptyList()
        val procedimientosDentales = if (selection.procedimientosDentales) {
            if (patientId != null) database.procedimientoDentalDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.procedimientoDentalDao().obtenerTodosLista()
        } else emptyList()
        val prescripcionesDentales = if (selection.prescripcionesDentales) {
            if (patientId != null) database.prescripcionDentalDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.prescripcionDentalDao().obtenerTodosLista()
        } else emptyList()
        val dientesEstado = if (selection.dientesEstado) {
            if (patientId != null) database.dienteEstadoDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.dienteEstadoDao().obtenerTodosLista()
        } else emptyList()
        val imagenesDentales = if (selection.imagenesDentales) {
            if (patientId != null) database.imagenDentalDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.imagenDentalDao().obtenerTodosLista()
        } else emptyList()
        val transaccionesDentales = if (selection.transaccionesDentales) {
            if (patientId != null) database.transaccionDentalDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.transaccionDentalDao().obtenerTodosLista()
        } else emptyList()
        val ortodoncias = if (selection.ortodoncias) {
            if (patientId != null) database.ortodonciaDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.ortodonciaDao().obtenerTodosLista()
        } else emptyList()
        val ajustesOrtodoncia = if (selection.ajustesOrtodoncia) database.ajusteOrtodonciaDao().obtenerTodosLista() else emptyList()
        val incidenciasOrtodoncia = if (selection.incidenciasOrtodoncia) database.incidenciaOrtodonciaDao().obtenerTodosLista() else emptyList()
        val elasticosOrtodoncia = if (selection.elasticosOrtodoncia) database.elasticoOrtodonciaDao().obtenerTodosLista() else emptyList()
        val registrosSedentarismo = if (selection.registrosSedentarismo) {
            if (patientId != null) database.sedentarismoDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.sedentarismoDao().obtenerTodosLista()
        } else emptyList()
        val configsSedentarismo = if (selection.configSedentarismo) database.sedentarismoDao().obtenerTodosConfig() else emptyList()
        val registrosHidratacion = if (selection.registrosHidratacion) {
            if (patientId != null) database.hidratacionDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.hidratacionDao().obtenerTodosLista()
        } else emptyList()
        val fallAlerts = if (selection.fallAlerts) {
            if (patientId != null) database.fallAlertDao().obtenerTodosLista().filter { it.patientId == patientId }
            else database.fallAlertDao().obtenerTodosLista()
        } else emptyList()
        val criticalAlertConfig = CriticalAlertSettings.load(context)
        val exportedAt = System.currentTimeMillis()
        val syncSnapshot = buildAndroidSyncSnapshot(
            deviceId = resolveAndroidDeviceId(context),
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = exportedAt,
            patients = patients,
            medications = medications,
            medicationIntakes = intakes,
            reports = reports,
            appointments = appointments,
            practitioners = practitioners,
            vaccinations = vaccinations,
            vitalSigns = vitalSigns
        )

        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", exportedAt)
            .put("criticalAlertSettings", criticalAlertConfig.toJson())
            .put("syncSnapshot", syncSnapshot.toJson())
            .put("patients", JSONArray().apply { patients.forEach { put(it.toJson(context)) } })
            .put("medications", JSONArray().apply { medications.forEach { put(it.toJson()) } })
            .put("intakes", JSONArray().apply { intakes.forEach { put(it.toJson()) } })
            .put("reports", JSONArray().apply { reports.forEach { put(it.toJson(context)) } })
            .put("vitalSigns", JSONArray().apply { vitalSigns.forEach { put(it.toJson()) } })
            .put("appointments", JSONArray().apply { appointments.forEach { put(it.toJson()) } })
            .put("practitioners", JSONArray().apply { practitioners.forEach { put(it.toJson()) } })
            .put("vaccinations", JSONArray().apply { vaccinations.forEach { put(it.toJson()) } })
            .put("medicationOrders", JSONArray().apply { medicationOrders.forEach { put(it.toJson()) } })
            .put("physicalActivities", JSONArray().apply { physicalActivities.forEach { put(it.toJson()) } })
            .put("carritoPendiente", JSONArray().apply { carritoItems.forEach { put(it.toJson()) } })
            .put("ciclosMenstruales", JSONArray().apply { ciclos.forEach { put(it.toJson()) } })
            .put("registrosDiarioCiclo", JSONArray().apply { registrosCiclo.forEach { put(it.toJson()) } })
            .put("controlesEmbarazo", JSONArray().apply { embarazos.forEach { put(it.toJson()) } })
            .put("visitasPrenatales", JSONArray().apply { visitas.forEach { put(it.toJson()) } })
            .put("metodosAnticonceptivos", JSONArray().apply { metodos.forEach { put(it.toJson()) } })
            .put("anticonceptivoIntakes", JSONArray().apply { anticonceptivoIntakes.forEach { put(it.toJson()) } })
            .put("bebesRecienNacidos", JSONArray().apply { bebes.forEach { put(it.toJson()) } })
            .put("ninos", JSONArray().apply { ninos.forEach { put(it.toJson()) } })
            .put("vacunas", JSONArray().apply { vacunas.forEach { put(it.toJson()) } })
            .put("controlesPediatricos", JSONArray().apply { controlesPediatricos.forEach { put(it.toJson()) } })
            .put("enfermedades", JSONArray().apply { enfermedades.forEach { put(it.toJson()) } })
            .put("diarioEntries", JSONArray().apply { diarioEntries.forEach { put(it.toJson(context)) } })
            .put("dentistas", JSONArray().apply { dentistas.forEach { put(it.toJson()) } })
            .put("visitasDentista", JSONArray().apply { visitasDentista.forEach { put(it.toJson()) } })
            .put("diagnosticosDentales", JSONArray().apply { diagnosticosDentales.forEach { put(it.toJson()) } })
            .put("procedimientosDentales", JSONArray().apply { procedimientosDentales.forEach { put(it.toJson()) } })
            .put("prescripcionesDentales", JSONArray().apply { prescripcionesDentales.forEach { put(it.toJson()) } })
            .put("dientesEstado", JSONArray().apply { dientesEstado.forEach { put(it.toJson()) } })
            .put("imagenesDentales", JSONArray().apply { imagenesDentales.forEach { put(it.toJson(context)) } })
            .put("transaccionesDentales", JSONArray().apply { transaccionesDentales.forEach { put(it.toJson()) } })
            .put("ortodoncias", JSONArray().apply { ortodoncias.forEach { put(it.toJson()) } })
            .put("ajustesOrtodoncia", JSONArray().apply { ajustesOrtodoncia.forEach { put(it.toJson()) } })
            .put("incidenciasOrtodoncia", JSONArray().apply { incidenciasOrtodoncia.forEach { put(it.toJson()) } })
            .put("elasticosOrtodoncia", JSONArray().apply { elasticosOrtodoncia.forEach { put(it.toJson()) } })
            .put("registrosSedentarismo", JSONArray().apply { registrosSedentarismo.forEach { put(it.toJson()) } })
            .put("configSedentarismo", JSONArray().apply { configsSedentarismo.forEach { put(it.toJson()) } })
            .put("registrosHidratacion", JSONArray().apply { registrosHidratacion.forEach { put(it.toJson()) } })
            .put("fallAlerts", JSONArray().apply { fallAlerts.forEach { put(it.toJson()) } })
    }

    private suspend fun restoreFromJson(context: Context, database: AppDatabase, json: JSONObject, selection: BackupSelection = BackupSelection.all(), patientId: Int? = null): BackupSummary {
        val syncSnapshot = json.optJSONObject("syncSnapshot")?.toSyncSnapshot()
        val patients = json.optJSONArray("patients")?.toPatientProfiles(context)
            ?: syncSnapshot?.patients?.map(SyncPatient::toEntity)
            ?: emptyList()
        val medications = (json.optJSONArray("medications")?.toMedications()
            ?: syncSnapshot?.medications?.map(SyncMedication::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val intakes = (json.optJSONArray("intakes")?.toMedicationIntakes()
            ?: syncSnapshot?.medicationIntakes?.map(SyncMedicationIntake::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val reports = (json.optJSONArray("reports")?.toMedicalReports(context)
            ?: syncSnapshot?.reports?.map(SyncMedicalReport::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val vitalSigns = (json.optJSONArray("vitalSigns")?.toVitalSigns()
            ?: syncSnapshot?.vitalSigns?.map(SyncVitalSigns::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val appointments = (json.optJSONArray("appointments")?.toMedicalAppointments()
            ?: syncSnapshot?.appointments?.map(SyncMedicalAppointment::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val practitioners = json.optJSONArray("practitioners")?.toMedicalPractitioners()
            ?: syncSnapshot?.practitioners?.map(SyncMedicalPractitioner::toEntity)
            ?: emptyList()
        val vaccinations = (json.optJSONArray("vaccinations")?.toVaccinationRecords()
            ?: syncSnapshot?.vaccinations?.map(SyncVaccinationRecord::toEntity)
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val medicationOrders = json.optJSONArray("medicationOrders").toMedicationOrders()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val physicalActivities = json.optJSONArray("physicalActivities").toPhysicalActivities()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val carritoItems = json.optJSONArray("carritoPendiente").toCarritoPendienteItems()
        val ciclos = json.optJSONArray("ciclosMenstruales").toCiclosMenstruales()
        val registrosCiclo = json.optJSONArray("registrosDiarioCiclo").toRegistrosDiarioCiclo()
        val embarazos = json.optJSONArray("controlesEmbarazo").toControlesEmbarazo()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val visitas = json.optJSONArray("visitasPrenatales").toVisitasPrenatales()
        val metodos = json.optJSONArray("metodosAnticonceptivos").toMetodosAnticonceptivos()
        val anticonceptivoIntakes = json.optJSONArray("anticonceptivoIntakes").toAnticonceptivoIntakes()
        val bebes = json.optJSONArray("bebesRecienNacidos").toBebesRecienNacidos()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val ninos = json.optJSONArray("ninos").toNinos()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val vacunas = json.optJSONArray("vacunas").toVacunas()
        val controlesPediatricos = json.optJSONArray("controlesPediatricos").toControlesPediatricos()
        val enfermedades = json.optJSONArray("enfermedades").toEnfermedades()
        val diarioEntries = json.optJSONArray("diarioEntries").toDiarioEntries(context)
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val dentistas = json.optJSONArray("dentistas").toDentistas()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val visitasDentista = json.optJSONArray("visitasDentista").toVisitasDentista()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val diagnosticosDentales = json.optJSONArray("diagnosticosDentales").toDiagnosticosDentales()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val procedimientosDentales = json.optJSONArray("procedimientosDentales").toProcedimientosDentales()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val prescripcionesDentales = json.optJSONArray("prescripcionesDentales").toPrescripcionesDentales()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val dientesEstado = json.optJSONArray("dientesEstado").toDientesEstado()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val imagenesDentales = json.optJSONArray("imagenesDentales").toImagenesDentales(context)
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val transaccionesDentales = json.optJSONArray("transaccionesDentales").toTransaccionesDentales()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val ortodoncias = json.optJSONArray("ortodoncias").toOrtodoncias()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val ajustesOrtodoncia = json.optJSONArray("ajustesOrtodoncia").toAjustesOrtodoncia()
        val incidenciasOrtodoncia = json.optJSONArray("incidenciasOrtodoncia").toIncidenciasOrtodoncia()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val elasticosOrtodoncia = json.optJSONArray("elasticosOrtodoncia").toElasticosOrtodoncia()
        val registrosSedentarismo = json.optJSONArray("registrosSedentarismo").toRegistrosSedentarismo()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val configsSedentarismo = json.optJSONArray("configSedentarismo").toConfigsSedentarismo()
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val registrosHidratacion = (json.optJSONArray("registrosHidratacion")?.toRegistrosHidratacion()
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val fallAlerts = (json.optJSONArray("fallAlerts")?.toFallAlerts()
            ?: emptyList())
            .map { if (patientId != null) it.copy(patientId = patientId) else it }
        val criticalAlertConfig = json.optJSONObject("criticalAlertSettings")?.toCriticalAlertConfig()
            ?: medications.firstOrNull()?.let {
                CriticalAlertConfig(
                    retryIntervalMinutes = it.retryIntervalMinutes,
                    soundUri = it.alarmaSonidoUri
                )
            }
            ?: CriticalAlertConfig()

        database.withTransaction {
            if (selection.diarioEntries) database.diarioEntryDao().eliminarTodos()
            if (selection.enfermedades) database.enfermedadDao().eliminarTodos()
            if (selection.controlesPediatricos) database.controlPediatricoDao().eliminarTodos()
            if (selection.vacunas) database.vacunaDao().eliminarTodos()
            if (selection.ninos) database.ninoDao().eliminarTodos()
            if (selection.bebesRecienNacidos) database.bebeRecienNacidoDao().eliminarTodos()
            if (selection.anticonceptivoIntakes) database.anticonceptivoIntakeDao().eliminarTodos()
            if (selection.metodosAnticonceptivos) database.metodoAnticonceptivoDao().eliminarTodos()
            if (selection.visitasPrenatales) database.visitaPrenatalDao().eliminarTodos()
            if (selection.controlesEmbarazo) database.controlEmbarazoDao().eliminarTodos()
            if (selection.registrosDiarioCiclo) database.registroDiarioCicloDao().eliminarTodos()
            if (selection.ciclosMenstruales) database.cicloMenstrualDao().eliminarTodos()
            if (selection.carritoPendiente) database.carritoPendienteDao().eliminarTodos()
            if (selection.physicalActivities) database.physicalActivityDao().eliminarTodos()
            if (selection.intakes) database.medicationIntakeDao().eliminarTodos()
            if (selection.vitalSigns) database.signosVitalesDao().eliminarTodos()
            if (selection.reports) database.medicalReportDao().eliminarTodos()
            if (selection.appointments) database.medicalAppointmentDao().eliminarTodos()
            if (selection.practitioners) database.medicalPractitionerDao().eliminarTodos()
            if (selection.vaccinations) database.vaccinationRecordDao().eliminarTodos()
            if (selection.medicationOrders) database.medicationOrderDao().eliminarTodos()
            if (selection.medications) database.medicationDao().eliminarTodos()
            if (selection.patients) database.patientProfileDao().eliminarTodos()
            if (selection.dentistas) database.dentistaDao().eliminarTodos()
            if (selection.visitasDentista) database.visitaDentistaDao().eliminarTodos()
            if (selection.diagnosticosDentales) database.diagnosticoDentalDao().eliminarTodos()
            if (selection.procedimientosDentales) database.procedimientoDentalDao().eliminarTodos()
            if (selection.prescripcionesDentales) database.prescripcionDentalDao().eliminarTodos()
            if (selection.dientesEstado) database.dienteEstadoDao().eliminarTodos()
            if (selection.imagenesDentales) database.imagenDentalDao().eliminarTodos()
            if (selection.transaccionesDentales) database.transaccionDentalDao().eliminarTodos()
            if (selection.ortodoncias) database.ortodonciaDao().eliminarTodos()
            if (selection.ajustesOrtodoncia) database.ajusteOrtodonciaDao().eliminarTodos()
            if (selection.incidenciasOrtodoncia) database.incidenciaOrtodonciaDao().eliminarTodos()
            if (selection.elasticosOrtodoncia) database.elasticoOrtodonciaDao().eliminarTodos()
            if (selection.registrosSedentarismo) database.sedentarismoDao().eliminarTodos()
            if (selection.configSedentarismo) database.sedentarismoDao().eliminarTodaConfig()
            if (selection.registrosHidratacion) database.hidratacionDao().eliminarTodos()
            if (selection.fallAlerts) database.fallAlertDao().eliminarTodos()

            if (selection.patients && patients.isNotEmpty()) database.patientProfileDao().guardarTodos(patients)
            if (selection.medications && medications.isNotEmpty()) database.medicationDao().guardarTodos(medications)
            if (selection.intakes && intakes.isNotEmpty()) database.medicationIntakeDao().guardarTodos(intakes)
            if (selection.reports && reports.isNotEmpty()) database.medicalReportDao().guardarTodos(reports)
            if (selection.vitalSigns && vitalSigns.isNotEmpty()) database.signosVitalesDao().guardarTodos(vitalSigns)
            if (selection.practitioners && practitioners.isNotEmpty()) database.medicalPractitionerDao().guardarTodos(practitioners)
            if (selection.appointments && appointments.isNotEmpty()) database.medicalAppointmentDao().guardarTodos(appointments)
            if (selection.vaccinations && vaccinations.isNotEmpty()) database.vaccinationRecordDao().guardarTodos(vaccinations)
            if (selection.medicationOrders && medicationOrders.isNotEmpty()) database.medicationOrderDao().guardarTodos(medicationOrders)
            if (selection.physicalActivities && physicalActivities.isNotEmpty()) database.physicalActivityDao().guardarTodos(physicalActivities)
            if (selection.carritoPendiente && carritoItems.isNotEmpty()) database.carritoPendienteDao().guardarTodos(carritoItems)
            if (selection.ciclosMenstruales && ciclos.isNotEmpty()) database.cicloMenstrualDao().guardarTodos(ciclos)
            if (selection.registrosDiarioCiclo && registrosCiclo.isNotEmpty()) database.registroDiarioCicloDao().guardarTodos(registrosCiclo)
            if (selection.controlesEmbarazo && embarazos.isNotEmpty()) database.controlEmbarazoDao().guardarTodos(embarazos)
            if (selection.visitasPrenatales && visitas.isNotEmpty()) database.visitaPrenatalDao().guardarTodos(visitas)
            if (selection.metodosAnticonceptivos && metodos.isNotEmpty()) database.metodoAnticonceptivoDao().guardarTodos(metodos)
            if (selection.anticonceptivoIntakes && anticonceptivoIntakes.isNotEmpty()) database.anticonceptivoIntakeDao().guardarTodos(anticonceptivoIntakes)
            if (selection.bebesRecienNacidos && bebes.isNotEmpty()) database.bebeRecienNacidoDao().guardarTodos(bebes)
            if (selection.ninos && ninos.isNotEmpty()) database.ninoDao().guardarTodos(ninos)
            if (selection.vacunas && vacunas.isNotEmpty()) database.vacunaDao().guardarTodos(vacunas)
            if (selection.controlesPediatricos && controlesPediatricos.isNotEmpty()) database.controlPediatricoDao().guardarTodos(controlesPediatricos)
            if (selection.enfermedades && enfermedades.isNotEmpty()) database.enfermedadDao().guardarTodos(enfermedades)
            if (selection.diarioEntries && diarioEntries.isNotEmpty()) database.diarioEntryDao().guardarTodos(diarioEntries)
            if (selection.dentistas && dentistas.isNotEmpty()) database.dentistaDao().guardarTodos(dentistas)
            if (selection.visitasDentista && visitasDentista.isNotEmpty()) database.visitaDentistaDao().guardarTodos(visitasDentista)
            if (selection.diagnosticosDentales && diagnosticosDentales.isNotEmpty()) database.diagnosticoDentalDao().guardarTodos(diagnosticosDentales)
            if (selection.procedimientosDentales && procedimientosDentales.isNotEmpty()) database.procedimientoDentalDao().guardarTodos(procedimientosDentales)
            if (selection.prescripcionesDentales && prescripcionesDentales.isNotEmpty()) database.prescripcionDentalDao().guardarTodos(prescripcionesDentales)
            if (selection.dientesEstado && dientesEstado.isNotEmpty()) database.dienteEstadoDao().guardarTodos(dientesEstado)
            if (selection.imagenesDentales && imagenesDentales.isNotEmpty()) database.imagenDentalDao().guardarTodos(imagenesDentales)
            if (selection.transaccionesDentales && transaccionesDentales.isNotEmpty()) database.transaccionDentalDao().guardarTodos(transaccionesDentales)
            if (selection.ortodoncias && ortodoncias.isNotEmpty()) database.ortodonciaDao().guardarTodos(ortodoncias)
            if (selection.ajustesOrtodoncia && ajustesOrtodoncia.isNotEmpty()) database.ajusteOrtodonciaDao().guardarTodos(ajustesOrtodoncia)
            if (selection.incidenciasOrtodoncia && incidenciasOrtodoncia.isNotEmpty()) database.incidenciaOrtodonciaDao().guardarTodos(incidenciasOrtodoncia)
            if (selection.elasticosOrtodoncia && elasticosOrtodoncia.isNotEmpty()) database.elasticoOrtodonciaDao().guardarTodos(elasticosOrtodoncia)
            if (selection.registrosSedentarismo && registrosSedentarismo.isNotEmpty()) database.sedentarismoDao().guardarTodos(registrosSedentarismo)
            if (selection.configSedentarismo && configsSedentarismo.isNotEmpty()) database.sedentarismoDao().guardarTodosConfig(configsSedentarismo)
            if (selection.registrosHidratacion && registrosHidratacion.isNotEmpty()) database.hidratacionDao().guardarTodos(registrosHidratacion)
            if (selection.fallAlerts && fallAlerts.isNotEmpty()) database.fallAlertDao().guardarTodos(fallAlerts)
        }

        CriticalAlertSettings.save(context, criticalAlertConfig)
        database.medicationDao().actualizarConfiguracionAlertasCriticas(
            retryIntervalMinutes = criticalAlertConfig.retryIntervalMinutes,
            alarmaSonidoUri = criticalAlertConfig.soundUri
        )

        val scheduler = MedicationScheduler(context)
        val appointmentScheduler = MedicalAppointmentScheduler(context)
        val vaccinationScheduler = VaccinationScheduler(context)
        database.medicationDao().obtenerActivosConAlarma().forEach { scheduler.programarAlarmas(it) }
        database.medicalAppointmentDao().obtenerPendientesConAlarma(System.currentTimeMillis()).forEach {
            appointmentScheduler.programar(it)
        }
        database.vaccinationRecordDao().obtenerPendientesConAlarma(System.currentTimeMillis()).forEach {
            vaccinationScheduler.programar(it)
        }

        return BackupSummary(
            patients = patients.size,
            medications = medications.size,
            reports = reports.size,
            vitalSigns = vitalSigns.size,
            intakes = intakes.size,
            appointments = appointments.size,
            practitioners = practitioners.size,
            vaccinations = vaccinations.size,
            medicationOrders = medicationOrders.size,
            physicalActivities = physicalActivities.size,
            carritoPendiente = carritoItems.size,
            ciclosMenstruales = ciclos.size,
            registrosDiarioCiclo = registrosCiclo.size,
            controlesEmbarazo = embarazos.size,
            visitasPrenatales = visitas.size,
            metodosAnticonceptivos = metodos.size,
            anticonceptivoIntakes = anticonceptivoIntakes.size,
            bebesRecienNacidos = bebes.size,
            ninos = ninos.size,
            vacunas = vacunas.size,
            controlesPediatricos = controlesPediatricos.size,
            enfermedades = enfermedades.size,
            diarioEntries = diarioEntries.size,
            dentistas = dentistas.size,
            visitasDentista = visitasDentista.size,
            diagnosticosDentales = diagnosticosDentales.size,
            procedimientosDentales = procedimientosDentales.size,
            prescripcionesDentales = prescripcionesDentales.size,
            dientesEstado = dientesEstado.size,
            imagenesDentales = imagenesDentales.size,
            transaccionesDentales = transaccionesDentales.size,
            ortodoncias = ortodoncias.size,
            ajustesOrtodoncia = ajustesOrtodoncia.size,
            incidenciasOrtodoncia = incidenciasOrtodoncia.size,
            elasticosOrtodoncia = elasticosOrtodoncia.size,
            registrosSedentarismo = registrosSedentarismo.size,
            configSedentarismo = configsSedentarismo.size,
            registrosHidratacion = registrosHidratacion.size,
            fallAlerts = fallAlerts.size
        )
    }

    private fun summarize(json: JSONObject): BackupSummary {
        val syncSnapshot = json.optJSONObject("syncSnapshot")?.toSyncSnapshot()
        return BackupSummary(
            patients = syncSnapshot?.patients?.size ?: json.optJSONArray("patients")?.length() ?: 0,
            medications = syncSnapshot?.medications?.size ?: json.optJSONArray("medications")?.length() ?: 0,
            reports = syncSnapshot?.reports?.size ?: json.optJSONArray("reports")?.length() ?: 0,
            vitalSigns = syncSnapshot?.vitalSigns?.size ?: json.optJSONArray("vitalSigns")?.length() ?: 0,
            intakes = syncSnapshot?.medicationIntakes?.size ?: json.optJSONArray("intakes")?.length() ?: 0,
            appointments = syncSnapshot?.appointments?.size ?: json.optJSONArray("appointments")?.length() ?: 0,
            practitioners = syncSnapshot?.practitioners?.size ?: json.optJSONArray("practitioners")?.length() ?: 0,
            vaccinations = syncSnapshot?.vaccinations?.size ?: json.optJSONArray("vaccinations")?.length() ?: 0,
            medicationOrders = json.optJSONArray("medicationOrders")?.length() ?: 0,
            physicalActivities = json.optJSONArray("physicalActivities")?.length() ?: 0,
            carritoPendiente = json.optJSONArray("carritoPendiente")?.length() ?: 0,
            ciclosMenstruales = json.optJSONArray("ciclosMenstruales")?.length() ?: 0,
            registrosDiarioCiclo = json.optJSONArray("registrosDiarioCiclo")?.length() ?: 0,
            controlesEmbarazo = json.optJSONArray("controlesEmbarazo")?.length() ?: 0,
            visitasPrenatales = json.optJSONArray("visitasPrenatales")?.length() ?: 0,
            metodosAnticonceptivos = json.optJSONArray("metodosAnticonceptivos")?.length() ?: 0,
            anticonceptivoIntakes = json.optJSONArray("anticonceptivoIntakes")?.length() ?: 0,
            bebesRecienNacidos = json.optJSONArray("bebesRecienNacidos")?.length() ?: 0,
            ninos = json.optJSONArray("ninos")?.length() ?: 0,
            vacunas = json.optJSONArray("vacunas")?.length() ?: 0,
            controlesPediatricos = json.optJSONArray("controlesPediatricos")?.length() ?: 0,
            enfermedades = json.optJSONArray("enfermedades")?.length() ?: 0,
            diarioEntries = json.optJSONArray("diarioEntries")?.length() ?: 0,
            dentistas = json.optJSONArray("dentistas")?.length() ?: 0,
            visitasDentista = json.optJSONArray("visitasDentista")?.length() ?: 0,
            diagnosticosDentales = json.optJSONArray("diagnosticosDentales")?.length() ?: 0,
            procedimientosDentales = json.optJSONArray("procedimientosDentales")?.length() ?: 0,
            prescripcionesDentales = json.optJSONArray("prescripcionesDentales")?.length() ?: 0,
            dientesEstado = json.optJSONArray("dientesEstado")?.length() ?: 0,
            imagenesDentales = json.optJSONArray("imagenesDentales")?.length() ?: 0,
            transaccionesDentales = json.optJSONArray("transaccionesDentales")?.length() ?: 0,
            ortodoncias = json.optJSONArray("ortodoncias")?.length() ?: 0,
            ajustesOrtodoncia = json.optJSONArray("ajustesOrtodoncia")?.length() ?: 0,
            incidenciasOrtodoncia = json.optJSONArray("incidenciasOrtodoncia")?.length() ?: 0,
            elasticosOrtodoncia = json.optJSONArray("elasticosOrtodoncia")?.length() ?: 0,
            registrosSedentarismo = json.optJSONArray("registrosSedentarismo")?.length() ?: 0,
            configSedentarismo = json.optJSONArray("configSedentarismo")?.length() ?: 0,
            registrosHidratacion = json.optJSONArray("registrosHidratacion")?.length() ?: 0,
            fallAlerts = json.optJSONArray("fallAlerts")?.length() ?: 0
        )
    }

    suspend fun restoreOnlyVitalSigns(context: Context, uri: Uri): Int {
        val database = AppDatabase.getDatabase(context)
        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw IllegalArgumentException("No se pudo leer el archivo")
        val json = JSONObject(jsonString)

        val vitalSignsArray = json.optJSONArray("vitalSigns")
            ?: json.optJSONObject("syncSnapshot")?.optJSONArray("vitalSigns")
            ?: return 0

        val vitalSigns = vitalSignsArray.toVitalSigns()

        database.signosVitalesDao().guardarTodos(vitalSigns)
        return vitalSigns.size
    }
}

private fun resolveAndroidDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?.takeIf { it.isNotBlank() }
        ?: "android-unknown-device"
}

private fun CriticalAlertConfig.toJson(): JSONObject = JSONObject()
    .put("retryIntervalMinutes", retryIntervalMinutes)
    .put("maxRetryCount", maxRetryCount)
    .put("soundUri", soundUri)

private fun SyncSnapshot.toJson(): JSONObject = JSONObject()
    .put("schemaVersion", schemaVersion)
    .put("source", source.toJson())
    .put("patients", JSONArray().apply { patients.forEach { put(it.toJson()) } })
    .put("medications", JSONArray().apply { medications.forEach { put(it.toJson()) } })
    .put("medicationIntakes", JSONArray().apply { medicationIntakes.forEach { put(it.toJson()) } })
    .put("reports", JSONArray().apply { reports.forEach { put(it.toJson()) } })
    .put("appointments", JSONArray().apply { appointments.forEach { put(it.toJson()) } })
    .put("practitioners", JSONArray().apply { practitioners.forEach { put(it.toJson()) } })
    .put("vaccinations", JSONArray().apply { vaccinations.forEach { put(it.toJson()) } })
    .put("vitalSigns", JSONArray().apply { vitalSigns.forEach { put(it.toJson()) } })

private fun SyncDeviceInfo.toJson(): JSONObject = JSONObject()
    .put("deviceId", deviceId)
    .put("platform", platform)
    .put("appVersion", appVersion)
    .put("exportedAt", exportedAt)

private fun SyncPatient.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("nombre", nombre)
    .put("apellidos", apellidos)
    .put("fechaNacimiento", fechaNacimiento)
    .put("edad", edad)
    .put("peso", peso)
    .put("pesoUnidad", pesoUnidad)
    .put("estatura", estatura)
    .put("estaturaUnidad", estaturaUnidad)
    .put("enfermedades", enfermedades)
    .put("prescripciones", prescripciones)
    .put("pais", pais)
    .put("moneda", moneda)
    .put("isActive", isActive)

private fun SyncMedication.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("nombre", nombre)
    .put("dosis", dosis)
    .put("formato", formato)
    .put("formaMedicamento", formaMedicamento)
    .put("colorMedicamento", colorMedicamento)
    .put("colorMedicamento2", colorMedicamento2)
    .put("presentacion", presentacion)
    .put("concentracion", concentracion)
    .put("repartoDosis", repartoDosis)
    .put("horariosTomas", horariosTomas)
    .put("fechaInicio", fechaInicio)
    .put("fechaFin", fechaFin)
    .put("horaToma", horaToma)
    .put("frecuenciaHoras", frecuenciaHoras)
    .put("esCicloCorto", esCicloCorto)
    .put("retryIntervalMinutes", retryIntervalMinutes)
    .put("alarmaSonidoUri", alarmaSonidoUri)
    .put("alarmaActiva", alarmaActiva)
    .put("estaActivo", estaActivo)
    .put("stockActual", stockActual ?: JSONObject.NULL)
    .put("stockMinimo", stockMinimo ?: JSONObject.NULL)
    .put("precioPorUnidad", precioPorUnidad ?: JSONObject.NULL)
    .put("telefonoPedidoWhatsapp", telefonoPedidoWhatsapp)
    .put("origenReposicion", origenReposicion)

private fun SyncMedicationIntake.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("medicationId", medicationId)
    .put("patientId", patientId)
    .put("scheduledAt", scheduledAt)
    .put("acceptedAt", acceptedAt)
    .put("medicationName", medicationName)
    .put("dosis", dosis)
    .put("status", status)

private fun SyncMedicalReport.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("practitionerId", practitionerId)
    .put("titulo", titulo)
    .put("descripcion", descripcion)
    .put("adjuntos", adjuntos)
    .put("attachments", adjuntos)
    .put("createdAt", createdAt)

private fun SyncMedicalAppointment.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("title", title)
    .put("doctorName", doctorName)
    .put("practitionerId", practitionerId)
    .put("location", location)
    .put("notes", notes)
    .put("scheduledAt", scheduledAt)
    .put("reminderMinutes", reminderMinutes)
    .put("alarmEnabled", alarmEnabled)
    .put("isCompleted", isCompleted)
    .put("createdAt", createdAt)

private fun SyncMedicalPractitioner.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("name", name)
    .put("specialty", specialty)
    .put("phone", phone)
    .put("createdAt", createdAt)

private fun SyncVaccinationRecord.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("vaccineName", vaccineName)
    .put("doseLabel", doseLabel)
    .put("appliedAt", appliedAt)
    .put("nextDoseAt", nextDoseAt)
    .put("reminderMinutes", reminderMinutes)
    .put("alarmEnabled", alarmEnabled)
    .put("notes", notes)
    .put("createdAt", createdAt)

private fun SyncVitalSigns.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("sistolica", sistolica)
    .put("diastolica", diastolica)
    .put("comentarioPresion", comentarioPresion)
    .put("latidos", latidos)
    .put("comentarioLatidos", comentarioLatidos)
    .put("spo2", spo2)
    .put("comentariosSpo2", comentariosSpo2)
    .put("glucemia", glucemia)
    .put("comentarioGlucemia", comentarioGlucemia)
    .put("temperatura", temperatura)
    .put("comentarioTemperatura", comentarioTemperatura)
    .put("peso", peso)
    .put("pesoUnidad", pesoUnidad)
    .put("imc", imc)
    .put("fechaRegistro", fechaRegistro)

private fun JSONObject.toSyncSnapshot(): SyncSnapshot = SyncSnapshot(
    schemaVersion = optInt("schemaVersion"),
    source = optJSONObject("source")?.toSyncDeviceInfo() ?: SyncDeviceInfo(
        deviceId = "unknown-device",
        platform = "unknown",
        appVersion = "unknown",
        exportedAt = 0L
    ),
    patients = optJSONArray("patients").toSyncPatients(),
    medications = optJSONArray("medications").toSyncMedications(),
    medicationIntakes = optJSONArray("medicationIntakes").toSyncMedicationIntakes(),
    reports = optJSONArray("reports").toSyncMedicalReports(),
    appointments = optJSONArray("appointments").toSyncMedicalAppointments(),
    practitioners = optJSONArray("practitioners").toSyncMedicalPractitioners(),
    vaccinations = optJSONArray("vaccinations").toSyncVaccinationRecords(),
    vitalSigns = optJSONArray("vitalSigns").toSyncVitalSigns()
)

private fun JSONObject.toSyncDeviceInfo(): SyncDeviceInfo = SyncDeviceInfo(
    deviceId = optString("deviceId"),
    platform = optString("platform"),
    appVersion = optString("appVersion"),
    exportedAt = optLong("exportedAt")
)

private fun JSONObject.toCriticalAlertConfig(): CriticalAlertConfig = CriticalAlertConfig(
    retryIntervalMinutes = optInt("retryIntervalMinutes", CriticalAlertSettings.DEFAULT_RETRY_INTERVAL_MINUTES),
    maxRetryCount = optInt("maxRetryCount", CriticalAlertSettings.DEFAULT_MAX_RETRY_COUNT),
    soundUri = optString("soundUri")
)

private fun PatientProfile.toJson(context: Context): JSONObject {
    val fotoBase64: String? = fotoPerfil?.let { ruta ->
        openAttachmentBytes(context, Uri.parse(ruta))?.let { bytes ->
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }
    return JSONObject()
        .put("id", id)
        .put("nombre", nombre)
        .put("apellidos", apellidos)
        .put("fechaNacimiento", fechaNacimiento)
        .put("edad", edad)
        .put("peso", peso)
        .put("pesoUnidad", pesoUnidad)
        .put("estatura", estatura)
        .put("estaturaUnidad", estaturaUnidad)
        .put("enfermedades", enfermedades)
        .put("prescripciones", prescripciones)
        .put("ultimoAnalisisIa", ultimoAnalisisIa)
        .put("sexo", sexo)
        .put("pais", pais)
        .put("moneda", moneda)
        .put("isActive", isActive)
        .put("fotoPerfil", fotoPerfil ?: JSONObject.NULL)
        .put("fotoPerfilBase64", fotoBase64 ?: JSONObject.NULL)
}

private fun Medication.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("nombre", nombre)
    .put("dosis", dosis)
    .put("formato", formato)
    .put("formaMedicamento", formaMedicamento)
    .put("colorMedicamento", colorMedicamento)
    .put("colorMedicamento2", colorMedicamento2)
    .put("presentacion", presentacion)
    .put("concentracion", concentracion)
    .put("repartoDosis", repartoDosis)
    .put("horariosTomas", horariosTomas)
    .put("fechaInicio", fechaInicio)
    .put("fechaFin", fechaFin)
    .put("horaToma", horaToma)
    .put("frecuenciaHoras", frecuenciaHoras)
    .put("esCicloCorto", esCicloCorto)
    .put("retryIntervalMinutes", retryIntervalMinutes)
    .put("alarmaSonidoUri", alarmaSonidoUri)
    .put("alarmaActiva", alarmaActiva)
    .put("estaActivo", estaActivo)
    .put("stockActual", stockActual ?: JSONObject.NULL)
    .put("stockMinimo", stockMinimo ?: JSONObject.NULL)
    .put("precioPorUnidad", precioPorUnidad ?: JSONObject.NULL)
    .put("telefonoPedidoWhatsapp", telefonoPedidoWhatsapp)
    .put("origenReposicion", origenReposicion)

private fun MedicationOrder.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("createdAt", createdAt)
    .put("itemCount", itemCount)
    .put("pricedItemCount", pricedItemCount)
    .put("totalAmount", totalAmount ?: JSONObject.NULL)
    .put("restockSource", restockSource)
    .put("supplierLabel", supplierLabel)
    .put("whatsappPhone", whatsappPhone)
    .put("itemsSummary", itemsSummary)
    .put("messagePreview", messagePreview)

private fun MedicationIntake.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("medicationId", medicationId)
    .put("patientId", patientId)
    .put("scheduledAt", scheduledAt)
    .put("acceptedAt", acceptedAt)
    .put("medicationName", medicationName)
    .put("dosis", dosis)
    .put("status", status)

private fun MedicalReport.toJson(context: Context): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("practitionerId", practitionerId)
    .put("titulo", titulo)
    .put("descripcion", descripcion)
    .put("adjuntos", adjuntos)
    .put("attachments", adjuntos)
    .put("attachmentPayloads", buildAttachmentPayloads(context, adjuntos))
    .put("analisisIa", analisisIa)
    .put("createdAt", createdAt)

private fun buildAttachmentPayloads(context: Context, attachments: String): JSONArray {
    val payloads = JSONArray()
    attachments.split("|")
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .forEachIndexed { index, path ->
            val attachmentUri = Uri.parse(path)
            val bytes = openAttachmentBytes(context, attachmentUri) ?: return@forEachIndexed
            payloads.put(
                JSONObject()
                    .put("originalUri", path)
                    .put("fileName", resolveAttachmentFileName(attachmentUri, index))
                    .put("mimeType", resolveAttachmentMimeType(path))
                    .put("encoding", "base64")
                    .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
            )
        }
    return payloads
}

private fun readMedicalReportAttachments(json: JSONObject): String {
    val directValue = json.optString("adjuntos").ifBlank { json.optString("attachments") }.trim()
    if (directValue.isNotBlank()) {
        return directValue
    }

    val jsonArray = json.optJSONArray("adjuntos") ?: json.optJSONArray("attachments")
    return jsonArray.toAttachmentString()
}

private fun readMedicalReportAttachments(context: Context, json: JSONObject): String {
    val embeddedAttachments = restoreEmbeddedAttachments(context, json.optJSONArray("attachmentPayloads"))
    if (embeddedAttachments.isNotBlank()) {
        return embeddedAttachments
    }
    return readMedicalReportAttachments(json)
}

private fun restoreEmbeddedAttachments(context: Context, payloads: JSONArray?): String {
    if (payloads == null || payloads.length() == 0) return ""

    val targetDir = File(context.filesDir, RESTORED_ATTACHMENTS_DIR).apply { mkdirs() }
    return List(payloads.length()) { index -> payloads.optJSONObject(index) }
        .mapIndexedNotNull { index, item ->
            item ?: return@mapIndexedNotNull null
            val encodedData = item.optString("data").trim()
            if (encodedData.isBlank()) return@mapIndexedNotNull null

            val fileName = item.optString("fileName")
                .takeIf { it.isNotBlank() }
                ?: "study_restored_${System.currentTimeMillis()}_$index.jpg"
            val targetFile = File(targetDir, buildSafeAttachmentFileName(fileName, index))

            runCatching {
                val bytes = Base64.decode(encodedData, Base64.DEFAULT)
                targetFile.writeBytes(bytes)
                Uri.fromFile(targetFile).toString()
            }.getOrNull()
        }
        .distinct()
        .joinToString("|")
}

private fun openAttachmentBytes(context: Context, attachmentUri: Uri): ByteArray? {
    return runCatching {
        when {
            attachmentUri.scheme.equals("file", ignoreCase = true) -> {
                val path = attachmentUri.path ?: return null
                File(path).takeIf { it.exists() }?.readBytes()
            }
            attachmentUri.scheme.isNullOrBlank() -> File(attachmentUri.toString()).takeIf { it.exists() }?.readBytes()
            else -> context.contentResolver.openInputStream(attachmentUri)?.use { it.readBytes() }
        }
    }.getOrNull()
}

private fun resolveAttachmentFileName(attachmentUri: Uri, index: Int): String {
    val lastPathSegment = attachmentUri.lastPathSegment?.substringAfterLast('/')?.trim().orEmpty()
    return lastPathSegment.ifBlank { "study_attachment_${index + 1}.jpg" }
}

private fun resolveAttachmentMimeType(path: String): String {
    val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }
}

private fun buildSafeAttachmentFileName(originalName: String, index: Int): String {
    val trimmed = originalName.substringAfterLast('/').substringAfterLast('\\').trim()
    val name = trimmed.substringBeforeLast('.', trimmed)
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(120)
        .takeIf { it.isNotBlank() }
        ?: "study_restored_${index + 1}"
    val extension = trimmed.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val normalizedExtension = when (extension) {
        "jpg", "jpeg", "png", "webp", "heic", "heif", "pdf" -> extension
        else -> "bin"
    }
    return "${name}_${System.currentTimeMillis()}_${index + 1}.$normalizedExtension"
}

private fun JSONArray?.toAttachmentString(): String {
    if (this == null) return ""
    return List(length()) { index -> opt(index) }
        .mapNotNull { item ->
            when (item) {
                is String -> item
                is JSONObject -> item.optString("uri")
                    .ifBlank { item.optString("path") }
                    .ifBlank { item.optString("value") }
                else -> null
            }
        }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString("|")
}

private fun MedicalAppointment.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("title", title)
    .put("doctorName", doctorName)
    .put("practitionerId", practitionerId)
    .put("location", location)
    .put("notes", notes)
    .put("scheduledAt", scheduledAt)
    .put("reminderMinutes", reminderMinutes)
    .put("alarmEnabled", alarmEnabled)
    .put("isCompleted", isCompleted)
    .put("createdAt", createdAt)

private fun MedicalPractitioner.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("name", name)
    .put("specialty", specialty)
    .put("phone", phone)
    .put("createdAt", createdAt)

private fun VaccinationRecord.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("vaccineName", vaccineName)
    .put("doseLabel", doseLabel)
    .put("appliedAt", appliedAt)
    .put("nextDoseAt", nextDoseAt)
    .put("reminderMinutes", reminderMinutes)
    .put("alarmEnabled", alarmEnabled)
    .put("notes", notes)
    .put("createdAt", createdAt)

private fun SignosVitales.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("sistolica", sistolica)
    .put("diastolica", diastolica)
    .put("comentarioPresion", comentarioPresion)
    .put("latidos", latidos)
    .put("comentarioLatidos", comentarioLatidos)
    .put("spo2", spo2)
    .put("comentariosSpo2", comentariosSpo2)
    .put("glucemia", glucemia)
    .put("comentarioGlucemia", comentarioGlucemia)
    .put("temperatura", temperatura)
    .put("comentarioTemperatura", comentarioTemperatura)
    .put("peso", peso)
    .put("pesoUnidad", pesoUnidad)
    .put("imc", imc)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toPatientProfiles(context: Context? = null): List<PatientProfile> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        val fotoRuta: String? = run {
            val base64 = json.optString("fotoPerfilBase64").ifBlank { null }
            if (base64 != null && context != null) {
                runCatching {
                    val dir = File(context.filesDir, "fotos_perfil").apply { mkdirs() }
                    val archivo = File(dir, "perfil_restored_${System.currentTimeMillis()}_${json.optInt("id")}.jpg")
                    archivo.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                    archivo.absolutePath
                }.getOrNull()
            } else {
                json.optString("fotoPerfil").ifBlank { null }
            }
        }
        PatientProfile(
            id = json.optInt("id"),
            nombre = json.optString("nombre"),
            apellidos = json.optString("apellidos"),
            fechaNacimiento = json.optLong("fechaNacimiento"),
            edad = json.optString("edad"),
            peso = json.optString("peso"),
            pesoUnidad = json.optString("pesoUnidad", "kg"),
            estatura = json.optString("estatura"),
            estaturaUnidad = json.optString("estaturaUnidad", "cm"),
            enfermedades = json.optString("enfermedades"),
            prescripciones = json.optString("prescripciones"),
            ultimoAnalisisIa = json.optString("ultimoAnalisisIa"),
            sexo = json.optString("sexo"),
            pais = json.optString("pais", CountryCurrencyCatalog.DEFAULT_COUNTRY),
            moneda = json.optString("moneda", CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL),
            isActive = json.optBoolean("isActive"),
            fotoPerfil = fotoRuta
        )
    }
}

private fun JSONArray?.toMedications(): List<Medication> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        Medication(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            nombre = json.optString("nombre"),
            dosis = json.optString("dosis"),
            formato = json.optString("formato"),
            formaMedicamento = json.optString("formaMedicamento"),
            colorMedicamento = json.optString("colorMedicamento"),
            colorMedicamento2 = json.optString("colorMedicamento2"),
            presentacion = json.optString("presentacion"),
            concentracion = json.optString("concentracion"),
            repartoDosis = json.optString("repartoDosis"),
            horariosTomas = json.optString("horariosTomas"),
            fechaInicio = json.optLong("fechaInicio"),
            fechaFin = json.optLong("fechaFin"),
            horaToma = json.optString("horaToma"),
            frecuenciaHoras = json.optInt("frecuenciaHoras"),
            esCicloCorto = json.optBoolean("esCicloCorto"),
            retryIntervalMinutes = json.optInt("retryIntervalMinutes", 10),
            alarmaSonidoUri = json.optString("alarmaSonidoUri"),
            alarmaActiva = json.optBoolean("alarmaActiva", true),
            estaActivo = json.optBoolean("estaActivo", true),
            stockActual = if (json.has("stockActual") && !json.isNull("stockActual")) json.optInt("stockActual") else null,
            stockMinimo = if (json.has("stockMinimo") && !json.isNull("stockMinimo")) json.optInt("stockMinimo") else null,
            precioPorUnidad = if (json.has("precioPorUnidad") && !json.isNull("precioPorUnidad")) json.optDouble("precioPorUnidad") else null,
            telefonoPedidoWhatsapp = json.optString("telefonoPedidoWhatsapp"),
            origenReposicion = json.optString("origenReposicion", RestockSource.WHATSAPP_NUMBER)
        )
    }
}

private fun JSONArray?.toMedicalReports(context: Context): List<MedicalReport> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MedicalReport(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            practitionerId = if (json.has("practitionerId") && !json.isNull("practitionerId")) json.optInt("practitionerId") else null,
            titulo = json.optString("titulo"),
            descripcion = json.optString("descripcion"),
            adjuntos = readMedicalReportAttachments(context, json),
            analisisIa = json.optString("analisisIa"),
            createdAt = json.optLong("createdAt")
        )
    }
}

private fun JSONArray?.toMedicationOrders(): List<MedicationOrder> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MedicationOrder(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            itemCount = json.optInt("itemCount"),
            pricedItemCount = json.optInt("pricedItemCount"),
            totalAmount = if (json.has("totalAmount") && !json.isNull("totalAmount")) json.optDouble("totalAmount") else null,
            restockSource = json.optString("restockSource", RestockSource.WHATSAPP_NUMBER),
            supplierLabel = json.optString("supplierLabel"),
            whatsappPhone = json.optString("whatsappPhone"),
            itemsSummary = json.optString("itemsSummary"),
            messagePreview = json.optString("messagePreview")
        )
    }
}

private fun JSONArray?.toMedicationIntakes(): List<MedicationIntake> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MedicationIntake(
            id = json.optInt("id"),
            medicationId = json.optInt("medicationId"),
            patientId = json.optInt("patientId"),
            scheduledAt = json.optLong("scheduledAt"),
            acceptedAt = json.optLong("acceptedAt"),
            medicationName = json.optString("medicationName"),
            dosis = json.optString("dosis"),
            status = json.optString("status", "TAKEN")
        )
    }
}

private fun JSONArray?.toMedicalAppointments(): List<MedicalAppointment> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MedicalAppointment(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            title = json.optString("title"),
            doctorName = json.optString("doctorName"),
            practitionerId = if (json.has("practitionerId") && !json.isNull("practitionerId")) json.optInt("practitionerId") else null,
            location = json.optString("location"),
            notes = json.optString("notes"),
            scheduledAt = json.optLong("scheduledAt"),
            reminderMinutes = json.optInt("reminderMinutes", 60),
            alarmEnabled = json.optBoolean("alarmEnabled", true),
            isCompleted = json.optBoolean("isCompleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toMedicalPractitioners(): List<MedicalPractitioner> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MedicalPractitioner(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            name = json.optString("name"),
            specialty = json.optString("specialty"),
            phone = json.optString("phone"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toVaccinationRecords(): List<VaccinationRecord> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        VaccinationRecord(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            vaccineName = json.optString("vaccineName"),
            doseLabel = json.optString("doseLabel"),
            appliedAt = json.optLong("appliedAt"),
            nextDoseAt = if (json.has("nextDoseAt") && !json.isNull("nextDoseAt")) json.optLong("nextDoseAt") else null,
            reminderMinutes = json.optInt("reminderMinutes", 1440),
            alarmEnabled = json.optBoolean("alarmEnabled", true),
            notes = json.optString("notes"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toVitalSigns(): List<SignosVitales> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SignosVitales(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            sistolica = json.optInt("sistolica"),
            diastolica = json.optInt("diastolica"),
            comentarioPresion = json.optString("comentarioPresion"),
            latidos = json.optInt("latidos"),
            comentarioLatidos = json.optString("comentarioLatidos"),
            spo2 = if (json.has("spo2") && !json.isNull("spo2")) json.optInt("spo2") else null,
            comentariosSpo2 = json.optString("comentariosSpo2"),
            glucemia = if (json.has("glucemia") && !json.isNull("glucemia")) json.optInt("glucemia") else null,
            comentarioGlucemia = json.optString("comentarioGlucemia"),
            temperatura = if (json.has("temperatura") && !json.isNull("temperatura")) json.optDouble("temperatura") else null,
            comentarioTemperatura = json.optString("comentarioTemperatura"),
            peso = if (json.has("peso") && !json.isNull("peso")) json.optDouble("peso") else null,
            pesoUnidad = json.optString("pesoUnidad", "kg"),
            imc = if (json.has("imc") && !json.isNull("imc")) json.optDouble("imc") else null,
            fechaRegistro = json.optLong("fechaRegistro")
        )
    }
}

private fun JSONArray?.toSyncPatients(): List<SyncPatient> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncPatient(
            id = json.optInt("id"),
            nombre = json.optString("nombre"),
            apellidos = json.optString("apellidos"),
            fechaNacimiento = json.optLong("fechaNacimiento"),
            edad = json.optString("edad"),
            peso = json.optString("peso"),
            pesoUnidad = json.optString("pesoUnidad", "kg"),
            estatura = json.optString("estatura"),
            estaturaUnidad = json.optString("estaturaUnidad", "cm"),
            enfermedades = json.optString("enfermedades"),
            prescripciones = json.optString("prescripciones"),
            isActive = json.optBoolean("isActive")
        )
    }
}

private fun JSONArray?.toSyncMedications(): List<SyncMedication> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncMedication(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            nombre = json.optString("nombre"),
            dosis = json.optString("dosis"),
            formato = json.optString("formato"),
            formaMedicamento = json.optString("formaMedicamento"),
            colorMedicamento = json.optString("colorMedicamento"),
            colorMedicamento2 = json.optString("colorMedicamento2"),
            presentacion = json.optString("presentacion"),
            concentracion = json.optString("concentracion"),
            repartoDosis = json.optString("repartoDosis"),
            horariosTomas = json.optString("horariosTomas"),
            fechaInicio = json.optLong("fechaInicio"),
            fechaFin = json.optLong("fechaFin"),
            horaToma = json.optString("horaToma"),
            frecuenciaHoras = json.optInt("frecuenciaHoras"),
            esCicloCorto = json.optBoolean("esCicloCorto"),
            retryIntervalMinutes = json.optInt("retryIntervalMinutes", 10),
            alarmaSonidoUri = json.optString("alarmaSonidoUri"),
            alarmaActiva = json.optBoolean("alarmaActiva", true),
            estaActivo = json.optBoolean("estaActivo", true),
            stockActual = if (json.has("stockActual") && !json.isNull("stockActual")) json.optInt("stockActual") else null,
            stockMinimo = if (json.has("stockMinimo") && !json.isNull("stockMinimo")) json.optInt("stockMinimo") else null,
            precioPorUnidad = if (json.has("precioPorUnidad") && !json.isNull("precioPorUnidad")) json.optDouble("precioPorUnidad") else null,
            telefonoPedidoWhatsapp = json.optString("telefonoPedidoWhatsapp"),
            origenReposicion = json.optString("origenReposicion", RestockSource.WHATSAPP_NUMBER)
        )
    }
}

private fun JSONArray?.toSyncMedicationIntakes(): List<SyncMedicationIntake> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncMedicationIntake(
            id = json.optInt("id"),
            medicationId = json.optInt("medicationId"),
            patientId = json.optInt("patientId"),
            scheduledAt = json.optLong("scheduledAt"),
            acceptedAt = json.optLong("acceptedAt"),
            medicationName = json.optString("medicationName"),
            dosis = json.optString("dosis"),
            status = json.optString("status", "TAKEN")
        )
    }
}

private fun JSONArray?.toSyncMedicalReports(): List<SyncMedicalReport> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncMedicalReport(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            practitionerId = if (json.has("practitionerId") && !json.isNull("practitionerId")) json.optInt("practitionerId") else null,
            titulo = json.optString("titulo"),
            descripcion = json.optString("descripcion"),
            adjuntos = readMedicalReportAttachments(json),
            createdAt = json.optLong("createdAt")
        )
    }
}

private fun JSONArray?.toSyncMedicalAppointments(): List<SyncMedicalAppointment> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncMedicalAppointment(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            title = json.optString("title"),
            doctorName = json.optString("doctorName"),
            practitionerId = if (json.has("practitionerId") && !json.isNull("practitionerId")) json.optInt("practitionerId") else null,
            location = json.optString("location"),
            notes = json.optString("notes"),
            scheduledAt = json.optLong("scheduledAt"),
            reminderMinutes = json.optInt("reminderMinutes", 60),
            alarmEnabled = json.optBoolean("alarmEnabled", true),
            isCompleted = json.optBoolean("isCompleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toSyncMedicalPractitioners(): List<SyncMedicalPractitioner> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncMedicalPractitioner(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            name = json.optString("name"),
            specialty = json.optString("specialty"),
            phone = json.optString("phone"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toSyncVaccinationRecords(): List<SyncVaccinationRecord> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncVaccinationRecord(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            vaccineName = json.optString("vaccineName"),
            doseLabel = json.optString("doseLabel"),
            appliedAt = json.optLong("appliedAt"),
            nextDoseAt = if (json.has("nextDoseAt") && !json.isNull("nextDoseAt")) json.optLong("nextDoseAt") else null,
            reminderMinutes = json.optInt("reminderMinutes", 1440),
            alarmEnabled = json.optBoolean("alarmEnabled", true),
            notes = json.optString("notes"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toSyncVitalSigns(): List<SyncVitalSigns> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        SyncVitalSigns(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            sistolica = json.optInt("sistolica"),
            diastolica = json.optInt("diastolica"),
            comentarioPresion = json.optString("comentarioPresion"),
            latidos = json.optInt("latidos"),
            comentarioLatidos = json.optString("comentarioLatidos"),
            spo2 = if (json.has("spo2") && !json.isNull("spo2")) json.optInt("spo2") else null,
            comentariosSpo2 = json.optString("comentariosSpo2"),
            glucemia = if (json.has("glucemia") && !json.isNull("glucemia")) json.optInt("glucemia") else null,
            comentarioGlucemia = json.optString("comentarioGlucemia"),
            temperatura = if (json.has("temperatura") && !json.isNull("temperatura")) json.optDouble("temperatura") else null,
            comentarioTemperatura = json.optString("comentarioTemperatura"),
            peso = if (json.has("peso") && !json.isNull("peso")) json.optDouble("peso") else null,
            pesoUnidad = json.optString("pesoUnidad", "kg"),
            imc = if (json.has("imc") && !json.isNull("imc")) json.optDouble("imc") else null,
            fechaRegistro = json.optLong("fechaRegistro")
        )
    }
}

private fun SyncPatient.toEntity(): PatientProfile = PatientProfile(
    id = id,
    nombre = nombre,
    apellidos = apellidos,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    peso = peso,
    pesoUnidad = pesoUnidad,
    estatura = estatura,
    estaturaUnidad = estaturaUnidad,
    enfermedades = enfermedades,
    prescripciones = prescripciones,
    pais = pais,
    moneda = moneda,
    isActive = isActive
)

private fun SyncMedication.toEntity(): Medication = Medication(
    id = id,
    patientId = patientId,
    nombre = nombre,
    dosis = dosis,
    formato = formato,
    formaMedicamento = formaMedicamento,
    colorMedicamento = colorMedicamento,
    colorMedicamento2 = colorMedicamento2,
    presentacion = presentacion,
    concentracion = concentracion,
    repartoDosis = repartoDosis,
    horariosTomas = horariosTomas,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    horaToma = horaToma,
    frecuenciaHoras = frecuenciaHoras,
    esCicloCorto = esCicloCorto,
    retryIntervalMinutes = retryIntervalMinutes,
    alarmaSonidoUri = alarmaSonidoUri,
    alarmaActiva = alarmaActiva,
    estaActivo = estaActivo,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    precioPorUnidad = precioPorUnidad,
    telefonoPedidoWhatsapp = telefonoPedidoWhatsapp,
    origenReposicion = origenReposicion
)

private fun SyncMedicationIntake.toEntity(): MedicationIntake = MedicationIntake(
    id = id,
    medicationId = medicationId,
    patientId = patientId,
    scheduledAt = scheduledAt,
    acceptedAt = acceptedAt,
    medicationName = medicationName,
    dosis = dosis,
    status = status
)

private fun SyncMedicalReport.toEntity(): MedicalReport = MedicalReport(
    id = id,
    patientId = patientId,
    practitionerId = practitionerId,
    titulo = titulo,
    descripcion = descripcion,
    adjuntos = adjuntos,
    createdAt = createdAt
)

private fun SyncMedicalAppointment.toEntity(): MedicalAppointment = MedicalAppointment(
    id = id,
    patientId = patientId,
    title = title,
    doctorName = doctorName,
    practitionerId = practitionerId,
    location = location,
    notes = notes,
    scheduledAt = scheduledAt,
    reminderMinutes = reminderMinutes,
    alarmEnabled = alarmEnabled,
    isCompleted = isCompleted,
    createdAt = createdAt
)

private fun SyncMedicalPractitioner.toEntity(): MedicalPractitioner = MedicalPractitioner(
    id = id,
    patientId = patientId,
    name = name,
    specialty = specialty,
    phone = phone,
    createdAt = createdAt
)

private fun SyncVaccinationRecord.toEntity(): VaccinationRecord = VaccinationRecord(
    id = id,
    patientId = patientId,
    vaccineName = vaccineName,
    doseLabel = doseLabel,
    appliedAt = appliedAt,
    nextDoseAt = nextDoseAt,
    reminderMinutes = reminderMinutes,
    alarmEnabled = alarmEnabled,
    notes = notes,
    createdAt = createdAt
)

private fun SyncVitalSigns.toEntity(): SignosVitales = SignosVitales(
    id = id,
    patientId = patientId,
    sistolica = sistolica,
    diastolica = diastolica,
    comentarioPresion = comentarioPresion,
    latidos = latidos,
    comentarioLatidos = comentarioLatidos,
    spo2 = spo2,
    comentariosSpo2 = comentariosSpo2,
    glucemia = glucemia,
    comentarioGlucemia = comentarioGlucemia,
    temperatura = temperatura,
    comentarioTemperatura = comentarioTemperatura,
    peso = peso,
    pesoUnidad = pesoUnidad,
    imc = imc,
    fechaRegistro = fechaRegistro
)

private fun PhysicalActivity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("tipo", tipo)
    .put("fechaInicio", fechaInicio)
    .put("fechaFin", fechaFin)
    .put("pasos", pasos)
    .put("distanciaMetros", distanciaMetros)
    .put("duracionSegundos", duracionSegundos)
    .put("calorias", calorias)
    .put("rutaJson", rutaJson)
    .put("altitudInicioMetros", altitudInicioMetros)
    .put("altitudMaxMetros", altitudMaxMetros)
    .put("desnivelPositivoMetros", desnivelPositivoMetros)
    .put("desnivelNegativoMetros", desnivelNegativoMetros)

private fun CarritoPendienteItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("medicationId", medicationId)
    .put("unidadesSolicitadas", unidadesSolicitadas)

private fun CicloMenstrual.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("fechaInicio", fechaInicio)
    .put("duracionDias", duracionDias)
    .put("duracionCicloDias", duracionCicloDias)
    .put("sintomas", sintomas)
    .put("notas", notas)

private fun RegistroDiarioCiclo.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("cicloId", cicloId)
    .put("fecha", fecha)
    .put("tipoSintoma", tipoSintoma)
    .put("valorSintoma", valorSintoma)
    .put("intensidad", intensidad ?: JSONObject.NULL)
    .put("notas", notas)

private fun ControlEmbarazo.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("fechaUltimaRegla", fechaUltimaRegla)
    .put("fechaProbableParto", fechaProbableParto)
    .put("notas", notas)
    .put("activo", activo)
    .put("fechaRegistro", fechaRegistro)
    .put("fechaParto", fechaParto ?: JSONObject.NULL)
    .put("tipoPartoRegistrado", tipoPartoRegistrado)
    .put("notasParto", notasParto)
    .put("estadoEmbarazo", estadoEmbarazo)
    .put("fechaFin", fechaFin ?: JSONObject.NULL)
    .put("tipoInterrupcion", tipoInterrupcion ?: JSONObject.NULL)
    .put("metodoInterrupcion", metodoInterrupcion ?: JSONObject.NULL)
    .put("notasInterrupcion", notasInterrupcion ?: JSONObject.NULL)
    .put("esPrueba", esPrueba)

private fun VisitaPrenatal.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("embarazoId", embarazoId)
    .put("fecha", fecha)
    .put("semanasGestacion", semanasGestacion)
    .put("peso", peso ?: JSONObject.NULL)
    .put("presionArterial", presionArterial)
    .put("alturaUterina", alturaUterina ?: JSONObject.NULL)
    .put("frecuenciaCardiacaFetal", frecuenciaCardiacaFetal ?: JSONObject.NULL)
    .put("edemas", edemas)
    .put("hemoglobina", hemoglobina ?: JSONObject.NULL)
    .put("glucemia", glucemia ?: JSONObject.NULL)
    .put("proteinasOrina", proteinasOrina)
    .put("suplementos", suplementos)
    .put("observaciones", observaciones)
    .put("proximaVisitaSemanas", proximaVisitaSemanas ?: JSONObject.NULL)
    .put("facultativo", facultativo)
    .put("contactoOMS", contactoOMS ?: JSONObject.NULL)

private fun MetodoAnticonceptivo.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("tipo", tipo)
    .put("fechaInicio", fechaInicio)
    .put("horaToma", horaToma)
    .put("activo", activo)
    .put("notas", notas)
    .put("fechaRegistro", fechaRegistro)
    .put("duracionCicloDias", duracionCicloDias ?: JSONObject.NULL)
    .put("diasDescanso", diasDescanso ?: JSONObject.NULL)
    .put("proximaCita", proximaCita ?: JSONObject.NULL)
    .put("recordatorioDiasAntes", recordatorioDiasAntes)

private fun AnticonceptivoIntake.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("metodoId", metodoId)
    .put("scheduledAt", scheduledAt)
    .put("acceptedAt", acceptedAt)

private fun BebeRecienNacido.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("embarazoId", embarazoId)
    .put("patientId", patientId)
    .put("nombre", nombre)
    .put("sexo", sexo)
    .put("fechaNacimiento", fechaNacimiento)
    .put("pesoAlNacer", pesoAlNacer)
    .put("tallaAlNacer", tallaAlNacer)
    .put("notas", notas)
    .put("fechaRegistro", fechaRegistro)

private fun NinoEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("embarazoId", embarazoId)
    .put("nombre", nombre)
    .put("fechaNacimiento", fechaNacimiento)
    .put("sexo", sexo)
    .put("notasParto", notasParto ?: JSONObject.NULL)
    .put("fechaRegistro", fechaRegistro)
    .put("esPrueba", esPrueba)

private fun VacunaEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ninoId", ninoId)
    .put("nombre", nombre)
    .put("descripcion", descripcion)
    .put("edadRecomendada", edadRecomendada)
    .put("estaAplicada", estaAplicada)
    .put("fechaAplicacion", fechaAplicacion ?: JSONObject.NULL)

private fun ControlPediatricoEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ninoId", ninoId)
    .put("fechaControl", fechaControl)
    .put("pesoKg", pesoKg ?: JSONObject.NULL)
    .put("tallaCm", tallaCm ?: JSONObject.NULL)
    .put("perimetroCefalicoCm", perimetroCefalicoCm ?: JSONObject.NULL)
    .put("observaciones", observaciones ?: JSONObject.NULL)

private fun EnfermedadEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ninoId", ninoId)
    .put("nombre", nombre)
    .put("fechaInicio", fechaInicio)
    .put("fechaFin", fechaFin ?: JSONObject.NULL)
    .put("sintomas", sintomas ?: JSONObject.NULL)
    .put("planPersonal", planPersonal ?: JSONObject.NULL)
    .put("esAlergia", esAlergia)

private fun DiarioEntry.toJson(context: Context): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("fecha", fecha)
    .put("texto", texto)
    .put("rutaImagen", rutaImagen ?: JSONObject.NULL)
    .put("imagePayload", buildImagePayload(context, rutaImagen))

private fun buildImagePayload(context: Context, ruta: String?): JSONObject? {
    if (ruta.isNullOrBlank()) return null
    val bytes = when {
        ruta.startsWith("/") -> {
            File(ruta).takeIf { it.exists() }?.readBytes()
        }
        else -> {
            val uri = runCatching { Uri.parse(ruta) }.getOrNull() ?: return null
            openAttachmentBytes(context, uri)
        }
    } ?: return null
    return JSONObject()
        .put("originalUri", ruta)
        .put("encoding", "base64")
        .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
}

private fun readImagePayload(context: Context, json: JSONObject, dirName: String, fallback: String): String {
    val payload = json.optJSONObject("imagePayload") ?: return fallback
    val encodedData = payload.optString("data").trim()
    if (encodedData.isBlank()) return fallback
    val targetDir = File(context.filesDir, dirName).apply { mkdirs() }
    val fileName = payload.optString("originalUri").substringAfterLast('/').ifBlank { "image_${System.currentTimeMillis()}.jpg" }
    val targetFile = File(targetDir, buildSafeAttachmentFileName(fileName, 0))
    return runCatching {
        val bytes = Base64.decode(encodedData, Base64.DEFAULT)
        targetFile.writeBytes(bytes)
        targetFile.absolutePath
    }.getOrDefault(fallback)
}

private fun JSONArray?.toPhysicalActivities(): List<PhysicalActivity> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        PhysicalActivity(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            tipo = json.optString("tipo"),
            fechaInicio = json.optLong("fechaInicio"),
            fechaFin = json.optLong("fechaFin"),
            pasos = json.optInt("pasos"),
            distanciaMetros = json.optDouble("distanciaMetros"),
            duracionSegundos = json.optLong("duracionSegundos"),
            calorias = json.optInt("calorias"),
            rutaJson = json.optString("rutaJson"),
            altitudInicioMetros = json.optDouble("altitudInicioMetros"),
            altitudMaxMetros = json.optDouble("altitudMaxMetros"),
            desnivelPositivoMetros = json.optDouble("desnivelPositivoMetros"),
            desnivelNegativoMetros = json.optDouble("desnivelNegativoMetros")
        )
    }
}

private fun JSONArray?.toCarritoPendienteItems(): List<CarritoPendienteItem> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        CarritoPendienteItem(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            medicationId = json.optInt("medicationId"),
            unidadesSolicitadas = json.optInt("unidadesSolicitadas")
        )
    }
}

private fun JSONArray?.toCiclosMenstruales(): List<CicloMenstrual> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        CicloMenstrual(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            fechaInicio = json.optLong("fechaInicio"),
            duracionDias = json.optInt("duracionDias", 5),
            duracionCicloDias = json.optInt("duracionCicloDias", 28),
            sintomas = json.optString("sintomas"),
            notas = json.optString("notas")
        )
    }
}

private fun JSONArray?.toRegistrosDiarioCiclo(): List<RegistroDiarioCiclo> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        RegistroDiarioCiclo(
            id = json.optInt("id"),
            cicloId = json.optInt("cicloId"),
            fecha = json.optLong("fecha"),
            tipoSintoma = json.optString("tipoSintoma"),
            valorSintoma = json.optString("valorSintoma"),
            intensidad = if (json.has("intensidad") && !json.isNull("intensidad")) json.optInt("intensidad") else null,
            notas = json.optString("notas")
        )
    }
}

private fun JSONArray?.toControlesEmbarazo(): List<ControlEmbarazo> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ControlEmbarazo(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            fechaUltimaRegla = json.optLong("fechaUltimaRegla"),
            fechaProbableParto = json.optLong("fechaProbableParto"),
            notas = json.optString("notas"),
            activo = json.optBoolean("activo", true),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis()),
            fechaParto = if (json.has("fechaParto") && !json.isNull("fechaParto")) json.optLong("fechaParto") else null,
            tipoPartoRegistrado = json.optString("tipoPartoRegistrado"),
            notasParto = json.optString("notasParto"),
            estadoEmbarazo = json.optString("estadoEmbarazo"),
            fechaFin = if (json.has("fechaFin") && !json.isNull("fechaFin")) json.optLong("fechaFin") else null,
            tipoInterrupcion = json.optString("tipoInterrupcion").ifBlank { null },
            metodoInterrupcion = json.optString("metodoInterrupcion").ifBlank { null },
            notasInterrupcion = json.optString("notasInterrupcion").ifBlank { null },
            esPrueba = json.optBoolean("esPrueba", false)
        )
    }
}

private fun JSONArray?.toVisitasPrenatales(): List<VisitaPrenatal> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        VisitaPrenatal(
            id = json.optInt("id"),
            embarazoId = json.optInt("embarazoId"),
            fecha = json.optLong("fecha"),
            semanasGestacion = json.optInt("semanasGestacion"),
            peso = if (json.has("peso") && !json.isNull("peso")) json.optDouble("peso").toFloat() else null,
            presionArterial = json.optString("presionArterial"),
            alturaUterina = if (json.has("alturaUterina") && !json.isNull("alturaUterina")) json.optDouble("alturaUterina").toFloat() else null,
            frecuenciaCardiacaFetal = if (json.has("frecuenciaCardiacaFetal") && !json.isNull("frecuenciaCardiacaFetal")) json.optInt("frecuenciaCardiacaFetal") else null,
            edemas = json.optBoolean("edemas", false),
            hemoglobina = if (json.has("hemoglobina") && !json.isNull("hemoglobina")) json.optDouble("hemoglobina").toFloat() else null,
            glucemia = if (json.has("glucemia") && !json.isNull("glucemia")) json.optDouble("glucemia").toFloat() else null,
            proteinasOrina = json.optBoolean("proteinasOrina", false),
            suplementos = json.optString("suplementos"),
            observaciones = json.optString("observaciones"),
            proximaVisitaSemanas = if (json.has("proximaVisitaSemanas") && !json.isNull("proximaVisitaSemanas")) json.optInt("proximaVisitaSemanas") else null,
            facultativo = json.optString("facultativo"),
            contactoOMS = if (json.has("contactoOMS") && !json.isNull("contactoOMS")) json.optInt("contactoOMS") else null
        )
    }
}

private fun JSONArray?.toMetodosAnticonceptivos(): List<MetodoAnticonceptivo> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        MetodoAnticonceptivo(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            tipo = json.optString("tipo"),
            fechaInicio = json.optLong("fechaInicio"),
            horaToma = json.optString("horaToma"),
            activo = json.optBoolean("activo", true),
            notas = json.optString("notas"),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis()),
            duracionCicloDias = if (json.has("duracionCicloDias") && !json.isNull("duracionCicloDias")) json.optInt("duracionCicloDias") else null,
            diasDescanso = if (json.has("diasDescanso") && !json.isNull("diasDescanso")) json.optInt("diasDescanso") else null,
            proximaCita = if (json.has("proximaCita") && !json.isNull("proximaCita")) json.optLong("proximaCita") else null,
            recordatorioDiasAntes = json.optInt("recordatorioDiasAntes", 1)
        )
    }
}

private fun JSONArray?.toAnticonceptivoIntakes(): List<AnticonceptivoIntake> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        AnticonceptivoIntake(
            id = json.optInt("id"),
            metodoId = json.optInt("metodoId"),
            scheduledAt = json.optLong("scheduledAt"),
            acceptedAt = json.optLong("acceptedAt")
        )
    }
}

private fun JSONArray?.toBebesRecienNacidos(): List<BebeRecienNacido> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        BebeRecienNacido(
            id = json.optInt("id"),
            embarazoId = json.optInt("embarazoId"),
            patientId = json.optInt("patientId"),
            nombre = json.optString("nombre"),
            sexo = json.optString("sexo"),
            fechaNacimiento = json.optLong("fechaNacimiento"),
            pesoAlNacer = json.optString("pesoAlNacer"),
            tallaAlNacer = json.optString("tallaAlNacer"),
            notas = json.optString("notas"),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun JSONArray?.toNinos(): List<NinoEntity> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        NinoEntity(
            id = json.optLong("id"),
            patientId = json.optInt("patientId"),
            embarazoId = json.optInt("embarazoId"),
            nombre = json.optString("nombre"),
            fechaNacimiento = json.optString("fechaNacimiento"),
            sexo = json.optString("sexo"),
            notasParto = json.optString("notasParto").ifBlank { null },
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis()),
            esPrueba = json.optBoolean("esPrueba", false)
        )
    }
}

private fun JSONArray?.toVacunas(): List<VacunaEntity> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        VacunaEntity(
            id = json.optLong("id"),
            ninoId = json.optLong("ninoId"),
            nombre = json.optString("nombre"),
            descripcion = json.optString("descripcion"),
            edadRecomendada = json.optString("edadRecomendada"),
            estaAplicada = json.optBoolean("estaAplicada", false),
            fechaAplicacion = json.optString("fechaAplicacion").ifBlank { null }
        )
    }
}

private fun JSONArray?.toControlesPediatricos(): List<ControlPediatricoEntity> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ControlPediatricoEntity(
            id = json.optLong("id"),
            ninoId = json.optLong("ninoId"),
            fechaControl = json.optString("fechaControl"),
            pesoKg = if (json.has("pesoKg") && !json.isNull("pesoKg")) json.optDouble("pesoKg") else null,
            tallaCm = if (json.has("tallaCm") && !json.isNull("tallaCm")) json.optDouble("tallaCm") else null,
            perimetroCefalicoCm = if (json.has("perimetroCefalicoCm") && !json.isNull("perimetroCefalicoCm")) json.optDouble("perimetroCefalicoCm") else null,
            observaciones = json.optString("observaciones").ifBlank { null }
        )
    }
}

private fun JSONArray?.toEnfermedades(): List<EnfermedadEntity> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        EnfermedadEntity(
            id = json.optLong("id"),
            ninoId = json.optLong("ninoId"),
            nombre = json.optString("nombre"),
            fechaInicio = json.optString("fechaInicio"),
            fechaFin = json.optString("fechaFin").ifBlank { null },
            sintomas = json.optString("sintomas").ifBlank { null },
            planPersonal = json.optString("planPersonal").ifBlank { null },
            esAlergia = json.optBoolean("esAlergia", false)
        )
    }
}


private fun JSONArray?.toDiarioEntries(context: Context): List<DiarioEntry> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        DiarioEntry(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            fecha = json.optLong("fecha"),
            texto = json.optString("texto"),
            rutaImagen = readImagePayload(context, json, "restored_diario_images", json.optString("rutaImagen"))
        )
    }
}

private fun Dentista.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("nombre", nombre)
    .put("especialidad", especialidad)
    .put("telefono", telefono)
    .put("direccion", direccion)
    .put("notas", notas)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toDentistas(): List<Dentista> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        Dentista(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            nombre = json.optString("nombre"),
            especialidad = json.optString("especialidad"),
            telefono = json.optString("telefono"),
            direccion = json.optString("direccion"),
            notas = json.optString("notas"),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun VisitaDentista.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("dentistaId", dentistaId ?: JSONObject.NULL)
    .put("fechaHora", fechaHora)
    .put("motivo", motivo)
    .put("estado", estado)
    .put("notas", notas)
    .put("recordatorio24h", recordatorio24h)
    .put("recordatorio2h", recordatorio2h)
    .put("seguimientoPostConsulta", seguimientoPostConsulta)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toVisitasDentista(): List<VisitaDentista> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        VisitaDentista(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            dentistaId = if (json.has("dentistaId") && !json.isNull("dentistaId")) json.optInt("dentistaId") else null,
            fechaHora = json.optLong("fechaHora"),
            motivo = json.optString("motivo"),
            estado = json.optString("estado", "PENDIENTE"),
            notas = json.optString("notas"),
            recordatorio24h = json.optBoolean("recordatorio24h", true),
            recordatorio2h = json.optBoolean("recordatorio2h", true),
            seguimientoPostConsulta = json.optBoolean("seguimientoPostConsulta", false),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun DiagnosticoDental.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("visitaId", visitaId)
    .put("patientId", patientId)
    .put("numeroDiente", numeroDiente)
    .put("zona", zona)
    .put("descripcion", descripcion)
    .put("estado", estado)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toDiagnosticosDentales(): List<DiagnosticoDental> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        DiagnosticoDental(
            id = json.optInt("id"),
            visitaId = json.optInt("visitaId"),
            patientId = json.optInt("patientId"),
            numeroDiente = json.optInt("numeroDiente", 0),
            zona = json.optString("zona"),
            descripcion = json.optString("descripcion"),
            estado = json.optString("estado", "ACTIVO"),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun ProcedimientoDental.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("diagnosticoId", diagnosticoId)
    .put("patientId", patientId)
    .put("tipo", tipo)
    .put("descripcion", descripcion)
    .put("fecha", fecha)
    .put("completado", completado)
    .put("costo", costo ?: JSONObject.NULL)
    .put("notas", notas)

private fun JSONArray?.toProcedimientosDentales(): List<ProcedimientoDental> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ProcedimientoDental(
            id = json.optInt("id"),
            diagnosticoId = json.optInt("diagnosticoId"),
            patientId = json.optInt("patientId"),
            tipo = json.optString("tipo"),
            descripcion = json.optString("descripcion"),
            fecha = json.optLong("fecha", System.currentTimeMillis()),
            completado = json.optBoolean("completado", false),
            costo = if (json.has("costo") && !json.isNull("costo")) json.optDouble("costo") else null,
            notas = json.optString("notas")
        )
    }
}

private fun PrescripcionDental.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("visitaId", visitaId)
    .put("patientId", patientId)
    .put("medicamento", medicamento)
    .put("dosis", dosis)
    .put("frecuencia", frecuencia)
    .put("duracionDias", duracionDias)
    .put("sincronizadaConAlarma", sincronizadaConAlarma)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toPrescripcionesDentales(): List<PrescripcionDental> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        PrescripcionDental(
            id = json.optInt("id"),
            visitaId = json.optInt("visitaId"),
            patientId = json.optInt("patientId"),
            medicamento = json.optString("medicamento"),
            dosis = json.optString("dosis"),
            frecuencia = json.optString("frecuencia"),
            duracionDias = json.optInt("duracionDias", 0),
            sincronizadaConAlarma = json.optBoolean("sincronizadaConAlarma", false),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun DienteEstado.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("numeroDiente", numeroDiente)
    .put("estado", estado)
    .put("notas", notas)
    .put("fechaActualizacion", fechaActualizacion)

private fun JSONArray?.toDientesEstado(): List<DienteEstado> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        DienteEstado(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            numeroDiente = json.optInt("numeroDiente", 0),
            estado = json.optString("estado", "SANO"),
            notas = json.optString("notas"),
            fechaActualizacion = json.optLong("fechaActualizacion", System.currentTimeMillis())
        )
    }
}

private fun ImagenDental.toJson(context: Context): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("numeroDiente", numeroDiente)
    .put("ortodonciaId", ortodonciaId ?: JSONObject.NULL)
    .put("uri", uri)
    .put("tipo", tipo)
    .put("etapa", etapa)
    .put("notas", notas)
    .put("fecha", fecha)
    .put("imagePayload", buildImagePayload(context, uri))

private fun JSONArray?.toImagenesDentales(context: Context): List<ImagenDental> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ImagenDental(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            numeroDiente = json.optInt("numeroDiente", 0),
            ortodonciaId = if (json.has("ortodonciaId") && !json.isNull("ortodonciaId")) json.optInt("ortodonciaId") else null,
            uri = readImagePayload(context, json, "restored_dental_images", json.optString("uri")),
            tipo = json.optString("tipo", "FOTO"),
            etapa = json.optString("etapa"),
            notas = json.optString("notas"),
            fecha = json.optLong("fecha", System.currentTimeMillis())
        )
    }
}

private fun TransaccionDental.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("concepto", concepto)
    .put("categoria", categoria)
    .put("tipo", tipo)
    .put("monto", monto)
    .put("fecha", fecha)
    .put("numeroDiente", numeroDiente)
    .put("visitaId", visitaId ?: JSONObject.NULL)
    .put("procedimientoId", procedimientoId ?: JSONObject.NULL)
    .put("ortodonciaId", ortodonciaId ?: JSONObject.NULL)
    .put("notas", notas)

private fun JSONArray?.toTransaccionesDentales(): List<TransaccionDental> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        TransaccionDental(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            concepto = json.optString("concepto"),
            categoria = json.optString("categoria", "OTRO"),
            tipo = json.optString("tipo", "GASTO"),
            monto = json.optDouble("monto"),
            fecha = json.optLong("fecha", System.currentTimeMillis()),
            numeroDiente = json.optInt("numeroDiente", 0),
            visitaId = if (json.has("visitaId") && !json.isNull("visitaId")) json.optInt("visitaId") else null,
            procedimientoId = if (json.has("procedimientoId") && !json.isNull("procedimientoId")) json.optInt("procedimientoId") else null,
            ortodonciaId = if (json.has("ortodonciaId") && !json.isNull("ortodonciaId")) json.optInt("ortodonciaId") else null,
            notas = json.optString("notas")
        )
    }
}

private fun Ortodoncia.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("tipo", tipo)
    .put("fechaInicio", fechaInicio)
    .put("fechaFinEstimada", fechaFinEstimada ?: JSONObject.NULL)
    .put("activo", activo)
    .put("notas", notas)
    .put("costoTotal", costoTotal)
    .put("abonoTotal", abonoTotal)
    .put("fechaRegistro", fechaRegistro)

private fun JSONArray?.toOrtodoncias(): List<Ortodoncia> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        Ortodoncia(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            tipo = json.optString("tipo", "BRACKETS"),
            fechaInicio = json.optLong("fechaInicio"),
            fechaFinEstimada = if (json.has("fechaFinEstimada") && !json.isNull("fechaFinEstimada")) json.optLong("fechaFinEstimada") else null,
            activo = json.optBoolean("activo", true),
            notas = json.optString("notas"),
            costoTotal = json.optDouble("costoTotal", 0.0),
            abonoTotal = json.optDouble("abonoTotal", 0.0),
            fechaRegistro = json.optLong("fechaRegistro", System.currentTimeMillis())
        )
    }
}

private fun AjusteOrtodoncia.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ortodonciaId", ortodonciaId)
    .put("fecha", fecha)
    .put("descripcion", descripcion)
    .put("dolor", dolor)
    .put("notas", notas)

private fun JSONArray?.toAjustesOrtodoncia(): List<AjusteOrtodoncia> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        AjusteOrtodoncia(
            id = json.optInt("id"),
            ortodonciaId = json.optInt("ortodonciaId"),
            fecha = json.optLong("fecha"),
            descripcion = json.optString("descripcion"),
            dolor = json.optString("dolor", "LEVE"),
            notas = json.optString("notas")
        )
    }
}

private fun IncidenciaOrtodoncia.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ortodonciaId", ortodonciaId ?: JSONObject.NULL)
    .put("patientId", patientId)
    .put("numeroDiente", numeroDiente)
    .put("tipo", tipo)
    .put("descripcion", descripcion)
    .put("fecha", fecha)
    .put("resuelto", resuelto)

private fun JSONArray?.toIncidenciasOrtodoncia(): List<IncidenciaOrtodoncia> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        IncidenciaOrtodoncia(
            id = json.optInt("id"),
            ortodonciaId = if (json.has("ortodonciaId") && !json.isNull("ortodonciaId")) json.optInt("ortodonciaId") else null,
            patientId = json.optInt("patientId"),
            numeroDiente = json.optInt("numeroDiente", 0),
            tipo = json.optString("tipo", "OTRO"),
            descripcion = json.optString("descripcion"),
            fecha = json.optLong("fecha", System.currentTimeMillis()),
            resuelto = json.optBoolean("resuelto", false)
        )
    }
}

private fun ElasticoOrtodoncia.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("ortodonciaId", ortodonciaId)
    .put("dienteOrigen", dienteOrigen)
    .put("dienteDestino", dienteDestino)
    .put("tipo", tipo)
    .put("activo", activo)
    .put("notas", notas)

private fun JSONArray?.toElasticosOrtodoncia(): List<ElasticoOrtodoncia> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ElasticoOrtodoncia(
            id = json.optInt("id"),
            ortodonciaId = json.optInt("ortodonciaId"),
            dienteOrigen = json.optInt("dienteOrigen"),
            dienteDestino = json.optInt("dienteDestino"),
            tipo = json.optString("tipo"),
            activo = json.optBoolean("activo", true),
            notas = json.optString("notas")
        )
    }
}

private fun RegistroSedentarismo.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("timestamp", timestamp)
    .put("tipoEvento", tipoEvento)
    .put("minutosInactivo", minutosInactivo)
    .put("notas", notas)

private fun JSONArray?.toRegistrosSedentarismo(): List<RegistroSedentarismo> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        RegistroSedentarismo(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            tipoEvento = json.optString("tipoEvento", "MOVIMIENTO"),
            minutosInactivo = json.optInt("minutosInactivo", 0),
            notas = json.optString("notas")
        )
    }
}

private fun ConfigSedentarismo.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("activado", activado)
    .put("limiteInactividadMinutos", limiteInactividadMinutos)
    .put("horaInicioMonitoreo", horaInicioMonitoreo)
    .put("horaFinMonitoreo", horaFinMonitoreo)
    .put("diasActivos", diasActivos)

private fun JSONArray?.toConfigsSedentarismo(): List<ConfigSedentarismo> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        ConfigSedentarismo(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            activado = json.optBoolean("activado", false),
            limiteInactividadMinutos = json.optInt("limiteInactividadMinutos", 60),
            horaInicioMonitoreo = json.optInt("horaInicioMonitoreo", 7),
            horaFinMonitoreo = json.optInt("horaFinMonitoreo", 22),
            diasActivos = json.optString("diasActivos", "1,2,3,4,5")
        )
    }
}

private fun RegistroHidratacion.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("cantidadMl", cantidadMl)
    .put("tipoBebida", tipoBebida)
    .put("timestamp", timestamp)

private fun JSONArray?.toRegistrosHidratacion(): List<RegistroHidratacion> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        RegistroHidratacion(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            cantidadMl = json.optInt("cantidadMl", 0),
            tipoBebida = json.optString("tipoBebida", "Agua"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

private fun FallAlert.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("patientId", patientId)
    .put("detectedAt", detectedAt)
    .put("confirmedAt", confirmedAt ?: JSONObject.NULL)
    .put("latitude", latitude ?: JSONObject.NULL)
    .put("longitude", longitude ?: JSONObject.NULL)
    .put("impactMagnitude", impactMagnitude?.toDouble() ?: JSONObject.NULL)
    .put("status", status)
    .put("notes", notes)

private fun JSONArray?.toFallAlerts(): List<FallAlert> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index) }.map { json ->
        FallAlert(
            id = json.optInt("id"),
            patientId = json.optInt("patientId"),
            detectedAt = json.optLong("detectedAt", System.currentTimeMillis()),
            confirmedAt = if (json.has("confirmedAt") && !json.isNull("confirmedAt")) json.optLong("confirmedAt") else null,
            latitude = if (json.has("latitude") && !json.isNull("latitude")) json.optDouble("latitude") else null,
            longitude = if (json.has("longitude") && !json.isNull("longitude")) json.optDouble("longitude") else null,
            impactMagnitude = if (json.has("impactMagnitude") && !json.isNull("impactMagnitude")) json.optDouble("impactMagnitude").toFloat() else null,
            status = json.optString("status", FALL_STATUS_DETECTED),
            notes = json.optString("notes")
        )
    }
}
