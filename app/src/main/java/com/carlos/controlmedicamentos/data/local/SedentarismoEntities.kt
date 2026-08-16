package com.carlos.controlmedicamentos.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    suspend fun guardarConfig(config: ConfigSedentarismo)

    @Query("SELECT * FROM config_sedentarismo WHERE patientId = :patientId LIMIT 1")
    suspend fun obtenerConfig(patientId: Int): ConfigSedentarismo?

    @Query("SELECT * FROM config_sedentarismo WHERE patientId = :patientId LIMIT 1")
    fun observarConfig(patientId: Int): Flow<ConfigSedentarismo?>

    @Query("SELECT * FROM config_sedentarismo")
    suspend fun obtenerTodosConfig(): List<ConfigSedentarismo>

    @Query("DELETE FROM config_sedentarismo")
    suspend fun eliminarTodaConfig()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodosConfig(lista: List<ConfigSedentarismo>)
}
