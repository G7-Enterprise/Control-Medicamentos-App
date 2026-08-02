package com.carlos.controlmedicamentos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@Composable
internal fun ListaInsumosPanel(
    insumosGuardados: List<Medication>,
    inventarioLazyRowState: LazyListState,
    monedaActiva: String,
    carritoItems: List<CarritoItem>,
    insumoSeleccionadoEnInventario: Int?,
    mostrarListaInsumos: Boolean,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    editingMedicationId: Int?,
    onInputRecargarStockChange: (String) -> Unit,
    onInsumoARecargarChange: (Medication?) -> Unit,
    onInputUnidadesPedidoChange: (String) -> Unit,
    onInsumoAPedirChange: (Medication?) -> Unit,
    onMostrarPanelPedidosChange: (Boolean) -> Unit,
    onMostrarFormularioChange: (Boolean) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit,
    onCargarMedicamentoEnFormulario: (Medication) -> Unit,
    onAlarmaActivaChange: (Boolean) -> Unit,
    onMedicationToDeleteChange: (Medication?) -> Unit,
    panelInternoScrollState: androidx.compose.foundation.ScrollState
) {
    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Medicamentos",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (insumosGuardados.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors()
                ) {
                    Text(
                        text = "Aún no hay medicamentos registrados.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyRow(
                    state = inventarioLazyRowState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(insumosGuardados, key = { it.id }) { insumo ->
                        MetallicMedicationCard(
                            modifier = Modifier.width(300.dp),
                            contentPadding = 12,
                            verticalSpacing = 4,
                            isInss = insumo.origenReposicion == "INSS",
                            isStockCritical = insumo.stockActual != null && insumo.stockActual <= 0
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val formaInfo = FORMAS_MEDICAMENTO.firstOrNull { it.first == insumo.formaMedicamento }
                                if (formaInfo != null) {
                                    val iconColor = insumo.colorMedicamento
                                        .takeIf { it.isNotBlank() }
                                        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                                        ?: Color.White
                                    Icon(
                                        imageVector = formaInfo.second,
                                        contentDescription = formaInfo.third,
                                        tint = iconColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(insumo.nombre, fontWeight = FontWeight.Bold)
                            }
                            if (insumo.concentracion.isNotBlank()) {
                                Text("Especificación: ${insumo.concentracion}")
                            }
                            Text("Cantidad: ${insumo.dosis}")
                            if (insumo.horariosTomas.isNotBlank()) {
                                Text("Horarios: ${insumo.horariosTomas.replace("|", ", ")}")
                            } else if (insumo.horaToma.isNotBlank()) {
                                Text("Hora del recordatorio: ${insumo.horaToma}")
                            }
                            if (!insumo.estaActivo) {
                                Text("Estado: Inactivo")
                            }
                            Text("Alarma: ${if (insumo.alarmaActiva) "Activa" else "Desactivada"}")
                            Text("Inicio: ${formatDateMain(insumo.fechaInicio)}")
                            if (insumo.esCicloCorto) {
                                Text("Fin: ${formatDateMain(insumo.fechaFin)}")
                            }
                            if (insumo.stockActual != null) {
                                val dosisPorToma = insumo.dosis.toIntOrNull() ?: 1
                                val tomasRestantes = insumo.stockActual / dosisPorToma
                                val stockCritico = insumo.stockActual <= 0
                                Text(
                                    "Stock: ${insumo.stockActual} uds. · $tomasRestantes tomas aprox.",
                                    color = if (stockCritico) Color(0xFFFF5252) else LocalContentColor.current,
                                    fontWeight = if (stockCritico) FontWeight.Bold else FontWeight.Normal
                                )
                                if (insumo.stockMinimo != null) {
                                    Text("Aviso stock: ${insumo.stockMinimo} uds.")
                                }
                                if (insumo.origenReposicion == "INSS") {
                                    Text("Adquisición sin costo")
                                } else if (insumo.precioPorUnidad != null) {
                                    Text("Precio: ${formatMoney(insumo.precioPorUnidad, monedaActiva)}/ud.")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        onCerrarPanelesSecundarios()
                                        onCargarMedicamentoEnFormulario(insumo)
                                        onMostrarFormularioChange(true)
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                }
                                IconButton(
                                    onClick = { onMedicationToDeleteChange(insumo) }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                }
                            }
                            if (insumo.stockActual != null) {
                                Button(
                                    onClick = {
                                        onInputRecargarStockChange("")
                                        onInsumoARecargarChange(insumo)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                                ) {
                                    Text("Recargar stock", color = Color.Black)
                                }
                                if (insumo.origenReposicion != "INSS") {
                                    Button(
                                        onClick = {
                                            onInputUnidadesPedidoChange("")
                                            onInsumoAPedirChange(insumo)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                                    ) {
                                        Text("Añadir a la lista de pedidos", color = Color.Black)
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val nuevoEstado = !insumo.estaActivo
                                        database.medicationDao().cambiarEstado(insumo.id, nuevoEstado)
                                        if (!nuevoEstado) {
                                            MedicationScheduler(context).cancelarAlarma(insumo.id)
                                        } else {
                                            val actualizado = insumo.copy(estaActivo = nuevoEstado)
                                            if (actualizado.alarmaActiva) MedicationScheduler(context).programarAlarmas(actualizado)
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                if (nuevoEstado) "Medicamento reactivado" else "Medicamento suspendido",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                            ) {
                                Text(if (insumo.estaActivo) "Suspender" else "Reactivar", color = Color.Black)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val nuevoEstado = !insumo.alarmaActiva
                                        database.medicationDao().cambiarAlarmaActiva(insumo.id, nuevoEstado)
                                        val actualizado = insumo.copy(alarmaActiva = nuevoEstado)
                                        if (nuevoEstado) {
                                            MedicationScheduler(context).programarAlarmas(actualizado)
                                        } else {
                                            MedicationScheduler(context).cancelarAlarma(insumo.id)
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (editingMedicationId == insumo.id) {
                                                onAlarmaActivaChange(nuevoEstado)
                                            }
                                            Toast.makeText(
                                                context,
                                                if (nuevoEstado) "Alarma activada" else "Alarma desactivada",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
                            ) {
                                Text(if (insumo.alarmaActiva) "Desactivar alarma" else "Activar alarma", color = Color.Black)
                            }
                        }
                    }
                }
            }

            LaunchedEffect(mostrarListaInsumos) {
                if (mostrarListaInsumos) {
                    coroutineScope.launch(Dispatchers.IO) {
                        NotificacionHelper.verificarYNotificarStockBajo(context)
                    }
                }
            }

            LaunchedEffect(mostrarListaInsumos, insumoSeleccionadoEnInventario, insumosGuardados) {
                if (mostrarListaInsumos && insumoSeleccionadoEnInventario != null) {
                    val index = insumosGuardados.indexOfFirst { it.id == insumoSeleccionadoEnInventario }
                    if (index >= 0) {
                        inventarioLazyRowState.animateScrollToItem(index)
                    }
                }
            }

            if (carritoItems.isNotEmpty()) {
                Button(
                    onClick = {
                        onCerrarPanelesSecundarios()
                        onMostrarPanelPedidosChange(true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20), contentColor = Color.Black)
                ) {
                    Text("Ver lista de pedidos (${carritoItems.size} ${if (carritoItems.size == 1) "medicamento" else "medicamentos"})", color = Color.Black)
                }
            }

            Button(
                onClick = { onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
            ) {
                Text("Volver al escritorio", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
