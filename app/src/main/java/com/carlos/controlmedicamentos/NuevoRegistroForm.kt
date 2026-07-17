package com.carlos.controlmedicamentos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.content.Intent
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NuevoRegistroForm(
    nombre: String, onNombreChange: (String) -> Unit,
    cantidad: String, onCantidadChange: (String) -> Unit,
    horaTomaSeleccionada: String, onHoraTomaChange: (String) -> Unit,
    formatoSeleccionado: String,
    formaInsumoSeleccionada: String, onFormaChange: (String) -> Unit,
    colorInsumoSeleccionado: androidx.compose.ui.graphics.Color, onColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    colorInsumo2Seleccionado: androidx.compose.ui.graphics.Color, onColor2Change: (androidx.compose.ui.graphics.Color) -> Unit,
    concentracionSeleccionada: String, onConcentracionChange: (String) -> Unit,
    cicloSeleccionado: String, onCicloChange: (String) -> Unit,
    fechaInicio: Long, onFechaInicioChange: (Long) -> Unit,
    fechaFin: Long, onFechaFinChange: (Long) -> Unit,
    stockActual: String, onStockActualChange: (String) -> Unit,
    stockMinimo: String, onStockMinimoChange: (String) -> Unit,
    precioPorUnidad: String, onPrecioChange: (String) -> Unit,
    telefonoPedidoWhatsapp: String, onTelefonoChange: (String) -> Unit,
    presentacionPersistida: String,
    esCicloCorto: Boolean, onEsCicloCortoChange: (Boolean) -> Unit,
    estaActivo: Boolean, onEstaActivoChange: (Boolean) -> Unit,
    alarmaActiva: Boolean, onAlarmaActivaChange: (Boolean) -> Unit,
    controlarExistencias: Boolean, onControlarExistenciasChange: (Boolean) -> Unit,
    dispensacionGratuita: Boolean, onDispensacionGratuitaChange: (Boolean) -> Unit,
    expandedNombre: Boolean, onExpandedNombreChange: (Boolean) -> Unit,
    expandedToma: Boolean, onExpandedTomaChange: (Boolean) -> Unit,
    expandedConcentracion: Boolean, onExpandedConcentracionChange: (Boolean) -> Unit,
    expandedCiclo: Boolean, onExpandedCicloChange: (Boolean) -> Unit,
    expandedOrigenReposicion: Boolean, onExpandedOrigenReposicionChange: (Boolean) -> Unit,
    horasTomas: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    sugerencias: List<VademecumMedication>,
    selectedMedication: VademecumMedication?, onSelectedMedicationChange: (VademecumMedication?) -> Unit,
    tomaSeleccionada: String, onTomaSeleccionadaChange: (String) -> Unit,
    origenReposicion: String, onOrigenReposicionChange: (String) -> Unit,
    editingMedicationId: Int?,
    pacienteActivo: PatientProfile?,
    insumosGuardados: List<Medication>,
    monedaActiva: String,
    database: AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    panelInternoScrollState: androidx.compose.foundation.ScrollState,
    tienePermisoNotificaciones: Boolean,
    tienePermisoAlarmaExacta: Boolean,
    tienePermisoPantallaCompleta: Boolean,
    tieneAccesoNoMolestar: Boolean,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>,
    fullScreenIntentPermissionLauncher: ActivityResultLauncher<Intent>,
    notificationPolicyAccessLauncher: ActivityResultLauncher<Intent>,
    duplicateMedication: Medication?, onDuplicateMedicationChange: (Medication?) -> Unit,
    onAbrirNuevaFichaPaciente: () -> Unit,
    onResetForm: () -> Unit,
    onCerrarPanelesSecundarios: () -> Unit,
    onMostrarFormularioChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val ciclos = listOf("Diario", "Cada 12 horas", "Cada 8 horas", "Cada 6 horas", "Semanal", "Mensual", "A demanda")
    val opcionesToma = listOf("En una sola toma", "En diferentes horarios")

    MetallicMedicationCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = 16,
        verticalSpacing = 8,
        expandVertically = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(panelInternoScrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Nuevo Registro",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expandedNombre && sugerencias.isNotEmpty(),
                onExpandedChange = { onExpandedNombreChange(!expandedNombre) }
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        onNombreChange(it)
                        onExpandedNombreChange(it.isNotBlank())
                    },
                    label = { Text("Nombre del medicamento") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNombre && sugerencias.isNotEmpty())
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedNombre && sugerencias.isNotEmpty(),
                    onDismissRequest = { onExpandedNombreChange(false) }
                ) {
                    sugerencias.forEach { insumo ->
                        DropdownMenuItem(
                            text = { Text(insumo.nombre) },
                            onClick = {
                                onNombreChange(insumo.nombre)
                                onSelectedMedicationChange(insumo)
                                onConcentracionChange(insumo.concentraciones.firstOrNull().orEmpty())
                                onExpandedNombreChange(false)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cantidad,
                onValueChange = { if (it.all(Char::isDigit)) onCantidadChange(it) },
                label = { Text("Cantidad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val cantidadInt = cantidad.toIntOrNull() ?: 0
            if (cantidadInt > 1) {
                ExposedDropdownMenuBox(
                    expanded = expandedToma,
                    onExpandedChange = { onExpandedTomaChange(!expandedToma) }
                ) {
                    OutlinedTextField(
                        value = tomaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reparto de la cantidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToma) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedToma,
                        onDismissRequest = { onExpandedTomaChange(false) }
                    ) {
                        opcionesToma.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    onTomaSeleccionadaChange(opcion)
                                    onExpandedTomaChange(false)
                                }
                            )
                        }
                    }
                }

                if (tomaSeleccionada == "En diferentes horarios") {
                    val maxTomas = cantidadInt.coerceAtMost(5)
                    for (i in 0 until maxTomas) {
                        val horaActual = horasTomas.getOrNull(i).orEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "Toma ${i + 1}", modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    while (horasTomas.size <= i) { horasTomas.add("") }
                                    TimePickerDialog(context, { _, hour, minute ->
                                        horasTomas[i] = String.format("%02d:%02d", hour, minute)
                                    }, 8, 0, true).show()
                                }
                            ) {
                                Text(if (horaActual.isBlank()) "Seleccionar hora" else horaActual)
                            }
                        }
                    }
                }
            }

            val usaHoraUnica = cantidadInt <= 1 || tomaSeleccionada == "En una sola toma"
            if (usaHoraUnica) {
                TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = horaTomaSeleccionada.ifBlank { "Selecciona una hora" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora del recordatorio") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Button(onClick = {
                                val horaBase = horaTomaSeleccionada.ifBlank { "08:00" }.split(":")
                                val horaInicial = horaBase.getOrNull(0)?.toIntOrNull() ?: 8
                                val minutoInicial = horaBase.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, hour, minute ->
                                    onHoraTomaChange(String.format("%02d:%02d", hour, minute))
                                }, horaInicial, minutoInicial, true).show()
                            }) { Text("Elegir") }
                        }
                    )
                }
            }

            TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                SelectorIconoMedicina(
                    formaActual = formaInsumoSeleccionada,
                    colorActual = colorInsumoSeleccionado,
                    color2Actual = colorInsumo2Seleccionado,
                    onFormaChange = onFormaChange,
                    onColorChange = onColorChange,
                    onColor2Change = onColor2Change
                )
            }

            val concentracionOpciones = selectedMedication?.concentraciones.orEmpty()
            ExposedDropdownMenuBox(
                expanded = expandedConcentracion && concentracionOpciones.isNotEmpty(),
                onExpandedChange = { onExpandedConcentracionChange(!expandedConcentracion) }
            ) {
                OutlinedTextField(
                    value = concentracionSeleccionada,
                    onValueChange = onConcentracionChange,
                    label = { Text("Especificación") },
                    placeholder = { Text("Ej: 500mg, 0.25%, 10UI") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (concentracionOpciones.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConcentracion)
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedConcentracion && concentracionOpciones.isNotEmpty(),
                    onDismissRequest = { onExpandedConcentracionChange(false) }
                ) {
                    concentracionOpciones.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = { onConcentracionChange(opcion); onExpandedConcentracionChange(false) }
                        )
                    }
                }
            }

            VademecumDropdown(
                label = "Ciclo",
                options = ciclos,
                selectedValue = cicloSeleccionado,
                expanded = expandedCiclo,
                onExpandedChange = { onExpandedCicloChange(!expandedCiclo) },
                onDismiss = { onExpandedCicloChange(false) },
                onSelect = { onCicloChange(it); onExpandedCicloChange(false) }
            )

            DateSelector(
                label = "Fecha de inicio",
                selectedDate = fechaInicio,
                onPickDate = {
                    calendar.timeInMillis = fechaInicio
                    DatePickerDialog(context, { _, year, month, dayOfMonth ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, 8, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onFechaInicioChange(picked.timeInMillis)
                        if (fechaFin < picked.timeInMillis) onFechaFinChange(picked.timeInMillis)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }
            )

            if (esCicloCorto) {
                DateSelector(
                    label = "Fecha de fin",
                    selectedDate = fechaFin,
                    onPickDate = {
                        calendar.timeInMillis = fechaFin
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val picked = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth, 8, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onFechaFinChange(picked.timeInMillis)
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = esCicloCorto, onCheckedChange = onEsCicloCortoChange)
                Text("¿Es ciclo corto?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = estaActivo, onCheckedChange = onEstaActivoChange)
                Text("¿Activo?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alarmaActiva, onCheckedChange = onAlarmaActivaChange)
                Text("¿Alarma activa?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = controlarExistencias,
                    onCheckedChange = {
                        onControlarExistenciasChange(it)
                        if (!it) {
                            onDispensacionGratuitaChange(false)
                            onPrecioChange("")
                            onTelefonoChange("")
                            onOrigenReposicionChange("WHATSAPP_NUMBER")
                        }
                    }
                )
                Text("¿Controlar existencias?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = controlarExistencias && dispensacionGratuita,
                    onCheckedChange = {
                        onDispensacionGratuitaChange(it)
                        onOrigenReposicionChange(if (it) "INSS" else "WHATSAPP_NUMBER")
                        if (it) { onPrecioChange(""); onTelefonoChange("") }
                    },
                    enabled = controlarExistencias
                )
                Text("Adquisición sin costo")
            }

            if (controlarExistencias) {
                TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stockActual,
                        onValueChange = { if (it.all(Char::isDigit)) onStockActualChange(it) },
                        label = { Text("Stock actual (unidades)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = stockMinimo,
                        onValueChange = { if (it.all(Char::isDigit)) onStockMinimoChange(it) },
                        label = { Text("Aviso de stock (unidades minimas)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    if (!dispensacionGratuita) {
                        OutlinedTextField(
                            value = precioPorUnidad,
                            onValueChange = { v ->
                                if (v.isEmpty() || v.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) onPrecioChange(v)
                            },
                            label = { Text("Precio por unidad ($monedaActiva)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        val origenesReposicion: List<Pair<String, String>> = listOf(
                            "WHATSAPP_NUMBER" to "WhatsApp (numero directo)",
                            "WHATSAPP_CONTACT" to "WhatsApp (contacto guardado)"
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedOrigenReposicion,
                            onExpandedChange = { onExpandedOrigenReposicionChange(!expandedOrigenReposicion) }
                        ) {
                            OutlinedTextField(
                                value = origenesReposicion.firstOrNull { (valor, _) -> valor == origenReposicion }?.second ?: "WhatsApp (numero directo)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Origen de reposicion") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigenReposicion) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedOrigenReposicion,
                                onDismissRequest = { onExpandedOrigenReposicionChange(false) }
                            ) {
                                origenesReposicion.forEach { (valor: String, etiqueta: String) ->
                                    DropdownMenuItem(
                                        text = { Text(etiqueta) },
                                        onClick = { onOrigenReposicionChange(valor); onExpandedOrigenReposicionChange(false) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = telefonoPedidoWhatsapp,
                            onValueChange = onTelefonoChange,
                            label = { Text("Telefono WhatsApp del pedido") },
                            placeholder = { Text("+34612345678") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }

            if (alarmaActiva) {
                TransparentFormSectionCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Alerta critica compartida")
                    Text("El sonido y los reintentos se configuran desde el menú hamburguesa para todos los medicamentos.")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val cantidadIntFinal = cantidad.toIntOrNull() ?: 0
            val usaHoraUnicaFinal = cantidadIntFinal <= 1 || tomaSeleccionada == "En una sola toma"

            Button(
                onClick = {
                    val activePatient = pacienteActivo
                    if (activePatient == null) {
                        onAbrirNuevaFichaPaciente()
                        Toast.makeText(context, "Primero crea o selecciona un paciente", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (nombre.isBlank()) {
                        Toast.makeText(context, "Introduce un medicamento", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cantidadIntFinal > 1 && tomaSeleccionada == "En diferentes horarios" && horasTomas.any { it.isBlank() }) {
                        Toast.makeText(context, "Selecciona la hora de cada toma", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (usaHoraUnicaFinal && horaTomaSeleccionada.isBlank()) {
                        Toast.makeText(context, "Elige manualmente la hora del recordatorio", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (alarmaActiva && !tienePermisoNotificaciones) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        Toast.makeText(context, "Concede permiso de notificaciones para activar recordatorios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (alarmaActiva && !tienePermisoAlarmaExacta) {
                        exactAlarmPermissionLauncher.launch(buildExactAlarmPermissionIntent(context))
                        Toast.makeText(context, "Concede permiso de alarmas exactas para programar recordatorios precisos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (alarmaActiva && !tienePermisoPantallaCompleta) {
                        fullScreenIntentPermissionLauncher.launch(buildFullScreenIntentPermissionIntent(context))
                        Toast.makeText(context, "Concede permiso de pantalla completa para que la alarma critica pueda saltar sobre la pantalla bloqueada", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (alarmaActiva && !tieneAccesoNoMolestar) {
                        notificationPolicyAccessLauncher.launch(buildNotificationPolicyAccessIntent())
                        Toast.makeText(context, "Concede acceso a No molestar para habilitar alertas críticas", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val duplicate = insumosGuardados.firstOrNull { existente ->
                        existente.id != (editingMedicationId ?: 0) && sonMedicamentosDuplicados(
                            existente = existente,
                            nombre = nombre,
                            formato = formatoSeleccionado,
                            concentracion = concentracionSeleccionada
                        )
                    }
                    if (duplicate != null) {
                        onDuplicateMedicationChange(duplicate)
                        return@Button
                    }
                    val criticalAlertConfig = CriticalAlertSettings.load(context)
                    val colorHex = "#%06X".format(colorInsumoSeleccionado.toArgb() and 0xFFFFFF)
                    val colorHex2 = "#%06X".format(colorInsumo2Seleccionado.toArgb() and 0xFFFFFF)
                    val medication = Medication(
                        id = editingMedicationId ?: 0,
                        patientId = activePatient.id,
                        nombre = nombre,
                        dosis = cantidad.ifBlank { "1" },
                        formato = formatoSeleccionado,
                        formaMedicamento = formaInsumoSeleccionada,
                        colorMedicamento = colorHex,
                        colorMedicamento2 = if (formaInsumoSeleccionada in FORMAS_DOS_COLORES) colorHex2 else "",
                        presentacion = presentacionPersistida,
                        concentracion = concentracionSeleccionada,
                        repartoDosis = if (cantidadIntFinal > 1) tomaSeleccionada else "En una sola toma",
                        horariosTomas = if (cantidadIntFinal > 1 && tomaSeleccionada == "En diferentes horarios") {
                            horasTomas.joinToString("|")
                        } else { "" },
                        fechaInicio = fechaInicio,
                        fechaFin = if (esCicloCorto) fechaFin else fechaInicio,
                        horaToma = when {
                            cantidadIntFinal > 1 && tomaSeleccionada == "En diferentes horarios" -> horasTomas.firstOrNull().orEmpty()
                            else -> horaTomaSeleccionada
                        },
                        frecuenciaHoras = cicloToHours(cicloSeleccionado),
                        esCicloCorto = esCicloCorto,
                        retryIntervalMinutes = criticalAlertConfig.retryIntervalMinutes,
                        alarmaSonidoUri = criticalAlertConfig.soundUri,
                        alarmaActiva = alarmaActiva,
                        estaActivo = estaActivo,
                        stockActual = if (controlarExistencias) stockActual.toIntOrNull() else null,
                        stockMinimo = if (controlarExistencias) stockMinimo.toIntOrNull() else null,
                        precioPorUnidad = if (controlarExistencias && !dispensacionGratuita) precioPorUnidad.toDoubleOrNull() else null,
                        telefonoPedidoWhatsapp = if (controlarExistencias && !dispensacionGratuita) telefonoPedidoWhatsapp else "",
                        origenReposicion = if (controlarExistencias) (if (dispensacionGratuita) "INSS" else "WHATSAPP_NUMBER") else "WHATSAPP_NUMBER"
                    )
                    val isEditing = editingMedicationId != null
                    val scheduler = MedicationScheduler(context)
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            if (!isEditing) {
                                val newId = database.medicationDao().insertar(medication).toInt()
                                scheduler.programarAlarmas(medication.copy(id = newId))
                            } else {
                                database.medicationDao().actualizar(medication)
                                scheduler.programarAlarmas(medication)
                            }
                            withContext(Dispatchers.Main) {
                                onResetForm()
                                onMostrarFormularioChange(false)
                                Toast.makeText(
                                    context,
                                    if (isEditing) "Medicamento actualizado" else "Medicamento guardado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (exception: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "No se pudo guardar o programar la alarma: ${exception.message ?: "error desconocido"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingMedicationId == null) "Guardar" else "Guardar cambios")
            }

            if (editingMedicationId != null) {
                Button(
                    onClick = { onResetForm(); onCerrarPanelesSecundarios() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar edición")
                }
            }

            IconButton(
                onClick = { onResetForm(); onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver al escritorio", tint = Color.Black)
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
