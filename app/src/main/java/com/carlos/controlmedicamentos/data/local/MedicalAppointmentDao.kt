package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalAppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(appointment: MedicalAppointment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(appointments: List<MedicalAppointment>)

    @Update
    suspend fun actualizar(appointment: MedicalAppointment)

    @Delete
    suspend fun eliminar(appointment: MedicalAppointment)

    @Query("SELECT * FROM medical_appointments WHERE patientId = :patientId ORDER BY scheduledAt ASC, id ASC")
    fun observarPorPaciente(patientId: Int): Flow<List<MedicalAppointment>>

    @Query("SELECT * FROM medical_appointments ORDER BY scheduledAt DESC, id DESC")
    fun observarTodos(): Flow<List<MedicalAppointment>>

    @Query("SELECT * FROM medical_appointments WHERE id = :appointmentId LIMIT 1")
    suspend fun buscarPorId(appointmentId: Int): MedicalAppointment?

    @Query("SELECT * FROM medical_appointments WHERE alarmEnabled = 1 AND isCompleted = 0 AND scheduledAt >= :now ORDER BY scheduledAt ASC")
    suspend fun obtenerPendientesConAlarma(now: Long): List<MedicalAppointment>

    @Query("SELECT * FROM medical_appointments ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<MedicalAppointment>

    @Query("DELETE FROM medical_appointments WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM medical_appointments")
    suspend fun eliminarTodos()
}