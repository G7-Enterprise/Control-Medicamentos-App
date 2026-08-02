package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MetodoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.TipoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.AnticonceptivoIntake
import com.carlos.controlmedicamentos.notifications.AnticonceptivoScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val AcPurple = Color(0xFF6A1B9A)
private val AcPurpleLight = Color(0xFFF3E5F5)
private val AcPurpleMedium = Color(0xFFCE93D8)
private val AcCardBg = Color.White
private val AcTextDark = Color(0xFF212121)
private val AcTextMuted = Color(0xFF757575)
private val AcGreen = Color(0xFF4CAF50)
private val AcRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnticonceptivosScreen(
    pacienteId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val metodoActivo by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(null)
        else database.metodoAnticonceptivoDao().observarActivo(pacienteId)
    }.collectAsState(initial = null)

    val todosMetodos by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(emptyList())
        else database.metodoAnticonceptivoDao().observarPorPaciente(pacienteId)
    }.collectAsState(initial = emptyList())

    val tomasRecientes by remember(metodoActivo?.id) {
        if (metodoActivo == null) flowOf(emptyList())
        else database.anticonceptivoIntakeDao().observarPorMetodo(metodoActivo!!.id)
    }.collectAsState(initial = emptyList())

    var mostrarDialogoNuevo by remember { mutableStateOf(false) }
    var mostrarConfirmarDesactivar by remember { mutableStateOf(false) }
    var mostrarConfirmarEliminar by remember { mutableStateOf<MetodoAnticonceptivo?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(AcPurpleLight)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AcPurple, Color(0xFFAB47BC))))
                .padding(horizontal = 8.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text("💊  Anticonceptivos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { mostrarDialogoNuevo = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir método", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Método activo
            if (metodoActivo != null) {
                item {
                    val m = metodoActivo!!
                    val tipoEnum = try { TipoAnticonceptivo.fromDisplayName(m.tipo) } catch (_: Exception) { TipoAnticonceptivo.PILDORA_COMBINADA }
                    val diasActivo = ((System.currentTimeMillis() - m.fechaInicio) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AcCardBg),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Método activo", fontSize = 12.sp, color = AcTextMuted)
                                    Text(m.tipo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AcPurple)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Activo", color = AcGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Inicio", fontSize = 11.sp, color = AcTextMuted)
                                    Text(sdf.format(Date(m.fechaInicio)), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AcPurple)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Días de uso", fontSize = 11.sp, color = AcTextMuted)
                                    Text("$diasActivo días", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AcPurple)
                                }
                            }

                            if (tipoEnum.requiereAlarmaDiaria) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Hora de toma", fontSize = 11.sp, color = AcTextMuted)
                                        Text(m.horaToma, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AcPurple)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Frecuencia", fontSize = 11.sp, color = AcTextMuted)
                                        Text(tipoEnum.frecuenciaRecordatorio, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AcPurple)
                                    }
                                }
                            }

                            if (m.proximaCita != null && m.proximaCita > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEDE7F6))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📅  Próxima cita: ${sdf.format(Date(m.proximaCita))}", fontWeight = FontWeight.Bold, color = AcPurple, fontSize = 13.sp)
                                }
                            }

                            if (m.notas.isNotBlank()) {
                                Text("📝 ${m.notas}", fontSize = 12.sp, color = AcTextMuted)
                            }

                            // Botón marcar toma del día
                            val hoy = remember {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                cal.timeInMillis
                            }
                            val tomaHoy = tomasRecientes.any { it.scheduledAt in hoy..(hoy + 24L * 60 * 60 * 1000 - 1) }

                            if (tipoEnum.requiereAlarmaDiaria) {
                                Button(
                                    onClick = {
                                        if (!tomaHoy) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                database.anticonceptivoIntakeDao().guardar(
                                                    AnticonceptivoIntake(metodoId = m.id, scheduledAt = System.currentTimeMillis())
                                                )
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "✓ Toma registrada", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tomaHoy) AcGreen else AcPurple
                                    ),
                                    enabled = !tomaHoy
                                ) {
                                    if (tomaHoy) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Toma de hoy registrada", color = Color.White, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("💊  Marcar toma de hoy", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Desactivar método
                            TextButton(
                                onClick = { mostrarConfirmarDesactivar = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Desactivar método", fontSize = 13.sp, color = AcRed)
                            }
                        }
                    }
                }

                // Historial de tomas recientes
                if (tomasRecientes.isNotEmpty()) {
                    item {
                        Text("Historial de tomas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AcPurple)
                    }
                    val ultimasTomas = tomasRecientes.take(15)
                    items(ultimasTomas) { toma ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AcCardBg)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AcGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                sdf.format(Date(toma.scheduledAt)) + "  " + sdfHora.format(Date(toma.acceptedAt)),
                                fontSize = 13.sp,
                                color = AcTextDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Sin método activo
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AcCardBg),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("💊", fontSize = 64.sp)
                            Text("Anticonceptivos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AcPurple, textAlign = TextAlign.Center)
                            Text(
                                "No tienes un método anticonceptivo activo. Registra tu método para llevar el seguimiento de tomas y recordatorios.",
                                fontSize = 14.sp,
                                color = AcTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { mostrarDialogoNuevo = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AcPurple)
                            ) {
                                Text("💊  Registrar método anticonceptivo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Historial de métodos anteriores
            val metodosAnteriores = todosMetodos.filter { !it.activo }
            if (metodosAnteriores.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Historial de métodos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AcPurple)
                }
                items(metodosAnteriores) { m ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AcCardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.tipo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AcTextDark)
                                Text("Inicio: ${sdf.format(Date(m.fechaInicio))}", fontSize = 12.sp, color = AcTextMuted)
                                if (m.notas.isNotBlank()) {
                                    Text(m.notas, fontSize = 11.sp, color = AcTextMuted)
                                }
                            }
                            IconButton(onClick = { mostrarConfirmarEliminar = m }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AcRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    // Diálogo nuevo método
    if (mostrarDialogoNuevo) {
        NuevoMetodoDialog(
            onDismiss = { mostrarDialogoNuevo = false },
            onGuardar = { metodo ->
                coroutineScope.launch(Dispatchers.IO) {
                    // Desactivar métodos anteriores y cancelar sus alarmas programadas
                    val activos = database.metodoAnticonceptivoDao().obtenerActivos(pacienteId)
                    val schedulerAnterior = AnticonceptivoScheduler(context)
                    activos.forEach {
                        database.metodoAnticonceptivoDao().desactivar(it.id)
                        schedulerAnterior.cancelar(it.id)
                    }
                    val nuevoId = database.metodoAnticonceptivoDao().insertar(metodo.copy(patientId = pacienteId))
                    val nuevoMetodo = database.metodoAnticonceptivoDao().obtenerPorId(nuevoId.toInt())
                    nuevoMetodo?.let { AnticonceptivoScheduler(context).programarAlarma(it) }
                    withContext(Dispatchers.Main) {
                        mostrarDialogoNuevo = false
                        Toast.makeText(context, "Método registrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Diálogo confirmar desactivar
    if (mostrarConfirmarDesactivar && metodoActivo != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarDesactivar = false },
            title = { Text("¿Desactivar método?", color = AcPurple, fontWeight = FontWeight.Bold) },
            text = { Text("El método pasará al historial. Podrás registrar uno nuevo en cualquier momento.", color = AcTextDark) },
            confirmButton = {
                TextButton(onClick = {
                    val id = metodoActivo!!.id
                    mostrarConfirmarDesactivar = false
                    coroutineScope.launch(Dispatchers.IO) {
                        database.metodoAnticonceptivoDao().desactivar(id)
                        AnticonceptivoScheduler(context).cancelar(id)
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Método desactivado", Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Desactivar", color = AcRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarDesactivar = false }) { Text("Cancelar", color = AcTextMuted) }
            },
            containerColor = Color.White
        )
    }

    // Diálogo confirmar eliminar
    if (mostrarConfirmarEliminar != null) {
        val m = mostrarConfirmarEliminar!!
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = null },
            title = { Text("¿Eliminar ${m.tipo}?", color = AcRed, fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará este registro del historial permanentemente.", color = AcTextDark) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmarEliminar = null
                    coroutineScope.launch(Dispatchers.IO) {
                        database.metodoAnticonceptivoDao().eliminar(m)
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Eliminar", color = AcRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = null }) { Text("Cancelar", color = AcTextMuted) }
            },
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoMetodoDialog(
    onDismiss: () -> Unit,
    onGuardar: (MetodoAnticonceptivo) -> Unit
) {
    val context = LocalContext.current
    var tipoSeleccionado by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var fechaInicio by remember { mutableStateOf(System.currentTimeMillis()) }
    var horaToma by remember { mutableStateOf("08:00") }
    var notas by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val tipos = TipoAnticonceptivo.entries.map { it.displayName }

    val tipoEnum = remember(tipoSeleccionado) {
        try { TipoAnticonceptivo.fromDisplayName(tipoSeleccionado) } catch (_: Exception) { null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo método anticonceptivo", color = AcTextDark, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = it }) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de método") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = AcTextDark,
                            focusedTextColor = AcTextDark,
                            unfocusedLabelColor = AcTextMuted,
                            focusedLabelColor = AcTextDark
                        )
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        tipos.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = { tipoSeleccionado = tipo; expandedTipo = false }
                            )
                        }
                    }
                }

                // Fecha de inicio
                OutlinedTextField(
                    value = sdf.format(Date(fechaInicio)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de inicio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = fechaInicio }
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance()
                                c.set(y, m, d, 0, 0, 0)
                                fechaInicio = c.timeInMillis
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = AcTextDark,
                        disabledBorderColor = Color(0xFFBDBDBD),
                        disabledLabelColor = AcTextMuted
                    )
                )

                // Hora de toma (solo si requiere alarma diaria)
                if (tipoEnum?.requiereAlarmaDiaria == true) {
                    OutlinedTextField(
                        value = horaToma,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora de toma diaria") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val parts = horaToma.split(":")
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, hour, minute ->
                                    horaToma = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                                }, h, m, true).show()
                            },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = AcTextDark,
                            disabledBorderColor = Color(0xFFBDBDBD),
                            disabledLabelColor = AcTextMuted
                        )
                    )
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = AcTextDark,
                        focusedTextColor = AcTextDark,
                        unfocusedLabelColor = AcTextMuted,
                        focusedLabelColor = AcTextDark
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tipoSeleccionado.isBlank()) {
                    Toast.makeText(context, "Selecciona un tipo de método", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onGuardar(
                    MetodoAnticonceptivo(
                        patientId = 0,
                        tipo = tipoSeleccionado,
                        fechaInicio = fechaInicio,
                        horaToma = horaToma,
                        notas = notas
                    )
                )
            }) {
                Text("Guardar", color = AcPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = AcTextMuted) }
        },
        containerColor = Color.White
    )
}
