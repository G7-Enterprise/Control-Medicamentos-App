package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert
    suspend fun insertar(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(medications: List<Medication>)

    @Query("SELECT * FROM insumos WHERE estaActivo = 1")
    fun obtenerActivos(): Flow<List<Medication>>

    @Query("SELECT * FROM insumos WHERE estaActivo = 1 AND patientId = :patientId")
    fun obtenerActivosPorPaciente(patientId: Int): Flow<List<Medication>>

    @Query("SELECT * FROM insumos WHERE patientId = :patientId ORDER BY estaActivo DESC, nombre ASC")
    fun observarTodosPorPaciente(patientId: Int): Flow<List<Medication>>

    @Query("SELECT * FROM insumos WHERE estaActivo = 1 AND alarmaActiva = 1")
    suspend fun obtenerActivosConAlarma(): List<Medication>

    @Query("SELECT * FROM insumos ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<Medication>

    @Query("SELECT * FROM insumos WHERE patientId = :patientId ORDER BY nombre ASC")
    suspend fun obtenerTodosPorPacienteLista(patientId: Int): List<Medication>

    @Query("DELETE FROM insumos WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Update
    suspend fun actualizar(medication: Medication)

    @Delete
    suspend fun eliminar(medication: Medication)

    @Query("UPDATE insumos SET estaActivo = :estado WHERE id = :id")
    suspend fun cambiarEstado(id: Int, estado: Boolean)

    @Query("UPDATE insumos SET alarmaActiva = :estado WHERE id = :id")
    suspend fun cambiarAlarmaActiva(id: Int, estado: Boolean)

    @Query("UPDATE insumos SET stockActual = :stockActual WHERE id = :id")
    suspend fun actualizarStock(id: Int, stockActual: Int?)

    @Query(
        "UPDATE insumos SET retryIntervalMinutes = :retryIntervalMinutes, alarmaSonidoUri = :alarmaSonidoUri"
    )
    suspend fun actualizarConfiguracionAlertasCriticas(
        retryIntervalMinutes: Int,
        alarmaSonidoUri: String
    )

    @Query("SELECT * FROM insumos WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): Medication?

    @Query("DELETE FROM insumos")
    suspend fun eliminarTodos()
}
