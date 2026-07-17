package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoMedicoDao {
    @Insert
    suspend fun insertar(doc: DocumentoMedico)

    @Delete
    suspend fun eliminar(doc: DocumentoMedico)

    @Query("SELECT * FROM documentos_medicos WHERE idEmbarazo = :embarazoId ORDER BY fecha DESC")
    fun observarPorEmbarazo(embarazoId: Int): Flow<List<DocumentoMedico>>

    @Query("SELECT * FROM documentos_medicos ORDER BY fecha DESC")
    suspend fun obtenerTodosLista(): List<DocumentoMedico>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(docs: List<DocumentoMedico>)

    @Query("DELETE FROM documentos_medicos")
    suspend fun eliminarTodos()
}
