package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visita_prenatal",
    foreignKeys = [ForeignKey(
        entity = ControlEmbarazo::class,
        parentColumns = ["id"],
        childColumns = ["embarazoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("embarazoId")]
)
data class VisitaPrenatal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val embarazoId: Int,
    val fecha: Long,
    val semanasGestacion: Int,
    val peso: Float? = null,
    val presionArterial: String = "",
    val alturaUterina: Float? = null,
    val frecuenciaCardiacaFetal: Int? = null,
    val edemas: Boolean = false,
    val hemoglobina: Float? = null,
    val glucemia: Float? = null,
    val proteinasOrina: Boolean = false,
    val suplementos: String = "",
    val observaciones: String = "",
    val proximaVisitaSemanas: Int? = null,
    val facultativo: String = "",
    val contactoOMS: Int? = null
)
