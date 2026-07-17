package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MenuHamburguesaFlotante(
    mostrarEscritorio: Boolean,
    mostrarMenuHamburguesaState: MutableState<Boolean>,
    pacienteActivo: PatientProfile?,
    alarmaSonidoUriState: MutableState<String>,
    alarmaSonidoNombreState: MutableState<String>,
    fallAlertPanelState: MutableState<Boolean>,
    intervaloReintentoSeleccionadoState: MutableState<Int>,
    numeroIntentosCriticosSeleccionadoState: MutableState<Int>,
    onCerrarPanelesSecundarios: () -> Unit,
    onAbrirNuevaFichaPaciente: () -> Unit,
    onResetForm: () -> Unit,
    onMostrarFormulario: (Boolean) -> Unit,
    onMostrarPanelPacientes: (Boolean) -> Unit,
    onMostrarPanelProfesionales: (Boolean) -> Unit,
    onMostrarPanelInformes: (Boolean) -> Unit,
    onMostrarListaInsumos: (Boolean) -> Unit,
    onMostrarDialogoMedia: (Boolean) -> Unit,
    onMostrarFormularioInformeChange: (Boolean) -> Unit,
    onMostrarPanelSignosVitales: () -> Unit,
    onMostrarPanelConfiguracionAlertas: (Boolean) -> Unit,
    onMostrarPanelAsistenteIa: (Boolean) -> Unit,
    onMostrarPanelPodometro: (Boolean) -> Unit,
    onMostrarPanelPedidos: (Boolean) -> Unit,
    onMostrarPanelBackups: (Boolean) -> Unit,
    onMostrarPanelHidratacion: (Boolean) -> Unit,
    onMostrarPanelSedentarismo: (Boolean) -> Unit,
    onMostrarPanelDentista: (Boolean) -> Unit,
    onMostrarPanelReporteClinico: (Boolean) -> Unit,
    onMostrarPanelEstadisticas: (Boolean) -> Unit,
    onMostrarPanelVerificadorTomas: (Boolean) -> Unit,
    onMostrarPanelDiario: (Boolean) -> Unit,
    onMostrarPanelCicloMenstrual: (Boolean) -> Unit,
    onResolveAlarmSoundLabel: (android.content.Context, String) -> String
) {
    if (mostrarEscritorio) return

    val context = LocalContext.current
    var mostrarMenuHamburguesa by mostrarMenuHamburguesaState
    var alarmaSonidoUri by alarmaSonidoUriState
    var alarmaSonidoNombre by alarmaSonidoNombreState
    var intervaloReintentoSeleccionado by intervaloReintentoSeleccionadoState
    var numeroIntentosCriticosSeleccionado by numeroIntentosCriticosSeleccionadoState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        contentAlignment = Alignment.TopEnd
    ) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(end = 4.dp, top = 4.dp)
    ) {
        IconButton(onClick = { mostrarMenuHamburguesa = true }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu principal",
                tint = Color.White
            )
        }
        val floatMenuShape = RoundedCornerShape(24.dp)
        val floatMenuItemColors = MenuDefaults.itemColors(
            textColor = Color(0xFFF3FBFF),
            leadingIconColor = Color(0xFFAEEBFF),
            disabledTextColor = Color(0x88F3FBFF),
            disabledLeadingIconColor = Color(0x66AEEBFF)
        )
        DropdownMenu(
            expanded = mostrarMenuHamburguesa,
            onDismissRequest = { mostrarMenuHamburguesa = false },
            scrollState = rememberScrollState(),
            shape = floatMenuShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, Color(0xFF84DEFF).copy(alpha = 0.42f)),
            modifier = Modifier
                .heightIn(max = 520.dp)
                .clip(floatMenuShape)
                .drawWithCache {
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF041B4F), Color(0xFF0B4FA2), Color(0xFF29B7FF), Color(0xFF0B4A98), Color(0xFF041334))
                    )
                    val sheenGradient = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color(0xFF9AEAFF).copy(alpha = 0.20f), Color.Transparent)
                    )
                    val overlayGradient = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.24f))
                    )
                    val lineColor = Color.White.copy(alpha = 0.07f)
                    val lineSpacing = 6.dp.toPx()
                    val stroke = 0.7.dp.toPx()
                    onDrawBehind {
                        drawRect(baseGradient)
                        drawRect(sheenGradient)
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
                            y += lineSpacing
                        }
                        drawRect(overlayGradient)
                    }
                }
        ) {
            DropdownMenuItem(
                text = { Text("Nuevo medicamento") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    if (pacienteActivo == null) { onAbrirNuevaFichaPaciente(); Toast.makeText(context, "Crea o selecciona primero un paciente", Toast.LENGTH_SHORT).show(); return@DropdownMenuItem }
                    onCerrarPanelesSecundarios(); onResetForm(); onMostrarFormulario(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Usuarios") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelPacientes(true) }
            )
            DropdownMenuItem(
                text = { Text("Médicos y especialistas") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    if (pacienteActivo == null) { onAbrirNuevaFichaPaciente(); Toast.makeText(context, "Crea o selecciona primero un paciente", Toast.LENGTH_SHORT).show(); return@DropdownMenuItem }
                    onCerrarPanelesSecundarios(); onMostrarPanelProfesionales(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Vacunas") },
                leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios()
                    context.startActivity(android.content.Intent(context, com.carlos.controlmedicamentos.NuevaVacunaActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Agenda") },
                leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios()
                    context.startActivity(android.content.Intent(context, com.carlos.controlmedicamentos.CitasMedicasActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Documentos") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    if (pacienteActivo == null) { onAbrirNuevaFichaPaciente(); Toast.makeText(context, "Crea o selecciona primero un paciente", Toast.LENGTH_SHORT).show(); return@DropdownMenuItem }
                    onCerrarPanelesSecundarios(); onMostrarPanelInformes(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Medicamentos en uso") },
                leadingIcon = { Icon(Icons.Filled.List, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    if (pacienteActivo == null) { onAbrirNuevaFichaPaciente(); Toast.makeText(context, "Crea o selecciona primero un paciente", Toast.LENGTH_SHORT).show(); return@DropdownMenuItem }
                    onCerrarPanelesSecundarios(); onMostrarListaInsumos(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Galería") },
                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    onCerrarPanelesSecundarios()
                    onMostrarPanelInformes(false)
                    onMostrarFormularioInformeChange(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Métricas Diarias") },
                leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelSignosVitales() }
            )
            DropdownMenuItem(
                text = { Text("Recordatorios") },
                leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios()
                    val currentConfig = CriticalAlertSettings.load(context)
                    intervaloReintentoSeleccionado = currentConfig.retryIntervalMinutes
                    numeroIntentosCriticosSeleccionado = currentConfig.maxRetryCount
                    alarmaSonidoUri = currentConfig.soundUri
                    alarmaSonidoNombre = onResolveAlarmSoundLabel(context, currentConfig.soundUri)
                    onMostrarPanelConfiguracionAlertas(true)
                }
            )
            DropdownMenuItem(
                text = { Text("Asistente IA") },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelAsistenteIa(true) }
            )
            DropdownMenuItem(
                text = { Text("Actividad Física") },
                leadingIcon = { Icon(Icons.Filled.DirectionsRun, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelPodometro(true) }
            )
            DropdownMenuItem(
                text = { Text("Alerta de caídas") },
                leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = {
                    mostrarMenuHamburguesa = false
                    if (pacienteActivo == null) { onAbrirNuevaFichaPaciente(); Toast.makeText(context, "Crea o selecciona primero un perfil", Toast.LENGTH_SHORT).show(); return@DropdownMenuItem }
                    onCerrarPanelesSecundarios(); fallAlertPanelState.value = true
                }
            )
            DropdownMenuItem(
                text = { Text("Historial de Compras") },
                leadingIcon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelPedidos(true) }
            )
            DropdownMenuItem(
                text = { Text("Copias de seguridad") },
                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelBackups(true) }
            )
            DropdownMenuItem(
                text = { Text("Hidratación") },
                leadingIcon = { Icon(Icons.Filled.WaterDrop, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelHidratacion(true) }
            )
            DropdownMenuItem(
                text = { Text("Sedentarismo") },
                leadingIcon = { Icon(Icons.Filled.Watch, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelSedentarismo(true) }
            )
            DropdownMenuItem(
                text = { Text("Dentista") },
                leadingIcon = { Icon(Icons.Filled.MedicalServices, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelDentista(true) }
            )
            DropdownMenuItem(
                text = { Text("Exportar Resumen") },
                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelReporteClinico(true) }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .combinedClickable(
                        onClick = {
                            mostrarMenuHamburguesa = false
                            onCerrarPanelesSecundarios()
                            onMostrarPanelEstadisticas(true)
                        },
                        onLongClick = {
                            mostrarMenuHamburguesa = false
                            onCerrarPanelesSecundarios()
                            onMostrarPanelVerificadorTomas(true)
                        }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = Color(0xFFAEEBFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Estadísticas de uso",
                        color = Color(0xFFF3FBFF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("Diario Personal") },
                leadingIcon = { Icon(Icons.Filled.Note, contentDescription = null) },
                colors = floatMenuItemColors,
                onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelDiario(true) }
            )
            if (pacienteActivo?.sexo == "Mujer") {
                DropdownMenuItem(
                    text = { Text("Ciclo menstrual") },
                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    colors = floatMenuItemColors,
                    onClick = { mostrarMenuHamburguesa = false; onCerrarPanelesSecundarios(); onMostrarPanelCicloMenstrual(true) }
                )
            }
        }
    }
    } // outer Box
}
