package com.carlos.controlmedicamentos

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ReminderAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_PATIENT_NAME = "EXTRA_PATIENT_NAME"
        const val EXTRA_MINUTES_INACTIVE = "EXTRA_MINUTES_INACTIVE"
        const val EXTRA_STOCK_MESSAGE = "EXTRA_STOCK_MESSAGE"
        const val TYPE_HIDRATACION = "HIDRATACION"
        const val TYPE_SEDENTARISMO = "SEDENTARISMO"
        const val TYPE_STOCK_BAJO = "STOCK_BAJO"
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        acquireWakeLock()

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_HIDRATACION
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Usuario"
        val minutesInactive = intent.getIntExtra(EXTRA_MINUTES_INACTIVE, 0)
        val stockMessage = intent.getStringExtra(EXTRA_STOCK_MESSAGE).orEmpty()

        setContent {
            MaterialTheme {
                when (type) {
                    TYPE_HIDRATACION -> HidratacionAlertScreen(
                        patientName = patientName,
                        onAccept = { finish() }
                    )
                    TYPE_SEDENTARISMO -> SedentarismoAlertScreen(
                        patientName = patientName,
                        minutesInactive = minutesInactive,
                        onAccept = { finish() }
                    )
                    TYPE_STOCK_BAJO -> StockBajoAlertScreen(
                        message = stockMessage,
                        onAccept = {
                            startActivity(
                                android.content.Intent(this@ReminderAlertActivity, MainActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    putExtra(com.carlos.controlmedicamentos.notifications.NotificacionHelper.EXTRA_OPEN_LISTA_INSUMOS, true)
                                }
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "ControlMedicamentos::ReminderAlertWakeLock"
            )
            wakeLock?.acquire(10_000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun HidratacionAlertScreen(
    patientName: String,
    onAccept: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "water_pulse")
    val iconScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(iconScale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCA7",
                        fontSize = 80.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "HORA DE HIDRATARSE",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = patientName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Llevas un rato sin tomar agua.\nRecuerda tu meta diaria de hidratacion.",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "ACEPTAR",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D47A1)
                )
            }
        }
    }
}

@Composable
private fun StockBajoAlertScreen(
    message: String,
    onAccept: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "stock_pulse")
    val iconScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stockIconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4A2500), Color(0xFFB85C00), Color(0xFF4A2500))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Stock bajo",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier
                        .size(90.dp)
                        .scale(iconScale)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ALERTA DE STOCK BAJO",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = message,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                Text(
                    text = "VER MEDICAMENTOS\nEN USO",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4A2500),
                    textAlign = TextAlign.Center,
                    lineHeight = 29.sp
                )
            }
        }
    }
}

@Composable
private fun SedentarismoAlertScreen(
    patientName: String,
    minutesInactive: Int,
    onAccept: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "sed_blink")
    val bgAlpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )
    val btnScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4A0000), Color(0xFFB71C1C).copy(alpha = bgAlpha), Color(0xFF4A0000))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Alerta sedentarismo",
                    tint = Color.Yellow,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "ALERTA DE INACTIVIDAD",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = patientName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Llevas $minutesInactive minutos sin moverte.",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Es un buen momento para levantarte,\nestirarte o dar una caminata corta.",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .scale(btnScale)
            ) {
                Text(
                    text = "ACEPTAR",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
