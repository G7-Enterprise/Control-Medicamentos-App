package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.PatientProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteClinicoScreen(
    pacienteActivo: PatientProfile?,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mesesAtras by remember { mutableStateOf(3) }
    var incluirAlertas by remember { mutableStateOf(true) }
    var incluirAnticonceptivos by remember { mutableStateOf(true) }
    var incluirSignosVitales by remember { mutableStateOf(true) }
    var incluirMedicamentos by remember { mutableStateOf(true) }
    var incluirActividad by remember { mutableStateOf(true) }
    var generando by remember { mutableStateOf(false) }
    var previewPayload by remember { mutableStateOf<ReporteClinicoPayload?>(null) }
    val esMujer = pacienteActivo?.sexo?.equals("Femenino", ignoreCase = true) == true

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        if (uri == null || previewPayload == null) { generando = false; return@rememberLauncherForActivityResult }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    escribirReporteClinicoDocx(out, previewPayload!!)
                }
                withContext(Dispatchers.Main) {
                    generando = false
                    Toast.makeText(context, "Reporte clínico exportado correctamente", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    generando = false
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📄 Reporte clínico", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A0038)
                )
            )
        },
        containerColor = Color(0xFF0F0A25)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Usuario ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F3C)),
                border = BorderStroke(1.dp, Color(0xFF7B1FA2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF4A148C), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatar = if (esMujer) "👩" else "👨"
                        Text(avatar, fontSize = 26.sp)
                    }
                    Column {
                        Text(
                            pacienteActivo?.let { "${it.nombre} ${it.apellidos}".trim() }.orEmpty().ifBlank { "Sin perfil seleccionado" },
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        pacienteActivo?.let {
                            if (it.fechaNacimiento > 0L) {
                                val years = ((System.currentTimeMillis() - it.fechaNacimiento) / (365.25 * 24 * 60 * 60 * 1000)).toInt()
                                Text("Edad: $years años", color = Color(0xFFB8A9D9), fontSize = 13.sp)
                            } else if (it.edad.isNotBlank()) {
                                Text("Edad: ${it.edad}", color = Color(0xFFB8A9D9), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── Configuración ─────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F3C))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Configurar reporte", color = Color(0xFFCE93D8), fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // Rango temporal
                    Text("Período a exportar", color = Color(0xFFB8A9D9), fontSize = 13.sp)
                    val opciones = listOf(1 to "1 mes", 3 to "3 meses", 6 to "6 meses", 12 to "12 meses", 0 to "Todo")
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        opciones.forEach { (v, label) ->
                            val sel = mesesAtras == v
                            FilterChip(
                                selected = sel,
                                onClick = { mesesAtras = v },
                                label = { Text(label, fontSize = 13.sp, color = if (sel) Color.White else Color(0xFFB8A9D9)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7B1FA2),
                                    containerColor = Color(0xFF2D1B69)
                                )
                            )
                        }
                    }

                    // Switches — secciones comunes
                    @Composable
                    fun SwitchFila(label: String, sub: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = Color.White, fontSize = 14.sp)
                                Text(sub, color = Color(0xFFB8A9D9), fontSize = 11.sp)
                            }
                            Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF7B1FA2)))
                        }
                    }
                    SwitchFila("Signos vitales", "Presión, frecuencia cardíaca, glucemia, temperatura, peso", incluirSignosVitales) { incluirSignosVitales = it }
                    SwitchFila("Medicamentos", "Listado activo/inactivo con tomas", incluirMedicamentos) { incluirMedicamentos = it }
                    SwitchFila("Actividad física", "Sesiones de ejercicio, pasos, distancia y calorías", incluirActividad) { incluirActividad = it }
                    SwitchFila("Alertas importantes", "Presion alta, fiebre, glucemia elevada", incluirAlertas) { incluirAlertas = it }
                    if (esMujer) {
                        SwitchFila("Anticonceptivos", "Último método y fecha de suspensión", incluirAnticonceptivos) { incluirAnticonceptivos = it }
                    }
                }
            }

            // ── Secciones que se incluirán ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E)),
                border = BorderStroke(1.dp, Color(0xFF3D2B6D))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contenido del reporte", color = Color(0xFFCE93D8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val items = buildList {
                        add("✅  Datos del paciente")
                        add("✅  Resumen ejecutivo")
                        if (incluirSignosVitales) add("✅  Signos vitales") else add("☐  Signos vitales (desactivado)")
                        if (incluirMedicamentos) add("✅  Medicamentos y tomas") else add("☐  Medicamentos (desactivado)")
                        if (incluirActividad) add("✅  Actividad física") else add("☐  Actividad física (desactivado)")
                        if (incluirAlertas) add("✅  Alertas importantes") else add("☐  Alertas importantes (desactivado)")
                        if (esMujer) {
                            add("✅  Ciclos menstruales")
                            add("✅  Embarazo / seguimiento")
                            if (incluirAnticonceptivos) add("✅  Anticonceptivos") else add("☐  Anticonceptivos (desactivado)")
                        }
                    }
                    items.forEach { s ->
                        Text(s, color = if (s.startsWith("✅")) Color(0xFF81C784) else Color(0xFF9E9E9E), fontSize = 12.sp)
                    }
                }
            }

            // ── Botón generar ─────────────────────────────────────
            Button(
                onClick = {
                    if (pacienteActivo == null) {
                        Toast.makeText(context, "Selecciona un perfil primero", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    generando = true
                    coroutineScope.launch {
                        val payload = compilarReporteClinico(
                            database = database,
                            paciente = pacienteActivo,
                            mesesAtras = mesesAtras,
                            incluirAlertas = incluirAlertas,
                            incluirAnticonceptivos = incluirAnticonceptivos,
                            incluirSignosVitales = incluirSignosVitales,
                            incluirMedicamentos = incluirMedicamentos,
                            incluirActividad = incluirActividad
                        )
                        previewPayload = payload
                        val nombre = "${pacienteActivo.nombre}_resumen_registros_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.docx"
                        exportLauncher.launch(nombre)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                enabled = !generando
            ) {
                if (generando) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Generando...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar reporte clínico (.docx)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "El reporte se genera en formato Word (.docx) compatible con Google Docs, LibreOffice y Microsoft Word.",
                color = Color(0xFF9E9E9E), fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

