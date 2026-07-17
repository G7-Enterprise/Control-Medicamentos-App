package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.CarritoPendienteItem
import com.carlos.controlmedicamentos.data.local.MedicationOrder
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DialogosOperacionesPanel(
    adjuntosPendientesReemplazo: androidx.compose.runtime.snapshots.SnapshotStateList<PendingAttachmentReplacement>,
    mostrarDialogoCerrarInformeSinGuardar: Boolean,
    medicationToDelete: com.carlos.controlmedicamentos.data.local.Medication?,
    duplicateMedication: com.carlos.controlmedicamentos.data.local.Medication?,
    insumoARecargar: com.carlos.controlmedicamentos.data.local.Medication?,
    insumoAPedir: com.carlos.controlmedicamentos.data.local.Medication?,
    itemCarritoAConfirmar: CarritoItem?,
    itemCarritoAEliminar: CarritoItem?,
    confirmarRecepcionTotal: Boolean,
    pedidoAEliminar: MedicationOrder?,
    pedidoAEditar: MedicationOrder?,
    tomaPendienteDeEliminar: IntakeRemovalConfirmation?,
    inputRecargarStock: String,
    inputUnidadesPedido: String,
    inputUnidadesRecibidas: String,
    inputPrecioActualizado: String,
    inputEditarResumen: String,
    inputEditarTotal: String,
    monedaActiva: String,
    carritoItems: List<CarritoItem>,
    pacienteActivoId: Int?,
    editingMedicationId: Int?,
    estudiosAdjuntos: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    database: AppDatabase,
    onMostrarDialogoCerrarInformeSinGuardarChange: (Boolean) -> Unit,
    onMedicationToDeleteChange: (com.carlos.controlmedicamentos.data.local.Medication?) -> Unit,
    onDuplicateMedicationChange: (com.carlos.controlmedicamentos.data.local.Medication?) -> Unit,
    onInsumoARecargarChange: (com.carlos.controlmedicamentos.data.local.Medication?) -> Unit,
    onInsumoAPedirChange: (com.carlos.controlmedicamentos.data.local.Medication?) -> Unit,
    onItemCarritoAConfirmarChange: (CarritoItem?) -> Unit,
    onItemCarritoAEliminarChange: (CarritoItem?) -> Unit,
    onConfirmarRecepcionTotalChange: (Boolean) -> Unit,
    onPedidoAEliminarChange: (MedicationOrder?) -> Unit,
    onPedidoAEditarChange: (MedicationOrder?) -> Unit,
    onTomaPendienteDeEliminarChange: (IntakeRemovalConfirmation?) -> Unit,
    onInputRecargarStockChange: (String) -> Unit,
    onInputUnidadesPedidoChange: (String) -> Unit,
    onInputUnidadesRecibidasChange: (String) -> Unit,
    onInputPrecioActualizadoChange: (String) -> Unit,
    onInputEditarResumenChange: (String) -> Unit,
    onInputEditarTotalChange: (String) -> Unit,
    onCargarMedicamentoEnFormulario: (com.carlos.controlmedicamentos.data.local.Medication) -> Unit,
    onGuardarInformeMedicoActual: () -> Unit,
    onCerrarFormularioInforme: () -> Unit,
    onResetForm: () -> Unit,
    onDeleteAttachmentFile: (String) -> Unit,
    onFormatDate: (Long) -> String,
    onFormatHour: (Long) -> String,
    onFormatMoney: (Double, String) -> String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    adjuntosPendientesReemplazo.firstOrNull()?.let { pendiente ->
        AlertDialog(
            onDismissRequest = {
                onDeleteAttachmentFile(pendiente.newPath)
                adjuntosPendientesReemplazo.remove(pendiente)
            },
            title = { Text("Adjunto repetido") },
            text = { Text("La imagen ${pendiente.displayName} ya existe en este informe. Quieres reemplazarla o cancelar?") },
            confirmButton = {
                Button(onClick = {
                    estudiosAdjuntos.remove(pendiente.existingPath)
                    onDeleteAttachmentFile(pendiente.existingPath)
                    if (!estudiosAdjuntos.contains(pendiente.newPath)) estudiosAdjuntos.add(pendiente.newPath)
                    adjuntosPendientesReemplazo.remove(pendiente)
                }) { Text("Reemplazar") }
            },
            dismissButton = {
                Button(onClick = {
                    onDeleteAttachmentFile(pendiente.newPath)
                    adjuntosPendientesReemplazo.remove(pendiente)
                }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoCerrarInformeSinGuardar) {
        AlertDialog(
            onDismissRequest = { onMostrarDialogoCerrarInformeSinGuardarChange(false) },
            title = { Text("Cambios sin guardar") },
            text = { Text("Aun no se ha guardado. Si cierra perdera todos los datos. Desea guardar?") },
            confirmButton = {
                Button(onClick = { onGuardarInformeMedicoActual() }) { Text("Guardar") }
            },
            dismissButton = {
                Button(onClick = { onCerrarFormularioInforme() }) { Text("Cerrar y perder datos") }
            }
        )
    }

    medicationToDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { onMedicationToDeleteChange(null) },
            title = { Text("Eliminar medicamento") },
            text = { Text("La eliminacion del medicamento es irreversible. Esta seguro?") },
            confirmButton = {
                Button(
                    onClick = {
                        onMedicationToDeleteChange(null)
                        coroutineScope.launch(Dispatchers.IO) {
                            database.medicationDao().eliminar(medication)
                            MedicationScheduler(context).cancelarAlarma(medication.id)
                            withContext(Dispatchers.Main) {
                                if (editingMedicationId == medication.id) onResetForm()
                                Toast.makeText(context, "Medicamento eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("✓") }
            },
            dismissButton = {
                Button(onClick = { onMedicationToDeleteChange(null) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("X") }
            }
        )
    }

    duplicateMedication?.let { medication ->
        AlertDialog(
            onDismissRequest = { onDuplicateMedicationChange(null) },
            title = { Text("Medicamento ya existe") },
            text = { Text("Ya existe un medicamento guardado con estos datos. Deseas modificar el registro existente?") },
            confirmButton = {
                Button(onClick = {
                    onDuplicateMedicationChange(null)
                    onCargarMedicamentoEnFormulario(medication)
                    Toast.makeText(context, "Se cargo el medicamento existente para modificarlo", Toast.LENGTH_SHORT).show()
                }) { Text("Modificar") }
            },
            dismissButton = {
                Button(onClick = { onDuplicateMedicationChange(null) }) { Text("Cancelar") }
            }
        )
    }

    insumoARecargar?.let { med ->
        AlertDialog(
            onDismissRequest = { onInsumoARecargarChange(null) },
            title = { Text("Recargar stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stock actual: ${med.stockActual ?: 0} uds. ¿Cuántas unidades añades?")
                    OutlinedTextField(
                        value = inputRecargarStock,
                        onValueChange = { if (it.all(Char::isDigit)) onInputRecargarStockChange(it) },
                        label = { Text("Unidades a añadir") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val añadir = inputRecargarStock.toIntOrNull() ?: 0
                    if (añadir > 0) {
                        val nuevoStock = (med.stockActual ?: 0) + añadir
                        coroutineScope.launch(Dispatchers.IO) {
                            database.medicationDao().actualizarStock(med.id, nuevoStock)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Stock actualizado: $nuevoStock uds.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    onInsumoARecargarChange(null)
                }) { Text("Guardar") }
            },
            dismissButton = {
                Button(onClick = { onInsumoARecargarChange(null) }) { Text("Cancelar") }
            }
        )
    }

    insumoAPedir?.let { med ->
        AlertDialog(
            onDismissRequest = { onInsumoAPedirChange(null) },
            title = { Text("Añadir a la lista de pedidos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Cuántas unidades de ${med.nombre} quieres solicitar?")
                    OutlinedTextField(
                        value = inputUnidadesPedido,
                        onValueChange = { if (it.all(Char::isDigit)) onInputUnidadesPedidoChange(it) },
                        label = { Text("Unidades") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val unidades = inputUnidadesPedido.toIntOrNull() ?: 0
                    if (unidades > 0 && pacienteActivoId != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val existing = database.carritoPendienteDao().buscarPorMedicamento(pacienteActivoId, med.id)
                            if (existing != null) {
                                database.carritoPendienteDao().actualizarUnidades(existing.id, existing.unidadesSolicitadas + unidades)
                            } else {
                                database.carritoPendienteDao().insertar(CarritoPendienteItem(patientId = pacienteActivoId, medicationId = med.id, unidadesSolicitadas = unidades))
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${med.nombre} añadido a la lista de pedidos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    onInsumoAPedirChange(null)
                }) { Text("Añadir a la lista") }
            },
            dismissButton = {
                Button(onClick = { onInsumoAPedirChange(null) }) { Text("Cancelar") }
            }
        )
    }

    itemCarritoAConfirmar?.let { item ->
        AlertDialog(
            onDismissRequest = { onItemCarritoAConfirmarChange(null) },
            title = { Text("Confirmar recepción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.medication.nombre)
                    Text("Unidades solicitadas: ${item.unidadesSolicitadas}")
                    Text("¿Cuántas unidades recibiste realmente?")
                    OutlinedTextField(
                        value = inputUnidadesRecibidas,
                        onValueChange = { if (it.all(Char::isDigit)) onInputUnidadesRecibidasChange(it) },
                        label = { Text("Unidades recibidas") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = inputPrecioActualizado,
                        onValueChange = { onInputPrecioActualizadoChange(it) },
                        label = { Text("Precio por unidad actual (opcional)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    if (item.medication.precioPorUnidad != null) {
                        Text("Precio anterior: ${onFormatMoney(item.medication.precioPorUnidad!!, monedaActiva)}", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val recibidas = inputUnidadesRecibidas.toIntOrNull() ?: 0
                    if (recibidas > 0) {
                        val med = item.medication
                        val unidadesPedidas = item.unidadesSolicitadas
                        val patId = pacienteActivoId ?: 0
                        val precioActualizado = inputPrecioActualizado.replace(',', '.').toDoubleOrNull()
                        coroutineScope.launch(Dispatchers.IO) {
                            val medActual = database.medicationDao().findById(med.id)
                            val newStock = (medActual?.stockActual ?: 0) + recibidas
                            if (medActual != null) {
                                database.medicationDao().actualizar(medActual.copy(stockActual = newStock, precioPorUnidad = if (precioActualizado != null && precioActualizado > 0) precioActualizado else medActual.precioPorUnidad))
                            } else {
                                database.medicationDao().actualizarStock(med.id, newStock)
                            }
                            database.carritoPendienteDao().eliminarPorId(item.carritoRowId)
                            val resumen = if (recibidas < unidadesPedidas) "${med.nombre} x$recibidas (recibidas de $unidadesPedidas solicitadas)" else "${med.nombre} x$recibidas (recibidas completas)"
                            val precioUsado = precioActualizado ?: med.precioPorUnidad
                            database.medicationOrderDao().insertar(MedicationOrder(patientId = patId, itemCount = 1, pricedItemCount = if (precioUsado != null) 1 else 0, totalAmount = precioUsado?.let { it * recibidas }, restockSource = med.origenReposicion.ifBlank { "MANUAL" }, supplierLabel = "Recepción confirmada", whatsappPhone = "", itemsSummary = resumen, messagePreview = resumen))
                            withContext(Dispatchers.Main) {
                                val msgPrecio = if (precioActualizado != null) " · Precio actualizado" else ""
                                Toast.makeText(context, "${med.nombre}: +$recibidas uds.$msgPrecio", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    onItemCarritoAConfirmarChange(null)
                }) { Text("Confirmar") }
            },
            dismissButton = {
                Button(onClick = { onItemCarritoAConfirmarChange(null) }) { Text("Cancelar") }
            }
        )
    }

    itemCarritoAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { onItemCarritoAEliminarChange(null) },
            title = { Text("Eliminar medicamento") },
            text = { Text("¿Eliminar ${item.medication.nombre} de la lista de pedidos?") },
            confirmButton = {
                IconButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) { database.carritoPendienteDao().eliminarPorId(item.carritoRowId) }
                    onItemCarritoAEliminarChange(null)
                }) { Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = Color(0xFF4CAF50)) }
            },
            dismissButton = {
                IconButton(onClick = { onItemCarritoAEliminarChange(null) }) { Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color(0xFFFF5252)) }
            }
        )
    }

    if (confirmarRecepcionTotal) {
        AlertDialog(
            onDismissRequest = { onConfirmarRecepcionTotalChange(false) },
            title = { Text("Confirmar recepción") },
            text = { Text("Se actualizará el stock de todos los medicamentos pedidos con las unidades solicitadas y se vaciará la lista de pedidos.") },
            confirmButton = {
                Button(onClick = {
                    onConfirmarRecepcionTotalChange(false)
                    val patId = pacienteActivoId ?: 0
                    coroutineScope.launch(Dispatchers.IO) {
                        val summary = carritoItems.joinToString("\n") { item ->
                            val det = buildString { if (item.medication.concentracion.isNotBlank()) append(" ${item.medication.concentracion}") }.trim()
                            "${item.medication.nombre}${if (det.isNotBlank()) " $det" else ""} x${item.unidadesSolicitadas}"
                        }
                        val totalAmt = carritoItems.sumOf { (it.medication.precioPorUnidad ?: 0.0) * it.unidadesSolicitadas }.takeIf { it > 0 }
                        carritoItems.forEach { item ->
                            val medActual = database.medicationDao().findById(item.medication.id)
                            database.medicationDao().actualizarStock(item.medication.id, (medActual?.stockActual ?: 0) + item.unidadesSolicitadas)
                        }
                        if (carritoItems.isNotEmpty()) {
                            database.medicationOrderDao().insertar(MedicationOrder(patientId = patId, itemCount = carritoItems.size, pricedItemCount = carritoItems.count { it.medication.precioPorUnidad != null }, totalAmount = totalAmt, restockSource = carritoItems.firstOrNull()?.medication?.origenReposicion ?: "MANUAL", supplierLabel = "Recepción confirmada", whatsappPhone = "", itemsSummary = summary, messagePreview = summary))
                        }
                        database.carritoPendienteDao().limpiarPorPaciente(patId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Recepción de todos los pedidos confirmada y stock actualizado", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                Button(onClick = { onConfirmarRecepcionTotalChange(false) }) { Text("Cancelar") }
            }
        )
    }

    pedidoAEliminar?.let { pedido ->
        AlertDialog(
            onDismissRequest = { onPedidoAEliminarChange(null) },
            title = { Text("Eliminar pedido") },
            text = { Text("¿Eliminar el pedido del ${onFormatDate(pedido.createdAt)}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    val id = pedido.id
                    onPedidoAEliminarChange(null)
                    coroutineScope.launch(Dispatchers.IO) { database.medicationOrderDao().eliminarPorId(id) }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                Button(onClick = { onPedidoAEliminarChange(null) }) { Text("Cancelar", color = Color.Black) }
            }
        )
    }

    pedidoAEditar?.let { pedido ->
        AlertDialog(
            onDismissRequest = { onPedidoAEditarChange(null) },
            title = { Text("Editar pedido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = inputEditarResumen, onValueChange = { onInputEditarResumenChange(it) }, label = { Text("Resumen de medicamentos") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = inputEditarTotal, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) onInputEditarTotalChange(it) }, label = { Text("Total ($monedaActiva)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nuevoTotal = inputEditarTotal.replace(',', '.').toDoubleOrNull()
                    val actualizado = pedido.copy(itemsSummary = inputEditarResumen.trim(), totalAmount = nuevoTotal)
                    onPedidoAEditarChange(null)
                    coroutineScope.launch(Dispatchers.IO) { database.medicationOrderDao().actualizar(actualizado) }
                }) { Text("Guardar", color = Color.Black) }
            },
            dismissButton = {
                Button(onClick = { onPedidoAEditarChange(null) }) { Text("Cancelar", color = Color.Black) }
            }
        )
    }

    tomaPendienteDeEliminar?.let { intake ->
        AlertDialog(
            onDismissRequest = { onTomaPendienteDeEliminarChange(null) },
            title = { Text("Eliminar registro de toma") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("La modificacion del registro real es irreversible. Desea continuar?")
                    Text("Medicamento: ${intake.medicationName}")
                    Text("Hora programada: ${onFormatHour(intake.scheduledAt)}")
                    Text("Hora real de toma: ${onFormatHour(intake.acceptedAt)}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pendingIntake = intake
                        onTomaPendienteDeEliminarChange(null)
                        coroutineScope.launch(Dispatchers.IO) {
                            database.medicationIntakeDao().eliminarPorMedicamentoYHorario(pendingIntake.medicationId, pendingIntake.scheduledAt)
                            val med = database.medicationDao().findById(pendingIntake.medicationId)
                            if (med?.stockActual != null) database.medicationDao().actualizarStock(med.id, med.stockActual + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("✓") }
            },
            dismissButton = {
                Button(onClick = { onTomaPendienteDeEliminarChange(null) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("X") }
            }
        )
    }
}
