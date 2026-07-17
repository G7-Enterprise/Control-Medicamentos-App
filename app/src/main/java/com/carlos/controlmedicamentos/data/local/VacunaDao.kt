package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VacunaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacunas(vacunas: List<VacunaEntity>)

    @Query("SELECT * FROM vacunas WHERE ninoId = :ninoId ORDER BY id ASC")
    fun getVacunasByNino(ninoId: Long): Flow<List<VacunaEntity>>

    @Update
    suspend fun updateVacuna(vacuna: VacunaEntity)

    @Query("SELECT * FROM vacunas ORDER BY id ASC")
    suspend fun obtenerTodosLista(): List<VacunaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(vacunas: List<VacunaEntity>)

    @Query("DELETE FROM vacunas WHERE ninoId = :ninoId")
    suspend fun deleteAllByNino(ninoId: Long)

    @Query("DELETE FROM vacunas")
    suspend fun eliminarTodos()
}
