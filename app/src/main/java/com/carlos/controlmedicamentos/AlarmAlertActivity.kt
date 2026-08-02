package com.carlos.controlmedicamentos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.carlos.controlmedicamentos.notifications.AlarmActionExecutor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.notifications.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme

class AlarmAlertActivity : ComponentActivity() {

    // Scope propio para que el registro de tomas continúe incluso si la Activity finaliza.
    private val actionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val EXTRA_TITULO = "alarm_titulo"
        const val EXTRA_MENSAJE = "alarm_mensaje"
        const val EXTRA_NOTIFICATION_ID = "alarm_notification_id"
        const val EXTRA_REMINDER_TOKENS = "alarm_reminder_tokens"
        const val EXTRA_MED_COUNT = "alarm_med_count"
        const val EXTRA_IS_OVERDUE = "alarm_is_overdue"
        const val EXTRA_SCHEDULED_ACTION_LABEL = "alarm_scheduled_action_label"
        const val EXTRA_IS_ANTICONCEPTIVO = "is_anticonceptivo"
        const val EXTRA_ANTICONCEPTIVO_ID = "anticonceptivo_id"
        const val EXTRA_ANTICONCEPTIVO_SCHEDULED = "anticonceptivo_scheduled"
        const val EXTRA_LINEAS_DETALLE = "alarm_lineas_detalle"
        const val EXTRA_MED_NAMES = "alarm_med_names"
        const val EXTRA_MED_COLORS = "alarm_med_colors"
        const val EXTRA_MED_CONCENTRATIONS = "alarm_med_concentrations"
        const val EXTRA_MED_DOSES = "alarm_med_doses"
        const val EXTRA_MED_FORMS = "alarm_med_forms"
        const val EXTRA_HOUR_LABEL = "alarm_hour_label"
        const val EXTRA_PATIENT_NAME = "alarm_patient_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Hora de tu medicamento"
        val mensaje = intent.getStringExtra(EXTRA_MENSAJE) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val reminderTokens = intent.getStringArrayExtra(EXTRA_REMINDER_TOKENS)?.toList() ?: emptyList()
        val medCount = intent.getIntExtra(EXTRA_MED_COUNT, 1)
        val isOverdue = intent.getBooleanExtra(EXTRA_IS_OVERDUE, false)
        val scheduledActionLabel = intent.getStringExtra(EXTRA_SCHEDULED_ACTION_LABEL) ?: "Tomado en hora programada"
        val isAnticonceptivo = intent.getBooleanExtra(EXTRA_IS_ANTICONCEPTIVO, false)
        val anticonceptivoId = intent.getIntExtra(EXTRA_ANTICONCEPTIVO_ID, 0)
        val anticonceptivoScheduled = intent.getLongExtra(EXTRA_ANTICONCEPTIVO_SCHEDULED, 0L)
        val lineasDetalle = intent.getStringArrayExtra(EXTRA_LINEAS_DETALLE)?.toList() ?: emptyList()
        val medNames = intent.getStringArrayListExtra(EXTRA_MED_NAMES) ?: arrayListOf()
        val medColors = intent.getStringArrayListExtra(EXTRA_MED_COLORS) ?: arrayListOf()
        val medConcentrations = intent.getStringArrayListExtra(EXTRA_MED_CONCENTRATIONS) ?: arrayListOf()
        val medDoses = intent.getStringArrayListExtra(EXTRA_MED_DOSES) ?: arrayListOf()
        val medForms = intent.getStringArrayListExtra(EXTRA_MED_FORMS) ?: arrayListOf()
        val hourLabel = intent.getStringExtra(EXTRA_HOUR_LABEL) ?: ""
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: ""

        val medItems = medNames.indices.map { i ->
            MedAlertItem(
                name = medNames[i],
                colorHex = medColors.getOrElse(i) { "" },
                concentration = medConcentrations.getOrElse(i) { "" },
                dose = medDoses.getOrElse(i) { "" },
                form = medForms.getOrElse(i) { "" }
            )
        }

        setContent {
            ControlMedicamentosTheme {
                AlarmAlertScreen(
                    titulo = titulo,
                    mensaje = mensaje,
                    medCount = medCount,
                    isOverdue = isOverdue,
                    scheduledActionLabel = scheduledActionLabel,
                    lineasDetalle = lineasDetalle,
                    medItems = medItems,
                    hourLabel = hourLabel,
                    patientName = patientName,
                    onTomar = {
                        if (isAnticonceptivo && anticonceptivoId > 0) {
                            enviarAccionAnticonceptivo(notificationId, anticonceptivoId, anticonceptivoScheduled)
                            finish()
                        } else {
                            ejecutarAccionYFinalizar(AlarmReceiver.ACTION_ACCEPT, notificationId, reminderTokens)
                        }
                    },
                    onTomadoProgramado = {
                        ejecutarAccionYFinalizar(AlarmReceiver.ACTION_ACCEPT_SCHEDULED_TIME, notificationId, reminderTokens)
                    },
                    onNoTomado = {
                        ejecutarAccionYFinalizar(AlarmReceiver.ACTION_MARK_NOT_TAKEN, notificationId, reminderTokens)
                    },
                    onPosponer = { minutos ->
                        if (isAnticonceptivo && anticonceptivoId > 0) {
                            enviarAccionAnticonceptivoSnooze(notificationId, anticonceptivoId)
                            finish()
                        } else {
                            ejecutarAccionYFinalizar(AlarmReceiver.ACTION_SNOOZE, notificationId, reminderTokens, minutos)
                        }
                    }
                )
            }
        }
    }

    private fun ejecutarAccionYFinalizar(action: String, notificationId: Int, reminderTokens: List<String>, snoozeMinutes: Int = 0) {
        actionScope.launch {
            try {
                when (action) {
                    AlarmReceiver.ACTION_ACCEPT -> {
                        NotificacionHelper.cancelar(this@AlarmAlertActivity, notificationId)
                        AlarmActionExecutor.cancelPendingReminders(this@AlarmAlertActivity, reminderTokens)
                        AlarmActionExecutor.registerAcceptedTakes(this@AlarmAlertActivity, reminderTokens)
                    }
                    AlarmReceiver.ACTION_ACCEPT_SCHEDULED_TIME -> {
                        NotificacionHelper.cancelar(this@AlarmAlertActivity, notificationId)
                        AlarmActionExecutor.cancelPendingReminders(this@AlarmAlertActivity, reminderTokens)
                        AlarmActionExecutor.registerAcceptedTakes(
                            this@AlarmAlertActivity,
                            reminderTokens,
                            useScheduledTime = true
                        )
                    }
                    AlarmReceiver.ACTION_MARK_NOT_TAKEN -> {
                        NotificacionHelper.cancelar(this@AlarmAlertActivity, notificationId)
                        AlarmActionExecutor.cancelPendingReminders(this@AlarmAlertActivity, reminderTokens)
                        AlarmActionExecutor.registerNotTakenTakes(this@AlarmAlertActivity, reminderTokens)
                    }
                    AlarmReceiver.ACTION_SNOOZE -> {
                        NotificacionHelper.cancelar(this@AlarmAlertActivity, notificationId)
                        val firstToken = reminderTokens.firstOrNull()
                            ?.let { AlarmActionExecutor.parseReminderToken(it) }
                        AlarmActionExecutor.postponeReminders(
                            context = this@AlarmAlertActivity,
                            reminderTokens = reminderTokens,
                            fallbackMedId = firstToken?.medicationId ?: 0,
                            fallbackSlotIndex = firstToken?.slotIndex ?: 0,
                            fallbackScheduledAt = firstToken?.scheduledAt ?: System.currentTimeMillis(),
                            customDelayMinutes = snoozeMinutes
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmAlertActivity", "Error executing action $action", e)
            }
        }
        finish()
    }

    private fun enviarAccionAnticonceptivo(notificationId: Int, metodoId: Int, scheduledAt: Long) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            this.action = AlarmReceiver.ACTION_ACCEPT_ANTICONCEPTIVO
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmReceiver.EXTRA_ANTICONCEPTIVO_ID, metodoId)
            putExtra(AlarmReceiver.EXTRA_ANTICONCEPTIVO_SCHEDULED, scheduledAt)
        }
        sendBroadcast(intent)
        NotificacionHelper.cancelar(this, notificationId)
    }

    private fun enviarAccionAnticonceptivoSnooze(notificationId: Int, metodoId: Int) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            this.action = AlarmReceiver.ACTION_SNOOZE_ANTICONCEPTIVO
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmReceiver.EXTRA_ANTICONCEPTIVO_ID, metodoId)
        }
        sendBroadcast(intent)
        NotificacionHelper.cancelar(this, notificationId)
    }
}

private data class MedAlertItem(
    val name: String,
    val colorHex: String,
    val concentration: String,
    val dose: String,
    val form: String
)

private fun parseColorOrDefault(hex: String): Color {
    return hex.takeIf { it.isNotBlank() }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: Color.White
}

private fun labelForma(forma: String): String = when (forma.lowercase()) {
    "capsula" -> "cápsula"
    "redonda", "ovalada" -> "pastilla"
    "gota" -> "gota"
    "inyeccion" -> "inyección"
    "parche" -> "parche"
    "frasco" -> "dosis"
    else -> "unidad"
}

private fun buildGreeting(patientName: String, hourLabel: String): String {
    return if (patientName.isNotBlank() && !patientName.startsWith("Usuario")) {
        "Hola $patientName, es hora de tomar tus medicamentos de las $hourLabel."
    } else {
        "Es hora de tomar tus medicamentos de las $hourLabel."
    }
}

@Composable
private fun AlarmAlertScreen(
    titulo: String,
    mensaje: String,
    medCount: Int,
    isOverdue: Boolean,
    scheduledActionLabel: String,
    lineasDetalle: List<String> = emptyList(),
    medItems: List<MedAlertItem> = emptyList(),
    hourLabel: String = "",
    patientName: String = "",
    onTomar: () -> Unit,
    onTomadoProgramado: () -> Unit,
    onNoTomado: () -> Unit,
    onPosponer: (Int) -> Unit
) {
    val backgroundColor = Color(0xFF4A4A4A)
    val cardColor = Color(0xFF2C2C2C)
    val accentCyan = Color(0xFF00BCD4)
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button top-left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onNoTomado) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            // Greeting with avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFCC80)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = buildGreeting(patientName, hourLabel),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }

            // Card with time and medications
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = hourLabel,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (medItems.isEmpty() && mensaje.isNotBlank()) {
                        Text(
                            text = mensaje,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    } else {
                        medItems.forEachIndexed { index, med ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(parseColorOrDefault(med.colorHex))
                                )
                                Column {
                                    val nameLine = buildString {
                                        append(med.name)
                                        if (med.concentration.isNotBlank()) {
                                            append(" ")
                                            append(med.concentration)
                                        }
                                    }
                                    Text(
                                        text = nameLine,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val detailLine = buildString {
                                        if (med.concentration.isNotBlank()) {
                                            append(med.concentration)
                                            append(", ")
                                        }
                                        append("Toma ")
                                        append(med.dose.ifBlank { "1" })
                                        append(" ")
                                        append(labelForma(med.form))
                                        append("(s)")
                                    }
                                    Text(
                                        text = detailLine,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            if (index < medItems.lastIndex) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Omitir todas
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onNoTomado,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF555555))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Omitir",
                            tint = accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OMITIR TODAS",
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Tomar todas
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onTomar,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Tomar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TOMAR TODAS",
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Posponer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showSnoozeDialog = true },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF555555))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Posponer",
                            tint = accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "POSPONER",
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (showSnoozeDialog) {
            AlertDialog(
                onDismissRequest = { showSnoozeDialog = false },
                title = { Text("Posponer recordatorio") },
                text = {
                    Column {
                        Text("\u00bfPor cu\u00e1nto tiempo deseas posponer?", color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { minutos ->
                                OutlinedButton(onClick = {
                                    showSnoozeDialog = false
                                    onPosponer(minutos)
                                }) {
                                    Text("$minutos min")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter(Char::isDigit) },
                            label = { Text("Personalizado (minutos)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val minutos = customMinutesText.toIntOrNull()
                        if (minutos != null && minutos > 0) {
                            showSnoozeDialog = false
                            onPosponer(minutos)
                        }
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { showSnoozeDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
