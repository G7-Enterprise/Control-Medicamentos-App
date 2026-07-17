package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "controles_pediatricos",
    foreignKeys = [
        ForeignKey(
            entity = NinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ninoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ControlPediatricoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ninoId: Long,
    val fechaControl: String,
    val pesoKg: Double?,
    val tallaCm: Double?,
    val perimetroCefalicoCm: Double?,
    val observaciones: String? = null
)
