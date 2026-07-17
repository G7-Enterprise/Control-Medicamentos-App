package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlEmbarazoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(embarazo: ControlEmbarazo): Long

    @Update
    suspend fun actualizar(embarazo: ControlEmbarazo)

    @Delete
    suspend fun eliminar(embarazo: ControlEmbarazo)

    @Query("SELECT * FROM control_embarazo WHERE patientId = :patientId AND activo = 1 LIMIT 1")
    fun observarEmbarazoActivo(patientId: Int): Flow<ControlEmbarazo?>

    @Query("SELECT * FROM control_embarazo WHERE patientId = :patientId AND activo = 1 LIMIT 1")
    suspend fun obtenerEmbarazoActivo(patientId: Int): ControlEmbarazo?

    @Query("SELECT * FROM control_embarazo WHERE patientId = :patientId ORDER BY fechaRegistro DESC")
    fun observarTodos(patientId: Int): Flow<List<ControlEmbarazo>>

    @Query("UPDATE control_embarazo SET activo = 0, estadoEmbarazo = 'FINALIZADO', fechaParto = :fechaParto, tipoPartoRegistrado = :tipoParto, notasParto = :notas WHERE id = :id")
    suspend fun registrarParto(id: Int, fechaParto: Long, tipoParto: String, notas: String)

    @Query("SELECT * FROM control_embarazo WHERE patientId = :patientId AND estadoEmbarazo = 'INTERRUMPIDO' ORDER BY fechaFin DESC")
    fun observarInterrumpidos(patientId: Int): Flow<List<ControlEmbarazo>>

    @Query("UPDATE control_embarazo SET fechaFin = :fechaFin, tipoInterrupcion = :tipo, metodoInterrupcion = :metodo, notasInterrupcion = :notas WHERE id = :id")
    suspend fun editarInterrupcion(id: Int, fechaFin: Long, tipo: String?, metodo: String?, notas: String?)

    @Query("DELETE FROM control_embarazo WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("SELECT * FROM control_embarazo ORDER BY fechaRegistro DESC")
    suspend fun obtenerTodosLista(): List<ControlEmbarazo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(embarazos: List<ControlEmbarazo>)

    @Query("DELETE FROM control_embarazo WHERE patientId = :patientId")
    suspend fun eliminarPorPaciente(patientId: Int)

    @Query("DELETE FROM control_embarazo")
    suspend fun eliminarTodos()
}
