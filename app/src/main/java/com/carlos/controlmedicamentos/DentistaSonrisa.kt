package com.carlos.controlmedicamentos

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
internal fun SonrisaTab(
    patientId: Int,
    db: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale("es")) }

    val imagenes by db.imagenDentalDao().observarPorTipo(patientId, "SONRISA").collectAsState(initial = emptyList())

    var mostrarDialogo by remember { mutableStateOf(false) }
    var etapaNueva by remember { mutableStateOf("PROGRESO") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, it)?.let { path ->
                    db.imagenDentalDao().insertar(
                        ImagenDental(
                            patientId = patientId,
                            uri = path,
                            tipo = "SONRISA",
                            etapa = etapaNueva,
                            fecha = System.currentTimeMillis()
                        )
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
                        ImagenDental(
                            patientId = patientId,
                            uri = path,
                            tipo = "SONRISA",
                            etapa = etapaNueva,
                            fecha = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePictureLauncher.launch(null) else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    val antes = imagenes.filter { it.etapa.equals("ANTES", ignoreCase = true) }
    val despues = imagenes.filter { it.etapa.equals("DESPUES", ignoreCase = true) }

    var selectedAntes by remember { mutableStateOf<ImagenDental?>(null) }
    var selectedDespues by remember { mutableStateOf<ImagenDental?>(null) }

    LaunchedEffect(imagenes) {
        selectedAntes = antes.firstOrNull()
        selectedDespues = despues.lastOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Comparación antes / después", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        item {
            BeforeAfterSlider(selectedAntes, selectedDespues)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Línea de tiempo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { mostrarDialogo = true }, colors = ButtonDefaults.buttonColors(containerColor = COLOR_DENTAL)) {
                    Icon(Icons.Filled.AddAPhoto, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir", fontSize = 12.sp)
                }
            }
        }
        if (imagenes.isEmpty()) {
            item {
                Text("No hay fotos del diario de sonrisa. Captura una foto antes o después de tu tratamiento.", color = Color.White.copy(0.5f), fontSize = 13.sp)
            }
        } else {
            items(imagenes, key = { it.id }) { img ->
                SonrisaCard(img, fmt)
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevaSonrisa(
            onDismiss = { mostrarDialogo = false },
            onGaleria = { etapa ->
                etapaNueva = etapa
                pickLauncher.launch("image/*")
            },
            onCamara = { etapa ->
                etapaNueva = etapa
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    takePictureLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
}

@Composable
private fun BeforeAfterSlider(antes: ImagenDental?, despues: ImagenDental?) {
    var split by remember { mutableStateOf(0.5f) }
    val beforeBitmap = remember(antes?.uri) { antes?.let { try { android.graphics.BitmapFactory.decodeFile(it.uri) } catch (_: Exception) { null } } }
    val afterBitmap = remember(despues?.uri) { despues?.let { try { android.graphics.BitmapFactory.decodeFile(it.uri) } catch (_: Exception) { null } } }

    Column {
        if (beforeBitmap != null && afterBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A2530))
            ) {
                Image(
                    bitmap = beforeBitmap.asImageBitmap(),
                    contentDescription = "Antes",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(split)
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    Image(
                        bitmap = afterBitmap.asImageBitmap(),
                        contentDescription = "Después",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = split,
                onValueChange = { split = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = COLOR_DENTAL,
                    activeTrackColor = COLOR_DENTAL,
                    inactiveTrackColor = Color.White.copy(0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Antes", color = Color.White.copy(0.6f), fontSize = 12.sp)
                Text("Después", color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
        } else {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Selecciona o captura fotos 'Antes' y 'Después' para comparar.", color = Color.White.copy(0.5f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SonrisaCard(img: ImagenDental, fmt: SimpleDateFormat) {
    val bitmap = remember(img.uri) { try { android.graphics.BitmapFactory.decodeFile(img.uri) } catch (_: Exception) { null } }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071C24)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(80.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("Foto", color = Color.White.copy(0.5f), fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fmt.format(Date(img.fecha)), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Etapa: ${img.etapa}", color = COLOR_DENTAL, fontSize = 12.sp)
                if (img.notas.isNotBlank()) Text(img.notas, color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuevaSonrisa(
    onDismiss: () -> Unit,
    onGaleria: (String) -> Unit,
    onCamara: (String) -> Unit
) {
    var etapa by remember { mutableStateOf("PROGRESO") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF071C24),
        title = { Text("Nueva foto", color = COLOR_DENTAL, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = etapa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Etapa") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = COLOR_DENTAL, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = COLOR_DENTAL, unfocusedLabelColor = Color.White.copy(0.5f))
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF0A2530))) {
                        listOf("ANTES", "DESPUES", "PROGRESO").forEach { e ->
                            DropdownMenuItem(text = { Text(e, color = Color.White) }, onClick = { etapa = e; expanded = false })
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onGaleria(etapa); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Galería", color = Color.White)
                    }
                    OutlinedButton(onClick = { onCamara(etapa); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cámara", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.6f)) } }
    )
}
