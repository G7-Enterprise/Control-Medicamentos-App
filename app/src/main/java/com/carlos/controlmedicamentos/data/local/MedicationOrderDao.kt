package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationOrderDao {
    @Insert
    suspend fun insertar(order: MedicationOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(orders: List<MedicationOrder>)

    @Update
    suspend fun actualizar(order: MedicationOrder)

    @Query("SELECT * FROM medication_orders WHERE patientId = :patientId ORDER BY createdAt DESC, id DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<MedicationOrder>>

    @Query("SELECT * FROM medication_orders ORDER BY createdAt DESC, id DESC")
    suspend fun obtenerTodosLista(): List<MedicationOrder>

    @Query("DELETE FROM medication_orders WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("DELETE FROM medication_orders WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM medication_orders")
    suspend fun eliminarTodos()
}