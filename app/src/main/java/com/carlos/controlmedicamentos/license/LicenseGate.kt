package com.carlos.controlmedicamentos.license

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState

@Composable
fun LicenseGate(
    viewModel: LicenseViewModel,
    lemonSqueezyUrl: String,
    content: @Composable () -> Unit
) {
    val status by viewModel.status.collectAsState()

    when (val current = status) {
        is LicenseStatus.Loading -> {
            LicenseLoadingScreen()
        }

        is LicenseStatus.Valid -> {
            LaunchedEffect(current) {
                // Aquí se puede registrar analytics o precargar datos si es necesario.
            }
            content()
        }

        is LicenseStatus.Expired -> {
            var showActivationDialog by remember { mutableStateOf(false) }

            LicenseExpiredScreen(
                lemonSqueezyUrl = lemonSqueezyUrl,
                onRetry = { viewModel.verifyLicense() },
                onActivateKey = { showActivationDialog = true }
            )

            if (showActivationDialog) {
                ActivationDialog(
                    viewModel = viewModel,
                    onDismiss = { showActivationDialog = false }
                )
            }
        }

        is LicenseStatus.Error -> {
            LicenseErrorScreen(
                message = current.message,
                canRetry = current.canRetry,
                onRetry = { viewModel.verifyLicense() }
            )
        }
    }
}

@Composable
private fun LicenseLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verificando licencia...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun LicenseExpiredScreen(
    lemonSqueezyUrl: String,
    onRetry: () -> Unit,
    onActivateKey: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF132238), Color(0xFF0A1628))
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
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = Color(0xFF1E3A5F),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Licencia Expirada",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEFEFEF),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Tu período de prueba ha finalizado o la licencia anual caducó. Adquiere una licencia anual para seguir usando Control de Medicamentos sin interrupciones.",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lemonSqueezyUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "Comprar Licencia Anual",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onActivateKey) {
                Text(
                    text = "Ya tengo una llave de licencia",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64B5F6)
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A5F),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Reverificar licencia",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Tus datos y registros permanecen intactos y se restaurarán al activar la licencia.",
                fontSize = 13.sp,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center
            )

        }
    }
}

@Composable
private fun LicenseErrorScreen(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF132238), Color(0xFF0A1628))
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
            Icon(
                imageVector = if (canRetry) Icons.Default.CloudOff else Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Error de verificación",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEFEFEF),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(32.dp))

            if (canRetry) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Reintentar",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
