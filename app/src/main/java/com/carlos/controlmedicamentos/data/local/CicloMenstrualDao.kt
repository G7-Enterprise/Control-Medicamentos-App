package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CicloMenstrualDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(ciclo: CicloMenstrual): Long

    @Update
    suspend fun actualizar(ciclo: CicloMenstrual)

    @Delete
    suspend fun eliminar(ciclo: CicloMenstrual)

    @Query("SELECT * FROM ciclos_menstruales WHERE patientId = :patientId ORDER BY fechaInicio DESC, id DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<CicloMenstrual>>

    @Query("SELECT * FROM ciclos_menstruales WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): CicloMenstrual?

    @Query("DELETE FROM ciclos_menstruales WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("SELECT * FROM ciclos_menstruales ORDER BY fechaInicio DESC")
    suspend fun obtenerTodosLista(): List<CicloMenstrual>

    @Query("DELETE FROM ciclos_menstruales")
    suspend fun eliminarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(ciclos: List<CicloMenstrual>)
}
