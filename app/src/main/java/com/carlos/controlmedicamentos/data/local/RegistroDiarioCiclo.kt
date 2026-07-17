package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_diarios_ciclo",
    foreignKeys = [
        ForeignKey(
            entity = CicloMenstrual::class,
            parentColumns = ["id"],
            childColumns = ["cicloId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cicloId"]),
        Index(value = ["cicloId", "fecha"]),
        Index(value = ["tipoSintoma"])
    ]
)
data class RegistroDiarioCiclo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cicloId: Int,
    val fecha: Long,
    val tipoSintoma: String,
    val valorSintoma: String,
    val intensidad: Int? = null,
    val notas: String = ""
)
