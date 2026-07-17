package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaccination_records")
data class VaccinationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val vaccineName: String,
    val doseLabel: String,
    val appliedAt: Long,
    val nextDoseAt: Long? = null,
    val reminderMinutes: Int = 30,
    val alarmEnabled: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
