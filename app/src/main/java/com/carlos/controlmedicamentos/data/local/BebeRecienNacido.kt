package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bebes_recien_nacidos")
data class BebeRecienNacido(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val embarazoId: Int,
    val patientId: Int,
    val nombre: String,
    val sexo: String,
    val fechaNacimiento: Long,
    val pesoAlNacer: String = "",
    val tallaAlNacer: String = "",
    val notas: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
)
