package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(report: MedicalReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(reports: List<MedicalReport>)

    @Update
    suspend fun actualizar(report: MedicalReport)

    @Delete
    suspend fun eliminar(report: MedicalReport)

    @Query("SELECT * FROM medical_reports WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<MedicalReport>>

    @Query("SELECT * FROM medical_reports ORDER BY createdAt DESC, id DESC")
    fun observarTodos(): Flow<List<MedicalReport>>

    @Query("SELECT * FROM medical_reports ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<MedicalReport>

    @Query("DELETE FROM medical_reports WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM medical_reports")
    suspend fun eliminarTodos()
}