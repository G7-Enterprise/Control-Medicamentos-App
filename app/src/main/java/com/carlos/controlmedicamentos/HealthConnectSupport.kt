package com.carlos.controlmedicamentos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

enum class HealthConnectAvailability {
    NOT_SUPPORTED,
    NOT_INSTALLED,
    UPDATE_REQUIRED,
    AVAILABLE
}

data class ExerciseSessionSummary(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val title: String,
    val exerciseTypeLabel: String,
    val sourceLabel: String,
    val notes: String
)

data class DailyActivitySummary(
    val dayStartMillis: Long,
    val steps: Long,
    val distanceKm: Double,
    val caloriesKcal: Double,
    val caloriesLabel: String?
)

data class DerivedExerciseSummary(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val title: String,
    val distanceKm: Double,
    val caloriesKcal: Double,
    val steps: Long,
    val sourceLabel: String,
    val caloriesLabel: String?
)

data class HealthConnectPermissionStatus(
    val label: String,
    val permission: String,
    val granted: Boolean
)

data class HealthConnectRecordStatus(
    val label: String,
    val hasData: Boolean,
    val sourceLabel: String?,
    val errorMessage: String? = null
)

data class HealthConnectDiagnostics(
    val permissions: List<HealthConnectPermissionStatus>,
    val records: List<HealthConnectRecordStatus>
) {
    val allRequiredGranted: Boolean
        get() = permissions.all { it.granted }
}

@RequiresApi(Build.VERSION_CODES.O)
fun healthConnectAvailability(context: Context): HealthConnectAvailability {
    val status = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
    return when (status) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NOT_INSTALLED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun healthConnectExercisePermissions(): Set<String> {
    return healthConnectPermissionDefinitions().mapTo(linkedSetOf()) { it.second }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun hasHealthConnectExercisePermissions(context: Context): Boolean {
    val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
    return granted.containsAll(healthConnectExercisePermissions())
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun readHealthConnectDiagnostics(
    context: Context,
    daysBack: Long = 30
): HealthConnectDiagnostics {
    val client = HealthConnectClient.getOrCreate(context)
    val grantedPermissions = client.permissionController.getGrantedPermissions()
    val endTime = Instant.now()
    val startTime = endTime.minus(Duration.ofDays(daysBack))
    val permissions = healthConnectPermissionDefinitions().map { (label, permission) ->
        HealthConnectPermissionStatus(
            label = label,
            permission = permission,
            granted = grantedPermissions.contains(permission)
        )
    }

    return HealthConnectDiagnostics(
        permissions = permissions,
        records = listOf(
            buildRecordStatus(
                label = "Sesiones de ejercicio",
                grantedPermissions = grantedPermissions,
                permission = HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                client = client,
                recordType = ExerciseSessionRecord::class,
                startTime = startTime,
                endTime = endTime
            ),
            buildRecordStatus(
                label = "Registros de pasos",
                grantedPermissions = grantedPermissions,
                permission = HealthPermission.getReadPermission(StepsRecord::class),
                client = client,
                recordType = StepsRecord::class,
                startTime = startTime,
                endTime = endTime
            ),
            buildRecordStatus(
                label = "Registros de distancia",
                grantedPermissions = grantedPermissions,
                permission = HealthPermission.getReadPermission(DistanceRecord::class),
                client = client,
                recordType = DistanceRecord::class,
                startTime = startTime,
                endTime = endTime
            ),
            buildRecordStatus(
                label = "Registros de calorias activas",
                grantedPermissions = grantedPermissions,
                permission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                client = client,
                recordType = ActiveCaloriesBurnedRecord::class,
                startTime = startTime,
                endTime = endTime
            ),
            buildRecordStatus(
                label = "Registros de calorias totales",
                grantedPermissions = grantedPermissions,
                permission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                client = client,
                recordType = TotalCaloriesBurnedRecord::class,
                startTime = startTime,
                endTime = endTime
            )
        )
    )
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun readRecentExerciseSessions(
    context: Context,
    daysBack: Long = 30
): List<ExerciseSessionSummary> {
    val client = HealthConnectClient.getOrCreate(context)
    val endTime = Instant.now()
    val startTime = endTime.minus(Duration.ofDays(daysBack))
    val items = mutableListOf<ExerciseSessionSummary>()
    var pageToken: String? = null

    do {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                pageToken = pageToken
            )
        )

        response.records.forEach { record ->
            items += ExerciseSessionSummary(
                startTimeMillis = record.startTime.toEpochMilli(),
                endTimeMillis = record.endTime.toEpochMilli(),
                title = record.title?.takeIf { it.isNotBlank() } ?: exerciseTypeLabel(record.exerciseType),
                exerciseTypeLabel = exerciseTypeLabel(record.exerciseType),
                sourceLabel = sourcePackageLabel(record.metadata.dataOrigin.packageName),
                notes = record.notes.orEmpty().trim()
            )
        }
        pageToken = response.pageToken
    } while (pageToken != null)

    return items.sortedByDescending { it.startTimeMillis }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun readRecentDailyActivitySummaries(
    context: Context,
    daysBack: Long = 30
): List<DailyActivitySummary> {
    val client = HealthConnectClient.getOrCreate(context)
    val now = Instant.now()
    val zoneId = ZoneId.systemDefault()
    val today = now.atZone(zoneId).toLocalDate()
    val startDay = today.minusDays(daysBack.coerceAtLeast(1) - 1)
    val summaries = mutableListOf<DailyActivitySummary>()
    var day = today

    while (!day.isBefore(startDay)) {
        val dayStart = day.atStartOfDay(zoneId).toInstant()
        val dayEnd = day.plusDays(1).atStartOfDay(zoneId).toInstant().let { nextMidnight ->
            if (nextMidnight.isAfter(now)) now else nextMidnight
        }

        if (dayStart.isBefore(dayEnd)) {

            val aggregation = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
            )

            val steps = aggregation[StepsRecord.COUNT_TOTAL] ?: 0L
            val distanceKm = (aggregation[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000.0
            val activeCaloriesKcal = aggregation[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            val caloriesKcal = activeCaloriesKcal
            val caloriesLabel = if (activeCaloriesKcal > 0.0) "Calorias activas" else null

            if (steps > 0L || distanceKm > 0.0 || caloriesKcal > 0.0) {
                summaries += DailyActivitySummary(
                    dayStartMillis = dayStart.toEpochMilli(),
                    steps = steps,
                    distanceKm = distanceKm,
                    caloriesKcal = caloriesKcal,
                    caloriesLabel = caloriesLabel
                )
            }
        }

        day = day.minusDays(1)
    }

    return summaries.sortedByDescending { it.dayStartMillis }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun readDerivedExerciseSummaries(
    context: Context,
    daysBack: Long = 7
): List<DerivedExerciseSummary> {
    val client = HealthConnectClient.getOrCreate(context)
    val endTime = Instant.now()
    val startTime = endTime.minus(Duration.ofDays(daysBack))
    val distanceRecords = readAllRecords(client, DistanceRecord::class, startTime, endTime)
        .filter { it.startTime.isBefore(it.endTime) && it.distance.inMeters > 0.0 }
    val activeCalorieRecords = readAllRecords(client, ActiveCaloriesBurnedRecord::class, startTime, endTime)
        .filter { it.startTime.isBefore(it.endTime) && it.energy.inKilocalories > 0.0 }
        .map {
            CalorieInterval(
                startTime = it.startTime,
                endTime = it.endTime,
                caloriesKcal = it.energy.inKilocalories,
                sourcePackageName = it.metadata.dataOrigin.packageName
            )
        }
    val totalCalorieRecords = readAllRecords(client, TotalCaloriesBurnedRecord::class, startTime, endTime)
        .filter { it.startTime.isBefore(it.endTime) && it.energy.inKilocalories > 0.0 }
        .map {
            CalorieInterval(
                startTime = it.startTime,
                endTime = it.endTime,
                caloriesKcal = it.energy.inKilocalories,
                sourcePackageName = it.metadata.dataOrigin.packageName
            )
        }
    val calorieRecords = if (activeCalorieRecords.isNotEmpty()) activeCalorieRecords else totalCalorieRecords
    val caloriesLabel = if (activeCalorieRecords.isNotEmpty()) "Calorias activas" else if (totalCalorieRecords.isNotEmpty()) "Calorias" else null

    if (calorieRecords.isEmpty()) {
        return emptyList()
    }

    return calorieRecords.map { calorieRecord ->
        val overlappingDistanceKm = distanceRecords
            .filter { distanceRecord -> overlaps(calorieRecord.startTime, calorieRecord.endTime, distanceRecord.startTime, distanceRecord.endTime) }
            .sumOf { it.distance.inMeters } / 1000.0
        val steps = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(calorieRecord.startTime, calorieRecord.endTime)
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0L

        DerivedExerciseSummary(
            startTimeMillis = calorieRecord.startTime.toEpochMilli(),
            endTimeMillis = calorieRecord.endTime.toEpochMilli(),
            title = deriveExerciseTitle(overlappingDistanceKm, steps),
            distanceKm = overlappingDistanceKm,
            caloriesKcal = calorieRecord.caloriesKcal,
            steps = steps,
            sourceLabel = sourcePackageLabel(calorieRecord.sourcePackageName),
            caloriesLabel = caloriesLabel
        )
    }
        .filter { it.endTimeMillis > it.startTimeMillis && (it.caloriesKcal > 0.0 || it.distanceKm > 0.0 || it.steps > 0L) }
        .sortedByDescending { it.startTimeMillis }
}

fun healthConnectInstallIntent(context: Context): Intent {
    val uri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
    return Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.android.vending")
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun healthConnectManageDataIntent(context: Context): Intent {
    return HealthConnectClient.getHealthConnectManageDataIntent(context, HEALTH_CONNECT_PACKAGE)
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun <T : Record> buildRecordStatus(
    label: String,
    grantedPermissions: Set<String>,
    permission: String,
    client: HealthConnectClient,
    recordType: KClass<T>,
    startTime: Instant,
    endTime: Instant
): HealthConnectRecordStatus {
    if (!grantedPermissions.contains(permission)) {
        return HealthConnectRecordStatus(label = label, hasData = false, sourceLabel = null)
    }

    return try {
        when (recordType) {
            ExerciseSessionRecord::class -> {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )
                val sourceLabel = response.records.firstOrNull()?.metadata?.dataOrigin?.packageName?.let(::sourcePackageLabel)
                HealthConnectRecordStatus(
                    label = label,
                    hasData = response.records.isNotEmpty(),
                    sourceLabel = sourceLabel
                )
            }

            StepsRecord::class -> buildAggregateStatus(
                label = label,
                aggregation = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )[StepsRecord.COUNT_TOTAL]?.toDouble() ?: 0.0
            )

            DistanceRecord::class -> buildAggregateStatus(
                label = label,
                aggregation = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            )

            ActiveCaloriesBurnedRecord::class -> buildAggregateStatus(
                label = label,
                aggregation = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            )

            TotalCaloriesBurnedRecord::class -> buildAggregateStatus(
                label = label,
                aggregation = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            )

            else -> HealthConnectRecordStatus(
                label = label,
                hasData = false,
                sourceLabel = null,
                errorMessage = "Tipo de registro no soportado en análisis."
            )
        }
    } catch (error: Exception) {
        HealthConnectRecordStatus(
            label = label,
            hasData = false,
            sourceLabel = null,
            errorMessage = error.message
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun healthConnectPermissionDefinitions(): List<Pair<String, String>> {
    return listOf(
        "Ejercicio" to HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        "Pasos" to HealthPermission.getReadPermission(StepsRecord::class),
        "Distancia" to HealthPermission.getReadPermission(DistanceRecord::class),
        "Calorias activas" to HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun <T : Record> readAllRecords(
    client: HealthConnectClient,
    recordType: KClass<T>,
    startTime: Instant,
    endTime: Instant
): List<T> {
    val items = mutableListOf<T>()
    var pageToken: String? = null

    do {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                pageToken = pageToken
            )
        )
        items += response.records
        pageToken = response.pageToken
    } while (pageToken != null)

    return items
}

private fun buildAggregateStatus(label: String, aggregation: Double): HealthConnectRecordStatus {
    return HealthConnectRecordStatus(
        label = label,
        hasData = aggregation > 0.0,
        sourceLabel = null
    )
}

private data class CalorieInterval(
    val startTime: Instant,
    val endTime: Instant,
    val caloriesKcal: Double,
    val sourcePackageName: String
)

private fun sourcePackageLabel(packageName: String): String {
    return when (packageName) {
        "com.sec.android.app.shealth" -> "Samsung Health"
        "com.google.android.apps.fitness" -> "Google Fit"
        "android" -> "Telefono"
        "" -> "Health Connect"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}

private fun exerciseTypeLabel(type: Int): String {
    return when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Caminata"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Correr"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Cinta de correr"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Bicicleta"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Bicicleta estatica"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Senderismo"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Fuerza"
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "Estiramientos"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Natacion en piscina"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Natacion en aguas abiertas"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "Otro ejercicio"
        else -> "Ejercicio"
    }
}

private fun overlaps(startA: Instant, endA: Instant, startB: Instant, endB: Instant): Boolean {
    return startA < endB && startB < endA
}

private fun deriveExerciseTitle(distanceKm: Double, steps: Long): String {
    return when {
        distanceKm >= 1.0 -> "Paseo"
        steps >= 500L -> "Paseo"
        distanceKm > 0.0 -> "Actividad detectada"
        steps > 0L -> "Actividad detectada"
        else -> "Ejercicio detectado"
    }
}