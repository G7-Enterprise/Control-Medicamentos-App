package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.SignosVitales
import java.util.Calendar

@Composable
internal fun ListadoSignosVitalesPanel(
    mostrarListadoSignosPanel: Boolean,
    signosVitales: List<SignosVitales>,
    filtroExportacionSignos: VitalSignsExportFilter,
    fechaInicioExportacionSignos: Long,
    fechaFinExportacionSignos: Long,
    expandedFiltroExportacionSignos: Boolean,
    registrosSignosSeleccionados: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    mesesExpandidosSignos: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    mostrarVistaPreviaSignosSeleccionados: Boolean,
    signosVitalesSeleccionados: List<SignosVitales>,
    exportandoSignosVitales: Boolean,
    onFiltroExportacionSignosChange: (VitalSignsExportFilter) -> Unit,
    onFechaInicioExportacionSignosChange: (Long) -> Unit,
    onFechaFinExportacionSignosChange: (Long) -> Unit,
    onExpandedFiltroExportacionSignosChange: (Boolean) -> Unit,
    onMostrarVistaPreviaSignosSeleccionadosChange: (Boolean) -> Unit,
    onExportandoSignosVitalesChange: (Boolean) -> Unit,
    onExportacionSignosPendienteChange: (VitalSignsExportRequest) -> Unit,
    onMostrarListadoSignosPanelChange: (Boolean) -> Unit,
    onMostrarPanelSignosVitalesChange: (Boolean) -> Unit,
    onLanzarExportVitalSigns: (String) -> Unit
) {
    if (!mostrarListadoSignosPanel) return

    val context = LocalContext.current

    BackHandler {
        onMostrarListadoSignosPanelChange(false)
        onMostrarPanelSignosVitalesChange(true)
        mesesExpandidosSignos.clear()
    }

    val listadoScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A0011), Color(0xFF3B0030),
                        Color(0xFF6D0050), Color(0xFF3B0030), Color(0xFF1A0011)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(listadoScrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registros guardados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = {
                onMostrarListadoSignosPanelChange(false)
                onMostrarPanelSignosVitalesChange(true)
                mesesExpandidosSignos.clear()
            }) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        Text("Exportar reporte a Word", color = Color.White, fontWeight = FontWeight.Medium)
        VademecumDropdown(
            label = "Filtro de fechas",
            options = VitalSignsExportFilter.entries.map { it.label },
            selectedValue = filtroExportacionSignos.label,
            expanded = expandedFiltroExportacionSignos,
            onExpandedChange = { onExpandedFiltroExportacionSignosChange(!expandedFiltroExportacionSignos) },
            onDismiss = { onExpandedFiltroExportacionSignosChange(false) },
            onSelect = { selected ->
                val nuevo = VitalSignsExportFilter.entries.firstOrNull { it.label == selected } ?: VitalSignsExportFilter.TODAY
                onFiltroExportacionSignosChange(nuevo)
                onExpandedFiltroExportacionSignosChange(false)
                if (nuevo == VitalSignsExportFilter.TODAY) {
                    onFechaInicioExportacionSignosChange(inicioDelDia(System.currentTimeMillis()))
                    onFechaFinExportacionSignosChange(finDelDia(System.currentTimeMillis()))
                }
            }
        )
        if (filtroExportacionSignos == VitalSignsExportFilter.CUSTOM) {
            DateSelector(
                label = "Fecha de inicio",
                selectedDate = fechaInicioExportacionSignos,
                onPickDate = {
                    val base = Calendar.getInstance().apply { timeInMillis = fechaInicioExportacionSignos }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                            val nuevo = inicioDelDia(picked.timeInMillis)
                            onFechaInicioExportacionSignosChange(nuevo)
                            if (fechaFinExportacionSignos < nuevo) onFechaFinExportacionSignosChange(finDelDia(nuevo))
                        },
                        base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            )
            DateSelector(
                label = "Fecha de fin",
                selectedDate = fechaFinExportacionSignos,
                onPickDate = {
                    val base = Calendar.getInstance().apply { timeInMillis = fechaFinExportacionSignos }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                            val nuevo = finDelDia(picked.timeInMillis)
                            onFechaFinExportacionSignosChange(nuevo)
                            if (fechaInicioExportacionSignos > nuevo) onFechaInicioExportacionSignosChange(inicioDelDia(nuevo))
                        },
                        base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            )
        }
        VitalSignsMetallicButton(
            text = if (exportandoSignosVitales) "Exportando..." else "Exportar a Word (.docx)",
            onClick = {
                val range = resolveVitalSignsExportRange(filtroExportacionSignos, fechaInicioExportacionSignos, fechaFinExportacionSignos)
                val registrosEnRango = signosVitales.filter { it.fechaRegistro in range.start..range.end }.sortedBy { it.fechaRegistro }
                if (registrosEnRango.isEmpty()) {
                    Toast.makeText(context, "No hay registros en el filtro seleccionado", Toast.LENGTH_SHORT).show()
                    return@VitalSignsMetallicButton
                }
                onExportacionSignosPendienteChange(VitalSignsExportRequest(label = range.label, fileSuffix = range.fileSuffix, records = registrosEnRango))
                onExportandoSignosVitalesChange(true)
                onLanzarExportVitalSigns("signos-vitales-${range.fileSuffix}-${timestampArchivo()}.docx")
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (signosVitales.isEmpty()) {
            Text("No hay registros guardados para mostrar.", color = Color.White)
        } else {
            Text("Listado de registros por mes", color = Color.White, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VitalSignsMetallicButton(
                    text = if (registrosSignosSeleccionados.size == signosVitales.size) "Quitar todo" else "Seleccionar todos",
                    onClick = {
                        if (registrosSignosSeleccionados.size == signosVitales.size) {
                            registrosSignosSeleccionados.clear()
                            onMostrarVistaPreviaSignosSeleccionadosChange(false)
                        } else {
                            registrosSignosSeleccionados.clear()
                            registrosSignosSeleccionados.addAll(signosVitales.map { it.id })
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                VitalSignsMetallicButton(
                    text = "Limpiar seleccion",
                    onClick = { registrosSignosSeleccionados.clear(); onMostrarVistaPreviaSignosSeleccionadosChange(false) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (mostrarVistaPreviaSignosSeleccionados && signosVitalesSeleccionados.isNotEmpty()) {
                Text("Visualizacion de seleccion", color = Color.White, fontWeight = FontWeight.Medium)
                signosVitalesSeleccionados.forEach { registro ->
                    MetallicRedVitalSignsCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12, verticalSpacing = 4) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (registro.sistolica != null && registro.diastolica != null) Text("Presion: ${registro.sistolica}/${registro.diastolica}", color = Color.White)
                            if (registro.comentarioPresion.isNotBlank()) Text("Comentario presion: ${registro.comentarioPresion}", color = Color.White)
                            if (registro.latidos != null) Text("Latidos: ${registro.latidos} lpm", color = Color.White)
                            if (registro.comentarioLatidos.isNotBlank()) Text("Comentario latidos: ${registro.comentarioLatidos}", color = Color.White)
                            if (registro.glucemia != null) Text("Glucemia: ${registro.glucemia}", color = Color.White)
                            if (registro.comentarioGlucemia.isNotBlank()) Text("Comentario glucemia: ${registro.comentarioGlucemia}", color = Color.White)
                            if (registro.temperatura != null) Text("Temperatura: ${formatTemperature(registro.temperatura)}", color = Color.White)
                            if (registro.comentarioTemperatura.isNotBlank()) Text("Comentario temperatura: ${registro.comentarioTemperatura}", color = Color.White)
                            if (registro.peso != null) Text("Peso: ${"%.1f".format(registro.peso)} ${registro.pesoUnidad}", color = Color.White)
                            if (registro.imc != null) Text("IMC: ${"%.1f".format(registro.imc)} — ${etiquetaIMC(registro.imc)}", color = Color.White)
                            Text("Fecha: ${formatDateTimeMain(registro.fechaRegistro)}", color = Color.White)
                        }
                    }
                }
            }

            val mesesAgrupados = signosVitales.sortedByDescending { it.fechaRegistro }.groupBy { yearMonthKey(it.fechaRegistro) }.toSortedMap(reverseOrder())

            mesesAgrupados.forEach { (mesKey, registrosMes) ->
                val expandido = mesesExpandidosSignos.contains(mesKey)
                val todosSeleccionadosMes = registrosMes.all { registrosSignosSeleccionados.contains(it.id) }
                val algunoSeleccionadoMes = registrosMes.any { registrosSignosSeleccionados.contains(it.id) }
                val labelMes = formatYearMonthLabel(registrosMes.first().fechaRegistro)

                MetallicRedVitalSignsCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12, verticalSpacing = 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (expandido) mesesExpandidosSignos.remove(mesKey)
                                else if (!mesesExpandidosSignos.contains(mesKey)) mesesExpandidosSignos.add(mesKey)
                            },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = todosSeleccionadosMes,
                                    onCheckedChange = { checked ->
                                        if (checked) registrosMes.forEach { if (!registrosSignosSeleccionados.contains(it.id)) registrosSignosSeleccionados.add(it.id) }
                                        else registrosMes.forEach { registrosSignosSeleccionados.remove(it.id) }
                                        if (registrosSignosSeleccionados.isEmpty()) onMostrarVistaPreviaSignosSeleccionadosChange(false)
                                    }
                                )
                                Text(text = "$labelMes (${registrosMes.size} registros)", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Text(text = if (expandido) "▲" else "▼", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (expandido) {
                            Spacer(Modifier.height(8.dp))
                            registrosMes.sortedByDescending { it.fechaRegistro }.forEach { registro ->
                                val estaSeleccionado = registrosSignosSeleccionados.contains(registro.id)
                                MetallicRedVitalSignsCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8, verticalSpacing = 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = estaSeleccionado,
                                            onCheckedChange = { checked ->
                                                if (checked) { if (!registrosSignosSeleccionados.contains(registro.id)) registrosSignosSeleccionados.add(registro.id) }
                                                else registrosSignosSeleccionados.remove(registro.id)
                                                if (registrosSignosSeleccionados.isEmpty()) onMostrarVistaPreviaSignosSeleccionadosChange(false)
                                            }
                                        )
                                        Text(text = formatDateTimeMain(registro.fechaRegistro), modifier = Modifier.weight(1f), color = Color.White)
                                    }
                                }
                            }
                            if (algunoSeleccionadoMes) {
                                Spacer(Modifier.height(8.dp))
                                VitalSignsMetallicButton(
                                    text = "Exportar mes seleccionado (.docx)",
                                    onClick = {
                                        val sel = registrosMes.filter { registrosSignosSeleccionados.contains(it.id) }.sortedBy { it.fechaRegistro }
                                        if (sel.isEmpty()) { Toast.makeText(context, "Selecciona al menos un registro del mes", Toast.LENGTH_SHORT).show(); return@VitalSignsMetallicButton }
                                        val etiqueta = "$labelMes (${sel.size} registros)"
                                        onExportacionSignosPendienteChange(VitalSignsExportRequest(label = etiqueta, fileSuffix = "mes", records = sel))
                                        onExportandoSignosVitalesChange(true)
                                        onLanzarExportVitalSigns("signos-vitales-mes-${timestampArchivo()}.docx")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VitalSignsMetallicButton(
                    text = if (mostrarVistaPreviaSignosSeleccionados) "Ocultar visualizacion" else "Visualizar seleccion",
                    onClick = {
                        if (signosVitalesSeleccionados.isEmpty()) { Toast.makeText(context, "Selecciona al menos un registro", Toast.LENGTH_SHORT).show(); return@VitalSignsMetallicButton }
                        onMostrarVistaPreviaSignosSeleccionadosChange(!mostrarVistaPreviaSignosSeleccionados)
                    },
                    modifier = Modifier.weight(1f)
                )
                VitalSignsMetallicButton(
                    text = "Exportar seleccion (.docx)",
                    onClick = {
                        if (signosVitalesSeleccionados.isEmpty()) { Toast.makeText(context, "Selecciona al menos un registro para exportar", Toast.LENGTH_SHORT).show(); return@VitalSignsMetallicButton }
                        val etiqueta = if (signosVitalesSeleccionados.size == 1) "Registro del ${formatDateTimeMain(signosVitalesSeleccionados.first().fechaRegistro)}" else "Seleccion manual (${signosVitalesSeleccionados.size} registros)"
                        onExportacionSignosPendienteChange(VitalSignsExportRequest(label = etiqueta, fileSuffix = "seleccion", records = signosVitalesSeleccionados))
                        onExportandoSignosVitalesChange(true)
                        onLanzarExportVitalSigns("signos-vitales-seleccion-${timestampArchivo()}.docx")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
