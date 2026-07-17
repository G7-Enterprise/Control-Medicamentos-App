package com.carlos.controlmedicamentos

import java.util.Calendar

fun inicioDeLaSemana(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return cal.timeInMillis
}

fun finDeLaSemana(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        add(Calendar.WEEK_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

fun inicioDelMes(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun finDelMes(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}


fun decodeAttachmentPaths(json: String?): List<String> {
    // TODO: Implement real decoding logic
    return json?.let { listOf(it) } ?: emptyList()
}

fun siguienteHoraDisponible(): Long {
    // Returns current time + 1 hour as a placeholder
    return System.currentTimeMillis() + 60 * 60 * 1000
}

fun buildMedicationConsumptionSummary(
    medicationId: Int,
    intakes: List<Any>,
    periodLabel: String
): String {
    // TODO: Implement real summary logic
    return "Resumen de consumo para $periodLabel"
}

