package com.carlos.controlmedicamentos

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CONTACTO_TELEFONO = "+50583363532"
private val CONTACTO_WHATSAPP = "+50583363532"
private val CONTACTO_EMAIL    = "carlosg7@gmail.com"

@Composable
fun TrialExpiredScreen() {
    val context = LocalContext.current

    // Bloquea el botón atrás — la pantalla no se puede cerrar
    BackHandler(enabled = true) { }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0018), Color(0xFF1A0033), Color(0xFF0A0018))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                text = "\uD83D\uDD12",
                fontSize = 72.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Periodo de prueba finalizado",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "El periodo de prueba de 6 meses ha concluido.\n\nPara seguir usando Control de Medicamentos, abona \$39.99. Al hacerlo recibirás una actualización con 1 año de uso.\n\nTus datos y registros se conservan intactos y se restaurarán al instalar la actualización de pago.",
                fontSize = 15.sp,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Contactar al desarrollador",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBB86FC)
                    )

                    // Botón WhatsApp
                    Button(
                        onClick = {
                            val numero = CONTACTO_WHATSAPP.replace("+", "").replace(" ", "")
                            val uri = Uri.parse("https://wa.me/$numero?text=Hola,%20el%20periodo%20de%20prueba%20terminó.%20Quiero%20abonar%20los%20\$39.99%20para%20obtener%201%20año%20de%20uso%20de%20Control%20de%20Medicamentos.")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("WhatsApp: $CONTACTO_WHATSAPP", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Botón llamar
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("tel:$CONTACTO_TELEFONO")
                            context.startActivity(Intent(Intent.ACTION_DIAL, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF82B1FF)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Llamar: $CONTACTO_TELEFONO")
                    }

                    // Botón correo
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("mailto:$CONTACTO_EMAIL?subject=Licencia%20Control%20de%20Medicamentos")
                            context.startActivity(Intent(Intent.ACTION_SENDTO, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A65)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(CONTACTO_EMAIL)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Sus datos y registros están intactos y se restaurarán al activar la licencia.",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    }
}
