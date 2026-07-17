package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarritoPendienteDao {

    @Query("SELECT * FROM carrito_pendiente WHERE patientId = :patientId ORDER BY id ASC")
    fun observarPorPaciente(patientId: Int): Flow<List<CarritoPendienteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(item: CarritoPendienteItem): Long

    @Query("SELECT * FROM carrito_pendiente WHERE patientId = :patientId AND medicationId = :medicationId LIMIT 1")
    suspend fun buscarPorMedicamento(patientId: Int, medicationId: Int): CarritoPendienteItem?

    @Query("UPDATE carrito_pendiente SET unidadesSolicitadas = :unidades WHERE id = :id")
    suspend fun actualizarUnidades(id: Int, unidades: Int)

    @Query("DELETE FROM carrito_pendiente WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("DELETE FROM carrito_pendiente WHERE patientId = :patientId")
    suspend fun limpiarPorPaciente(patientId: Int)

    @Query("SELECT * FROM carrito_pendiente ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<CarritoPendienteItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(items: List<CarritoPendienteItem>)

    @Query("DELETE FROM carrito_pendiente")
    suspend fun eliminarTodos()
}
