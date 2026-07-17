package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccinationRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(record: VaccinationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(records: List<VaccinationRecord>)

    @Delete
    suspend fun eliminar(record: VaccinationRecord)

    @Query("SELECT * FROM vaccination_records WHERE patientId = :patientId ORDER BY appliedAt DESC, id DESC")
    fun observarPorPaciente(patientId: Int): Flow<List<VaccinationRecord>>

    @Query("SELECT * FROM vaccination_records WHERE id = :recordId LIMIT 1")
    suspend fun buscarPorId(recordId: Int): VaccinationRecord?

    @Query("SELECT * FROM vaccination_records WHERE alarmEnabled = 1 AND nextDoseAt IS NOT NULL AND nextDoseAt >= :now ORDER BY nextDoseAt ASC")
    suspend fun obtenerPendientesConAlarma(now: Long): List<VaccinationRecord>

    @Query("SELECT * FROM vaccination_records ORDER BY appliedAt DESC, id DESC")
    suspend fun obtenerTodosLista(): List<VaccinationRecord>

    @Query("DELETE FROM vaccination_records WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM vaccination_records")
    suspend fun eliminarTodos()
}