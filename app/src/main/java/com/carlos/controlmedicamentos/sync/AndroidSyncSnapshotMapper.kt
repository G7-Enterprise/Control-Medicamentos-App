package com.carlos.controlmedicamentos.sync

import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MedicationIntake
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.SignosVitales
import com.carlos.controlmedicamentos.data.local.VaccinationRecord
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

internal fun buildAndroidSyncSnapshot(
    deviceId: String,
    appVersion: String,
    exportedAt: Long,
    patients: List<PatientProfile>,
    medications: List<Medication>,
    medicationIntakes: List<MedicationIntake>,
    reports: List<MedicalReport>,
    appointments: List<MedicalAppointment>,
    practitioners: List<MedicalPractitioner>,
    vaccinations: List<VaccinationRecord>,
    vitalSigns: List<SignosVitales>
): SyncSnapshot {
    return SyncSnapshot(
        source = SyncDeviceInfo(
            deviceId = deviceId,
            platform = "android",
            appVersion = appVersion,
            exportedAt = exportedAt
        ),
        patients = patients.map(PatientProfile::toSyncModel),
        medications = medications.map(Medication::toSyncModel),
        medicationIntakes = medicationIntakes.map(MedicationIntake::toSyncModel),
        reports = reports.map(MedicalReport::toSyncModel),
        appointments = appointments.map(MedicalAppointment::toSyncModel),
        practitioners = practitioners.map(MedicalPractitioner::toSyncModel),
        vaccinations = vaccinations.map(VaccinationRecord::toSyncModel),
        vitalSigns = vitalSigns.map(SignosVitales::toSyncModel)
    )
}

internal fun PatientProfile.toSyncModel(): SyncPatient = SyncPatient(
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

internal fun Medication.toSyncModel(): SyncMedication = SyncMedication(
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

internal fun MedicationIntake.toSyncModel(): SyncMedicationIntake = SyncMedicationIntake(
    id = id,
    medicationId = medicationId,
    patientId = patientId,
    scheduledAt = scheduledAt,
    acceptedAt = acceptedAt,
    medicationName = medicationName,
    dosis = dosis,
    status = status
)

internal fun MedicalReport.toSyncModel(): SyncMedicalReport = SyncMedicalReport(
    id = id,
    patientId = patientId,
    practitionerId = practitionerId,
    titulo = titulo,
    descripcion = descripcion,
    adjuntos = adjuntos,
    createdAt = createdAt
)

internal fun MedicalAppointment.toSyncModel(): SyncMedicalAppointment = SyncMedicalAppointment(
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

internal fun MedicalPractitioner.toSyncModel(): SyncMedicalPractitioner = SyncMedicalPractitioner(
    id = id,
    patientId = patientId,
    name = name,
    specialty = specialty,
    phone = phone,
    createdAt = createdAt
)

internal fun VaccinationRecord.toSyncModel(): SyncVaccinationRecord = SyncVaccinationRecord(
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

internal fun SignosVitales.toSyncModel(): SyncVitalSigns = SyncVitalSigns(
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
