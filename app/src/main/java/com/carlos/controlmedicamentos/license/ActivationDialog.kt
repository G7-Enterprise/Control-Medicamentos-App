package com.carlos.controlmedicamentos.license

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ActivationDialog(
    viewModel: LicenseViewModel,
    onDismiss: () -> Unit
) {
    var licenseKey by remember { mutableStateOf("") }
    val activationState by viewModel.activationState.collectAsState()

    LaunchedEffect(activationState) {
        if (activationState is ActivationUiState.Success) {
            delay(1_200L)
            viewModel.resetActivationState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (activationState !is ActivationUiState.Validating) {
                viewModel.resetActivationState()
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Activar licencia",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pega la llave de licencia que recibiste tras tu compra en Lemon Squeezy.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = licenseKey,
                    onValueChange = { licenseKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Llave de licencia") },
                    placeholder = { Text("XXXXXXXX-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    enabled = activationState !is ActivationUiState.Validating,
                    shape = RoundedCornerShape(12.dp)
                )
                when (val state = activationState) {
                    is ActivationUiState.Error -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is ActivationUiState.Success -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Licencia activada correctamente.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.activate(licenseKey) },
                enabled = licenseKey.isNotBlank() &&
                    activationState !is ActivationUiState.Validating &&
                    activationState !is ActivationUiState.Success
            ) {
                if (activationState is ActivationUiState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Validar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetActivationState()
                    onDismiss()
                },
                enabled = activationState !is ActivationUiState.Validating
            ) {
                Text("Cancelar")
            }
        }
    )
}
