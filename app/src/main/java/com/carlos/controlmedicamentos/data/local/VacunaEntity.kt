package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "vacunas",
    foreignKeys = [
        ForeignKey(
            entity = NinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ninoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VacunaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ninoId: Long,
    val nombre: String,
    val descripcion: String,
    val edadRecomendada: String, // Ej: "Recién Nacido", "2 meses", "11-12 años"
    val estaAplicada: Boolean = false,
    val fechaAplicacion: String? = null
)
