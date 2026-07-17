package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object RestockSource {
    const val WHATSAPP_NUMBER = "WHATSAPP_NUMBER"
    const val WHATSAPP_CONTACT = "WHATSAPP_CONTACT"
    const val INSS = "INSS"
}

@Entity(tableName = "insumos")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val nombre: String,
    val dosis: String,
    val formato: String = "",
    val formaMedicamento: String = "",
    val colorMedicamento: String = "",
    val colorMedicamento2: String = "",
    val presentacion: String = "",
    val concentracion: String = "",
    val repartoDosis: String = "",
    val horariosTomas: String = "",
    val fechaInicio: Long, // Almacenado como timestamp
    val fechaFin: Long,
    val horaToma: String = "", // Ejemplo "08:00"
    val frecuenciaHoras: Int,
    val esCicloCorto: Boolean,
    val retryIntervalMinutes: Int = 10,
    val alarmaSonidoUri: String = "",
    val alarmaActiva: Boolean = true,
    val estaActivo: Boolean = true,
    val stockActual: Int? = null,
    val stockMinimo: Int? = null,
    val precioPorUnidad: Double? = null,
    val telefonoPedidoWhatsapp: String = "",
    val origenReposicion: String = RestockSource.WHATSAPP_NUMBER
)

fun Medication.unidadesPorToma(): Int {
    val dosisTotal = dosis.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val tomasDelDia = if (repartoDosis == "En diferentes horarios" && horariosTomas.isNotBlank()) {
        horariosTomas.split("|").filter { it.isNotBlank() }.size.coerceAtLeast(1)
    } else {
        1
    }
    return (dosisTotal / tomasDelDia).coerceAtLeast(1)
}
