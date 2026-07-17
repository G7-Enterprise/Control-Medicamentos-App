package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.EnfermedadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EnfermedadesAlergiasScreen(ninoId: Long, database: AppDatabase, onVolver: () -> Unit) {
    val enfermedades by database.enfermedadDao().getEnfermedadesByNino(ninoId).collectAsState(initial = emptyList())
    var mostrarFormEnf by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (mostrarFormEnf) {
        FormularioEnfAlergia(ninoId = ninoId, onDismiss = { mostrarFormEnf = false }, onGuardar = { ent ->
            coroutineScope.launch(Dispatchers.IO) {
                database.enfermedadDao().insertEnfermedad(ent)
                withContext(Dispatchers.Main) {
                    mostrarFormEnf = false
                    Toast.makeText(context, "Registro guardado", Toast.LENGTH_SHORT).show()
                }
            }
        })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Text("Enfermedades y Alergias", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { mostrarFormEnf = true }) { Icon(Icons.Filled.Add, "Agregar", tint = Color(0xFFFF9800)) }
        }
        if (enfermedades.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sin registros personales", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { mostrarFormEnf = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                        Text("Agregar registro")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(enfermedades, key = { it.id }) { registro ->
                    val colorBorde = if (registro.esAlergia) Color(0xFFFF9800) else Color(0xFF03A9F4)
                    val tipoTexto = if (registro.esAlergia) "ALERGIA" else "CONDICIÓN"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2450)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(registro.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(color = colorBorde.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text(tipoTexto, color = colorBorde, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            val textoFechas = if (registro.fechaFin != null) "Del ${registro.fechaInicio} al ${registro.fechaFin}" else "Inicio: ${registro.fechaInicio} (Activa)"
                            Text(textoFechas, color = Color(0xFFBDBDBD), fontSize = 13.sp)
                            if (!registro.sintomas.isNullOrBlank()) Text("Notas: ${registro.sintomas}", color = Color(0xFFBDBDBD), fontSize = 13.sp)
                            if (!registro.planPersonal.isNullOrBlank()) Text("Plan personal: ${registro.planPersonal}", color = Color(0xFF81D4FA), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormularioEnfAlergia(ninoId: Long, onDismiss: () -> Unit, onGuardar: (EnfermedadEntity) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var fechaFin by remember { mutableStateOf("") }
    var sintomas by remember { mutableStateOf("") }
    var planPersonal by remember { mutableStateOf("") }
    var esAlergia by remember { mutableStateOf(false) }
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF9C27B0),
        unfocusedBorderColor = Color(0xFF6A1B9A),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE1BEE7),
        focusedLabelColor = Color(0xFFCE93D8),
        unfocusedLabelColor = Color(0xFF9E9E9E)
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF120F26))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1A3C)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = Color.White) }
            Text("Nuevo Registro Personal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2450)), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tipo de registro", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (esAlergia) "Alergia permanente" else "Condición temporal", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                    }
                    Switch(
                        checked = esAlergia,
                        onCheckedChange = { esAlergia = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.5f))
                    )
                }
            }
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fechaInicio, onValueChange = { fechaInicio = it }, label = { Text("Fecha Inicio") }, modifier = Modifier.weight(1f), singleLine = true, colors = tfColors)
                if (!esAlergia) {
                    OutlinedTextField(value = fechaFin, onValueChange = { fechaFin = it }, label = { Text("Fecha Fin") }, modifier = Modifier.weight(1f), singleLine = true, colors = tfColors)
                }
            }
            OutlinedTextField(value = sintomas, onValueChange = { sintomas = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = tfColors)
            OutlinedTextField(value = planPersonal, onValueChange = { planPersonal = it }, label = { Text("Plan personal") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = tfColors)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = Color(0xFF9E9E9E)) }
                Button(
                    onClick = {
                        onGuardar(EnfermedadEntity(
                            ninoId = ninoId,
                            nombre = nombre,
                            fechaInicio = fechaInicio,
                            fechaFin = if (fechaFin.isBlank()) null else fechaFin,
                            sintomas = sintomas.ifBlank { null },
                            planPersonal = planPersonal.ifBlank { null },
                            esAlergia = esAlergia
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    enabled = nombre.isNotBlank() && fechaInicio.isNotBlank()
                ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
