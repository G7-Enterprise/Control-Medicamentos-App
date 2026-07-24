package com.carlos.controlmedicamentos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.media.RingtoneManager
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons as MaterialIcons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.FallAlert
import com.carlos.controlmedicamentos.data.local.FALL_STATUS_CONFIRMED
import com.carlos.controlmedicamentos.fall.FallDetectionService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmergencyContact(
    @com.google.gson.annotations.SerializedName("phone") val phone: String,
    @com.google.gson.annotations.SerializedName("name") val name: String = "",
    @com.google.gson.annotations.SerializedName("photoUri") val photoUri: String? = null
)

private const val PREFS_FALL_ALERT = "fall_alert_prefs"
private const val KEY_ENABLED = "fall_alert_enabled"
private const val KEY_CONTACTS_PREFIX = "emergency_contacts_"
private const val KEY_SENSITIVITY = "fall_sensitivity"
private const val KEY_ALARM_SOUND_URI = "alarm_sound_uri"
private const val KEY_SMS_MESSAGE = "sms_message"

private fun contactsKey(patientId: Int) = "$KEY_CONTACTS_PREFIX$patientId"

/**
 * Pantalla de configuración y historial del módulo de alerta de caídas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallAlertScreen(
    patientId: Int,
    database: AppDatabase,
    onVolver: () -> Unit
) {
    android.util.Log.d("FallAlertScreen", "FallAlertScreen iniciado patientId=$patientId")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_FALL_ALERT, Context.MODE_PRIVATE) }

    val paciente by database.patientProfileDao().observeById(patientId).collectAsState(initial = null)
    val edadPaciente = remember(paciente) {
        paciente?.edad?.trim()?.toIntOrNull() ?: 55
    }
    val alturaCmPaciente = remember(paciente) {
        val e = paciente?.estatura?.trim()?.toFloatOrNull() ?: 170f
        val unidad = paciente?.estaturaUnidad ?: "cm"
        if (unidad.lowercase() == "m") (e * 100).toInt() else e.toInt()
    }

    var isEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_ENABLED, false)) }
    var contactReloadTrigger by remember { mutableStateOf(0) }
    var contactPhones by remember(patientId, contactReloadTrigger) { 
        mutableStateOf(loadEmergencyContacts(prefs, patientId))
    }
    var newContactPhone by remember { mutableStateOf("") }
    var sensitivity by remember { mutableStateOf(prefs.getFloat(KEY_SENSITIVITY, 0.5f)) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }
    var pendingSensitivity by remember { mutableStateOf(sensitivity) }
    var showSoundConfigDialog by remember { mutableStateOf(false) }
    var selectedAlarmSound by remember { mutableStateOf(prefs.getString(KEY_ALARM_SOUND_URI, null)) }
    var customSmsMessage by remember { 
        mutableStateOf(
            prefs.getString(KEY_SMS_MESSAGE, "ALERTA DE CAÍDA\n\nSe ha detectado una posible caída. Por favor verifica la situación inmediatamente.\n\nUbicación: {maps}\n\nMagnitud del impacto: {magnitude} m/s²") 
        ) 
    }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    var alertToDelete by remember { mutableStateOf<FallAlert?>(null) }

    val alerts by database.fallAlertDao().observeByPatient(patientId).collectAsState(initial = emptyList())

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        android.util.Log.d("FallAlertScreen", "Contact picker result: $uri")
        uri?.let {
            val contactInfo = extractContactInfo(context, it)
            android.util.Log.d("FallAlertScreen", "Contact info extracted: $contactInfo")
            if (contactInfo != null) {
                val currentContacts = loadEmergencyContacts(prefs, patientId)
                android.util.Log.d("FallAlertScreen", "Current contacts from prefs: ${currentContacts.size}")
                if (currentContacts.size < 4) {
                    val updatedContacts = currentContacts + contactInfo
                    saveEmergencyContacts(prefs, patientId, updatedContacts)
                    android.util.Log.d("FallAlertScreen", "Contact saved to prefs. New count: ${updatedContacts.size}")
                    contactReloadTrigger++
                    Toast.makeText(context, "Contacto agregado: ${contactInfo.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Máximo de 4 contactos alcanzado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No se pudo extraer la información del contacto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Se necesita permiso de contactos para seleccionar de la agenda", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Sin permiso de ubicación, la alerta no incluirá GPS", Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (name, granted) ->
            android.util.Log.d("FallAlertScreen", "Permiso $name -> granted=$granted")
        }
        val postGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        } else true
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: (
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
        android.util.Log.d("FallAlertScreen", "postGranted=$postGranted smsGranted=$smsGranted")
        if (postGranted) {
            val phonesString = contactPhones.map { it.phone }.joinToString(",")
            startMonitoring(context, patientId, phonesString, sensitivity, edadPaciente, alturaCmPaciente)
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false) {
                Toast.makeText(context, "Monitoreo activo. Concede permiso de ubicación para incluir GPS en la alerta.", Toast.LENGTH_LONG).show()
            }
            if (!smsGranted) {
                Toast.makeText(context, "Monitoreo activo. Concede permiso SMS para envío automático de alertas.", Toast.LENGTH_LONG).show()
            }
        } else {
            showPermissionDeniedDialog = true
            isEnabled = false
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permisos necesarios") },
            text = { Text("Para activar la detección de caídas se necesita el permiso de notificaciones. Ábrelo manualmente en la configuración de la app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Ir a configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSensitivityDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSensitivityDialog = false
                pendingSensitivity = sensitivity
            },
            title = { Text("Confirmar cambio de sensibilidad") },
            text = { 
                Text("¿Estás seguro de cambiar la sensibilidad de detección de caídas?\n\n" +
                     "Sensibilidad actual: ${"%.0f".format(sensitivity * 100)}%\n" +
                     "Nueva sensibilidad: ${"%.0f".format(pendingSensitivity * 100)}%\n\n" +
                     "Una sensibilidad más alta detectará caídas más leves pero puede generar falsas alarmas.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sensitivity = pendingSensitivity
                        prefs.edit().putFloat(KEY_SENSITIVITY, sensitivity).apply()
                        showSensitivityDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSensitivityDialog = false
                    pendingSensitivity = sensitivity
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSoundConfigDialog) {
        val availableSounds = loadAvailableAlarmSounds(context)
        AlertDialog(
            onDismissRequest = { showSoundConfigDialog = false },
            title = { Text("Configuración de alarma") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sonido de alarma (solo para este módulo):")
                    LazyColumn(
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(availableSounds) { (name, uri) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAlarmSound = uri.toString()
                                        prefs.edit().putString(KEY_ALARM_SOUND_URI, uri.toString()).apply()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAlarmSound == uri.toString(),
                                    onClick = {
                                        selectedAlarmSound = uri.toString()
                                        prefs.edit().putString(KEY_ALARM_SOUND_URI, uri.toString()).apply()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Mensaje de alerta (WhatsApp):")
                    Text(
                        text = "Variables disponibles: {lat}, {lon}, {magnitude}, {maps}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = customSmsMessage ?: "",
                        onValueChange = { customSmsMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1F8EF1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF1F8EF1),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit().putString(KEY_SMS_MESSAGE, customSmsMessage).apply()
                        showSoundConfigDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoundConfigDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Eliminar contacto") },
            text = { Text("¿Eliminar a ${contact.name.ifBlank { contact.phone }} de los contactos de emergencia?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedContacts = contactPhones.toMutableList().apply { remove(contact) }
                        saveEmergencyContacts(prefs, patientId, updatedContacts)
                        contactReloadTrigger++
                        contactToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    alertToDelete?.let { alert ->
        AlertDialog(
            onDismissRequest = { alertToDelete = null },
            title = { Text("Eliminar registro") },
            text = { Text("¿Eliminar este registro del historial de caídas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { database.fallAlertDao().deleteById(alert.id) }
                        alertToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { alertToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerta de caídas") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(MaterialIcons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSoundConfigDialog = true }) {
                        Icon(MaterialIcons.Filled.Settings, contentDescription = "Configuración de sonido", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B0000),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))))
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = MaterialIcons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Monitoreo de caídas",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                prefs.edit().putBoolean(KEY_ENABLED, checked).apply()
                                if (checked) {
                                    val neededPermissions = mutableListOf<String>()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                        neededPermissions.add(Manifest.permission.SEND_SMS)
                                    }
                                    if (neededPermissions.isEmpty()) {
                                        val phonesString = contactPhones.map { it.phone }.joinToString(",")
                                        startMonitoring(context, patientId, phonesString, sensitivity, edadPaciente, alturaCmPaciente)
                                    } else {
                                        permissionLauncher.launch(neededPermissions.toTypedArray())
                                    }
                                } else {
                                    FallDetectionService.stop(context)
                                }
                            }
                        )
                    }
                    Text(
                        text = "Cuando está activo, el dispositivo detecta caídas mediante el acelerómetro y muestra una alarma de emergencia.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensibilidad de detección",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%.0f".format(sensitivity * 100)}%",
                            color = Color(0xFF1F8EF1),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = sensitivity,
                        onValueChange = { 
                            pendingSensitivity = it
                        },
                        onValueChangeFinished = {
                            if (pendingSensitivity != sensitivity) {
                                showSensitivityDialog = true
                            }
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = when {
                            sensitivity >= 0.8f -> "Muy alta - Detecta caídas leves (puede generar falsas alarmas)"
                            sensitivity >= 0.6f -> "Alta - Detecta caídas moderadas"
                            sensitivity >= 0.4f -> "Media - Balance entre detección y falsas alarmas"
                            sensitivity >= 0.2f -> "Baja - Solo detecta caídas fuertes"
                            else -> "Muy baja - Solo detecta caídas muy fuertes"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Contactos de emergencia (${contactPhones.size}/4)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Lista de contactos actuales
                    if (contactPhones.isNotEmpty()) {
                        contactPhones.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (contact.photoUri != null) {
                                        AsyncImage(
                                            model = Uri.parse(contact.photoUri),
                                            contentDescription = "Foto de contacto",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1F8EF1)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (contact.name.isNotBlank()) contact.name.first().uppercaseChar().toString() else contact.phone.first().toString(),
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (contact.name.isNotBlank()) contact.name else contact.phone,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 14.sp,
                                            fontWeight = if (contact.name.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (contact.name.isNotBlank()) {
                                            Text(
                                                text = contact.phone,
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { contactToDelete = contact }
                                ) {
                                    Icon(
                                        MaterialIcons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No hay contactos agregados",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Campo para agregar nuevo contacto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            label = { Text("Nuevo contacto") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1F8EF1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF1F8EF1),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            enabled = contactPhones.size < 4
                        ) {
                            Icon(
                                MaterialIcons.Filled.Person,
                                contentDescription = "Seleccionar de agenda",
                                tint = Color(0xFF1F8EF1)
                            )
                        }
                        Button(
                            onClick = {
                                if (newContactPhone.isNotBlank() && contactPhones.size < 4) {
                                    val updatedContacts = contactPhones + EmergencyContact(newContactPhone.trim())
                                    saveEmergencyContacts(prefs, patientId, updatedContacts)
                                    newContactPhone = ""
                                    contactReloadTrigger++
                                }
                            },
                            enabled = newContactPhone.isNotBlank() && contactPhones.size < 4,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F8EF1))
                        ) {
                            Text("Agregar")
                        }
                    }
                    
                    if (contactPhones.size >= 4) {
                        Text(
                            text = "Máximo de 4 contactos alcanzado",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val phonesString = contactPhones.joinToString(",")
                    val prefs = context.getSharedPreferences("fall_alert_prefs", Context.MODE_PRIVATE)
                    val alarmSoundUri = prefs.getString("alarm_sound_uri", null)
                    
                    val simulatedImpact = 200.0f
                    
                    val intent = android.content.Intent(context, FallAlertActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(FallAlertActivity.EXTRA_PATIENT_ID, patientId)
                        putExtra(FallAlertActivity.EXTRA_CONTACT_PHONE, phonesString)
                        putExtra(FallAlertActivity.EXTRA_IMPACT_MAGNITUDE, simulatedImpact)
                        if (alarmSoundUri != null) {
                            putExtra(FallAlertActivity.EXTRA_ALARM_SOUND_URI, android.net.Uri.parse(alarmSoundUri))
                        }
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Probar alarma de emergencia")
            }

            Text(
                text = "Historial de caídas (${alerts.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (alerts.isEmpty()) {
                Text(
                    text = "No hay caídas registradas.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            onDeleteClick = { alertToDelete = alert }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onVolver,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver al escritorio", color = Color(0xFF64B5F6))
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: FallAlert,
    onDeleteClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.status == FALL_STATUS_CONFIRMED) Color(0xFF4A0000).copy(alpha = 0.6f) else Color(0xFF1A3A1A).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateFallAlert(alert.detectedAt),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Estado: ${if (alert.status == FALL_STATUS_CONFIRMED) "Confirmada" else "Descartada"}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                alert.impactMagnitude?.let {
                    Text(
                        text = "Impacto: %.2f g".format(it / 9.81f),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                if (alert.latitude != null && alert.longitude != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ubicación: %.5f, %.5f".format(alert.latitude, alert.longitude),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:${alert.latitude},${alert.longitude}?q=${alert.latitude},${alert.longitude}(Ubicación de caída)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            }
                        ) {
                            Text("Ver mapa", fontSize = 12.sp, color = Color(0xFF1F8EF1))
                        }
                    }
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(MaterialIcons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252))
            }
        }
    }
}

private fun startMonitoring(context: Context, patientId: Int, contactPhone: String = "", sensitivity: Float = 0.5f, edad: Int = 55, alturaCm: Int = 170) {
    FallDetectionService.start(context, patientId, contactPhone, sensitivity, edad, alturaCm)
    android.util.Log.d("FallAlertScreen", "Monitoreo iniciado: sensitivity=$sensitivity edad=$edad alturaCm=$alturaCm")
    Toast.makeText(context, "Detección de caídas activada", Toast.LENGTH_SHORT).show()
}

private fun loadEmergencyContacts(prefs: android.content.SharedPreferences, patientId: Int): List<EmergencyContact> {
    val gson = Gson()
    val json = prefs.getString(contactsKey(patientId), null)
    return if (json != null) {
        try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("FallAlertScreen", "Error loading contacts", e)
            emptyList()
        }
    } else {
        emptyList()
    }
}

private fun saveEmergencyContacts(prefs: android.content.SharedPreferences, patientId: Int, contacts: List<EmergencyContact>) {
    val gson = Gson()
    val json = gson.toJson(contacts)
    prefs.edit().putString(contactsKey(patientId), json).commit()
}

private fun extractContactInfo(context: Context, contactUri: Uri): EmergencyContact? {
    var phoneNumber = ""
    var displayName = ""
    var photoUri: String? = null
    
    try {
        android.util.Log.d("FallAlertScreen", "Extracting contact info from URI: $contactUri")
        
        // Primero obtener el ID del contacto
        val contactId: String?
        context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                android.util.Log.d("FallAlertScreen", "Contact ID: $contactId")
                
                // Obtener nombre del contacto
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: ""
                    android.util.Log.d("FallAlertScreen", "Contact name: $displayName")
                }
                
                // Obtener foto del contacto
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                if (photoIndex >= 0) {
                    photoUri = cursor.getString(photoIndex)
                    android.util.Log.d("FallAlertScreen", "Contact photo URI: $photoUri")
                }
            } else {
                android.util.Log.e("FallAlertScreen", "Cursor is empty for contact URI")
                return null
            }
        } ?: return null
        
        // Ahora buscar el número de teléfono del contacto
        val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val phoneSelectionArgs = arrayOf(contactId)
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            phoneSelection,
            phoneSelectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    phoneNumber = cursor.getString(numberIndex) ?: ""
                    android.util.Log.d("FallAlertScreen", "Phone number: $phoneNumber")
                }
            } else {
                android.util.Log.e("FallAlertScreen", "No phone number found for contact")
            }
        }
        
    } catch (e: Exception) {
        android.util.Log.e("FallAlertScreen", "Error extracting contact info", e)
        e.printStackTrace()
    }
    
    android.util.Log.d("FallAlertScreen", "Final result: phone='$phoneNumber', name='$displayName', photo='$photoUri'")
    
    return if (phoneNumber.isNotBlank()) {
        EmergencyContact(phoneNumber, displayName, photoUri)
    } else {
        null
    }
}

private fun loadAvailableAlarmSounds(context: Context): List<Pair<String, Uri>> {
    val sounds = mutableListOf<Pair<String, Uri>>()
    try {
        // Agregar sonidos predeterminados
        val defaultAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        defaultAlarm?.let { sounds.add("Alarma del sistema" to it) }
        
        val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        defaultRingtone?.let { sounds.add("Tono de llamada" to it) }
        
        val defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        defaultNotification?.let { sounds.add("Notificación" to it) }

        // Cargar todas las alarmas del sistema
        val alarmManager = RingtoneManager(context)
        alarmManager.setType(RingtoneManager.TYPE_ALARM)
        val alarmCursor = alarmManager.cursor
        while (alarmCursor.moveToNext()) {
            val title = alarmCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = alarmManager.getRingtoneUri(alarmCursor.position)
            if (title != null && uri != null && !sounds.any { it.first == title }) {
                sounds.add(title to uri)
            }
        }
        alarmCursor.close()

        // Cargar tonos de llamada
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
        val ringtoneCursor = ringtoneManager.cursor
        while (ringtoneCursor.moveToNext()) {
            val title = ringtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(ringtoneCursor.position)
            if (title != null && uri != null && !sounds.any { it.first == title }) {
                sounds.add(title to uri)
            }
        }
        ringtoneCursor.close()

        // Cargar notificaciones
        val notificationManager = RingtoneManager(context)
        notificationManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val notificationCursor = notificationManager.cursor
        while (notificationCursor.moveToNext()) {
            val title = notificationCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = notificationManager.getRingtoneUri(notificationCursor.position)
            if (title != null && uri != null && !sounds.any { it.first == title }) {
                sounds.add(title to uri)
            }
        }
        notificationCursor.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sounds
}

private fun formatDateFallAlert(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}
