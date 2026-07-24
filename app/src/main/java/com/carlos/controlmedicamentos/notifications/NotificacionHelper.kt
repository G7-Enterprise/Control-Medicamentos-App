package com.carlos.controlmedicamentos.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.carlos.controlmedicamentos.AlarmAlertActivity
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.CountryCurrencyCatalog
import com.carlos.controlmedicamentos.MainActivity
import com.carlos.controlmedicamentos.R
import com.carlos.controlmedicamentos.ReminderAlertActivity
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.formatMoney
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificacionHelper {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    internal const val CRITICAL_CHANNEL_ID = "MED_ALERTS_CRITICAL"
    private const val PLAYBACK_CHANNEL_ID = "MED_ALERTS_PLAYBACK"
    private const val APPOINTMENT_CHANNEL_ID = "MED_APPOINTMENTS"
    private const val VACCINATION_CHANNEL_ID = "MED_VACCINATIONS"
    private const val STOCK_CHANNEL_ID = "MED_STOCK"
    private const val SIGNOS_CHANNEL_ID = "MED_SIGNOS_VITALES"
    internal const val FALL_DETECTION_CHANNEL_ID = "FALL_DETECTION"
    const val SILENT_SOUND_URI = "silent"
    const val NOTIFICATION_ID_MISSED_MEDS = 999_998
    const val EXTRA_REMINDER_TOKENS = "REMINDER_TOKENS"
    const val EXTRA_LAUNCH_CRITICAL_ALERT = "EXTRA_LAUNCH_CRITICAL_ALERT"
    const val EXTRA_OPEN_SIGNOS_VITALES = "EXTRA_OPEN_SIGNOS_VITALES"
    const val EXTRA_OPEN_LISTA_INSUMOS = "EXTRA_OPEN_LISTA_INSUMOS"
    const val EXTRA_PEDIR_MEDICATION_ID = "EXTRA_PEDIR_MEDICATION_ID"
    const val CRITICAL_PLAYBACK_NOTIFICATION_ID = 91_001

    data class StockOrderItem(
        val medicationId: Int,
        val medicationName: String,
        val concentration: String,
        val remainingUnits: Int,
        val lowStockThreshold: Int,
        val unitsPerTake: Int,
        val unitPrice: Double?,
        val currencySymbol: String = CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL,
        val requestedUnits: Int? = null
    )

    fun suggestedUnitsForOrder(item: StockOrderItem): Int {
        return item.requestedUnits ?: if (item.remainingUnits >= 0) {
            (item.lowStockThreshold * 2 - item.remainingUnits).coerceAtLeast(item.unitsPerTake)
        } else {
            item.unitsPerTake
        }
    }

    fun estimateOrderTotal(items: List<StockOrderItem>): Double? {
        var estimatedTotal = 0.0
        var pricedItems = 0
        items.forEach { item ->
            item.unitPrice?.let { unitPrice ->
                estimatedTotal += suggestedUnitsForOrder(item) * unitPrice
                pricedItems += 1
            }
        }
        return estimatedTotal.takeIf { pricedItems > 0 }
    }

    fun summarizeOrderItems(items: List<StockOrderItem>): String {
        return items.joinToString(separator = " | ") { item ->
            buildString {
                append(item.medicationName)
                if (item.concentration.isNotBlank()) {
                    append(" ")
                    append(item.concentration)
                }
                append(" x")
                append(suggestedUnitsForOrder(item))
            }
        }
    }

    fun buildWhatsappOrderMessage(items: List<StockOrderItem>): String {
        return buildString {
            append(if (items.size > 1) "Hola, necesito reponer estos medicamentos:\n" else "Hola, necesito reponer este medicamento:\n")
            var estimatedTotal = 0.0
            items.forEach { item ->
                val hasTrackedStock = item.remainingUnits >= 0
                val suggestedUnits = suggestedUnitsForOrder(item)
                append("- ")
                append(item.medicationName)
                if (item.concentration.isNotBlank()) {
                    append(" ")
                    append(item.concentration)
                }
                if (hasTrackedStock) {
                    append(" · quedan ")
                    append(item.remainingUnits)
                    append(" u")
                } else {
                    append(" · sin control de stock")
                }
                append(" · pedir ")
                append(suggestedUnits)
                append(" u")
                item.unitPrice?.let {
                    estimatedTotal += suggestedUnits * it
                    append(" · ")
                    append(formatMoney(it, item.currencySymbol))
                    append("/u")
                }
                append("\n")
            }
            if (estimatedTotal > 0) {
                append("Total estimado: ")
                append(formatMoney(estimatedTotal, items.firstOrNull { it.unitPrice != null }?.currencySymbol ?: CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL))
                append("\n")
            }
            append("\nGracias.")
        }
    }

    fun mostrar(
        context: Context,
        titulo: String,
        mensaje: String,
        alarmaSonidoUri: String,
        reminderTokens: List<String>,
        notificationId: Int,
        retryActionLabel: String,
        lineasDetalle: List<String> = emptyList(),
        medCount: Int = 1,
        medNames: List<String> = emptyList(),
        medColors: List<String> = emptyList(),
        medConcentrations: List<String> = emptyList(),
        medDoses: List<String> = emptyList(),
        medForms: List<String> = emptyList(),
        hourLabel: String = "",
        patientName: String = ""
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val acceptIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
        }
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
        }

        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmAlertActivity.EXTRA_TITULO, titulo)
            putExtra(AlarmAlertActivity.EXTRA_MENSAJE, mensaje)
            putExtra(AlarmAlertActivity.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmAlertActivity.EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
            putExtra(AlarmAlertActivity.EXTRA_MED_COUNT, medCount)
            putExtra(AlarmAlertActivity.EXTRA_MED_NAMES, ArrayList(medNames))
            putExtra(AlarmAlertActivity.EXTRA_MED_COLORS, ArrayList(medColors))
            putExtra(AlarmAlertActivity.EXTRA_MED_CONCENTRATIONS, ArrayList(medConcentrations))
            putExtra(AlarmAlertActivity.EXTRA_MED_DOSES, ArrayList(medDoses))
            putExtra(AlarmAlertActivity.EXTRA_MED_FORMS, ArrayList(medForms))
            putExtra(AlarmAlertActivity.EXTRA_HOUR_LABEL, hourLabel)
            putExtra(AlarmAlertActivity.EXTRA_PATIENT_NAME, patientName)
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 3,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptLabel = if (medCount > 1) "Tomar todas" else "Tomar"

        val notification = NotificationCompat.Builder(context, CRITICAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    lineasDetalle.takeIf { it.isNotEmpty() }?.forEach(style::addLine)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, acceptLabel, acceptPendingIntent)
            .addAction(0, "Posponer", snoozePendingIntent)
            .build()

        manager.notify(notificationId, notification)
        CriticalAlertService.start(context, alarmaSonidoUri)
    }

    fun mostrarPendientes(
        context: Context,
        titulo: String,
        mensaje: String,
        alarmaSonidoUri: String,
        reminderTokens: List<String>,
        notificationId: Int,
        lineasDetalle: List<String> = emptyList(),
        medCount: Int = 1,
        scheduledActionLabel: String = "Tomado en hora programada",
        medNames: List<String> = emptyList(),
        medColors: List<String> = emptyList(),
        medConcentrations: List<String> = emptyList(),
        medDoses: List<String> = emptyList(),
        medForms: List<String> = emptyList(),
        hourLabel: String = "",
        patientName: String = ""
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val acceptNowIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
        }
        val acceptScheduledIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT_SCHEDULED_TIME
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
        }
        val notTakenIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MARK_NOT_TAKEN
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
        }

        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmAlertActivity.EXTRA_TITULO, titulo)
            putExtra(AlarmAlertActivity.EXTRA_MENSAJE, mensaje)
            putExtra(AlarmAlertActivity.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmAlertActivity.EXTRA_REMINDER_TOKENS, reminderTokens.toTypedArray())
            putExtra(AlarmAlertActivity.EXTRA_MED_COUNT, medCount)
            putExtra(AlarmAlertActivity.EXTRA_IS_OVERDUE, true)
            putExtra(AlarmAlertActivity.EXTRA_SCHEDULED_ACTION_LABEL, scheduledActionLabel)
            putExtra(AlarmAlertActivity.EXTRA_LINEAS_DETALLE, lineasDetalle.toTypedArray())
            putExtra(AlarmAlertActivity.EXTRA_MED_NAMES, ArrayList(medNames))
            putExtra(AlarmAlertActivity.EXTRA_MED_COLORS, ArrayList(medColors))
            putExtra(AlarmAlertActivity.EXTRA_MED_CONCENTRATIONS, ArrayList(medConcentrations))
            putExtra(AlarmAlertActivity.EXTRA_MED_DOSES, ArrayList(medDoses))
            putExtra(AlarmAlertActivity.EXTRA_MED_FORMS, ArrayList(medForms))
            putExtra(AlarmAlertActivity.EXTRA_HOUR_LABEL, hourLabel)
            putExtra(AlarmAlertActivity.EXTRA_PATIENT_NAME, patientName)
        }

        val acceptNowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            acceptNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acceptScheduledPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 5,
            acceptScheduledIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 6,
            notTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 7,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CRITICAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    lineasDetalle.takeIf { it.isNotEmpty() }?.forEach(style::addLine)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Tomado ahora", acceptNowPendingIntent)
            .addAction(0, scheduledActionLabel, acceptScheduledPendingIntent)
            .addAction(0, "No tomado", notTakenPendingIntent)
            .build()

        manager.notify(notificationId, notification)
        CriticalAlertService.start(context, alarmaSonidoUri)
    }

    fun cancelar(context: Context, notificationId: Int) {
        if (notificationId == 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
        CriticalAlertService.stop(context)
    }

    fun mostrarCitaMedica(
        context: Context,
        appointmentId: Int,
        titulo: String,
        mensaje: String,
        alarmaSonidoUri: String,
        notificationId: Int = 80_000 + appointmentId
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_LAUNCH_CRITICAL_ALERT, true)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            appointmentId * 10 + 7,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acceptIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT_APPOINTMENT
            putExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_ID, appointmentId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId * 10 + 8,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(0, "Aceptar", acceptPendingIntent)
            .build()

        manager.notify(notificationId, notification)
        CriticalAlertService.start(context, alarmaSonidoUri)
    }

    fun cancelarCitaMedica(context: Context, appointmentId: Int) {
        if (appointmentId == 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(80_000 + appointmentId)
        CriticalAlertService.stop(context)
    }

    fun mostrarVacunacion(
        context: Context,
        vaccinationId: Int,
        titulo: String,
        mensaje: String,
        alarmaSonidoUri: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            vaccinationId * 10 + 9,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VACCINATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        manager.notify(90_000 + vaccinationId, notification)
        CriticalAlertService.start(context, alarmaSonidoUri)
    }

    fun cancelarVacunacion(context: Context, vaccinationId: Int) {
        if (vaccinationId == 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(90_000 + vaccinationId)
        CriticalAlertService.stop(context)
    }

    fun mostrarStockBajo(
        context: Context,
        medicationId: Int,
        medicationName: String,
        concentration: String,
        remainingUnits: Int,
        unitsPerTake: Int,
        lowStockThreshold: Int,
        unitPrice: Double?,
        whatsappPhone: String,
        restockSource: String
    ) {
        mostrarStockBajoAgrupado(
            context = context,
            notificationId = 100_000 + medicationId,
            items = listOf(
                StockOrderItem(
                    medicationId = medicationId,
                    medicationName = medicationName,
                    concentration = concentration,
                    remainingUnits = remainingUnits,
                    lowStockThreshold = lowStockThreshold,
                    unitsPerTake = unitsPerTake,
                    unitPrice = unitPrice
                )
            ),
            whatsappPhone = whatsappPhone,
            restockSource = restockSource
        )
    }

    fun mostrarStockBajoAgrupado(
        context: Context,
        notificationId: Int,
        items: List<StockOrderItem>,
        whatsappPhone: String,
        restockSource: String
    ): Intent? {
        if (items.isEmpty()) return null

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_LISTA_INSUMOS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 11,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (items.size == 1) {
            "Stock bajo"
        } else {
            "Stock bajo en ${items.size} medicamentos"
        }
        val message = buildStockNotificationMessage(items)
        val detailLines = items.map { item ->
            item.medicationName
        }

        val whatsappPendingIntent = buildWhatsappPendingIntent(
            context = context,
            notificationId = notificationId,
            items = items,
            whatsappPhone = whatsappPhone,
            restockSource = restockSource
        )

        val verMedicamentosIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_LISTA_INSUMOS, true)
        }
        val verMedicamentosPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 13,
            verMedicamentosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ReminderAlertActivity.EXTRA_TYPE, ReminderAlertActivity.TYPE_STOCK_BAJO)
            putExtra(ReminderAlertActivity.EXTRA_STOCK_MESSAGE, message)
            putExtra(ReminderAlertActivity.EXTRA_STOCK_ITEMS_JSON, Gson().toJson(items))
            putExtra(ReminderAlertActivity.EXTRA_STOCK_WHATSAPP_PHONE, whatsappPhone)
            putExtra(ReminderAlertActivity.EXTRA_STOCK_RESTOCK_SOURCE, restockSource)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 14,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, STOCK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    detailLines.forEach(style::addLine)
                    style.setSummaryText(message)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .apply {
                addAction(0, "Ver medicamentos", verMedicamentosPendingIntent)
                whatsappPendingIntent?.let {
                    addAction(0, if (items.size > 1) "Pedir todo por WhatsApp" else "Pedir por WhatsApp", it)
                }
            }
            .build()

        manager.notify(notificationId, notification)
        return fullScreenIntent
    }

    fun abrirPedidoWhatsapp(
        context: Context,
        medicationId: Int,
        medicationName: String,
        concentration: String,
        remainingUnits: Int,
        lowStockThreshold: Int,
        unitsPerTake: Int,
        unitPrice: Double?,
        whatsappPhone: String,
        restockSource: String
    ): Boolean {
        return abrirPedidoWhatsappAgrupado(
            context = context,
            items = listOf(
                StockOrderItem(
                    medicationId = medicationId,
                    medicationName = medicationName,
                    concentration = concentration,
                    remainingUnits = remainingUnits,
                    lowStockThreshold = lowStockThreshold,
                    unitsPerTake = unitsPerTake,
                    unitPrice = unitPrice
                )
            ),
            whatsappPhone = whatsappPhone,
            restockSource = restockSource
        )
    }

    fun abrirPedidoWhatsappAgrupado(
        context: Context,
        items: List<StockOrderItem>,
        whatsappPhone: String,
        restockSource: String
    ): Boolean {
        val intent = buildWhatsappIntent(
            context = context,
            items = items,
            whatsappPhone = whatsappPhone,
            restockSource = restockSource
        ) ?: return false

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildWhatsappPendingIntent(
        context: Context,
        notificationId: Int,
        items: List<StockOrderItem>,
        whatsappPhone: String,
        restockSource: String
    ): PendingIntent? {
        val intent = buildWhatsappIntent(
            context = context,
            items = items,
            whatsappPhone = whatsappPhone,
            restockSource = restockSource
        ) ?: return null

        return PendingIntent.getActivity(
            context,
            notificationId * 10 + 12,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildWhatsappIntent(
        context: Context,
        items: List<StockOrderItem>,
        whatsappPhone: String,
        restockSource: String
    ): Intent? {
        if (items.isEmpty()) return null
        if (restockSource == RestockSource.INSS) return null

        val message = buildWhatsappOrderMessage(items)

        return when (restockSource) {
            RestockSource.WHATSAPP_CONTACT -> {
                val packageName = resolveWhatsappPackage(context)
                if (packageName == null) {
                    null
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        setPackage(packageName)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                }
            }

            else -> {
                val normalizedPhone = whatsappPhone.filter(Char::isDigit)
                if (normalizedPhone.isBlank()) {
                    null
                } else {
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$normalizedPhone?text=${Uri.encode(message)}")
                    )
                }
            }
        }
    }

    private fun resolveWhatsappPackage(context: Context): String? {
        return WHATSAPP_PACKAGES.firstOrNull { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun buildStockNotificationMessage(items: List<StockOrderItem>): String {
        if (items.size == 1) {
            val item = items.first()
            return if (item.remainingUnits <= 0) {
                "Se agotó el stock de ${item.medicationName}. Conviene preparar un nuevo pedido."
            } else if (item.remainingUnits == item.unitsPerTake) {
                "Solo queda stock para una toma de ${item.medicationName}."
            } else {
                "Stock bajo de ${item.medicationName}: quedan ${item.remainingUnits} unidades."
            }
        }

        return "Hay ${items.size} medicamentos con stock bajo listos para pedir juntos."
    }

    suspend fun verificarYNotificarStockBajo(context: Context, launchFullScreen: Boolean = false) {
        val db = AppDatabase.getDatabase(context)
        val allMedications = db.medicationDao().obtenerTodosLista()
        val carritoTodos = db.carritoPendienteDao().obtenerTodosLista()
        val carritoMedicationIds = carritoTodos.map { it.medicationId }.toSet()
        val lowStockPairs = allMedications.mapNotNull { med ->
            toStockOrderItem(db, med)?.let { med to it }
        }.filter { (med, _) -> med.id !in carritoMedicationIds }
        if (lowStockPairs.isEmpty()) return

        val grouped = lowStockPairs.groupBy { (med, _) ->
            val phoneKey = med.telefonoPedidoWhatsapp.filter(Char::isDigit)
            when (med.origenReposicion) {
                RestockSource.WHATSAPP_NUMBER -> "${med.origenReposicion}|$phoneKey"
                else -> med.origenReposicion
            }
        }
        var launched = false
        grouped.forEach { (groupKey, pairs) ->
            val items = pairs.map { it.second }.sortedBy { it.medicationName.lowercase() }
            val firstMed = pairs.first().first
            val fullScreenIntent = mostrarStockBajoAgrupado(
                context = context,
                notificationId = groupKey.hashCode(),
                items = items,
                whatsappPhone = firstMed.telefonoPedidoWhatsapp,
                restockSource = firstMed.origenReposicion
            )
            if (launchFullScreen && !launched && fullScreenIntent != null) {
                launched = true
                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(fullScreenIntent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    suspend fun verificarYNotificarTomasOlvidadas(context: Context) {
        ensureChannels(context)
        val db = AppDatabase.getDatabase(context)

        val now = System.currentTimeMillis()
        val lookbackDays = 7
        val lookbackStart = now - (lookbackDays * 24L * 60L * 60L * 1000L)

        detectarYNotificarTomasPerdidas(context, db, lookbackStart, now, isStartupCheck = true)
    }

    suspend fun detectarYNotificarTomasPerdidas(
        context: Context,
        db: AppDatabase,
        rangeStart: Long,
        rangeEnd: Long,
        isStartupCheck: Boolean = false
    ) {
        val missedDoses = mutableListOf<Pair<Medication, Long>>()

        db.medicationDao().obtenerActivosConAlarma().forEach { medication ->
            val scheduled = scheduledDoseTimesInRange(medication, rangeStart, rangeEnd)
            scheduled.forEach { (slotIndex, scheduledAt) ->
                if (scheduledAt >= rangeEnd) return@forEach
                val intake = db.medicationIntakeDao().buscarPorMedicamentoYHorario(
                    medication.id,
                    scheduledAt
                )
                if (intake == null) {
                    missedDoses.add(medication to scheduledAt)
                }
            }
        }

        if (missedDoses.isEmpty()) return

        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val patientNamesById = missedDoses.map { it.first.patientId }.distinct().associateWith { patientId ->
            db.patientProfileDao().buscarPorId(patientId)?.let { p ->
                listOfNotNull(p.nombre, p.apellidos).joinToString(" ")
            } ?: "Usuario $patientId"
        }

        val groupedByPatient = missedDoses.groupBy { it.first.patientId }
        val lineasDetalle = groupedByPatient.flatMap { (patientId, doses) ->
            val patientName = patientNamesById[patientId] ?: "Usuario $patientId"
            doses.map { (medication, scheduledAt) ->
                "$patientName · ${medication.nombre} · ${sdf.format(scheduledAt)}"
            }
        }

        val title = if (missedDoses.size == 1) {
            "Toma pendiente detectada"
        } else {
            "Tomas pendientes detectadas (${missedDoses.size})"
        }
        val message = if (isStartupCheck) {
            "Se encontraron tomas no registradas en los últimos días. Toca para abrir la app."
        } else {
            "Se encontraron tomas no registradas mientras el dispositivo estuvo apagado. Toca para abrir la app."
        }

        mostrarTomasPerdidasBoot(
            context = context,
            titulo = title,
            mensaje = message,
            lineasDetalle = lineasDetalle,
            notificationId = NOTIFICATION_ID_MISSED_MEDS
        )
    }

    private fun scheduledDoseTimesInRange(medication: Medication, rangeStart: Long, rangeEnd: Long): List<Pair<Int, Long>> {
        val result = mutableListOf<Pair<Int, Long>>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = rangeStart

        while (calendar.timeInMillis < rangeEnd) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            medication.horariosTomas.split("|").forEachIndexed { index, horarioStr ->
                val parts = horarioStr.split(":")
                if (parts.size >= 2) {
                    val hora = parts[0].toIntOrNull() ?: continue
                    val minuto = parts[1].toIntOrNull() ?: continue
                    calendar.set(Calendar.HOUR_OF_DAY, hora)
                    calendar.set(Calendar.MINUTE, minuto)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val scheduledTime = calendar.timeInMillis
                    if (scheduledTime >= rangeStart && scheduledTime < rangeEnd) {
                        result.add(index to scheduledTime)
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    private suspend fun toStockOrderItem(db: AppDatabase, medication: Medication): StockOrderItem? {
        if (!medication.estaActivo || medication.origenReposicion == RestockSource.INSS) {
            return null
        }
        val stock = medication.stockActual ?: return null
        val unitsPerTake = medication.dosis.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val threshold = (medication.stockMinimo ?: (unitsPerTake * 2)).coerceAtLeast(unitsPerTake)
        if (stock > threshold) {
            return null
        }
        val patient = db.patientProfileDao().buscarPorId(medication.patientId)
        return StockOrderItem(
            medicationId = medication.id,
            medicationName = medication.nombre,
            concentration = medication.concentracion,
            remainingUnits = stock,
            lowStockThreshold = threshold,
            unitsPerTake = unitsPerTake,
            unitPrice = medication.precioPorUnidad,
            currencySymbol = CountryCurrencyCatalog.symbolFor(patient?.pais.orEmpty(), patient?.moneda.orEmpty())
        )
    }

    fun buildPlaybackNotification(context: Context): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alerta critica de recordatorio")
            .setContentText("Reproduciendo recordatorio")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun resolveCriticalSoundUri(alarmaSonidoUri: String): Uri {
        return when {
            alarmaSonidoUri == SILENT_SOUND_URI -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            alarmaSonidoUri.isBlank() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> Uri.parse(alarmaSonidoUri)
        }
    }

    internal fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val criticalChannel = NotificationChannel(
            CRITICAL_CHANNEL_ID,
            "Alertas criticas de recordatorio",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas críticas con prioridad máxima y repetición hasta interacción"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 250, 300)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && manager.isNotificationPolicyAccessGranted) {
                setBypassDnd(true)
            }
        }

        val playbackChannel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            "Reproducción de alertas críticas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }

        val appointmentChannel = NotificationChannel(
            APPOINTMENT_CHANNEL_ID,
            "Recordatorios de citas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de proximas citas agendadas"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 200, 250)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && manager.isNotificationPolicyAccessGranted) {
                setBypassDnd(true)
            }
        }

        val vaccinationChannel = NotificationChannel(
            VACCINATION_CHANNEL_ID,
            "Recordatorios de vacunación",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de próximas vacunas o refuerzos"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val stockChannel = NotificationChannel(
            STOCK_CHANNEL_ID,
            "Avisos de stock de medicamentos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando el stock de un medicamento esta por agotarse"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 120, 180)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val signosChannel = NotificationChannel(
            SIGNOS_CHANNEL_ID,
            "Recordatorios de metricas diarias",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recordatorios diarios para registrar metricas"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val fallDetectionChannel = NotificationChannel(
            FALL_DETECTION_CHANNEL_ID,
            "Detección de caídas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación persistente mientras el monitoreo de caídas está activo"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(criticalChannel)
        manager.createNotificationChannel(playbackChannel)
        manager.createNotificationChannel(appointmentChannel)
        manager.createNotificationChannel(vaccinationChannel)
        manager.createNotificationChannel(stockChannel)
        manager.createNotificationChannel(signosChannel)
        manager.createNotificationChannel(fallDetectionChannel)
    }

    fun mostrarRecordatorioSignosVitales(
        context: Context,
        patientId: Int,
        patientName: String,
        alarmaSonidoUri: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val acceptIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT_SIGNOS_VITALES
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, 70_000 + patientId)
            putExtra(SignosVitalesScheduler.EXTRA_PATIENT_ID, patientId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            patientId * 10 + 8,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_LAUNCH_CRITICAL_ALERT, true)
            putExtra(EXTRA_OPEN_SIGNOS_VITALES, true)
            putExtra(SignosVitalesScheduler.EXTRA_PATIENT_ID, patientId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            patientId * 10 + 9,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SIGNOS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recordatorio de presión y signos vitales · $patientName")
            .setContentText("Aún no has registrado tu presión arterial y signos vitales hoy. Hazlo ahora.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Aún no has registrado tu presión arterial y signos vitales hoy. Hazlo ahora para llevar un control actualizado."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Registrar ahora", acceptPendingIntent)
            .build()

        manager.notify(70_000 + patientId, notification)
        CriticalAlertService.start(context, alarmaSonidoUri)
    }

    fun cancelarRecordatorioSignosVitales(context: Context, patientId: Int) {
        if (patientId == 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(70_000 + patientId)
    }

    fun mostrarTomasPerdidasBoot(
        context: Context,
        titulo: String,
        mensaje: String,
        lineasDetalle: List<String>,
        notificationId: Int
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_LAUNCH_CRITICAL_ALERT, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 9,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CRITICAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    lineasDetalle.forEach(style::addLine)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    // ── Hidratación ───────────────────────────────────────────────────────────
    private const val HIDRATACION_CHANNEL_ID = "HID_REMINDER"
    private const val SEDENTARISMO_CHANNEL_ID = "SED_REMINDER"
    private const val DENTISTA_CHANNEL_ID = "DENTISTA_REMINDER"

    fun mostrarRecordatorioHidratacion(context: Context, patientId: Int, patientName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)
        // Notificación base para compatibilidad
        val openIntent = Intent(context, com.carlos.controlmedicamentos.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(context, 75_000 + patientId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, SIGNOS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hora de hidratarte 💧 · $patientName")
            .setContentText("Llevas un rato sin tomar agua. Recuerda tu meta diaria de hidratacion.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        manager.notify(75_000 + patientId, n)
        // Lanzar actividad a pantalla completa
        val fullScreenIntent = Intent(context, com.carlos.controlmedicamentos.ReminderAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_TYPE, com.carlos.controlmedicamentos.ReminderAlertActivity.TYPE_HIDRATACION)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_PATIENT_NAME, patientName)
        }
        context.startActivity(fullScreenIntent)
    }

    // ── Sedentarismo ─────────────────────────────────────────────────────────
    fun mostrarAlertaSedentarismo(context: Context, patientId: Int, minutosInactivo: Int, metaMinutos: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)
        val recomendacion = "Llevas $minutosInactivo minutos sin moverte. Camina al menos ${metaMinutos} minutos para reactivar la circulación."
        // Notificación base para compatibilidad
        val openIntent = Intent(context, com.carlos.controlmedicamentos.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(context, 76_000 + patientId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, SIGNOS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Llevas $minutosInactivo minutos sin moverte 🚶")
            .setContentText(recomendacion)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        manager.notify(76_000 + patientId, n)
        // Lanzar actividad a pantalla completa
        val fullScreenIntent = Intent(context, com.carlos.controlmedicamentos.ReminderAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_TYPE, com.carlos.controlmedicamentos.ReminderAlertActivity.TYPE_SEDENTARISMO)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_PATIENT_NAME, "Usuario")
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_PATIENT_ID, patientId)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_MINUTES_INACTIVE, minutosInactivo)
            putExtra(com.carlos.controlmedicamentos.ReminderAlertActivity.EXTRA_META_MINUTOS, metaMinutos)
        }
        context.startActivity(fullScreenIntent)
    }

    // ── Dentista ─────────────────────────────────────────────────────────────
    fun mostrarRecordatorioCitaDentista(context: Context, patientId: Int, motivo: String, horasAntes: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)
        val openIntent = Intent(context, com.carlos.controlmedicamentos.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(context, 77_000 + patientId + horasAntes, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val titulo = if (horasAntes >= 24) "Tienes cita dental manana 🦷" else "Tu cita dental es en $horasAntes horas 🦷"
        val texto  = if (motivo.isNotBlank()) "Motivo: $motivo. Recuerda no comer 1 hora antes si es una intervencion." else "Recuerda prepararte con tiempo."
        val n = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        manager.notify(77_000 + patientId + horasAntes, n)
    }

    fun mostrarSeguimientoPostConsulta(context: Context, patientId: Int, motivo: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context)
        val openIntent = Intent(context, com.carlos.controlmedicamentos.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(context, 78_000 + patientId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val texto = "Como te encuentras despues de tu visita? Si tienes dolor fuerte o sangrado, contacta a tu dentista. Recuerda tomar los medicamentos recetados."
        val n = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Seguimiento post-consulta 🦷")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        manager.notify(78_000 + patientId, n)
    }

}
