package com.carlos.controlmedicamentos.notifications

import android.content.Context
import android.util.Log
import com.carlos.controlmedicamentos.CountryCurrencyCatalog
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_NOT_TAKEN
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_TAKEN
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MedicationIntake
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.data.local.unidadesPorToma

/**
 * Ejecutor de acciones disparadas desde notificaciones o desde la pantalla de
 * alarma a pantalla completa. Centraliza el registro de tomas, posponer alarmas
 * y cancelar recordatorios pendientes, para que pueda invocarse tanto desde
 * [AlarmReceiver] como directamente desde una Activity sin depender de que un
 * broadcast llegue correctamente justo antes de que la Activity finalice.
 */
internal object AlarmActionExecutor {

    internal data class ReminderToken(
        val medicationId: Int,
        val slotIndex: Int,
        val scheduledAt: Long
    )

    fun tokenFor(medicationId: Int, slotIndex: Int, scheduledAt: Long): String {
        return "$medicationId:$slotIndex:$scheduledAt"
    }

    fun parseReminderToken(token: String): ReminderToken? {
        val parts = token.split(":")
        val medicationId = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val slotIndex = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val scheduledAt = parts.getOrNull(2)?.toLongOrNull() ?: return null
        return ReminderToken(medicationId, slotIndex, scheduledAt)
    }

    suspend fun cancelPendingReminders(context: Context, reminderTokens: List<String>) {
        val db = AppDatabase.getDatabase(context)
        val scheduler = MedicationScheduler(context)
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            db.medicationDao().findById(token.medicationId) ?: return@forEach
            scheduler.cancelarRecordatoriosPendientes(token.medicationId, token.slotIndex)
        }
    }

    suspend fun registerAcceptedTakes(
        context: Context,
        reminderTokens: List<String>,
        useScheduledTime: Boolean = false
    ) {
        if (reminderTokens.isEmpty()) {
            Log.w("AlarmActionExecutor", "No reminder tokens to register accepted takes")
            return
        }
        val db = AppDatabase.getDatabase(context)
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            try {
                val existing = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                    token.medicationId,
                    token.scheduledAt
                )
                if (existing != null) {
                    return@forEach
                }

                val medication = db.medicationDao().findById(token.medicationId)
                    ?: run {
                        Log.w(
                            "AlarmActionExecutor",
                            "Medication ${token.medicationId} not found for token $token"
                        )
                        return@forEach
                    }

                val unitsPerTake = medication.unidadesPorToma()
                db.medicationIntakeDao().guardar(
                    MedicationIntake(
                        medicationId = token.medicationId,
                        patientId = medication.patientId,
                        scheduledAt = token.scheduledAt,
                        acceptedAt = if (useScheduledTime) token.scheduledAt else System.currentTimeMillis(),
                        medicationName = medication.nombre,
                        dosis = unitsPerTake.toString(),
                        status = MEDICATION_INTAKE_STATUS_TAKEN
                    )
                )
                Log.d(
                    "AlarmActionExecutor",
                    "Registered taken intake for medId=${token.medicationId} scheduledAt=${token.scheduledAt}"
                )

                val currentStock = medication.stockActual ?: return@forEach
                val updatedStock = currentStock - unitsPerTake
                db.medicationDao().actualizarStock(medication.id, updatedStock)

                val lowStockThreshold = medication.stockMinimo?.coerceAtLeast(unitsPerTake) ?: unitsPerTake

                if (currentStock > lowStockThreshold && updatedStock <= lowStockThreshold) {
                    val carritoMedicationIds = db.carritoPendienteDao().obtenerTodosLista()
                        .map { it.medicationId }.toSet()
                    val groupedLowStockItems = buildList {
                        db.medicationDao().obtenerTodosLista()
                            .filter { buildRestockGroupKey(it) == buildRestockGroupKey(medication) }
                            .forEach { candidate ->
                                toStockOrderItem(db, candidate)?.let(::add)
                            }
                    }.filter { it.medicationId !in carritoMedicationIds }
                        .sortedBy { it.medicationName.lowercase() }
                        .toList()

                    if (groupedLowStockItems.isEmpty()) return@forEach

                    NotificacionHelper.mostrarStockBajoAgrupado(
                        context = context,
                        notificationId = buildStockNotificationId(medication),
                        items = groupedLowStockItems.ifEmpty {
                            listOf(
                                NotificacionHelper.StockOrderItem(
                                    medicationId = medication.id,
                                    medicationName = medication.nombre,
                                    concentration = medication.concentracion,
                                    remainingUnits = updatedStock,
                                    lowStockThreshold = lowStockThreshold,
                                    unitsPerTake = unitsPerTake,
                                    unitPrice = medication.precioPorUnidad,
                                    currencySymbol = currencySymbolFor(db, medication.patientId)
                                )
                            )
                        },
                        whatsappPhone = medication.telefonoPedidoWhatsapp,
                        restockSource = medication.origenReposicion
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "AlarmActionExecutor",
                    "Error registering accepted take for token $token",
                    e
                )
            }
        }
    }

    suspend fun registerNotTakenTakes(context: Context, reminderTokens: List<String>) {
        if (reminderTokens.isEmpty()) {
            Log.w("AlarmActionExecutor", "No reminder tokens to register not-taken takes")
            return
        }
        val db = AppDatabase.getDatabase(context)
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            try {
                val existing = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                    token.medicationId,
                    token.scheduledAt
                )
                if (existing != null) {
                    return@forEach
                }

                val medication = db.medicationDao().findById(token.medicationId)
                    ?: run {
                        Log.w(
                            "AlarmActionExecutor",
                            "Medication ${token.medicationId} not found for token $token"
                        )
                        return@forEach
                    }

                db.medicationIntakeDao().guardar(
                    MedicationIntake(
                        medicationId = token.medicationId,
                        patientId = medication.patientId,
                        scheduledAt = token.scheduledAt,
                        acceptedAt = token.scheduledAt,
                        medicationName = medication.nombre,
                        dosis = medication.dosis,
                        status = MEDICATION_INTAKE_STATUS_NOT_TAKEN
                    )
                )
                Log.d(
                    "AlarmActionExecutor",
                    "Registered not-taken intake for medId=${token.medicationId} scheduledAt=${token.scheduledAt}"
                )
            } catch (e: Exception) {
                Log.e(
                    "AlarmActionExecutor",
                    "Error registering not-taken take for token $token",
                    e
                )
            }
        }
    }

    suspend fun postponeReminders(
        context: Context,
        reminderTokens: List<String>,
        fallbackMedId: Int,
        fallbackSlotIndex: Int,
        fallbackScheduledAt: Long,
        customDelayMinutes: Int = 0
    ) {
        val scheduler = MedicationScheduler(context)
        val db = AppDatabase.getDatabase(context)
        val tokens = reminderTokens.ifEmpty {
            listOf(tokenFor(fallbackMedId, fallbackSlotIndex, fallbackScheduledAt))
        }
        tokens.forEach { token ->
            val parsedToken = parseReminderToken(token) ?: return@forEach
            val medicationId = parsedToken.medicationId
            val slotIndex = parsedToken.slotIndex
            val medication = db.medicationDao().findById(medicationId) ?: return@forEach
            if (medication.estaActivo && medication.alarmaActiva) {
                scheduler.cancelarRecordatoriosPendientes(medicationId, slotIndex)
                val delayMinutes = if (customDelayMinutes > 0) {
                    customDelayMinutes
                } else {
                    CriticalAlertSettings.normalizeRetryInterval(medication.retryIntervalMinutes)
                }
                scheduler.programarRecordatorioPospuesto(
                    medication = medication,
                    slotIndex = slotIndex,
                    scheduledAtMillis = parsedToken.scheduledAt,
                    delayMinutes = delayMinutes
                )
            }
        }
    }

    private fun buildStockNotificationId(medication: Medication): Int =
        buildRestockGroupKey(medication).hashCode()

    private fun buildRestockGroupKey(medication: Medication): String {
        val phoneKey = medication.telefonoPedidoWhatsapp.filter(Char::isDigit)
        return when (medication.origenReposicion) {
            RestockSource.WHATSAPP_NUMBER -> "${medication.origenReposicion}|$phoneKey"
            else -> medication.origenReposicion
        }
    }

    private suspend fun toStockOrderItem(
        db: AppDatabase,
        medication: Medication
    ): NotificacionHelper.StockOrderItem? {
        if (!medication.estaActivo || medication.origenReposicion == RestockSource.INSS) {
            return null
        }

        val stock = medication.stockActual ?: return null
        val unitsPerTake = medication.dosis.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val threshold = medication.stockMinimo?.coerceAtLeast(unitsPerTake) ?: unitsPerTake
        if (stock > threshold) {
            return null
        }

        return NotificacionHelper.StockOrderItem(
            medicationId = medication.id,
            medicationName = medication.nombre,
            concentration = medication.concentracion,
            remainingUnits = stock,
            lowStockThreshold = threshold,
            unitsPerTake = unitsPerTake,
            unitPrice = medication.precioPorUnidad,
            currencySymbol = currencySymbolFor(db, medication.patientId)
        )
    }

    private suspend fun currencySymbolFor(db: AppDatabase, patientId: Int): String {
        val patient = db.patientProfileDao().buscarPorId(patientId)
        return CountryCurrencyCatalog.symbolFor(patient?.pais.orEmpty(), patient?.moneda.orEmpty())
    }
}
