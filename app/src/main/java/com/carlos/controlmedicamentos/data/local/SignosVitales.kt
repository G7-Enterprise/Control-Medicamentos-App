package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presion_arterial")
data class SignosVitales(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int = 0,
    val sistolica: Int? = null,
    val diastolica: Int? = null,
    val comentarioPresion: String = "",
    val latidos: Int? = null,
    val comentarioLatidos: String = "",
    val spo2: Int? = null,
    val comentariosSpo2: String = "",
    val glucemia: Int? = null,
    val comentarioGlucemia: String = "",
    val temperatura: Double? = null,
    val comentarioTemperatura: String = "",
    val peso: Double? = null,
    val pesoUnidad: String = "kg",
    val imc: Double? = null,
    val fechaRegistro: Long = System.currentTimeMillis()
)
