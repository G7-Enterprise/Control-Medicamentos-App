package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FallAlertDao {
    @Insert
    suspend fun insert(alert: FallAlert): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(alerts: List<FallAlert>)

    @Update
    suspend fun update(alert: FallAlert)

    @Query("SELECT * FROM fall_alerts WHERE patientId = :patientId ORDER BY detectedAt DESC")
    fun observeByPatient(patientId: Int): Flow<List<FallAlert>>

    @Query("SELECT * FROM fall_alerts ORDER BY detectedAt DESC LIMIT 100")
    fun observeAll(): Flow<List<FallAlert>>

    @Query("SELECT * FROM fall_alerts ORDER BY detectedAt DESC")
    suspend fun obtenerTodosLista(): List<FallAlert>

    @Query("DELETE FROM fall_alerts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM fall_alerts")
    suspend fun eliminarTodos()
}
