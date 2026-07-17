package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalPractitionerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(practitioner: MedicalPractitioner): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(practitioners: List<MedicalPractitioner>)

    @Delete
    suspend fun eliminar(practitioner: MedicalPractitioner)

    @Query("SELECT * FROM medical_practitioners WHERE patientId = :patientId ORDER BY name COLLATE NOCASE ASC, specialty COLLATE NOCASE ASC, id ASC")
    fun observarPorPaciente(patientId: Int): Flow<List<MedicalPractitioner>>

    @Query("SELECT * FROM medical_practitioners ORDER BY name COLLATE NOCASE ASC, specialty COLLATE NOCASE ASC, id ASC")
    fun observarTodos(): Flow<List<MedicalPractitioner>>

    @Query("SELECT * FROM medical_practitioners ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<MedicalPractitioner>

    @Query("DELETE FROM medical_practitioners WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM medical_practitioners")
    suspend fun eliminarTodos()
}