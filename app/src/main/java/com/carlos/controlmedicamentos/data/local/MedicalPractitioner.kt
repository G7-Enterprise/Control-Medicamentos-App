package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_practitioners",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["name", "specialty"])
    ]
)
data class MedicalPractitioner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int = 0,
    val name: String,
    val specialty: String,
    val createdAt: Long = System.currentTimeMillis()
)