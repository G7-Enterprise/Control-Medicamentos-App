package com.carlos.controlmedicamentos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carlos.controlmedicamentos.CountryCurrencyCatalog
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_NOT_TAKEN
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_TAKEN
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MedicationIntake
import com.carlos.controlmedicamentos.data.local.unidadesPorToma
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.data.local.VaccinationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ACCEPT = "com.carlos.controlmedicamentos.notifications.ACCEPT"
        const val ACTION_ACCEPT_SCHEDULED_TIME = "com.carlos.controlmedicamentos.notifications.ACCEPT_SCHEDULED_TIME"
        const val ACTION_MARK_NOT_TAKEN = "com.carlos.controlmedicamentos.notifications.MARK_NOT_TAKEN"
        const val ACTION_SNOOZE = "com.carlos.controlmedicamentos.notifications.SNOOZE"
        const val ACTION_ACCEPT_APPOINTMENT = "com.carlos.controlmedicamentos.notifications.ACCEPT_APPOINTMENT"
        const val ACTION_ACCEPT_ANTICONCEPTIVO = "com.carlos.controlmedicamentos.notifications.ACCEPT_ANTICONCEPTIVO"
        const val ACTION_SNOOZE_ANTICONCEPTIVO = "com.carlos.controlmedicamentos.notifications.SNOOZE_ANTICONCEPTIVO"
        const val ACTION_ACCEPT_SIGNOS_VITALES = "com.carlos.controlmedicamentos.notifications.ACCEPT_SIGNOS_VITALES"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
        const val EXTRA_ANTICONCEPTIVO_ID = "ANTICONCEPTIVO_ID"
        const val EXTRA_ANTICONCEPTIVO_SCHEDULED = "ANTICONCEPTIVO_SCHEDULED"
        private const val PREFS_NAME = "alarm_receiver_state"
        private const val KEY_LAST_GROUP = "last_group"
        private const val KEY_LAST_GROUP_AT = "last_group_at"
        private const val GROUP_DEDUP_WINDOW_MS = 60_000L
        private const val OVERDUE_LOOKBACK_DAYS = 3
        private const val MAX_OVERDUE_TAKES_IN_ALERT = 8
    }

    private data class ReminderCandidate(
        val medication: Medication,
        val slotIndex: Int,
        val scheduledAt: Long,
        val isOverdue: Boolean
    )

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appointmentId = intent.getIntExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_ID, 0)
        val vaccinationId = intent.getIntExtra(VaccinationScheduler.EXTRA_VACCINATION_ID, 0)
        val medId = intent.getIntExtra(MedicationScheduler.EXTRA_MED_ID, 0)
        val medNombre = intent.getStringExtra(MedicationScheduler.EXTRA_MED_NOMBRE) ?: "Medicamento"
        val slotIndex = intent.getIntExtra(MedicationScheduler.EXTRA_SLOT_INDEX, 0)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val originalNotificationId = intent.getIntExtra(MedicationScheduler.EXTRA_NOTIFICATION_ID, 0)
        val isSnooze = intent.getBooleanExtra(MedicationScheduler.EXTRA_IS_SNOOZE, false)
        val isRetry = intent.getBooleanExtra(MedicationScheduler.EXTRA_IS_RETRY, false)
        val retryAttempt = intent.getIntExtra(MedicationScheduler.EXTRA_RETRY_ATTEMPT, 0).coerceAtLeast(0)
        val appointmentRetryAttempt = intent.getIntExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_RETRY_ATTEMPT, 0).coerceAtLeast(0)
        val scheduledAt = intent.getLongExtra(
            MedicationScheduler.EXTRA_SCHEDULED_AT,
            truncateToMinute(System.currentTimeMillis())
        )
        val reminderTokens = intent.getStringArrayExtra(NotificacionHelper.EXTRA_REMINDER_TOKENS)?.toList().orEmpty()

        // Ejecutamos en un hilo de fondo (Coroutine)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                if (appointmentId > 0) {
                    val appointment = db.medicalAppointmentDao().buscarPorId(appointmentId)
                    if (intent.action == ACTION_ACCEPT_APPOINTMENT) {
                        if (appointment != null && !appointment.isCompleted) {
                            db.medicalAppointmentDao().guardar(appointment.copy(isCompleted = true))
                        }
                        MedicalAppointmentScheduler(context).cancelar(appointmentId)
                        NotificacionHelper.cancelarCitaMedica(context, appointmentId)
                        return@launch
                    }
                    if (appointment == null || appointment.isCompleted || !appointment.alarmEnabled || appointment.scheduledAt < System.currentTimeMillis()) {
                        MedicalAppointmentScheduler(context).cancelar(appointmentId)
                        NotificacionHelper.cancelarCitaMedica(context, appointmentId)
                        return@launch
                    }
                    val criticalAlertConfig = CriticalAlertSettings.load(context)
                    val patientName = appointment.patientId.let { patientId ->
                        db.patientProfileDao().buscarPorId(patientId)?.let(::formatPatientFullName)
                            ?: "Usuario ${appointment.patientId}"
                    }
                    val originalAppointmentNotificationId = intent.getIntExtra(MedicalAppointmentScheduler.EXTRA_NOTIFICATION_ID, 0)
                    val appointmentNotificationId = if (originalAppointmentNotificationId != 0) {
                        originalAppointmentNotificationId
                    } else {
                        80_000 + appointmentId
                    }

                    val title = intent.getStringExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: appointment.title
                    val doctor = intent.getStringExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_DOCTOR)
                        ?.takeIf { it.isNotBlank() }
                        ?: appointment.doctorName
                    val location = intent.getStringExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_LOCATION)
                        ?.takeIf { it.isNotBlank() }
                        ?: appointment.location
                    val scheduledAtAppointment = intent.getLongExtra(
                        MedicalAppointmentScheduler.EXTRA_APPOINTMENT_TIME,
                        appointment.scheduledAt
                    )

                    val message = buildAppointmentMessage(
                        patientName = patientName,
                        title = title,
                        doctor = doctor,
                        location = location,
                        scheduledAt = scheduledAtAppointment
                    )
                    NotificacionHelper.mostrarCitaMedica(
                        context = context,
                        appointmentId = appointmentId,
                        titulo = "Cita agendada · $patientName",
                        mensaje = message,
                        alarmaSonidoUri = criticalAlertConfig.soundUri,
                        notificationId = appointmentNotificationId
                    )

                    val nextRetryAttempt = appointmentRetryAttempt + 1
                    if (nextRetryAttempt <= criticalAlertConfig.maxRetryCount) {
                        MedicalAppointmentScheduler(context).programarReintento(
                            appointment = appointment,
                            retryAttempt = nextRetryAttempt,
                            delayMinutes = CriticalAlertSettings.normalizeRetryInterval(
                                criticalAlertConfig.retryIntervalMinutes
                            ),
                            notificationId = appointmentNotificationId
                        )
                    }
                    return@launch
                }

                if (vaccinationId > 0) {
                    val record = db.vaccinationRecordDao().buscarPorId(vaccinationId)
                    val nextDoseAt = intent.getLongExtra(
                        VaccinationScheduler.EXTRA_NEXT_DOSE_AT,
                        record?.nextDoseAt ?: 0L
                    )
                    if (record == null || !record.alarmEnabled || record.nextDoseAt == null || record.nextDoseAt <= 0L) {
                        VaccinationScheduler(context).cancelar(vaccinationId)
                        NotificacionHelper.cancelarVacunacion(context, vaccinationId)
                        return@launch
                    }

                    val criticalAlertConfig = CriticalAlertSettings.load(context)
                    val patientName = record.patientId.let { patientId ->
                        db.patientProfileDao().buscarPorId(patientId)?.let(::formatPatientFullName)
                            ?: "Usuario ${record.patientId}"
                    }
                    val vaccineName = intent.getStringExtra(VaccinationScheduler.EXTRA_VACCINE_NAME)
                        ?.takeIf { it.isNotBlank() }
                        ?: record.vaccineName

                    NotificacionHelper.mostrarVacunacion(
                        context = context,
                        vaccinationId = vaccinationId,
                        titulo = "Proximo registro · $patientName",
                        mensaje = buildVaccinationMessage(record, vaccineName, nextDoseAt, patientName),
                        alarmaSonidoUri = criticalAlertConfig.soundUri
                    )
                    return@launch
                }

                // ── Recordatorio de metricas diarias ──────────────────────
                if (intent.action == SignosVitalesScheduler.ACTION_SIGNOS_VITALES_REMINDER) {
                    val patientId = intent.getIntExtra(SignosVitalesScheduler.EXTRA_PATIENT_ID, 0)
                    val patientName = intent.getStringExtra(SignosVitalesScheduler.EXTRA_PATIENT_NAME) ?: "Usuario"
                    if (patientId > 0) {
                        val inicioHoy = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val finHoy = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        val registrosHoy = db.signosVitalesDao().obtenerEnRango(patientId, inicioHoy, finHoy)
                        if (registrosHoy.isEmpty()) {
                            val criticalAlertConfig = CriticalAlertSettings.load(context)
                            NotificacionHelper.mostrarRecordatorioSignosVitales(
                                context = context,
                                patientId = patientId,
                                patientName = patientName,
                                alarmaSonidoUri = criticalAlertConfig.soundUri
                            )
                        }
                        // Reprogramar para mañana
                        SignosVitalesScheduler(context).programar(patientId, patientName)
                    }
                    return@launch
                }

                if (intent.action == ACTION_ACCEPT_SIGNOS_VITALES) {
                    val patientId = intent.getIntExtra(SignosVitalesScheduler.EXTRA_PATIENT_ID, 0)
                    NotificacionHelper.cancelarRecordatorioSignosVitales(context, patientId)
                    val openIntent = android.content.Intent(context, com.carlos.controlmedicamentos.MainActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(NotificacionHelper.EXTRA_LAUNCH_CRITICAL_ALERT, true)
                        putExtra(NotificacionHelper.EXTRA_OPEN_SIGNOS_VITALES, true)
                        putExtra(SignosVitalesScheduler.EXTRA_PATIENT_ID, patientId)
                    }
                    context.startActivity(openIntent)
                    return@launch
                }

                // ── Recordatorio de hidratacion ────────────────────────────
                if (intent.action == HidratacionScheduler.ACTION_HIDRATACION_REMINDER) {
                    val pid  = intent.getIntExtra(HidratacionScheduler.EXTRA_PATIENT_ID, 0)
                    val pnom = intent.getStringExtra(HidratacionScheduler.EXTRA_PATIENT_NAME) ?: "Usuario"
                    if (pid > 0) {
                        NotificacionHelper.mostrarRecordatorioHidratacion(context, pid, pnom)
                        HidratacionScheduler(context).programar(pid, pnom)
                    }
                    return@launch
                }

                // ── Sedentarismo ───────────────────────────────────────────
                if (intent.action == SedentarismoScheduler.ACTION_SEDENTARISMO_CHECK) {
                    val pid = intent.getIntExtra(SedentarismoScheduler.EXTRA_PATIENT_ID, 0)
                    if (pid > 0) {
                        val config = db.sedentarismoDao().obtenerConfig(pid)
                        if (config != null && config.activado) {
                            val cal = java.util.Calendar.getInstance()
                            val hora = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            if (hora in config.horaInicioMonitoreo until config.horaFinMonitoreo) {
                                val inicioHoy = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val alertasPrevias = db.sedentarismoDao().contarAlertasHoy(pid, inicioHoy).first()
                                val intervaloMin = config.limiteInactividadMinutos
                                val minutosInactivo = (alertasPrevias + 1) * intervaloMin
                                val metaMinutos = when {
                                    minutosInactivo >= 360 -> 30
                                    minutosInactivo >= 180 -> 15
                                    else -> 5
                                }
                                db.sedentarismoDao().insertarRegistro(
                                    com.carlos.controlmedicamentos.data.local.RegistroSedentarismo(
                                        patientId = pid,
                                        tipoEvento = "ALERTA_INACTIVIDAD",
                                        minutosInactivo = minutosInactivo
                                    )
                                )
                                NotificacionHelper.mostrarAlertaSedentarismo(context, pid, minutosInactivo, metaMinutos)
                            }
                            SedentarismoScheduler(context).programar(pid)
                        }
                    }
                    return@launch
                }

                // ── Cita Dentista 24h ──────────────────────────────────────
                if (intent.action == DentistaScheduler.ACTION_DENTISTA_CITA_24H ||
                    intent.action == DentistaScheduler.ACTION_DENTISTA_CITA_2H) {
                    val pid    = intent.getIntExtra(DentistaScheduler.EXTRA_PATIENT_ID, 0)
                    val motivo = intent.getStringExtra(DentistaScheduler.EXTRA_MOTIVO) ?: ""
                    val horas  = if (intent.action == DentistaScheduler.ACTION_DENTISTA_CITA_24H) 24 else 2
                    NotificacionHelper.mostrarRecordatorioCitaDentista(context, pid, motivo, horas)
                    return@launch
                }

                // ── Seguimiento post-consulta dentista ────────────────────
                if (intent.action == DentistaScheduler.ACTION_DENTISTA_SEGUIMIENTO) {
                    val pid    = intent.getIntExtra(DentistaScheduler.EXTRA_PATIENT_ID, 0)
                    val motivo = intent.getStringExtra(DentistaScheduler.EXTRA_MOTIVO) ?: ""
                    NotificacionHelper.mostrarSeguimientoPostConsulta(context, pid, motivo)
                    return@launch
                }

                val medication = db.medicationDao().findById(medId)

                when (intent.action) {
                    ACTION_ACCEPT -> {
                        NotificacionHelper.cancelar(context, notificationId)
                        cancelarRecordatoriosPendientes(context, db, reminderTokens)
                        registrarTomasAceptadas(context, db, reminderTokens)
                        return@launch
                    }

                    ACTION_ACCEPT_SCHEDULED_TIME -> {
                        NotificacionHelper.cancelar(context, notificationId)
                        cancelarRecordatoriosPendientes(context, db, reminderTokens)
                        registrarTomasAceptadas(context, db, reminderTokens, useScheduledTime = true)
                        return@launch
                    }

                    ACTION_MARK_NOT_TAKEN -> {
                        NotificacionHelper.cancelar(context, notificationId)
                        cancelarRecordatoriosPendientes(context, db, reminderTokens)
                        registrarTomasNoTomadas(db, reminderTokens)
                        return@launch
                    }

                    ACTION_SNOOZE -> {
                        NotificacionHelper.cancelar(context, notificationId)
                        posponerRecordatorios(context, db, reminderTokens, medId, slotIndex, scheduledAt)
                        return@launch
                    }
                }

                if (medication == null || !medication.estaActivo || !medication.alarmaActiva) {
                    MedicationScheduler(context).cancelarAlarma(medId)
                    return@launch
                }

                if (medication.esCicloCorto && System.currentTimeMillis() > medication.fechaFin) {
                    MedicationScheduler(context).cancelarAlarma(medId)
                    return@launch
                }

                // Verificar si el medicamento ya fue tomado en este horario
                val intake = db.medicationIntakeDao().buscarPorMedicamentoYHorario(medId, scheduledAt)
                if (intake != null) {
                    MedicationScheduler(context).cancelarAlarma(medId)
                    return@launch
                }

                val scheduler = MedicationScheduler(context)
                val criticalAlertConfig = CriticalAlertSettings.load(context)

                // 1. Obtener la última medición de presión
                val ultimaPresion = db.signosVitalesDao().obtenerUltimoRegistroPorPaciente(medication.patientId)

                val minuteBucket = truncateToMinute(System.currentTimeMillis())
                val grupo = if (isRetry || isSnooze) {
                    listOf(MedicationTrigger(medication, slotIndex))
                } else {
                    obtenerGrupoDeTomas(db, medication, slotIndex, minuteBucket)
                }
                val patientNamesById = grupo.map { it.medication.patientId }.distinct().associateWith { patientId ->
                    db.patientProfileDao().buscarPorId(patientId)?.let(::formatPatientFullName) ?: "Usuario $patientId"
                }
                val patientNames = grupo.map { trigger ->
                    patientNamesById[trigger.medication.patientId] ?: "Usuario ${trigger.medication.patientId}"
                }.distinct()
                val groupKey = buildGroupKey(grupo, scheduledAt)
                val notificationIdToUse = if (isRetry && originalNotificationId != 0) {
                    originalNotificationId
                } else {
                    buildNotificationId(groupKey)
                }

                if (debeMostrarGrupo(context, groupKey)) {
                    val currentCandidates = grupo.map { trigger ->
                        ReminderCandidate(
                            medication = trigger.medication,
                            slotIndex = trigger.slotIndex,
                            scheduledAt = scheduledAt,
                            isOverdue = false
                        )
                    }
                    val currentTokenKeys = currentCandidates
                        .map { tokenFor(it.medication.id, it.slotIndex, it.scheduledAt) }
                        .toSet()
                    val overdueCandidates = obtenerTomasVencidasSinRegistrar(
                        db = db,
                        patientIds = grupo.map { it.medication.patientId }.distinct(),
                        now = minuteBucket,
                        excludedTokenKeys = currentTokenKeys
                    )
                    val mensajeBase = buildMensajeBase(
                        medications = grupo.map { it.medication },
                        patientNames = patientNames,
                        ultimaPresion = ultimaPresion
                    )
                    val retryActionLabel = if (grupo.size == 1) {
                        "Repetir ${CriticalAlertSettings.normalizeRetryInterval(grupo.first().medication.retryIntervalMinutes)} min"
                    } else {
                        "Repetir luego"
                    }

                    val hourLabel = formatHora(scheduledAt)
                    val firstPatientName = patientNames.firstOrNull() ?: ""

                    NotificacionHelper.mostrar(
                        context,
                        buildMedicationTitle(grupo.size, patientNames),
                        mensajeBase,
                        grupo.firstOrNull()?.medication?.alarmaSonidoUri ?: criticalAlertConfig.soundUri,
                        reminderTokens = currentCandidates.map { tokenFor(it.medication.id, it.slotIndex, it.scheduledAt) },
                        notificationId = notificationIdToUse,
                        retryActionLabel = retryActionLabel,
                        lineasDetalle = currentCandidates.map { candidate ->
                            val patientName = patientNamesById[candidate.medication.patientId] ?: "Usuario ${candidate.medication.patientId}"
                            val overduePrefix = if (candidate.isOverdue) "Pendiente ${formatRelativeDoseDate(candidate.scheduledAt)} · " else ""
                            "• $overduePrefix${candidate.medication.nombre} · ${formatHora(candidate.scheduledAt)} · $patientName"
                        },
                        medCount = currentCandidates.size,
                        medNames = currentCandidates.map { it.medication.nombre },
                        medColors = currentCandidates.map { it.medication.colorMedicamento },
                        medConcentrations = currentCandidates.map { it.medication.concentracion },
                        medDoses = currentCandidates.map { it.medication.unidadesPorToma().toString() },
                        medForms = currentCandidates.map { it.medication.formaMedicamento },
                        hourLabel = hourLabel,
                        patientName = firstPatientName
                    )
                    if (!isRetry && !isSnooze && overdueCandidates.isNotEmpty()) {
                        val overdueTokens = overdueCandidates.map { tokenFor(it.medication.id, it.slotIndex, it.scheduledAt) }
                        val overdueNotificationId = buildNotificationId("overdue|${overdueTokens.joinToString("|")}")
                        val overdueHourLabel = formatHora(overdueCandidates.firstOrNull()?.scheduledAt ?: scheduledAt)
                        val overduePatientName = patientNames.firstOrNull() ?: ""

                        NotificacionHelper.mostrarPendientes(
                            context = context,
                            titulo = buildOverdueMedicationTitle(overdueCandidates.size, patientNames),
                            mensaje = buildOverdueMedicationMessage(overdueCandidates.size),
                            alarmaSonidoUri = grupo.firstOrNull()?.medication?.alarmaSonidoUri ?: criticalAlertConfig.soundUri,
                            reminderTokens = overdueTokens,
                            notificationId = overdueNotificationId,
                            lineasDetalle = overdueCandidates.map { candidate ->
                                val patientName = patientNamesById[candidate.medication.patientId] ?: "Usuario ${candidate.medication.patientId}"
                                "Pendiente ${formatRelativeDoseDate(candidate.scheduledAt)} - ${candidate.medication.nombre} - ${formatDateTime(candidate.scheduledAt)} - $patientName"
                            },
                            medCount = overdueCandidates.size,
                            scheduledActionLabel = buildScheduledActionLabel(overdueCandidates),
                            medNames = overdueCandidates.map { it.medication.nombre },
                            medColors = overdueCandidates.map { it.medication.colorMedicamento },
                            medConcentrations = overdueCandidates.map { it.medication.concentracion },
                            medDoses = overdueCandidates.map { it.medication.unidadesPorToma().toString() },
                            medForms = overdueCandidates.map { it.medication.formaMedicamento },
                            hourLabel = overdueHourLabel,
                            patientName = overduePatientName
                        )
                    }
                    marcarGrupoMostrado(context, groupKey)
                }

                val nextRetryAttempt = retryAttempt + 1
                if (nextRetryAttempt <= criticalAlertConfig.maxRetryCount) {
                    grupo.forEach { trigger ->
                        scheduler.programarRecordatorioReintento(
                            medication = trigger.medication,
                            slotIndex = trigger.slotIndex,
                            scheduledAtMillis = scheduledAt,
                            retryAttempt = nextRetryAttempt,
                            delayMinutes = CriticalAlertSettings.normalizeRetryInterval(
                                trigger.medication.retryIntervalMinutes
                            ),
                            notificationId = notificationIdToUse
                        )
                    }
                }

                if (!isSnooze && !isRetry) {
                    scheduler.programarSiguienteToma(medication, slotIndex)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildAppointmentMessage(
        patientName: String,
        title: String,
        doctor: String,
        location: String,
        scheduledAt: Long
    ): String {
        val details = mutableListOf<String>()
        details += "Usuario: $patientName"
        details += title
        if (doctor.isNotBlank()) {
            details += "Profesional: $doctor"
        }
        if (location.isNotBlank()) {
            details += "Lugar: $location"
        }
        details += "Hora: ${formatDateTime(scheduledAt)}"
        return details.joinToString(". ")
    }

    private fun buildVaccinationMessage(
        record: VaccinationRecord,
        vaccineName: String,
        nextDoseAt: Long,
        patientName: String
    ): String {
        val details = mutableListOf<String>()
        details += "Usuario: $patientName"
        details += vaccineName
        if (record.doseLabel.isNotBlank()) {
            details += "Aplicación: ${record.doseLabel}"
        }
        details += "Proxima fecha: ${formatDateTime(nextDoseAt)}"
        details += "Ultimo registro: ${formatDateTime(record.appliedAt)}"
        if (record.notes.isNotBlank()) {
            details += "Notas: ${record.notes}"
        }
        return details.joinToString(". ")
    }

    private suspend fun posponerRecordatorios(
        context: Context,
        db: AppDatabase,
        reminderTokens: List<String>,
        fallbackMedId: Int,
        fallbackSlotIndex: Int,
        fallbackScheduledAt: Long
    ) {
        val scheduler = MedicationScheduler(context)
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
                scheduler.programarRecordatorioPospuesto(
                    medication = medication,
                    slotIndex = slotIndex,
                    scheduledAtMillis = parsedToken.scheduledAt,
                    delayMinutes = CriticalAlertSettings.normalizeRetryInterval(
                        medication.retryIntervalMinutes
                    )
                )
            }
        }
    }

    private suspend fun cancelarRecordatoriosPendientes(
        context: Context,
        db: AppDatabase,
        reminderTokens: List<String>
    ) {
        val scheduler = MedicationScheduler(context)
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            db.medicationDao().findById(token.medicationId) ?: return@forEach
            scheduler.cancelarRecordatoriosPendientes(token.medicationId, token.slotIndex)
        }
    }

    private suspend fun obtenerTomasVencidasSinRegistrar(
        db: AppDatabase,
        patientIds: List<Int>,
        now: Long,
        excludedTokenKeys: Set<String>
    ): List<ReminderCandidate> {
        if (patientIds.isEmpty()) return emptyList()

        val start = Calendar.getInstance().apply {
            timeInMillis = inicioDelDia(now)
            add(Calendar.DAY_OF_YEAR, -OVERDUE_LOOKBACK_DAYS)
        }.timeInMillis

        return buildList {
            db.medicationDao().obtenerTodosLista()
                .filter { it.patientId in patientIds && it.estaActivo && it.alarmaActiva }
                .forEach { medication ->
                    scheduledDoseTimesInRange(medication, start, now).forEach { (slotIndex, scheduledAt) ->
                        if (scheduledAt >= now) return@forEach
                        if (tokenFor(medication.id, slotIndex, scheduledAt) in excludedTokenKeys) return@forEach
                        val intake = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                            medication.id,
                            scheduledAt
                        )
                        if (intake == null) {
                            add(
                                ReminderCandidate(
                                    medication = medication,
                                    slotIndex = slotIndex,
                                    scheduledAt = scheduledAt,
                                    isOverdue = true
                                )
                            )
                        }
                    }
                }
        }.sortedByDescending { it.scheduledAt }
            .take(MAX_OVERDUE_TAKES_IN_ALERT)
            .sortedBy { it.scheduledAt }
            .toList()
    }

    private suspend fun registrarTomasAceptadas(
        context: Context,
        db: AppDatabase,
        reminderTokens: List<String>,
        useScheduledTime: Boolean = false
    ) {
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            val existing = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                token.medicationId,
                token.scheduledAt
            )
            if (existing != null) {
                return@forEach
            }

            val medication = db.medicationDao().findById(token.medicationId) ?: return@forEach
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
        }
    }

    private suspend fun registrarTomasNoTomadas(
        db: AppDatabase,
        reminderTokens: List<String>
    ) {
        reminderTokens.mapNotNull(::parseReminderToken).forEach { token ->
            val existing = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                token.medicationId,
                token.scheduledAt
            )
            if (existing != null) {
                return@forEach
            }

            val medication = db.medicationDao().findById(token.medicationId) ?: return@forEach
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
        }
    }

    private suspend fun obtenerGrupoDeTomas(
        db: AppDatabase,
        medication: Medication,
        slotIndex: Int,
        now: Long
    ): List<MedicationTrigger> {
        val minuteBucket = truncateToMinute(now)
        val activos = db.medicationDao().obtenerActivosConAlarma()
        val grupo = activos.mapNotNull { candidata ->
            resolveTriggeredSlot(candidata, minuteBucket)?.let { triggeredSlot ->
                MedicationTrigger(candidata, triggeredSlot)
            }
        }.filter { it.slotIndex == slotIndex || it.medication.id != medication.id }

        return grupo.ifEmpty { listOf(MedicationTrigger(medication, slotIndex)) }
            .sortedBy { it.medication.nombre.lowercase() }
    }

    private fun resolveTriggeredSlot(medication: Medication, minuteBucket: Long): Int? {
        if (!medication.estaActivo || !medication.alarmaActiva) return null
        if (medication.esCicloCorto && minuteBucket > medication.fechaFin) return null

        val horarios = obtenerHorarios(medication)
        return horarios.indexOfFirst { horario ->
            val horarioMillis = horarioEnDia(minuteBucket, horario)
            horarioMillis == minuteBucket && horarioMillis >= truncateToMinute(medication.fechaInicio) &&
                (!medication.esCicloCorto || horarioMillis <= medication.fechaFin)
        }.takeIf { it >= 0 }
    }

    private fun obtenerHorarios(medication: Medication): List<String> {
        return if (medication.repartoDosis == "En diferentes horarios" && medication.horariosTomas.isNotBlank()) {
            medication.horariosTomas.split("|").filter { it.isNotBlank() }
        } else {
            listOf(medication.horaToma.takeIf { it.isNotBlank() } ?: formatHora(medication.fechaInicio))
        }
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

        val horarios = obtenerHorarios(medication)
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

    private fun buildMensajeBase(
        medications: List<Medication>,
        patientNames: List<String>,
        ultimaPresion: com.carlos.controlmedicamentos.data.local.SignosVitales?,
        overdueCount: Int = 0
    ): String {
        val nombres = medications.joinToString(", ") { it.nombre }
        val patientPrefix = when (patientNames.size) {
            0 -> ""
            1 -> "Usuario: ${patientNames.first()}. "
            else -> "Usuarios: ${patientNames.joinToString(", ")}. "
        }
        val overdueSuffix = when (overdueCount) {
            0 -> ""
            1 -> " Hay 1 toma anterior sin marcar; puedes registrarla en este aviso."
            else -> " Hay $overdueCount tomas anteriores sin marcar; puedes registrarlas en este aviso."
        }
        val presion = ultimaPresion
        val sis = presion?.sistolica
        val dias = presion?.diastolica
        val base = when {
            medications.size > 1 && presion == null -> "${patientPrefix}Es hora de tus medicamentos: $nombres. No olvides registrar la presion."
            medications.size > 1 && sis != null && dias != null && (sis > 140 || dias > 90) ->
                "${patientPrefix}Hay varios medicamentos ahora: $nombres. La presion esta alta (${sis}/${dias})."
            medications.size > 1 && sis != null && sis < 100 ->
                "${patientPrefix}Hay varios medicamentos ahora: $nombres. La presion esta baja (${sis})."
            medications.size > 1 -> "${patientPrefix}Es hora de tus medicamentos: $nombres."
            presion == null -> "${patientPrefix}Es hora de ${medications.first().nombre}. No olvides registrar la presion."
            sis != null && dias != null && (sis > 140 || dias > 90) ->
                "${patientPrefix}Alerta: la presion esta alta (${sis}/${dias}). Registra ${medications.first().nombre} ahora."
            sis != null && sis < 100 ->
                "${patientPrefix}Atencion: la presion es baja (${sis}). Verifica si debes usar ${medications.first().nombre}."
            else -> "${patientPrefix}Hora de ${medications.first().nombre}. Las metricas estan estables."
        }
        return base + overdueSuffix
    }

    private fun buildMedicationTitle(groupSize: Int, patientNames: List<String>): String {
        val patientSuffix = if (patientNames.size == 1) " · ${patientNames.first()}" else ""
        return if (groupSize > 1) "Hora de tus medicamentos$patientSuffix" else "Control de medicamentos$patientSuffix"
    }

    private fun buildOverdueMedicationTitle(count: Int, patientNames: List<String>): String {
        val patientSuffix = if (patientNames.size == 1) " - ${patientNames.first()}" else ""
        return if (count > 1) "Tomas pendientes$patientSuffix" else "Toma pendiente$patientSuffix"
    }

    private fun buildOverdueMedicationMessage(count: Int): String {
        return if (count > 1) {
            "Hay $count avisos anteriores sin marcar. Elige como registrarlos."
        } else {
            "Hay un aviso anterior sin marcar. Elige como registrarlo."
        }
    }

    private fun buildScheduledActionLabel(candidates: List<ReminderCandidate>): String {
        val first = candidates.firstOrNull() ?: return "Tomado en hora programada"
        return if (candidates.size == 1) {
            "Tomado en ${formatDateTime(first.scheduledAt)}"
        } else {
            "Tomado en hora programada"
        }
    }

    private fun formatPatientFullName(profile: com.carlos.controlmedicamentos.data.local.PatientProfile): String {
        val fullName = listOf(profile.nombre, profile.apellidos)
            .map(String::trim)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return fullName.ifBlank { "Usuario ${profile.id}" }
    }

    private fun debeMostrarGrupo(context: Context, groupKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastGroup = prefs.getString(KEY_LAST_GROUP, null)
        val lastAt = prefs.getLong(KEY_LAST_GROUP_AT, 0L)
        val now = System.currentTimeMillis()
        return !(groupKey == lastGroup && now - lastAt < GROUP_DEDUP_WINDOW_MS)
    }

    private fun marcarGrupoMostrado(context: Context, groupKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_GROUP, groupKey)
            .putLong(KEY_LAST_GROUP_AT, System.currentTimeMillis())
            .apply()
    }

    private fun buildGroupKey(grupo: List<MedicationTrigger>, minuteBucket: Long): String {
        val ids = grupo.map { tokenFor(it.medication.id, it.slotIndex, minuteBucket) }.sorted().joinToString("|")
        return "$minuteBucket|$ids"
    }

    private fun buildNotificationId(groupKey: String): Int = groupKey.hashCode()

    private fun buildStockNotificationId(medication: Medication): Int = buildRestockGroupKey(medication).hashCode()

    private fun buildRestockGroupKey(medication: Medication): String {
        val phoneKey = medication.telefonoPedidoWhatsapp.filter(Char::isDigit)
        return when (medication.origenReposicion) {
            RestockSource.WHATSAPP_NUMBER -> "${medication.origenReposicion}|$phoneKey"
            else -> medication.origenReposicion
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

    private fun formatRelativeDoseDate(timestamp: Long): String {
        val today = inicioDelDia(System.currentTimeMillis())
        val doseDay = inicioDelDia(timestamp)
        val diffDays = ((today - doseDay) / 86_400_000L).toInt()
        return when (diffDays) {
            0 -> "de hoy"
            1 -> "de ayer"
            else -> "del ${formatDate(timestamp)}"
        }
    }

    private suspend fun toStockOrderItem(db: AppDatabase, medication: Medication): NotificacionHelper.StockOrderItem? {
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

    private fun formatDateTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%02d/%02d/%04d %02d:%02d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%02d/%02d/%04d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
    }

    private fun tokenFor(medicationId: Int, slotIndex: Int, scheduledAt: Long): String {
        return "$medicationId:$slotIndex:$scheduledAt"
    }

    private fun parseReminderToken(token: String): ReminderToken? {
        val parts = token.split(":")
        val medicationId = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val slotIndex = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val scheduledAt = parts.getOrNull(2)?.toLongOrNull() ?: return null
        return ReminderToken(medicationId, slotIndex, scheduledAt)
    }

    private fun truncateToMinute(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun horarioEnDia(referenceMinute: Long, horario: String): Long {
        val parts = horario.split(":")
        return Calendar.getInstance().apply {
            timeInMillis = referenceMinute
            set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 8)
            set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun formatHora(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private data class MedicationTrigger(
        val medication: Medication,
        val slotIndex: Int
    )

    private data class ReminderToken(
        val medicationId: Int,
        val slotIndex: Int,
        val scheduledAt: Long
    )
}
