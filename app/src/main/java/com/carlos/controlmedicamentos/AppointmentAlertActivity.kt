package com.carlos.controlmedicamentos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.notifications.AlarmReceiver
import com.carlos.controlmedicamentos.notifications.CriticalAlertService
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_TITLE = "appointment_title"
        const val EXTRA_DOCTOR = "appointment_doctor"
        const val EXTRA_LOCATION = "appointment_location"
        const val EXTRA_SCHEDULED_AT = "appointment_scheduled_at"
        const val EXTRA_NOTIFICATION_ID = "appointment_notification_id"
        const val EXTRA_PATIENT_NAME = "appointment_patient_name"
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

        val appointmentId = intent.getIntExtra(EXTRA_APPOINTMENT_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Cita médica"
        val doctor = intent.getStringExtra(EXTRA_DOCTOR) ?: ""
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: ""

        setContent {
            ControlMedicamentosTheme {
                AppointmentAlertScreen(
                    title = title,
                    doctor = doctor,
                    location = location,
                    scheduledAt = scheduledAt,
                    patientName = patientName,
                    onRealizado = { marcarRealizado(appointmentId, notificationId) },
                    onCerrar = { cerrar() }
                )
            }
        }
    }

    private fun marcarRealizado(appointmentId: Int, notificationId: Int) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ACCEPT_APPOINTMENT
            putExtra(MedicalAppointmentScheduler.EXTRA_APPOINTMENT_ID, appointmentId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        sendBroadcast(intent)
        NotificacionHelper.cancelarCitaMedica(this, appointmentId)
        CriticalAlertService.stop(this)
        finish()
    }

    private fun cerrar() {
        CriticalAlertService.stop(this)
        finish()
    }
}

@Composable
private fun AppointmentAlertScreen(
    title: String,
    doctor: String,
    location: String,
    scheduledAt: Long,
    patientName: String,
    onRealizado: () -> Unit,
    onCerrar: () -> Unit
) {
    val backgroundColor = Color(0xFF1C2150)
    val cardColor = Color(0xFF2A2F6A)
    val accentColor = Color(0xFF7B1FA2)
    val dateText = if (scheduledAt > 0) {
        SimpleDateFormat("EEEE dd MMM · HH:mm", Locale.forLanguageTag("es-ES")).format(Date(scheduledAt))
            .replaceFirstChar { it.uppercase() }
    } else "Fecha no disponible"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onCerrar) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recordatorio de cita",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (patientName.isNotBlank() && !patientName.startsWith("Usuario")) "Hola $patientName" else "Tienes una cita próximamente",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    DetailRow(Icons.Default.CalendarToday, dateText)
                    if (doctor.isNotBlank()) {
                        DetailRow(Icons.Default.Person, doctor)
                    }
                    if (location.isNotBlank()) {
                        DetailRow(Icons.Default.LocationOn, location)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onRealizado,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "MARCAR COMO REALIZADA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCerrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "CERRAR",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp
        )
    }
}
