package com.carlos.controlmedicamentos

// Enums

enum class IntakeExportPeriod(val label: String, val fileSuffix: String) {
    DAY("Día", "dia"),
    WEEK("Semana", "semana"),
    MONTH("Mes", "mes"),
    YEAR("Año", "anio")
}

enum class VitalSignsExportFilter(val label: String) {
    TODAY("Hoy"),
    WEEK("Semana"),
    MONTH("Mes"),
    CUSTOM("Rango personalizado")
}

enum class TipoSintomaCiclo(val label: String) {
    DOLOR("Dolor/Cólicos"),
    FLUJO("Flujo/Menstruación"),
    HUMOR("Estado de ánimo"),
    FISICO("Síntomas físicos"),
    LIBIDO("Libido"),
    ENERGIA("Nivel de energía"),
    SUENO("Calidad de sueño"),
    FIEBRE("Fiebre/Temperatura"),
    ANTICONCEPTIVO("Toma de anticonceptivo"),
    OTRO("Otro")
}

enum class IntensidadSintoma(val label: String, val valor: Int) {
    NINGUNA("Ninguna", 0),
    LEVE("Leve", 1),
    MODERADA("Moderada", 2),
    MODERADA_ALTA("Moderada-Alta", 3),
    SEVERA("Severa", 4),
    MUY_SEVERA("Muy severa", 5)
}

// Data classes

data class PendingIntakeRemoval(val medicationId: Int, val scheduledAt: Long)

data class AttachmentViewerState(
    val paths: List<String>,
    val currentIndex: Int = 0
) {
    val currentPath: String
        get() = paths[currentIndex]

    val canGoPrevious: Boolean
        get() = currentIndex > 0

    val canGoNext: Boolean
        get() = currentIndex < paths.lastIndex

    fun previous(): AttachmentViewerState = copy(currentIndex = (currentIndex - 1).coerceAtLeast(0))

    fun next(): AttachmentViewerState = copy(currentIndex = (currentIndex + 1).coerceAtMost(paths.lastIndex))
}
