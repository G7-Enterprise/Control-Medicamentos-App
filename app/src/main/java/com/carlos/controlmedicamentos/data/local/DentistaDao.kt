package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Dentista ──────────────────────────────────────────────────────────────────
@Dao
interface DentistaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(dentista: Dentista): Long

    @Update
    suspend fun actualizar(dentista: Dentista)

    @Delete
    suspend fun eliminar(dentista: Dentista)

    @Query("SELECT * FROM dentistas WHERE patientId = :patientId ORDER BY nombre ASC")
    fun observarPorPaciente(patientId: Int): Flow<List<Dentista>>

    @Query("SELECT * FROM dentistas WHERE patientId = :patientId ORDER BY nombre ASC")
    suspend fun obtenerPorPacienteLista(patientId: Int): List<Dentista>

    @Query("SELECT * FROM dentistas WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Dentista?

    @Query("DELETE FROM dentistas WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM dentistas")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM dentistas")
    suspend fun obtenerTodosLista(): List<Dentista>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(dentistas: List<Dentista>)
}

// ── Visita Dentista ───────────────────────────────────────────────────────────
@Dao
interface VisitaDentistaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(visita: VisitaDentista): Long

    @Update
    suspend fun actualizar(visita: VisitaDentista)

    @Delete
    suspend fun eliminar(visita: VisitaDentista)

    @Query("SELECT * FROM visitas_dentista WHERE patientId = :patientId ORDER BY fechaHora DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<VisitaDentista>>

    @Query("SELECT * FROM visitas_dentista WHERE patientId = :patientId ORDER BY fechaHora DESC")
    suspend fun obtenerPorPacienteLista(patientId: Int): List<VisitaDentista>

    @Query("SELECT * FROM visitas_dentista WHERE id = :id")
    suspend fun obtenerPorId(id: Int): VisitaDentista?

    @Query("SELECT * FROM visitas_dentista WHERE patientId = :patientId AND fechaHora > :ahora AND estado = 'PENDIENTE' ORDER BY fechaHora ASC LIMIT 1")
    suspend fun proximaCitaPendiente(patientId: Int, ahora: Long): VisitaDentista?

    @Query("SELECT * FROM visitas_dentista WHERE patientId = :patientId AND estado = 'PENDIENTE' AND fechaHora > :ahora ORDER BY fechaHora ASC")
    suspend fun obtenerPendientesConAlarma(patientId: Int, ahora: Long): List<VisitaDentista>

    @Query("SELECT * FROM visitas_dentista WHERE seguimientoPostConsulta = 1 AND estado = 'COMPLETADA'")
    suspend fun obtenerConSeguimientoPendiente(): List<VisitaDentista>

    @Query("DELETE FROM visitas_dentista WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM visitas_dentista")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM visitas_dentista")
    suspend fun obtenerTodosLista(): List<VisitaDentista>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(visitas: List<VisitaDentista>)
}

// ── Diagnóstico ───────────────────────────────────────────────────────────────
@Dao
interface DiagnosticoDentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(dx: DiagnosticoDental): Long

    @Update
    suspend fun actualizar(dx: DiagnosticoDental)

    @Delete
    suspend fun eliminar(dx: DiagnosticoDental)

    @Query("SELECT * FROM diagnosticos_dentales WHERE visitaId = :visitaId ORDER BY numeroDiente ASC")
    fun observarPorVisita(visitaId: Int): Flow<List<DiagnosticoDental>>

    @Query("SELECT * FROM diagnosticos_dentales WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<DiagnosticoDental>>

    @Query("SELECT * FROM diagnosticos_dentales WHERE patientId = :patientId AND numeroDiente = :diente ORDER BY fechaRegistro DESC")
    suspend fun obtenerPorDiente(patientId: Int, diente: Int): List<DiagnosticoDental>

    @Query("SELECT * FROM diagnosticos_dentales WHERE visitaId = :visitaId")
    suspend fun obtenerPorVisitaLista(visitaId: Int): List<DiagnosticoDental>

    @Query("DELETE FROM diagnosticos_dentales WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM diagnosticos_dentales")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM diagnosticos_dentales")
    suspend fun obtenerTodosLista(): List<DiagnosticoDental>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<DiagnosticoDental>)
}

// ── Procedimiento ─────────────────────────────────────────────────────────────
@Dao
interface ProcedimientoDentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(p: ProcedimientoDental): Long

    @Update
    suspend fun actualizar(p: ProcedimientoDental)

    @Delete
    suspend fun eliminar(p: ProcedimientoDental)

    @Query("SELECT * FROM procedimientos_dentales WHERE diagnosticoId = :diagnosticoId ORDER BY fecha DESC")
    fun observarPorDiagnostico(diagnosticoId: Int): Flow<List<ProcedimientoDental>>

    @Query("SELECT * FROM procedimientos_dentales WHERE patientId = :patientId ORDER BY fecha DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<ProcedimientoDental>>

    @Query("SELECT * FROM procedimientos_dentales WHERE patientId = :patientId AND completado = 0 ORDER BY fecha ASC")
    fun observarPendientes(patientId: Int): Flow<List<ProcedimientoDental>>

    @Query("DELETE FROM procedimientos_dentales WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM procedimientos_dentales")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM procedimientos_dentales")
    suspend fun obtenerTodosLista(): List<ProcedimientoDental>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<ProcedimientoDental>)
}

// ── Prescripción dental ───────────────────────────────────────────────────────
@Dao
interface PrescripcionDentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(p: PrescripcionDental): Long

    @Update
    suspend fun actualizar(p: PrescripcionDental)

    @Delete
    suspend fun eliminar(p: PrescripcionDental)

    @Query("SELECT * FROM prescripciones_dentales WHERE visitaId = :visitaId ORDER BY fechaRegistro DESC")
    fun observarPorVisita(visitaId: Int): Flow<List<PrescripcionDental>>

    @Query("SELECT * FROM prescripciones_dentales WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<PrescripcionDental>>

    @Query("DELETE FROM prescripciones_dentales WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM prescripciones_dentales")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM prescripciones_dentales")
    suspend fun obtenerTodosLista(): List<PrescripcionDental>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<PrescripcionDental>)
}

// ── Estado visual por diente ──────────────────────────────────────────────────
@Dao
interface DienteEstadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(estado: DienteEstado): Long

    @Update
    suspend fun actualizar(estado: DienteEstado)

    @Query("SELECT * FROM diente_estado WHERE patientId = :patientId")
    fun observarPorPaciente(patientId: Int): Flow<List<DienteEstado>>

    @Query("SELECT * FROM diente_estado WHERE patientId = :patientId AND numeroDiente = :numero")
    suspend fun obtener(patientId: Int, numero: Int): DienteEstado?

    @Query("DELETE FROM diente_estado WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM diente_estado")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM diente_estado")
    suspend fun obtenerTodosLista(): List<DienteEstado>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<DienteEstado>)
}

// ── Imágenes dentales / sonrisa / radiografías ────────────────────────────────
@Dao
interface ImagenDentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(imagen: ImagenDental): Long

    @Delete
    suspend fun eliminar(imagen: ImagenDental)

    @Query("SELECT * FROM imagenes_dentales WHERE patientId = :patientId ORDER BY fecha DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<ImagenDental>>

    @Query("SELECT * FROM imagenes_dentales WHERE patientId = :patientId AND numeroDiente = :numero ORDER BY fecha DESC")
    fun observarPorDiente(patientId: Int, numero: Int): Flow<List<ImagenDental>>

    @Query("SELECT * FROM imagenes_dentales WHERE patientId = :patientId AND tipo = :tipo ORDER BY fecha DESC")
    fun observarPorTipo(patientId: Int, tipo: String): Flow<List<ImagenDental>>

    @Query("SELECT * FROM imagenes_dentales WHERE ortodonciaId = :ortodonciaId ORDER BY fecha DESC")
    fun observarPorOrtodoncia(ortodonciaId: Int): Flow<List<ImagenDental>>

    @Query("SELECT * FROM imagenes_dentales")
    suspend fun obtenerTodosLista(): List<ImagenDental>

    @Query("DELETE FROM imagenes_dentales")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<ImagenDental>)
}

// ── Finanzas dentales ─────────────────────────────────────────────────────────
@Dao
interface TransaccionDentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(t: TransaccionDental): Long

    @Update
    suspend fun actualizar(t: TransaccionDental)

    @Delete
    suspend fun eliminar(t: TransaccionDental)

    @Query("SELECT * FROM transacciones_dentales WHERE patientId = :patientId ORDER BY fecha DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<TransaccionDental>>

    @Query("SELECT * FROM transacciones_dentales WHERE patientId = :patientId AND numeroDiente = :numero ORDER BY fecha DESC")
    fun observarPorDiente(patientId: Int, numero: Int): Flow<List<TransaccionDental>>

    @Query("SELECT SUM(monto) FROM transacciones_dentales WHERE patientId = :patientId AND tipo = :tipo")
    fun totalPorTipo(patientId: Int, tipo: String): Flow<Double?>

    @Query("SELECT * FROM transacciones_dentales")
    suspend fun obtenerTodosLista(): List<TransaccionDental>

    @Query("DELETE FROM transacciones_dentales")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<TransaccionDental>)
}

// ── Ortodoncia ────────────────────────────────────────────────────────────────
@Dao
interface OrtodonciaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(o: Ortodoncia): Long

    @Update
    suspend fun actualizar(o: Ortodoncia)

    @Delete
    suspend fun eliminar(o: Ortodoncia)

    @Query("SELECT * FROM ortodoncias WHERE patientId = :patientId ORDER BY fechaInicio DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<Ortodoncia>>

    @Query("SELECT * FROM ortodoncias WHERE patientId = :patientId AND activo = 1 ORDER BY fechaInicio DESC LIMIT 1")
    suspend fun obtenerActiva(patientId: Int): Ortodoncia?

    @Query("SELECT * FROM ortodoncias")
    suspend fun obtenerTodosLista(): List<Ortodoncia>

    @Query("DELETE FROM ortodoncias")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<Ortodoncia>)
}

@Dao
interface AjusteOrtodonciaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(a: AjusteOrtodoncia): Long

    @Delete
    suspend fun eliminar(a: AjusteOrtodoncia)

    @Query("SELECT * FROM ajustes_ortodoncia WHERE ortodonciaId = :ortodonciaId ORDER BY fecha DESC")
    fun observarPorOrtodoncia(ortodonciaId: Int): Flow<List<AjusteOrtodoncia>>

    @Query("SELECT * FROM ajustes_ortodoncia")
    suspend fun obtenerTodosLista(): List<AjusteOrtodoncia>

    @Query("DELETE FROM ajustes_ortodoncia")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<AjusteOrtodoncia>)
}

@Dao
interface IncidenciaOrtodonciaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(i: IncidenciaOrtodoncia): Long

    @Update
    suspend fun actualizar(i: IncidenciaOrtodoncia)

    @Delete
    suspend fun eliminar(i: IncidenciaOrtodoncia)

    @Query("SELECT * FROM incidencias_ortodoncia WHERE patientId = :patientId ORDER BY fecha DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<IncidenciaOrtodoncia>>

    @Query("SELECT * FROM incidencias_ortodoncia WHERE ortodonciaId = :ortodonciaId ORDER BY fecha DESC")
    fun observarPorOrtodoncia(ortodonciaId: Int): Flow<List<IncidenciaOrtodoncia>>

    @Query("SELECT * FROM incidencias_ortodoncia")
    suspend fun obtenerTodosLista(): List<IncidenciaOrtodoncia>

    @Query("DELETE FROM incidencias_ortodoncia")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<IncidenciaOrtodoncia>)
}

@Dao
interface ElasticoOrtodonciaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(e: ElasticoOrtodoncia): Long

    @Delete
    suspend fun eliminar(e: ElasticoOrtodoncia)

    @Query("SELECT * FROM elasticos_ortodoncia WHERE ortodonciaId = :ortodonciaId ORDER BY id DESC")
    fun observarPorOrtodoncia(ortodonciaId: Int): Flow<List<ElasticoOrtodoncia>>

    @Query("SELECT * FROM elasticos_ortodoncia")
    suspend fun obtenerTodosLista(): List<ElasticoOrtodoncia>

    @Query("DELETE FROM elasticos_ortodoncia")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<ElasticoOrtodoncia>)
}
