package com.carlos.controlmedicamentos

import com.carlos.controlmedicamentos.data.local.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ════════════════════════════════════════════════════════════════
//  MODELOS
// ════════════════════════════════════════════════════════════════

enum class TipoReporteClinico {
    SOLO_CICLOS, EMBARAZO_ACTIVO, EMBARAZO_INTERRUMPIDO, EMBARAZO_FINALIZADO
}

data class AlertaSalud(
    val fecha: Long,
    val tipo: String,
    val descripcion: String,
    val nivel: String  // "CRÍTICA" | "MODERADA" | "INFORMATIVA"
)

data class ResumenSignos(
    val fecha: Long,
    val sistolica: Int?,
    val diastolica: Int?,
    val latidos: Int?,
    val glucemia: Int?,
    val temperatura: Double?,
    val peso: Double?,
    val imc: Double?
)

data class ResumenActividad(
    val fecha: Long,
    val tipo: String,
    val pasos: Int,
    val distanciaKm: Double,
    val duracionMin: Long,
    val calorias: Int
)

data class ResumenMedicamento(
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val activo: Boolean,
    val totalTomas: Int
)

data class ReporteClinicoPayload(
    val fechaGeneracion: Long,
    val esMujer: Boolean,
    val tipo: TipoReporteClinico,
    val usuarioLabel: String,
    val edadUsuario: String,
    val rangoLabel: String,
    // Ciclos (solo mujer)
    val totalCiclos: Int,
    val duracionPromCiclo: Double?,
    val variabilidadCiclo: Double?,
    val duracionPromSangrado: Double?,
    val ciclos: List<CicloMenstrual>,
    // Embarazo (solo mujer)
    val semanasGest: Int?,
    val diasGest: Int?,
    val trimestre: String?,
    val fpp: Long?,
    val fechaFinEmbarazo: Long?,
    val tipoInterrupcion: String?,
    val metodoInterrupcion: String?,
    val notasInterrupcion: String?,
    val visitasPrenatales: List<VisitaPrenatal>,
    // Anticonceptivos (solo mujer)
    val ultimoMac: String?,
    val fechaSuspMac: Long?,
    // Metricas diarias (ambos)
    val signosVitales: List<ResumenSignos>,
    // Medicamentos (ambos)
    val medicamentos: List<ResumenMedicamento>,
    // Actividad física (ambos)
    val actividades: List<ResumenActividad>,
    // Alertas (ambos)
    val alertas: List<AlertaSalud>
)

// ════════════════════════════════════════════════════════════════
//  COMPILADOR
// ════════════════════════════════════════════════════════════════

suspend fun compilarReporteClinico(
    database: AppDatabase,
    paciente: PatientProfile?,
    mesesAtras: Int,
    incluirAlertas: Boolean,
    incluirAnticonceptivos: Boolean,
    incluirSignosVitales: Boolean,
    incluirMedicamentos: Boolean,
    incluirActividad: Boolean
): ReporteClinicoPayload {
    val patientId = paciente?.id ?: 0
    val esMujer = paciente?.sexo?.equals("Mujer", ignoreCase = true) == true
    val ahora = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { if (mesesAtras > 0) add(Calendar.MONTH, -mesesAtras) else set(2000, 0, 1) }
    val fechaLimite = cal.timeInMillis
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ── Estado embarazo (solo mujer) ──────────────────────────────
    var tipo = TipoReporteClinico.SOLO_CICLOS
    var embarazoRef: ControlEmbarazo? = null
    var ciclosEnRango = emptyList<CicloMenstrual>()
    var promCiclo: Double? = null; var variabilidad: Double? = null; var promSangrado: Double? = null
    var semanasGest: Int? = null; var diasGest: Int? = null; var trimestre: String? = null
    var visitas = emptyList<VisitaPrenatal>()
    var ultimoMac: String? = null; var fechaSuspMac: Long? = null

    if (esMujer && patientId > 0) {
        val embarazos = database.controlEmbarazoDao().observarTodos(patientId).first()
        val embarazoActivo = embarazos.firstOrNull { it.activo }
        val ultimoEmbarazo = embarazos.firstOrNull()
        tipo = when {
            embarazoActivo != null -> TipoReporteClinico.EMBARAZO_ACTIVO
            ultimoEmbarazo?.estadoEmbarazo == "INTERRUMPIDO" -> TipoReporteClinico.EMBARAZO_INTERRUMPIDO
            ultimoEmbarazo?.estadoEmbarazo == "FINALIZADO" -> TipoReporteClinico.EMBARAZO_FINALIZADO
            else -> TipoReporteClinico.SOLO_CICLOS
        }
        embarazoRef = embarazoActivo ?: ultimoEmbarazo

        // Ciclos
        val todosCiclos = database.cicloMenstrualDao().observarPorPaciente(patientId).first()
        ciclosEnRango = todosCiclos.filter { it.fechaInicio >= fechaLimite }
        val ords = ciclosEnRango.sortedBy { it.fechaInicio }
        val durs = mutableListOf<Int>()
        for (i in 1 until ords.size) {
            val d = ((ords[i].fechaInicio - ords[i-1].fechaInicio) / (24L*60*60*1000)).toInt()
            if (d in 15..60) durs.add(d)
        }
        promCiclo = if (durs.isNotEmpty()) durs.average() else null
        variabilidad = if (durs.size >= 2) { val m = durs.average(); Math.sqrt(durs.map { (it-m)*(it-m) }.average()) } else null
        val ds = ciclosEnRango.map { it.duracionDias }.filter { it > 0 }
        promSangrado = if (ds.isNotEmpty()) ds.average() else null

        // Gestación
        embarazoRef?.let { emb ->
            val refMs = if (emb.activo) ahora else (emb.fechaFin ?: ahora)
            val td = ((refMs - emb.fechaUltimaRegla) / (24L*60*60*1000)).toInt().coerceAtLeast(0)
            semanasGest = td / 7; diasGest = td % 7
            trimestre = when { (semanasGest?:0) <= 12 -> "1er trimestre"; (semanasGest?:0) <= 27 -> "2do trimestre"; else -> "3er trimestre" }
        }
        visitas = if (embarazoRef != null) database.visitaPrenatalDao().observarPorEmbarazo(embarazoRef.id).first() else emptyList()

        // Anticonceptivos
        if (incluirAnticonceptivos) {
            val macs = database.metodoAnticonceptivoDao().observarPorPaciente(patientId).first()
            val ref = macs.firstOrNull { it.activo } ?: macs.firstOrNull()
            ref?.let { ultimoMac = it.tipo; if (!it.activo) fechaSuspMac = it.fechaRegistro }
        }
    }

    // ── Metricas diarias (ambos) ─────────────────────────────────
    val signosResumen = if (incluirSignosVitales && patientId > 0) {
        database.signosVitalesDao().obtenerEnRango(patientId, fechaLimite, ahora)
            .sortedByDescending { it.fechaRegistro }
            .map { sv -> ResumenSignos(sv.fechaRegistro, sv.sistolica, sv.diastolica, sv.latidos, sv.glucemia, sv.temperatura, sv.peso, sv.imc) }
    } else emptyList()

    // ── Medicamentos (ambos) ──────────────────────────────────────────
    val insResumen = if (incluirMedicamentos && patientId > 0) {
        val meds = database.medicationDao().obtenerTodosPorPacienteLista(patientId)
        val intakesPac = database.medicationIntakeDao().observarPorPaciente(patientId).first()
            .filter { it.scheduledAt >= fechaLimite }
        meds.map { m ->
            val tomas = intakesPac.count { it.medicationId == m.id }
            ResumenMedicamento(m.nombre, m.dosis, "c/${m.frecuenciaHoras}h", m.estaActivo, tomas)
        }.sortedByDescending { it.activo }
    } else emptyList()

    // ── Actividad física (ambos) ──────────────────────────────────
    val actResumen = if (incluirActividad && patientId > 0) {
        database.physicalActivityDao().observarPorPaciente(patientId).first()
            .filter { it.fechaInicio >= fechaLimite }
            .sortedByDescending { it.fechaInicio }
            .map { a -> ResumenActividad(a.fechaInicio, a.tipo, a.pasos, a.distanciaMetros / 1000.0, a.duracionSegundos / 60, a.calorias) }
    } else emptyList()

    // ── Alertas (ambos) ───────────────────────────────────────────
    val alertas = mutableListOf<AlertaSalud>()
    if (incluirAlertas && patientId > 0) {
        val signos = database.signosVitalesDao().obtenerEnRango(patientId, fechaLimite, ahora)
        signos.forEach { sv ->
            val s = sv.sistolica; val d = sv.diastolica
            if (s != null && s >= 140) alertas.add(AlertaSalud(sv.fechaRegistro, "PRESIÓN ARTERIAL", "Sistólica $s/${d?:"?"} mmHg", "CRÍTICA"))
            else if (d != null && d >= 90) alertas.add(AlertaSalud(sv.fechaRegistro, "PRESIÓN ARTERIAL", "Diastólica ${s?:"?"}/$d mmHg", "MODERADA"))
            sv.temperatura?.let { if (it >= 38.0) alertas.add(AlertaSalud(sv.fechaRegistro, "TEMPERATURA", "Fiebre ${"%.1f".format(it)}°C", "MODERADA")) }
            sv.glucemia?.let { if (it > 140) alertas.add(AlertaSalud(sv.fechaRegistro, "GLUCEMIA", "Glucemia elevada: $it mg/dL", "MODERADA")) }
        }
        if (esMujer) {
            ciclosEnRango.forEach { ciclo ->
                database.registroDiarioCicloDao().obtenerPorCiclo(ciclo.id)
                    .filter { it.tipoSintoma == "SANGRADO" && (it.intensidad ?: 0) >= 4 }
                    .forEach { r -> alertas.add(AlertaSalud(r.fecha, "SANGRADO", "Sangrado muy abundante registrado", "MODERADA")) }
            }
        }
        alertas.sortWith(compareByDescending { if (it.nivel == "CRÍTICA") 1 else 0 })
    }

    val rangoLabel = if (mesesAtras == 0) "Todo el historial"
    else "Últimos $mesesAtras ${if (mesesAtras == 1) "mes" else "meses"} (desde ${sdf.format(Date(fechaLimite))})"
    val nombrePac = paciente?.let { "${it.nombre} ${it.apellidos}".trim() }.orEmpty().ifBlank { "Usuario no especificado" }
    val edadPac = paciente?.let {
        val dobMs = it.fechaNacimiento
        if (dobMs > 0L) { val y = ((ahora - dobMs) / (365.25*24*60*60*1000)).toInt(); "$y años" }
        else it.edad.ifBlank { "No registrada" }
    } ?: "No registrada"

    return ReporteClinicoPayload(
        fechaGeneracion = ahora, esMujer = esMujer, tipo = tipo,
        usuarioLabel = nombrePac, edadUsuario = edadPac, rangoLabel = rangoLabel,
        totalCiclos = ciclosEnRango.size, duracionPromCiclo = promCiclo,
        variabilidadCiclo = variabilidad, duracionPromSangrado = promSangrado,
        ciclos = ciclosEnRango.sortedByDescending { it.fechaInicio },
        semanasGest = semanasGest, diasGest = diasGest, trimestre = trimestre,
        fpp = embarazoRef?.fechaProbableParto, fechaFinEmbarazo = embarazoRef?.fechaFin,
        tipoInterrupcion = embarazoRef?.tipoInterrupcion, metodoInterrupcion = embarazoRef?.metodoInterrupcion,
        notasInterrupcion = embarazoRef?.notasInterrupcion, visitasPrenatales = visitas,
        ultimoMac = ultimoMac, fechaSuspMac = fechaSuspMac,
        signosVitales = signosResumen, medicamentos = insResumen,
        actividades = actResumen, alertas = alertas
    )
}

// ════════════════════════════════════════════════════════════════
//  GENERADOR DOCX
// ════════════════════════════════════════════════════════════════

fun escribirReporteClinicoDocx(output: java.io.OutputStream, r: ReporteClinicoPayload) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfDt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    var seccion = 0
    fun numSec() = "${++seccion}."

    fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    fun parrafo(texto: String, bold: Boolean = false, size: Int = 20, color: String = "000000", espacio: Int = 120): String {
        val b = if (bold) "<w:b/>" else ""
        return "<w:p><w:pPr><w:spacing w:after=\"$espacio\"/></w:pPr><w:r><w:rPr>$b<w:sz w:val=\"$size\"/><w:color w:val=\"$color\"/></w:rPr><w:t xml:space=\"preserve\">${esc(texto)}</w:t></w:r></w:p>"
    }
    fun titulo(t: String) = parrafo(t, bold = true, size = 28, color = "4A148C", espacio = 100)
    fun sub(t: String) = parrafo("${numSec()} $t", bold = true, size = 22, color = "6A1B9A", espacio = 80)
    fun linea(t: String) = parrafo(t, size = 19, color = "212121")
    fun lineaGris(t: String) = parrafo(t, size = 18, color = "757575")
    fun alerta(t: String, nivel: String): String {
        val col = if (nivel == "CRÍTICA") "B71C1C" else "E65100"
        return parrafo("⚠ $t", bold = true, size = 19, color = col, espacio = 60)
    }
    fun sep() = "<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"1\" w:color=\"CE93D8\"/></w:pBdr><w:spacing w:after=\"80\"/></w:pPr></w:p>"

    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>")

    val titulo_ = "REPORTE CLÍNICO MÉDICO"
    sb.append(titulo(titulo_))
    sb.append(lineaGris("Generado: ${sdfDt.format(Date(r.fechaGeneracion))}"))
    sb.append(sep())

    // I. Datos del paciente
    sb.append(sub("DATOS DEL PACIENTE"))
    sb.append(linea("Paciente: ${r.usuarioLabel}"))
    sb.append(linea("Edad: ${r.edadUsuario}"))
    sb.append(linea("Sexo: ${if (r.esMujer) "Femenino" else "Masculino"}"))
    sb.append(linea("Período analizado: ${r.rangoLabel}"))
    sb.append(sep())

    // II. Resumen ejecutivo
    sb.append(sub("RESUMEN EJECUTIVO"))
    if (r.esMujer) {
        val tipoLabel = when (r.tipo) {
            TipoReporteClinico.SOLO_CICLOS -> "Control de ciclos menstruales"
            TipoReporteClinico.EMBARAZO_ACTIVO -> "Embarazo en curso"
            TipoReporteClinico.EMBARAZO_INTERRUMPIDO -> "Embarazo interrumpido"
            TipoReporteClinico.EMBARAZO_FINALIZADO -> "Embarazo finalizado (parto)"
        }
        sb.append(linea("Estado ginecológico: $tipoLabel"))
        if (r.tipo == TipoReporteClinico.SOLO_CICLOS) {
            sb.append(linea("Ciclos en el período: ${r.totalCiclos}"))
            r.duracionPromCiclo?.let { sb.append(linea("Duración promedio ciclo: ${"%.1f".format(it)} días")) }
            r.variabilidadCiclo?.let { sb.append(linea("Variabilidad (±): ${"%.1f".format(it)} días${if (it > 5) "  ▶ posible irregularidad" else ""}")) }
            r.duracionPromSangrado?.let { sb.append(linea("Duración promedio sangrado: ${"%.1f".format(it)} días")) }
        } else {
            r.semanasGest?.let { sb.append(linea("Edad gestacional: $it sem. ${r.diasGest ?: 0} días  (${r.trimestre ?: ""})")) }
            r.fpp?.let { sb.append(linea("Fecha probable de parto (FPP): ${sdf.format(Date(it))}")) }
            r.fechaFinEmbarazo?.let { sb.append(linea("Fecha de finalización: ${sdf.format(Date(it))}")) }
            if (r.tipo == TipoReporteClinico.EMBARAZO_INTERRUMPIDO) {
                r.tipoInterrupcion?.let { sb.append(linea("Tipo de interrupción: $it")) }
                r.metodoInterrupcion?.let { sb.append(linea("Método: $it")) }
                r.notasInterrupcion?.takeIf { it.isNotBlank() }?.let { sb.append(linea("Notas: $it")) }
            }
        }
        r.ultimoMac?.let {
            sb.append(linea("Último anticonceptivo: $it"))
            r.fechaSuspMac?.let { f -> sb.append(linea("Suspendido aprox.: ${sdf.format(Date(f))}")) }
        }
    }
    if (r.signosVitales.isNotEmpty()) {
        val ultimo = r.signosVitales.first()
        ultimo.sistolica?.let { sb.append(linea("Última PA: $it/${ultimo.diastolica ?: "?"} mmHg  (${sdf.format(Date(ultimo.fecha))})")) }
        ultimo.peso?.let { sb.append(linea("Último peso: ${"%.1f".format(it)} kg${ultimo.imc?.let { i -> "  IMC: ${"%.1f".format(i)}" } ?: ""}")) }
        ultimo.glucemia?.let { sb.append(linea("Última glucemia: $it mg/dL")) }
    }
    if (r.medicamentos.isNotEmpty()) {
        val activos = r.medicamentos.count { it.activo }
        sb.append(linea("Medicamentos activos: $activos de ${r.medicamentos.size} registrados"))
    }
    if (r.actividades.isNotEmpty()) {
        val totalPasos = r.actividades.sumOf { it.pasos }
        val totalKm = r.actividades.sumOf { it.distanciaKm }
        sb.append(linea("Actividad física: ${r.actividades.size} sesiones  |  $totalPasos pasos totales  |  ${"%.1f".format(totalKm)} km"))
    }
    sb.append(sep())

    // III. Alertas
    if (r.alertas.isNotEmpty()) {
        sb.append(sub("ALERTAS DE SALUD (${r.alertas.size})"))
        r.alertas.forEach { a -> sb.append(alerta("[${a.tipo}] ${sdf.format(Date(a.fecha))}: ${a.descripcion}", a.nivel)) }
        sb.append(sep())
    }

    // IV. Metricas diarias
    if (r.signosVitales.isNotEmpty()) {
        sb.append(sub("SIGNOS VITALES (${r.signosVitales.size} registros)"))
        r.signosVitales.forEach { sv ->
            val partes = mutableListOf("${sdf.format(Date(sv.fecha))}")
            sv.sistolica?.let { partes.add("PA: $it/${sv.diastolica?:"?"} mmHg") }
            sv.latidos?.let { partes.add("FC: $it lpm") }
            sv.glucemia?.let { partes.add("Gluc: $it mg/dL") }
            sv.temperatura?.let { partes.add("Temp: ${"%.1f".format(it)}°C") }
            sv.peso?.let { partes.add("Peso: ${"%.1f".format(it)} kg") }
            sv.imc?.let { partes.add("IMC: ${"%.1f".format(it)}") }
            sb.append(linea("• ${partes.joinToString("  |  ")}"))
        }
        sb.append(sep())
    }

    // V. Medicamentos
    if (r.medicamentos.isNotEmpty()) {
        sb.append(sub("ESTADÍSTICAS POR MEDICAMENTO (${r.medicamentos.size})"))
        r.medicamentos.forEach { m ->
            val estado = if (m.activo) "✓ Activo" else "✗ Inactivo"
            val dosis = if (m.dosis.isNotBlank()) "  ${m.dosis}" else ""
            val frec = if (m.frecuencia.isNotBlank()) "  ${m.frecuencia}" else ""
            val tomas = if (m.totalTomas > 0) "  (${m.totalTomas} tomas en el período)" else ""
            sb.append(linea("• ${m.nombre}$dosis$frec  [$estado]$tomas"))
        }
        sb.append(sep())
    }

    // VI. Actividad física
    if (r.actividades.isNotEmpty()) {
        sb.append(sub("ACTIVIDAD FÍSICA (${r.actividades.size} sesiones)"))
        r.actividades.forEach { a ->
            sb.append(linea("• ${sdf.format(Date(a.fecha))}  ${a.tipo.replaceFirstChar { it.uppercase() }}  ${a.pasos} pasos  ${"%.2f".format(a.distanciaKm)} km  ${a.duracionMin} min  ${a.calorias} kcal"))
        }
        sb.append(sep())
    }

    // VII. Visitas prenatales (solo mujer)
    if (r.esMujer && r.visitasPrenatales.isNotEmpty()) {
        sb.append(sub("VISITAS PRENATALES (${r.visitasPrenatales.size})"))
        r.visitasPrenatales.sortedBy { it.fecha }.forEach { v ->
            sb.append(linea("• ${sdf.format(Date(v.fecha))}  Sem ${v.semanasGestacion}${v.contactoOMS?.let { "  Contacto OMS $it" } ?: ""}"))
            if (v.presionArterial.isNotBlank()) sb.append(lineaGris("  PA: ${v.presionArterial} mmHg"))
            v.peso?.let { sb.append(lineaGris("  Peso: ${"%.1f".format(it)} kg")) }
            v.frecuenciaCardiacaFetal?.let { sb.append(lineaGris("  FCF: $it lpm")) }
            if (v.observaciones.isNotBlank()) sb.append(lineaGris("  Obs: ${v.observaciones}"))
        }
        sb.append(sep())
    }

    // VIII. Historial de ciclos menstruales (solo mujer)
    if (r.esMujer && r.ciclos.isNotEmpty()) {
        sb.append(sub("HISTORIAL DE CICLOS MENSTRUALES (${r.ciclos.size})"))
        r.ciclos.forEach { c ->
            val ini = sdf.format(Date(c.fechaInicio))
            val finEst = sdf.format(Date(c.fechaInicio + c.duracionDias.toLong() * 24 * 60 * 60 * 1000))
            sb.append(linea("• Inicio: $ini  Fin est.: $finEst  Sangrado: ${c.duracionDias} d  Ciclo: ${c.duracionCicloDias} d"))
        }
        sb.append(sep())
    }

    sb.append(lineaGris("Reporte generado automáticamente por Control medicamentos. Este documento es de uso informativo y no sustituye la consulta médica profesional."))

    sb.append("<w:sectPr><w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr>")
    sb.append("</w:body></w:document>")

    ZipOutputStream(output).use { zip ->
        fun put(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""")
        put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""")
        put("word/_rels/document.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>""")
        put("word/document.xml", sb.toString())
    }
}




