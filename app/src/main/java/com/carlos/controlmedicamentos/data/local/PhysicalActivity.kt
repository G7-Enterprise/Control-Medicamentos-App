package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ActivityOrigin {
    BACKGROUND_DETECTED,
    MANUAL_EXERCISE
}

enum class ActivityEventType {
    MOVEMENT,
    INACTIVITY,
    ALERT_RESPONSE
}

class ActivityConverters {
    @TypeConverter
    fun fromOrigin(origin: ActivityOrigin): String = origin.name

    @TypeConverter
    fun toOrigin(value: String): ActivityOrigin =
        runCatching { ActivityOrigin.valueOf(value) }
            .getOrDefault(ActivityOrigin.MANUAL_EXERCISE)

    @TypeConverter
    fun fromEventType(eventType: ActivityEventType): String = eventType.name

    @TypeConverter
    fun toEventType(value: String): ActivityEventType =
        runCatching { ActivityEventType.valueOf(value) }
            .getOrDefault(ActivityEventType.MOVEMENT)
}

@Entity(tableName = "physical_activities")
data class PhysicalActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val tipo: String,            // "caminar" | "bicicleta"
    val fechaInicio: Long,
    val fechaFin: Long = 0L,
    val pasos: Int = 0,
    val distanciaMetros: Double = 0.0,
    val duracionSegundos: Long = 0L,
    val calorias: Int = 0,
    val rutaJson: String = "",   // "lat1:lon1,lat2:lon2,..."
    val altitudInicioMetros: Double   = 0.0,  // altitud GPS al iniciar
    val altitudMaxMetros: Double      = 0.0,  // altitud máxima alcanzada
    val desnivelPositivoMetros: Double = 0.0, // metros totales de ascenso
    val desnivelNegativoMetros: Double  = 0.0, // metros totales de descenso
    val origen: ActivityOrigin = ActivityOrigin.MANUAL_EXERCISE,
    val tipoEvento: ActivityEventType = ActivityEventType.MOVEMENT,
    val minutosInactivo: Int = 0,
    val notas: String = ""
)
