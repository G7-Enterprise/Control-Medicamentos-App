package com.carlos.controlmedicamentos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_appointments",
    indices = [
        Index(value = ["patientId", "scheduledAt"]),
        Index(value = ["practitionerId"])
    ]
)
data class MedicalAppointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val title: String,
    @ColumnInfo(defaultValue = "''")
    val doctorName: String = "",
    val practitionerId: Int? = null,
    @ColumnInfo(defaultValue = "''")
    val location: String = "",
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    val scheduledAt: Long,
    @ColumnInfo(defaultValue = "60")
    val reminderMinutes: Int = 60,
    @ColumnInfo(defaultValue = "1")
    val alarmEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)