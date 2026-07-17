package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_profile")
data class PatientProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val apellidos: String,
    val fechaNacimiento: Long = 0L,
    val edad: String,
    val peso: String,
    val pesoUnidad: String = "kg",
    val estatura: String,
    val estaturaUnidad: String = "cm",
    val enfermedades: String,
    val prescripciones: String,
    val ultimoAnalisisIa: String = "",
    val sexo: String = "",
    val pais: String = "Nicaragua",
    val moneda: String = "C$",
    val isActive: Boolean = false,
    val fotoPerfil: String? = null
)
