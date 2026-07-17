package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhysicalActivityDao {

    @Insert
    suspend fun insertar(activity: PhysicalActivity): Long

    @Query("SELECT * FROM physical_activities WHERE patientId = :patientId ORDER BY fechaInicio DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<PhysicalActivity>>

    @Query("DELETE FROM physical_activities WHERE id = :id")
    suspend fun eliminar(id: Int)

    @Query("SELECT * FROM physical_activities ORDER BY fechaInicio DESC")
    suspend fun obtenerTodosLista(): List<PhysicalActivity>

    @Query("DELETE FROM physical_activities WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(activities: List<PhysicalActivity>)

    @Query("DELETE FROM physical_activities")
    suspend fun eliminarTodos()
}
