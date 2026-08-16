package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AppUpdateDialog(
    update: AppUpdateCheck,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val forceUpdate = update.forceUpdate
    val updateUrl = update.config.updateUrl.trim()

    BackHandler(enabled = forceUpdate) { /* La actualizaciÃ³n obligatoria no se puede posponer. */ }

    Dialog(
        onDismissRequest = {
            if (!forceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !forceUpdate,
            dismissOnClickOutside = !forceUpdate
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nueva actualizaciÃ³n disponible",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "La versiÃ³n ${update.config.latestVersion} ya estÃ¡ disponible para Control de Medicamentos.",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (forceUpdate) {
                    Text(
                        text = "Esta actualizaciÃ³n es necesaria para continuar usando la aplicaciÃ³n.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = updateUrl.isNotBlank(),
                    onClick = {
                        runCatching { AppUpdateDownloader.enqueue(context, updateUrl) }
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    "Descargando la actualización...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    "No se pudo iniciar la descarga.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                ) {
                    Text("Actualizar ahora")
                }
                if (!forceUpdate) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismiss
                    ) {
                        Text("MÃ¡s tarde", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
