package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

const val FALL_STATUS_DETECTED = "DETECTED"
const val FALL_STATUS_CONFIRMED = "CONFIRMED"
const val FALL_STATUS_DISMISSED = "DISMISSED"

@Entity(tableName = "fall_alerts")
data class FallAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val detectedAt: Long,
    val confirmedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val impactMagnitude: Float? = null,
    val status: String = FALL_STATUS_DETECTED,
    val notes: String = ""
)
