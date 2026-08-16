package com.carlos.controlmedicamentos.license

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carlos.controlmedicamentos.AppUpdateDownloader
import com.carlos.controlmedicamentos.AppUpdateRemoteConfig
import com.carlos.controlmedicamentos.BuildConfig
import com.carlos.controlmedicamentos.LicenseManager
import kotlinx.coroutines.launch

@Composable
fun LicenseInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseViewModel: LicenseViewModel = viewModel()
    val status by licenseViewModel.status.collectAsState()
    var showActivationDialog by remember { mutableStateOf(false) }
    var checkingForUpdate by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("license_cache", Context.MODE_PRIVATE)
    val cachedKey = prefs.getString("license_key", null)
    val appVersion = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        ?: "Desconocida"

    if (showActivationDialog) {
        ActivationDialog(
            viewModel = licenseViewModel,
            onDismiss = { showActivationDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mi Licencia",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (val current = status) {
                    is LicenseStatus.Valid -> {
                        val tipoTexto = when (current.type) {
                            LicenseType.TRIAL -> "Prueba gratuita"
                            LicenseType.ANNUAL -> "Licencia Anual"
                        }
                        val diasRestantes = ((current.endDate - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L))
                            .coerceAtLeast(0L)

                        LicenseInfoRow("Estado", "Activa")
                        LicenseInfoRow("Tipo", tipoTexto)
                        LicenseInfoRow("Tiempo restante", "$diasRestantes días")
                        LicenseInfoRow("Vence el", LicenseManager.formatDate(current.endDate))
                    }
                    is LicenseStatus.Expired -> {
                        LicenseInfoRow("Estado", "Expirada")
                        LicenseInfoRow("Expiró el", LicenseManager.formatDate(current.endDate))
                    }
                    is LicenseStatus.Error -> {
                        LicenseInfoRow("Estado", "No se pudo verificar")
                        Text(
                            text = current.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LicenseStatus.Loading -> {
                        LicenseInfoRow("Estado", "Verificando...")
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                LicenseInfoRow("Versión", appVersion)
                Button(
                    onClick = {
                        if (checkingForUpdate) return@Button
                        checkingForUpdate = true
                        coroutineScope.launch {
                            val result = runCatching {
                                AppUpdateRemoteConfig.fetchAndCheck(
                                    installedVersion = appVersion,
                                    installedVersionCode = BuildConfig.VERSION_CODE,
                                    forceFetch = true
                                )
                            }
                            checkingForUpdate = false
                            result.onSuccess { update ->
                                if (update.updateAvailable && update.config.updateUrl.isNotBlank()) {
                                    runCatching {
                                        AppUpdateDownloader.enqueue(context, update.config.updateUrl.trim())
                                    }.onSuccess {
                                        Toast.makeText(
                                            context,
                                            "Descargando la actualización...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "No se pudo iniciar la descarga.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else if (update.updateAvailable) {
                                    Toast.makeText(
                                        context,
                                        "Hay una actualización, pero no tiene APK configurado.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "La aplicación ya está actualizada.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "No se pudo consultar las actualizaciones.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !checkingForUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (checkingForUpdate) "Buscando..." else "Buscar actualizaciones")
                }

                if (!cachedKey.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    LicenseInfoRow("Llave", maskLicenseKey(cachedKey))
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(LicenseManager.URL_LICENCIA)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                ) {
                    Text("Comprar / Renovar")
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { showActivationDialog = true }) {
                    Text("Activar llave")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun LicenseInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(Modifier.height(6.dp))
}

private fun maskLicenseKey(key: String): String {
    val visibleLength = key.length / 2
    val visible = key.takeLast(visibleLength)
    return "•".repeat(key.length - visibleLength) + visible
}
