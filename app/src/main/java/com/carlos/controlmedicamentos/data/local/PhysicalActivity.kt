package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "physical_activities")
data class PhysicalActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val tipo: String,            // "caminar" | "bicicleta"
    val fechaInicio: Long,
    val fechaFin: Long = 0L,
    val pasos: Int = 0,
    val distanciaMetros: Double = 0.0,
    val duracionSegundos: Long = 0L,
    val calorias: Int = 0,
    val rutaJson: String = "",   // "lat1:lon1,lat2:lon2,..."
    val altitudInicioMetros: Double   = 0.0,  // altitud GPS al iniciar
    val altitudMaxMetros: Double      = 0.0,  // altitud máxima alcanzada
    val desnivelPositivoMetros: Double = 0.0, // metros totales de ascenso
    val desnivelNegativoMetros: Double = 0.0  // metros totales de descenso
)
