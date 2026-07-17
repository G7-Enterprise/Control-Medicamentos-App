package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val MEDICATION_INTAKE_STATUS_TAKEN = "TAKEN"
const val MEDICATION_INTAKE_STATUS_NOT_TAKEN = "NOT_TAKEN"

@Entity(
    tableName = "medication_intakes",
    indices = [Index(value = ["medicationId", "scheduledAt"], unique = true)]
)
data class MedicationIntake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val patientId: Int = 0,
    val scheduledAt: Long,
    val acceptedAt: Long = System.currentTimeMillis(),
    val medicationName: String = "",
    val dosis: String = "",
    val status: String = MEDICATION_INTAKE_STATUS_TAKEN
)
