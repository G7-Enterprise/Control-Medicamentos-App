package com.carlos.controlmedicamentos

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrtodonciaTab(
    patientId: Int,
    db: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ortodoncias by db.ortodonciaDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    var mostrarNueva by remember { mutableStateOf(false) }
    var seleccionada by remember { mutableStateOf<Ortodoncia?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = { mostrarNueva = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nuevo tratamiento de ortodoncia")
                }
            }
            if (ortodoncias.isEmpty()) {
                item {
                    Text(
                        "No hay tratamientos de ortodoncia registrados.",
                        color = Color.White.copy(0.5f), fontSize = 14.sp
                    )
                }
            } else {
                items(ortodoncias, key = { it.id }) { o ->
                    OrtodonciaCard(o, onClick = { seleccionada = o })
                }
            }
        }

        if (mostrarNueva) {
            DialogoNuevaOrtodoncia(
                patientId = patientId,
                db = db,
                onDismiss = { mostrarNueva = false }
            )
        }

        if (seleccionada != null) {
            ModalBottomSheet(
                onDismissRequest = { seleccionada = null },
                sheetState = sheetState,
                containerColor = Color(0xFF071C24),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) }
            ) {
                DetalleOrtodonciaContent(
                    patientId = patientId,
                    ortodoncia = seleccionada!!,
                    db = db,
                    onCerrar = { scope.launch { sheetState.hide(); seleccionada = null } }
                )
            }
        }
    }
}

@Composable
private fun OrtodonciaCard(ortodoncia: Ortodoncia, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${ortodoncia.tipo} ${if (ortodoncia.activo) "(Activo)" else ""}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (ortodoncia.activo) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF66BB6A)) {
                        Text("ACTIVO", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Inicio: ${fmt.format(Date(ortodoncia.fechaInicio))}", color = Color.White.copy(0.6f), fontSize = 12.sp)
            Text("Costo total: $${ortodoncia.costoTotal}  ·  Abonado: $${ortodoncia.abonoTotal}", color = Color.White.copy(0.6f), fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleOrtodonciaContent(
    patientId: Int,
    ortodoncia: Ortodoncia,
    db: AppDatabase,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }
    var pestana by remember { mutableStateOf(0) }
    var mostrarAjuste by remember { mutableStateOf(false) }
    var mostrarIncidencia by remember { mutableStateOf(false) }
    var mostrarElastico by remember { mutableStateOf(false) }

    val ajustes by db.ajusteOrtodonciaDao().observarPorOrtodoncia(ortodoncia.id).collectAsState(initial = emptyList())
    val incidencias by db.incidenciaOrtodonciaDao().observarPorOrtodoncia(ortodoncia.id).collectAsState(initial = emptyList())
    val elasticos by db.elasticoOrtodonciaDao().observarPorOrtodoncia(ortodoncia.id).collectAsState(initial = emptyList())
    val imagenes by db.imagenDentalDao().observarPorOrtodoncia(ortodoncia.id).collectAsState(initial = emptyList())

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, it)?.let { path ->
                    db.imagenDentalDao().insertar(
                        ImagenDental(patientId = patientId, ortodonciaId = ortodoncia.id, uri = path, tipo = "ORTODONCIA")
                    )
                }
            }
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                saveBitmapToInternalStorage(context, it)?.let { path ->
                    db.imagenDentalDao().insertar(
                        ImagenDental(patientId = patientId, ortodonciaId = ortodoncia.id, uri = path, tipo = "ORTODONCIA")
                    )
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePictureLauncher.launch(null) else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${ortodoncia.tipo}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, null, tint = Color.White.copy(0.6f)) }
        }
        Text("Inicio: ${fmt.format(Date(ortodoncia.fechaInicio))}", color = Color.White.copy(0.6f), fontSize = 12.sp)
        Text("Costo: $${ortodoncia.costoTotal}  ·  Abonado: $${ortodoncia.abonoTotal}  ·  Saldo: $${ortodoncia.costoTotal - ortodoncia.abonoTotal}", color = Color.White.copy(0.6f), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = pestana,
            containerColor = Color.Transparent,
            contentColor = COLOR_DENTAL,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pestana]),
                    color = COLOR_DENTAL
                )
            }
        ) {
            listOf("Ajustes", "Incidencias", "Elásticos", "Fotos").forEachIndexed { i, label ->
                Tab(
                    selected = pestana == i,
                    onClick = { pestana = i },
                    text = { Text(label, fontSize = 11.sp, color = if (pestana == i) COLOR_DENTAL else Color.White.copy(0.5f)) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (pestana) {
            0 -> AjustesPanel(ajustes, { mostrarAjuste = true }, fmt)
            1 -> IncidenciasPanel(incidencias, { mostrarIncidencia = true }, fmt)
            2 -> ElasticosPanel(elasticos, { mostrarElastico = true })
            3 -> FotosOrtodonciaPanel(imagenes, { pickImageLauncher.launch("image/*") }, {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> takePictureLauncher.launch(null)
                    else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            })
        }

        Spacer(Modifier.height(24.dp))
    }

    if (mostrarAjuste) DialogoNuevoAjuste(ortodonciaId = ortodoncia.id, db = db, onDismiss = { mostrarAjuste = false })
    if (mostrarIncidencia) DialogoNuevaIncidencia(ortodoncia = ortodoncia, db = db, onDismiss = { mostrarIncidencia = false })
    if (mostrarElastico) DialogoNuevoElastico(ortodonciaId = ortodoncia.id, db = db, onDismiss = { mostrarElastico = false })
}

@Composable
private fun AjustesPanel(ajustes: List<AjusteOrtodoncia>, onNuevo: () -> Unit, fmt: SimpleDateFormat) {
    Column {
        Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)) {
            Text("Registrar ajuste / apriete")
        }
        Spacer(Modifier.height(10.dp))
        if (ajustes.isEmpty()) {
            Text("No hay ajustes registrados.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            ajustes.forEach { a ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(fmt.format(Date(a.fecha)), color = Color.White.copy(0.5f), fontSize = 11.sp)
                        Text(a.descripcion, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Dolor: ${a.dolor}", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidenciasPanel(incidencias: List<IncidenciaOrtodoncia>, onNuevo: () -> Unit, fmt: SimpleDateFormat) {
    Column {
        Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))) {
            Text("Reportar incidencia")
        }
        Spacer(Modifier.height(10.dp))
        if (incidencias.isEmpty()) {
            Text("No hay incidencias registradas.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            incidencias.forEach { inc ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(inc.tipo, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (inc.resuelto) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF66BB6A)) { Text("Resuelto", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
                        }
                        Text(fmt.format(Date(inc.fecha)), color = Color.White.copy(0.5f), fontSize = 11.sp)
                        Text(inc.descripcion, color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ElasticosPanel(elasticos: List<ElasticoOrtodoncia>, onNuevo: () -> Unit) {
    Column {
        Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))) {
            Text("Añadir elástico")
        }
        Spacer(Modifier.height(10.dp))
        if (elasticos.isEmpty()) {
            Text("No hay elásticos registrados.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            elasticos.forEach { e ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${e.tipo}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${e.dienteOrigen} → ${e.dienteDestino}", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        if (e.activo) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF66BB6A)) { Text("Activo", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FotosOrtodonciaPanel(imagenes: List<ImagenDental>, onGaleria: () -> Unit, onCamara: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onGaleria, modifier = Modifier.weight(1f)) { Text("Galería", color = Color.White) }
            OutlinedButton(onClick = onCamara, modifier = Modifier.weight(1f)) { Text("Cámara", color = Color.White) }
        }
        Spacer(Modifier.height(10.dp))
        if (imagenes.isEmpty()) {
            Text("No hay fotos de seguimiento.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imagenes.forEach { img ->
                    val bitmap = remember(img.uri) { try { android.graphics.BitmapFactory.decodeFile(img.uri) } catch (_: Exception) { null } }
                    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(100.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                            else Text("Foto", color = Color.White.copy(0.5f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevaOrtodoncia(
    patientId: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tipo by remember { mutableStateOf("BRACKETS") }
    var fechaInicio by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date())) }
    var fechaFin by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nueva ortodoncia", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("BRACKETS", "INVISALIGN", "RETENEDOR", "OTRO").forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = Color.White) }, onClick = { tipo = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = fechaInicio,
                    onValueChange = { fechaInicio = it },
                    label = { Text("Fecha inicio (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = fechaFin,
                    onValueChange = { fechaFin = it },
                    label = { Text("Fecha fin estimada (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = costo,
                    onValueChange = { costo = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Costo total") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
                    val inicio = fmt.parse(fechaInicio)?.time ?: System.currentTimeMillis()
                    val fin = fechaFin.takeIf { it.isNotBlank() }?.let { fmt.parse(it)?.time }
                    val costoVal = costo.toDoubleOrNull() ?: 0.0
                    scope.launch(Dispatchers.IO) {
                        db.ortodonciaDao().insertar(
                            Ortodoncia(
                                patientId = patientId,
                                tipo = tipo,
                                fechaInicio = inicio,
                                fechaFinEstimada = fin,
                                costoTotal = costoVal,
                                notas = notas
                            )
                        )
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevoAjuste(
    ortodonciaId: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var descripcion by remember { mutableStateOf("") }
    var dolor by remember { mutableStateOf("LEVE") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nuevo ajuste", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = dolor,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nivel de dolor") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("NINGUNO", "LEVE", "MODERADO", "SEVERO").forEach { d ->
                            DropdownMenuItem(text = { Text(d, color = Color.White) }, onClick = { dolor = d; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (descripcion.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        db.ajusteOrtodonciaDao().insertar(
                            AjusteOrtodoncia(
                                ortodonciaId = ortodonciaId,
                                fecha = System.currentTimeMillis(),
                                descripcion = descripcion,
                                dolor = dolor
                            )
                        )
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevaIncidencia(
    ortodoncia: Ortodoncia,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tipo by remember { mutableStateOf("OTRO") }
    var descripcion by remember { mutableStateOf("") }
    var numeroDiente by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nueva incidencia", color = Color(0xFFFFA726), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFA726), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFA726), unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("BRACKET_DESPEGADO", "ALAMBRE_PUNZANTE", "GOMA_ROTA", "ULCERA", "OTRO").forEach { t ->
                            DropdownMenuItem(text = { Text(t.replace("_", " "), color = Color.White) }, onClick = { tipo = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFA726), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFA726), unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = numeroDiente,
                    onValueChange = { numeroDiente = it.filter { c -> c.isDigit() } },
                    label = { Text("Diente FDI (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFA726), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFA726), unfocusedLabelColor = Color.White.copy(0.5f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (descripcion.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        db.incidenciaOrtodonciaDao().insertar(
                            IncidenciaOrtodoncia(
                                ortodonciaId = ortodoncia.id,
                                patientId = ortodoncia.patientId,
                                numeroDiente = numeroDiente.toIntOrNull() ?: 0,
                                tipo = tipo,
                                descripcion = descripcion
                            )
                        )
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
            ) { Text("Reportar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

@Composable
private fun DialogoNuevoElastico(
    ortodonciaId: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tipo by remember { mutableStateOf("") }
    var origen by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nuevo elástico", color = Color(0xFF42A5F5), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (ej. Conejo, Clase II)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF42A5F5), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF42A5F5), unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = origen,
                    onValueChange = { origen = it.filter { c -> c.isDigit() } },
                    label = { Text("Diente origen (FDI)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF42A5F5), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF42A5F5), unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = destino,
                    onValueChange = { destino = it.filter { c -> c.isDigit() } },
                    label = { Text("Diente destino (FDI)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF42A5F5), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF42A5F5), unfocusedLabelColor = Color.White.copy(0.5f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val o = origen.toIntOrNull() ?: return@Button
                    val d = destino.toIntOrNull() ?: return@Button
                    scope.launch(Dispatchers.IO) {
                        db.elasticoOrtodonciaDao().insertar(
                            ElasticoOrtodoncia(
                                ortodonciaId = ortodonciaId,
                                dienteOrigen = o,
                                dienteDestino = d,
                                tipo = tipo
                            )
                        )
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}
