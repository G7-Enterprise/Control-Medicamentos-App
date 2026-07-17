package com.carlos.controlmedicamentos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.carlos.controlmedicamentos.StatisticsPdfExporter
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_NOT_TAKEN
import com.carlos.controlmedicamentos.data.local.MedicalReport
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val SpanishLocale = Locale.forLanguageTag("es-ES")

data class MonthOption(val label: String, val start: Long, val end: Long)

@Composable
fun StatisticsScreen(
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val pacienteActivoState = database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)
    val pacienteActivo by pacienteActivoState
    val paciente = pacienteActivo
    val esMujer = paciente?.sexo?.equals("Femenino", ignoreCase = true) == true || paciente?.sexo?.equals("Mujer", ignoreCase = true) == true
    val medicationIntakes by remember(paciente?.id) {
        if (paciente == null) flowOf(emptyList())
        else database.medicationIntakeDao().observarPorPaciente(paciente.id)
    }.collectAsState(initial = emptyList())
    val patientMedications by remember(paciente?.id) {
        if (paciente == null) flowOf(emptyList())
        else database.medicationDao().observarTodosPorPaciente(paciente.id)
    }.collectAsState(initial = emptyList())
    val medicationUsageByInsumo = remember(medicationIntakes, patientMedications) {
        val activeMedIds = patientMedications.map { it.id }.toSet()
        val fromActive = patientMedications.map { medication ->
            medication.nombre to medicationIntakes.count { it.medicationId == medication.id }
        }
        val fromOrphans = medicationIntakes
            .filter { it.medicationId !in activeMedIds && it.medicationName.isNotBlank() }
            .groupBy { it.medicationName }
            .map { (name, list) -> name to list.size }
        (fromActive + fromOrphans).distinctBy { it.first }
    }
    val metodoAnticonceptivoActivo by remember(paciente?.id) {
        if (paciente == null) flowOf(null)
        else database.metodoAnticonceptivoDao().observarActivo(paciente.id)
    }.collectAsState(initial = null)
    val metodoActivo = metodoAnticonceptivoActivo
    val anticonceptivoIntakes by remember(metodoActivo?.id) {
        if (metodoActivo == null) flowOf(emptyList())
        else database.anticonceptivoIntakeDao().observarPorMetodo(metodoActivo.id)
    }.collectAsState(initial = emptyList())

    val ahora = remember { System.currentTimeMillis() }
    val monthOptions = remember(ahora) {
        (0..5).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = ahora
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -offset)
            }
            val start = cal.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis
            MonthOption(SimpleDateFormat("MMM yyyy", SpanishLocale).format(Date(start)), start, end)
        }
    }
    var selectedMonthIndex by remember { mutableStateOf(0) }
    val selectedMonth = monthOptions.getOrElse(selectedMonthIndex) { monthOptions.first() }
    val takenMedicationIntakes = medicationIntakes.filter { it.status != MEDICATION_INTAKE_STATUS_NOT_TAKEN }
    val monthlyIntakes = takenMedicationIntakes.filter {
        it.scheduledAt in selectedMonth.start..selectedMonth.end || it.acceptedAt in selectedMonth.start..selectedMonth.end
    }
    val monthlyUsageByInsumo = remember(monthlyIntakes, patientMedications) {
        val activeMedIds = patientMedications.map { it.id }.toSet()
        val fromActive = patientMedications.map { medication ->
            medication.nombre to monthlyIntakes.count { it.medicationId == medication.id }
        }
        val fromOrphans = monthlyIntakes
            .filter { it.medicationId !in activeMedIds && it.medicationName.isNotBlank() }
            .groupBy { it.medicationName }
            .map { (name, list) -> name to list.size }
        (fromActive + fromOrphans).distinctBy { it.first }.sortedByDescending { it.second }
    }
    val tomasMes = monthlyIntakes.size
    val mesSeleccionadoTexto = selectedMonth.label
    var exportandoPdf by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val inicioHoy = remember { inicioDelDia(ahora) }
    val inicioSemana = remember { inicioDeLaSemana(ahora) }
    val tomasHoy = takenMedicationIntakes.count {
        it.scheduledAt in inicioHoy..ahora || it.acceptedAt in inicioHoy..ahora
    }
    val tomasSemana = takenMedicationIntakes.count {
        it.scheduledAt in inicioSemana..ahora || it.acceptedAt in inicioSemana..ahora
    }
    val ultimaToma = takenMedicationIntakes.maxByOrNull { maxOf(it.scheduledAt, it.acceptedAt) }
    val ultimaTomaTexto = ultimaToma?.let {
        val fecha = if (it.acceptedAt > 0) it.acceptedAt else it.scheduledAt
        "${fecha.formatDate()} ${fecha.formatHour()}"
    } ?: "No hay registros de toma"
    val pacienteTexto = pacienteActivo?.let { "${it.nombre} ${it.apellidos}" } ?: "Sin paciente activo"
    val metodoActivoTexto = metodoActivo?.tipo ?: "Sin método anticonceptivo activo"
    val tomasAnticonceptivo = anticonceptivoIntakes.size

    Dialog(
        onDismissRequest = onVolver,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090B1A).copy(alpha = 0.94f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, start = 0.dp, end = 0.dp, bottom = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2150)),
                shape = RoundedCornerShape(24.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estadísticas de uso",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        IconButton(onClick = onVolver) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    StatLine(label = "Usuario activo", value = pacienteTexto)
                    StatLine(label = "Tomas registradas", value = medicationIntakes.size.toString())
                    StatLine(label = "Tomas hoy", value = tomasHoy.toString())
                    StatLine(label = "Tomas esta semana", value = tomasSemana.toString())
                    StatLine(label = "Última toma", value = ultimaTomaTexto)
                    if (esMujer) {
                        StatLine(label = "Método anticonceptivo activo", value = metodoActivoTexto)
                        StatLine(label = "Tomas anticonceptivo", value = tomasAnticonceptivo.toString())
                    }

                    Text(
                        text = "Mes seleccionado",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthOptions.forEachIndexed { index, option ->
                            val selected = index == selectedMonthIndex
                            Button(
                                onClick = { selectedMonthIndex = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF7B1FA2) else Color(0xFF2D1B69),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(option.label, fontSize = 12.sp)
                            }
                        }
                    }
                    StatLine(label = "Mes", value = mesSeleccionadoTexto)
                    StatLine(label = "Tomas en el mes", value = tomasMes.toString())
                    StatLine(label = "Medicamentos consumidos", value = monthlyUsageByInsumo.count { it.second > 0 }.toString())

                    Button(
                        onClick = {
                            if (paciente == null) return@Button
                            coroutineScope.launch {
                                exportandoPdf = true
                                val fileUri = StatisticsPdfExporter.exportMonthlyStatisticsPdf(
                                    context = context,
                                    patientName = pacienteTexto,
                                    monthLabel = mesSeleccionadoTexto,
                                    medicationUsage = monthlyUsageByInsumo,
                                    totalTomas = tomasMes
                                )
                                if (fileUri != null) {
                                    val report = MedicalReport(
                                        patientId = paciente.id,
                                        titulo = "Estadísticas mensuales $mesSeleccionadoTexto",
                                        descripcion = buildString {
                                            appendLine("Mes: $mesSeleccionadoTexto")
                                            appendLine("Tomas totales: $tomasMes")
                                            appendLine("Medicamentos consumidos: ${monthlyUsageByInsumo.count { it.second > 0 }}")
                                            monthlyUsageByInsumo.forEach { (name, count) ->
                                                appendLine("• $name: $count tomas")
                                            }
                                        },
                                        adjuntos = fileUri,
                                        analisisIa = "",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    database.medicalReportDao().guardar(report)
                                    Toast.makeText(context, "PDF guardado y registro almacenado en base de datos", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_LONG).show()
                                }
                                exportandoPdf = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !exportandoPdf
                    ) {
                        if (exportandoPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando PDF...", fontSize = 14.sp)
                        } else {
                            Text("Exportar estadísticas a PDF", fontSize = 14.sp)
                        }
                    }

                    Text(
                        text = "Consumo por medicamento",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (medicationUsageByInsumo.isEmpty()) {
                        Text(
                            text = "No hay medicamentos registrados",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            medicationUsageByInsumo.forEach { (name, count) ->
                                val countText = if (count == 1) "1 toma" else "$count tomas"
                                StatLine(label = name, value = countText)
                            }
                        }
                    }

                    Text(
                        text = "Actualizado: ${ahora.formatDate()} ${ahora.formatHour()}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun inicioDelDia(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun inicioDeLaSemana(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.formatDate(): String {
    return SimpleDateFormat("dd MMM yyyy", SpanishLocale).format(Date(this))
}

private fun Long.formatHour(): String {
    return SimpleDateFormat("HH:mm", SpanishLocale).format(Date(this))
}



