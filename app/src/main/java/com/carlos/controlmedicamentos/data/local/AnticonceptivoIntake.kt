package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anticonceptivo_intakes",
    indices = [Index(value = ["metodoId", "scheduledAt"], unique = true)]
)
data class AnticonceptivoIntake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val metodoId: Int,
    val scheduledAt: Long,
    val acceptedAt: Long = System.currentTimeMillis()
)
