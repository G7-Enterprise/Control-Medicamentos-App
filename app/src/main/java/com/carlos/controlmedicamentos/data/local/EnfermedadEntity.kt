package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "enfermedades_alergias",
    foreignKeys = [
        ForeignKey(
            entity = NinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ninoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EnfermedadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ninoId: Long,
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val sintomas: String? = null,
    val planPersonal: String? = null,
    val esAlergia: Boolean = false // false para enfermedad común, true para alergia permanente
)
