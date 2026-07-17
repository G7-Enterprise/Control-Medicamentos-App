package com.carlos.controlmedicamentos.data.remote

data class VademecumMedication(
    val nombre: String,
    val formatos: List<String>,
    val presentaciones: List<String>,
    val concentraciones: List<String>
)
