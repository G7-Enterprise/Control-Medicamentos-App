package com.carlos.controlmedicamentos

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.carlos.controlmedicamentos.data.local.*
import com.carlos.controlmedicamentos.data.remote.FakeVademecumRepository
import com.carlos.controlmedicamentos.data.remote.MedicalAiConfig
import com.carlos.controlmedicamentos.data.remote.MedicalAiSettings
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.notifications.CriticalAlertConfig
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun resetFormAction(
    setNombre: (String) -> Unit,
    setCantidad: (String) -> Unit,
    setSelectedMedication: (VademecumMedication?) -> Unit,
    setFormatoSeleccionado: (String) -> Unit,
    setFormaInsumoSeleccionada: (String) -> Unit,
    setColorInsumoSeleccionado: (Color) -> Unit,
    setColorInsumo2Seleccionado: (Color) -> Unit,
    setPresentacionPersistida: (String) -> Unit,
    setConcentracionSeleccionada: (String) -> Unit,
    setMostrarConcentracionLibre: (Boolean) -> Unit,
    setCicloSeleccionado: (String) -> Unit,
    setTomaSeleccionada: (String) -> Unit,
    horasTomas: SnapshotStateList<String>,
    setFechaInicio: (Long) -> Unit,
    setFechaFin: (Long) -> Unit,
    setHoraTomaSeleccionada: (String) -> Unit,
    setAlarmaActiva: (Boolean) -> Unit,
    setEsCicloCorto: (Boolean) -> Unit,
    setEstaActivo: (Boolean) -> Unit,
    setEditingMedicationId: (Int?) -> Unit,
    setControlarExistencias: (Boolean) -> Unit,
    setStockActual: (String) -> Unit,
    setStockMinimo: (String) -> Unit,
    setPrecioPorUnidad: (String) -> Unit,
    setTelefonoPedidoWhatsapp: (String) -> Unit,
    setDispensacionGratuita: (Boolean) -> Unit,
    setOrigenReposicion: (String) -> Unit
) {
    setNombre("")
    setCantidad("")
    setSelectedMedication(null)
    setFormatoSeleccionado("")
    setFormaInsumoSeleccionada("")
    setColorInsumoSeleccionado(Color(0xFFFFFF00))
    setColorInsumo2Seleccionado(Color(0xFFFFFFFF))
    setPresentacionPersistida("")
    setConcentracionSeleccionada("")
    setMostrarConcentracionLibre(false)
    setCicloSeleccionado("Diario")
    setTomaSeleccionada("En una sola toma")
    horasTomas.clear()
    setFechaInicio(System.currentTimeMillis())
    setFechaFin(System.currentTimeMillis())
    setHoraTomaSeleccionada("")
    setAlarmaActiva(true)
    setEsCicloCorto(false)
    setEstaActivo(true)
    setEditingMedicationId(null)
    setControlarExistencias(false)
    setStockActual("")
    setStockMinimo("")
    setPrecioPorUnidad("")
    setTelefonoPedidoWhatsapp("")
    setDispensacionGratuita(false)
    setOrigenReposicion(RestockSource.WHATSAPP_NUMBER)
}

internal fun cerrarPanelesSecundariosAction(
    setMostrarFichaPaciente: (Boolean) -> Unit,
    setMostrarFormularioInforme: (Boolean) -> Unit,
    setMostrarFormularioCitaMedica: (Boolean) -> Unit,
    setMostrarPanelPacientes: (Boolean) -> Unit,
    setMostrarPanelCitasMedicas: (Boolean) -> Unit,
    setMostrarPanelProfesionales: (Boolean) -> Unit,
    setMostrarPanelInformes: (Boolean) -> Unit,
    setMostrarListaInsumos: (Boolean) -> Unit,
    setMostrarPanelBackups: (Boolean) -> Unit,
    setMostrarPanelPedidos: (Boolean) -> Unit,
    setMostrarPanelPodometro: (Boolean) -> Unit,
    setMostrarPanelConfiguracionAlertas: (Boolean) -> Unit,
    setMostrarPanelSignosVitales: (Boolean) -> Unit,
    setMostrarListadoSignosPanel: (Boolean) -> Unit,
    mesesExpandidosSignos: SnapshotStateList<String>,
    setMostrarPanelConfiguracionIa: (Boolean) -> Unit,
    setMostrarPanelAsistenteIa: (Boolean) -> Unit,
    setMostrarPanelCicloMenstrual: (Boolean) -> Unit,
    setMostrarPanelEmbarazo: (Boolean) -> Unit,
    setMostrarPanelAnticonceptivos: (Boolean) -> Unit,
    setMostrarPanelPediatrico: (Boolean) -> Unit,
    setMostrarPanelReporteClinico: (Boolean) -> Unit,
    setMostrarPanelEstadisticas: (Boolean) -> Unit,
    setMostrarPanelDiario: (Boolean) -> Unit,
    fallAlertPanelState: androidx.compose.runtime.MutableState<Boolean>,
    setMostrarPanelVerificadorTomas: (Boolean) -> Unit,
    setMostrarPanelHidratacion: (Boolean) -> Unit,
    setMostrarPanelSedentarismo: (Boolean) -> Unit,
    setMostrarPanelDentista: (Boolean) -> Unit,
    setMostrarFormulario: (Boolean) -> Unit,
    setTomaPendienteDeEliminar: (IntakeRemovalConfirmation?) -> Unit
) {
    setMostrarFichaPaciente(false)
    setMostrarFormularioInforme(false)
    setMostrarFormularioCitaMedica(false)
    setMostrarPanelPacientes(false)
    setMostrarPanelCitasMedicas(false)
    setMostrarPanelProfesionales(false)
    setMostrarPanelInformes(false)
    setMostrarListaInsumos(false)
    setMostrarPanelBackups(false)
    setMostrarPanelPedidos(false)
    setMostrarPanelPodometro(false)
    setMostrarPanelConfiguracionAlertas(false)
    setMostrarPanelSignosVitales(false)
    setMostrarListadoSignosPanel(false)
    mesesExpandidosSignos.clear()
    setMostrarPanelConfiguracionIa(false)
    setMostrarPanelAsistenteIa(false)
    setMostrarPanelCicloMenstrual(false)
    setMostrarPanelEmbarazo(false)
    setMostrarPanelAnticonceptivos(false)
    setMostrarPanelPediatrico(false)
    setMostrarPanelReporteClinico(false)
    setMostrarPanelEstadisticas(false)
    setMostrarPanelDiario(false)
    fallAlertPanelState.value = false
    setMostrarPanelVerificadorTomas(false)
    setMostrarPanelHidratacion(false)
    setMostrarPanelSedentarismo(false)
    setMostrarPanelDentista(false)
    setMostrarFormulario(false)
    setTomaPendienteDeEliminar(null)
}

internal fun guardarConfiguracionAlertasCriticasAction(
    context: Context,
    intervaloReintentoSeleccionado: Int,
    numeroIntentosCriticosSeleccionado: Int,
    alarmaSonidoUri: String,
    setAlarmaSonidoNombre: (String) -> Unit,
    resolveAlarmSoundLabel: (Context, String) -> String
) {
    val config = CriticalAlertConfig(
        retryIntervalMinutes = intervaloReintentoSeleccionado,
        maxRetryCount = numeroIntentosCriticosSeleccionado,
        soundUri = alarmaSonidoUri
    )
    CriticalAlertSettings.save(context, config)
    setAlarmaSonidoNombre(resolveAlarmSoundLabel(context, alarmaSonidoUri))
}

internal fun guardarConfiguracionIaAction(
    context: Context,
    urlServicioIa: String,
    modeloServicioIa: String
) {
    MedicalAiSettings.save(
        context,
        MedicalAiConfig(endpointUrl = urlServicioIa, modelName = modeloServicioIa)
    )
}

internal fun resetFichaPacienteAction(
    setEditingPatientId: (Int?) -> Unit,
    setEditandoFichaPaciente: (Boolean) -> Unit,
    setNombrePaciente: (String) -> Unit,
    setApellidosPaciente: (String) -> Unit,
    setFechaNacimientoPaciente: (Long?) -> Unit,
    setEdadPaciente: (String) -> Unit,
    setPesoPaciente: (String) -> Unit,
    setPesoUnidadPaciente: (String) -> Unit,
    setEstaturaPaciente: (String) -> Unit,
    setEstaturaUnidadPaciente: (String) -> Unit,
    setSexoPaciente: (String) -> Unit,
    setPaisPaciente: (String) -> Unit,
    setMonedaPaciente: (String) -> Unit,
    setEnfermedadesPaciente: (String) -> Unit,
    setPrescripcionesPaciente: (String) -> Unit,
    setFotoPerfilPaciente: (String?) -> Unit
) {
    setEditingPatientId(null)
    setEditandoFichaPaciente(true)
    setNombrePaciente("")
    setApellidosPaciente("")
    setFechaNacimientoPaciente(null)
    setEdadPaciente("")
    setPesoPaciente("")
    setPesoUnidadPaciente("kg")
    setEstaturaPaciente("")
    setEstaturaUnidadPaciente("cm")
    setSexoPaciente("")
    setPaisPaciente(CountryCurrencyCatalog.DEFAULT_COUNTRY)
    setMonedaPaciente(CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL)
    setEnfermedadesPaciente("")
    setPrescripcionesPaciente("")
    setFotoPerfilPaciente(null)
}

internal fun resetCitaMedicaAction(
    setEditingAppointmentId: (Int?) -> Unit,
    setCitaMedicaSeleccionadaId: (Int?) -> Unit,
    setTituloCitaMedica: (String) -> Unit,
    setProfesionalCitaMedica: (String) -> Unit,
    setLugarCitaMedica: (String) -> Unit,
    setNotasCitaMedica: (String) -> Unit,
    setFechaCitaMedica: (Long) -> Unit,
    setRecordatorioCitaMinutos: (Int) -> Unit,
    setAlarmaCitaMedicaActiva: (Boolean) -> Unit,
    setExpandedRecordatorioCita: (Boolean) -> Unit
) {
    setEditingAppointmentId(null)
    setCitaMedicaSeleccionadaId(null)
    setTituloCitaMedica("")
    setProfesionalCitaMedica("")
    setLugarCitaMedica("")
    setNotasCitaMedica("")
    setFechaCitaMedica(siguienteHoraDisponible())
    setRecordatorioCitaMinutos(60)
    setAlarmaCitaMedicaActiva(true)
    setExpandedRecordatorioCita(false)
}

internal fun resetMedicoHabitualAction(
    setEditingPractitionerId: (Int?) -> Unit,
    setNombreProfesional: (String) -> Unit,
    setEspecialidadProfesional: (String) -> Unit
) {
    setEditingPractitionerId(null)
    setNombreProfesional("")
    setEspecialidadProfesional("")
}

internal fun cargarFichaPacienteAction(
    profile: PatientProfile,
    calcularEdadDesdeNacimiento: (Long) -> Int,
    setEditingPatientId: (Int?) -> Unit,
    setNombrePaciente: (String) -> Unit,
    setApellidosPaciente: (String) -> Unit,
    setFechaNacimientoPaciente: (Long?) -> Unit,
    setEdadPaciente: (String) -> Unit,
    setPesoPaciente: (String) -> Unit,
    setPesoUnidadPaciente: (String) -> Unit,
    setEstaturaPaciente: (String) -> Unit,
    setEstaturaUnidadPaciente: (String) -> Unit,
    setSexoPaciente: (String) -> Unit,
    setPaisPaciente: (String) -> Unit,
    setMonedaPaciente: (String) -> Unit,
    setEnfermedadesPaciente: (String) -> Unit,
    setPrescripcionesPaciente: (String) -> Unit,
    setFotoPerfilPaciente: (String?) -> Unit
) {
    setEditingPatientId(profile.id)
    setNombrePaciente(profile.nombre)
    setApellidosPaciente(profile.apellidos)
    setFechaNacimientoPaciente(profile.fechaNacimiento.takeIf { it > 0L })
    setEdadPaciente(profile.edad.ifBlank {
        profile.fechaNacimiento.takeIf { it > 0L }
            ?.let { calcularEdadDesdeNacimiento(it).toString() }
            .orEmpty()
    })
    setPesoPaciente(profile.peso)
    setPesoUnidadPaciente(profile.pesoUnidad)
    setEstaturaPaciente(profile.estatura)
    setEstaturaUnidadPaciente(profile.estaturaUnidad)
    setSexoPaciente(profile.sexo)
    val pais = profile.pais.ifBlank { CountryCurrencyCatalog.DEFAULT_COUNTRY }
    setPaisPaciente(pais)
    setMonedaPaciente(CountryCurrencyCatalog.symbolFor(pais, profile.moneda))
    setEnfermedadesPaciente(profile.enfermedades)
    setPrescripcionesPaciente(profile.prescripciones)
    setFotoPerfilPaciente(profile.fotoPerfil)
}

internal fun resetInformeMedicoAction(
    setEditingReportId: (Int?) -> Unit,
    setPractitionerIdInforme: (Int?) -> Unit,
    setTituloInforme: (String) -> Unit,
    setDescripcionInforme: (String) -> Unit,
    estudiosAdjuntos: SnapshotStateList<String>,
    setBorradorInformeInicial: (ReportDraftSnapshot) -> Unit
) {
    setEditingReportId(null)
    setPractitionerIdInforme(null)
    setTituloInforme("")
    setDescripcionInforme("")
    estudiosAdjuntos.clear()
    setBorradorInformeInicial(ReportDraftSnapshot())
}

internal fun guardarMedicoHabitualActualAction(
    context: Context,
    pacienteActivo: PatientProfile?,
    nombreProfesional: String,
    especialidadProfesional: String,
    editingPractitionerId: Int?,
    profesionalesHabituales: List<MedicalPractitioner>,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    setProfesionalSeleccionadoId: (Int) -> Unit,
    setEditingPractitionerId: (Int?) -> Unit,
    setNombreProfesional: (String) -> Unit,
    setEspecialidadProfesional: (String) -> Unit,
    setMostrarFormularioProfesional: (Boolean) -> Unit,
    setMostrarPanelProfesionales: (Boolean) -> Unit
) {
    if (pacienteActivo == null) {
        Toast.makeText(context, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
        return
    }
    if (nombreProfesional.isBlank() || especialidadProfesional.isBlank()) {
        Toast.makeText(context, "Completa nombre y especialidad", Toast.LENGTH_SHORT).show()
        return
    }
    val practitionerBase = MedicalPractitioner(
        id = editingPractitionerId ?: 0,
        patientId = pacienteActivo.id,
        name = nombreProfesional.trim(),
        specialty = especialidadProfesional.trim(),
        createdAt = profesionalesHabituales.firstOrNull { it.id == editingPractitionerId }?.createdAt
            ?: System.currentTimeMillis()
    )
    coroutineScope.launch(Dispatchers.IO) {
        val practitionerId = database.medicalPractitionerDao().guardar(practitionerBase).toInt()
        withContext(Dispatchers.Main) {
            setProfesionalSeleccionadoId(practitionerId)
            setEditingPractitionerId(null)
            setNombreProfesional("")
            setEspecialidadProfesional("")
            setMostrarFormularioProfesional(false)
            setMostrarPanelProfesionales(true)
            Toast.makeText(context, "Profesional guardado", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun guardarCitaMedicaActualAction(
    context: Context,
    pacienteActivo: PatientProfile?,
    profesionalCitaMedica: String,
    profesionalesHabituales: List<MedicalPractitioner>,
    citasMedicas: List<MedicalAppointment>,
    editingAppointmentId: Int?,
    tituloCitaMedica: String,
    lugarCitaMedica: String,
    notasCitaMedica: String,
    fechaCitaMedica: Long,
    recordatorioCitaMinutos: Int,
    alarmaCitaMedicaActiva: Boolean,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    onResetCitaMedica: () -> Unit,
    setMostrarFormularioCitaMedica: (Boolean) -> Unit,
    setMostrarPanelCitasMedicas: (Boolean) -> Unit
) {
    if (pacienteActivo == null) {
        Toast.makeText(context, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
        return
    }
    if (profesionalCitaMedica.isBlank()) {
        Toast.makeText(context, "Indica el especialista de la cita", Toast.LENGTH_SHORT).show()
        return
    }
    val practitionerId = profesionalesHabituales.firstOrNull {
        it.name.trim().equals(profesionalCitaMedica.trim(), ignoreCase = true)
    }?.id
    val citaExistente = citasMedicas.firstOrNull { it.id == editingAppointmentId }
    val appointment = MedicalAppointment(
        id = editingAppointmentId ?: 0,
        patientId = pacienteActivo.id,
        title = tituloCitaMedica.ifBlank { "Visita con ${profesionalCitaMedica.trim()}" },
        doctorName = profesionalCitaMedica.trim(),
        practitionerId = practitionerId,
        location = lugarCitaMedica.trim(),
        notes = notasCitaMedica.trim(),
        scheduledAt = fechaCitaMedica,
        reminderMinutes = recordatorioCitaMinutos,
        alarmEnabled = alarmaCitaMedicaActiva,
        isCompleted = citaExistente?.isCompleted ?: false,
        createdAt = citaExistente?.createdAt ?: System.currentTimeMillis()
    )
    coroutineScope.launch(Dispatchers.IO) {
        val scheduler = MedicalAppointmentScheduler(context)
        database.medicalAppointmentDao().guardar(appointment)
        val savedAppointment = if (appointment.id > 0) appointment
        else database.medicalAppointmentDao().obtenerTodosLista().lastOrNull()
        savedAppointment?.let {
            if (it.alarmEnabled && !it.isCompleted) scheduler.programar(it)
            else scheduler.cancelar(it.id)
        }
        withContext(Dispatchers.Main) {
            onResetCitaMedica()
            setMostrarFormularioCitaMedica(false)
            setMostrarPanelCitasMedicas(true)
            Toast.makeText(context, "Cita guardada", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun guardarInformeMedicoActualAction(
    context: Context,
    pacienteActivo: PatientProfile?,
    editingReportId: Int?,
    practitionerIdInforme: Int?,
    tituloInforme: String,
    descripcionInforme: String,
    estudiosAdjuntos: SnapshotStateList<String>,
    database: AppDatabase,
    coroutineScope: CoroutineScope,
    onCerrarFormularioInforme: () -> Unit
) {
    if (pacienteActivo == null) {
        Toast.makeText(context, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
        return
    }
    if (tituloInforme.isBlank()) {
        Toast.makeText(context, "Indica un titulo para el informe", Toast.LENGTH_SHORT).show()
        return
    }
    val reportBase = MedicalReport(
        id = editingReportId ?: 0,
        patientId = pacienteActivo.id,
        practitionerId = practitionerIdInforme,
        titulo = tituloInforme,
        descripcion = descripcionInforme,
        adjuntos = estudiosAdjuntos.joinToString("|")
    )
    coroutineScope.launch(Dispatchers.IO) {
        database.medicalReportDao().guardar(reportBase)
        withContext(Dispatchers.Main) {
            onCerrarFormularioInforme()
            Toast.makeText(context, "Documento guardado", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun cargarMedicamentoEnFormularioAction(
    medication: Medication,
    setMostrarFormulario: (Boolean) -> Unit,
    setEditingMedicationId: (Int?) -> Unit,
    setNombre: (String) -> Unit,
    setCantidad: (String) -> Unit,
    setAlarmaActiva: (Boolean) -> Unit,
    setEsCicloCorto: (Boolean) -> Unit,
    setEstaActivo: (Boolean) -> Unit,
    setFechaInicio: (Long) -> Unit,
    setFechaFin: (Long) -> Unit,
    setHoraTomaSeleccionada: (String) -> Unit,
    setFormatoSeleccionado: (String) -> Unit,
    setFormaInsumoSeleccionada: (String) -> Unit,
    setColorInsumoSeleccionado: (Color) -> Unit,
    setColorInsumo2Seleccionado: (Color) -> Unit,
    setPresentacionPersistida: (String) -> Unit,
    setConcentracionSeleccionada: (String) -> Unit,
    setMostrarConcentracionLibre: (Boolean) -> Unit,
    setTomaSeleccionada: (String) -> Unit,
    horasTomas: SnapshotStateList<String>,
    setCicloSeleccionado: (String) -> Unit,
    setSelectedMedication: (VademecumMedication?) -> Unit,
    setControlarExistencias: (Boolean) -> Unit,
    setStockActual: (String) -> Unit,
    setStockMinimo: (String) -> Unit,
    setPrecioPorUnidad: (String) -> Unit,
    setTelefonoPedidoWhatsapp: (String) -> Unit,
    setOrigenReposicion: (String) -> Unit,
    setDispensacionGratuita: (Boolean) -> Unit
) {
    setMostrarFormulario(true)
    setEditingMedicationId(medication.id)
    setNombre(medication.nombre)
    setCantidad(medication.dosis)
    setAlarmaActiva(medication.alarmaActiva)
    setEsCicloCorto(medication.esCicloCorto)
    setEstaActivo(medication.estaActivo)
    setFechaInicio(medication.fechaInicio)
    setFechaFin(medication.fechaFin)
    setHoraTomaSeleccionada(medication.horaToma)
    setFormatoSeleccionado(medication.formato)
    setFormaInsumoSeleccionada(medication.formaMedicamento)
    setColorInsumoSeleccionado(
        medication.colorMedicamento.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: Color(0xFFFFFF00)
    )
    setColorInsumo2Seleccionado(
        medication.colorMedicamento2.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: Color(0xFFFFFFFF)
    )
    setPresentacionPersistida(medication.presentacion)
    setConcentracionSeleccionada(medication.concentracion)
    setMostrarConcentracionLibre(
        medication.concentracion.isNotBlank() &&
            FakeVademecumRepository.obtenerExacto(medication.nombre)?.concentraciones
                ?.contains(medication.concentracion) != true
    )
    setTomaSeleccionada(medication.repartoDosis.ifBlank { "En una sola toma" })
    horasTomas.clear()
    if (medication.horariosTomas.isNotBlank()) {
        horasTomas.addAll(medication.horariosTomas.split("|"))
    }
    setCicloSeleccionado(hoursToCycle(medication.frecuenciaHoras))
    setSelectedMedication(
        FakeVademecumRepository.obtenerExacto(medication.nombre)
            ?: medicationToVademecum(medication)
    )
    setControlarExistencias(medication.stockActual != null)
    setStockActual(medication.stockActual?.toString() ?: "")
    setStockMinimo(medication.stockMinimo?.toString() ?: "")
    setPrecioPorUnidad(medication.precioPorUnidad?.toString() ?: "")
    setTelefonoPedidoWhatsapp(medication.telefonoPedidoWhatsapp)
    setOrigenReposicion(medication.origenReposicion.ifBlank { RestockSource.WHATSAPP_NUMBER })
    setDispensacionGratuita(medication.origenReposicion == RestockSource.INSS)
}

internal fun resetFormWithState(s: MedicamentoFormState) = resetFormAction(
    setNombre = { s.nombreState.value = it },
    setCantidad = { s.cantidadState.value = it },
    setSelectedMedication = { s.selectedMedicationState.value = it },
    setFormatoSeleccionado = { s.formatoSeleccionadoState.value = it },
    setFormaInsumoSeleccionada = { s.formaInsumoSeleccionadaState.value = it },
    setColorInsumoSeleccionado = { s.colorInsumoSeleccionadoState.value = it },
    setColorInsumo2Seleccionado = { s.colorInsumo2SeleccionadoState.value = it },
    setPresentacionPersistida = { s.presentacionPersistidaState.value = it },
    setConcentracionSeleccionada = { s.concentracionSeleccionadaState.value = it },
    setMostrarConcentracionLibre = { s.mostrarConcentracionLibreState.value = it },
    setCicloSeleccionado = { s.cicloSeleccionadoState.value = it },
    setTomaSeleccionada = { s.tomaSeleccionadaState.value = it },
    horasTomas = s.horasTomas,
    setFechaInicio = { s.fechaInicioState.value = it },
    setFechaFin = { s.fechaFinState.value = it },
    setHoraTomaSeleccionada = { s.horaTomaSeleccionadaState.value = it },
    setAlarmaActiva = { s.alarmaActivaState.value = it },
    setEsCicloCorto = { s.esCicloCortoState.value = it },
    setEstaActivo = { s.estaActivoState.value = it },
    setEditingMedicationId = { s.editingMedicationIdState.value = it },
    setControlarExistencias = { s.controlarExistenciasState.value = it },
    setStockActual = { s.stockActualState.value = it },
    setStockMinimo = { s.stockMinimoState.value = it },
    setPrecioPorUnidad = { s.precioPorUnidadState.value = it },
    setTelefonoPedidoWhatsapp = { s.telefonoPedidoWhatsappState.value = it },
    setDispensacionGratuita = { s.dispensacionGratuitaState.value = it },
    setOrigenReposicion = { s.origenReposicionState.value = it }
)

internal fun resetFichaPacienteWithState(s: MedicamentoFormState) = resetFichaPacienteAction(
    setEditingPatientId = { s.editingPatientIdState.value = it },
    setEditandoFichaPaciente = { s.editandoFichaPacienteState.value = it },
    setNombrePaciente = { s.nombrePacienteState.value = it },
    setApellidosPaciente = { s.apellidosPacienteState.value = it },
    setFechaNacimientoPaciente = { s.fechaNacimientoPacienteState.value = it },
    setEdadPaciente = { s.edadPacienteState.value = it },
    setPesoPaciente = { s.pesoPacienteState.value = it },
    setPesoUnidadPaciente = { s.pesoUnidadPacienteState.value = it },
    setEstaturaPaciente = { s.estaturaPacienteState.value = it },
    setEstaturaUnidadPaciente = { s.estaturaUnidadPacienteState.value = it },
    setSexoPaciente = { s.sexoPacienteState.value = it },
    setPaisPaciente = { s.paisPacienteState.value = it },
    setMonedaPaciente = { s.monedaPacienteState.value = it },
    setEnfermedadesPaciente = { s.enfermedadesPacienteState.value = it },
    setPrescripcionesPaciente = { s.prescripcionesPacienteState.value = it },
    setFotoPerfilPaciente = { s.fotoPerfilPacienteState.value = it }
)

internal fun resetCitaMedicaWithState(s: MedicamentoFormState) = resetCitaMedicaAction(
    setEditingAppointmentId = { s.editingAppointmentIdState.value = it },
    setCitaMedicaSeleccionadaId = { s.citaMedicaSeleccionadaIdState.value = it },
    setTituloCitaMedica = { s.tituloCitaMedicaState.value = it },
    setProfesionalCitaMedica = { s.profesionalCitaMedicaState.value = it },
    setLugarCitaMedica = { s.lugarCitaMedicaState.value = it },
    setNotasCitaMedica = { s.notasCitaMedicaState.value = it },
    setFechaCitaMedica = { s.fechaCitaMedicaState.value = it },
    setRecordatorioCitaMinutos = { s.recordatorioCitaMinutosState.value = it },
    setAlarmaCitaMedicaActiva = { s.alarmaCitaMedicaActivaState.value = it },
    setExpandedRecordatorioCita = { s.expandedRecordatorioCitaState.value = it }
)

internal fun resetMedicoHabitualWithState(s: MedicamentoFormState) = resetMedicoHabitualAction(
    setEditingPractitionerId = { s.editingPractitionerIdState.value = it },
    setNombreProfesional = { s.nombreProfesionalState.value = it },
    setEspecialidadProfesional = { s.especialidadProfesionalState.value = it }
)

internal fun cargarFichaPacienteWithState(
    s: MedicamentoFormState,
    profile: PatientProfile,
    calcularEdadDesdeNacimiento: (Long) -> Int
) = cargarFichaPacienteAction(
    profile = profile,
    calcularEdadDesdeNacimiento = calcularEdadDesdeNacimiento,
    setEditingPatientId = { s.editingPatientIdState.value = it },
    setNombrePaciente = { s.nombrePacienteState.value = it },
    setApellidosPaciente = { s.apellidosPacienteState.value = it },
    setFechaNacimientoPaciente = { s.fechaNacimientoPacienteState.value = it },
    setEdadPaciente = { s.edadPacienteState.value = it },
    setPesoPaciente = { s.pesoPacienteState.value = it },
    setPesoUnidadPaciente = { s.pesoUnidadPacienteState.value = it },
    setEstaturaPaciente = { s.estaturaPacienteState.value = it },
    setEstaturaUnidadPaciente = { s.estaturaUnidadPacienteState.value = it },
    setSexoPaciente = { s.sexoPacienteState.value = it },
    setPaisPaciente = { s.paisPacienteState.value = it },
    setMonedaPaciente = { s.monedaPacienteState.value = it },
    setEnfermedadesPaciente = { s.enfermedadesPacienteState.value = it },
    setPrescripcionesPaciente = { s.prescripcionesPacienteState.value = it },
    setFotoPerfilPaciente = { s.fotoPerfilPacienteState.value = it }
)

internal fun cargarMedicamentoEnFormularioWithState(
    s: MedicamentoFormState,
    medication: Medication
) = cargarMedicamentoEnFormularioAction(
    medication = medication,
    setMostrarFormulario = { s.mostrarFormularioState.value = it },
    setEditingMedicationId = { s.editingMedicationIdState.value = it },
    setNombre = { s.nombreState.value = it },
    setCantidad = { s.cantidadState.value = it },
    setAlarmaActiva = { s.alarmaActivaState.value = it },
    setEsCicloCorto = { s.esCicloCortoState.value = it },
    setEstaActivo = { s.estaActivoState.value = it },
    setFechaInicio = { s.fechaInicioState.value = it },
    setFechaFin = { s.fechaFinState.value = it },
    setHoraTomaSeleccionada = { s.horaTomaSeleccionadaState.value = it },
    setFormatoSeleccionado = { s.formatoSeleccionadoState.value = it },
    setFormaInsumoSeleccionada = { s.formaInsumoSeleccionadaState.value = it },
    setColorInsumoSeleccionado = { s.colorInsumoSeleccionadoState.value = it },
    setColorInsumo2Seleccionado = { s.colorInsumo2SeleccionadoState.value = it },
    setPresentacionPersistida = { s.presentacionPersistidaState.value = it },
    setConcentracionSeleccionada = { s.concentracionSeleccionadaState.value = it },
    setMostrarConcentracionLibre = { s.mostrarConcentracionLibreState.value = it },
    setTomaSeleccionada = { s.tomaSeleccionadaState.value = it },
    horasTomas = s.horasTomas,
    setCicloSeleccionado = { s.cicloSeleccionadoState.value = it },
    setSelectedMedication = { s.selectedMedicationState.value = it },
    setControlarExistencias = { s.controlarExistenciasState.value = it },
    setStockActual = { s.stockActualState.value = it },
    setStockMinimo = { s.stockMinimoState.value = it },
    setPrecioPorUnidad = { s.precioPorUnidadState.value = it },
    setTelefonoPedidoWhatsapp = { s.telefonoPedidoWhatsappState.value = it },
    setOrigenReposicion = { s.origenReposicionState.value = it },
    setDispensacionGratuita = { s.dispensacionGratuitaState.value = it }
)

internal fun cerrarPanelesSecundariosWithState(
    s: MedicamentoFormState,
    setMostrarListadoSignosPanel: (Boolean) -> Unit,
    mesesExpandidosSignos: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    fallAlertPanelState: androidx.compose.runtime.MutableState<Boolean>
) = cerrarPanelesSecundariosAction(
    setMostrarFichaPaciente = { s.mostrarFichaPacienteState.value = it },
    setMostrarFormularioInforme = { s.mostrarFormularioInformeState.value = it },
    setMostrarFormularioCitaMedica = { s.mostrarFormularioCitaMedicaState.value = it },
    setMostrarPanelPacientes = { s.mostrarPanelPacientesState.value = it },
    setMostrarPanelCitasMedicas = { s.mostrarPanelCitasMedicasState.value = it },
    setMostrarPanelProfesionales = { s.mostrarPanelProfesionalesState.value = it },
    setMostrarPanelInformes = { s.mostrarPanelInformesState.value = it },
    setMostrarListaInsumos = { s.mostrarListaInsumosState.value = it },
    setMostrarPanelBackups = { s.mostrarPanelBackupsState.value = it },
    setMostrarPanelPedidos = { s.mostrarPanelPedidosState.value = it },
    setMostrarPanelPodometro = { s.mostrarPanelPodometroState.value = it },
    setMostrarPanelConfiguracionAlertas = { s.mostrarPanelConfiguracionAlertasState.value = it },
    setMostrarPanelSignosVitales = { s.mostrarPanelSignosVitalesState.value = it },
    setMostrarListadoSignosPanel = setMostrarListadoSignosPanel,
    mesesExpandidosSignos = mesesExpandidosSignos,
    setMostrarPanelConfiguracionIa = { s.mostrarPanelConfiguracionIaState.value = it },
    setMostrarPanelAsistenteIa = { s.mostrarPanelAsistenteIaState.value = it },
    setMostrarPanelCicloMenstrual = { s.mostrarPanelCicloMenstrualState.value = it },
    setMostrarPanelEmbarazo = { s.mostrarPanelEmbarazoState.value = it },
    setMostrarPanelAnticonceptivos = { s.mostrarPanelAnticonceptivosState.value = it },
    setMostrarPanelPediatrico = { s.mostrarPanelPediatricoState.value = it },
    setMostrarPanelReporteClinico = { s.mostrarPanelReporteClinicoState.value = it },
    setMostrarPanelEstadisticas = { s.mostrarPanelEstadisticasState.value = it },
    setMostrarPanelDiario = { s.mostrarPanelDiarioState.value = it },
    fallAlertPanelState = fallAlertPanelState,
    setMostrarPanelVerificadorTomas = { s.mostrarPanelVerificadorTomasState.value = it },
    setMostrarPanelHidratacion = { s.mostrarPanelHidratacionState.value = it },
    setMostrarPanelSedentarismo = { s.mostrarPanelSedentarismoState.value = it },
    setMostrarPanelDentista = { s.mostrarPanelDentistaState.value = it },
    setMostrarFormulario = { s.mostrarFormularioState.value = it },
    setTomaPendienteDeEliminar = { s.tomaPendienteDeEliminarState.value = it }
)
