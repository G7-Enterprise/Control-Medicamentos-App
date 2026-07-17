package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HidratacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarToma(registro: RegistroHidratacion)

    @Query("DELETE FROM registro_hidratacion WHERE id = :id")
    suspend fun eliminarToma(id: Int)

    @Query("SELECT SUM(cantidadMl) FROM registro_hidratacion WHERE patientId = :patientId AND timestamp >= :inicioDelDia")
    fun obtenerTotalAguaHoy(patientId: Int, inicioDelDia: Long): Flow<Int?>

    @Query("SELECT * FROM registro_hidratacion WHERE patientId = :patientId AND timestamp >= :inicioDelDia ORDER BY timestamp DESC")
    fun obtenerTomasDeHoy(patientId: Int, inicioDelDia: Long): Flow<List<RegistroHidratacion>>

    @Query("SELECT * FROM registro_hidratacion WHERE patientId = :patientId ORDER BY timestamp DESC LIMIT 50")
    fun obtenerHistorial(patientId: Int): Flow<List<RegistroHidratacion>>

    @Query("DELETE FROM registro_hidratacion WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("SELECT * FROM registro_hidratacion")
    suspend fun obtenerTodosLista(): List<RegistroHidratacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(registros: List<RegistroHidratacion>)
}
