package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiarioEntryDao {
    @Insert
    suspend fun insertar(entry: DiarioEntry): Long

    @Query("SELECT * FROM diario_entradas WHERE patientId = :patientId ORDER BY fecha DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<DiarioEntry>>

    @Query("SELECT * FROM diario_entradas WHERE patientId = :patientId AND fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC")
    suspend fun obtenerEnRango(patientId: Int, inicio: Long, fin: Long): List<DiarioEntry>

    @Query("SELECT * FROM diario_entradas WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): DiarioEntry?

    @Delete
    suspend fun eliminar(entry: DiarioEntry)

    @Update
    suspend fun actualizar(entry: DiarioEntry)

    @Query("DELETE FROM diario_entradas WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM diario_entradas")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM diario_entradas ORDER BY fecha DESC")
    suspend fun obtenerTodosLista(): List<DiarioEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(entries: List<DiarioEntry>)
}
