package com.carlos.controlmedicamentos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.DiarioEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiarioScreen(
    patientId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (patientId <= 0) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF120F26)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Selecciona un usuario primero", color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onVolver) { Text("Volver") }
        }
        return
    }

    val entradas by database.diarioEntryDao().observarPorPaciente(patientId)
        .collectAsState(initial = emptyList())

    var texto by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var imagenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraPermissionPending by remember { mutableStateOf(false) }
    var mostrarVisor by remember { mutableStateOf(false) }
    var indiceEntradaVisor by remember { mutableStateOf(0) }
    var entradaAEliminar by remember { mutableStateOf<DiarioEntry?>(null) }
    var editandoEntrada by remember { mutableStateOf(false) }
    var textoEditado by remember { mutableStateOf("") }
    var imagenUriEditada by remember { mutableStateOf<Uri?>(null) }
    var imagenBitmapEditado by remember { mutableStateOf<Bitmap?>(null) }
    var tienePermisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fechaHoyStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    val inicioDelDia = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val finDelDia = inicioDelDia + 24 * 60 * 60 * 1000 - 1

    // Entrada de hoy si existe
    val entradaHoy = entradas.find { it.fecha in inicioDelDia..finDelDia }

    // Lanzador galería
    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imagenUri = it
            imagenBitmap = null
        }
    }

    // Lanzador cámara
    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            imagenBitmap = it
            imagenUri = null
        }
    }

    // Lanzador permiso cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        tienePermisoCamara = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted && cameraPermissionPending) {
            cameraPermissionPending = false
            camaraLauncher.launch()
        } else {
            cameraPermissionPending = false
        }
    }

    val colorP = Color(0xFF7B1FA2)
    val colorFondo = Color(0xFF0F0A25)
    val colorCard = Color(0xFF1A0F3C)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White)
            }
            Text("📝", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Diario Personal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Fecha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, "Fecha", tint = Color(0xFFCE93D8), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Hoy: $fechaHoyStr",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Entrada de hoy si existe
            if (entradaHoy != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✍️ Entrada de hoy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            DiarioPdfExporter.exportarDiarioAPdf(context, entradaHoy)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, "Exportar PDF", tint = Color(0xFFCE93D8), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { entradaAEliminar = entradaHoy },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(entradaHoy.texto, color = Color.White, fontSize = 14.sp)
                        if (!entradaHoy.rutaImagen.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            DiarioImage(
                                ruta = entradaHoy.rutaImagen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Campo de texto
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("¿Cómo te sientes hoy?", color = Color.White) },
                placeholder = { Text("Escribe tu entrada de hoy...", color = Color(0xFFAAAAAA)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorCard,
                    unfocusedContainerColor = colorCard,
                    focusedBorderColor = colorP,
                    unfocusedBorderColor = Color(0xFF3D2B6D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Preview imagen
            if (imagenUri != null || imagenBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2D1B69)),
                    contentAlignment = Alignment.Center
                ) {
                    val uriLocal = imagenUri
                    val bitmapLocal = imagenBitmap
                    val previewBitmap = when {
                        uriLocal != null -> loadUriAsBitmap(context, uriLocal)
                        bitmapLocal != null -> bitmapLocal
                        else -> null
                    }
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Vista previa",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("No se pudo cargar la imagen", color = Color(0xFFFF6B6B))
                    }
                    IconButton(
                        onClick = { imagenUri = null; imagenBitmap = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color(0x80000000), CircleShape)
                    ) {
                        Icon(Icons.Filled.Delete, "Quitar", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Botones foto
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { galeriaLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería", fontSize = 13.sp, color = Color.White)
                }
                Button(
                    onClick = {
                        if (tienePermisoCamara) {
                            camaraLauncher.launch()
                        } else {
                            cameraPermissionPending = true
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara", fontSize = 13.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botón guardar
            Button(
                onClick = {
                    if (texto.isBlank()) {
                        Toast.makeText(context, "Escribe algo en tu diario primero", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        val rutaImagen = guardarImagenDiario(context, imagenUri, imagenBitmap)

                        // Si ya existe entrada de hoy, eliminarla primero
                        entradaHoy?.let { database.diarioEntryDao().eliminar(it) }

                        database.diarioEntryDao().insertar(
                            DiarioEntry(
                                patientId = patientId,
                                fecha = System.currentTimeMillis(),
                                texto = texto,
                                rutaImagen = rutaImagen
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Diario guardado", Toast.LENGTH_SHORT).show()
                            texto = ""
                            imagenUri = null
                            imagenBitmap = null
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorP),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar entrada", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            // Historial
            if (entradas.isNotEmpty()) {
                Text(
                    "📖 Historial",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                entradas.forEach { entry ->
                    val fechaStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.fecha))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                indiceEntradaVisor = entradas.indexOf(entry)
                                mostrarVisor = true
                            },
                        colors = CardDefaults.cardColors(containerColor = colorCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4A148C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📝", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fechaStr, color = Color.White, fontSize = 12.sp)
                                Text(
                                    entry.texto.take(60) + if (entry.texto.length > 60) "..." else "",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        DiarioPdfExporter.exportarDiarioAPdf(context, entry)
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, "Exportar PDF", tint = Color(0xFFCE93D8), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { entradaAEliminar = entry }
                            ) {
                                Icon(Icons.Filled.Delete, "Eliminar", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Visor de entradas estilo libro/fotos
    if (mostrarVisor && entradas.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = indiceEntradaVisor,
            pageCount = { entradas.size }
        )

        LaunchedEffect(indiceEntradaVisor) {
            pagerState.animateScrollToPage(indiceEntradaVisor)
        }

        LaunchedEffect(pagerState.currentPage) {
            val entry = entradas[pagerState.currentPage]
            textoEditado = entry.texto
            imagenUriEditada = null
            imagenBitmapEditado = null
            editandoEntrada = false
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                mostrarVisor = false
                editandoEntrada = false
            },
            containerColor = colorCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${entradas.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row {
                        if (!editandoEntrada) {
                            IconButton(
                                onClick = { editandoEntrada = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Save, "Editar", tint = colorP, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = { mostrarVisor = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val entry = entradas[pageIndex]
                        val fechaStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.fecha))

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(fechaStr, color = Color(0xFFCE93D8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))

                            if (editandoEntrada && pageIndex == pagerState.currentPage) {
                                OutlinedTextField(
                                    value = textoEditado,
                                    onValueChange = { textoEditado = it },
                                    label = { Text("Editar texto", color = Color.White) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colorCard,
                                        unfocusedContainerColor = colorCard,
                                        focusedBorderColor = colorP,
                                        unfocusedBorderColor = Color(0xFF3D2B6D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                // Botones para cambiar imagen
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { galeriaLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Cambiar foto", fontSize = 12.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            if (tienePermisoCamara) {
                                                camaraLauncher.launch()
                                            } else {
                                                cameraPermissionPending = true
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Nueva foto", fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Preview de imagen editada
                                if (imagenUriEditada != null || imagenBitmapEditado != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2D1B69)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val uriLocal = imagenUriEditada
                                        val bitmapLocal = imagenBitmapEditado
                                        val previewBitmap = when {
                                            uriLocal != null -> loadUriAsBitmap(context, uriLocal)
                                            bitmapLocal != null -> bitmapLocal
                                            else -> null
                                        }
                                        if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = "Vista previa",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        IconButton(
                                            onClick = { imagenUriEditada = null; imagenBitmapEditado = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(Color(0x80000000), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Delete, "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            } else {
                                Text(entry.texto, fontSize = 14.sp, lineHeight = 20.sp)
                            }

                            if (!entry.rutaImagen.isNullOrBlank() && (editandoEntrada && pageIndex == pagerState.currentPage || imagenUriEditada == null && imagenBitmapEditado == null)) {
                                Spacer(Modifier.height(12.dp))
                                DiarioImage(
                                    ruta = entry.rutaImagen,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }

                            // Botones de acción en la parte inferior
                            if (!editandoEntrada || pageIndex != pagerState.currentPage) {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                DiarioPdfExporter.exportarDiarioAPdf(context, entry)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("PDF", fontSize = 12.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = { entradaAEliminar = entry },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Eliminar", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (editandoEntrada) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val entry = entradas[pagerState.currentPage]
                                val nuevaRutaImagen = guardarImagenDiario(context, imagenUriEditada, imagenBitmapEditado)
                                // Eliminar imagen anterior si existe y se cambió
                                if (nuevaRutaImagen != null && !entry.rutaImagen.isNullOrBlank()) {
                                    try { File(entry.rutaImagen).delete() } catch (_: Exception) { }
                                }
                                database.diarioEntryDao().actualizar(
                                    entry.copy(
                                        texto = textoEditado,
                                        rutaImagen = nuevaRutaImagen ?: entry.rutaImagen
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Entrada actualizada", Toast.LENGTH_SHORT).show()
                                    editandoEntrada = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorP)
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                } else {
                    Box {}
                }
            },
            dismissButton = {
                if (editandoEntrada) {
                    TextButton(onClick = { editandoEntrada = false }) {
                        Text("Cancelar", color = Color.White)
                    }
                } else {
                    Box {}
                }
            }
        )
    }

    // Diálogo de confirmación para eliminar
    entradaAEliminar?.let { entry ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { entradaAEliminar = null },
            containerColor = colorCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("¿Eliminar entrada?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Esta acción no se puede deshacer. ¿Estás seguro de que quieres eliminar esta entrada del diario?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            entry.rutaImagen?.let { ruta ->
                                try { File(ruta).delete() } catch (_: Exception) { }
                            }
                            database.diarioEntryDao().eliminar(entry)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Entrada eliminada", Toast.LENGTH_SHORT).show()
                                entradaAEliminar = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entradaAEliminar = null }) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun DiarioImage(ruta: String?, modifier: Modifier = Modifier) {
    if (ruta.isNullOrBlank()) return
    var bitmap by remember(ruta) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(ruta) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(ruta)?.asImageBitmap()
            } catch (_: Exception) { null }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Foto del diario",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun loadUriAsBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    } catch (_: Exception) { null }
}

private suspend fun guardarImagenDiario(context: Context, uri: Uri?, bitmap: Bitmap?): String? {
    if (uri == null && bitmap == null) return null

    val dir = File(context.filesDir, "diario_imagenes").apply { if (!exists()) mkdirs() }
    val fileName = "diario_${System.currentTimeMillis()}.jpg"
    val file = File(dir, fileName)

    return withContext(Dispatchers.IO) {
        try {
            when {
                uri != null -> {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                bitmap != null -> {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
