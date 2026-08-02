package com.carlos.controlmedicamentos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Tiempo visible del banner antes de auto-ocultarse (2 minutos). */
private const val BANNER_AUTO_HIDE_MS = 120_000L

/**
 * Banner sutil y visible en el escritorio durante los últimos 30 días
 * del periodo de prueba. Muestra una cuenta regresiva de días restantes
 * y un enlace discreto para adquirir la licencia anual.
 */
@Composable
fun TrialCountdownBanner(
    endDate: Long,
    onAcquireLicense: () -> Unit,
    onActivateKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingDays = ((endDate - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L))
        .coerceAtLeast(0L)

    var mostrarBanner by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(BANNER_AUTO_HIDE_MS)
        mostrarBanner = false
    }

    AnimatedVisibility(
        visible = remainingDays in 1..30 && mostrarBanner,
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            onClick = onAcquireLicense,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Su licencia finaliza en $remainingDays días",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBF360C)
                    )
                    Text(
                        text = "Adquiera su licencia anual para continuar sin interrupciones.",
                        fontSize = 12.sp,
                        color = Color(0xFF6D4C41)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(
                        onClick = onAcquireLicense,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
                    ) {
                        Text("Adquirir", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onActivateKey,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6D4C41))
                    ) {
                        Text("Activar llave", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
