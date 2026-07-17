package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_reports",
    indices = [Index(value = ["practitionerId"])]
)
data class MedicalReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val practitionerId: Int? = null,
    val titulo: String,
    val descripcion: String,
    val adjuntos: String = "",
    val analisisIa: String = "",
    val createdAt: Long = System.currentTimeMillis()
)