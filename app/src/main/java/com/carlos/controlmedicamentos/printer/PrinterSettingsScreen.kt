package com.carlos.controlmedicamentos.printer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { PrinterManager(context) }

    var settings by remember { mutableStateOf(printerManager.getSavedSettings()) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showLetterDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            pairedDevices = printerManager.getPairedBluetoothDevices()
        } else {
            Toast.makeText(context, "Se necesitan permisos de Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    LaunchedEffect(Unit) {
        if (printerManager.hasBluetoothPermissions()) {
            pairedDevices = printerManager.getPairedBluetoothDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuracion de Impresoras", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A1929)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0A1929), Color(0xFF1B3A4B), Color(0xFF0A1929)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Impresora de tickets
                PrinterCard(
                    title = "Impresora de Tickets (termica)",
                    icon = Icons.Default.Bluetooth,
                    config = settings.ticketPrinter,
                    onConfigure = {
                        if (!printerManager.hasBluetoothPermissions()) {
                            requestBluetoothPermissions()
                        } else {
                            pairedDevices = printerManager.getPairedBluetoothDevices()
                            showTicketDialog = true
                        }
                    },
                    onClear = {
                        PrinterSettingsStorage.clearTicket(context)
                        settings = printerManager.getSavedSettings()
                        Toast.makeText(context, "Impresora de tickets eliminada", Toast.LENGTH_SHORT).show()
                    }
                )

                // Impresora de carta
                PrinterCard(
                    title = "Impresora de Carta",
                    icon = Icons.Default.Print,
                    config = settings.letterPrinter,
                    onConfigure = {
                        if (!printerManager.hasBluetoothPermissions()) {
                            requestBluetoothPermissions()
                        } else {
                            pairedDevices = printerManager.getPairedBluetoothDevices()
                            showLetterDialog = true
                        }
                    },
                    onClear = {
                        PrinterSettingsStorage.clearLetter(context)
                        settings = printerManager.getSavedSettings()
                        Toast.makeText(context, "Impresora de carta eliminada", Toast.LENGTH_SHORT).show()
                    }
                )

                // Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF112233)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Tipos soportados",
                            color = Color(0xFF80D8FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "- Tickets: impresora termica Bluetooth (ESC/POS), ancho 58mm o 80mm.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "- Carta: impresora Bluetooth o red; se genera PDF/HTML y se envia por el sistema de impresion de Android.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showTicketDialog) {
        PrinterConfigDialog(
            title = "Configurar impresora de tickets",
            existing = settings.ticketPrinter,
            devices = pairedDevices,
            onDismiss = { showTicketDialog = false },
            onSave = { config ->
                PrinterSettingsStorage.saveTicket(context, config)
                settings = printerManager.getSavedSettings()
                showTicketDialog = false
                Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLetterDialog) {
        PrinterConfigDialog(
            title = "Configurar impresora de carta",
            existing = settings.letterPrinter,
            devices = pairedDevices,
            showTypeSelector = true,
            onDismiss = { showLetterDialog = false },
            onSave = { config ->
                PrinterSettingsStorage.saveLetter(context, config)
                settings = printerManager.getSavedSettings()
                showLetterDialog = false
                Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PrinterCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    config: PrinterConfig?,
    onConfigure: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112233)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                if (config != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF6B6B))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (config != null) {
                Text("Nombre: ${config.name}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text("Direccion: ${config.address}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            } else {
                Text("No configurada", color = Color.Gray, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfigure,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (config != null) "Cambiar" else "Configurar")
            }
        }
    }
}

@Composable
private fun PrinterConfigDialog(
    title: String,
    existing: PrinterConfig?,
    devices: List<BluetoothDevice>,
    showTypeSelector: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (PrinterConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var selectedType by remember { mutableStateOf(existing?.type ?: PrinterType.TICKET_BLUETOOTH) }
    var width by remember { mutableStateOf(existing?.paperWidth?.toString() ?: "384") }
    var chars by remember { mutableStateOf(existing?.charactersPerLine?.toString() ?: "32") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White) },
        containerColor = Color(0xFF1B3A4B),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF80D8FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (showTypeSelector) {
                    Text("Tipo de conexion", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == PrinterType.LETTER_BLUETOOTH,
                            onClick = { selectedType = PrinterType.LETTER_BLUETOOTH },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF80D8FF))
                        )
                        Text("Bluetooth", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = selectedType == PrinterType.LETTER_WIFI,
                            onClick = { selectedType = PrinterType.LETTER_WIFI },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF80D8FF))
                        )
                        Text("WiFi / Red", color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = {
                        Text(
                            if (selectedType == PrinterType.LETTER_WIFI) "IP:Puerto" else "MAC (direccion Bluetooth)",
                            color = Color.LightGray
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF80D8FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!showTypeSelector || selectedType != PrinterType.LETTER_WIFI) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Dispositivos Bluetooth emparejados:", color = Color(0xFF80D8FF), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    devices.forEach { device ->
                        val devName = try { device.name } catch (_: SecurityException) { "Dispositivo" } ?: "Dispositivo"
                        val devAddress = device.address ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    address = devAddress
                                    if (name.isBlank()) name = devName
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = Color(0xFF80D8FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(devName, color = Color.White, fontSize = 13.sp)
                                Text(devAddress, color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (address == devAddress) {
                                Text("Seleccionado", color = Color(0xFF80D8FF), fontSize = 11.sp)
                            }
                        }
                    }
                    if (devices.isEmpty()) {
                        Text("No hay dispositivos emparejados.", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                if (!showTypeSelector) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it.filter { c -> c.isDigit() } },
                        label = { Text("Ancho papel (puntos)", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF80D8FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chars,
                        onValueChange = { chars = it.filter { c -> c.isDigit() } },
                        label = { Text("Caracteres por linea", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF80D8FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cfg = PrinterConfig(
                        id = existing?.id ?: "default",
                        name = name.ifBlank { "Impresora" },
                        type = selectedType,
                        address = address.trim(),
                        paperWidth = width.toIntOrNull() ?: 384,
                        charactersPerLine = chars.toIntOrNull() ?: 32,
                        enabled = true
                    )
                    onSave(cfg)
                }
            ) {
                Text("Guardar", color = Color(0xFF80D8FF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.LightGray)
            }
        }
    )
}
