package com.carlos.controlmedicamentos.sync.model

const val CURRENT_SYNC_SCHEMA_VERSION: Int = 1

data class SyncDeviceInfo(
    val deviceId: String,
    val platform: String,
    val appVersion: String,
    val exportedAt: Long
)

data class SyncSnapshot(
    val schemaVersion: Int = CURRENT_SYNC_SCHEMA_VERSION,
    val source: SyncDeviceInfo,
    val patients: List<SyncPatient> = emptyList(),
    val medications: List<SyncMedication> = emptyList(),
    val medicationIntakes: List<SyncMedicationIntake> = emptyList(),
    val reports: List<SyncMedicalReport> = emptyList(),
    val appointments: List<SyncMedicalAppointment> = emptyList(),
    val practitioners: List<SyncMedicalPractitioner> = emptyList(),
    val vaccinations: List<SyncVaccinationRecord> = emptyList(),
    val vitalSigns: List<SyncVitalSigns> = emptyList()
)

data class SyncPatient(
    val id: Int,
    val nombre: String,
    val apellidos: String,
    val fechaNacimiento: Long,
    val edad: String,
    val peso: String,
    val pesoUnidad: String,
    val estatura: String,
    val estaturaUnidad: String,
    val enfermedades: String,
    val prescripciones: String,
    val pais: String = "Nicaragua",
    val moneda: String = "C$",
    val isActive: Boolean
)

data class SyncMedication(
    val id: Int,
    val patientId: Int,
    val nombre: String,
    val dosis: String,
    val formato: String,
    val formaMedicamento: String = "",
    val colorMedicamento: String = "",
    val colorMedicamento2: String = "",
    val presentacion: String,
    val concentracion: String,
    val repartoDosis: String,
    val horariosTomas: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val horaToma: String,
    val frecuenciaHoras: Int,
    val esCicloCorto: Boolean,
    val retryIntervalMinutes: Int,
    val alarmaSonidoUri: String,
    val alarmaActiva: Boolean,
    val estaActivo: Boolean,
    val stockActual: Int?,
    val stockMinimo: Int?,
    val precioPorUnidad: Double?,
    val telefonoPedidoWhatsapp: String,
    val origenReposicion: String
)

data class SyncMedicationIntake(
    val id: Int,
    val medicationId: Int,
    val patientId: Int = 0,
    val scheduledAt: Long,
    val acceptedAt: Long,
    val medicationName: String = "",
    val dosis: String = "",
    val status: String = "TAKEN"
)

data class SyncMedicalReport(
    val id: Int,
    val patientId: Int,
    val practitionerId: Int?,
    val titulo: String,
    val descripcion: String,
    val adjuntos: String,
    val createdAt: Long
)

data class SyncMedicalAppointment(
    val id: Int,
    val patientId: Int,
    val title: String,
    val doctorName: String,
    val practitionerId: Int?,
    val location: String,
    val notes: String,
    val scheduledAt: Long,
    val reminderMinutes: Int,
    val alarmEnabled: Boolean,
    val isCompleted: Boolean,
    val createdAt: Long
)

data class SyncMedicalPractitioner(
    val id: Int,
    val patientId: Int,
    val name: String,
    val specialty: String,
    val phone: String = "",
    val createdAt: Long
)

data class SyncVaccinationRecord(
    val id: Int,
    val patientId: Int,
    val vaccineName: String,
    val doseLabel: String,
    val appliedAt: Long,
    val nextDoseAt: Long?,
    val reminderMinutes: Int,
    val alarmEnabled: Boolean,
    val notes: String,
    val createdAt: Long
)

data class SyncVitalSigns(
    val id: Int,
    val patientId: Int,
    val sistolica: Int?,
    val diastolica: Int?,
    val comentarioPresion: String,
    val latidos: Int?,
    val comentarioLatidos: String,
    val spo2: Int?,
    val comentariosSpo2: String,
    val glucemia: Int?,
    val comentarioGlucemia: String,
    val temperatura: Double?,
    val comentarioTemperatura: String,
    val peso: Double?,
    val pesoUnidad: String,
    val imc: Double?,
    val fechaRegistro: Long
)
