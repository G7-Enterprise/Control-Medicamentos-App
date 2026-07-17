package com.carlos.controlmedicamentos

import android.Manifest
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings

@Composable
internal fun ConfiguracionAlertasPanel(
    mostrarPanelConfiguracionAlertas: Boolean,
    intervaloReintentoSeleccionado: Int,
    numeroIntentosCriticosSeleccionado: Int,
    expandedReintentoCritico: Boolean,
    expandedIntentosCriticos: Boolean,
    alarmaSonidoUri: String,
    alarmaSonidoNombre: String,
    opcionesReintentoCritico: List<Int>,
    opcionesIntentosCriticos: List<Int>,
    tienePermisoNotificaciones: Boolean,
    tienePermisoAlarmaExacta: Boolean,
    tienePermisoPantallaCompleta: Boolean,
    tieneAccesoNoMolestar: Boolean,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>,
    fullScreenIntentPermissionLauncher: ActivityResultLauncher<Intent>,
    notificationPolicyAccessLauncher: ActivityResultLauncher<Intent>,
    ringtonePickerLauncher: ActivityResultLauncher<Intent>,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onIntervaloReintentoSeleccionadoChange: (Int) -> Unit,
    onNumeroIntentosCriticosSeleccionadoChange: (Int) -> Unit,
    onExpandedReintentoCriticoChange: (Boolean) -> Unit,
    onExpandedIntentosCriticosChange: (Boolean) -> Unit,
    onAlarmaSonidoUriChange: (String) -> Unit,
    onAlarmaSonidoNombreChange: (String) -> Unit,
    onGuardarConfiguracionAlertasCriticas: () -> Unit,
    onCerrarPanelesSecundarios: () -> Unit
) {
    val context = LocalContext.current

    if (!mostrarPanelConfiguracionAlertas) return

    MetallicMedicationCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = 16,
        verticalSpacing = 8,
        expandVertically = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(panelInternoScrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Configuracion de alertas criticas",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text("Esta configuracion se aplica a todos los medicamentos y citas con alarma critica activa.")

            VademecumDropdown(
                label = "Intervalo de reintento",
                options = opcionesReintentoCritico.map { "$it min" },
                selectedValue = "$intervaloReintentoSeleccionado min",
                expanded = expandedReintentoCritico,
                onExpandedChange = { onExpandedReintentoCriticoChange(!expandedReintentoCritico) },
                onDismiss = { onExpandedReintentoCriticoChange(false) },
                onSelect = {
                    onIntervaloReintentoSeleccionadoChange(
                        it.substringBefore(' ').toIntOrNull()
                            ?.let(::normalizarReintentoCritico)
                            ?: CriticalAlertSettings.DEFAULT_RETRY_INTERVAL_MINUTES
                    )
                    onExpandedReintentoCriticoChange(false)
                }
            )

            VademecumDropdown(
                label = "Numero de repeticiones",
                options = opcionesIntentosCriticos.map(Int::toString),
                selectedValue = numeroIntentosCriticosSeleccionado.toString(),
                expanded = expandedIntentosCriticos,
                onExpandedChange = { onExpandedIntentosCriticosChange(!expandedIntentosCriticos) },
                onDismiss = { onExpandedIntentosCriticosChange(false) },
                onSelect = {
                    onNumeroIntentosCriticosSeleccionadoChange(
                        it.toIntOrNull()
                            ?.let(CriticalAlertSettings::normalizeMaxRetryCount)
                            ?: CriticalAlertSettings.DEFAULT_MAX_RETRY_COUNT
                    )
                    onExpandedIntentosCriticosChange(false)
                }
            )

            Text(
                if (numeroIntentosCriticosSeleccionado == 0) {
                    "Tras la alerta inicial no se programaran repeticiones automaticas."
                } else {
                    "Tras la alerta inicial, se repetira hasta $numeroIntentosCriticosSeleccionado veces cada $intervaloReintentoSeleccionado minutos si no se confirma ni se pospone."
                }
            )

            Text("Sonido critico")
            Text(alarmaSonidoNombre)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        ringtonePickerLauncher.launch(buildRingtonePickerIntent(alarmaSonidoUri))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Elegir sonido")
                }
                Button(
                    onClick = {
                        onAlarmaSonidoUriChange("")
                        onAlarmaSonidoNombreChange("Alarma predeterminada")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Predeterminada")
                }
            }

            Text("Permisos del sistema")
            if (!tienePermisoNotificaciones) {
                Text("Activa las notificaciones para que las alertas puedan mostrarse.")
                Button(
                    onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir notificaciones")
                }
            }
            if (!tienePermisoAlarmaExacta) {
                Text("Permite alarmas exactas para disparar el recordatorio a la hora prevista.")
                Button(
                    onClick = { exactAlarmPermissionLauncher.launch(buildExactAlarmPermissionIntent(context)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir alarmas exactas")
                }
            }
            if (!tienePermisoPantallaCompleta) {
                Text("Permite las alertas de pantalla completa para que la alarma critica pueda abrirse incluso con el dispositivo bloqueado.")
                Button(
                    onClick = { fullScreenIntentPermissionLauncher.launch(buildFullScreenIntentPermissionIntent(context)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir pantalla completa")
                }
            }
            if (!tieneAccesoNoMolestar) {
                Text("Concede acceso a No molestar para que la alerta critica intente sobresalir sobre ese filtro.")
                Button(
                    onClick = { notificationPolicyAccessLauncher.launch(buildNotificationPolicyAccessIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir alertas criticas")
                }
            }
            if (tienePermisoNotificaciones && tienePermisoAlarmaExacta && tienePermisoPantallaCompleta && tieneAccesoNoMolestar) {
                Text("Todos los permisos criticos ya estan concedidos.")
            }

            Button(
                onClick = {
                    onGuardarConfiguracionAlertasCriticas()
                    Toast.makeText(context, "Configuracion de alertas actualizada", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar configuracion")
            }
            Button(
                onClick = { onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al escritorio", color = Color.Black)
            }
        }
    }
}
