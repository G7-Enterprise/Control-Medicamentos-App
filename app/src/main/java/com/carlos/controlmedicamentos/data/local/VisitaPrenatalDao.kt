package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitaPrenatalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(visita: VisitaPrenatal): Long

    @Update
    suspend fun actualizar(visita: VisitaPrenatal)

    @Delete
    suspend fun eliminar(visita: VisitaPrenatal)

    @Query("SELECT * FROM visita_prenatal WHERE embarazoId = :embarazoId ORDER BY fecha ASC")
    fun observarPorEmbarazo(embarazoId: Int): Flow<List<VisitaPrenatal>>

    @Query("SELECT COUNT(*) FROM visita_prenatal WHERE embarazoId = :embarazoId")
    suspend fun contarVisitas(embarazoId: Int): Int

    @Query("SELECT * FROM visita_prenatal ORDER BY fecha DESC")
    suspend fun obtenerTodosLista(): List<VisitaPrenatal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(visitas: List<VisitaPrenatal>)

    @Query("DELETE FROM visita_prenatal")
    suspend fun eliminarTodos()
}
