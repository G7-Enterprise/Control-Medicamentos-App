package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ciclos_menstruales")
data class CicloMenstrual(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val fechaInicio: Long,
    val duracionDias: Int = 5,
    val duracionCicloDias: Int = 28,
    val sintomas: String = "",
    val notas: String = ""
)
