package com.carlos.controlmedicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.carlos.controlmedicamentos.data.local.Medication
import java.util.Calendar

class MedicationScheduler(private val context: Context) {

    companion object {
        const val EXTRA_MED_ID = "MED_ID"
        const val EXTRA_MED_NOMBRE = "MED_NOMBRE"
        const val EXTRA_SLOT_INDEX = "MED_SLOT_INDEX"
        const val EXTRA_SCHEDULED_AT = "SCHEDULED_AT"
        const val EXTRA_IS_SNOOZE = "IS_SNOOZE"
        const val EXTRA_IS_RETRY = "IS_RETRY"
        const val EXTRA_RETRY_ATTEMPT = "RETRY_ATTEMPT"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
        private const val REQUEST_CODE_FACTOR = 1_000
        private const val MAX_SLOTS = 5
        private const val SNOOZE_SLOT_OFFSET = 100
        private const val RETRY_SLOT_OFFSET = 200
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun programarAlarmas(medication: Medication) {
        cancelarAlarma(medication.id)
        if (!medication.alarmaActiva || !medication.estaActivo) {
            return
        }
        obtenerHorarios(medication).forEachIndexed { index, horario ->
            val triggerAt = calcularPrimerDisparo(medication, horario, index)
            programarSlot(
                medication = medication,
                slotIndex = index,
                triggerAtMillis = triggerAt,
                scheduledAtMillis = truncateToMinute(triggerAt)
            )
        }
    }

    fun cancelarAlarma(medicationId: Int) {
        repeat(MAX_SLOTS) { slotIndex ->
            cancelarPendingIntent(buildRequestCode(medicationId, slotIndex))
            cancelarPendingIntent(buildSnoozeRequestCode(medicationId, slotIndex))
            cancelarPendingIntent(buildRetryRequestCode(medicationId, slotIndex))
        }
    }

    fun cancelarRecordatoriosPendientes(medicationId: Int, slotIndex: Int) {
        cancelarPendingIntent(buildSnoozeRequestCode(medicationId, slotIndex))
        cancelarPendingIntent(buildRetryRequestCode(medicationId, slotIndex))
    }

    fun programarRecordatorioPospuesto(
        medication: Medication,
        slotIndex: Int,
        scheduledAtMillis: Long,
        delayMinutes: Int? = null
    ) {
        if (!medication.alarmaActiva || !medication.estaActivo) {
            return
        }

        val retryDelayMinutes = delayMinutes
            ?: CriticalAlertSettings.normalizeRetryInterval(medication.retryIntervalMinutes)

        cancelarRecordatoriosPendientes(medication.id, slotIndex)

        val triggerAtMillis = System.currentTimeMillis() + retryDelayMinutes * 60_000L
        programarSlot(
            medication = medication,
            slotIndex = slotIndex,
            triggerAtMillis = triggerAtMillis,
            scheduledAtMillis = scheduledAtMillis,
            isSnooze = true
        )
    }

    fun programarRecordatorioReintento(
        medication: Medication,
        slotIndex: Int,
        scheduledAtMillis: Long,
        retryAttempt: Int,
        delayMinutes: Int? = null,
        notificationId: Int = 0
    ) {
        if (!medication.alarmaActiva || !medication.estaActivo) {
            return
        }

        val retryDelayMinutes = delayMinutes
            ?: CriticalAlertSettings.normalizeRetryInterval(medication.retryIntervalMinutes)

        cancelarPendingIntent(buildRetryRequestCode(medication.id, slotIndex))

        val triggerAtMillis = System.currentTimeMillis() + retryDelayMinutes * 60_000L
        programarSlot(
            medication = medication,
            slotIndex = slotIndex,
            triggerAtMillis = triggerAtMillis,
            scheduledAtMillis = scheduledAtMillis,
            isRetry = true,
            retryAttempt = retryAttempt,
            notificationId = notificationId
        )
    }

    fun programarSiguienteToma(medication: Medication, slotIndex: Int) {
        if (!medication.alarmaActiva || !medication.estaActivo) {
            cancelarAlarma(medication.id)
            return
        }

        if (medication.esCicloCorto && System.currentTimeMillis() > medication.fechaFin) {
            cancelarAlarma(medication.id)
            return
        }

        val horarios = obtenerHorarios(medication)
        if (slotIndex !in horarios.indices) {
            return
        }

        val siguiente = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val partes = horarios[slotIndex].split(":")
            val hour = partes.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = partes.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, if (getTimeInMillis() <= System.currentTimeMillis()) 1 else 0)
        }

        if (!usaHorariosMultiples(medication)) {
            val intervalHours = medication.frecuenciaHoras.coerceAtLeast(24)
            while (siguiente.timeInMillis <= System.currentTimeMillis()) {
                siguiente.add(Calendar.HOUR_OF_DAY, intervalHours)
            }
        }

        if (medication.esCicloCorto && siguiente.timeInMillis > medication.fechaFin) {
            cancelarAlarma(medication.id)
            return
        }

        programarSlot(
            medication = medication,
            slotIndex = slotIndex,
            triggerAtMillis = siguiente.timeInMillis,
            scheduledAtMillis = truncateToMinute(siguiente.timeInMillis)
        )
    }

    private fun calcularPrimerDisparo(medication: Medication, horario: String, slotIndex: Int): Long {
        val partes = horario.split(":")
        val hour = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = partes.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            timeInMillis = medication.fechaInicio
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val ahora = System.currentTimeMillis()
        if (usaHorariosMultiples(medication)) {
            while (calendar.timeInMillis < maxOf(ahora, medication.fechaInicio)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            val intervalHours = medication.frecuenciaHoras.coerceAtLeast(24)
            while (calendar.timeInMillis < maxOf(ahora, medication.fechaInicio)) {
                calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
            }
        }

        if (medication.esCicloCorto && calendar.timeInMillis > medication.fechaFin) {
            return medication.fechaFin + 1
        }

        return calendar.timeInMillis
    }

    private fun programarSlot(
        medication: Medication,
        slotIndex: Int,
        triggerAtMillis: Long,
        scheduledAtMillis: Long,
        isSnooze: Boolean = false,
        isRetry: Boolean = false,
        retryAttempt: Int = 0,
        notificationId: Int = 0
    ) {
        if (medication.esCicloCorto && triggerAtMillis > medication.fechaFin) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MED_ID, medication.id)
            putExtra(EXTRA_MED_NOMBRE, medication.nombre)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
            putExtra(EXTRA_IS_RETRY, isRetry)
            putExtra(EXTRA_RETRY_ATTEMPT, retryAttempt)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAtMillis)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            when {
                isSnooze -> buildSnoozeRequestCode(medication.id, slotIndex)
                isRetry -> buildRetryRequestCode(medication.id, slotIndex)
                else -> buildRequestCode(medication.id, slotIndex)
            },
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }

                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun obtenerHorarios(medication: Medication): List<String> {
        return if (usaHorariosMultiples(medication)) {
            medication.horariosTomas.split("|").filter { it.isNotBlank() }
        } else {
            listOf(
                medication.horaToma.takeIf { it.isNotBlank() } ?: formatHora(medication.fechaInicio)
            )
        }
    }

    private fun usaHorariosMultiples(medication: Medication): Boolean {
        return medication.repartoDosis == "En diferentes horarios" && medication.horariosTomas.isNotBlank()
    }

    private fun formatHora(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    private fun buildRequestCode(medicationId: Int, slotIndex: Int): Int {
        return medicationId * REQUEST_CODE_FACTOR + slotIndex
    }

    private fun buildSnoozeRequestCode(medicationId: Int, slotIndex: Int): Int {
        return medicationId * REQUEST_CODE_FACTOR + SNOOZE_SLOT_OFFSET + slotIndex
    }

    private fun buildRetryRequestCode(medicationId: Int, slotIndex: Int): Int {
        return medicationId * REQUEST_CODE_FACTOR + RETRY_SLOT_OFFSET + slotIndex
    }

    private fun truncateToMinute(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun cancelarPendingIntent(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
