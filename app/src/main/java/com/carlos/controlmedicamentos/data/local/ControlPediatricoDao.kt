package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPediatricoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControl(control: ControlPediatricoEntity)

    @Query("SELECT * FROM controles_pediatricos WHERE ninoId = :ninoId ORDER BY fechaControl DESC")
    fun getControlesByNino(ninoId: Long): Flow<List<ControlPediatricoEntity>>

    @Query("SELECT * FROM controles_pediatricos ORDER BY fechaControl DESC")
    suspend fun obtenerTodosLista(): List<ControlPediatricoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(controles: List<ControlPediatricoEntity>)

    @Query("DELETE FROM controles_pediatricos WHERE ninoId = :ninoId")
    suspend fun deleteAllByNino(ninoId: Long)

    @Query("DELETE FROM controles_pediatricos")
    suspend fun eliminarTodos()
}
