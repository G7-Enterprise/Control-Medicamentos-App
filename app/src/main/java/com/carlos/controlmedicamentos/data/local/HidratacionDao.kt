package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HidratacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarToma(registro: RegistroHidratacion): Long

    @Query("DELETE FROM registro_hidratacion WHERE id = :id")
    suspend fun eliminarToma(id: Int)

    @Query("SELECT SUM(cantidadMl) FROM registro_hidratacion WHERE patientId = :patientId AND timestamp >= :inicioDelDia")
    fun obtenerTotalAguaHoy(patientId: Int, inicioDelDia: Long): Flow<Int?>

    @Query("SELECT * FROM registro_hidratacion WHERE patientId = :patientId AND timestamp >= :inicioDelDia ORDER BY timestamp DESC")
    fun obtenerTomasDeHoy(patientId: Int, inicioDelDia: Long): Flow<List<RegistroHidratacion>>

    @Query("SELECT DISTINCT strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') FROM registro_hidratacion WHERE patientId = :patientId ORDER BY 1 DESC")
    fun observarMesesConHistorial(patientId: Int): Flow<List<String>>

    @Query("SELECT * FROM registro_hidratacion WHERE patientId = :patientId AND timestamp BETWEEN :desde AND :hasta ORDER BY timestamp DESC")
    fun observarEnRango(patientId: Int, desde: Long, hasta: Long): Flow<List<RegistroHidratacion>>

    @Query("SELECT * FROM registro_hidratacion WHERE patientId = :patientId AND timestamp BETWEEN :desde AND :hasta ORDER BY timestamp DESC")
    suspend fun obtenerEnRango(patientId: Int, desde: Long, hasta: Long): List<RegistroHidratacion>

    @Query("DELETE FROM registro_hidratacion WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM registro_hidratacion")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM registro_hidratacion")
    suspend fun obtenerTodosLista(): List<RegistroHidratacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(registros: List<RegistroHidratacion>)
}
