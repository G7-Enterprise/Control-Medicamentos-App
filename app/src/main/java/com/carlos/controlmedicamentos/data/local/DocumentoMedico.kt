package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documentos_medicos",
    foreignKeys = [ForeignKey(
        entity = ControlEmbarazo::class,
        parentColumns = ["id"],
        childColumns = ["idEmbarazo"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idEmbarazo")]
)
data class DocumentoMedico(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idEmbarazo: Int,
    val tipoDocumento: String = "Ecografía",
    val fecha: Long = System.currentTimeMillis(),
    val rutaImagen: String,
    val descripcion: String = ""
)
