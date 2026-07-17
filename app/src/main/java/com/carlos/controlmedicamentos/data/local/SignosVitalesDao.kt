package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignosVitalesDao {
    @Insert
    suspend fun insertar(signos: SignosVitales)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(signos: List<SignosVitales>)

    @Query("SELECT * FROM presion_arterial ORDER BY fechaRegistro DESC LIMIT 1")
    suspend fun obtenerUltimoRegistro(): SignosVitales?

    @Query("SELECT * FROM presion_arterial WHERE patientId = :patientId ORDER BY fechaRegistro DESC LIMIT 1")
    suspend fun obtenerUltimoRegistroPorPaciente(patientId: Int): SignosVitales?

    @Query("SELECT * FROM presion_arterial WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun obtenerPorPaciente(patientId: Int): Flow<List<SignosVitales>>

    @Query("SELECT * FROM presion_arterial ORDER BY fechaRegistro DESC")
    fun obtenerTodos(): Flow<List<SignosVitales>>

    @Query("SELECT * FROM presion_arterial ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<SignosVitales>

    @Query("SELECT * FROM presion_arterial WHERE patientId = :patientId AND fechaRegistro BETWEEN :start AND :end ORDER BY fechaRegistro ASC")
    suspend fun obtenerEnRango(patientId: Int, start: Long, end: Long): List<SignosVitales>

    @Query("DELETE FROM presion_arterial WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM presion_arterial")
    suspend fun eliminarTodos()
}
