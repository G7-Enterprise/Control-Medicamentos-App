package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ninos")
data class NinoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Int, // Relación con el paciente madre
    val embarazoId: Int, // Relación con el embarazo
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String, // "Niño", "Niña", "No definido"
    val notasParto: String? = null,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val esPrueba: Boolean = false
)
