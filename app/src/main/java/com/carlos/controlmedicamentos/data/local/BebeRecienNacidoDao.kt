package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BebeRecienNacidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(bebe: BebeRecienNacido): Long

    @Update
    suspend fun actualizar(bebe: BebeRecienNacido)

    @Delete
    suspend fun eliminar(bebe: BebeRecienNacido)

    @Query("SELECT * FROM bebes_recien_nacidos WHERE embarazoId = :embarazoId ORDER BY fechaNacimiento ASC")
    fun observarPorEmbarazo(embarazoId: Int): Flow<List<BebeRecienNacido>>

    @Query("SELECT * FROM bebes_recien_nacidos WHERE patientId = :patientId ORDER BY fechaNacimiento DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<BebeRecienNacido>>

    @Query("SELECT * FROM bebes_recien_nacidos ORDER BY fechaNacimiento DESC")
    suspend fun obtenerTodosLista(): List<BebeRecienNacido>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(bebes: List<BebeRecienNacido>)

    @Query("DELETE FROM bebes_recien_nacidos WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM bebes_recien_nacidos")
    suspend fun eliminarTodos()
}
