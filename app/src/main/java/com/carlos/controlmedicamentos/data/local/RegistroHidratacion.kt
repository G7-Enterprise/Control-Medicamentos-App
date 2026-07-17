package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registro_hidratacion",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["timestamp"])
    ]
)
data class RegistroHidratacion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int = 0,
    val cantidadMl: Int,
    val tipoBebida: String = "Agua",
    val timestamp: Long = System.currentTimeMillis()
)
