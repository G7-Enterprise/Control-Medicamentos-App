package com.carlos.controlmedicamentos

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import com.carlos.controlmedicamentos.EXTRA_META_MINUTOS
import com.carlos.controlmedicamentos.EXTRA_ORIGEN
import com.carlos.controlmedicamentos.EXTRA_PACIENTE_ID
import com.carlos.controlmedicamentos.ORIGEN_SEDENTARISMO
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.notifications.ActivityRecognitionReceiver
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.notifications.SedentarismoScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper.StockOrderItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.RegistroHidratacion
import com.carlos.controlmedicamentos.notifications.HidratacionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_PATIENT_NAME = "EXTRA_PATIENT_NAME"
        const val EXTRA_PATIENT_ID = "EXTRA_PATIENT_ID"
        const val EXTRA_MINUTES_INACTIVE = "EXTRA_MINUTES_INACTIVE"
        const val EXTRA_META_MINUTOS = "EXTRA_META_MINUTOS"
        const val EXTRA_STOCK_MESSAGE = "EXTRA_STOCK_MESSAGE"
        const val EXTRA_STOCK_ITEMS_JSON = "EXTRA_STOCK_ITEMS_JSON"
        const val EXTRA_STOCK_WHATSAPP_PHONE = "EXTRA_STOCK_WHATSAPP_PHONE"
        const val EXTRA_STOCK_RESTOCK_SOURCE = "EXTRA_STOCK_RESTOCK_SOURCE"
        const val EXTRA_SOUND_ENABLED = "EXTRA_SOUND_ENABLED"
        const val EXTRA_TITULO_ALERTA = "EXTRA_TITULO_ALERTA"
        const val EXTRA_MENSAJE_ALERTA = "EXTRA_MENSAJE_ALERTA"
        const val TYPE_HIDRATACION = "HIDRATACION"
        const val TYPE_SEDENTARISMO = "SEDENTARISMO"
        const val TYPE_STOCK_BAJO = "STOCK_BAJO"
        const val TYPE_TOMAS_PENDIENTES = "TOMAS_PENDIENTES"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private val soundHandler = Handler(Looper.getMainLooper())
    private val stopSoundRunnable = Runnable { releaseMediaPlayer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        acquireWakeLock()

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_HIDRATACION
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Usuario"
        val patientId = intent.getIntExtra(EXTRA_PATIENT_ID, 0)
        val minutesInactive = intent.getIntExtra(EXTRA_MINUTES_INACTIVE, 0)
        val metaMinutos = intent.getIntExtra(EXTRA_META_MINUTOS, 5)
        val soundEnabled = when (type) {
            TYPE_HIDRATACION -> intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
            TYPE_SEDENTARISMO -> SedentarismoScheduler.loadSoundEnabled(this)
            else -> true
        }
        val tituloAlerta = intent.getStringExtra(EXTRA_TITULO_ALERTA).orEmpty()
        val mensajeAlerta = intent.getStringExtra(EXTRA_MENSAJE_ALERTA).orEmpty()
        val stockMessage = intent.getStringExtra(EXTRA_STOCK_MESSAGE).orEmpty()
        val stockItemsJson = intent.getStringExtra(EXTRA_STOCK_ITEMS_JSON)
        val stockWhatsappPhone = intent.getStringExtra(EXTRA_STOCK_WHATSAPP_PHONE).orEmpty()
        val stockRestockSource = intent.getStringExtra(EXTRA_STOCK_RESTOCK_SOURCE) ?: RestockSource.WHATSAPP_NUMBER
        val stockItems: List<StockOrderItem> = try {
            if (stockItemsJson != null) {
                val type = object : TypeToken<List<StockOrderItem>>() {}.type
                Gson().fromJson(stockItemsJson, type) ?: emptyList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        setContent {
            MaterialTheme {
                when (type) {
                    TYPE_HIDRATACION -> HidratacionAlertScreen(
                        patientName = patientName,
                        onRegisterIntake = { cantidadMl, tipoBebida ->
                            registrarTomaHidratacion(patientId, patientName, cantidadMl, tipoBebida)
                        },
                        onAccept = { finish() }
                    )
                    TYPE_SEDENTARISMO -> SedentarismoAlertScreen(
                        patientName = patientName,
                        patientId = patientId,
                        minutesInactive = minutesInactive,
                        metaMinutos = metaMinutos,
                        titulo = tituloAlerta,
                        mensaje = mensajeAlerta,
                        onStartActivity = {
                            releaseMediaPlayer()
                            ActivityRecognitionReceiver.iniciarMonitoreoDespuesAlerta(this@ReminderAlertActivity, patientId, metaMinutos)
                            finish()
                        },
                        onAccept = { finish() }
                    )
                    TYPE_TOMAS_PENDIENTES -> InfoAlertScreen(
                        title = tituloAlerta.ifBlank { "Tomas pendientes" },
                        message = mensajeAlerta,
                        onAccept = {
                            startActivity(
                                Intent(this@ReminderAlertActivity, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    putExtra(NotificacionHelper.EXTRA_LAUNCH_CRITICAL_ALERT, true)
                                }
                            )
                            finish()
                        }
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
                        },
                        onOrderByWhatsapp = if (stockItems.isNotEmpty() && stockRestockSource != RestockSource.INSS) {
                            {
                                NotificacionHelper.abrirPedidoWhatsappAgrupado(
                                    this@ReminderAlertActivity,
                                    stockItems,
                                    stockWhatsappPhone,
                                    stockRestockSource
                                )
                                finish()
                            }
                        } else null
                    )
                }
            }
        }

        when (type) {
            TYPE_HIDRATACION -> if (soundEnabled) playAlertSound(R.raw.water_sound)
            TYPE_SEDENTARISMO -> if (soundEnabled) playAlertSound(R.raw.heartbeat_sound)
            else -> { /* silence */ }
        }
    }

    private fun registrarTomaHidratacion(
        patientId: Int,
        patientName: String,
        cantidadMl: Int,
        tipoBebida: String
    ) {
        if (patientId <= 0 || cantidadMl <= 0) return
        releaseMediaPlayer()
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@ReminderAlertActivity).hidratacionDao().registrarToma(
                RegistroHidratacion(
                    patientId = patientId,
                    cantidadMl = cantidadMl,
                    tipoBebida = tipoBebida
                )
            )
            HidratacionScheduler(this@ReminderAlertActivity).programar(patientId, patientName)
            runOnUiThread { finish() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        releaseMediaPlayer()
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

    private fun playAlertSound(rawResId: Int) {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(this, rawResId)?.apply {
                isLooping = true
                start()
            }
            soundHandler.postDelayed(stopSoundRunnable, 15_000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        soundHandler.removeCallbacks(stopSoundRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun HidratacionAlertScreen(
    patientName: String,
    onRegisterIntake: (Int, String) -> Unit,
    onAccept: () -> Unit
) {
    var tipoBebida by remember { mutableStateOf("Agua") }
    var cantidadPersonalizada by remember { mutableStateOf("") }
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

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Registrar toma rápida",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Agua", "Electrolitos", "Té / Infusiones").forEach { tipo ->
                        Button(
                            onClick = { tipoBebida = tipo },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tipoBebida == tipo) Color(0xFF00B0FF) else Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tipo, fontSize = 11.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(150, 250, 500, 1000).forEach { cantidad ->
                        Button(
                            onClick = { onRegisterIntake(cantidad, tipoBebida) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${cantidad} ml", fontSize = 12.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cantidadPersonalizada,
                        onValueChange = { cantidadPersonalizada = it.filter(Char::isDigit) },
                        label = { Text("ml personalizado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { cantidadPersonalizada.toIntOrNull()?.takeIf { it > 0 }?.let { onRegisterIntake(it, tipoBebida) } },
                        enabled = cantidadPersonalizada.toIntOrNull()?.let { it > 0 } == true,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                    ) {
                        Text("Añadir")
                    }
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "ACEPTAR SIN REGISTRAR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D47A1)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoAlertScreen(
    title: String,
    message: String,
    onAccept: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF1976D2), Color(0xFF1A237E))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💊", fontSize = 82.sp)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = message,
                color = Color.White,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                Text(
                    text = "ACEPTAR",
                    fontSize = 23.sp,
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
    onAccept: () -> Unit,
    onOrderByWhatsapp: (() -> Unit)? = null
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

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
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

                onOrderByWhatsapp?.let { onClick ->
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text(
                            text = "PEDIR POR\nWHATSAPP",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 29.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SedentarismoAlertScreen(
    patientName: String,
    patientId: Int,
    minutesInactive: Int,
    metaMinutos: Int,
    titulo: String = "",
    mensaje: String = "",
    onStartActivity: () -> Unit,
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
    val esPersonalizado = mensaje.isNotBlank()
    val tituloMostrado = titulo.takeIf { it.isNotBlank() } ?: "ALERTA DE INACTIVIDAD"
    val mensajeMostrado = mensaje.takeIf { it.isNotBlank() }
        ?: "Llevas $minutesInactive minutos sin moverte.\nDebes caminar al menos $metaMinutos minutos para reactivar la circulación."

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
                    text = tituloMostrado,
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
                if (!esPersonalizado) {
                    Text(
                        text = "$minutesInactive minutos sin moverte",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = mensajeMostrado,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Para registrar su actividad, lleve el teléfono con usted",
                    color = Color(0xFF81D4FA),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStartActivity,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .scale(btnScale)
                ) {
                    Text(
                        text = "ENTERADO",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D47A1)
                    )
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Text(
                        text = "ACEPTAR",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
