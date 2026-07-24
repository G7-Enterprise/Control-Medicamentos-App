package com.carlos.controlmedicamentos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.MedicationOrder
import com.carlos.controlmedicamentos.formatMoney
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
internal fun PanelPedidosPanel(
    mostrarPanelPedidos: Boolean,
    pedidosPaciente: List<MedicationOrder>,
    carritoItems: List<CarritoItem>,
    monedaActiva: String,
    pacienteActivoId: Int?,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    inputUnidadesRecibidas: String,
    inputPrecioActualizado: String,
    itemCarritoAConfirmar: CarritoItem?,
    itemCarritoAEliminar: CarritoItem?,
    confirmarRecepcionTotal: Boolean,
    pedidoAEditar: MedicationOrder?,
    inputEditarResumen: String,
    inputEditarTotal: String,
    pedidoAEliminar: MedicationOrder?,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    onInputUnidadesRecibidasChange: (String) -> Unit,
    onInputPrecioActualizadoChange: (String) -> Unit,
    onItemCarritoAConfirmarChange: (CarritoItem?) -> Unit,
    onItemCarritoAEliminarChange: (CarritoItem?) -> Unit,
    onConfirmarRecepcionTotalChange: (Boolean) -> Unit,
    onMostrarPanelPedidosChange: (Boolean) -> Unit,
    onMostrarListaInsumosChange: (Boolean) -> Unit,
    onPedidoAEditarChange: (MedicationOrder?) -> Unit,
    onInputEditarResumenChange: (String) -> Unit,
    onInputEditarTotalChange: (String) -> Unit,
    onPedidoAEliminarChange: (MedicationOrder?) -> Unit,
    onCerrarPanelesSecundarios: () -> Unit
) {
    val context = LocalContext.current

    if (!mostrarPanelPedidos) return

    var mostrarDialogoVaciar by remember { mutableStateOf(false) }

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
                "Lista de pedidos pendientes",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (carritoItems.isEmpty()) {
                Text("La lista de pedidos está vacía. Ve a Medicamentos en uso y usa «Añadir a la lista de pedidos» para cada medicamento que necesites.", fontSize = 13.sp)
            } else {
                for (item in carritoItems) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A3A6A),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(item.medication.nombre, fontWeight = FontWeight.Bold)
                            val detalle = buildString {
                                if (item.medication.concentracion.isNotBlank()) append(item.medication.concentracion)
                            }.trim()
                            if (detalle.isNotBlank()) Text(detalle, fontSize = 12.sp)
                            Text("Unidades solicitadas: ${item.unidadesSolicitadas}")
                            if (item.medication.precioPorUnidad != null) {
                                Text("Coste estimado: ${formatMoney(item.medication.precioPorUnidad * item.unidadesSolicitadas, monedaActiva)}", fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        onInputUnidadesRecibidasChange(item.unidadesSolicitadas.toString())
                                        onInputPrecioActualizadoChange(item.medication.precioPorUnidad?.let { "%.2f".format(it).replace(".00", "") } ?: "")
                                        onItemCarritoAConfirmarChange(item)
                                    }
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = "Confirmar recibido")
                                }
                                IconButton(
                                    onClick = {
                                        onItemCarritoAEliminarChange(item)
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Quitar")
                                }
                            }
                        }
                    }
                }

                val totalEstimado = carritoItems.sumOf {
                    (it.medication.precioPorUnidad ?: 0.0) * it.unidadesSolicitadas
                }
                if (totalEstimado > 0) {
                    Text("Total estimado del pedido: ${formatMoney(totalEstimado, monedaActiva)}", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val lineas = carritoItems.joinToString("\n") { item ->
                            val det = buildString {
                                if (item.medication.concentracion.isNotBlank()) append(" ${item.medication.concentracion}")
                            }.trim()
                            val precioUnitario = item.medication.precioPorUnidad
                            val precioTexto = if (precioUnitario != null && precioUnitario > 0) {
                                " (${formatMoney(precioUnitario * item.unidadesSolicitadas, monedaActiva)})"
                            } else {
                                " (precio)"
                            }
                            "- ${item.unidadesSolicitadas} und. ${item.medication.nombre}${if (det.isNotBlank()) "  ($det)" else ""}$precioTexto"
                        }
                        val totalMensaje = if (totalEstimado > 0) {
                            "---Total: ${formatMoney(totalEstimado, monedaActiva)}"
                        } else {
                            "---Total: XXX. XX"
                        }
                        val mensaje = "Hola, necesito los siguientes medicamentos:\n$lineas\n$totalMensaje"
                        val telefono = carritoItems
                            .firstOrNull { it.medication.telefonoPedidoWhatsapp.isNotBlank() }
                            ?.medication?.telefonoPedidoWhatsapp
                            ?.filter { it.isDigit() || it == '+' }
                            ?: ""
                        val uri = if (telefono.isNotBlank()) {
                            "https://wa.me/$telefono?text=${Uri.encode(mensaje)}"
                        } else {
                            "https://wa.me/?text=${Uri.encode(mensaje)}"
                        }
                        val resumen = carritoItems.joinToString("\n") { item ->
                            val det = buildString {
                                if (item.medication.concentracion.isNotBlank()) append(" ${item.medication.concentracion}")
                            }.trim()
                            "${item.medication.nombre}${if (det.isNotBlank()) " $det" else ""} x${item.unidadesSolicitadas}"
                        }
                        val totalAmt = totalEstimado.takeIf { it > 0 }
                        val pricedCount = carritoItems.count { it.medication.precioPorUnidad != null }
                        coroutineScope.launch(Dispatchers.IO) {
                            database.medicationOrderDao().insertar(
                                MedicationOrder(
                                    patientId = pacienteActivoId ?: 0,
                                    itemCount = carritoItems.size,
                                    pricedItemCount = pricedCount,
                                    totalAmount = totalAmt,
                                    restockSource = carritoItems.firstOrNull()?.medication?.origenReposicion ?: "WHATSAPP_NUMBER",
                                    supplierLabel = if (telefono.isNotBlank()) "WhatsApp $telefono" else "Manual",
                                    whatsappPhone = telefono,
                                    itemsSummary = resumen,
                                    messagePreview = mensaje
                                )
                            )
                        }
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        )
                        Toast.makeText(
                            context,
                            "Pedido guardado en historial. Para actualizar stock confirma la recepción cuando recibas las unidades.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("Enviar lista por WhatsApp", color = Color.White)
                }

                Button(
                    onClick = {
                        onConfirmarRecepcionTotalChange(true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar recepción de todo", color = Color.White)
                }

                Button(
                    onClick = { mostrarDialogoVaciar = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                ) {
                    Text("Vaciar lista de pedidos", color = Color.White)
                }

                if (mostrarDialogoVaciar) {
                    AlertDialog(
                        onDismissRequest = { mostrarDialogoVaciar = false },
                        title = { Text("Vaciar lista") },
                        text = { Text("¿Seguro que deseas vaciar la lista de pedidos pendientes?") },
                        confirmButton = {
                            IconButton(
                                onClick = {
                                    mostrarDialogoVaciar = false
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.carritoPendienteDao().limpiarPorPaciente(pacienteActivoId ?: 0)
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Aceptar", tint = Color(0xFF4CAF50))
                            }
                        },
                        dismissButton = {
                            IconButton(onClick = { mostrarDialogoVaciar = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = Color(0xFFFF5252))
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    onMostrarPanelPedidosChange(false)
                    onMostrarListaInsumosChange(true)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8A))
            ) {
                Text("Seguir añadiendo medicamentos", color = Color.White)
            }
            Button(
                onClick = { onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al escritorio", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Historial de pedidos enviados",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            val mesesExpanded = remember { mutableStateMapOf<Long, Boolean>() }
            val mesesNombres = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
            if (pedidosPaciente.isEmpty()) {
                Text("Aún no hay pedidos enviados.", fontSize = 13.sp)
            } else {
                val pedidosPorMes = pedidosPaciente
                    .groupBy { pedido ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = pedido.createdAt }
                        val year = cal.get(java.util.Calendar.YEAR).toLong()
                        val month = cal.get(java.util.Calendar.MONTH).toLong()
                        year * 100 + month
                    }
                    .entries
                    .sortedByDescending { it.key }
                for ((mesKey, pedidosMes) in pedidosPorMes) {
                    val year = (mesKey / 100).toInt()
                    val month = (mesKey % 100).toInt()
                    val totalMes = pedidosMes.sumOf { it.totalAmount ?: 0.0 }
                    val isExpanded = mesesExpanded.getOrDefault(mesKey, false)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0D2247),
                            contentColor = Color.White
                        )
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mesesExpanded[mesKey] = !isExpanded }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${mesesNombres[month]} $year",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${pedidosMes.size} pedido(s)",
                                        color = Color(0xFFAAC8FF),
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (totalMes > 0) {
                                        Text(
                                            formatMoney(totalMes, monedaActiva),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFFAAC8FF)
                                    )
                                }
                            }
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (pedido in pedidosMes) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF1A3A6A),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = formatDateMain(pedido.createdAt) + " " + formatHourMain(pedido.createdAt),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(pedido.itemsSummary)
                                                        if (pedido.totalAmount != null && pedido.totalAmount > 0) {
                                                            Text("Total: ${formatMoney(pedido.totalAmount, monedaActiva)}")
                                                        }
                                                        if (pedido.supplierLabel.isNotBlank()) {
                                                            Text("Destino: ${pedido.supplierLabel}")
                                                        }
                                                    }
                                                    Row {
                                                        IconButton(onClick = {
                                                            onPedidoAEditarChange(pedido)
                                                            onInputEditarResumenChange(pedido.itemsSummary)
                                                            onInputEditarTotalChange(pedido.totalAmount?.let { "%.2f".format(it) } ?: "")
                                                        }) {
                                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFFAAC8FF))
                                                        }
                                                        IconButton(onClick = {
                                                            onPedidoAEliminarChange(pedido)
                                                        }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF6B6B))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
