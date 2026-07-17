package com.carlos.controlmedicamentos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── 1. Perfil del dentista / profesional dental ──────────────────────────────
@Entity(tableName = "dentistas")
data class Dentista(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val nombre: String,
    val especialidad: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
)

// ── 2. Visita / Cita odontológica ─────────────────────────────────────────────
@Entity(
    tableName = "visitas_dentista",
    foreignKeys = [ForeignKey(
        entity = Dentista::class,
        parentColumns = ["id"],
        childColumns = ["dentistaId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("dentistaId"), Index("patientId")]
)
data class VisitaDentista(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val dentistaId: Int?,
    val fechaHora: Long,
    val motivo: String = "",
    val estado: String = "PENDIENTE",       // PENDIENTE | COMPLETADA | CANCELADA
    val notas: String = "",
    val recordatorio24h: Boolean = true,
    val recordatorio2h: Boolean = true,
    val seguimientoPostConsulta: Boolean = false,
    val fechaRegistro: Long = System.currentTimeMillis()
)

// ── 3. Diagnóstico por visita y diente ────────────────────────────────────────
@Entity(
    tableName = "diagnosticos_dentales",
    foreignKeys = [ForeignKey(
        entity = VisitaDentista::class,
        parentColumns = ["id"],
        childColumns = ["visitaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("visitaId")]
)
data class DiagnosticoDental(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitaId: Int,
    val patientId: Int,
    val numeroDiente: Int = 0,           // 0 = general, 11-48 = notación FDI
    val zona: String = "",               // ej. "Superior derecho"
    val descripcion: String,
    val estado: String = "ACTIVO",       // ACTIVO | RESUELTO | EN_TRATAMIENTO
    val fechaRegistro: Long = System.currentTimeMillis()
)

// ── 4. Procedimiento realizado o pendiente ────────────────────────────────────
@Entity(
    tableName = "procedimientos_dentales",
    foreignKeys = [ForeignKey(
        entity = DiagnosticoDental::class,
        parentColumns = ["id"],
        childColumns = ["diagnosticoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("diagnosticoId"), Index("patientId")]
)
data class ProcedimientoDental(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val diagnosticoId: Int,
    val patientId: Int,
    val tipo: String,                    // Empaste, Extraccion, Limpieza, Endodoncia, Corona, Ortodoncia, Otro
    val descripcion: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val completado: Boolean = false,
    val costo: Double? = null,
    val notas: String = ""
)

// ── 5. Prescripción dental ────────────────────────────────────────────────────
@Entity(
    tableName = "prescripciones_dentales",
    foreignKeys = [ForeignKey(
        entity = VisitaDentista::class,
        parentColumns = ["id"],
        childColumns = ["visitaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("visitaId"), Index("patientId")]
)
data class PrescripcionDental(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitaId: Int,
    val patientId: Int,
    val medicamento: String,
    val dosis: String = "",
    val frecuencia: String = "",
    val duracionDias: Int = 0,
    val sincronizadaConAlarma: Boolean = false,
    val fechaRegistro: Long = System.currentTimeMillis()
)

// ── 6. Estado visual por diente (para odontograma con color) ─────────────────
@Entity(
    tableName = "diente_estado",
    indices = [Index(value = ["patientId", "numeroDiente"], unique = true)]
)
data class DienteEstado(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val numeroDiente: Int,
    val estado: String = "SANO", // SANO, TRATAMIENTO_PENDIENTE, EN_TRATAMIENTO, TRATAMIENTO_FINALIZADO, OBSERVACION, ORTODONCIA
    val notas: String = "",
    val fechaActualizacion: Long = System.currentTimeMillis()
)

// ── 7. Imágenes vinculadas a diente, ortodoncia o sonrisa ─────────────────────
@Entity(
    tableName = "imagenes_dentales",
    indices = [Index("patientId"), Index("numeroDiente"), Index("ortodonciaId")]
)
data class ImagenDental(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val numeroDiente: Int = 0,          // 0 = general / sonrisa / ortodoncia
    val ortodonciaId: Int? = null,
    val uri: String,                    // ruta de archivo
    val tipo: String = "FOTO",          // RADIOGRAFIA, FOTO, ORTODONCIA, SONRISA
    val etapa: String = "",             // antes / despues / progreso (solo SONRISA)
    val notas: String = "",
    val fecha: Long = System.currentTimeMillis()
)

// ── 8. Transacciones financieras (visitas, tratamientos, medicamentos, etc.) ──
@Entity(
    tableName = "transacciones_dentales",
    indices = [Index("patientId"), Index("numeroDiente"), Index("visitaId"), Index("procedimientoId"), Index("ortodonciaId")]
)
data class TransaccionDental(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val concepto: String,
    val categoria: String = "OTRO",     // VISITA, TRATAMIENTO, MEDICAMENTO, ORTODONCIA, OTRO
    val tipo: String = "GASTO",         // INGRESO, GASTO
    val monto: Double,
    val fecha: Long = System.currentTimeMillis(),
    val numeroDiente: Int = 0,
    val visitaId: Int? = null,
    val procedimientoId: Int? = null,
    val ortodonciaId: Int? = null,
    val notas: String = "",
    val reciboUri: String = ""
)

// ── 9. Tratamiento de ortodoncia ──────────────────────────────────────────────
@Entity(tableName = "ortodoncias", indices = [Index("patientId")])
data class Ortodoncia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val tipo: String = "BRACKETS",      // BRACKETS, INVISALIGN, RETENEDOR, OTRO
    val fechaInicio: Long,
    val fechaFinEstimada: Long? = null,
    val activo: Boolean = true,
    val notas: String = "",
    val costoTotal: Double = 0.0,
    val abonoTotal: Double = 0.0,
    val fechaRegistro: Long = System.currentTimeMillis()
)

// ── 10. Ajustes de ortodoncia (aprietes, cambios de arco) ─────────────────────
@Entity(
    tableName = "ajustes_ortodoncia",
    foreignKeys = [ForeignKey(
        entity = Ortodoncia::class,
        parentColumns = ["id"],
        childColumns = ["ortodonciaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ortodonciaId")]
)
data class AjusteOrtodoncia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ortodonciaId: Int,
    val fecha: Long,
    val descripcion: String,
    val dolor: String = "LEVE",         // NINGUNO, LEVE, MODERADO, SEVERO
    val notas: String = ""
)

// ── 11. Incidencias / reparaciones de ortodoncia ─────────────────────────────
@Entity(
    tableName = "incidencias_ortodoncia",
    foreignKeys = [ForeignKey(
        entity = Ortodoncia::class,
        parentColumns = ["id"],
        childColumns = ["ortodonciaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ortodonciaId"), Index("patientId"), Index("numeroDiente")]
)
data class IncidenciaOrtodoncia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ortodonciaId: Int? = null,
    val patientId: Int,
    val numeroDiente: Int = 0,
    val tipo: String = "OTRO",          // BRACKET_DESPEGADO, ALAMBRE_PUNZANTE, GOMA_ROTA, ULCERA, OTRO
    val descripcion: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val resuelto: Boolean = false
)

// ── 12. Mapeo de elásticos (gomas) entre dientes ──────────────────────────────
@Entity(
    tableName = "elasticos_ortodoncia",
    foreignKeys = [ForeignKey(
        entity = Ortodoncia::class,
        parentColumns = ["id"],
        childColumns = ["ortodonciaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ortodonciaId")]
)
data class ElasticoOrtodoncia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ortodonciaId: Int,
    val dienteOrigen: Int,
    val dienteDestino: Int,
    val tipo: String = "",              // ej. "Conejo", "Clase II", "Intermaxilar"
    val activo: Boolean = true,
    val notas: String = ""
)
