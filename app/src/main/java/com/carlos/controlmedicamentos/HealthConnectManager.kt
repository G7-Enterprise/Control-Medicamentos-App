package com.carlos.controlmedicamentos

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId

data class HealthConnectVitalSigns(
    val sistolica: Int? = null,
    val diastolica: Int? = null,
    val latidos: Int? = null,
    val spo2: Int? = null
)

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )

    fun isAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            healthConnectAvailability(appContext) == HealthConnectAvailability.AVAILABLE
    }

    suspend fun hasRequiredPermissions(): Boolean {
        if (!isAvailable()) return false
        return HealthConnectClient.getOrCreate(appContext)
            .permissionController
            .getGrantedPermissions()
            .containsAll(requiredPermissions)
    }

    suspend fun readLatestToday(): HealthConnectVitalSigns {
        if (!isAvailable() || !hasRequiredPermissions()) return HealthConnectVitalSigns()

        val client = HealthConnectClient.getOrCreate(appContext)
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, now)

        val heartRate = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = timeRange
            )
        ).records
            .flatMap { it.samples }
            .maxByOrNull { it.time }
            ?.beatsPerMinute

        val bloodPressure = client.readRecords(
            ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = timeRange
            )
        ).records
            .maxByOrNull { it.time }

        val oxygenSaturation = client.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = timeRange
            )
        ).records
            .maxByOrNull { it.time }
            ?.percentage
            ?.value

        return HealthConnectVitalSigns(
            sistolica = bloodPressure?.systolic?.inMillimetersOfMercury?.toInt(),
            diastolica = bloodPressure?.diastolic?.inMillimetersOfMercury?.toInt(),
            latidos = heartRate?.toInt(),
            spo2 = oxygenSaturation?.toInt()
        )
    }
}
