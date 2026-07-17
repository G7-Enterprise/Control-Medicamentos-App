package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_orders",
    indices = [
        Index(value = ["patientId", "createdAt"]),
        Index(value = ["createdAt"])
    ]
)
data class MedicationOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int,
    val pricedItemCount: Int,
    val totalAmount: Double?,
    val restockSource: String,
    val supplierLabel: String,
    val whatsappPhone: String = "",
    val itemsSummary: String,
    val messagePreview: String
)