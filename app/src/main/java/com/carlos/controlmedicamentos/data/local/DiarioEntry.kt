package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diario_entradas")
data class DiarioEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val fecha: Long,
    val texto: String,
    val rutaImagen: String? = null
)
