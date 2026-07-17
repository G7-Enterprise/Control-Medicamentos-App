package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NinoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNino(nino: NinoEntity): Long

    @Query("SELECT * FROM ninos WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun getNinosByPatient(patientId: Int): Flow<List<NinoEntity>>

    @Query("SELECT * FROM ninos WHERE id = :id")
    suspend fun getNinoById(id: Long): NinoEntity?

    @Query("SELECT * FROM ninos WHERE embarazoId = :embarazoId")
    fun getNinosByEmbarazo(embarazoId: Int): Flow<List<NinoEntity>>

    @Update
    suspend fun updateNino(nino: NinoEntity)

    @Delete
    suspend fun deleteNino(nino: NinoEntity)

    @Query("SELECT * FROM ninos ORDER BY fechaRegistro DESC")
    suspend fun obtenerTodosLista(): List<NinoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(ninos: List<NinoEntity>)

    @Query("DELETE FROM ninos WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM ninos")
    suspend fun eliminarTodos()
}
