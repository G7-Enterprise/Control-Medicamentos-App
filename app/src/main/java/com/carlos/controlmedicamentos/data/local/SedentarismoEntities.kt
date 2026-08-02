package com.carlos.controlmedicamentos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "registros_sedentarismo",
    indices = [Index("patientId"), Index("timestamp")]
)
data class RegistroSedentarismo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val tipoEvento: String = "MOVIMIENTO",  // MOVIMIENTO | ALERTA_INACTIVIDAD | INICIO_SESION | FIN_SESION
    val minutosInactivo: Int = 0,
    val notas: String = ""
)

@Entity(
    tableName = "config_sedentarismo",
    indices = [Index("patientId", unique = true)]
)
data class ConfigSedentarismo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val activado: Boolean = false,
    val limiteInactividadMinutos: Int = 60,
    val horaInicioMonitoreo: Int = 7,
    val horaFinMonitoreo: Int = 22,
    val diasActivos: String = "1,2,3,4,5"   // días semana separados por coma (1=lunes)
)

@Dao
interface SedentarismoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistro(r: RegistroSedentarismo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarConfig(config: ConfigSedentarismo)

    @Query("SELECT * FROM config_sedentarismo WHERE patientId = :patientId LIMIT 1")
    suspend fun obtenerConfig(patientId: Int): ConfigSedentarismo?

    @Query("SELECT * FROM config_sedentarismo WHERE patientId = :patientId LIMIT 1")
    fun observarConfig(patientId: Int): Flow<ConfigSedentarismo?>

    @Query("SELECT * FROM registros_sedentarismo WHERE patientId = :patientId AND timestamp >= :desde ORDER BY timestamp DESC")
    fun observarDesde(patientId: Int, desde: Long): Flow<List<RegistroSedentarismo>>

    @Query("SELECT * FROM registros_sedentarismo WHERE patientId = :patientId ORDER BY timestamp DESC LIMIT 100")
    fun observarHistorial(patientId: Int): Flow<List<RegistroSedentarismo>>

    @Query("SELECT DISTINCT strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') FROM registros_sedentarismo WHERE patientId = :patientId ORDER BY 1 DESC")
    fun observarMesesConHistorial(patientId: Int): Flow<List<String>>

    @Query("SELECT * FROM registros_sedentarismo WHERE patientId = :patientId AND timestamp BETWEEN :desde AND :hasta ORDER BY timestamp DESC")
    fun observarEnRango(patientId: Int, desde: Long, hasta: Long): Flow<List<RegistroSedentarismo>>

    @Query("SELECT * FROM registros_sedentarismo WHERE patientId = :patientId AND timestamp BETWEEN :desde AND :hasta ORDER BY timestamp DESC")
    suspend fun obtenerEnRango(patientId: Int, desde: Long, hasta: Long): List<RegistroSedentarismo>

    @Query("SELECT COUNT(*) FROM registros_sedentarismo WHERE patientId = :patientId AND tipoEvento = 'ALERTA_INACTIVIDAD' AND timestamp >= :desde")
    fun contarAlertasHoy(patientId: Int, desde: Long): Flow<Int>

    @Query("DELETE FROM registros_sedentarismo WHERE id = :id")
    suspend fun eliminarRegistro(id: Int)

    @Query("DELETE FROM registros_sedentarismo WHERE patientId = :patientId AND timestamp < :hasta")
    suspend fun limpiarAntiguos(patientId: Int, hasta: Long)

    @Query("DELETE FROM registros_sedentarismo WHERE patientId = :patientId")
    suspend fun eliminarTodos(patientId: Int)

    @Query("DELETE FROM registros_sedentarismo")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM registros_sedentarismo")
    suspend fun obtenerTodosLista(): List<RegistroSedentarismo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(lista: List<RegistroSedentarismo>)

    @Query("SELECT * FROM config_sedentarismo")
    suspend fun obtenerTodosConfig(): List<ConfigSedentarismo>

    @Query("DELETE FROM config_sedentarismo")
    suspend fun eliminarTodaConfig()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodosConfig(lista: List<ConfigSedentarismo>)
}
