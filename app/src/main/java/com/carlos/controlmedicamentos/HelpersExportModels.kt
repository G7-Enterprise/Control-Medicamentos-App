package com.carlos.controlmedicamentos

import com.carlos.controlmedicamentos.data.local.SignosVitales

data class PendingAttachmentReplacement(
    val existingPath: String,
    val newPath: String,
    val displayName: String
)

data class ReportDraftSnapshot(
    val reportId: Int? = null,
    val practitionerId: Int? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val adjuntos: List<String> = emptyList()
)

data class IntakeRemovalConfirmation(
    val medicationId: Int,
    val medicationName: String,
    val scheduledAt: Long,
    val acceptedAt: Long
)

data class VitalSignsExportRequest(
    val label: String,
    val fileSuffix: String,
    val records: List<SignosVitales>
)

data class VitalSignsExportRow(
    val recordedAt: Long,
    val systolic: Int?,
    val diastolic: Int?,
    val pressureComment: String,
    val heartRate: Int?,
    val heartRateComment: String,
    val glucose: Int?,
    val glucoseComment: String,
    val temperature: Double?,
    val temperatureComment: String,
    val peso: Double?,
    val pesoUnidad: String,
    val imc: Double?
)

data class VitalSignsExportReport(
    val title: String,
    val patientLabel: String,
    val rangeLabel: String,
    val generatedAt: String,
    val totalRecords: Int,
    val averageSystolic: String,
    val averageDiastolic: String,
    val averageHeartRate: String,
    val averageGlucose: String,
    val averageTemperature: String,
    val averageWeight: String,
    val rows: List<VitalSignsExportRow>
)

data class VitalSignsExportRange(
    val start: Long,
    val end: Long,
    val label: String,
    val fileSuffix: String
)

data class IntakeExportRange(
    val start: Long,
    val end: Long,
    val label: String,
    val fileSuffix: String
)

data class MedicationIntakeExportRow(
    val scheduledAt: Long,
    val medicationName: String,
    val dose: String,
    val status: String,
    val acceptedAt: Long?
)
