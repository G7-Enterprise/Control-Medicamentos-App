package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.backup.BackupSelection
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.ui.screens.StatisticsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.notifications.SignosVitalesScheduler
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme
import com.carlos.controlmedicamentos.backup.AutoBackupScheduler
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.zIndex

// Dead code — MedicamentoFormUI was removed to avoid DEX 197-register limit crash.
// The rest of this file contains live composables (FallAlertPanelManager, CicloMenstrualScreen, etc.).

@Composable
internal fun FallAlertPanelManager(
    mostrar: MutableState<Boolean>,
    patientId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    if (mostrar.value) {
        android.util.Log.d("MainActivity", "Renderizando FallAlertScreen")
        FallAlertScreen(
            patientId = patientId,
            database = database,
            onVolver = onVolver
        )
    }
}

@Composable
internal fun CicloMenstrualScreen(
    pacienteId: Int,
    database: AppDatabase,
    onVolver: () -> Unit,
    onIrAEmbarazo: () -> Unit = {}
) {
    var mostrarHistorial by remember { mutableStateOf(false) }

    if (mostrarHistorial) {
        HistorialCiclosScreen(
            pacienteId = pacienteId,
            database = database,
            onVolver = { mostrarHistorial = false }
        )
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ciclos by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(emptyList())
        else database.cicloMenstrualDao().observarPorPaciente(pacienteId)
    }.collectAsState(initial = emptyList())
    var fechaRegistro by remember { mutableStateOf(inicioDelDia(System.currentTimeMillis())) }
    var duracionPeriodo by remember { mutableStateOf("5") }
    var duracionCiclo by remember { mutableStateOf("28") }
    var sintomas by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var isSangradoSeleccionado by remember { mutableStateOf(false) }
    var isFlujoSeleccionado by remember { mutableStateOf(false) }
    var isDolorSeleccionado by remember { mutableStateOf(false) }
    var isMolestiaSeleccionada by remember { mutableStateOf(false) }
    var isEstadoSeleccionado by remember { mutableStateOf(false) }
    var isFiebreSeleccionada by remember { mutableStateOf(false) }
    var sintomaDialogoInicial by remember { mutableStateOf<String?>(null) }
    var mostrarAjustesCiclo by remember { mutableStateOf(false) }
    fun sintomasSeleccionadosTexto(): String {
        return listOfNotNull(
            "Sangrado".takeIf { isSangradoSeleccionado },
            "Flujo".takeIf { isFlujoSeleccionado },
            "Dolor".takeIf { isDolorSeleccionado },
            "Molestia".takeIf { isMolestiaSeleccionada },
            "Estado".takeIf { isEstadoSeleccionado },
            "Fiebre".takeIf { isFiebreSeleccionada },
            sintomas.trim().takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }
    fun abrirSelectorFecha(onFechaSeleccionada: ((Long) -> Unit)? = null) {
        val cal = Calendar.getInstance().apply { timeInMillis = fechaRegistro }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val fechaSeleccionada = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                fechaRegistro = fechaSeleccionada
                onFechaSeleccionada?.invoke(fechaSeleccionada)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    fun guardarPeriodo(fechaInicio: Long) {
        if (pacienteId <= 0) {
            Toast.makeText(context, "Selecciona primero un perfil", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            database.cicloMenstrualDao().guardar(
                CicloMenstrual(
                    patientId = pacienteId,
                    fechaInicio = fechaInicio,
                    duracionDias = duracionPeriodo.toIntOrNull()?.coerceIn(1, 12) ?: 5,
                    duracionCicloDias = duracionCiclo.toIntOrNull()?.coerceIn(15, 60) ?: 28,
                    sintomas = sintomasSeleccionadosTexto(),
                    notas = notas
                )
            )
            withContext(Dispatchers.Main) {
                sintomas = ""
                notas = ""
                isSangradoSeleccionado = false
                isFlujoSeleccionado = false
                isDolorSeleccionado = false
                isMolestiaSeleccionada = false
                isEstadoSeleccionado = false
                isFiebreSeleccionada = false
                Toast.makeText(context, "Periodo registrado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun finalizarSangrado() {
        val cicloActual = ciclos.firstOrNull() ?: return
        if (pacienteId <= 0) {
            Toast.makeText(context, "Selecciona primero un perfil", Toast.LENGTH_SHORT).show()
            return
        }
        val diasReales = ((inicioDelDia(System.currentTimeMillis()) - inicioDelDia(cicloActual.fechaInicio)) / (24L * 60L * 60L * 1000L)).toInt() + 1
        coroutineScope.launch(Dispatchers.IO) {
            database.cicloMenstrualDao().actualizar(
                cicloActual.copy(duracionDias = diasReales.coerceAtLeast(1))
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Sangrado finalizado: $diasReales días", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun abrirDialogoSintoma(tipo: String) {
        when (tipo) {
            "Sangrado intermenstrual" -> isSangradoSeleccionado = true
            "Flujo excesivo" -> isFlujoSeleccionado = true
            "Dolor / C?licos" -> isDolorSeleccionado = true
            "Molestia general" -> isMolestiaSeleccionada = true
            "Estado de ?nimo" -> isEstadoSeleccionado = true
            "Fiebre / Temperatura" -> isFiebreSeleccionada = true
        }
        sintomaDialogoInicial = tipo
    }
    val ultimoCiclo = ciclos.firstOrNull()
    val hoy = inicioDelDia(System.currentTimeMillis())
    val diaCiclo = ultimoCiclo?.let { ((hoy - inicioDelDia(it.fechaInicio)) / (24L * 60L * 60L * 1000L)).toInt() + 1 }
    val proximoPeriodo = ultimoCiclo?.let { moverFecha(it.fechaInicio, it.duracionCicloDias) }
    val ovulacionEstimada = ultimoCiclo?.let { moverFecha(it.fechaInicio, 14) }
    val diasHastaProximo = proximoPeriodo?.let {
        ((inicioDelDia(it) - hoy) / (24L * 60L * 60L * 1000L)).toInt()
    }
    val diasHastaOvulacion = ovulacionEstimada?.let {
        ((inicioDelDia(it) - hoy) / (24L * 60L * 60L * 1000L)).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE91E63), Color(0xFFFFF4FA), Color(0xFFFCE4EC))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFD81B60), Color(0xFFC218A8), Color(0xFFE91E63))))
                .statusBarsPadding()
                .padding(start = 16.dp, end = 24.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver", tint = Color.White)
            }
            Text("Ciclo menstrual", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "⚙",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier.clickable { mostrarAjustesCiclo = true }
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        if (ultimoCiclo == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("🌸", fontSize = 52.sp)
                    Text(
                        "Empieza a seguir tu ciclo",
                        color = Color(0xFFC2185B),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Registra el inicio de tu último periodo para ver predicciones, fase del ciclo y estadísticas.",
                        color = Color(0xFF77717A),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            abrirSelectorFecha { fechaSeleccionada ->
                                guardarPeriodo(fechaSeleccionada)
                            }
                            Toast.makeText(context, "Elige la fecha de inicio del periodo", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Registrar primer periodo", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9F1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("🩸", fontSize = 40.sp)
                        Column {
                            Text(
                                "Menstruacion",
                                color = Color(0xFFD32F2F),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Dia ${diaCiclo ?: 1} del ciclo",
                                color = Color(0xFFE57373),
                                fontSize = 16.sp
                            )
                        }
                    }
                    Text("Progreso del ciclo", color = Color(0xFFE57373), fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(((diaCiclo ?: 1).coerceIn(1, duracionCiclo.toIntOrNull() ?: 28)).toFloat() / (duracionCiclo.toIntOrNull() ?: 28).toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFE91E63))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFFC1DC))
                                .padding(12.dp)
                        ) {
                            Text("Proximo en ${diasHastaProximo ?: 28}d", color = Color(0xFFE91E63), fontWeight = FontWeight.Medium)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFEAD4E4))
                                .padding(12.dp)
                        ) {
                            Text("Ovulacion en ${diasHastaOvulacion ?: 14}d", color = Color(0xFF8E44AD), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8FC))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Calendario del ciclo", color = Color(0xFFAD1457), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((-3..24).toList()) { offset ->
                        val fecha = moverFecha(fechaRegistro, offset)
                        val fechaInicio = inicioDelDia(fecha)
                        val selected = fechaInicio == inicioDelDia(fechaRegistro)
                        val esPeriodo = ultimoCiclo != null && run {
                            val inicio = inicioDelDia(ultimoCiclo.fechaInicio)
                            val fin = inicio + (ultimoCiclo.duracionDias - 1) * 24L * 60L * 60L * 1000L
                            fechaInicio in inicio..fin
                        }
                        val bgColor = when {
                            selected -> Color(0xFFE91E63)
                            esPeriodo -> Color(0xFFFFC1DC)
                            else -> Color.Transparent
                        }
                        val borderColor = when {
                            selected || esPeriodo -> Color(0xFFE91E63)
                            else -> Color(0xFFE7BED3)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(SimpleDateFormat("EE", Locale("es", "ES")).format(Date(fecha)).take(2), color = Color.Gray, fontSize = 11.sp)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .border(1.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(fecha)), color = if (selected) Color.White else Color(0xFF4A3A46))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("● Periodo", color = Color(0xFFE91E63), fontSize = 12.sp)
                    Text("● Folicular", color = Color(0xFF90CAF9), fontSize = 12.sp)
                    Text("● Ovulación", color = Color(0xFFFF7043), fontSize = 12.sp)
                    Text("● Lútea", color = Color(0xFFCE93D8), fontSize = 12.sp)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEF6))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registrar nota de hoy", color = Color(0xFFAD1457), fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SymptomToggle(
                        icon = "🩸",
                        label = "Sangrado",
                        selected = isSangradoSeleccionado,
                        onClick = { abrirDialogoSintoma("Sangrado intermenstrual") }
                    )
                    SymptomToggle(
                        icon = "💧",
                        label = "Flujo",
                        selected = isFlujoSeleccionado,
                        onClick = { abrirDialogoSintoma("Flujo excesivo") }
                    )
                    SymptomToggle(
                        icon = "⚡",
                        label = "Dolor",
                        selected = isDolorSeleccionado,
                        onClick = { abrirDialogoSintoma("Dolor / Cólicos") }
                    )
                    SymptomToggle(
                        icon = "😰",
                        label = "Molestia",
                        selected = isMolestiaSeleccionada,
                        onClick = { abrirDialogoSintoma("Molestia general") }
                    )
                    SymptomToggle(
                        icon = "😊",
                        label = "Estado",
                        selected = isEstadoSeleccionado,
                        onClick = { abrirDialogoSintoma("Estado de ánimo") }
                    )
                    SymptomToggle(
                        icon = "🌡️",
                        label = "Fiebre",
                        selected = isFiebreSeleccionada,
                        onClick = { abrirDialogoSintoma("Fiebre / Temperatura") }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ultimoCiclo != null) {
                Button(
                    onClick = { finalizarSangrado() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF880E4F)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("⏹ El sangrado terminó hoy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = {
                    fechaRegistro = inicioDelDia(System.currentTimeMillis())
                    guardarPeriodo(fechaRegistro)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("✓ El periodo empezó hoy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    abrirSelectorFecha { fechaSeleccionada ->
                        guardarPeriodo(fechaSeleccionada)
                    }
                    Toast.makeText(context, "Elige la fecha de inicio del periodo", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFE91E63))
            ) {
                Text("+  Registrar con fecha y detalles", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onIrAEmbarazo() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF8E24AA))
            ) {
                Text("🤰 Iniciar seguimiento", fontWeight = FontWeight.Bold)
            }
        }

        if (ciclos.isNotEmpty()) {
            Button(
                onClick = { mostrarHistorial = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAD1457)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                    Text("Ver historial de ciclos (${ciclos.size} registros)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    sintomaDialogoInicial?.let { tipoInicial ->
        RegistrarSintomaDialog(
            tipoInicial = tipoInicial,
            fechaInicial = fechaRegistro,
            onDismiss = { sintomaDialogoInicial = null },
            onGuardar = { fecha, tipo, intensidad, estadoAnimo, nota ->
                if (pacienteId <= 0) {
                    Toast.makeText(context, "Selecciona primero un perfil", Toast.LENGTH_SHORT).show()
                    sintomaDialogoInicial = null
                    return@RegistrarSintomaDialog
                }
                if (ultimoCiclo == null) {
                    fechaRegistro = fecha
                    sintomas = tipo
                    notas = nota
                    sintomaDialogoInicial = null
                    Toast.makeText(context, "Registra primero el inicio de tu periodo", Toast.LENGTH_SHORT).show()
                    return@RegistrarSintomaDialog
                }
                coroutineScope.launch(Dispatchers.IO) {
                    val fechaTexto = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(fecha))
                    val sintomaTexto = listOf("$fechaTexto: $tipo", intensidad, estadoAnimo)
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    val sintomasActualizados = if (ultimoCiclo.sintomas.isBlank()) sintomaTexto
                        else "${ultimoCiclo.sintomas}\n$sintomaTexto"
                    val notasActualizadas = when {
                        nota.isBlank() -> ultimoCiclo.notas
                        ultimoCiclo.notas.isBlank() -> nota
                        else -> "${ultimoCiclo.notas}\n$nota"
                    }
                    database.cicloMenstrualDao().actualizar(
                        ultimoCiclo.copy(
                            sintomas = sintomasActualizados,
                            notas = notasActualizadas
                        )
                    )
                    withContext(Dispatchers.Main) {
                        sintomaDialogoInicial = null
                        Toast.makeText(context, "Nota registrada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    if (mostrarAjustesCiclo) {
        AjustesCicloDialog(
            duracionCicloInicial = duracionCiclo,
            duracionSangradoInicial = duracionPeriodo,
            onDismiss = { mostrarAjustesCiclo = false },
            onGuardar = { nuevaDuracionCiclo, nuevaDuracionSangrado ->
                duracionCiclo = nuevaDuracionCiclo
                duracionPeriodo = nuevaDuracionSangrado
                mostrarAjustesCiclo = false
                Toast.makeText(context, "Ajustes del ciclo guardados", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AjustesCicloDialog(
    duracionCicloInicial: String,
    duracionSangradoInicial: String,
    onDismiss: () -> Unit,
    onGuardar: (String, String) -> Unit
) {
    var duracionCicloLocal by remember { mutableStateOf(duracionCicloInicial) }
    var duracionSangradoLocal by remember { mutableStateOf(duracionSangradoInicial) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF292932))
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Ajustes del ciclo", color = Color(0xFFE91E63), fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Estos valores se usaran como referencia para las predicciones.",
                    color = Color(0xFFAAA7B3),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                OutlinedTextField(
                    value = duracionCicloLocal,
                    onValueChange = { duracionCicloLocal = it.filter(Char::isDigit).take(2) },
                    label = { Text("Duracion del ciclo (dias)", color = Color(0xFFE2DFE8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White)
                )
                OutlinedTextField(
                    value = duracionSangradoLocal,
                    onValueChange = { duracionSangradoLocal = it.filter(Char::isDigit).take(2) },
                    label = { Text("Duracion del sangrado (dias)", color = Color(0xFFE2DFE8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFFC4D1FF), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            onGuardar(
                                duracionCicloLocal.toIntOrNull()?.coerceIn(15, 60)?.toString() ?: "28",
                                duracionSangradoLocal.toIntOrNull()?.coerceIn(1, 12)?.toString() ?: "5"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Guardar", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrarSintomaDialog(
    tipoInicial: String,
    fechaInicial: Long,
    onDismiss: () -> Unit,
    onGuardar: (Long, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var fecha by remember { mutableStateOf(fechaInicial) }
    var tipoSeleccionado by remember(tipoInicial) { mutableStateOf(tipoInicial) }
    var intensidadSeleccionada by remember { mutableStateOf("Leve") }
    var estadoAnimoSeleccionado by remember { mutableStateOf("") }
    var temperaturaGrados by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val esFiebre = tipoSeleccionado.contains("Fiebre", ignoreCase = true)
    val tipos = listOf(
        "i Sangrado intermenstrual",
        "i Flujo excesivo",
        "i Dolor / Colicos",
        "i Molestia general",
        "i Estado de animo",
        "i Fiebre / Temperatura",
        "i OTRO"
    )
    val intensidades = listOf("Leve", "Moderada", "Intensa")
    val estados = listOf("i Muy bien", "i Bien", "i Neutral", "i Mal", "i Muy mal", "i Irritable", "i Ansiosa")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Registrar nota", color = Color(0xFFC2185B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF06292))
                )
                Text("Fecha", color = Color.Black, fontSize = 16.sp)
                Button(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = fecha }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                fecha = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFF9EA0AE))
                ) {
                    Text("i  ${SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(Date(fecha))}")
                }
                Text("Tipo de nota", color = Color.Black, fontSize = 16.sp)
                for (row in tipos.chunked(2)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            val clean = item.substringAfter(' ')
                            SelectablePill(
                                text = item,
                                selected = tipoSeleccionado == clean,
                                onClick = { tipoSeleccionado = clean },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (esFiebre) {
                    Text("Temperatura (°C)", color = Color.Black, fontSize = 16.sp)
                    OutlinedTextField(
                        value = temperaturaGrados,
                        onValueChange = { input ->
                            temperaturaGrados = input.filter { it.isDigit() || it == '.' }.take(4)
                        },
                        label = { Text("Ej: 37.5, 38.2", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("°C") }
                    )
                } else {
                    Text("Intensidad", color = Color.Black, fontSize = 16.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        intensidades.forEach { intensidad ->
                            SelectablePill(
                                text = intensidad,
                                selected = intensidadSeleccionada == intensidad,
                                onClick = { intensidadSeleccionada = intensidad },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text("Estado de animo (opcional)", color = Color.Black, fontSize = 16.sp)
                for (row in estados.chunked(3)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { estado ->
                            SelectablePill(
                                text = estado,
                                selected = estadoAnimoSeleccionado == estado,
                                onClick = { estadoAnimoSeleccionado = if (estadoAnimoSeleccionado == estado) "" else estado },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (opcional)", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF9AB8FF)),
                        border = BorderStroke(1.dp, Color(0xFF9EA0AE))
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val intensidadFinal = if (esFiebre && temperaturaGrados.isNotBlank()) {
                                "$temperaturaGrados°C"
                            } else {
                                intensidadSeleccionada
                            }
                            onGuardar(fecha, tipoSeleccionado, intensidadFinal, estadoAnimoSeleccionado, nota)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF454A63) else Color.White,
            contentColor = if (selected) Color.White else Color.Black
        ),
        border = BorderStroke(1.dp, if (selected) Color(0xFF454A63) else Color(0xFF9EA0AE)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, textAlign = TextAlign.Center, color = if (selected) Color.White else Color.Black)
    }
}

@Composable
private fun SymptomToggle(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFFFC1DC) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color(0xFFE91E63) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp,
            modifier = Modifier.graphicsLayer { alpha = if (selected) 1f else 0.55f }
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = if (selected) Color(0xFFE91E63) else Color(0xFF7B1B4D),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
internal fun DateSelector(
    label: String,
    selectedDate: Long,
    onPickDate: () -> Unit
) {
    OutlinedTextField(
        value = formatDate(selectedDate),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Button(onClick = onPickDate) {
                Text("Elegir")
            }
        }
    )
}

internal fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

internal fun formatDateMain(timestamp: Long): String = formatDate(timestamp)

private const val PATIENT_BIRTHDAY_PREFS = "patient_birthday_prefs"

internal fun savePersistedBirthday(context: Context, patientId: Int, birthDate: Long) {
    if (patientId <= 0 || birthDate <= 0L) return
    context.getSharedPreferences(PATIENT_BIRTHDAY_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong("birthday_$patientId", birthDate)
    .commit()
}

internal fun clearPersistedBirthday(context: Context, patientId: Int) {
    if (patientId <= 0) return
    context.getSharedPreferences(PATIENT_BIRTHDAY_PREFS, Context.MODE_PRIVATE)
        .edit()
        .remove("birthday_$patientId")
        .commit()
}

internal fun resolvePersistedBirthday(
    context: Context,
    patientId: Int?,
    inMemoryBirthday: Long?
): Long? {
    val persisted = patientId?.let { loadPersistedBirthday(context, it) }
    return persisted ?: inMemoryBirthday?.takeIf { it > 0L }
}

internal fun loadPersistedBirthday(context: Context, patientId: Int): Long? {
    if (patientId <= 0) return null
    val saved = context.getSharedPreferences(PATIENT_BIRTHDAY_PREFS, Context.MODE_PRIVATE)
        .getLong("birthday_$patientId", 0L)
    return saved.takeIf { it > 0L }
}

internal fun applyPersistedBirthdayFallback(context: Context, profile: PatientProfile?): PatientProfile? {
    profile ?: return null
    val persistedBirthday = resolvePersistedBirthday(
        context = context,
        patientId = profile.id,
        inMemoryBirthday = profile.fechaNacimiento
    )
    val edadResuelta = profile.edad.ifBlank {
        persistedBirthday?.let { calcularEdadDesdeNacimiento(it).toString() }.orEmpty()
    }
    return if (persistedBirthday == null && edadResuelta == profile.edad) {
        profile
    } else {
        profile.copy(
            fechaNacimiento = persistedBirthday ?: profile.fechaNacimiento,
            edad = edadResuelta
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

internal fun formatDateTimeMain(timestamp: Long): String = formatDateTime(timestamp)

internal fun yearMonthKey(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
}

internal fun formatYearMonthLabel(timestamp: Long): String {
    val meses = arrayOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${meses[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

internal fun formatReminderMinutesLabel(minutes: Int): String {
    return when {
        minutes >= 1440 && minutes % 1440 == 0 -> {
            val days = minutes / 1440
            if (days == 1) "1 dia antes" else "$days dias antes"
        }

        minutes >= 60 && minutes % 60 == 0 -> {
            val hours = minutes / 60
            if (hours == 1) "1 hora antes" else "$hours horas antes"
        }

        minutes == 1 -> "1 minuto antes"
        else -> "$minutes minutos antes"
    }
}

private fun siguienteHoraDisponible(baseTime: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = baseTime
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.HOUR_OF_DAY, 1)
    }.timeInMillis
}

internal fun timestampArchivo(timestamp: Long = System.currentTimeMillis()): String {
    return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(timestamp))
}

private fun frecuenciaBackupLabel(frequency: String): String {
    return when (frequency) {
        AutoBackupScheduler.FREQUENCY_DAILY -> "Diario"
        AutoBackupScheduler.FREQUENCY_WEEKLY -> "Semanal"
        else -> "Solo manual"
    }
}

internal fun frecuenciaBackupLabelMain(frequency: String): String = frecuenciaBackupLabel(frequency)

internal fun calcularEdadDesdeNacimiento(fechaNacimiento: Long, referencia: Long = System.currentTimeMillis()): Int {
    val nacimiento = Calendar.getInstance().apply { timeInMillis = fechaNacimiento }
    val hoy = Calendar.getInstance().apply { timeInMillis = referencia }
    var edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)

    if (
        hoy.get(Calendar.MONTH) < nacimiento.get(Calendar.MONTH) ||
        (hoy.get(Calendar.MONTH) == nacimiento.get(Calendar.MONTH) &&
            hoy.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH))
    ) {
        edad -= 1
    }

    return edad.coerceAtLeast(0)
}

internal fun formatDashboardDate(timestamp: Long): String {
    return SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date(timestamp))
}

internal fun formatHour(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

internal fun formatHourMain(timestamp: Long): String = formatHour(timestamp)

internal fun cicloToHours(ciclo: String): Int {
    return when (ciclo) {
        "Cada 8 horas" -> 8
        "Cada 12 horas" -> 12
        "Diario" -> 24
        "Semanal" -> 24 * 7
        "Mensual" -> 24 * 30
        else -> 0
    }
}

internal fun hoursToCycle(hours: Int): String {
    return when (hours) {
        8 -> "Cada 8 horas"
        12 -> "Cada 12 horas"
        24 -> "Diario"
        24 * 7 -> "Semanal"
        24 * 30 -> "Mensual"
        else -> "Personalizado"
    }
}

internal fun medicationToVademecum(medication: Medication): VademecumMedication {
    return VademecumMedication(
        nombre = medication.nombre,
        formatos = listOf(medication.formato.ifBlank { "No indicado" }),
        presentaciones = listOf(medication.presentacion.ifBlank { "No indicada" }),
        concentraciones = listOf(medication.concentracion.ifBlank { "No indicada" })
    )
}

internal fun sonMedicamentosDuplicados(
    existente: Medication,
    nombre: String,
    formato: String,
    concentracion: String
): Boolean {
    return existente.nombre.trim().equals(nombre.trim(), ignoreCase = true) &&
        existente.formato.trim().equals(formato.trim(), ignoreCase = true) &&
        existente.concentracion.trim().equals(concentracion.trim(), ignoreCase = true)
}

internal fun duplicateSignature(medication: Medication): String {
    return listOf(
        medication.nombre.trim().lowercase(Locale.getDefault()),
        medication.formato.trim().lowercase(Locale.getDefault()),
        medication.concentracion.trim().lowercase(Locale.getDefault())
    ).joinToString("|")
}

internal fun notificationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

internal fun exactAlarmPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.canScheduleExactAlarms() ?: true
    } else {
        true
    }
}

internal fun notificationPolicyAccessGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.isNotificationPolicyAccessGranted ?: true
    } else {
        true
    }
}

internal fun fullScreenIntentPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.canUseFullScreenIntent() ?: true
    } else {
        true
    }
}

internal fun buildExactAlarmPermissionIntent(context: Context): Intent {
    val packageUri = Uri.parse("package:${context.packageName}")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }
}

internal fun buildNotificationPolicyAccessIntent(): Intent {
    return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}

internal fun buildFullScreenIntentPermissionIntent(context: Context): Intent {
    val packageUri = Uri.parse("package:${context.packageName}")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }
}

internal fun buildRingtonePickerIntent(alarmaSonidoUri: String): Intent {
    val existingUri = when {
        alarmaSonidoUri == NotificacionHelper.SILENT_SOUND_URI -> null
        alarmaSonidoUri.isBlank() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else -> Uri.parse(alarmaSonidoUri)
    }

    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
    }
}

internal fun resolveAlarmSoundLabel(context: Context, alarmaSonidoUri: String): String {
    return when {
        alarmaSonidoUri == NotificacionHelper.SILENT_SOUND_URI -> "Alarma predeterminada"
        alarmaSonidoUri.isBlank() -> "Alarma predeterminada"
        else -> resolveRingtoneTitle(context, Uri.parse(alarmaSonidoUri))
    }
}

internal fun normalizarReintentoCritico(valor: Int): Int {
    return when {
        valor <= 5 -> 5
        valor <= 10 -> 10
        else -> 15
    }
}

internal fun resolveRingtoneTitle(context: Context, uri: Uri): String {
    return try {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)?.takeIf { it.isNotBlank() }
            ?: "Sonido personalizado"
    } catch (_: Exception) {
        "Sonido personalizado"
    }
}

internal fun cameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private const val STUDY_ATTACHMENTS_DIR = "study_attachments"
private const val STUDY_IMAGE_MAX_SIZE = 2400

internal fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
    return writeBitmapAttachment(context, bitmap, "study_capture")
}

internal fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val mimeType = context.contentResolver.getType(sourceUri).orEmpty()
        if (mimeType.startsWith("image/")) {
            val bitmap = loadBitmapFromUri(context, sourceUri, STUDY_IMAGE_MAX_SIZE)
                ?: return null
            writeBitmapAttachment(context, bitmap, "study_import")
        } else {
            val extension = resolveImportedAttachmentExtension(sourceUri, mimeType)
            val file = createStudyAttachmentFile(context, "study_import", extension)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(file).toString()
        }
    } catch (_: Exception) {
        null
    }
}

private fun loadBitmapFromUri(context: Context, sourceUri: Uri, targetSize: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longestSide = maxOf(info.size.width, info.size.height)
                val sampleSize = (longestSide / targetSize).coerceAtLeast(1)
                decoder.setTargetSampleSize(sampleSize)
                decoder.isMutableRequired = false
            }
        } else {
            decodeAttachmentBitmapLegacy(context, sourceUri, targetSize)
        }
    } catch (_: Exception) {
        decodeAttachmentBitmapLegacy(context, sourceUri, targetSize)
    }
}

private fun writeBitmapAttachment(context: Context, bitmap: Bitmap, prefix: String): String? {
    return try {
        val file = createStudyAttachmentFile(context, prefix, "jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }
        Uri.fromFile(file).toString()
    } catch (_: Exception) {
        null
    }
}

private fun createStudyAttachmentFile(context: Context, prefix: String, extension: String): File {
    val directory = File(context.filesDir, STUDY_ATTACHMENTS_DIR).apply { mkdirs() }
    return File(directory, "${prefix}_${System.currentTimeMillis()}.$extension")
}

private fun resolveImportedAttachmentExtension(sourceUri: Uri, mimeType: String): String {
    val mimeExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    if (!mimeExtension.isNullOrBlank()) {
        return mimeExtension.lowercase()
    }

    val uriExtension = sourceUri.lastPathSegment
        ?.substringAfterLast('.', "")
        ?.lowercase()
        .orEmpty()
    return uriExtension.ifBlank { "bin" }
}

internal fun enqueueAttachmentForReport(
    context: Context,
    currentAttachments: MutableList<String>,
    pendingReplacements: MutableList<PendingAttachmentReplacement>,
    newAttachmentPath: String
) {
    val duplicatePath = findDuplicateAttachmentPath(context, currentAttachments, newAttachmentPath)
    if (duplicatePath == null) {
        if (!currentAttachments.contains(newAttachmentPath)) {
            currentAttachments.add(newAttachmentPath)
        }
        return
    }

    if (pendingReplacements.none { it.newPath == newAttachmentPath }) {
        pendingReplacements.add(
            PendingAttachmentReplacement(
                existingPath = duplicatePath,
                newPath = newAttachmentPath,
                displayName = attachmentDisplayName(duplicatePath, currentAttachments.indexOf(duplicatePath).coerceAtLeast(0))
            )
        )
    }
}

private fun findDuplicateAttachmentPath(
    context: Context,
    currentAttachments: List<String>,
    candidatePath: String
): String? {
    val candidateFingerprint = attachmentFingerprint(context, candidatePath) ?: return null
    return currentAttachments.firstOrNull { existingPath ->
        existingPath != candidatePath && attachmentFingerprint(context, existingPath) == candidateFingerprint
    }
}

private fun attachmentFingerprint(context: Context, path: String): String? {
    val bytes = readAttachmentBytes(context, path) ?: return null
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun readAttachmentBytes(context: Context, path: String): ByteArray? {
    return try {
        val uri = Uri.parse(path)
        when {
            uri.scheme.equals("file", ignoreCase = true) -> {
                val localPath = uri.path ?: return null
                File(localPath).takeIf { it.exists() }?.readBytes()
            }
            uri.scheme.isNullOrBlank() && path.startsWith("/") -> File(path).takeIf { it.exists() }?.readBytes()
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    } catch (_: Exception) {
        null
    }
}

internal fun deleteAttachmentFile(path: String) {
    runCatching {
        val uri = Uri.parse(path)
        val localPath = when {
            uri.scheme.equals("file", ignoreCase = true) -> uri.path
            uri.scheme.isNullOrBlank() && path.startsWith("/") -> path
            else -> null
        }
        if (!localPath.isNullOrBlank()) {
            File(localPath).takeIf { it.exists() }?.delete()
        }
    }
}

internal fun decodeAttachmentPaths(paths: String): List<String> {
    return paths.split("|").filter { it.isNotBlank() }
}

internal fun attachmentDisplayName(path: String, index: Int): String {
    return Uri.parse(path).lastPathSegment ?: "Estudio ${index + 1}"
}

@Composable
internal fun AttachmentThumbnail(
    path: String,
    modifier: Modifier = Modifier.size(88.dp),
    onClick: () -> Unit,
    contentScale: ContentScale = ContentScale.Crop,
    targetSize: Int = 512
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            decodeAttachmentBitmap(context, path, targetSize)
        }
    }

    Card(modifier = modifier.clickable(onClick = onClick)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Adjunto del informe",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Text("Sin vista previa")
            }
        }
    }
}

@Composable
internal fun AttachmentFullscreenViewer(
    visor: AttachmentViewerState,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var scale by remember(visor.currentPath) { mutableFloatStateOf(1f) }
    var offsetX by remember(visor.currentPath) { mutableFloatStateOf(0f) }
    var offsetY by remember(visor.currentPath) { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF120F26))
        ) {
            AttachmentThumbnail(
                path = visor.currentPath,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 72.dp, horizontal = 12.dp)
                    .pointerInput(visor.currentPath) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = nextScale
                            if (nextScale <= 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                onClick = {},
                contentScale = ContentScale.Fit,
                targetSize = 2048
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = attachmentDisplayName(visor.currentPath, visor.currentIndex),
                    color = Color.White
                )
                Text(
                    text = "${visor.currentIndex + 1} de ${visor.paths.size}",
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrevious,
                        enabled = visor.canGoPrevious,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("←")
                    }
                    Button(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⤢")
                    }
                    Button(
                        onClick = onNext,
                        enabled = visor.canGoNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("→")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✕")
                    }
                }
            }
        }
    }
}

private fun decodeAttachmentBitmap(context: Context, path: String, targetSize: Int): Bitmap? {
    return try {
        val uri = Uri.parse(path)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !uri.scheme.isNullOrBlank()) {
            loadBitmapFromUri(context, uri, targetSize)
        } else {
            decodeAttachmentBitmapLegacy(context, uri, targetSize)
        }
    } catch (_: Exception) {
        null
    }
}

private fun decodeAttachmentBitmapLegacy(context: Context, uri: Uri, targetSize: Int): Bitmap? {
    return try {
        val path = uri.toString()
        val localPath = when {
            uri.scheme.equals("file", ignoreCase = true) -> uri.path
            uri.scheme.isNullOrBlank() && path.startsWith("/") -> path
            else -> null
        }

        if (!localPath.isNullOrBlank()) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(localPath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, targetSize, targetSize)
            }
            return BitmapFactory.decodeFile(localPath, options)
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, targetSize, targetSize)
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize.coerceAtLeast(1)
}

internal fun guardarFotoPerfil(context: Context, uri: Uri?, bitmap: Bitmap?): String? {
    return try {
        val dir = File(context.filesDir, "fotos_perfil").also { it.mkdirs() }
        val archivo = File(dir, "perfil_${System.currentTimeMillis()}.jpg")
        if (bitmap != null) {
            archivo.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } else if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                archivo.outputStream().use { out -> input.copyTo(out) }
            }
        } else {
            return null
        }
        archivo.absolutePath
    } catch (_: Exception) {
        null
    }
}

private data class UpcomingDose(
    val medication: Medication,
    val doseTimeMillis: Long,
    val hora: String
)

private data class MedicationDaySchedule(
    val medication: Medication,
    val tomas: List<UpcomingDose>,
    val primerHorarioMillis: Long
)

private data class ConfirmacionTomaVencida(
    val medicationId: Int,
    val patientId: Int,
    val medicationName: String,
    val scheduledAt: Long,
    val hora: String
)

// Types moved to HelpersExportModels.kt

internal fun resolveVitalSignsExportRange(
    filter: VitalSignsExportFilter,
    customStart: Long,
    customEnd: Long
): VitalSignsExportRange {
    return when (filter) {
        VitalSignsExportFilter.TODAY -> {
            val now = System.currentTimeMillis()
            VitalSignsExportRange(
                start = inicioDelDia(now),
                end = finDelDia(now),
                label = "Hoy (${formatDate(now)})",
                fileSuffix = "hoy"
            )
        }
        VitalSignsExportFilter.WEEK -> {
            val now = System.currentTimeMillis()
            VitalSignsExportRange(
                start = inicioDeLaSemana(now),
                end = finDeLaSemana(now),
                label = "Semana (${formatDate(now)})",
                fileSuffix = "semana"
            )
        }
        VitalSignsExportFilter.MONTH -> {
            val now = System.currentTimeMillis()
            VitalSignsExportRange(
                start = inicioDelMes(now),
                end = finDelMes(now),
                label = "Mes (${formatDate(now)})",
                fileSuffix = "mes"
            )
        }
        VitalSignsExportFilter.CUSTOM -> {
            val start = inicioDelDia(customStart)
            val end = finDelDia(customEnd)
            VitalSignsExportRange(
                start = start,
                end = end,
                label = "Del ${formatDate(start)} al ${formatDate(end)}",
                fileSuffix = "rango"
            )
        }
    }
}

internal fun buildVitalSignsExportReport(
    records: List<SignosVitales>,
    patient: PatientProfile?,
    rangeLabel: String
): VitalSignsExportReport {
    val orderedRecords = records.sortedBy { it.fechaRegistro }

    fun averageOrDash(values: List<Int>): String {
        return if (values.isEmpty()) "Sin datos" else "%.1f".format(Locale.US, values.average())
    }

    fun averageDoubleOrDash(values: List<Double>): String {
        return if (values.isEmpty()) "Sin datos" else "%.1f".format(Locale.US, values.average())
    }

    return VitalSignsExportReport(
        title = "Informe de Métricas Diarias",
        patientLabel = patient?.let { "${it.nombre} ${it.apellidos}" }?.ifBlank { "Paciente no especificado" }
            ?: "Paciente no especificado",
        rangeLabel = rangeLabel,
        generatedAt = formatDateTime(System.currentTimeMillis()),
        totalRecords = orderedRecords.size,
        averageSystolic = averageOrDash(orderedRecords.mapNotNull { it.sistolica }),
        averageDiastolic = averageOrDash(orderedRecords.mapNotNull { it.diastolica }),
        averageHeartRate = averageOrDash(orderedRecords.mapNotNull { it.latidos }),
        averageGlucose = averageOrDash(orderedRecords.mapNotNull { it.glucemia }),
        averageTemperature = averageDoubleOrDash(orderedRecords.mapNotNull { it.temperatura }),
        averageWeight = averageDoubleOrDash(orderedRecords.mapNotNull { it.peso }),
        rows = orderedRecords.map {
            VitalSignsExportRow(
                recordedAt = it.fechaRegistro,
                systolic = it.sistolica,
                diastolic = it.diastolica,
                pressureComment = it.comentarioPresion,
                heartRate = it.latidos,
                heartRateComment = it.comentarioLatidos,
                glucose = it.glucemia,
                glucoseComment = it.comentarioGlucemia,
                temperature = it.temperatura,
                temperatureComment = it.comentarioTemperatura,
                peso = it.peso,
                pesoUnidad = it.pesoUnidad,
                imc = it.imc
            )
        }
    )
}

internal fun writeVitalSignsDocxDocument(
    output: java.io.OutputStream,
    report: VitalSignsExportReport
) {
    ZipOutputStream(output).use { zip ->
        fun putEntry(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        putEntry(
            "[Content_Types].xml",
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
              <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
              <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
              <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
            </Types>
            """.trimIndent()
        )
        putEntry(
            "_rels/.rels",
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
            </Relationships>
            """.trimIndent()
        )
        putEntry(
            "docProps/core.xml",
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <dc:title>${escapeXml(report.title)}</dc:title>
              <dc:creator>ControlMedicamentos</dc:creator>
              <cp:lastModifiedBy>ControlMedicamentos</cp:lastModifiedBy>
              <dcterms:created xsi:type="dcterms:W3CDTF">${formatDocxTimestamp()}</dcterms:created>
              <dcterms:modified xsi:type="dcterms:W3CDTF">${formatDocxTimestamp()}</dcterms:modified>
            </cp:coreProperties>
            """.trimIndent()
        )
        putEntry(
            "docProps/app.xml",
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
              <Application>ControlMedicamentos</Application>
              <DocSecurity>0</DocSecurity>
              <ScaleCrop>false</ScaleCrop>
              <Company>ControlMedicamentos</Company>
              <LinksUpToDate>false</LinksUpToDate>
              <SharedDoc>false</SharedDoc>
              <HyperlinksChanged>false</HyperlinksChanged>
              <AppVersion>1.0</AppVersion>
            </Properties>
            """.trimIndent()
        )
        putEntry(
            "word/styles.xml",
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                <w:name w:val="Normal"/>
                <w:qFormat/>
              </w:style>
              <w:style w:type="paragraph" w:styleId="Heading1">
                <w:name w:val="heading 1"/>
                <w:basedOn w:val="Normal"/>
                <w:qFormat/>
                <w:rPr>
                  <w:b/>
                  <w:sz w:val="30"/>
                </w:rPr>
              </w:style>
              <w:style w:type="table" w:styleId="TableGrid">
                <w:name w:val="Table Grid"/>
                <w:tblPr>
                  <w:tblBorders>
                    <w:top w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>
                    <w:left w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>
                    <w:bottom w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>
                    <w:right w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>
                    <w:insideH w:val="single" w:sz="4" w:space="0" w:color="D9D9D9"/>
                    <w:insideV w:val="single" w:sz="4" w:space="0" w:color="D9D9D9"/>
                  </w:tblBorders>
                </w:tblPr>
              </w:style>
            </w:styles>
            """.trimIndent()
        )
        putEntry(
            "word/document.xml",
            buildVitalSignsDocumentXml(report)
        )
    }
}

private fun buildVitalSignsDocumentXml(report: VitalSignsExportReport): String {
    fun paragraph(text: String, styleId: String? = null, bold: Boolean = false): String {
        val style = styleId?.let { "<w:pPr><w:pStyle w:val=\"$it\"/></w:pPr>" }.orEmpty()
        val runProps = if (bold) "<w:rPr><w:b/></w:rPr>" else ""
        return "<w:p>$style<w:r>$runProps<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p>"
    }

    fun tableCell(text: String, bold: Boolean = false): String {
        val runProps = if (bold) "<w:rPr><w:b/></w:rPr>" else ""
        return "<w:tc><w:p><w:r>$runProps<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p></w:tc>"
    }

    val headerRow = "<w:tr>${tableCell("Fecha", true)}${tableCell("Hora", true)}${tableCell("Sistolica", true)}${tableCell("Diastolica", true)}${tableCell("Latidos", true)}${tableCell("Glucemia", true)}${tableCell("Temperatura", true)}${tableCell("Peso", true)}${tableCell("IMC", true)}${tableCell("Comentarios", true)}</w:tr>"
    val bodyRows = if (report.rows.isEmpty()) {
        "<w:tr><w:tc><w:p><w:r><w:t xml:space=\"preserve\">No hay registros para el rango seleccionado.</w:t></w:r></w:p></w:tc><w:tc/><w:tc/><w:tc/><w:tc/><w:tc/><w:tc/><w:tc/><w:tc/><w:tc/></w:tr>"
    } else {
        report.rows.joinToString(separator = "") { row ->
            val comments = buildList {
                row.pressureComment.takeIf { it.isNotBlank() }?.let { add("Presion: $it") }
                row.heartRateComment.takeIf { it.isNotBlank() }?.let { add("Latidos: $it") }
                row.glucoseComment.takeIf { it.isNotBlank() }?.let { add("Glucemia: $it") }
                row.temperatureComment.takeIf { it.isNotBlank() }?.let { add("Temperatura: $it") }
            }.joinToString(" | ").ifBlank { "Sin comentarios" }
            val presionTxt = if (row.systolic != null && row.diastolic != null) "${row.systolic}/${row.diastolic}" else "Sin dato"
            "<w:tr>" +
                tableCell(formatDate(row.recordedAt)) +
                tableCell(formatHour(row.recordedAt)) +
                tableCell(row.systolic?.toString() ?: "--") +
                tableCell(row.diastolic?.toString() ?: "--") +
                tableCell(row.heartRate?.toString() ?: "--") +
                tableCell(row.glucose?.toString() ?: "--") +
                tableCell(row.temperature?.let(::formatTemperature) ?: "--") +
                tableCell(row.peso?.let { "${"%.1f".format(it)} ${row.pesoUnidad}" } ?: "--") +
                tableCell(row.imc?.let { "${"%.1f".format(it)} (${etiquetaIMC(it)})" } ?: "--") +
                tableCell(comments) +
                "</w:tr>"
        }
    }

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 wp14">
          <w:body>
            ${paragraph(report.title, styleId = "Heading1")}
            ${paragraph("Paciente: ${report.patientLabel}", bold = true)}
            ${paragraph("Periodo: ${report.rangeLabel}")}
            ${paragraph("Generado: ${report.generatedAt}")}
            ${paragraph("Total de registros: ${report.totalRecords}")}
            ${paragraph("Promedio sistolica: ${report.averageSystolic}")}
            ${paragraph("Promedio diastolica: ${report.averageDiastolic}")}
            ${paragraph("Promedio latidos: ${report.averageHeartRate}")}
            ${paragraph("Promedio glucemia: ${report.averageGlucose}")}
            ${paragraph("Promedio temperatura: ${report.averageTemperature}")}
            ${paragraph("Promedio peso: ${report.averageWeight}")}
            <w:p/>
            <w:tbl>
              <w:tblPr>
                <w:tblStyle w:val="TableGrid"/>
                <w:tblW w:w="0" w:type="auto"/>
              </w:tblPr>
              <w:tblGrid>
                <w:gridCol w:w="1800"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="1300"/>
                <w:gridCol w:w="3000"/>
              </w:tblGrid>
              $headerRow
              $bodyRows
            </w:tbl>
            <w:sectPr>
              <w:pgSz w:w="11906" w:h="16838"/>
              <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>
            </w:sectPr>
          </w:body>
        </w:document>
    """.trimIndent()
}

private fun escapeXml(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun formatDocxTimestamp(timestamp: Long = System.currentTimeMillis()): String {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(timestamp))
}

internal fun formatTemperature(value: Double): String {
    return "${"%.1f".format(Locale.US, value)} °C"
}

internal fun calcularRangoExportacion(period: IntakeExportPeriod, reference: Long): IntakeExportRange {
    val startCalendar = Calendar.getInstance().apply { timeInMillis = reference }
    val endCalendar = Calendar.getInstance().apply { timeInMillis = reference }

    when (period) {
        IntakeExportPeriod.DAY -> {
            // Día actual
            // Ya está en el día del reference
        }
        IntakeExportPeriod.WEEK -> {
            val dayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK)
            val offset = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
            startCalendar.add(Calendar.DAY_OF_YEAR, offset)
            endCalendar.timeInMillis = startCalendar.timeInMillis
            endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        }
        IntakeExportPeriod.MONTH -> {
            startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        IntakeExportPeriod.YEAR -> {
            startCalendar.set(Calendar.DAY_OF_YEAR, 1)
            endCalendar.set(Calendar.DAY_OF_YEAR, endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR))
        }
    }

    startCalendar.set(Calendar.HOUR_OF_DAY, 0)
    startCalendar.set(Calendar.MINUTE, 0)
    startCalendar.set(Calendar.SECOND, 0)
    startCalendar.set(Calendar.MILLISECOND, 0)

    endCalendar.set(Calendar.HOUR_OF_DAY, 23)
    endCalendar.set(Calendar.MINUTE, 59)
    endCalendar.set(Calendar.SECOND, 59)
    endCalendar.set(Calendar.MILLISECOND, 999)

    return IntakeExportRange(
        start = startCalendar.timeInMillis,
        end = endCalendar.timeInMillis,
        label = when (period) {
            IntakeExportPeriod.DAY -> "Día ${formatDate(startCalendar.timeInMillis)}"
            IntakeExportPeriod.WEEK -> "Semana del ${formatDate(startCalendar.timeInMillis)} al ${formatDate(endCalendar.timeInMillis)}"
            IntakeExportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date(reference))
            IntakeExportPeriod.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(reference))
        },
        fileSuffix = when (period) {
            IntakeExportPeriod.DAY -> "dia"
            IntakeExportPeriod.WEEK -> "semana"
            IntakeExportPeriod.MONTH -> "mes"
            IntakeExportPeriod.YEAR -> "anio"
        }
    )
}

internal fun buildMedicationIntakeRtfDocument(
    patient: PatientProfile,
    medications: List<Medication>,
    intakes: List<MedicationIntake>,
    range: IntakeExportRange
): String {
    val intakeLookup = intakes.associateBy { it.medicationId to it.scheduledAt }
    val medicationLookup = medications.associateBy { it.id }

    val rowsFromScheduled = medications
        .flatMap { medication ->
            obtenerTomasEnRango(medication, range.start, range.end).map { dose ->
                val intake = intakeLookup[medication.id to dose.doseTimeMillis]
                MedicationIntakeExportRow(
                    scheduledAt = dose.doseTimeMillis,
                    medicationName = medication.nombre,
                    dose = medication.dosis,
                    status = if (intake != null) "Tomado" else "Pendiente",
                    acceptedAt = intake?.acceptedAt
                )
            }
        }

    val scheduledKeys = rowsFromScheduled.mapTo(mutableSetOf()) { it.scheduledAt }
    val rowsFromOrphans = intakes
        .filter { it.medicationId !in medicationLookup && it.scheduledAt !in scheduledKeys }
        .map { intake ->
            MedicationIntakeExportRow(
                scheduledAt = intake.scheduledAt,
                medicationName = intake.medicationName.ifBlank { "Medicamento eliminado (id=${intake.medicationId})" },
                dose = intake.dosis,
                status = "Tomado",
                acceptedAt = intake.acceptedAt
            )
        }

    val rows = (rowsFromScheduled + rowsFromOrphans)
        .sortedBy { it.scheduledAt }

    val total = rows.size
    val taken = rows.count { it.status == "Tomado" }
    val pending = total - taken
    val adherence = if (total > 0) ((taken * 100.0) / total) else 0.0

    val bodyRows = if (rows.isEmpty()) {
        "No hay tomas registradas para este periodo.\\par"
    } else {
        rows.joinToString(separator = "") { row ->
            buildString {
                append(escapeRtf(formatDate(row.scheduledAt)))
                append("\\tab ")
                append(escapeRtf(formatHour(row.scheduledAt)))
                append("\\tab ")
                append(escapeRtf(row.medicationName))
                append("\\tab ")
                append(escapeRtf(row.dose))
                append("\\tab ")
                append(escapeRtf(row.status))
                row.acceptedAt?.let {
                    append(" (")
                    append(escapeRtf(formatDateTime(it)))
                    append(")")
                }
                append("\\par\n")
            }
        }
    }

    return buildString {
        append("{\\rtf1\\ansi\\deff0")
        append("\\fs24 ")
        append("\\b ")
        append(escapeRtf("Informe de registros de medicamentos"))
        append("\\b0\\par\\par ")
        append("\\b ")
        append(escapeRtf("Paciente:"))
        append("\\b0 ")
        append(escapeRtf("${patient.nombre} ${patient.apellidos}"))
        append("\\par ")
        append("\\b ")
        append(escapeRtf("Periodo:"))
        append("\\b0 ")
        append(escapeRtf(range.label))
        append("\\par ")
        append("\\b ")
        append(escapeRtf("Generado:"))
        append("\\b0 ")
        append(escapeRtf(formatDateTime(System.currentTimeMillis())))
        append("\\par\\par ")
        append(escapeRtf("Total de tomas programadas: $total"))
        append("\\par ")
        append(escapeRtf("Tomas marcadas como tomadas: $taken"))
        append("\\par ")
        append(escapeRtf("Tomas pendientes: $pending"))
        append("\\par ")
        append(escapeRtf("Adherencia registrada: ${"%.1f".format(Locale.US, adherence)}%"))
        append("\\par\\par ")
        append("\\b ")
        append(escapeRtf("Fecha\tHora\tMedicamento\tDosis\tEstado"))
        append("\\b0\\par ")
        append(bodyRows)
        append("}")
    }
}

private fun escapeRtf(value: String): String {
    val builder = StringBuilder()
    value.forEach { char ->
        when (char) {
            '\\' -> builder.append("\\\\")
            '{' -> builder.append("\\{")
            '}' -> builder.append("\\}")
            '\n' -> builder.append("\\par ")
            '\t' -> builder.append("\\tab ")
            else -> {
                if (char.code in 32..126) {
                    builder.append(char)
                } else {
                    builder.append("\\u${char.code}?")
                }
            }
        }
    }
    return builder.toString()
}

internal fun inicioDelDia(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun finDelDia(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = inicioDelDia(timestamp)
        add(Calendar.DAY_OF_YEAR, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis
}

private fun obtenerInsumosProgramadosDelDia(
    insumos: List<Medication>,
    fechaSeleccionada: Long
): List<MedicationDaySchedule> {
    val inicio = inicioDelDia(fechaSeleccionada)
    val fin = finDelDia(fechaSeleccionada)

    return insumos
        .mapNotNull { medication ->
            val tomas = obtenerTomasEnRango(medication, inicio, fin)
            if (tomas.isEmpty()) {
                null
            } else {
                MedicationDaySchedule(
                    medication = medication,
                    tomas = tomas,
                    primerHorarioMillis = tomas.minOf { it.doseTimeMillis }
                )
            }
        }
        .sortedBy { it.primerHorarioMillis }
}

@Composable
internal fun DashboardMedicationPage(
    pageDate: Long,
    medications: List<Medication>,
    patientId: Int,
    database: AppDatabase,
    onRequestRemoveIntake: (IntakeRemovalConfirmation) -> Unit,
    onRecargarStock: (Medication) -> Unit = {},
    onAnadirAPedido: (Medication) -> Unit = {},
    onViewStock: (Medication) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var tomaVencidaConfirmacion by remember { mutableStateOf<ConfirmacionTomaVencida?>(null) }
    val inicioFecha = remember(pageDate) { inicioDelDia(pageDate) }
    val finFecha = remember(pageDate) { finDelDia(pageDate) }
    // Sin remember para que siempre se recalcule con la hora actual
    val esHoy = inicioDelDia(pageDate) == inicioDelDia(System.currentTimeMillis())
    val tomasRegistradasDelDia by remember(pageDate) {
        database.medicationIntakeDao().observarEnRango(inicioFecha, finFecha)
    }.collectAsState(initial = emptyList())
    val tomasRegistradasLookup = remember(tomasRegistradasDelDia) {
        tomasRegistradasDelDia.associateBy { it.medicationId to it.scheduledAt }
    }
    val medicamentosProgramadosDelDia = remember(medications, pageDate) {
        obtenerInsumosProgramadosDelDia(medications, pageDate)
    }
    // ── Anticonceptivos diarios ─────────────────────────────────────
    val anticonceptivosActivos by produceState<List<MetodoAnticonceptivo>>(emptyList(), patientId) {
        value = if (patientId > 0) {
            database.metodoAnticonceptivoDao().obtenerActivos(patientId)
                .filter { TipoAnticonceptivo.fromDisplayName(it.tipo).requiereAlarmaDiaria }
        } else emptyList()
    }
    var tomasAnticonceptivosLookup by remember(pageDate, anticonceptivosActivos) {
        mutableStateOf<Map<Int, AnticonceptivoIntake?>>(emptyMap())
    }
    LaunchedEffect(pageDate, anticonceptivosActivos) {
        val result = mutableMapOf<Int, AnticonceptivoIntake?>()
        anticonceptivosActivos.forEach { metodo ->
            val tomas = database.anticonceptivoIntakeDao().observarEnRango(metodo.id, inicioFecha, finFecha)
                .first()
            result[metodo.id] = tomas.firstOrNull()
        }
        tomasAnticonceptivosLookup = result
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (medicamentosProgramadosDelDia.isEmpty()) {
            item(key = "empty-$pageDate") {
                Text(
                    text = "No hay medicamentos programados para esta fecha.",
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(medicamentosProgramadosDelDia, key = { it.medication.id }) { programacion ->
                val medication = programacion.medication
                val suspendido = !medication.estaActivo
                val stockCritico = medication.stockActual != null && medication.stockActual <= 0
                MetallicMedicationCard(
                    modifier = Modifier.fillMaxWidth(),
                    isStockCritical = stockCritico,
                    isSuspended = suspendido
                ) {
                    val contentColor = if (suspendido) Color(0xFF333333) else Color.White
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val formaInfo = FORMAS_MEDICAMENTO.firstOrNull { it.first == medication.formaMedicamento }
                        if (formaInfo != null) {
                            val iconColor = medication.colorMedicamento
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
                        Text(
                            text = medication.nombre + if (suspendido) " (Suspendido)" else "",
                            color = contentColor,
                            fontWeight = if (suspendido) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cantidad", color = contentColor)
                            Text(medication.dosis, color = contentColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frecuencia", color = contentColor)
                            Text(hoursToCycle(medication.frecuenciaHoras), color = contentColor)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !suspendido) {
                                    onViewStock(medication)
                                }
                        ) {
                            Text("Stock", color = contentColor)
                            Text(
                                medication.stockActual?.let { "$it uds." } ?: "Sin control",
                                color = if (stockCritico) Color(0xFFFF5252) else Color(0xFF64B5F6),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (suspendido) {
                        Text(
                            text = "Horarios: ${programacion.tomas.joinToString(", ") { it.hora }}",
                            color = contentColor.copy(alpha = 0.85f)
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.medicationDao().cambiarEstado(medication.id, true)
                                    val actualizado = medication.copy(estaActivo = true)
                                    if (actualizado.alarmaActiva) {
                                        MedicationScheduler(context).programarAlarmas(actualizado)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Medicamento reactivado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reactivar")
                        }
                        Text(
                            text = "Este medicamento está suspendido. No se registrarán tomas ni alarmas hasta reactivarlo.",
                            color = contentColor.copy(alpha = 0.70f),
                            fontSize = 12.sp
                        )
                    } else {
                        Text("Horarios del dia", color = contentColor)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(programacion.tomas, key = { "${medication.id}-${it.doseTimeMillis}" }) { toma ->
                                val registroToma = tomasRegistradasLookup[medication.id to toma.doseTimeMillis]
                                val noTomada = registroToma?.status == MEDICATION_INTAKE_STATUS_NOT_TAKEN
                                val tomada = registroToma != null && !noTomada
                                val yaVencida = esHoy && !tomada && !noTomada && toma.doseTimeMillis < System.currentTimeMillis()
                                val colorTextoEstado = when {
                                    tomada -> Color(0xFF1B5E20)
                                    noTomada -> Color(0xFF424242)
                                    yaVencida -> Color(0xFFB71C1C)
                                    else -> Color(0xFFC62828)
                                }
                                val subtituloEstado = when {
                                    tomada -> "Tomada ${formatHour(registroToma!!.acceptedAt)}"
                                    noTomada -> "No tomada"
                                    yaVencida -> "\u00bfLa tomaste?"
                                    else -> "Pendiente"
                                }
                                Card(
                                    modifier = Modifier.clickable {
                                        if (tomada) {
                                            onRequestRemoveIntake(
                                                IntakeRemovalConfirmation(
                                                    medicationId = medication.id,
                                                    medicationName = medication.nombre,
                                                    scheduledAt = toma.doseTimeMillis,
                                                    acceptedAt = registroToma!!.acceptedAt
                                                )
                                            )
                                        } else if (noTomada) {
                                            return@clickable
                                        } else if (yaVencida) {
                                            tomaVencidaConfirmacion = ConfirmacionTomaVencida(
                                                medicationId = medication.id,
                                                patientId = medication.patientId,
                                                medicationName = medication.nombre,
                                                scheduledAt = toma.doseTimeMillis,
                                                hora = toma.hora
                                            )
                                        } else if (esHoy) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val unidadesToma = medication.unidadesPorToma()
                                                database.medicationIntakeDao().guardar(
                                                    MedicationIntake(
                                                        medicationId = medication.id,
                                                        patientId = medication.patientId,
                                                        scheduledAt = toma.doseTimeMillis,
                                                        medicationName = medication.nombre,
                                                        dosis = unidadesToma.toString()
                                                    )
                                                )
                                                val med = database.medicationDao().findById(medication.id)
                                                if (med?.stockActual != null) {
                                                    val nuevoStock = med.stockActual - unidadesToma
                                                    database.medicationDao().actualizarStock(med.id, nuevoStock)
                                                }
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            tomada -> Color(0xFFC8E6C9)
                                            noTomada -> Color(0xFFE0E0E0)
                                            yaVencida -> Color(0xFFFFCDD2)
                                            !esHoy -> Color(0xFFB0BEC5)
                                            else -> Color(0xFFFFF3E0)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = toma.hora,
                                            color = colorTextoEstado,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = subtituloEstado,
                                            color = colorTextoEstado.copy(alpha = 0.85f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (esHoy) "Toca una hora para marcarla o desmarcarla como tomada." else "Solo se puede registrar tomas en el día actual.",
                            color = contentColor
                        )
                    }
                }
            }
        }
        // ── Anticonceptivos diarios en el dashboard ─────────────────
        items(anticonceptivosActivos, key = { "anticonceptivo-${it.id}" }) { metodo ->
            val tomaRegistrada = tomasAnticonceptivosLookup[metodo.id]
            val tomada = tomaRegistrada != null
            val partesHora = metodo.horaToma.split(":")
            val horaInt = partesHora.getOrNull(0)?.toIntOrNull() ?: 8
            val minutoInt = partesHora.getOrNull(1)?.toIntOrNull() ?: 0
            val horaTomaMillis = Calendar.getInstance().apply {
                timeInMillis = pageDate
                set(Calendar.HOUR_OF_DAY, horaInt)
                set(Calendar.MINUTE, minutoInt)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val yaVencida = esHoy && !tomada && horaTomaMillis < System.currentTimeMillis()
            val colorTextoEstado = when {
                tomada -> Color(0xFF1B5E20)
                yaVencida -> Color(0xFFB71C1C)
                else -> Color(0xFFC62828)
            }
            val subtituloEstado = when {
                tomada -> "Tomada ${formatHour(tomaRegistrada!!.acceptedAt)}"
                yaVencida -> "¿La tomaste?"
                else -> "Pendiente"
            }
            MetallicMedicationCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Medication,
                        contentDescription = null,
                        tint = Color(0xFFCE93D8),
                        modifier = Modifier.size(28.dp)
                    )
                    Text("💊 ${metodo.tipo}", color = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cantidad", color = Color.White)
                        Text("1 unidad", color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frecuencia", color = Color.White)
                        Text("Diaria", color = Color.White)
                    }
                }
                Text("Horarios del dia", color = Color.White)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "anticonceptivo-${metodo.id}-hora") {
                        Card(
                            modifier = Modifier.clickable {
                                if (tomada) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.anticonceptivoIntakeDao().eliminarPorMetodoYHorario(
                                            metodo.id, horaTomaMillis
                                        )
                                    }
                                } else if (esHoy) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.anticonceptivoIntakeDao().guardar(
                                            AnticonceptivoIntake(
                                                metodoId = metodo.id,
                                                scheduledAt = horaTomaMillis
                                            )
                                        )
                                    }
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    tomada -> Color(0xFFC8E6C9)
                                    yaVencida -> Color(0xFFFFCDD2)
                                    !esHoy -> Color(0xFFB0BEC5)
                                    else -> Color(0xFFFFF3E0)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = metodo.horaToma,
                                    color = colorTextoEstado,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subtituloEstado,
                                    color = colorTextoEstado.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Text(
                    text = if (esHoy) "Toca una hora para marcarla o desmarcarla como tomada." else "Solo se puede registrar tomas en el día actual.",
                    color = Color.White
                )
            }
        }
    }

    val confirmacion = tomaVencidaConfirmacion
    if (confirmacion != null) {
        ConfirmacionTomaVencidaDialog(
            confirmacion = confirmacion,
            onConfirm = {
                tomaVencidaConfirmacion = null
                coroutineScope.launch(Dispatchers.IO) {
                    val med = database.medicationDao().findById(confirmacion.medicationId)
                    val unidadesToma = med?.unidadesPorToma() ?: 1
                    database.medicationIntakeDao().guardar(
                        MedicationIntake(
                            medicationId = confirmacion.medicationId,
                            patientId = confirmacion.patientId,
                            scheduledAt = confirmacion.scheduledAt,
                            medicationName = med?.nombre ?: confirmacion.medicationName,
                            dosis = unidadesToma.toString()
                        )
                    )
                    if (med?.stockActual != null) {
                        val nuevoStock = med.stockActual - unidadesToma
                        database.medicationDao().actualizarStock(med.id, nuevoStock)
                    }
                }
            },
            onDismiss = { tomaVencidaConfirmacion = null }
        )
    }
}

@Composable
private fun ConfirmacionTomaVencidaDialog(
    confirmacion: ConfirmacionTomaVencida,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ya registraste este medicamento?") },
        text = {
            Text(
                "${confirmacion.medicationName} estaba programado a las ${confirmacion.hora}.\n\n" +
                "No estaba disponible el dispositivo en ese momento. " +
                "Deseas marcarlo como tomado igualmente?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Si, ya la tome")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No, omitir")
            }
        }
    )
}

@Composable
internal fun AnimatedDashboardDateSummary(
    text: String,
    isToday: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isToday) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 240f
        ),
        label = "dashboardDateScale"
    )
    val glowTransition = rememberInfiniteTransition(label = "dashboardDateGlow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboardDateGlowAlpha"
    )

    Text(
        text = text,
        color = Color.White,
        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        style = TextStyle(
            shadow = if (isToday) {
                Shadow(
                    color = Color(0xFF8FF3FF).copy(alpha = glowAlpha),
                    blurRadius = 22f
                )
            } else {
                Shadow(
                    color = Color.Transparent,
                    blurRadius = 0f
                )
            }
        )
    )
}

private data class DashboardWeekDay(
    val timestamp: Long,
    val dayLabel: String,
    val dayNumber: String,
    val isToday: Boolean
)

@Composable
internal fun DashboardWeekSelector(
    selectedDate: Long,
    onSelectDate: (Long) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val carouselRadius = 14
    val carouselDays = remember(selectedDate) {
        (-carouselRadius..carouselRadius).map { offset ->
            val fecha = moverFecha(selectedDate, offset)
            DashboardWeekDay(
                timestamp = fecha,
                dayLabel = formatDashboardWeekDayLabel(fecha),
                dayNumber = Calendar.getInstance().apply { timeInMillis = fecha }.get(Calendar.DAY_OF_MONTH).toString(),
                isToday = fecha == inicioDelDia(System.currentTimeMillis())
            )
        }
    }
    val centeredIndex = carouselRadius
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = centeredIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(selectedDate, carouselDays) {
        listState.animateScrollToItem(centeredIndex)
    }

    LaunchedEffect(listState.isScrollInProgress, carouselDays, selectedDate) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val centeredDay = layoutInfo.visibleItemsInfo
                .minByOrNull { itemInfo -> abs((itemInfo.offset + itemInfo.size / 2f) - viewportCenter) }
                ?.let { itemInfo -> carouselDays.getOrNull(itemInfo.index) }

            if (centeredDay != null && inicioDelDia(centeredDay.timestamp) != inicioDelDia(selectedDate)) {
                onSelectDate(centeredDay.timestamp)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Dia anterior"
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val fadeWidth = size.width * 0.18f
                    val fadeBrush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            fadeWidth / size.width to Color.Black,
                            1f - (fadeWidth / size.width) to Color.Black,
                            1f to Color.Transparent
                        )
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = fadeBrush,
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            val itemWidth = 72.dp
            val horizontalPadding = (maxWidth - itemWidth).coerceAtLeast(0.dp) / 2

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                flingBehavior = flingBehavior
            ) {
                items(
                    items = carouselDays,
                    key = { day -> day.timestamp }
                ) { day ->
                    val itemIndex = carouselDays.indexOf(day)
                    DashboardCarouselDay(
                        day = day,
                        itemWidth = itemWidth,
                        isSelected = inicioDelDia(day.timestamp) == inicioDelDia(selectedDate),
                        scale = rememberCarouselItemScale(
                            listState = listState,
                            index = itemIndex,
                            selected = inicioDelDia(day.timestamp) == inicioDelDia(selectedDate)
                        ),
                        onClick = { onSelectDate(day.timestamp) }
                    )
                }
            }
        }
        IconButton(onClick = onNextDay) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Dia siguiente"
            )
        }
    }
}

@Composable
private fun DashboardCarouselDay(
    day: DashboardWeekDay,
    itemWidth: Dp,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "dashboardCarouselScale"
    )

    Column(
        modifier = Modifier
            .width(itemWidth)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .zIndex(if (isSelected) 1f else 0f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day.dayLabel,
            color = if (isSelected) Color(0xFFB6EDFF) else Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF18C0F4) else Color(0x1AF7FAFF)
            ),
            border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF59616C))
        ) {
            Box(
                modifier = Modifier.size(if (isSelected) 52.dp else 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.dayNumber,
                    color = Color.White,
                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isSelected) 20.sp else 16.sp
                )
            }
        }
    }
}

@Composable
private fun rememberCarouselItemScale(
    listState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    selected: Boolean
): Float {
    val targetScale by remember(listState, index, selected) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

            if (itemInfo == null) {
                if (selected) 1.18f else 0.9f
            } else {
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                val itemSize = itemInfo.size.toFloat().coerceAtLeast(1f)
                val normalizedDistance = (abs(itemCenter - viewportCenter) / itemSize).coerceIn(0f, 1.2f)
                1.2f - (normalizedDistance.coerceAtMost(1f) * 0.28f)
            }
        }
    }

    return targetScale
}

internal fun calcularIMC(
    pesoStr: String,
    pesoUnidad: String,
    estaturaStr: String,
    estaturaUnidad: String
): Double? {
    val peso = pesoStr.replace(',', '.').toDoubleOrNull() ?: return null
    val estatura = estaturaStr.replace(',', '.').toDoubleOrNull() ?: return null
    if (estatura <= 0.0) return null
    val pesoKg = if (pesoUnidad == "kg") peso else peso * 0.453592
    val alturaM = when (estaturaUnidad) {
        "cm" -> estatura / 100.0
        "in" -> estatura * 0.0254
        else -> estatura / 100.0
    }
    if (alturaM <= 0.0) return null
    return pesoKg / (alturaM * alturaM)
}

internal fun etiquetaIMC(imc: Double): String = when {
    imc < 18.5 -> "Bajo peso"
    imc < 25.0 -> "Normal"
    imc < 30.0 -> "Sobrepeso"
    imc < 35.0 -> "Obesidad I"
    imc < 40.0 -> "Obesidad II"
    else       -> "Obesidad III"
}

private fun obtenerProximasTomasDelDia(insumos: List<Medication>, ahora: Long): List<UpcomingDose> {
    val inicioDelDia = inicioDelDia(ahora)
    val finDelDia = finDelDia(ahora)

    val candidatas = insumos
        .filter { it.estaActivo }
        .flatMap { medication -> obtenerTomasDelDia(medication, ahora, inicioDelDia, finDelDia) }

    val proximaHora = candidatas.minOfOrNull { it.doseTimeMillis } ?: return emptyList()
    return candidatas.filter { it.doseTimeMillis == proximaHora }
}

private fun obtenerTomasDelDia(
    medication: Medication,
    ahora: Long,
    inicioDelDia: Long,
    finDelDia: Long
): List<UpcomingDose> {
    return obtenerTomasEnRango(medication, inicioDelDia, finDelDia)
        .filter { it.doseTimeMillis >= ahora }
}

private fun obtenerTomasEnRango(
    medication: Medication,
    inicioDelDia: Long,
    finDelDia: Long
): List<UpcomingDose> {
    if (medication.esCicloCorto && inicioDelDia > medication.fechaFin) {
        return emptyList()
    }

    return if (medication.repartoDosis == "En diferentes horarios" && medication.horariosTomas.isNotBlank()) {
        medication.horariosTomas.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { horario ->
                val millis = horarioEnDia(inicioDelDia, horario)
                if (millis in inicioDelDia..finDelDia && millis >= medication.fechaInicio && (!medication.esCicloCorto || millis <= medication.fechaFin)) {
                    UpcomingDose(medication, millis, horario)
                } else {
                    null
                }
            }
    } else {
        val frecuenciaHoras = medication.frecuenciaHoras.takeIf { it > 0 } ?: 24
        val base = medication.horaToma.takeIf { it.isNotBlank() } ?: formatHour(medication.fechaInicio)
        val baseCalendar = Calendar.getInstance().apply {
            timeInMillis = medication.fechaInicio
            val partes = base.split(":")
            set(Calendar.HOUR_OF_DAY, partes.getOrNull(0)?.toIntOrNull() ?: 8)
            set(Calendar.MINUTE, partes.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val candidatas = mutableListOf<UpcomingDose>()
        while (baseCalendar.timeInMillis <= finDelDia) {
            val time = baseCalendar.timeInMillis
            if (time >= inicioDelDia && time >= medication.fechaInicio && (!medication.esCicloCorto || time <= medication.fechaFin)) {
                candidatas += UpcomingDose(medication, time, formatHour(time))
            }
            baseCalendar.add(Calendar.HOUR_OF_DAY, frecuenciaHoras)
        }
        candidatas
    }
}

private fun horarioEnDia(inicioDelDia: Long, horario: String): Long {
    val partes = horario.split(":")
    return Calendar.getInstance().apply {
        timeInMillis = inicioDelDia
        set(Calendar.HOUR_OF_DAY, partes.getOrNull(0)?.toIntOrNull() ?: 8)
        set(Calendar.MINUTE, partes.getOrNull(1)?.toIntOrNull() ?: 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun obtenerSemanaEscritorio(fechaSeleccionada: Long): List<DashboardWeekDay> {
    val hoy = inicioDelDia(System.currentTimeMillis())
    val inicioSemana = inicioDeSemana(fechaSeleccionada)

    return (0 until 7).map { offset ->
        val fecha = moverFecha(inicioSemana, offset)
        DashboardWeekDay(
            timestamp = fecha,
            dayLabel = formatDashboardWeekDayLabel(fecha),
            dayNumber = Calendar.getInstance().apply { timeInMillis = fecha }.get(Calendar.DAY_OF_MONTH).toString(),
            isToday = fecha == hoy
        )
    }
}

private fun inicioDeSemana(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = inicioDelDia(timestamp)
        while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            add(Calendar.DAY_OF_YEAR, -1)
        }
    }.timeInMillis
}

internal fun moverFecha(timestamp: Long, dias: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = inicioDelDia(timestamp)
        add(Calendar.DAY_OF_YEAR, dias)
    }.timeInMillis
}

internal fun calcularDiferenciaDias(fechaBase: Long, fechaObjetivo: Long): Int {
    val inicioBase = inicioDelDia(fechaBase)
    val inicioObjetivo = inicioDelDia(fechaObjetivo)
    val diferenciaMillis = inicioObjetivo - inicioBase
    return (diferenciaMillis / (24L * 60L * 60L * 1000L)).toInt()
}

private fun formatDashboardWeekDayLabel(timestamp: Long): String {
    return when (Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "X"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        Calendar.SATURDAY -> "S"
        else -> "D"
    }
}

internal fun formatDashboardDateSummary(timestamp: Long): String {
    val localeEs = Locale.forLanguageTag("es-ES")
    val fecha = SimpleDateFormat("dd MMM", localeEs).format(Date(timestamp)).lowercase(localeEs)
    return if (inicioDelDia(timestamp) == inicioDelDia(System.currentTimeMillis())) {
        "Hoy, $fecha"
    } else {
        fecha.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VademecumDropdown(
    label: String,
    options: List<String>,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded && options.isNotEmpty(),
        onExpandedChange = {
            if (options.isNotEmpty()) {
                onExpandedChange()
            }
        }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            enabled = options.isNotEmpty(),
            label = { Text(label) },
            placeholder = {
                if (options.isEmpty()) {
                    Text("Selecciona primero un medicamento")
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && options.isNotEmpty())
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded && options.isNotEmpty(),
            onDismissRequest = onDismiss
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicamentoFormPreview() {
    ControlMedicamentosTheme {
        MedicamentoForm(
            fallAlertPanelState = remember { mutableStateOf(false) }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Selector visual de icono y color del insumo
// ──────────────────────────────────────────────────────────────────────────────

// Icono de cápsula personalizado: pastilla horizontal con divisor central
internal val IconCapsula: ImageVector = ImageVector.Builder(
    name = "Capsula",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    // Contorno de la cápsula (pastilla horizontal)
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(7f, 7f)
        lineTo(17f, 7f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 17f, y1 = 17f)
        lineTo(7f, 17f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 7f, y1 = 7f)
        close()
    }
    // Línea divisoria central
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 7f)
        lineTo(12f, 17f)
    }
}.build()

internal val FORMAS_MEDICAMENTO = listOf(
    Triple("capsula",   IconCapsula,             "Cilindro"),
    Triple("redonda",   Icons.Filled.Circle,     "Redonda"),
    Triple("ovalada",   Icons.Filled.Lens,       "Ovalada"),
    Triple("gota",      Icons.Filled.WaterDrop,  "Gota"),
    Triple("inyeccion", Icons.Filled.History,    "Líquido"),
    Triple("parche",    Icons.Filled.Description, "Externo"),
    Triple("frasco",    Icons.Filled.Edit,       "Contenedor")
)

// Formas que admiten segundo color (cuerpo bicolor)
internal val FORMAS_DOS_COLORES = setOf("capsula", "ovalada")

private val COLORES_MEDICAMENTO = listOf(
    Color(0xFF4CAF50) to "Verde",
    Color(0xFF8BC34A) to "Lima",
    Color(0xFFFFEB3B) to "Amarillo",
    Color(0xFFFF9800) to "Naranja",
    Color(0xFFF44336) to "Rojo",
    Color(0xFF2196F3) to "Azul",
    Color(0xFF9C27B0) to "Violeta",
    Color(0xFFFFFFFF) to "Blanco",
    Color(0xFF9E9E9E) to "Gris",
    Color(0xFF795548) to "Marrón",
    Color(0xFF00BCD4) to "Cyan",
    Color(0xFF000000) to "Negro"
)

@Composable
internal fun SelectorIconoMedicina(
    formaActual: String,
    colorActual: Color,
    color2Actual: Color,
    onFormaChange: (String) -> Unit,
    onColorChange: (Color) -> Unit,
    onColor2Change: (Color) -> Unit
) {
    val esDosColores = formaActual in FORMAS_DOS_COLORES

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Encabezado ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ícono identificativo",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (formaActual.isNotBlank()) {
                val icono = FORMAS_MEDICAMENTO.firstOrNull { it.first == formaActual }?.second
                if (icono != null) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = colorActual,
                        modifier = Modifier.size(24.dp)
                    )
                    if (esDosColores) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color2Actual)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }

        // ── Fila 1: Formas ─────────────────────────────────────────────
        Text(
            text = "Forma",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            items(FORMAS_MEDICAMENTO) { (forma, icono, label) ->
                OpcionForma(
                    forma = forma,
                    icono = icono,
                    label = label,
                    formaSeleccionada = formaActual,
                    colorSeleccionado = colorActual,
                    onFormaChange = {
                        onFormaChange(it)
                        if (it !in FORMAS_DOS_COLORES) onColor2Change(Color(0xFFFFFFFF))
                    }
                )
            }
        }

        // ── Fila 2: Color primario ──────────────────────────────────────
        Text(
            text = if (esDosColores) "Color principal" else "Color",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            items(COLORES_MEDICAMENTO) { (color, descripcion) ->
                OpcionColor(
                    color = color,
                    descripcion = descripcion,
                    colorSeleccionado = colorActual,
                    onColorChange = onColorChange
                )
            }
        }

        // ── Fila 3: Color secundario (SOLO para cápsulas / ovaladas) ─────
        if (esDosColores) {
            Text(
                text = "Segundo color",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                items(COLORES_MEDICAMENTO) { (color, descripcion) ->
                    OpcionColor(
                        color = color,
                        descripcion = descripcion,
                        colorSeleccionado = color2Actual,
                        onColorChange = onColor2Change
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionForma(
    forma: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    formaSeleccionada: String,
    colorSeleccionado: Color,
    onFormaChange: (String) -> Unit
) {
    val esSeleccionado = forma == formaSeleccionada
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable { onFormaChange(forma) }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (esSeleccionado) Color.White.copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.07f)
                )
                .then(
                    if (esSeleccionado)
                        Modifier.border(2.dp, Color.Cyan, CircleShape)
                    else
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = forma,
                tint = if (esSeleccionado) colorSeleccionado else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (esSeleccionado) Color.Cyan else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun OpcionColor(
    color: Color,
    descripcion: String,
    colorSeleccionado: Color,
    onColorChange: (Color) -> Unit
) {
    val esSeleccionado = color == colorSeleccionado
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (esSeleccionado)
                    Modifier.border(3.dp, Color.Cyan, CircleShape)
                else
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape)
            )
            .clickable { onColorChange(color) }
    )
}

@Composable
private fun HistorialCiclosScreen(
    pacienteId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ciclos by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(emptyList())
        else database.cicloMenstrualDao().observarPorPaciente(pacienteId)
    }.collectAsState(initial = emptyList())

    val ciclosPorAnio = remember(ciclos) {
        ciclos.groupBy { ciclo ->
            SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(ciclo.fechaInicio))
        }.toSortedMap(compareByDescending { it })
    }

    var anioExpandido by remember { mutableStateOf<String?>(null) }
    var cicloAEditar by remember { mutableStateOf<CicloMenstrual?>(null) }
    var mostrarDialogoNotas by remember { mutableStateOf(false) }
    var notasTemp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE91E63), Color(0xFFFFF4FA), Color(0xFFFCE4EC))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFD81B60), Color(0xFFC218A8), Color(0xFFE91E63))))
                .statusBarsPadding()
                .padding(start = 16.dp, end = 24.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver", tint = Color.White)
            }
            Text("Historial de ciclos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text("${ciclos.size}", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        }

        if (ciclos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🌸", fontSize = 64.sp)
                    Text(
                        "Sin registros de ciclos",
                        color = Color(0xFFC2185B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Los ciclos registrados aparecerán aquí organizados por año.",
                        color = Color(0xFF77717A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ciclosPorAnio.forEach { (anio, ciclosDelAnio) ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { anioExpandido = if (anioExpandido == anio) null else anio },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "📅 $anio",
                                            color = Color(0xFFAD1457),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFFC1DC))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${ciclosDelAnio.size} ciclos",
                                                color = Color(0xFFE91E63),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (anioExpandido == anio) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (anioExpandido == anio) "Colapsar" else "Expandir",
                                        tint = Color(0xFFAD1457)
                                    )
                                }

                                if (anioExpandido == anio) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ciclosDelAnio.forEachIndexed { index, ciclo ->
                                        if (index > 0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4FA)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text("🩸", fontSize = 28.sp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            formatDate(ciclo.fechaInicio),
                                                            color = Color(0xFF3D3A45),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                        Text(
                                                            "${ciclo.duracionDias}d sangrado · ciclo ${ciclo.duracionCicloDias}d",
                                                            color = Color(0xFF8A8490),
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                cicloAEditar = ciclo
                                                                notasTemp = ciclo.notas
                                                                mostrarDialogoNotas = true
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Editar notas",
                                                                tint = Color(0xFFAD1457),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    database.cicloMenstrualDao().eliminar(ciclo)
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Eliminar ciclo",
                                                                tint = Color(0xFF9E9E9E),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (ciclo.sintomas.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        "🩺 ${ciclo.sintomas}",
                                                        color = Color(0xFFAD1457),
                                                        fontSize = 12.sp,
                                                        lineHeight = 17.sp
                                                    )
                                                }
                                                if (ciclo.notas.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "📝 ${ciclo.notas}",
                                                        color = Color(0xFF77717A),
                                                        fontSize = 12.sp,
                                                        lineHeight = 17.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (mostrarDialogoNotas && cicloAEditar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNotas = false; cicloAEditar = null },
            title = { Text("Editar notas del ciclo") },
            text = {
                OutlinedTextField(
                    value = notasTemp,
                    onValueChange = { notasTemp = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.cicloMenstrualDao().actualizar(
                                cicloAEditar!!.copy(notas = notasTemp)
                            )
                            withContext(Dispatchers.Main) {
                                mostrarDialogoNotas = false
                                cicloAEditar = null
                                Toast.makeText(context, "Notas actualizadas", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNotas = false; cicloAEditar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ControlEmbarazoScreen(
    pacienteId: Int,
    nombrePaciente: String,
    pesoPaciente: String,
    pesoUnidadPaciente: String,
    estaturaPaciente: String,
    estaturaUnidadPaciente: String,
    database: AppDatabase,
    onVolver: () -> Unit,
    onIrAPediatrico: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val embarazo by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(null)
        else database.controlEmbarazoDao().observarEmbarazoActivo(pacienteId)
    }.collectAsState(initial = null)
    val historialEmbarazos by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(emptyList())
        else database.controlEmbarazoDao().observarTodos(pacienteId)
    }.collectAsState(initial = emptyList())
    val embarazosTerminados = historialEmbarazos.filter { !it.activo }
    val visitas by remember(embarazo?.id) {
        if (embarazo == null) flowOf(emptyList<VisitaPrenatal>())
        else database.visitaPrenatalDao().observarPorEmbarazo(embarazo!!.id)
    }.collectAsState(initial = emptyList())
    val interrupciones by remember(pacienteId) {
        if (pacienteId <= 0) flowOf(emptyList())
        else database.controlEmbarazoDao().observarInterrumpidos(pacienteId)
    }.collectAsState(initial = emptyList())
    var mostrarDialogoIniciar by remember { mutableStateOf(false) }
    var mostrarEnhorabuena by remember { mutableStateOf(false) }
    var mostrarDialogoVisita by remember { mutableStateOf(false) }
    var mostrarDialogoParto by remember { mutableStateOf(false) }
    var trimestre1Expandido by remember { mutableStateOf(true) }
    var trimestre2Expandido by remember { mutableStateOf(false) }
    var trimestre3Expandido by remember { mutableStateOf(false) }
    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    var mostrarDialogoPerdida by remember { mutableStateOf(false) }
    var historialInterrupcionesExpandido by remember { mutableStateOf(false) }
    var interrupcionAEditar by remember { mutableStateOf<ControlEmbarazo?>(null) }
    var interrupcionABorrar by remember { mutableStateOf<ControlEmbarazo?>(null) }
    var mostrarHistorialEmbarazos by remember { mutableStateOf(false) }
    var mostrarFelicitacionParto by remember { mutableStateOf(false) }
    var bebesRegistrados by remember { mutableStateOf<List<BebePartoData>>(emptyList()) }

    if (mostrarEnhorabuena) {
        EnhorabuenaEmbarazoScreen(nombrePaciente = nombrePaciente, onFinish = { mostrarEnhorabuena = false })
        return
    }

    if (mostrarFelicitacionParto) {
        FelicitacionPartoScreen(
            nombrePaciente = nombrePaciente,
            bebes = bebesRegistrados,
            onFinish = { mostrarFelicitacionParto = false; onIrAPediatrico() }
        )
        return
    }

    if (mostrarHistorialEmbarazos) {
        HistorialEmbarazosScreen(
            embarazosTerminados = embarazosTerminados,
            database = database,
            onVolver = { mostrarHistorialEmbarazos = false }
        )
        return
    }

    if (embarazo == null) {
        val sdfHist = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3E5F5)).verticalScroll(rememberScrollState()).padding(24.dp)) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFF6A1B9A))
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("🤰", fontSize = 64.sp)
                    Text("Seguimiento prenatal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C), textAlign = TextAlign.Center)
                    Text("Introduce la fecha de tu última regla (FUR) para comenzar el seguimiento de embarazo y ver predicciones, controles y visitas prenatales.", fontSize = 14.sp, color = Color(0xFF757575), textAlign = TextAlign.Center)
                    Button(onClick = { mostrarDialogoIniciar = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                        Text("🤰  Iniciar seguimiento de embarazo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (embarazosTerminados.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { mostrarHistorialEmbarazos = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Historial de embarazos (${embarazosTerminados.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    } else {
        val fur = embarazo!!.fechaUltimaRegla
        val ahora = System.currentTimeMillis()
        val semanas = ((ahora - fur) / (7L * 24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        val meses = semanas / 4
        val semanasResto = semanas % 4
        val trimestre = when { semanas <= 12 -> "1er trim."; semanas <= 27 -> "2do trim."; else -> "3er trim." }
        val ppp = embarazo!!.fechaProbableParto
        val diasRestantes = ((ppp - ahora) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        val progreso = (semanas / 40f).coerceIn(0f, 1f)
        val tamanoBebe = tamanoBebePorSemana(semanas)
        val controles1 = contactosPrenatales(1)
        val controles2 = contactosPrenatales(2)
        val controles3 = contactosPrenatales(3)
        val recomendacionPeso = recomendacionPesoEmbarazo(pesoPaciente, pesoUnidadPaciente, estaturaPaciente, estaturaUnidadPaciente)
        val completados1 = visitas.count { it.semanasGestacion <= 12 }
        val completados2 = visitas.count { it.semanasGestacion in 13..27 }
        val completados3 = visitas.count { it.semanasGestacion >= 28 }
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3E5F5))) {
            Row(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)))).padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                Text("🤰  Seguimiento reproductivo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (embarazo!!.esPrueba) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🧪", fontSize = 18.sp)
                            Text("Prueba de funcionamiento", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$semanas semanas", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                                Text("$meses meses y $semanasResto sem", fontSize = 14.sp, color = Color(0xFF757575))
                            }
                            Box(modifier = Modifier.background(Color(0xFFE1BEE7), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(trimestre, color = Color(0xFF6A1B9A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tamanoBebe.first, fontSize = 28.sp)
                                Column { Text("Tu bebé ahora", fontSize = 12.sp, color = Color(0xFF757575)); Text("tiene el tamaño de ${tamanoBebe.second}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A148C)) }
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE1BEE7))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progreso).clip(RoundedCornerShape(6.dp)).background(Color(0xFF7B1FA2)))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("FUR", fontSize = 11.sp, color = Color(0xFF9E9E9E)); Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fur)), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4A148C)) }
                            Column(horizontalAlignment = Alignment.End) { Text("Fecha probable de parto", fontSize = 11.sp, color = Color(0xFF9E9E9E)); Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ppp)), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4A148C)) }
                        }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFEDE7F6)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("⏳  Faltan $diasRestantes días para el parto", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                        }
                    }
                }
                SenalesAlertaEmbarazoCard()
                EstiloVidaEmbarazoCard(recomendacionPeso = recomendacionPeso)
                Text("8 contactos prenatales recomendados (OMS)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A148C))
                TrimestreControlesEmbarazo("1er trimestre  (sem 1–12)", controles1, completados1, trimestre1Expandido) { trimestre1Expandido = !trimestre1Expandido }
                TrimestreControlesEmbarazo("2º trimestre  (sem 13–27)", controles2, completados2, trimestre2Expandido) { trimestre2Expandido = !trimestre2Expandido }
                TrimestreControlesEmbarazo("3er trimestre  (sem 28–40)", controles3, completados3, trimestre3Expandido) { trimestre3Expandido = !trimestre3Expandido }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visitas prenatales", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A148C))
                    Text("${visitas.size} registradas", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                }
                Button(onClick = { mostrarDialogoVisita = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                    Text("+  Añadir visita prenatal", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (visitas.isEmpty()) {
                    Text("Todavía no hay visitas registradas.", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                } else {
                    visitas.forEach { visita -> VisitaPrenatalCard(visita = visita, onEliminar = { coroutineScope.launch(Dispatchers.IO) { database.visitaPrenatalDao().eliminar(visita) } }) }
                }
                OutlinedButton(onClick = { mostrarDialogoParto = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, Color(0xFFE91E63)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE91E63))) {
                    Text("🚀  Registrar parto / finalizar embarazo", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { mostrarConfirmarEliminar = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, Color(0xFFB71C1C)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C))) {
                    Text("🗑  Eliminar seguimiento de embarazo", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { mostrarDialogoPerdida = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Registrar pérdida o interrupción del embarazo", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (mostrarDialogoIniciar) {
        IniciarEmbarazoDialog(onDismiss = { mostrarDialogoIniciar = false }, onIniciar = { fur, notas, esPrueba ->
            val ppp = fur + (280L * 24 * 60 * 60 * 1000)
            coroutineScope.launch(Dispatchers.IO) {
                database.controlEmbarazoDao().insertar(ControlEmbarazo(patientId = pacienteId, fechaUltimaRegla = fur, fechaProbableParto = ppp, notas = notas, fechaRegistro = System.currentTimeMillis(), esPrueba = esPrueba))
                withContext(Dispatchers.Main) { mostrarDialogoIniciar = false; mostrarEnhorabuena = true }
            }
        })
    }
    if (mostrarDialogoVisita && embarazo != null) {
        VisitaPrenatalDialog(
            embarazoId = embarazo!!.id,
            semanasActuales = ((System.currentTimeMillis() - embarazo!!.fechaUltimaRegla) / (7L * 24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0),
            onDismiss = { mostrarDialogoVisita = false },
            onGuardar = { visita -> coroutineScope.launch(Dispatchers.IO) { database.visitaPrenatalDao().insertar(visita); withContext(Dispatchers.Main) { mostrarDialogoVisita = false; Toast.makeText(context, "Visita registrada", Toast.LENGTH_SHORT).show() } } }
        )
    }
    if (mostrarDialogoParto && embarazo != null) {
        RegistrarPartoDialog(onDismiss = { mostrarDialogoParto = false }, onRegistrar = { fechaParto, tipoParto, notas, bebes ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val embarazoId = embarazo!!.id
                    database.controlEmbarazoDao().registrarParto(id = embarazoId, fechaParto = fechaParto, tipoParto = tipoParto, notas = notas)
                    val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaParto))
                    bebes.forEach { bebe ->
                        database.bebeRecienNacidoDao().insertar(
                            BebeRecienNacido(
                                embarazoId = embarazoId,
                                patientId = pacienteId,
                                nombre = bebe.nombre.ifBlank { "Bebé" },
                                sexo = bebe.sexo,
                                fechaNacimiento = fechaParto,
                                pesoAlNacer = bebe.peso,
                                tallaAlNacer = bebe.talla,
                                notas = bebe.observaciones
                            )
                        )
                        // Crear NinoEntity y esquema de vacunas
                        val ninoId = database.ninoDao().insertNino(
                            NinoEntity(
                                patientId = pacienteId,
                                embarazoId = embarazoId,
                                nombre = bebe.nombre.ifBlank { "Bebé" },
                                fechaNacimiento = fechaFormateada,
                                sexo = bebe.sexo,
                                notasParto = notas,
                                esPrueba = embarazo!!.esPrueba
                            )
                        )
                        // Solo insertar vacunas si ninoId es válido
                        if (ninoId > 0) {
                            database.vacunaDao().insertVacunas(ProtocoloVacunacion.generarEsquemaBase(ninoId))
                        }
                    }
                    withContext(Dispatchers.Main) { mostrarDialogoParto = false; bebesRegistrados = bebes; mostrarFelicitacionParto = true }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al registrar parto: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
    if (mostrarDialogoPerdida && embarazo != null) {
        var tipoPerdida by remember { mutableStateOf("") }
        var metodoPerdida by remember { mutableStateOf("") }
        var notasPerdida by remember { mutableStateOf("") }
        var expandedTipo by remember { mutableStateOf(false) }
        val tiposPerdida = listOf(
            "Aborto espontáneo",
            "Embarazo ectópico",
            "Pérdida por enfermedad materna",
            "Interrupción por malformación fetal",
            "Interrupción con metotrexato",
            "Interrupción farmacológica (misoprostol/mifepristona)",
            "Interrupción quirúrgica (legrado/aspiración)",
            "Muerte fetal intrauterina",
            "Otro"
        )
        AlertDialog(
            onDismissRequest = { mostrarDialogoPerdida = false },
            title = { Text("Registrar pérdida / interrupción", color = Color(0xFF4A148C), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta información es privada y solo se guarda en tu dispositivo.", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                    ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = it }) {
                        OutlinedTextField(
                            value = tipoPerdida,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de pérdida o interrupción") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                            tiposPerdida.forEach { tipo ->
                                DropdownMenuItem(text = { Text(tipo) }, onClick = { tipoPerdida = tipo; expandedTipo = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = metodoPerdida,
                        onValueChange = { metodoPerdida = it },
                        label = { Text("Plan personal o procedimiento (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notasPerdida,
                        onValueChange = { notasPerdida = it },
                        label = { Text("Notas clínicas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tipoPerdida.isBlank()) {
                            Toast.makeText(context, "Selecciona el tipo de pérdida", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val emb = embarazo!!
                        mostrarDialogoPerdida = false
                        coroutineScope.launch(Dispatchers.IO) {
                            database.controlEmbarazoDao().actualizar(
                                emb.copy(
                                    activo = false,
                                    estadoEmbarazo = "INTERRUMPIDO",
                                    fechaFin = System.currentTimeMillis(),
                                    tipoInterrupcion = tipoPerdida,
                                    metodoInterrupcion = metodoPerdida.ifBlank { null },
                                    notasInterrupcion = notasPerdida.ifBlank { null }
                                )
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Registro guardado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Guardar", color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPerdida = false }) {
                    Text("Cancelar", color = Color(0xFF757575))
                }
            },
            containerColor = Color.White
        )
    }
    if (mostrarConfirmarEliminar && embarazo != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            title = { Text("¿Eliminar seguimiento?", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará el seguimiento de embarazo y todas las visitas prenatales asociadas. Esta acción no se puede deshacer.", color = Color(0xFF424242)) },
            confirmButton = {
                TextButton(onClick = {
                    val emb = embarazo!!
                    mostrarConfirmarEliminar = false
                    coroutineScope.launch(Dispatchers.IO) {
                        database.controlEmbarazoDao().eliminar(emb)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Seguimiento eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = false }) {
                    Text("Cancelar", color = Color(0xFF757575))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun HistorialEmbarazosScreen(
    embarazosTerminados: List<ControlEmbarazo>,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var editandoEmbarazo by remember { mutableStateOf<ControlEmbarazo?>(null) }
    var embarazoAEliminar by remember { mutableStateOf<ControlEmbarazo?>(null) }

    // Campos de edición
    var editTipoParto by remember { mutableStateOf("") }
    var editNotasParto by remember { mutableStateOf("") }
    var editTipoInterrupcion by remember { mutableStateOf("") }
    var editMetodoInterrupcion by remember { mutableStateOf("") }
    var editNotasInterrupcion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3E5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                "Historial de embarazos",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (embarazosTerminados.isEmpty()) {
                item {
                    Text(
                        "No hay registros en el historial.",
                        color = Color(0xFF757575),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(embarazosTerminados, key = { it.id }) { emb ->
                    val estaEditando = editandoEmbarazo?.id == emb.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "FUR: ${sdf.format(Date(emb.fechaUltimaRegla))}",
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!estaEditando) {
                                    Row {
                                        IconButton(onClick = {
                                            editandoEmbarazo = emb
                                            editTipoParto = emb.tipoPartoRegistrado
                                            editNotasParto = emb.notasParto
                                            editTipoInterrupcion = emb.tipoInterrupcion ?: ""
                                            editMetodoInterrupcion = emb.metodoInterrupcion ?: ""
                                            editNotasInterrupcion = emb.notasInterrupcion ?: ""
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF6A1B9A))
                                        }
                                        IconButton(onClick = { embarazoAEliminar = emb }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFB71C1C))
                                        }
                                    }
                                }
                            }

                            if (emb.fechaFin != null) {
                                Text("Fin: ${sdf.format(Date(emb.fechaFin!!))}", fontSize = 12.sp, color = Color.Black)
                            } else if (emb.fechaParto != null && emb.fechaParto!! > 0) {
                                Text("Parto: ${sdf.format(Date(emb.fechaParto!!))}", fontSize = 12.sp, color = Color.Black)
                            }

                            if (estaEditando) {
                                Spacer(Modifier.height(8.dp))
                                if (emb.tipoInterrupcion != null) {
                                    OutlinedTextField(
                                        value = editTipoInterrupcion,
                                        onValueChange = { editTipoInterrupcion = it },
                                        label = { Text("Tipo de interrupción", color = Color(0xFF4A148C)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                                    )
                                    OutlinedTextField(
                                        value = editMetodoInterrupcion,
                                        onValueChange = { editMetodoInterrupcion = it },
                                        label = { Text("Plan personal", color = Color(0xFF4A148C)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                                    )
                                    OutlinedTextField(
                                        value = editNotasInterrupcion,
                                        onValueChange = { editNotasInterrupcion = it },
                                        label = { Text("Notas", color = Color(0xFF4A148C)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = editTipoParto,
                                        onValueChange = { editTipoParto = it },
                                        label = { Text("Tipo de parto", color = Color(0xFF4A148C)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                                    )
                                    OutlinedTextField(
                                        value = editNotasParto,
                                        onValueChange = { editNotasParto = it },
                                        label = { Text("Notas del parto", color = Color(0xFF4A148C)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { editandoEmbarazo = null }) {
                                        Text("Cancelar", color = Color(0xFF757575))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val actualizado = if (emb.tipoInterrupcion != null) {
                                                emb.copy(
                                                    tipoInterrupcion = editTipoInterrupcion.ifBlank { null },
                                                    metodoInterrupcion = editMetodoInterrupcion.ifBlank { null },
                                                    notasInterrupcion = editNotasInterrupcion.ifBlank { null }
                                                )
                                            } else {
                                                emb.copy(
                                                    tipoPartoRegistrado = editTipoParto,
                                                    notasParto = editNotasParto
                                                )
                                            }
                                            editandoEmbarazo = null
                                            coroutineScope.launch(Dispatchers.IO) {
                                                database.controlEmbarazoDao().actualizar(actualizado)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Registro actualizado", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                                    ) {
                                        Text("Guardar", color = Color.White)
                                    }
                                }
                            } else {
                                if (emb.esPrueba) {
                                    Text("🧪 Prueba de funcionamiento", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                                }
                                if (emb.tipoInterrupcion != null) {
                                    Text("⚠️ ${emb.tipoInterrupcion}", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                                    if (emb.metodoInterrupcion != null) {
                                        Text("Plan personal: ${emb.metodoInterrupcion}", fontSize = 12.sp, color = Color.Black)
                                    }
                                    if (emb.notasInterrupcion != null) {
                                        Text("📝 ${emb.notasInterrupcion}", fontSize = 12.sp, color = Color.Black)
                                    }
                                } else if (emb.tipoPartoRegistrado.isNotBlank()) {
                                    Text("👶 Parto: ${emb.tipoPartoRegistrado}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                    if (emb.notasParto.isNotBlank()) {
                                        Text("📝 ${emb.notasParto}", fontSize = 12.sp, color = Color.Black)
                                    }
                                }
                                val semanasEmb = ((((emb.fechaFin ?: emb.fechaParto ?: emb.fechaRegistro) - emb.fechaUltimaRegla) / (7L * 24 * 60 * 60 * 1000)).toInt()).coerceAtLeast(0)
                                Text("Duración: $semanasEmb semanas", fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    embarazoAEliminar?.let { emb ->
        AlertDialog(
            onDismissRequest = { embarazoAEliminar = null },
            title = { Text("Eliminar registro", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Eliminar este registro del historial de embarazos? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val aEliminar = emb
                    embarazoAEliminar = null
                    coroutineScope.launch(Dispatchers.IO) {
                        database.controlEmbarazoDao().eliminar(aEliminar)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { embarazoAEliminar = null }) {
                    Text("Cancelar", color = Color(0xFF757575))
                }
            },
            containerColor = Color.White
        )
    }
}

private fun tamanoBebePorSemana(semanas: Int): Pair<String, String> = when (semanas) {
    in 0..4 -> "🌸" to "una semilla de amapola"
    in 5..6 -> "🫐" to "un arándano"
    in 7..8 -> "🫒" to "una aceituna"
    in 9..10 -> "🍓" to "una fresa"
    in 11..12 -> "🍋" to "un limón"
    in 13..14 -> "🍊" to "una mandarina"
    in 15..16 -> "🥑" to "un aguacate"
    in 17..18 -> "🍐" to "una pera"
    in 19..20 -> "🍌" to "un plátano"
    in 21..22 -> "🌽" to "una mazorca de maíz"
    in 23..25 -> "🥕" to "una zanahoria grande"
    in 26..28 -> "🥦" to "una cabeza de brócoli"
    in 29..31 -> "🥥" to "un coco"
    in 32..34 -> "🍍" to "una piña"
    in 35..37 -> "🎃" to "una calabaza mediana"
    else -> "👶" to "un bebé listo para nacer"
}

private fun contactosPrenatales(trimestre: Int): List<Pair<String, String>> = when (trimestre) {
    1 -> listOf(
        "1" to "Etapa 1 · ≤ semana 12",
        "🩺" to "Confirmar embarazo, fecha última regla y datos personales",
        "⚖️" to "Registro de presión, peso y bienestar general",
        "🧪" to "Registros iniciales segun tus necesidades"
    )
    2 -> listOf(
        "2" to "Etapa 2 · semana 20",
        "3" to "Etapa 3 · semana 26",
        "📷" to "Control de crecimiento y seguimiento del desarrollo",
        "🩸" to "Seguimiento y registros personales"
    )
    else -> listOf(
        "4" to "Etapa 4 · semana 30",
        "5" to "Etapa 5 · semana 34",
        "6" to "Etapa 6 · semana 36",
        "7" to "Etapa 7 · semana 38",
        "8" to "Etapa 8 · semana 40",
        "⚖️" to "Registro de presión, peso, hidratación y bienestar",
        "👶" to "Seguimiento de movimientos del bebé",
        "🏥" to "Preparación para el parto y planificación del traslado"
    )
}

private fun recomendacionPesoEmbarazo(
    peso: String,
    pesoUnidad: String,
    estatura: String,
    estaturaUnidad: String
): String {
    val pesoKg = peso.replace(',', '.').toFloatOrNull()?.let {
        if (pesoUnidad.equals("lb", ignoreCase = true) || pesoUnidad.equals("lbs", ignoreCase = true)) it * 0.453592f else it
    }
    val estaturaMetros = estatura.replace(',', '.').toFloatOrNull()?.let {
        if (estaturaUnidad.equals("cm", ignoreCase = true)) it / 100f else it
    }
    if (pesoKg == null || estaturaMetros == null || estaturaMetros <= 0f) {
        return "Registra peso y estatura en el perfil para calcular el rango recomendado."
    }
    val imc = pesoKg / (estaturaMetros * estaturaMetros)
    val rango = when {
        imc < 18.5f -> "12.5 a 18 kg"
        imc < 25f -> "11.5 a 16 kg"
        imc < 30f -> "7 a 11.5 kg"
        else -> "5 a 9 kg"
    }
    val categoria = when {
        imc < 18.5f -> "bajo peso"
        imc < 25f -> "peso normal"
        imc < 30f -> "sobrepeso"
        else -> "obesidad"
    }
    return "IMC previo aprox. ${String.format(Locale.getDefault(), "%.1f", imc)} ($categoria): aumento recomendado $rango."
}

@Composable
private fun SenalesAlertaEmbarazoCard() {
    val senales = listOf(
        "Presión arterial alta, dolor de cabeza intenso o visión borrosa",
        "Sangrado vaginal o salida de líquido",
        "Dolor abdominal fuerte o contracciones antes de tiempo",
        "Fiebre, convulsiones, dificultad para respirar o hinchazón marcada",
        "Disminución o ausencia de movimientos fetales"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🚨", fontSize = 24.sp)
                Column {
                    Text("Señales de alerta", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Acceso rápido: revisar de inmediato si aparece alguna.", color = Color(0xFF8A4B4B), fontSize = 12.sp)
                }
            }
            senales.forEach { senal ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    Text(senal, color = Color(0xFF4A2A2A), fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EstiloVidaEmbarazoCard(recomendacionPeso: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Estilo de vida saludable", color = Color(0xFF4A148C), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(recomendacionPeso, color = Color(0xFF6A1B9A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            LifestyleInfoRow("🥗", "Nutricion", "Proteinas, acido folico, hierro y calcio. No es comer por dos: es duplicar nutrientes.")
            LifestyleInfoRow("🚫", "Evitar", "Alcohol, tabaco, carnes crudas, pescados altos en mercurio y cafeina > 200 mg/dia.")
            LifestyleInfoRow("🚶", "Actividad fisica", "Objetivo: 150 min/semana de actividad moderada si no hay contraindicacion medica.")
            LifestyleInfoRow("🏊", "Recomendado", "Caminar, natacion/gimnasia acuatica y yoga prenatal. Evitar contacto, caidas y supino tras 1er trimestre.")
        }
    }
}

@Composable
private fun LifestyleInfoRow(icono: String, titulo: String, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(icono, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color(0xFF4A148C), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(texto, color = Color(0xFF616161), fontSize = 13.sp)
        }
    }
}

@Composable
private fun TrimestreControlesEmbarazo(titulo: String, controles: List<Pair<String, String>>, completados: Int, expandido: Boolean, onToggle: () -> Unit) {
    val total = controles.size
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF4A148C), modifier = Modifier.weight(1f))
                Box(modifier = Modifier.background(if (completados == total && total > 0) Color(0xFF4CAF50) else Color(0xFFEDE7F6), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("$completados/$total", fontSize = 11.sp, color = if (completados == total && total > 0) Color.White else Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Icon(if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF9C27B0))
            }
            if (expandido) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDE7F6)))
                controles.forEachIndexed { i, (icono, texto) ->
                    val partes = texto.split(" · ")
                    val nombre = partes.firstOrNull() ?: texto
                    val semana = partes.getOrNull(1) ?: ""
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(icono, fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nombre, fontSize = 13.sp, color = Color(0xFF212121))
                            if (semana.isNotBlank()) Text(semana, fontSize = 11.sp, color = Color(0xFF9C27B0))
                        }
                        Box(modifier = Modifier.size(22.dp).border(1.5.dp, Color(0xFF9E9E9E), CircleShape))
                    }
                    if (i < controles.lastIndex) Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color(0xFFEEEEEE)))
                }
            }
        }
    }
}

@Composable
private fun VisitaPrenatalCard(visita: VisitaPrenatal, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sem ${visita.semanasGestacion} — ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(visita.fecha))}", fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                IconButton(onClick = onEliminar, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp)) }
            }
            if (visita.presionArterial.isNotBlank()) Text("🩸 TA: ${visita.presionArterial}", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.peso != null) Text("⚖️ Peso: ${visita.peso} kg", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.alturaUterina != null) Text("📏 AU: ${visita.alturaUterina} cm", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.frecuenciaCardiacaFetal != null) Text("💓 FCF: ${visita.frecuenciaCardiacaFetal} lpm", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.hemoglobina != null) Text("🧪 Hb: ${visita.hemoglobina} g/dL", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.glucemia != null) Text("🍬 Glucemia: ${visita.glucemia} mg/dL", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.edemas) Text("⚠️ Edemas presentes", fontSize = 12.sp, color = Color(0xFFE65100))
            if (visita.proteinasOrina) Text("⚠️ Proteínas en orina", fontSize = 12.sp, color = Color(0xFFE65100))
            if (visita.suplementos.isNotBlank()) Text("💊 ${visita.suplementos}", fontSize = 12.sp, color = Color(0xFF616161))
            if (visita.observaciones.isNotBlank()) Text("📝 ${visita.observaciones}", fontSize = 12.sp, color = Color(0xFF757575))
            if (visita.facultativo.isNotBlank()) Text("👨‍⚕️ ${visita.facultativo}", fontSize = 12.sp, color = Color(0xFF9C27B0))
        }
    }
}

@Composable
private fun IniciarEmbarazoDialog(onDismiss: () -> Unit, onIniciar: (fur: Long, notas: String, esPrueba: Boolean) -> Unit) {
    val context = LocalContext.current
    var fur by remember { mutableStateOf(System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000) }
    var notas by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var opcionPrueba by remember { mutableStateOf(false) }
    val semanasCalc = ((System.currentTimeMillis() - fur) / (7L * 24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    val trimestreCalc = when { semanasCalc <= 12 -> "1er trimestre"; semanasCalc <= 27 -> "2do trimestre"; else -> "3er trimestre" }
    val pppCalc = fur + (280L * 24 * 60 * 60 * 1000)

    if (mostrarConfirmacion) {
        Dialog(onDismissRequest = { mostrarConfirmacion = false }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1040))) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Confirmar inicio", color = Color(0xFFCE93D8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Selecciona el tipo de seguimiento:", color = Color(0xFFE1BEE7), fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!opcionPrueba) Color(0xFF4A1070) else Color.Transparent)
                            .border(1.dp, Color(0xFF9C27B0), RoundedCornerShape(10.dp))
                            .clickable { opcionPrueba = false }
                            .padding(12.dp)
                    ) {
                        Checkbox(checked = !opcionPrueba, onCheckedChange = { opcionPrueba = false }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7B1FA2), uncheckedColor = Color(0xFFCE93D8), checkmarkColor = Color.White))
                        Column {
                            Text("Proceso real", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Se registrará como un proceso real", color = Color(0xFFE1BEE7), fontSize = 12.sp)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (opcionPrueba) Color(0xFF4A1070) else Color.Transparent)
                            .border(1.dp, Color(0xFF9C27B0), RoundedCornerShape(10.dp))
                            .clickable { opcionPrueba = true }
                            .padding(12.dp)
                    ) {
                        Checkbox(checked = opcionPrueba, onCheckedChange = { opcionPrueba = true }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7B1FA2), uncheckedColor = Color(0xFFCE93D8), checkmarkColor = Color.White))
                        Column {
                            Text("Prueba de funcionamiento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Todos los registros se marcarán como prueba", color = Color(0xFFE1BEE7), fontSize = 12.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { mostrarConfirmacion = false }, modifier = Modifier.weight(1f)) { Text("Volver", color = Color(0xFF9E9E9E)) }
                        Button(onClick = { onIniciar(fur, notas, opcionPrueba) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                            Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1040))) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Iniciar seguimiento", color = Color(0xFFCE93D8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Fecha de la última regla (FUR)", color = Color(0xFFE1BEE7), fontSize = 14.sp)
                    Button(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = fur }
                            DatePickerDialog(context, { _, y, m, d -> fur = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1070), contentColor = Color(0xFFCE93D8)),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0))
                    ) { Text("📅  FUR: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fur))}", fontWeight = FontWeight.Medium) }
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1060)), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Semanas: $semanasCalc · $trimestreCalc", color = Color(0xFFCE93D8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Fecha probable de parto: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(pppCalc))}", color = Color(0xFFE1BEE7), fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("Notas (opcional)", color = Color(0xFF9E9E9E)) }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9C27B0), unfocusedBorderColor = Color(0xFF6A1B9A), focusedTextColor = Color.White, unfocusedTextColor = Color(0xFFE1BEE7)))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = Color(0xFF9E9E9E)) }
                        Button(onClick = { mostrarConfirmacion = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                            Text("Iniciar seguimiento", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhorabuenaEmbarazoScreen(nombrePaciente: String, onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(5500); visible = false; onFinish() }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(800), label = "alpha_enhorabuena")
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF4A0E7A), Color(0xFF1A0030), Color(0xFF0D001A))))
            .alpha(alpha)
            .clickable { visible = false; onFinish() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Text("¡Enhorabuena! 🤰", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(nombrePaciente, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF48FB1), textAlign = TextAlign.Center)
            Text("¡Que sea un proceso\nmaravilloso! 💕", fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FelicitacionPartoScreen(nombrePaciente: String, bebes: List<BebePartoData>, onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(8000); visible = false; onFinish() }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(1000), label = "alpha_felicitacion_parto")

    val bgColor = if (bebes.size == 1) {
        when (bebes.first().sexo) {
            "Niño" -> listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF42A5F5))
            "Niña" -> listOf(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFF48FB1))
            else -> listOf(Color(0xFF4A0E7A), Color(0xFF1A0030), Color(0xFF6A1B9A))
        }
    } else {
        listOf(Color(0xFF4A0E7A), Color(0xFF1A0030), Color(0xFF6A1B9A))
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.radialGradient(bgColor))
            .alpha(alpha)
            .clickable { visible = false; onFinish() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("¡Felicidades! 🎉", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(nombrePaciente, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF8BBD0), textAlign = TextAlign.Center)

            Spacer(Modifier.height(8.dp))

            if (bebes.size == 1) {
                val bebe = bebes.first()
                val emoji = when (bebe.sexo) { "Niño" -> "👶💙"; "Niña" -> "👶💗"; else -> "👶✨" }
                Text(emoji, fontSize = 48.sp, textAlign = TextAlign.Center)
                if (bebe.nombre.isNotBlank()) {
                    Text("¡Bienvenid${if (bebe.sexo == "Niña") "a" else "o"},\n${bebe.nombre}!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                } else {
                    Text("¡Ha nacido tu bebé!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                }
                if (bebe.peso.isNotBlank()) {
                    Text("Peso: ${bebe.peso} kg", fontSize = 16.sp, color = Color(0xFFE1BEE7), textAlign = TextAlign.Center)
                }
                if (bebe.talla.isNotBlank()) {
                    Text("Talla: ${bebe.talla} cm", fontSize = 16.sp, color = Color(0xFFE1BEE7), textAlign = TextAlign.Center)
                }
            } else {
                Text("👶👶 ¡Nacieron tus bebés!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                bebes.forEachIndexed { i, bebe ->
                    val emoji = when (bebe.sexo) { "Niño" -> "💙"; "Niña" -> "💗"; else -> "✨" }
                    val nombre = bebe.nombre.ifBlank { "Bebé ${i + 1}" }
                    Text("$emoji $nombre", fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("¡Un momento mágico! 🌟", fontSize = 18.sp, color = Color(0xFFCE93D8), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VisitaPrenatalDialog(embarazoId: Int, semanasActuales: Int, onDismiss: () -> Unit, onGuardar: (VisitaPrenatal) -> Unit) {
    val context = LocalContext.current
    var fecha by remember { mutableStateOf(System.currentTimeMillis()) }
    var semanas by remember { mutableStateOf(semanasActuales.toString()) }
    var presionArterial by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var alturaUterina by remember { mutableStateOf("") }
    var fcf by remember { mutableStateOf("") }
    var hemoglobina by remember { mutableStateOf("") }
    var glucemia by remember { mutableStateOf("") }
    var edemas by remember { mutableStateOf(false) }
    var proteinasOrina by remember { mutableStateOf(false) }
    var suplementos by remember { mutableStateOf("Ácido fólico, Hierro, Yodo") }
    var observaciones by remember { mutableStateOf("") }
    var facultativo by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registrar visita prenatal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4A148C))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCE93D8)))
                Button(onClick = { val cal = Calendar.getInstance().apply { timeInMillis = fecha }; DatePickerDialog(context, { _, y, m, d -> fecha = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4A148C)), border = BorderStroke(1.dp, Color(0xFF9EA0AE))) { Text("📅  ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fecha))}") }
                val tfColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedLabelColor = Color(0xFF4A148C), unfocusedLabelColor = Color(0xFF424242))
                OutlinedTextField(value = semanas, onValueChange = { semanas = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Semanas de gestación") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                OutlinedTextField(value = presionArterial, onValueChange = { presionArterial = it }, label = { Text("Presión arterial") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                OutlinedTextField(value = peso, onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("kg", color = Color.Black) }, colors = tfColors)
                OutlinedTextField(value = alturaUterina, onValueChange = { alturaUterina = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, label = { Text("Altura uterina (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("cm", color = Color.Black) }, colors = tfColors)
                OutlinedTextField(value = fcf, onValueChange = { fcf = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Frec. cardíaca fetal (lpm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("lpm", color = Color.Black) }, colors = tfColors)
                OutlinedTextField(value = hemoglobina, onValueChange = { hemoglobina = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, label = { Text("Hemoglobina (g/dL)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("g/dL", color = Color.Black) }, colors = tfColors)
                OutlinedTextField(value = glucemia, onValueChange = { glucemia = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Glucemia (mg/dL)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, suffix = { Text("mg/dL", color = Color.Black) }, colors = tfColors)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = edemas, onCheckedChange = { edemas = it }); Text("Edemas presentes", fontSize = 14.sp, color = Color.Black) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = proteinasOrina, onCheckedChange = { proteinasOrina = it }); Text("Proteínas en orina", fontSize = 14.sp, color = Color.Black) }
                OutlinedTextField(value = suplementos, onValueChange = { suplementos = it }, label = { Text("Suplementos indicados") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = tfColors)
                OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = tfColors)
                OutlinedTextField(value = facultativo, onValueChange = { facultativo = it }, label = { Text("Médico / Centro") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cancelar") }
                    IconButton(onClick = { onGuardar(VisitaPrenatal(embarazoId = embarazoId, fecha = fecha, semanasGestacion = semanas.toIntOrNull() ?: semanasActuales, presionArterial = presionArterial, peso = peso.toFloatOrNull(), alturaUterina = alturaUterina.toFloatOrNull(), frecuenciaCardiacaFetal = fcf.toIntOrNull(), hemoglobina = hemoglobina.toFloatOrNull(), glucemia = glucemia.toFloatOrNull(), edemas = edemas, proteinasOrina = proteinasOrina, suplementos = suplementos, observaciones = observaciones, facultativo = facultativo)) }) { Icon(Icons.Filled.Save, contentDescription = "Guardar") }
                }
            }
        }
    }
}

@Composable
private fun RegistrarPartoDialog(
    onDismiss: () -> Unit,
    onRegistrar: (fechaParto: Long, tipoParto: String, notas: String, bebes: List<BebePartoData>) -> Unit
) {
    val context = LocalContext.current
    var fechaParto by remember { mutableStateOf(System.currentTimeMillis()) }
    var tipoParto by remember { mutableStateOf("Sencillo") }
    var notasParto by remember { mutableStateOf("") }
    var numBebes by remember { mutableStateOf(1) }

    // Datos del bebé (soporta múltiple)
    var bebesData by remember { mutableStateOf(listOf(BebePartoData())) }

    // Cuando cambia tipo de parto, ajustar número de bebés
    LaunchedEffect(tipoParto) {
        if (tipoParto == "Sencillo") {
            numBebes = 1
            bebesData = listOf(bebesData.firstOrNull() ?: BebePartoData())
        } else if (bebesData.size < 2) {
            bebesData = bebesData + BebePartoData()
            numBebes = 2
        }
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE1BEE7),
        focusedBorderColor = Color(0xFF9C27B0),
        unfocusedBorderColor = Color(0xFF6A1B9A),
        focusedLabelColor = Color(0xFFCE93D8),
        unfocusedLabelColor = Color(0xFF9E9E9E)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1040))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🚀", fontSize = 22.sp)
                        Text("Registrar parto", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // Fecha
                Text("Fecha del parto (dd/mm/aaaa)", color = Color(0xFFCE93D8), fontSize = 13.sp)
                Button(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = fechaParto }
                        DatePickerDialog(context, { _, y, m, d -> fechaParto = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1070), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF9C27B0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaParto)), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                // Tipo de parto
                Text("Tipo de parto", color = Color(0xFFCE93D8), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Sencillo", "Múltiple").forEach { tipo ->
                        val selected = tipoParto == tipo
                        Button(
                            onClick = { tipoParto = tipo },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFFE91E63) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (selected) Color(0xFFE91E63) else Color(0xFF9C27B0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(tipo, color = if (selected) Color.White else Color(0xFFCE93D8), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Si es múltiple, botón para agregar bebé
                if (tipoParto == "Múltiple") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Bebés: ${bebesData.size}", color = Color(0xFFE1BEE7), fontSize = 13.sp)
                        if (bebesData.size < 4) {
                            TextButton(onClick = { bebesData = bebesData + BebePartoData() }) {
                                Text("+ Agregar bebé", color = Color(0xFFCE93D8), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF6A1B9A)))

                // Datos de cada bebé
                Text("Datos de tu bebé", color = Color(0xFFCE93D8), fontSize = 15.sp, fontWeight = FontWeight.Medium)

                bebesData.forEachIndexed { index, bebe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1060)),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👶 Bebé${if (bebesData.size > 1) " ${index + 1}" else ""}", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (bebesData.size > 1) {
                                    IconButton(onClick = { bebesData = bebesData.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bebe.nombre,
                                onValueChange = { v -> bebesData = bebesData.toMutableList().also { it[index] = it[index].copy(nombre = v) } },
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = tfColors
                            )

                            Text("Sexo", color = Color(0xFFE1BEE7), fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                data class SexoOption(val label: String, val value: String, val selectedColor: Color, val selectedBorder: Color)
                                val sexoOptions = listOf(
                                    SexoOption("👶 Niño", "Niño", Color(0xFF81D4FA), Color(0xFF29B6F6)),
                                    SexoOption("👶 Niña", "Niña", Color(0xFFF48FB1), Color(0xFFEC407A)),
                                    SexoOption("⚪ No definido", "No definido", Color(0xFF9E9E9E), Color(0xFF757575))
                                )
                                sexoOptions.forEach { opt ->
                                    val sel = bebe.sexo == opt.value
                                    Button(
                                        onClick = { bebesData = bebesData.toMutableList().also { it[index] = it[index].copy(sexo = opt.value) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (sel) opt.selectedColor else Color.Transparent),
                                        border = BorderStroke(1.dp, if (sel) opt.selectedBorder else Color(0xFF6A1B9A)),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(opt.label, color = if (sel) Color.White else Color(0xFFCE93D8), fontSize = 12.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bebe.peso,
                                onValueChange = { v -> bebesData = bebesData.toMutableList().also { it[index] = it[index].copy(peso = v.filter { c -> c.isDigit() || c == '.' }.take(5)) } },
                                label = { Text("Peso al nacer (kg, opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = tfColors
                            )

                            OutlinedTextField(
                                value = bebe.talla,
                                onValueChange = { v -> bebesData = bebesData.toMutableList().also { it[index] = it[index].copy(talla = v.filter { c -> c.isDigit() || c == '.' }.take(4)) } },
                                label = { Text("Talla al nacer (cm, opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = tfColors
                            )

                            OutlinedTextField(
                                value = bebe.observaciones,
                                onValueChange = { v -> bebesData = bebesData.toMutableList().also { it[index] = it[index].copy(observaciones = v) } },
                                label = { Text("Observaciones al nacer (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = tfColors
                            )
                        }
                    }
                }

                // Notas del parto
                OutlinedTextField(
                    value = notasParto,
                    onValueChange = { notasParto = it },
                    label = { Text("Notas del parto (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = tfColors
                )

                // Botones
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFCE93D8)),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0)),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = { onRegistrar(fechaParto, tipoParto, notasParto, bebesData) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Registrar parto", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

private data class BebePartoData(
    val nombre: String = "",
    val sexo: String = "No definido",
    val peso: String = "",
    val talla: String = "",
    val observaciones: String = ""
)

// ===================== MÓDULO PEDIÁTRICO =====================

@Composable
internal fun ControlPediatricoMainScreen(pacienteId: Int, database: AppDatabase, onVolver: () -> Unit) {
    val ninos by database.ninoDao().getNinosByPatient(pacienteId).collectAsState(initial = emptyList())
    var ninoSeleccionado by remember { mutableStateOf<NinoEntity?>(null) }
    var pantallaActual by remember { mutableStateOf("lista") }

    BackHandler(enabled = pantallaActual != "lista") {
        if (pantallaActual == "dashboard") { pantallaActual = "lista"; ninoSeleccionado = null }
        else pantallaActual = "dashboard"
    }

    when (pantallaActual) {
        "lista" -> ListaNinosScreen(ninos = ninos, database = database, onSeleccionar = { nino -> ninoSeleccionado = nino; pantallaActual = "dashboard" }, onVolver = onVolver)
        "dashboard" -> ninoSeleccionado?.let { nino ->
            DashboardNinoScreen(nino = nino, database = database, onVolver = { pantallaActual = "lista"; ninoSeleccionado = null },
                onIrAVacunas = { pantallaActual = "vacunas" }, onIrAControles = { pantallaActual = "controles" }, onIrAEnfermedades = { pantallaActual = "enfermedades" })
        }
        "vacunas" -> ninoSeleccionado?.let { nino -> EsquemaVacunacionScreen(ninoId = nino.id, database = database, onVolver = { pantallaActual = "dashboard" }) }
        "controles" -> ninoSeleccionado?.let { nino -> ControlesPediatricosScreen(nino = nino, database = database, onVolver = { pantallaActual = "dashboard" }) }
        "enfermedades" -> ninoSeleccionado?.let { nino -> EnfermedadesAlergiasScreen(ninoId = nino.id, database = database, onVolver = { pantallaActual = "dashboard" }) }
    }
}

@Composable
private fun ListaNinosScreen(ninos: List<NinoEntity>, database: AppDatabase, onSeleccionar: (NinoEntity) -> Unit, onVolver: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var ninoAEliminar by remember { mutableStateOf<NinoEntity?>(null) }
    var ninoAEditar by remember { mutableStateOf<NinoEntity?>(null) }
    var nombreEditado by remember { mutableStateOf("") }
    var sexoEditado by remember { mutableStateOf("") }

    // Diálogo de eliminación
    if (ninoAEliminar != null) {
        AlertDialog(
            onDismissRequest = { ninoAEliminar = null },
            title = { Text("Eliminar registro", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar a ${ninoAEliminar!!.nombre} y todos sus registros?") },
            confirmButton = {
                TextButton(onClick = {
                    val nino = ninoAEliminar!!
                    ninoAEliminar = null
                    coroutineScope.launch(Dispatchers.IO) {
                        database.vacunaDao().deleteAllByNino(nino.id)
                        database.controlPediatricoDao().deleteAllByNino(nino.id)
                        database.enfermedadDao().deleteAllByNino(nino.id)
                        database.ninoDao().deleteNino(nino)
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Eliminar", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { ninoAEliminar = null }) { Text("Cancelar", color = Color(0xFF757575)) } },
            containerColor = Color.White
        )
    }

    // Diálogo de edición
    if (ninoAEditar != null) {
        AlertDialog(
            onDismissRequest = { ninoAEditar = null },
            title = { Text("Editar bebé", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nombreEditado, onValueChange = { nombreEditado = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Sexo:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Niño", "Niña", "No definido").forEach { opcion ->
                            FilterChip(
                                selected = sexoEditado == opcion,
                                onClick = { sexoEditado = opcion },
                                label = { Text(opcion, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (opcion) { "Niño" -> Color(0xFF1565C0); "Niña" -> Color(0xFFC2185B); else -> Color(0xFF616161) },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nino = ninoAEditar!!
                        ninoAEditar = null
                        coroutineScope.launch(Dispatchers.IO) {
                            database.ninoDao().updateNino(nino.copy(nombre = nombreEditado, sexo = sexoEditado))
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    enabled = nombreEditado.isNotBlank()
                ) {
                    Icon(Icons.Filled.Save, "Guardar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { ninoAEditar = null }) {
                    Icon(Icons.Filled.Close, "Cancelar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancelar")
                }
            },
            containerColor = Color.White
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Text("\uD83D\uDC76 Registro de Bebés", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (ninos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\uD83D\uDC76", fontSize = 64.sp)
                    Text("No hay bebés registrados", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Registra un parto en el módulo de\nembarazo para comenzar", fontSize = 14.sp, color = Color(0xFFCE93D8), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ninos, key = { it.id }) { nino ->
                    val bgColor = when (nino.sexo) { "Niño" -> Color(0xFF1A237E); "Niña" -> Color(0xFF4A0E4E); else -> Color(0xFF2A2450) }
                    val accentColor = when (nino.sexo) { "Niño" -> Color(0xFF64B5F6); "Niña" -> Color(0xFFF48FB1); else -> Color(0xFFCE93D8) }
                    val emojiNino = when (nino.sexo) { "Niño" -> "\uD83D\uDC66"; "Niña" -> "\uD83D\uDC67"; else -> "\uD83D\uDC76" }
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(nino) }, colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(16.dp)) {
                        Column {
                            if (nino.esPrueba) {
                                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0)).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("\uD83E\uDDEA", fontSize = 14.sp)
                                    Text("Prueba de funcionamiento", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                            }
                            Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(emojiNino, fontSize = 40.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(nino.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Nacimiento: ${nino.fechaNacimiento}", fontSize = 13.sp, color = accentColor)
                                    Text(nino.sexo, fontSize = 13.sp, color = Color(0xFFBDBDBD))
                                }
                                IconButton(onClick = { ninoAEditar = nino; nombreEditado = nino.nombre; sexoEditado = nino.sexo }) {
                                    Icon(Icons.Filled.Edit, "Editar", tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { ninoAEliminar = nino }) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardNinoScreen(
    nino: NinoEntity,
    database: AppDatabase,
    onVolver: () -> Unit,
    onIrAVacunas: () -> Unit,
    onIrAControles: () -> Unit,
    onIrAEnfermedades: () -> Unit
) {
    val vacunas by database.vacunaDao().getVacunasByNino(nino.id).collectAsState(initial = emptyList())
    val controles by database.controlPediatricoDao().getControlesByNino(nino.id).collectAsState(initial = emptyList())
    val enfermedades by database.enfermedadDao().getEnfermedadesByNino(nino.id).collectAsState(initial = emptyList())
    val vacunasAplicadas = vacunas.count { it.estaAplicada }
    val vacunasTotales = vacunas.size
    val ultimoControl = controles.firstOrNull()
    val alergias = enfermedades.count { it.esAlergia }
    val bgColor = when (nino.sexo) {
        "Niño" -> Color(0xFF0D1B3E)
        "Niña" -> Color(0xFF3E0D2E)
        else -> Color(0xFF120F26)
    }
    val accentColor = when (nino.sexo) {
        "Niño" -> Color(0xFF64B5F6)
        "Niña" -> Color(0xFFF48FB1)
        else -> Color(0xFFCE93D8)
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(nino.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Nacimiento: ${nino.fechaNacimiento}", fontSize = 13.sp, color = accentColor)
            }
        }
        if (nino.esPrueba) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0)).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\uD83E\uDDEA", fontSize = 16.sp)
                Text("Prueba de funcionamiento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2450)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protección", fontSize = 12.sp, color = Color(0xFFBDBDBD))
                            Text(
                                "$vacunasAplicadas / $vacunasTotales",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (vacunasAplicadas == vacunasTotales && vacunasTotales > 0) Color(0xFF4CAF50) else accentColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desarrollo", fontSize = 12.sp, color = Color(0xFFBDBDBD))
                            Text("${controles.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reacciones", fontSize = 12.sp, color = Color(0xFFBDBDBD))
                            Text("$alergias", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (alergias > 0) Color(0xFFFF9800) else accentColor)
                        }
                    }
                    if (ultimoControl != null) {
                        Divider(color = Color(0xFF3E3E6E), modifier = Modifier.padding(vertical = 4.dp))
                        Text("Último registro: ${ultimoControl.fechaControl}", fontSize = 12.sp, color = Color(0xFFBDBDBD))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ultimoControl.pesoKg?.let { Text("Peso: ${"%.1f".format(it)} kg", fontSize = 13.sp, color = Color.White) }
                            ultimoControl.tallaCm?.let { Text("Talla: ${"%.1f".format(it)} cm", fontSize = 13.sp, color = Color.White) }
                            ultimoControl.perimetroCefalicoCm?.let { Text("PC: ${"%.1f".format(it)} cm", fontSize = 13.sp, color = Color.White) }
                        }
                    }
                }
            }
            Text("Expediente Personal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PedModuloCard(modifier = Modifier.weight(1f), titulo = "Protección", subtitulo = "$vacunasAplicadas/$vacunasTotales", cardColor = Color(0xFF1B5E20), onClick = onIrAVacunas)
                PedModuloCard(modifier = Modifier.weight(1f), titulo = "Desarrollo", subtitulo = "${controles.size} registros", cardColor = Color(0xFF0D47A1), onClick = onIrAControles)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PedModuloCard(modifier = Modifier.weight(1f), titulo = "Condiciones\ny Reacciones", subtitulo = "${enfermedades.size} registros", cardColor = Color(0xFF4E342E), onClick = onIrAEnfermedades)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PedModuloCard(modifier: Modifier = Modifier, titulo: String, subtitulo: String, cardColor: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.aspectRatio(1.1f).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(subtitulo, color = Color(0xFFBDBDBD), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EsquemaVacunacionScreen(ninoId: Long, database: AppDatabase, onVolver: () -> Unit) {
    val vacunas by database.vacunaDao().getVacunasByNino(ninoId).collectAsState(initial = emptyList())
    val vacunasPorEdad = vacunas.groupBy { it.edadRecomendada }
    val coroutineScope = rememberCoroutineScope()
    val ordenEdades = listOf("Recién Nacido", "2 meses", "4 meses", "6 meses", "12 meses", "18 meses", "4-6 años", "11-12 años")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Cartilla de registros", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                val aplicadas = vacunas.count { it.estaAplicada }
                Text("$aplicadas de ${vacunas.size} registradas", fontSize = 13.sp, color = Color(0xFF4CAF50))
            }
        }
        if (vacunas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay registros en la cartilla", color = Color(0xFFCE93D8), fontSize = 16.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ordenEdades.forEach { edad ->
                    val listaVacunas = vacunasPorEdad[edad] ?: return@forEach
                    val todasAplicadas = listaVacunas.all { it.estaAplicada }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = edad, color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (todasAplicadas) Text("Completo", color = Color(0xFF4CAF50), fontSize = 12.sp)
                        }
                    }
                    items(listaVacunas, key = { it.id }) { vacuna ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (vacuna.estaAplicada) Color(0xFF1B3A1B) else Color(0xFF2A2450)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vacuna.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(vacuna.descripcion, color = Color(0xFFBDBDBD), fontSize = 11.sp, maxLines = 2)
                                    if (vacuna.estaAplicada && vacuna.fechaAplicacion != null) {
                                        Text("Registrada: ${vacuna.fechaAplicacion}", color = Color(0xFF4CAF50), fontSize = 12.sp)
                                    }
                                }
                                Checkbox(
                                    checked = vacuna.estaAplicada,
                                    onCheckedChange = { checked ->
                                        val fecha = if (checked) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) else null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            database.vacunaDao().updateVacuna(vacuna.copy(estaAplicada = checked, fechaAplicacion = fecha))
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = Color.Gray, checkmarkColor = Color.White)
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
private fun ControlesPediatricosScreen(nino: NinoEntity, database: AppDatabase, onVolver: () -> Unit) {
    val controles by database.controlPediatricoDao().getControlesByNino(nino.id).collectAsState(initial = emptyList())
    var mostrarFormularioCtrl by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (mostrarFormularioCtrl) {
        FormularioControlPed(ninoId = nino.id, onDismiss = { mostrarFormularioCtrl = false }, onGuardar = { control ->
            coroutineScope.launch(Dispatchers.IO) {
                database.controlPediatricoDao().insertControl(control)
                withContext(Dispatchers.Main) { mostrarFormularioCtrl = false; Toast.makeText(context, "Registro guardado", Toast.LENGTH_SHORT).show() }
            }
        })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Text("Registro de Desarrollo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { mostrarFormularioCtrl = true }) { Icon(Icons.Filled.Add, "Agregar", tint = Color(0xFF4CAF50)) }
        }
        if (controles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sin registros de desarrollo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { mostrarFormularioCtrl = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                        Text("Registrar primer registro")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(controles, key = { it.id }) { control ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2450)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(control.fechaControl, color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                control.pesoKg?.let { Column { Text("Peso", fontSize = 11.sp, color = Color(0xFFBDBDBD)); Text("${"%.1f".format(it)} kg", color = Color.White, fontWeight = FontWeight.Bold) } }
                                control.tallaCm?.let { Column { Text("Talla", fontSize = 11.sp, color = Color(0xFFBDBDBD)); Text("${"%.1f".format(it)} cm", color = Color.White, fontWeight = FontWeight.Bold) } }
                                control.perimetroCefalicoCm?.let { Column { Text("Circ. Cefálica", fontSize = 11.sp, color = Color(0xFFBDBDBD)); Text("${"%.1f".format(it)} cm", color = Color.White, fontWeight = FontWeight.Bold) } }
                            }
                            if (!control.observaciones.isNullOrBlank()) {
                                Text("Obs: ${control.observaciones}", fontSize = 13.sp, color = Color(0xFFCE93D8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioControlPed(ninoId: Long, onDismiss: () -> Unit, onGuardar: (ControlPediatricoEntity) -> Unit) {
    var fecha by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var peso by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var pc by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    val tfColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9C27B0), unfocusedBorderColor = Color(0xFF6A1B9A), focusedTextColor = Color.White, unfocusedTextColor = Color(0xFFE1BEE7), focusedLabelColor = Color(0xFFCE93D8), unfocusedLabelColor = Color(0xFF9E9E9E))

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Text("Nuevo Registro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (dd/MM/yyyy)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
            OutlinedTextField(value = peso, onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = talla, onValueChange = { talla = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Talla (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = pc, onValueChange = { pc = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Circunferencia cefálica (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Observaciones (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = tfColors)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = Color(0xFF9E9E9E)) }
                Button(
                    onClick = {
                        onGuardar(ControlPediatricoEntity(ninoId = ninoId, fechaControl = fecha, pesoKg = peso.toDoubleOrNull(), tallaCm = talla.toDoubleOrNull(), perimetroCefalicoCm = pc.toDoubleOrNull(), observaciones = observaciones.ifBlank { null }))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    enabled = fecha.isNotBlank()
                ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
internal fun VerificadorTomasPasadasScreen(
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estados
    var medicamentos by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var tomasPendientes by remember { mutableStateOf<List<TomaPendiente>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedToma by remember { mutableStateOf<TomaPendiente?>(null) }
    var mostrarDialogoOpciones by remember { mutableStateOf(false) }
    var horaPersonalizada by remember { mutableStateOf("") }
    
    // Cargar datos
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val meds = database.medicationDao().obtenerActivosConAlarma()
                medicamentos = meds
                val medById = meds.associateBy { it.id }

                // Fuente de verdad: intakes ya registrados como NOT_TAKEN en los últimos 30 días
                val now = System.currentTimeMillis()
                val lookbackStart = now - (30L * 24L * 60L * 60L * 1000L)
                val intakesRegistrados = database.medicationIntakeDao().obtenerEnRango(lookbackStart, now)

                val pendientes = intakesRegistrados
                    .filter { it.status == MEDICATION_INTAKE_STATUS_NOT_TAKEN }
                    .mapNotNull { intake ->
                        val med = medById[intake.medicationId] ?: return@mapNotNull null
                        TomaPendiente(medication = med, scheduledAt = intake.scheduledAt)
                    }

                tomasPendientes = pendientes.sortedByDescending { it.scheduledAt }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Diálogo de opciones
    if (mostrarDialogoOpciones && selectedToma != null) {
        AlertDialog(
            onDismissRequest = { 
                mostrarDialogoOpciones = false
                selectedToma = null
                horaPersonalizada = ""
            },
            title = { Text("Registrar toma") },
            text = {
                Column {
                    Text("¿Qué deseas hacer con esta toma?")
                    Text(
                        "${selectedToma!!.medication.nombre}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(selectedToma!!.scheduledAt))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = horaPersonalizada,
                        onValueChange = { horaPersonalizada = it },
                        label = { Text("Hora personalizada (HH:mm)") },
                        placeholder = { Text("Ej: 14:30") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            registrarToma(context, database, selectedToma!!, "tomado", horaPersonalizada)
                            mostrarDialogoOpciones = false
                            selectedToma = null
                            horaPersonalizada = ""
                            
                            // Recargar lista
                            coroutineScope.launch(Dispatchers.IO) {
                                val now = System.currentTimeMillis()
                                val lookbackStart = now - (30L * 24L * 60L * 60L * 1000L)
                                val medById = medicamentos.associateBy { it.id }
                                val intakesRegistrados = database.medicationIntakeDao().obtenerEnRango(lookbackStart, now)
                                tomasPendientes = intakesRegistrados
                                    .filter { it.status == MEDICATION_INTAKE_STATUS_NOT_TAKEN }
                                    .mapNotNull { intake ->
                                        val med = medById[intake.medicationId] ?: return@mapNotNull null
                                        TomaPendiente(medication = med, scheduledAt = intake.scheduledAt)
                                    }
                                    .sortedByDescending { it.scheduledAt }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tomado")
                    }
                    
                    TextButton(
                        onClick = {
                            registrarToma(context, database, selectedToma!!, "no_tomado", horaPersonalizada)
                            mostrarDialogoOpciones = false
                            selectedToma = null
                            horaPersonalizada = ""

                            // Recargar lista
                            coroutineScope.launch(Dispatchers.IO) {
                                val now = System.currentTimeMillis()
                                val lookbackStart = now - (30L * 24L * 60L * 60L * 1000L)
                                val medById = medicamentos.associateBy { it.id }
                                val intakesRegistrados = database.medicationIntakeDao().obtenerEnRango(lookbackStart, now)
                                tomasPendientes = intakesRegistrados
                                    .filter { it.status == MEDICATION_INTAKE_STATUS_NOT_TAKEN }
                                    .mapNotNull { intake ->
                                        val med = medById[intake.medicationId] ?: return@mapNotNull null
                                        TomaPendiente(medication = med, scheduledAt = intake.scheduledAt)
                                    }
                                    .sortedByDescending { it.scheduledAt }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No tomado")
                    }
                    
                    TextButton(
                        onClick = {
                            mostrarDialogoOpciones = false
                            selectedToma = null
                            horaPersonalizada = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Omitir")
                    }
                }
            }
        )
    }
    
    // UI principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Text(
                text = "Verificador de tomas pasadas",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (tomasPendientes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay tomas pendientes\nen los últimos 30 días",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tomasPendientes.size) { index ->
                    val toma = tomasPendientes[index]
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedToma = toma
                                mostrarDialogoOpciones = true
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color del medicamento
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        toma.medication.colorMedicamento.takeIf { it.isNotBlank() }
                                            ?.let { Color(android.graphics.Color.parseColor(it)) }
                                            ?: Color.Gray
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = toma.medication.nombre,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        .format(Date(toma.scheduledAt)),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Dosis: ${toma.medication.unidadesPorToma()}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data class para representar una toma pendiente
private data class TomaPendiente(
    val medication: Medication,
    val scheduledAt: Long
)


// Función para registrar una toma
private fun registrarToma(
    context: Context,
    database: AppDatabase,
    toma: TomaPendiente,
    accion: String,
    horaPersonalizada: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val acceptedAt = if (horaPersonalizada.isNotBlank()) {
                val parts = horaPersonalizada.split(":")
                if (parts.size >= 2) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = toma.scheduledAt
                        set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 12)
                        set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    calendar.timeInMillis
                } else {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
            
            val status = when (accion) {
                "tomado" -> MEDICATION_INTAKE_STATUS_TAKEN
                "no_tomado" -> MEDICATION_INTAKE_STATUS_NOT_TAKEN
                else -> return@launch
            }
            
            val unitsPerTake = toma.medication.unidadesPorToma()
            
            database.medicationIntakeDao().guardar(
                MedicationIntake(
                    medicationId = toma.medication.id,
                    patientId = toma.medication.patientId,
                    scheduledAt = toma.scheduledAt,
                    acceptedAt = acceptedAt,
                    medicationName = toma.medication.nombre,
                    dosis = unitsPerTake.toString(),
                    status = status
                )
            )
            
            // Si fue tomado, actualizar stock
            if (accion == "tomado") {
                val currentStock = toma.medication.stockActual ?: return@launch
                val unitsPerTake = toma.medication.unidadesPorToma()
                val updatedStock = currentStock - unitsPerTake
                database.medicationDao().actualizarStock(toma.medication.id, updatedStock)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (accion == "tomado") "Toma registrada como tomada" else "Toma registrada como no tomada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelSignosVitalesContent(
    sistolicaInput: String, onSistolicaChange: (String) -> Unit,
    diastolicaInput: String, onDiastolicaChange: (String) -> Unit,
    comentarioPresionInput: String, onComentarioPresionChange: (String) -> Unit,
    latidosInput: String, onLatidosChange: (String) -> Unit,
    comentarioLatidosInput: String, onComentarioLatidosChange: (String) -> Unit,
    glucemiaInput: String, onGlucemiaChange: (String) -> Unit,
    comentarioGlucemiaInput: String, onComentarioGlucemiaChange: (String) -> Unit,
    temperaturaInput: String, onTemperaturaChange: (String) -> Unit,
    comentarioTemperaturaInput: String, onComentarioTemperaturaChange: (String) -> Unit,
    pesoInput: String, onPesoChange: (String) -> Unit,
    pesoUnidadKg: Boolean, onPesoUnidadToggle: () -> Unit,
    estaturaPaciente: String,
    estaturaUnidadPaciente: String,
    pacienteActivo: PatientProfile?,
    database: AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    recordatorioSignosActivo: Boolean,
    recordatorioSignosHora: Int,
    recordatorioSignosMinuto: Int,
    mostrarTimePickerSignos: Boolean,
    onRecordatorioActivoChange: (Boolean) -> Unit,
    onRecordatorioHoraChange: (Int) -> Unit,
    onRecordatorioMinutoChange: (Int) -> Unit,
    onMostrarTimePickerChange: (Boolean) -> Unit,
    onPesoPacienteChange: (String) -> Unit,
    onPesoUnidadPacienteChange: (String) -> Unit,
    onSistolicaInputClear: () -> Unit,
    onDiastolicaInputClear: () -> Unit,
    onComentarioPresionClear: () -> Unit,
    onLatidosInputClear: () -> Unit,
    onComentarioLatidosClear: () -> Unit,
    onGlucemiaInputClear: () -> Unit,
    onComentarioGlucemiaClear: () -> Unit,
    onTemperaturaInputClear: () -> Unit,
    onComentarioTemperaturaClear: () -> Unit,
    onPesoInputClear: () -> Unit,
    onVerListado: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    MetallicRedVitalSignsCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = 10) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Seguimiento diario", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Registra valores diarios, ritmo, niveles de azúcar y temperatura.")

            OutlinedTextField(
                value = sistolicaInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}$"))) onSistolicaChange(it) },
                label = { Text("Sistólica") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = diastolicaInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}$"))) onDiastolicaChange(it) },
                label = { Text("Diastólica") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = comentarioPresionInput, onValueChange = onComentarioPresionChange,
                label = { Text("Comentarios de presión arterial") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = latidosInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}$"))) onLatidosChange(it) },
                label = { Text("Latidos") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = comentarioLatidosInput, onValueChange = onComentarioLatidosChange,
                label = { Text("Comentarios de latidos") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = glucemiaInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}$"))) onGlucemiaChange(it) },
                label = { Text("Glucemia") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = comentarioGlucemiaInput, onValueChange = onComentarioGlucemiaChange,
                label = { Text("Comentarios de glucemia") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = temperaturaInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,2}([.,]\\d?)?$"))) onTemperaturaChange(it) },
                label = { Text("Temperatura") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = comentarioTemperaturaInput, onValueChange = onComentarioTemperaturaChange,
                label = { Text("Comentarios de temperatura") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pesoInput,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}([.,]\\d?)?$"))) onPesoChange(it) },
                    label = { Text("Peso (${if (pesoUnidadKg) "kg" else "lbs"})") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                VitalSignsMetallicButton(
                    text = if (pesoUnidadKg) "kg" else "lbs",
                    onClick = onPesoUnidadToggle,
                    modifier = Modifier
                )
            }

            val imcVital = if (pesoInput.isNotBlank() && estaturaPaciente.isNotBlank()) {
                calcularIMC(
                    pesoStr = pesoInput,
                    pesoUnidad = if (pesoUnidadKg) "kg" else "lbs",
                    estaturaStr = estaturaPaciente,
                    estaturaUnidad = estaturaUnidadPaciente
                )
            } else null
            if (imcVital != null) {
                Text(
                    "IMC calculado: ${"%.1f".format(imcVital)} — ${etiquetaIMC(imcVital)}",
                    color = Color.White, fontWeight = FontWeight.Medium
                )
            }

            VitalSignsMetallicButton(
                text = "Ver listado de registros guardados",
                onClick = onVerListado,
                modifier = Modifier.fillMaxWidth()
            )

            VitalSignsMetallicButton(
                text = "Guardar registro",
                onClick = {
                    val sistolica = sistolicaInput.toIntOrNull()
                    val diastolica = diastolicaInput.toIntOrNull()
                    val latidos = latidosInput.toIntOrNull()
                    val glucemia = glucemiaInput.toIntOrNull()
                    val temperatura = temperaturaInput.replace(',', '.').toDoubleOrNull()
                    val peso = pesoInput.replace(',', '.').toDoubleOrNull()
                    val hayPresion = sistolica != null && diastolica != null
                    val hayAlgoDato = hayPresion || latidos != null || glucemia != null || temperatura != null || peso != null
                    if (!hayAlgoDato) {
                        Toast.makeText(context, "Rellena al menos un campo antes de guardar", Toast.LENGTH_SHORT).show()
                        return@VitalSignsMetallicButton
                    }
                    if ((sistolica != null) != (diastolica != null)) {
                        Toast.makeText(context, "Completa sistolica y diastolica juntas", Toast.LENGTH_SHORT).show()
                        return@VitalSignsMetallicButton
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        val pesoUnidadActual = if (pesoUnidadKg) "kg" else "lbs"
                        val imcCalculado = if (peso != null) calcularIMC(
                            pesoStr = pesoInput, pesoUnidad = pesoUnidadActual,
                            estaturaStr = estaturaPaciente, estaturaUnidad = estaturaUnidadPaciente
                        ) else null
                        database.signosVitalesDao().insertar(
                            SignosVitales(
                                patientId = pacienteActivo?.id ?: 0,
                                sistolica = sistolica, diastolica = diastolica,
                                comentarioPresion = comentarioPresionInput,
                                latidos = latidos, comentarioLatidos = comentarioLatidosInput,
                                glucemia = glucemia, comentarioGlucemia = comentarioGlucemiaInput,
                                temperatura = temperatura, comentarioTemperatura = comentarioTemperaturaInput,
                                peso = peso, pesoUnidad = pesoUnidadActual, imc = imcCalculado
                            )
                        )
                        if (peso != null) {
                            pacienteActivo?.let { paciente ->
                                val pesoStr = pesoInput.trim().replace(',', '.')
                                database.patientProfileDao().actualizar(
                                    paciente.copy(peso = pesoStr, pesoUnidad = pesoUnidadActual)
                                )
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (peso != null) {
                                onPesoPacienteChange(pesoInput.trim().replace(',', '.'))
                                onPesoUnidadPacienteChange(if (pesoUnidadKg) "kg" else "lbs")
                            }
                            onSistolicaInputClear(); onDiastolicaInputClear(); onComentarioPresionClear()
                            onLatidosInputClear(); onComentarioLatidosClear(); onGlucemiaInputClear()
                            onComentarioGlucemiaClear(); onTemperaturaInputClear(); onComentarioTemperaturaClear()
                            onPesoInputClear()
                            Toast.makeText(context, "Métricas guardadas", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (mostrarTimePickerSignos) {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        onRecordatorioHoraChange(hourOfDay)
                        onRecordatorioMinutoChange(minute)
                        onMostrarTimePickerChange(false)
                        SignosVitalesScheduler.saveSettings(context, hourOfDay, minute, recordatorioSignosActivo)
                        val patientId = pacienteActivo?.id ?: 0
                        val patientName = "${pacienteActivo?.nombre.orEmpty()} ${pacienteActivo?.apellidos.orEmpty()}".trim().ifBlank { "Paciente" }
                        if (patientId > 0) {
                            SignosVitalesScheduler.savePatientInfo(context, patientId, patientName)
                            SignosVitalesScheduler(context).programar(patientId, patientName)
                        }
                    },
                    recordatorioSignosHora, recordatorioSignosMinuto, true
                ).show()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VitalSignsMetallicButton(
                    text = "Volver al escritorio", onClick = onVolver, modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val nuevoActivo = !recordatorioSignosActivo
                        onRecordatorioActivoChange(nuevoActivo)
                        SignosVitalesScheduler.saveSettings(context, recordatorioSignosHora, recordatorioSignosMinuto, nuevoActivo)
                        val patientId = pacienteActivo?.id ?: 0
                        val patientName = "${pacienteActivo?.nombre.orEmpty()} ${pacienteActivo?.apellidos.orEmpty()}".trim().ifBlank { "Paciente" }
                        if (nuevoActivo && patientId > 0) {
                            SignosVitalesScheduler.savePatientInfo(context, patientId, patientName)
                            SignosVitalesScheduler(context).programar(patientId, patientName)
                            onMostrarTimePickerChange(true)
                        } else {
                            SignosVitalesScheduler(context).cancelar(patientId)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Recordatorio diario",
                        tint = if (recordatorioSignosActivo) Color(0xFF7B1FA2) else Color.White
                    )
                }
            }
        }
    }
}
