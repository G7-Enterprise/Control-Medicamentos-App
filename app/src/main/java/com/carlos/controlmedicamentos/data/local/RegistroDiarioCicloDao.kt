package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDiarioCicloDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(registro: RegistroDiarioCiclo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarVarios(registros: List<RegistroDiarioCiclo>): List<Long>

    @Update
    suspend fun actualizar(registro: RegistroDiarioCiclo)

    @Delete
    suspend fun eliminar(registro: RegistroDiarioCiclo)

    @Query("SELECT * FROM registros_diarios_ciclo WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): RegistroDiarioCiclo?

    @Query("SELECT * FROM registros_diarios_ciclo WHERE cicloId = :cicloId ORDER BY fecha DESC, id DESC")
    fun observarPorCiclo(cicloId: Int): Flow<List<RegistroDiarioCiclo>>

    @Query("SELECT * FROM registros_diarios_ciclo WHERE cicloId = :cicloId ORDER BY fecha DESC, id DESC")
    suspend fun obtenerPorCiclo(cicloId: Int): List<RegistroDiarioCiclo>

    @Query("SELECT * FROM registros_diarios_ciclo WHERE cicloId = :cicloId AND fecha = :fecha")
    suspend fun obtenerPorCicloYFecha(cicloId: Int, fecha: Long): List<RegistroDiarioCiclo>

    @Query("SELECT * FROM registros_diarios_ciclo WHERE cicloId = :cicloId AND tipoSintoma = :tipoSintoma ORDER BY fecha DESC")
    fun observarPorCicloYTipo(cicloId: Int, tipoSintoma: String): Flow<List<RegistroDiarioCiclo>>

    @Query("SELECT DISTINCT tipoSintoma FROM registros_diarios_ciclo WHERE cicloId = :cicloId")
    suspend fun obtenerTiposSintomasPorCiclo(cicloId: Int): List<String>

    @Query("DELETE FROM registros_diarios_ciclo WHERE cicloId = :cicloId")
    suspend fun eliminarPorCiclo(cicloId: Int)

    @Query("DELETE FROM registros_diarios_ciclo WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("DELETE FROM registros_diarios_ciclo")
    suspend fun eliminarTodos()

    @Query("""
        SELECT * FROM registros_diarios_ciclo 
        WHERE cicloId IN (SELECT id FROM ciclos_menstruales WHERE patientId = :patientId)
        ORDER BY fecha DESC
    """)
    fun observarPorPaciente(patientId: Int): Flow<List<RegistroDiarioCiclo>>

    @Query("""
        SELECT COUNT(*) FROM registros_diarios_ciclo 
        WHERE cicloId = :cicloId AND tipoSintoma = :tipoSintoma
    """)
    suspend fun contarPorCicloYTipo(cicloId: Int, tipoSintoma: Int): Int

    @Query("SELECT * FROM registros_diarios_ciclo ORDER BY fecha DESC")
    suspend fun obtenerTodosLista(): List<RegistroDiarioCiclo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(registros: List<RegistroDiarioCiclo>)
}
