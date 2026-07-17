package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetodoAnticonceptivoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(metodo: MetodoAnticonceptivo): Long

    @Update
    suspend fun actualizar(metodo: MetodoAnticonceptivo)

    @Delete
    suspend fun eliminar(metodo: MetodoAnticonceptivo)

    @Query("SELECT * FROM metodos_anticonceptivos WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<MetodoAnticonceptivo>>

    @Query("SELECT * FROM metodos_anticonceptivos WHERE patientId = :patientId AND activo = 1 LIMIT 1")
    fun observarActivo(patientId: Int): Flow<MetodoAnticonceptivo?>

    @Query("SELECT * FROM metodos_anticonceptivos WHERE patientId = :patientId AND activo = 1")
    suspend fun obtenerActivos(patientId: Int): List<MetodoAnticonceptivo>

    @Query("SELECT * FROM metodos_anticonceptivos WHERE activo = 1")
    suspend fun obtenerActivos(): List<MetodoAnticonceptivo>

    @Query("UPDATE metodos_anticonceptivos SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Int)

    @Query("SELECT * FROM metodos_anticonceptivos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): MetodoAnticonceptivo?

    @Query("SELECT * FROM metodos_anticonceptivos ORDER BY fechaRegistro DESC")
    suspend fun obtenerTodosLista(): List<MetodoAnticonceptivo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(metodos: List<MetodoAnticonceptivo>)

    @Query("DELETE FROM metodos_anticonceptivos WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM metodos_anticonceptivos")
    suspend fun eliminarTodos()
}
