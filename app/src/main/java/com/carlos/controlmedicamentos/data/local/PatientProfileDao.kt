package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(profile: PatientProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(profiles: List<PatientProfile>)

    @Update
    suspend fun actualizar(profile: PatientProfile)

    @Query("SELECT * FROM patient_profile ORDER BY isActive DESC, apellidos ASC, nombre ASC")
    fun observarTodos(): Flow<List<PatientProfile>>

    @Query("SELECT * FROM patient_profile WHERE isActive = 1 LIMIT 1")
    fun observarPerfilActivo(): Flow<PatientProfile?>

    @Query("UPDATE patient_profile SET isActive = 0")
    suspend fun desactivarTodos()

    @Query("UPDATE patient_profile SET isActive = 1 WHERE id = :patientId")
    suspend fun activarPaciente(patientId: Int)

    @Query("SELECT * FROM patient_profile WHERE id = :patientId LIMIT 1")
    suspend fun buscarPorId(patientId: Int): PatientProfile?

    @Query("SELECT * FROM patient_profile WHERE id = :patientId LIMIT 1")
    fun observeById(patientId: Int): Flow<PatientProfile?>

    @Query("SELECT * FROM patient_profile ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<PatientProfile>

    @Query("DELETE FROM patient_profile WHERE id = :patientId")
    suspend fun eliminarPorId(patientId: Int)

    @Query("DELETE FROM patient_profile")
    suspend fun eliminarTodos()
}