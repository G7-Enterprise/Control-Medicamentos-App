package com.carlos.controlmedicamentos.data.remote

object FakeVademecumRepository {
    val insumos = listOf(
        VademecumMedication(
            nombre = "Metformina",
            formatos = listOf("Comprimido", "Comprimido recubierto de liberacion prolongada"),
            presentaciones = listOf("Caja con 30 comprimidos", "Caja con 60 comprimidos"),
            concentraciones = listOf("500 mg", "850 mg", "1000 mg")
        ),
        VademecumMedication(
            nombre = "Ibuprofeno",
            formatos = listOf("Comprimido", "Capsula blanda", "Suspension oral"),
            presentaciones = listOf("Caja con 20 comprimidos", "Frasco 150 ml"),
            concentraciones = listOf("200 mg", "400 mg", "600 mg")
        ),
        VademecumMedication(
            nombre = "Paracetamol",
            formatos = listOf("Comprimido", "Solucion oral", "Supositorio"),
            presentaciones = listOf("Caja con 20 comprimidos", "Frasco 120 ml"),
            concentraciones = listOf("500 mg", "650 mg", "1 g")
        ),
        VademecumMedication(
            nombre = "Enalapril",
            formatos = listOf("Comprimido"),
            presentaciones = listOf("Caja con 28 comprimidos", "Caja con 56 comprimidos"),
            concentraciones = listOf("5 mg", "10 mg", "20 mg")
        ),
        VademecumMedication(
            nombre = "Amoxicilina",
            formatos = listOf("Capsula", "Suspension oral", "Inyectable"),
            presentaciones = listOf("Caja con 12 capsulas", "Frasco 100 ml", "Vial inyectable"),
            concentraciones = listOf("250 mg", "500 mg", "875 mg")
        )
    )

    fun buscarPorNombre(texto: String): List<VademecumMedication> {
        if (texto.isBlank()) return emptyList()
        return insumos.filter { it.nombre.contains(texto, ignoreCase = true) }
    }

    fun obtenerExacto(texto: String): VademecumMedication? {
        return insumos.firstOrNull { it.nombre.equals(texto, ignoreCase = true) }
    }
}
