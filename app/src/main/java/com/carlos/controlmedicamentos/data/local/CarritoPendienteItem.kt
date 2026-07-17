package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carrito_pendiente")
data class CarritoPendienteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val medicationId: Int,
    val unidadesSolicitadas: Int
)
