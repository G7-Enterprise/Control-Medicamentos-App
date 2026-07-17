package com.carlos.controlmedicamentos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carlos.controlmedicamentos.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val COLOR_DENTAL = Color(0xFF00BCD4)

@Composable
internal fun FinanzasTab(
    patientId: Int,
    db: AppDatabase,
    onExportarPdf: () -> Unit
) {
    val transacciones by db.transaccionDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mostrarNueva by remember { mutableStateOf(false) }
    var transaccionEditar by remember { mutableStateOf<TransaccionDental?>(null) }
    var transaccionEliminar by remember { mutableStateOf<TransaccionDental?>(null) }
    var reciboAmpliado by remember { mutableStateOf<TransaccionDental?>(null) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    val totalIngresos = transacciones.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
    val totalGastos = transacciones.filter { it.tipo == "GASTO" }.sumOf { it.monto }
    val saldo = totalIngresos - totalGastos

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatFinanciero("Ingresos", totalIngresos, Color(0xFF66BB6A), Modifier.weight(1f))
                StatFinanciero("Gastos", totalGastos, Color(0xFFEF5350), Modifier.weight(1f))
                StatFinanciero("Saldo", saldo, COLOR_DENTAL, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { mostrarNueva = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)) {
                    Text("Nueva transacción")
                }
                OutlinedButton(onClick = onExportarPdf, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", color = Color.White)
                }
            }
        }
        if (transacciones.isEmpty()) {
            item { Text("No hay transacciones registradas.", color = Color.White.copy(0.5f), fontSize = 13.sp) }
        } else {
            items(transacciones, key = { it.id }) { t ->
                TransaccionCard(
                    t = t,
                    fmt = fmt,
                    onEditar = { transaccionEditar = it },
                    onEliminar = { transaccionEliminar = it },
                    onVerRecibo = { reciboAmpliado = t }
                )
            }
        }
    }

    if (mostrarNueva || transaccionEditar != null) {
        DialogoNuevaTransaccion(
            patientId = patientId,
            db = db,
            transaccion = transaccionEditar,
            onDismiss = {
                mostrarNueva = false
                transaccionEditar = null
            }
        )
    }

    if (transaccionEliminar != null) {
        DialogoConfirmarEliminar(
            titulo = "Eliminar transacción",
            mensaje = "¿Eliminar \"${transaccionEliminar!!.concepto}\" por $${transaccionEliminar!!.monto}? Esta acción no se puede deshacer.",
            onConfirmar = {
                scope.launch(Dispatchers.IO) {
                    db.transaccionDentalDao().eliminar(transaccionEliminar!!)
                    withContext(Dispatchers.Main) { transaccionEliminar = null }
                }
            },
            onDismiss = { transaccionEliminar = null }
        )
    }

    if (reciboAmpliado != null) {
        val context = LocalContext.current
        val bitmap = remember(reciboAmpliado!!.reciboUri) { cargarBitmapDesdeRutaOUri(context, reciboAmpliado!!.reciboUri) }
        AlertDialog(
            onDismissRequest = { reciboAmpliado = null },
            containerColor = Color(0xFF071C24),
            title = { Text("Recibo / presupuesto", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("No se pudo cargar la imagen", color = Color.White.copy(0.5f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { reciboAmpliado = null }) { Text("Cerrar", color = Color.White.copy(0.6f)) } }
        )
    }
}

@Composable
private fun StatFinanciero(titulo: String, monto: Double, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$%.2f".format(monto), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(titulo, color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun TransaccionCard(
    t: TransaccionDental,
    fmt: SimpleDateFormat,
    onEditar: (TransaccionDental) -> Unit,
    onEliminar: (TransaccionDental) -> Unit,
    onVerRecibo: () -> Unit
) {
    val color = when (t.tipo) {
        "INGRESO" -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
    }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.concepto, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${t.categoria} · ${fmt.format(Date(t.fecha))}", color = Color.White.copy(0.5f), fontSize = 11.sp)
                if (t.numeroDiente > 0) Text("Diente #${t.numeroDiente}", color = COLOR_DENTAL.copy(0.8f), fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${if (t.tipo == "INGRESO") "+" else "-"}$${t.monto}", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onEditar(t) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onEliminar(t) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
                if (t.reciboUri.isNotBlank()) {
                    IconButton(onClick = onVerRecibo, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = COLOR_DENTAL, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevaTransaccion(
    patientId: Int,
    db: AppDatabase,
    transaccion: TransaccionDental? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val esEdicion = transaccion != null
    var concepto by remember(transaccion?.id) { mutableStateOf(transaccion?.concepto ?: "") }
    var monto by remember(transaccion?.id) { mutableStateOf(transaccion?.monto?.toString() ?: "") }
    var tipo by remember(transaccion?.id) { mutableStateOf(transaccion?.tipo ?: "GASTO") }
    var categoria by remember(transaccion?.id) { mutableStateOf(transaccion?.categoria ?: "TRATAMIENTO") }
    var numeroDiente by remember(transaccion?.id) { mutableStateOf(transaccion?.numeroDiente?.toString() ?: "") }
    var reciboUri by remember(transaccion?.id) { mutableStateOf(transaccion?.reciboUri ?: "") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedCat by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, it)?.let { path ->
                    reciboUri = path
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                saveBitmapToInternalStorage(context, it)?.let { path ->
                    reciboUri = path
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text(if (esEdicion) "Editar transacción" else "Nueva transacción", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = it }) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTipo) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("INGRESO", "GASTO").forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = Color.White) }, onClick = { tipo = t; expandedTipo = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("VISITA", "TRATAMIENTO", "ORTODONCIA", "MEDICAMENTO", "PAGO A CUENTA", "OTRO").forEach { c ->
                            DropdownMenuItem(text = { Text(c, color = Color.White) }, onClick = { categoria = c; expandedCat = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = numeroDiente,
                    onValueChange = { numeroDiente = it.filter { c -> c.isDigit() } },
                    label = { Text("Diente FDI (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                Spacer(Modifier.height(4.dp))
                Text("Recibo / presupuesto", color = Color.White.copy(0.7f), fontSize = 12.sp)
                if (reciboUri.isBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Adjuntar", color = Color.White) }
                        OutlinedButton(
                            onClick = {
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> cameraLauncher.launch(null)
                                    else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Escanear", color = Color.White) }
                    }
                } else {
                    val reciboBitmap = remember(reciboUri) { cargarBitmapDesdeRutaOUri(context, reciboUri) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(8.dp)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (reciboBitmap != null) {
                                    Image(bitmap = reciboBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                } else {
                                    Text("Recibo", color = Color.White.copy(0.5f), fontSize = 10.sp)
                                }
                            }
                        }
                        TextButton(onClick = { reciboUri = "" }) { Text("Quitar recibo", color = Color(0xFFEF5350)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoVal = monto.toDoubleOrNull() ?: return@Button
                    scope.launch(Dispatchers.IO) {
                        val nueva = TransaccionDental(
                            id = transaccion?.id ?: 0,
                            patientId = patientId,
                            concepto = concepto,
                            categoria = categoria,
                            tipo = tipo,
                            monto = montoVal,
                            fecha = transaccion?.fecha ?: System.currentTimeMillis(),
                            numeroDiente = numeroDiente.toIntOrNull() ?: 0,
                            reciboUri = reciboUri
                        )
                        if (esEdicion) db.transaccionDentalDao().actualizar(nueva)
                        else db.transaccionDentalDao().insertar(nueva)
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

@Composable
private fun DialogoConfirmarEliminar(
    titulo: String,
    mensaje: String,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text(titulo, color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = { Text(mensaje, color = Color.White) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}
