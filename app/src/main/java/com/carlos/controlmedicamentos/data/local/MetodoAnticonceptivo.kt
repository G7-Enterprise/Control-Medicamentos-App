package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metodos_anticonceptivos")
data class MetodoAnticonceptivo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val tipo: String,
    val fechaInicio: Long,
    val horaToma: String = "08:00",
    val activo: Boolean = true,
    val notas: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    // Campos específicos por tipo
    val duracionCicloDias: Int? = null, // Para píldoras (21+7, 24+4, etc)
    val diasDescanso: Int? = null, // Días de placebo/descanso
    val proximaCita: Long? = null, // Para inyecciones, DIU, implante
    val recordatorioDiasAntes: Int = 3 // Días de anticipación para recordatorio
)

enum class TipoAnticonceptivo(val displayName: String, val requiereAlarmaDiaria: Boolean, val frecuenciaRecordatorio: String) {
    PILDORA_COMBINADA("Píldora combinada", true, "Diaria"),
    MINIPILDORA("Minipíldora", true, "Diaria"),
    PARCHE("Parche transdérmico", true, "Semanal"),
    ANILLO("Anillo vaginal", true, "Mensual"),
    INYECCION_MENSUAL("Inyección mensual", false, "Mensual"),
    INYECCION_TRIMESTRAL("Inyección trimestral", false, "Trimestral"),
    DIU_HORMONAL("DIU hormonal", false, "Mensual (control)"),
    DIU_COBRE("DIU de cobre", false, "Mensual (control)"),
    IMPLANTE("Implante subdérmico", false, "Semestral"),
    PRESERVATIVO("Preservativo", false, "Por uso"),
    EMERGENCIA("Anticoncepción de emergencia", false, "Situacional");

    companion object {
        fun fromDisplayName(name: String): TipoAnticonceptivo {
            return entries.find { it.displayName == name } ?: PILDORA_COMBINADA
        }
    }
}
