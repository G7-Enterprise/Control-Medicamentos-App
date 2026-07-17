package com.carlos.controlmedicamentos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

internal fun colorEstadoDiente(estado: String?): Color = when (estado) {
    "TRATAMIENTO_PENDIENTE" -> Color(0xFFEF5350)
    "EN_TRATAMIENTO" -> Color(0xFFFFA726)
    "OBSERVACION" -> Color(0xFFFFEB3B)
    "ORTODONCIA" -> Color(0xFF42A5F5)
    "TRATAMIENTO_FINALIZADO" -> Color(0xFF66BB6A)
    "EMPASTE_COMPOSITO" -> Color(0xFF81D4FA)
    "EMPASTE_AMALGAMA" -> Color(0xFF9E9E9E)
    "ENDODONCIA" -> Color(0xFFFFB74D)
    "CORONA" -> Color(0xFFFFD54F)
    "CORONA_PORCELANA" -> Color(0xFFF5F5F5)
    "IMPLANTE" -> Color(0xFFAB47BC)
    "RECONSTRUCCION" -> Color(0xFF5C6BC0)
    "INJERTO_HUESO" -> Color(0xFFA1887F)
    "INJERTO_OTRO" -> Color(0xFF8D6E63)
    "PUENTE" -> Color(0xFF7E57C2)
    "PROTESIS_REMOVIBLE" -> Color(0xFFF06292)
    "PROTESIS_TOTAL" -> Color(0xFFEC407A)
    "CARILLA" -> Color(0xFFFFF9C4)
    "APARATOLOGIA" -> Color(0xFF26C6DA)
    "EXTRACCION" -> Color(0xFF424242)
    else -> Color(0xFF0A2530)
}

internal fun nombreEstadoDiente(estado: String?): String = when (estado) {
    "TRATAMIENTO_PENDIENTE" -> "Tratamiento pendiente"
    "EN_TRATAMIENTO" -> "En tratamiento"
    "OBSERVACION" -> "En observación"
    "ORTODONCIA" -> "Ortodoncia"
    "TRATAMIENTO_FINALIZADO" -> "Tratamiento finalizado"
    "EMPASTE_COMPOSITO" -> "Empaste composite"
    "EMPASTE_AMALGAMA" -> "Empaste amalgama"
    "ENDODONCIA" -> "Endodoncia"
    "CORONA" -> "Corona"
    "CORONA_PORCELANA" -> "Corona porcelana"
    "IMPLANTE" -> "Implante"
    "RECONSTRUCCION" -> "Reconstrucción"
    "INJERTO_HUESO" -> "Injerto de hueso"
    "INJERTO_OTRO" -> "Injerto otro material"
    "PUENTE" -> "Puente"
    "PROTESIS_REMOVIBLE" -> "Prótesis removible"
    "PROTESIS_TOTAL" -> "Prótesis total"
    "CARILLA" -> "Carilla"
    "APARATOLOGIA" -> "Aparatología"
    "EXTRACCION" -> "Extracción"
    else -> "Sano"
}

internal fun cargarBitmapDesdeRutaOUri(context: android.content.Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        when {
            uriString.startsWith("content://") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uriString))
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = false
                    }
                } else {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                }
            }
            uriString.startsWith("file://") -> {
                val path = Uri.parse(uriString).path ?: uriString
                android.graphics.BitmapFactory.decodeFile(path)
            }
            else -> android.graphics.BitmapFactory.decodeFile(uriString)
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OdontogramaInteractivoTab(
    patientId: Int,
    db: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val estadosDientes by db.dienteEstadoDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val diagnosticos by db.diagnosticoDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val procedimientos by db.procedimientoDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val transacciones by db.transaccionDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())
    val imagenes by db.imagenDentalDao().observarPorPaciente(patientId).collectAsState(initial = emptyList())

    var dienteSeleccionado by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    val imagenPendiente = remember { mutableStateOf<ImagenDental?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, it)?.let { path ->
                    val diente = dienteSeleccionado ?: 0
                    val nueva = ImagenDental(
                        patientId = patientId,
                        numeroDiente = diente,
                        uri = path,
                        tipo = if (diente == 0) "FOTO" else "RADIOGRAFIA",
                        fecha = System.currentTimeMillis()
                    )
                    db.imagenDentalDao().insertar(nueva)
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                saveBitmapToInternalStorage(context, it)?.let { path ->
                    val diente = dienteSeleccionado ?: 0
                    val nueva = ImagenDental(
                        patientId = patientId,
                        numeroDiente = diente,
                        uri = path,
                        tipo = if (diente == 0) "FOTO" else "RADIOGRAFIA",
                        fecha = System.currentTimeMillis()
                    )
                    db.imagenDentalDao().insertar(nueva)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    fun agregarImagenDesdeGaleria() = pickImageLauncher.launch("image/*")
    fun agregarImagenDesdeCamara() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> takePictureLauncher.launch(null)
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Leyenda", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        LeyendaItem("Sano", colorEstadoDiente("SANO"))
                        LeyendaItem("Empaste composite", colorEstadoDiente("EMPASTE_COMPOSITO"))
                        LeyendaItem("Empaste amalgama", colorEstadoDiente("EMPASTE_AMALGAMA"))
                        LeyendaItem("Endodoncia", colorEstadoDiente("ENDODONCIA"))
                        LeyendaItem("Corona", colorEstadoDiente("CORONA"))
                        LeyendaItem("Corona porcelana", colorEstadoDiente("CORONA_PORCELANA"))
                        LeyendaItem("Implante", colorEstadoDiente("IMPLANTE"))
                        LeyendaItem("Reconstrucción", colorEstadoDiente("RECONSTRUCCION"))
                        LeyendaItem("Injerto de hueso", colorEstadoDiente("INJERTO_HUESO"))
                        LeyendaItem("Injerto otro material", colorEstadoDiente("INJERTO_OTRO"))
                        LeyendaItem("Puente", colorEstadoDiente("PUENTE"))
                        LeyendaItem("Prótesis removible", colorEstadoDiente("PROTESIS_REMOVIBLE"))
                        LeyendaItem("Prótesis total", colorEstadoDiente("PROTESIS_TOTAL"))
                        LeyendaItem("Carilla", colorEstadoDiente("CARILLA"))
                        LeyendaItem("Aparatología", colorEstadoDiente("APARATOLOGIA"))
                        LeyendaItem("Tratamiento finalizado", colorEstadoDiente("TRATAMIENTO_FINALIZADO"))
                        LeyendaItem("En observación", colorEstadoDiente("OBSERVACION"))
                        LeyendaItem("En tratamiento", colorEstadoDiente("EN_TRATAMIENTO"))
                        LeyendaItem("Requiere tratamiento", colorEstadoDiente("TRATAMIENTO_PENDIENTE"))
                        LeyendaItem("Ortodoncia", colorEstadoDiente("ORTODONCIA"))
                        LeyendaItem("Extracción", colorEstadoDiente("EXTRACCION"))
                    }
                }
            }
            item {
                Text("Toca un diente para ver su expediente", color = Color.White.copy(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item {
                OdontogramaVisualInteractivo(
                    estados = estadosDientes,
                    seleccionado = dienteSeleccionado,
                    onDienteClick = { dienteSeleccionado = it }
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Resumen por estado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        val resumen = estadosDientes.groupBy { it.estado }.mapValues { it.value.size }
                        if (resumen.isEmpty()) {
                            Text("No hay estados registrados. Toca un diente para empezar.", color = Color.White.copy(0.5f), fontSize = 12.sp)
                        } else {
                            resumen.forEach { (estado, cantidad) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(colorEstadoDiente(estado)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${nombreEstadoDiente(estado)}: $cantidad", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dienteSeleccionado != null) {
            ModalBottomSheet(
                onDismissRequest = { dienteSeleccionado = null },
                sheetState = sheetState,
                containerColor = Color(0xFF071C24),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) }
            ) {
                DientePanelContent(
                    patientId = patientId,
                    numero = dienteSeleccionado ?: 0,
                    db = db,
                    estados = estadosDientes,
                    diagnosticos = diagnosticos,
                    procedimientos = procedimientos,
                    transacciones = transacciones,
                    imagenes = imagenes,
                    onCerrar = { scope.launch { sheetState.hide(); dienteSeleccionado = null } },
                    onAgregarImagenGaleria = { agregarImagenDesdeGaleria() },
                    onAgregarImagenCamara = { agregarImagenDesdeCamara() },
                    fmt = fmt
                )
            }
        }
    }
}

@Composable
private fun LeyendaItem(texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color).border(0.5.dp, Color.White.copy(0.2f), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = Color.White.copy(0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun OdontogramaVisualInteractivo(
    estados: List<DienteEstado>,
    seleccionado: Int?,
    onDienteClick: (Int) -> Unit
) {
    val cuadrantes = listOf(
        (18 downTo 11).toList(),
        (21..28).toList(),
        (38 downTo 31).toList(),
        (41..48).toList()
    )

    val odontogramaScrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SUPERIOR", color = Color.White.copy(0.4f), fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .horizontalScroll(odontogramaScrollState)
                        .width(520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        cuadrantes[0].forEach { num -> DienteBotonColor(num, estados, seleccionado, onDienteClick) }
                        Spacer(Modifier.width(6.dp))
                        cuadrantes[1].forEach { num -> DienteBotonColor(num, estados, seleccionado, onDienteClick) }
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color.White.copy(0.15f))
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        cuadrantes[2].forEach { num -> DienteBotonColor(num, estados, seleccionado, onDienteClick) }
                        Spacer(Modifier.width(6.dp))
                        cuadrantes[3].forEach { num -> DienteBotonColor(num, estados, seleccionado, onDienteClick) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("INFERIOR", color = Color.White.copy(0.4f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DienteBotonColor(
    numero: Int,
    estados: List<DienteEstado>,
    seleccionado: Int?,
    onClick: (Int) -> Unit
) {
    val esMolar = numero % 10 >= 6
    val size = if (esMolar) 32.dp else 28.dp
    val selected = seleccionado == numero
    val estado = estados.find { it.numeroDiente == numero }?.estado ?: "SANO"
    val color = colorEstadoDiente(estado)
    Box(
        modifier = Modifier
            .padding(1.dp)
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(if (selected) 1f else 0.65f))
            .border(1.dp, if (selected) Color.White else Color.White.copy(0.15f), RoundedCornerShape(6.dp))
            .clickable { onClick(numero) },
        contentAlignment = Alignment.Center
    ) {
        val textColor = when (estado) {
            "SANO", "EXTRACCION", "RECONSTRUCCION", "PUENTE", "PROTESIS_TOTAL", "INJERTO_OTRO" -> Color.White.copy(0.9f)
            else -> Color.Black
        }
        Text(
            text = if (estado == "EXTRACCION") "X" else (numero % 10).toString(),
            color = textColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DientePanelContent(
    patientId: Int,
    numero: Int,
    db: AppDatabase,
    estados: List<DienteEstado>,
    diagnosticos: List<DiagnosticoDental>,
    procedimientos: List<ProcedimientoDental>,
    transacciones: List<TransaccionDental>,
    imagenes: List<ImagenDental>,
    onCerrar: () -> Unit,
    onAgregarImagenGaleria: () -> Unit,
    onAgregarImagenCamara: () -> Unit,
    fmt: SimpleDateFormat
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pestana by remember { mutableStateOf(0) }
    var mostrarDialogoDiagnostico by remember { mutableStateOf(false) }
    var mostrarDialogoPago by remember { mutableStateOf(false) }
    var mostrarDialogoRecordatorio by remember { mutableStateOf(false) }

    val estadoActual = estados.find { it.numeroDiente == numero }
    val diagnosticosDiente = remember(diagnosticos, numero) { diagnosticos.filter { it.numeroDiente == numero } }
    val procedimientosDiente = remember(procedimientos, diagnosticosDiente) {
        val ids = diagnosticosDiente.map { it.id }.toSet()
        procedimientos.filter { it.diagnosticoId in ids }
    }
    val transaccionesDiente = remember(transacciones, numero) { transacciones.filter { it.numeroDiente == numero } }
    val imagenesDiente = remember(imagenes, numero) { imagenes.filter { it.numeroDiente == numero } }

    val costoTotal = procedimientosDiente.sumOf { it.costo ?: 0.0 }
    val pagadoTotal = transaccionesDiente.filter { it.tipo == "GASTO" }.sumOf { it.monto }
    val saldo = costoTotal - pagadoTotal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Diente FDI #$numero",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
            IconButton(onClick = onCerrar) {
                Icon(Icons.Filled.Close, null, tint = Color.White.copy(0.6f))
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colorEstadoDiente(estadoActual?.estado).copy(0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    nombreEstadoDiente(estadoActual?.estado),
                    color = colorEstadoDiente(estadoActual?.estado),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                var expandedEstado by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expandedEstado = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Cambiar estado", color = Color.White, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = expandedEstado,
                        onDismissRequest = { expandedEstado = false },
                        modifier = Modifier.background(Color(0xFF0A2530))
                    ) {
                        listOf("SANO", "TRATAMIENTO_PENDIENTE", "EN_TRATAMIENTO", "TRATAMIENTO_FINALIZADO", "OBSERVACION", "ORTODONCIA", "EMPASTE_COMPOSITO", "EMPASTE_AMALGAMA", "ENDODONCIA", "CORONA", "CORONA_PORCELANA", "IMPLANTE", "RECONSTRUCCION", "INJERTO_HUESO", "INJERTO_OTRO", "PUENTE", "PROTESIS_REMOVIBLE", "PROTESIS_TOTAL", "CARILLA", "APARATOLOGIA", "EXTRACCION").forEach { e ->
                            DropdownMenuItem(
                                text = { Text(nombreEstadoDiente(e), color = colorEstadoDiente(e), fontSize = 13.sp) },
                                onClick = {
                                    scope.launch {
                                        val nuevo = DienteEstado(
                                            id = estadoActual?.id ?: 0,
                                            patientId = patientId,
                                            numeroDiente = numero,
                                            estado = e
                                        )
                                        db.dienteEstadoDao().guardar(nuevo)
                                    }
                                    expandedEstado = false
                                }
                            )
                        }
                    }
                }
            }
        }

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
            listOf("Historial", "Imágenes", "Presupuesto", "Recordatorio").forEachIndexed { i, label ->
                Tab(
                    selected = pestana == i,
                    onClick = { pestana = i },
                    text = { Text(label, fontSize = 11.sp, color = if (pestana == i) COLOR_DENTAL else Color.White.copy(0.5f)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (pestana) {
            0 -> DienteHistorialPanel(
                diagnosticos = diagnosticosDiente,
                procedimientos = procedimientosDiente,
                fmt = fmt,
                onNuevoDiagnostico = { mostrarDialogoDiagnostico = true }
            )
            1 -> DienteImagenesPanel(
                imagenes = imagenesDiente,
                onGaleria = onAgregarImagenGaleria,
                onCamara = onAgregarImagenCamara
            )
            2 -> DientePresupuestoPanel(
                costoTotal = costoTotal,
                pagadoTotal = pagadoTotal,
                saldo = saldo,
                transacciones = transaccionesDiente,
                fmt = fmt,
                onNuevoPago = { mostrarDialogoPago = true }
            )
            3 -> {
                Button(
                    onClick = { mostrarDialogoRecordatorio = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
                ) {
                    Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Programar recordatorio analgésico")
                }
                Text(
                    "Sugerencia: después de extracciones, ajustes fuertes o cirugías, programa un analgésico/antiinflamatorio.",
                    color = Color.White.copy(0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (mostrarDialogoDiagnostico) {
        DialogoNuevoDiagnostico(
            patientId = patientId,
            numero = numero,
            db = db,
            onDismiss = { mostrarDialogoDiagnostico = false }
        )
    }

    if (mostrarDialogoPago) {
        DialogoNuevoPago(
            patientId = patientId,
            numero = numero,
            db = db,
            onDismiss = { mostrarDialogoPago = false }
        )
    }

    if (mostrarDialogoRecordatorio) {
        DialogoSugerirMedicamento(
            patientId = patientId,
            numero = numero,
            db = db,
            onDismiss = { mostrarDialogoRecordatorio = false }
        )
    }
}

@Composable
private fun DienteHistorialPanel(
    diagnosticos: List<DiagnosticoDental>,
    procedimientos: List<ProcedimientoDental>,
    fmt: SimpleDateFormat,
    onNuevoDiagnostico: () -> Unit
) {
    Column {
        Button(
            onClick = onNuevoDiagnostico,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nuevo diagnóstico / tratamiento")
        }
        Spacer(Modifier.height(10.dp))

        if (diagnosticos.isEmpty()) {
            Text("Sin historial registrado para este diente.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            diagnosticos.forEach { dx ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Circle, null, tint = when (dx.estado) {
                                "RESUELTO" -> Color(0xFF66BB6A)
                                "EN_TRATAMIENTO" -> Color(0xFFFFA726)
                                else -> Color(0xFFEF5350)
                            }, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dx.descripcion, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${fmt.format(Date(dx.fechaRegistro))} · ${dx.estado}", color = Color.White.copy(0.5f), fontSize = 11.sp)
                        val procs = procedimientos.filter { it.diagnosticoId == dx.id }
                        if (procs.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            procs.forEach { p ->
                                Text(
                                    "· ${p.tipo}${if (p.costo != null) " - $${p.costo}" else ""}${if (p.completado) " (completado)" else ""}",
                                    color = Color.White.copy(0.7f), fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DienteImagenesPanel(
    imagenes: List<ImagenDental>,
    onGaleria: () -> Unit,
    onCamara: () -> Unit
) {
    val context = LocalContext.current
    var imagenAmpliada by remember { mutableStateOf<ImagenDental?>(null) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onGaleria, modifier = Modifier.weight(1f)) { Text("Galería", color = Color.White) }
            OutlinedButton(onClick = onCamara, modifier = Modifier.weight(1f)) { Text("Cámara", color = Color.White) }
        }
        Spacer(Modifier.height(10.dp))
        if (imagenes.isEmpty()) {
            Text("No hay imágenes o radiografías para este diente.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imagenes.forEach { img ->
                    val bitmap = remember(img.uri) { cargarBitmapDesdeRutaOUri(context, img.uri) }
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(100.dp).clickable { imagenAmpliada = img }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("Imagen", color = Color.White.copy(0.5f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (imagenAmpliada != null) {
        val bitmap = remember(imagenAmpliada!!.uri) { cargarBitmapDesdeRutaOUri(context, imagenAmpliada!!.uri) }
        AlertDialog(
            onDismissRequest = { imagenAmpliada = null },
            containerColor = Color(0xFF071C24),
            title = { Text("Imagen dental", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("No se pudo cargar la imagen", color = Color.White.copy(0.5f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { imagenAmpliada = null }) { Text("Cerrar", color = Color.White.copy(0.6f)) } }
        )
    }
}

@Composable
private fun DientePresupuestoPanel(
    costoTotal: Double,
    pagadoTotal: Double,
    saldo: Double,
    transacciones: List<TransaccionDental>,
    fmt: SimpleDateFormat,
    onNuevoPago: () -> Unit
) {
    Column {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Resumen de la pieza", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                ResumenRow("Costo total tratamientos", costoTotal, Color.White)
                ResumenRow("Pagado / abonado", pagadoTotal, Color(0xFF66BB6A))
                ResumenRow("Saldo pendiente", saldo, if (saldo > 0) Color(0xFFEF5350) else Color(0xFF66BB6A))
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onNuevoPago,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
        ) {
            Text("Registrar pago / abono")
        }
        Spacer(Modifier.height(10.dp))
        if (transacciones.isEmpty()) {
            Text("No hay pagos registrados.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        } else {
            transacciones.forEach { t ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2530)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.concepto, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(fmt.format(Date(t.fecha)), color = Color.White.copy(0.5f), fontSize = 10.sp)
                        }
                        Text(
                            "${if (t.tipo == "INGRESO") "+" else "-"}$${t.monto}",
                            color = if (t.tipo == "INGRESO") Color(0xFF66BB6A) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenRow(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
        Text("$${value}", color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevoDiagnostico(
    patientId: Int,
    numero: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var descripcion by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("ACTIVO") }
    var tipoProcedimiento by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var expandedEstado by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }

    val tipos = listOf("Empaste", "Extracción", "Endodoncia", "Corona", "Limpieza", "Ortodoncia", "Cirugía", "Otro")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nuevo diagnóstico", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                ExposedDropdownMenuBox(expanded = expandedEstado, onExpandedChange = { expandedEstado = it }) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado del diagnóstico") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedEstado) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expandedEstado, onDismissRequest = { expandedEstado = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("ACTIVO", "EN_TRATAMIENTO", "RESUELTO").forEach { e ->
                            DropdownMenuItem(text = { Text(e, color = Color.White) }, onClick = { estado = e; expandedEstado = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = it }) {
                    OutlinedTextField(
                        value = tipoProcedimiento,
                        onValueChange = { tipoProcedimiento = it },
                        label = { Text("Procedimiento / tratamiento") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTipo) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        tipos.forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = Color.White) }, onClick = { tipoProcedimiento = t; expandedTipo = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = costo,
                    onValueChange = { costo = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Costo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (descripcion.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        val visitaId = 0 // Sin visita vinculada, se guarda como expediente del diente
                        val dx = DiagnosticoDental(
                            visitaId = visitaId,
                            patientId = patientId,
                            numeroDiente = numero,
                            descripcion = descripcion,
                            estado = estado
                        )
                        val dxId = db.diagnosticoDentalDao().insertar(dx).toInt()
                        if (tipoProcedimiento.isNotBlank()) {
                            val proc = ProcedimientoDental(
                                diagnosticoId = dxId,
                                patientId = patientId,
                                tipo = tipoProcedimiento,
                                costo = costo.toDoubleOrNull(),
                                completado = estado == "RESUELTO"
                            )
                            db.procedimientoDentalDao().insertar(proc)
                        }
                        // Actualizar estado visual del diente según diagnóstico
                        val estadoActual = db.dienteEstadoDao().obtener(patientId, numero)
                        val nuevoEstado = when (estado) {
                            "RESUELTO" -> "TRATAMIENTO_FINALIZADO"
                            "EN_TRATAMIENTO" -> "EN_TRATAMIENTO"
                            else -> "TRATAMIENTO_PENDIENTE"
                        }
                        db.dienteEstadoDao().guardar(
                            DienteEstado(
                                id = estadoActual?.id ?: 0,
                                patientId = patientId,
                                numeroDiente = numero,
                                estado = nuevoEstado
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

@Composable
private fun DialogoNuevoPago(
    patientId: Int,
    numero: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var concepto by remember { mutableStateOf("Pago diente #$numero") }
    var monto by remember { mutableStateOf("") }
    var reciboUri by remember { mutableStateOf("") }

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
        title = { Text("Registrar pago", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF66BB6A), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF66BB6A), unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF66BB6A), unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF66BB6A), unfocusedLabelColor = Color.White.copy(0.5f))
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
                        db.transaccionDentalDao().insertar(
                            TransaccionDental(
                                patientId = patientId,
                                concepto = concepto,
                                categoria = "TRATAMIENTO",
                                tipo = "GASTO",
                                monto = montoVal,
                                numeroDiente = numero,
                                reciboUri = reciboUri
                            )
                        )
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

@Composable
private fun DialogoSugerirMedicamento(
    patientId: Int,
    numero: Int,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var medicamento by remember { mutableStateOf("Paracetamol / Ibuprofeno") }
    var dosis by remember { mutableStateOf("1 tableta") }
    var frecuencia by remember { mutableStateOf("Cada 8 horas") }
    var duracion by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Programar recordatorio", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = medicamento,
                    onValueChange = { medicamento = it },
                    label = { Text("Medicamento") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text("Dosis") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text("Frecuencia") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
                OutlinedTextField(
                    value = duracion,
                    onValueChange = { duracion = it.filter { c -> c.isDigit() } },
                    label = { Text("Duración (días)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val frecuenciaHoras = Regex("\\d+").find(frecuencia)?.value?.toIntOrNull() ?: 8
                    val dias = duracion.toIntOrNull() ?: 3
                    val ahora = System.currentTimeMillis()
                    scope.launch(Dispatchers.IO) {
                        db.medicationDao().insertar(
                            Medication(
                                patientId = patientId,
                                nombre = medicamento,
                                dosis = "$dosis (diente #$numero)",
                                formato = "Analgésico",
                                formaMedicamento = "Oral",
                                presentacion = "Tableta",
                                concentracion = "500 mg",
                                fechaInicio = ahora,
                                fechaFin = ahora + dias * 24 * 60 * 60 * 1000L,
                                frecuenciaHoras = frecuenciaHoras,
                                esCicloCorto = false
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Recordatorio creado en el módulo principal", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)
            ) { Text("Crear recordatorio") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}

