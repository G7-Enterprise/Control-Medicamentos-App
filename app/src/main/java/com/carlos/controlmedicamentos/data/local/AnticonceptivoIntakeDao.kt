package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnticonceptivoIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(intake: AnticonceptivoIntake)

    @Query("SELECT * FROM anticonceptivo_intakes WHERE metodoId = :metodoId ORDER BY scheduledAt DESC")
    fun observarPorMetodo(metodoId: Int): Flow<List<AnticonceptivoIntake>>

    @Query("SELECT * FROM anticonceptivo_intakes WHERE metodoId = :metodoId AND scheduledAt BETWEEN :inicio AND :fin")
    fun observarEnRango(metodoId: Int, inicio: Long, fin: Long): Flow<List<AnticonceptivoIntake>>

    @Query("DELETE FROM anticonceptivo_intakes WHERE metodoId = :metodoId AND scheduledAt = :scheduledAt")
    suspend fun eliminarPorMetodoYHorario(metodoId: Int, scheduledAt: Long)

    @Query("SELECT * FROM anticonceptivo_intakes WHERE metodoId = :metodoId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun obtenerPorMetodoYHorario(metodoId: Int, scheduledAt: Long): AnticonceptivoIntake?

    @Query("SELECT COUNT(*) FROM anticonceptivo_intakes WHERE metodoId = :metodoId")
    suspend fun contarPorMetodo(metodoId: Int): Int

    @Query("DELETE FROM anticonceptivo_intakes WHERE metodoId = :metodoId")
    suspend fun eliminarPorMetodoId(metodoId: Int)

    @Query("SELECT * FROM anticonceptivo_intakes ORDER BY scheduledAt DESC")
    suspend fun obtenerTodosLista(): List<AnticonceptivoIntake>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(intakes: List<AnticonceptivoIntake>)

    @Query("DELETE FROM anticonceptivo_intakes")
    suspend fun eliminarTodos()
}
