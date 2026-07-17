package com.carlos.controlmedicamentos.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carlos.controlmedicamentos.R
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MetodoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.TipoAnticonceptivo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "boot_receiver_prefs"
        private const val KEY_LAST_BOOT_TIME = "last_boot_time"
        private const val MAX_LOOKBACK_DAYS = 3
        private const val NOTIFICATION_ID_MISSED_MEDS = 300_000
        private const val NOTIFICATION_ID_MISSED_ANTICONCEPTIVOS = 300_001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("BootReceiver", "onReceive action=$action")
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                Log.d("BootReceiver", "Coroutine started at $now")

                val db = AppDatabase.getDatabase(context)
                val scheduler = MedicationScheduler(context)
                val appointmentScheduler = MedicalAppointmentScheduler(context)
                val vaccinationScheduler = VaccinationScheduler(context)
                val vitalSignsScheduler = SignosVitalesScheduler(context)
                val anticonceptivoScheduler = AnticonceptivoScheduler(context)

                // Reprogramar alarmas de medicamentos
                val medsConAlarma = db.medicationDao().obtenerActivosConAlarma()
                Log.d("BootReceiver", "Medicamentos con alarma: ${medsConAlarma.size}")
                medsConAlarma.forEach { medication ->
                    scheduler.programarAlarmas(medication)
                }

                // Reprogramar citas pendientes
                db.medicalAppointmentDao().obtenerPendientesConAlarma(now).forEach { appointment ->
                    if (!appointment.isCompleted && appointment.scheduledAt > now) {
                        appointmentScheduler.programar(appointment)
                    }
                }

                // Reprogramar vacunas con proximo refuerzo pendiente
                db.vaccinationRecordDao().obtenerPendientesConAlarma(now).forEach { record ->
                    if (record.nextDoseAt != null && record.nextDoseAt > now) {
                        vaccinationScheduler.programar(record)
                    }
                }

                // Reprogramar recordatorio diario de métricas si estaba activo
                val reminderSettings = SignosVitalesScheduler.loadSettings(context)
                val (savedPatientId, savedPatientName) = SignosVitalesScheduler.loadPatientInfo(context)
                if (reminderSettings.third && savedPatientId > 0) {
                    vitalSignsScheduler.programar(savedPatientId, savedPatientName)
                }

                // Reprogramar anticonceptivos activos
                val metodosActivos = db.metodoAnticonceptivoDao().obtenerActivos()
                Log.d("BootReceiver", "Métodos anticonceptivos activos: ${metodosActivos.size}")
                metodosActivos.forEach { metodo ->
                    anticonceptivoScheduler.programarAlarma(metodo)
                }

                // Reprogramar hidratacion
                val (hidPatientId, hidPatientName) = HidratacionScheduler.loadPatientInfo(context)
                val hidSettings = HidratacionScheduler.loadSettings(context)
                if (hidSettings.enabled && hidPatientId > 0) {
                    HidratacionScheduler(context).programar(hidPatientId, hidPatientName)
                }

                // Reprogramar sedentarismo para el perfil activo
                val perfiles = db.patientProfileDao().obtenerTodosLista()
                val perfilActivo = perfiles.find { it.isActive } ?: perfiles.firstOrNull()
                if (perfilActivo != null) {
                    SedentarismoScheduler(context).programar(perfilActivo.id)
                }

                // Reprogramar citas dentistas pendientes
                val dentistaScheduler = DentistaScheduler(context)
                val visitasPendientes = db.visitaDentistaDao().obtenerPorPacienteLista(0).filter { it.estado == "PENDIENTE" }
                visitasPendientes.forEach { dentistaScheduler.programarCita(it) }

                // ── Detectar tomas perdidas mientras el teléfono estuvo apagado ──
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastBootTime = prefs.getLong(KEY_LAST_BOOT_TIME, 0L)
                Log.d("BootReceiver", "lastBootTime=$lastBootTime, now=$now")
                prefs.edit().putLong(KEY_LAST_BOOT_TIME, now).apply()

                if (lastBootTime > 0) {
                    val lookbackStart = maxOf(lastBootTime, now - MAX_LOOKBACK_DAYS * 24L * 60L * 60L * 1000L)
                    Log.d("BootReceiver", "lookbackStart=$lookbackStart range=${lookbackStart..now}")
                    NotificacionHelper.detectarYNotificarTomasPerdidas(context, db, lookbackStart, now, isStartupCheck = false)
                    detectarYNotificarAnticonceptivosPerdidos(context, db, lookbackStart, now)
                } else {
                    Log.d("BootReceiver", "Primer boot detectado, no hay lookback previo")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error en boot receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun mostrarNotificacionDiagnostico(context: Context, titulo: String, mensaje: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            NotificacionHelper.ensureChannels(context)
            val notification = NotificationCompat.Builder(context, NotificacionHelper.CRITICAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(999_999, notification)
        } catch (e: Exception) {
            Log.e("BootReceiver", "No se pudo mostrar notificación de diagnóstico", e)
        }
    }

    
    private suspend fun detectarYNotificarAnticonceptivosPerdidos(
        context: Context,
        db: AppDatabase,
        rangeStart: Long,
        rangeEnd: Long
    ) {
        Log.d("BootReceiver", "detectarYNotificarAnticonceptivosPerdidos range=$rangeStart..$rangeEnd")
        val missedDoses = mutableListOf<Pair<MetodoAnticonceptivo, Long>>()

        db.metodoAnticonceptivoDao().obtenerActivos().forEach { metodo ->
            val tipo = TipoAnticonceptivo.fromDisplayName(metodo.tipo)
            Log.d("BootReceiver", "Metodo ${metodo.tipo} requiereAlarma=${tipo.requiereAlarmaDiaria}")
            if (!tipo.requiereAlarmaDiaria) return@forEach

            val scheduledTimes = scheduledAnticonceptivoTimesInRange(metodo, rangeStart, rangeEnd)
            Log.d("BootReceiver", "  Scheduled times: ${scheduledTimes.size}")
            scheduledTimes.forEach { scheduledAt ->
                if (scheduledAt >= rangeEnd) return@forEach
                val intake = db.anticonceptivoIntakeDao().obtenerPorMetodoYHorario(metodo.id, scheduledAt)
                Log.d("BootReceiver", "    Buscar intake metodo=${metodo.id} scheduledAt=$scheduledAt -> intake=${intake != null}")
                if (intake == null) {
                    missedDoses.add(metodo to scheduledAt)
                }
            }
        }

        Log.d("BootReceiver", "Anticonceptivos perdidos detectados: ${missedDoses.size}")
        if (missedDoses.isEmpty()) return

        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val patientNamesById = missedDoses.map { it.first.patientId }.distinct().associateWith { patientId ->
            db.patientProfileDao().buscarPorId(patientId)?.let { p ->
                listOfNotNull(p.nombre, p.apellidos).joinToString(" ")
            } ?: "Usuario $patientId"
        }

        val lineasDetalle = missedDoses.map { (metodo, scheduledAt) ->
            val patientName = patientNamesById[metodo.patientId] ?: "Usuario ${metodo.patientId}"
            "$patientName · ${metodo.tipo} · ${sdf.format(scheduledAt)}"
        }

        val title = if (missedDoses.size == 1) {
            "Anticonceptivo pendiente detectado"
        } else {
            "Anticonceptivos pendientes detectados (${missedDoses.size})"
        }
        val message = "Se encontraron registros no realizados mientras el dispositivo estuvo apagado. Toca para abrir la app."

        NotificacionHelper.mostrarTomasPerdidasBoot(
            context = context,
            titulo = title,
            mensaje = message,
            lineasDetalle = lineasDetalle,
            notificationId = NOTIFICATION_ID_MISSED_ANTICONCEPTIVOS
        )
    }

    private fun scheduledDoseTimesInRange(
        medication: Medication,
        rangeStart: Long,
        rangeEnd: Long
    ): List<Pair<Int, Long>> {
        val effectiveStart = maxOf(rangeStart, truncateToMinute(medication.fechaInicio))
        val effectiveEnd = if (medication.esCicloCorto) {
            minOf(rangeEnd, truncateToMinute(medication.fechaFin))
        } else {
            rangeEnd
        }
        if (effectiveStart > effectiveEnd) return emptyList()

        val horarios = if (medication.repartoDosis == "En diferentes horarios" && medication.horariosTomas.isNotBlank()) {
            medication.horariosTomas.split("|").filter { it.isNotBlank() }
        } else {
            listOf(medication.horaToma.takeIf { it.isNotBlank() } ?: formatHora(medication.fechaInicio))
        }

        if (medication.repartoDosis == "En diferentes horarios" && medication.horariosTomas.isNotBlank()) {
            return buildList {
                horarios.forEachIndexed { slotIndex, horario ->
                    val partes = horario.split(":")
                    val hour = partes.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = partes.getOrNull(1)?.toIntOrNull() ?: 0
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = inicioDelDia(effectiveStart)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    while (calendar.timeInMillis < effectiveStart) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    while (calendar.timeInMillis <= effectiveEnd) {
                        add(slotIndex to calendar.timeInMillis)
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
        }

        val horario = horarios.firstOrNull().orEmpty()
        val partes = horario.split(":")
        val hour = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = partes.getOrNull(1)?.toIntOrNull() ?: 0
        val intervalHours = medication.frecuenciaHoras.coerceAtLeast(24)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = medication.fechaInicio
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (calendar.timeInMillis < effectiveStart) {
            calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
        }

        return buildList {
            while (calendar.timeInMillis <= effectiveEnd) {
                add(0 to calendar.timeInMillis)
                calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
            }
        }
    }

    private fun scheduledAnticonceptivoTimesInRange(
        metodo: MetodoAnticonceptivo,
        rangeStart: Long,
        rangeEnd: Long
    ): List<Long> {
        val partes = metodo.horaToma.split(":").map { it.toIntOrNull() ?: 0 }
        val hour = partes.getOrElse(0) { 8 }
        val minute = partes.getOrElse(1) { 0 }

        val tipo = TipoAnticonceptivo.fromDisplayName(metodo.tipo)
        val intervalDays = when (tipo) {
            TipoAnticonceptivo.PILDORA_COMBINADA, TipoAnticonceptivo.MINIPILDORA -> 1
            TipoAnticonceptivo.PARCHE -> 7
            TipoAnticonceptivo.ANILLO -> 30
            else -> 1
        }

        val effectiveStart = maxOf(rangeStart, truncateToMinute(metodo.fechaInicio))
        if (effectiveStart > rangeEnd) return emptyList()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = metodo.fechaInicio
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (calendar.timeInMillis < effectiveStart) {
            calendar.add(Calendar.DAY_OF_YEAR, intervalDays)
        }

        return buildList {
            while (calendar.timeInMillis <= rangeEnd) {
                add(calendar.timeInMillis)
                calendar.add(Calendar.DAY_OF_YEAR, intervalDays)
            }
        }
    }

    private fun inicioDelDia(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun truncateToMinute(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun formatHora(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
}
