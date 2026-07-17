package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carlos.controlmedicamentos.ui.screens.AIChatScreen

@Composable
internal fun PanelConfiguracionIaPanel(
    mostrarPanelConfiguracionIa: Boolean,
    urlServicioIa: String,
    modeloServicioIa: String,
    onUrlServicioIaChange: (String) -> Unit,
    onModeloServicioIaChange: (String) -> Unit,
    onGuardarConfiguracionIa: () -> Unit,
    onCerrarPanelesSecundarios: () -> Unit
) {
    if (!mostrarPanelConfiguracionIa) return
    val context = LocalContext.current
    MetallicGreenHeaderCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Configuracion del asistente")
            Text("Para este entorno se usa la IP Wi\u2011Fi de la PC. En la app debes guardar la URL completa del API, por ejemplo http://192.168.40.162:11434/api/generate. En el navegador del telefono puedes probar solo la base http://192.168.40.162:11434 para verificar que Ollama esta activo.")
            Text("Si el telefono no conecta, confirma que ambos dispositivos estan en la misma Wi\u2011Fi, que OLLAMA_HOST esta en 0.0.0.0 y que el Firewall de Windows no bloquea el puerto 11434.")
            OutlinedTextField(
                value = urlServicioIa,
                onValueChange = onUrlServicioIaChange,
                label = { Text("URL del servicio") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = modeloServicioIa,
                onValueChange = onModeloServicioIaChange,
                label = { Text("Modelo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    onGuardarConfiguracionIa()
                    Toast.makeText(context, "Configuracion IA guardada", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar configuracion")
            }
            Button(
                onClick = onCerrarPanelesSecundarios,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al escritorio", color = Color.Black)
            }
        }
    }
}

@Composable
internal fun PanelAsistenteIaPanel(
    mostrarPanelAsistenteIa: Boolean,
    perfilActivoNombre: String?,
    onCerrarPanelesSecundarios: () -> Unit
) {
    if (!mostrarPanelAsistenteIa) return
    Dialog(
        onDismissRequest = onCerrarPanelesSecundarios,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
        ) {
            AIChatScreen(
                onNavigateBack = onCerrarPanelesSecundarios,
                perfilActivoNombre = perfilActivoNombre
            )
        }
    }
}
