package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(intake: MedicationIntake)

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun buscarPorMedicamentoYHorario(medicationId: Int, scheduledAt: Long): MedicationIntake?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(intakes: List<MedicationIntake>)

    @Query("SELECT * FROM medication_intakes WHERE scheduledAt BETWEEN :start AND :end")
    fun observarEnRango(start: Long, end: Long): Flow<List<MedicationIntake>>

    @Query(
        "SELECT mi.* FROM medication_intakes mi " +
            "LEFT JOIN insumos m ON m.id = mi.medicationId " +
            "WHERE mi.patientId = :patientId " +
            "ORDER BY mi.acceptedAt DESC, mi.id DESC"
    )
    fun observarPorPaciente(patientId: Int): Flow<List<MedicationIntake>>

    @Query("SELECT * FROM medication_intakes ORDER BY scheduledAt ASC")
    suspend fun obtenerTodosLista(): List<MedicationIntake>

    @Query("SELECT * FROM medication_intakes WHERE scheduledAt BETWEEN :start AND :end ORDER BY scheduledAt ASC")
    suspend fun obtenerEnRango(start: Long, end: Long): List<MedicationIntake>

    @Query("DELETE FROM medication_intakes WHERE medicationId = :medicationId AND scheduledAt = :scheduledAt")
    suspend fun eliminarPorMedicamentoYHorario(medicationId: Int, scheduledAt: Long)

    @Query(
        "DELETE FROM medication_intakes WHERE medicationId IN (SELECT id FROM insumos WHERE patientId = :patientId)"
    )
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM medication_intakes")
    suspend fun eliminarTodos()
}