package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EnfermedadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnfermedad(enfermedad: EnfermedadEntity)

    @Query("SELECT * FROM enfermedades_alergias WHERE ninoId = :ninoId ORDER BY fechaInicio DESC")
    fun getEnfermedadesByNino(ninoId: Long): Flow<List<EnfermedadEntity>>

    @Update
    suspend fun updateEnfermedad(enfermedad: EnfermedadEntity)

    @Query("SELECT * FROM enfermedades_alergias ORDER BY fechaInicio DESC")
    suspend fun obtenerTodosLista(): List<EnfermedadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(enfermedades: List<EnfermedadEntity>)

    @Query("DELETE FROM enfermedades_alergias WHERE ninoId = :ninoId")
    suspend fun deleteAllByNino(ninoId: Long)

    @Query("DELETE FROM enfermedades_alergias")
    suspend fun eliminarTodos()
}
