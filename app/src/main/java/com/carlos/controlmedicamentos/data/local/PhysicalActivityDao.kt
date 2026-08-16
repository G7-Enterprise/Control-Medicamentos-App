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

    @Query("SELECT * FROM physical_activities WHERE patientId = :patientId AND origen = 'BACKGROUND_DETECTED' AND tipoEvento IN ('INACTIVITY', 'MOVEMENT') ORDER BY fechaInicio DESC LIMIT 100")
    fun observarHistorialSedentarismo(patientId: Int): Flow<List<PhysicalActivity>>

    @Query("SELECT DISTINCT strftime('%Y-%m', fechaInicio / 1000, 'unixepoch', 'localtime') FROM physical_activities WHERE patientId = :patientId AND origen = 'BACKGROUND_DETECTED' AND tipoEvento IN ('INACTIVITY', 'MOVEMENT') ORDER BY 1 DESC")
    fun observarMesesSedentarismo(patientId: Int): Flow<List<String>>

    @Query("SELECT * FROM physical_activities WHERE patientId = :patientId AND origen = 'BACKGROUND_DETECTED' AND tipoEvento IN ('INACTIVITY', 'MOVEMENT') AND fechaInicio BETWEEN :desde AND :hasta ORDER BY fechaInicio DESC")
    fun observarSedentarismoEnRango(patientId: Int, desde: Long, hasta: Long): Flow<List<PhysicalActivity>>

    @Query("SELECT COUNT(*) FROM physical_activities WHERE patientId = :patientId AND origen = 'BACKGROUND_DETECTED' AND tipoEvento = 'INACTIVITY' AND fechaInicio >= :desde")
    fun contarInactividadHoy(patientId: Int, desde: Long): Flow<Int>

    @Query("DELETE FROM physical_activities WHERE origen = 'BACKGROUND_DETECTED'")
    suspend fun eliminarSedentarismo()

    @Query("SELECT * FROM physical_activities WHERE patientId = :patientId AND origen = 'BACKGROUND_DETECTED' AND tipoEvento IN ('INACTIVITY', 'MOVEMENT') AND fechaInicio BETWEEN :desde AND :hasta ORDER BY fechaInicio DESC")
    suspend fun obtenerSedentarismoEnRango(patientId: Int, desde: Long, hasta: Long): List<PhysicalActivity>

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
