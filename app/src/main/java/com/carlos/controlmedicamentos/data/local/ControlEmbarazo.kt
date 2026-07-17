package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "control_embarazo")
data class ControlEmbarazo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val fechaUltimaRegla: Long,
    val fechaProbableParto: Long,
    val notas: String = "",
    val activo: Boolean = true,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val fechaParto: Long? = null,
    val tipoPartoRegistrado: String = "",
    val notasParto: String = "",
    val estadoEmbarazo: String = "",
    val fechaFin: Long? = null,
    val tipoInterrupcion: String? = null,
    val metodoInterrupcion: String? = null,
    val notasInterrupcion: String? = null,
    val esPrueba: Boolean = false
)
