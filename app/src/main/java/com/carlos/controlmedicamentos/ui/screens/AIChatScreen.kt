package com.carlos.controlmedicamentos.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ModuloDirectorio(
    val id: String,
    val titulo: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val descripcionCorta: String,
    val explicacionDetallada: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    perfilActivoNombre: String? = null
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var mostrarDirectorio by remember { mutableStateOf(true) }
    var moduloActivo by remember { mutableStateOf<ModuloDirectorio?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // TTS State
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsEnabled by remember { mutableStateOf(true) }
    var lastSpokenMessage by remember { mutableStateOf<String?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var speechRate by remember { mutableStateOf(0.85f) }
    var showTtsControls by remember { mutableStateOf(false) }
    var utteranceId by remember { mutableStateOf<String?>(null) }

    // Initialize TTS
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { ttsInstance ->
                    val result = ttsInstance.setLanguage(Locale("es", "ES"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsInstance.setLanguage(Locale.US)
                    }
                    ttsInstance.setSpeechRate(speechRate)
                    
                    // Set up utterance progress listener
                    ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                            isPaused = false
                        }
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            isPaused = false
                        }
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            isPaused = false
                        }
                    })
                    
                    ttsReady = true
                }
            }
        }
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // Update speech rate when changed
    LaunchedEffect(speechRate) {
        tts?.setSpeechRate(speechRate)
    }

    // Function to speak text
    fun speakText(text: String) {
        if (ttsEnabled && ttsReady && tts != null) {
            val cleanText = text
                .replace(Regex("[📋📦✅⏰📅📊🤰🌙👶🛒📝💾•]"), "")
                .replace(Regex("\\n+"), ". ")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            if (cleanText.isNotEmpty()) {
                lastSpokenMessage = cleanText
                val id = "msg_${System.currentTimeMillis()}"
                utteranceId = id
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
    }

    // Stop speaking
    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        isPaused = false
    }

    // Toggle pause/resume (using stop and restart for simplicity)
    fun togglePauseResume() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
            isPaused = true
        } else if (isPaused && lastSpokenMessage != null) {
            speakText(lastSpokenMessage!!)
            isPaused = false
        }
    }

    val modulos = remember {
        listOf(
            ModuloDirectorio(
                id = "perfiles",
                titulo = "Perfiles de Usuario",
                icono = Icons.Default.Person,
                descripcionCorta = "Gestiona usuarios y sus datos",
                explicacionDetallada = """📋 PERFILES DE USUARIO

Que es:
Es el modulo principal donde registras a cada persona que usara la aplicacion. Puedes crear perfiles ilimitados para ti, familiares o personas a tu cargo.

Como usarlo:
1. Toca el boton '+' para crear un perfil nuevo
2. Ingresa nombre, fecha de nacimiento y contacto
3. Agrega una foto si deseas
4. El perfil queda activo automáticamente

Para que sirve:
• Cada perfil tiene su propio inventario de rutinas
• Permite separar información por persona
• Facilita el seguimiento individual
• Puedes cambiar de perfil en cualquier momento desde el menu

Consejo: Activa el perfil correcto antes de registrar cualquier dato."""
            ),
            ModuloDirectorio(
                id = "inventario",
                titulo = "Inventario",
                icono = Icons.Default.Medication,
                descripcionCorta = "Registra y gestiona tus artículos",
                explicacionDetallada = """📦 INVENTARIO

Que es:
Es tu lista personal de artículos, productos o rutinas que necesitas controlar. Puedes registrar cualquier cosa que consumas regularmente.

Como usarlo:
1. Selecciona un perfil activo primero
2. Toca '+' para agregar nuevo artículo
3. Ingresa nombre, dosis/presentacion y cantidad
4. Configura stock y fecha de vencimiento si aplica
5. Agrega fotos para identificarlo fácilmente

Funciones especiales:
• Busqueda rápida por nombre
• Filtrar por perfil
• Alertas de stock bajo
• Control de vencimientos
• Historial de consumo

Para que sirve:
Mantener un control organizado de todo lo que necesitas, cuando lo necesitas y cuanto te queda."""
            ),
            ModuloDirectorio(
                id = "tomas",
                titulo = "Registro de Consumo",
                icono = Icons.Default.CheckCircle,
                descripcionCorta = "Registra cuando usas tus artículos",
                explicacionDetallada = """✅ REGISTRO DE CONSUMO (TOMAS)

Que es:
Es donde marcas cada vez que consumes o usas un artículo de tu inventario. Crea un historial completo de tus rutinas.

Como usarlo:
1. Ve a la seccion de tomas
2. Selecciona el artículo que usaste
3. Marca la fecha y hora
4. Puedes agregar notas (ej: "con alimentos")
5. Confirma la toma

Funciones:
• Lista de tomas del dia
• Calendario histórico
• Filtrar por artículo o periodo
• Exportar registros
• Estadísticas de uso

Para que sirve:
• Llevar control de cumplimiento
• Detectar olvidos o excesos
• Generar reportes para compartir
• Analizar patrones de uso
• Recordar ultima vez que usaste algo"""
            ),
            ModuloDirectorio(
                id = "alarmas",
                titulo = "Recordatorios y Alarmas",
                icono = Icons.Default.Notifications,
                descripcionCorta = "Configura alertas personalizadas",
                explicacionDetallada = """⏰ RECORDATORIOS Y ALARMAS

Que es:
El sistema de notificaciónes que te avisa cuando es momento de realizar una rutina o tomar algo del inventario.

Como configurar:
1. Ve a un artículo del inventario
2. Toca 'Configurar recordatorio'
3. Elige frecuencia: Diaria, cada X horas, semanal, etc.
4. Establece la hora exacta
5. Guarda la alarma

Tipos de alarmas:
• Diarias (misma hora todos los dias)
• Intervalos (cada 8 horas, 12 horas, etc)
• Específicos (lunes y jueves a las 9am)
• Unicas (solo una fecha específica)

Funciones:
• Sonido de alarma personalizable
• Posponer (snooze)
• Desactivar temporalmente
• Lista de próximas alarmas
• Historial de alarmas sonadas

Nota: La app debe tener permiso de notificaciónes activado en Android."""
            ),
            ModuloDirectorio(
                id = "citas",
                titulo = "Agenda y Contactos",
                icono = Icons.Default.CalendarToday,
                descripcionCorta = "Organiza reuniones y contactos",
                explicacionDetallada = """📅 AGENDA Y CONTACTOS

Que es:
Tu calendario personal para registrar reuniones, visitas, citas o cualquier evento importante relacionado con tus rutinas.

Como usarlo:
1. Toca '+' para nueva cita
2. Selecciona fecha y hora
3. Escribe nombre del contacto o lugar
4. Agrega motivo/descripcion
5. Guarda la cita

Funciones:
• Calendario mensual visual
• Lista por proximidad
• Recordatorios antes de la cita
• Marcar como completada
• Historial de citas pasadas
• Agregar notas despues de la visita

Sincronización:
Puedes sincronizar con Google Calendar para ver todo en un solo lugar, pero el registro principal se mantiene en la app.

Tip: Usa esto para registrar visitas a profesionales, reuniones importantes o cualquier evento que quieras recordar."""
            ),
            ModuloDirectorio(
                id = "metricas",
                titulo = "Metricas Diarias",
                icono = Icons.Default.MonitorHeart,
                descripcionCorta = "Registra tus valores diarios",
                explicacionDetallada = """📊 METRICAS DIARIAS

Que es:
Modulo para registrar valores numericos de tu estado fisico: presion, frecuencia cardiaca, temperatura, peso, etc.

Como usarlo:
1. Selecciona el tipo de metrica
2. Ingresa el valor numerico
3. Puedes agregar notas de contexto
4. La fecha se registra automáticamente

Valores que puedes registrar:
• Presion (sistolica/diastolica)
• Frecuencia cardiaca (latidos/min)
• Temperatura corporal
• Frecuencia respiratoria
• Saturacion de oxigeno
• Peso y estatura
• Glucemia

Análisis:
La app genera:
• Gráficos de tendencia
• Promedios por semana/mes
• Alertas si valores estan fuera de rango
• Comparativas periodo a periodo

Para que sirve:
Llevar un seguimiento continuo de tus indicadores para detectar cambios o patrones a lo largo del tiempo."""
            ),
            ModuloDirectorio(
                id = "embarazo",
                titulo = "Control de Gestacion",
                icono = Icons.Default.PregnantWoman,
                descripcionCorta = "Seguimiento de embarazo",
                explicacionDetallada = """🤰 CONTROL DE GESTACION

Que es:
Modulo especializado para seguimiento de embarazo, desde la confirmacion hasta el parto.

Como usarlo:
1. Activa el modo embarazo en un perfil
2. Registra fecha de ultima regla o fecha estimada
3. La app calcula automáticamente las semanas
4. Registra cada visita de control
5. Al finalizar, registra los datos del bebe

Funciones:
• Calculadora de semanas de gestacion
• Registro de visitas de control
• Seguimiento de peso materno
• Control de presion
• Alertas de citas programadas
• Registro de datos del recien nacido al parto
• Generacion automática de perfil infantil

Datos que registra:
• Fecha probable de parto
• Peso y presion por visita
• Notas de cada control
• Examenes y resultados
• Síntomas o eventos relevantes

Al finalizar:
La app crea automáticamente perfiles pediátricos para los bebes registrados."""
            ),
            ModuloDirectorio(
                id = "ciclo",
                titulo = "Ciclo Menstrual",
                icono = Icons.Default.DateRange,
                descripcionCorta = "Seguimiento del periodo",
                explicacionDetallada = """🌙 CICLO MENSTRUAL

Que es:
Registro de tu periodo menstrual para predicción, análisis y seguimiento de regularidad.

Como usarlo:
1. Toca el dia de inicio de tu regla
2. Marca cuando termina
3. Registra intensidad (leve/moderada/abundante)
4. Agrega síntomas si deseas
5. La app calcula automáticamente

Predicciónes:
La app calcula:
• Duración del ciclo actual
• Dia estimado de siguiente periodo
• Dias de fertilidad (opcional)
• Fase del ciclo actual

Estadísticas:
• Duración promedio de ciclos
• Regularidad (variacion entre ciclos)
• Síntomas recurrentes
• Dias promedio de duración

Registro diario:
Durante el ciclo puedes registrar:
• Síntomas (dolor, cambios de humor, etc)
• Flujo y caracteristicas
• Eventos relevantes
• Notas personales

Para que sirve:
Predecir tu próxima regla, identificar irregularidades y mantener un historial completo de tu bienestar reproductivo."""
            ),
            ModuloDirectorio(
                id = "infantil",
                titulo = "Perfiles Infantiles",
                icono = Icons.Default.ChildCare,
                descripcionCorta = "Control de crecimiento de ninos",
                explicacionDetallada = """👶 PERFILES INFANTILES

Que es:
Modulo para seguimiento de crecimiento y desarrollo de bebes y ninos, incluyendo calendario de vacunación.

Como se crea:
• Automáticamente al registrar un parto
• Manualmente desde el menu infantil
• Importando datos de control pediátrico

Datos del perfil:
• Nombre y fecha de nacimiento
• Peso y talla al nacer
• Tipo de parto y semanas de gestacion
• Notas especiales

Seguimiento de crecimiento:
• Registro de peso y estatura periódica
• Gráficos de percentiles
• Comparativa con estandares WHO
• Alertas de controles pendientes

Inmunizaciónes:
• Calendario nacional de vacunación
• Registro de vacunas aplicadas
• Fechas de próximas dosis
• Alertas de vacunas pendientes
• Historial completo de inmunización

Registros personales:
• Afecciones o alergias
• Controles periódicos
• Notas del pediatra
• Eventos relevantes

Ideal para:
Padres que quieren llevar un control organizado del crecimiento y bienestar de sus hijos."""
            ),
            ModuloDirectorio(
                id = "compras",
                titulo = "Lista de Compras",
                icono = Icons.Default.ShoppingCart,
                descripcionCorta = "Organiza tus compras pendientes",
                explicacionDetallada = """🛒 LISTA DE COMPRAS

Que es:
Tu carrito de compras pendientes. Agrega artículos que necesitas comprar y lleva control de precios.

Como usarlo:
1. Ve a Inventario > Carrito de Compras
2. Toca '+' para agregar artículo
3. Escribe nombre y cantidad necesaria
4. Puedes agregar precio estimado
5. Marca como comprado cuando lo adquieras

Funciones:
• Lista organizada por prioridad
• Autocompletado con artículos del historial
• Comparacion de precios entre compras
• Registro de lugar de compra
• Total estimado de la lista
• Historial de compras pasadas
• Reporte mensual de gastos

Al comprar:
Cuando marcas como comprado, puedes:
• Actualizar precio real
• Registrar fecha de compra
• Agregar lugar donde compraste
• Agregar al inventario automáticamente

Consejo: Usa esto para no olvidar lo que necesitas y comparar precios entre diferentes compras."""
            ),
            ModuloDirectorio(
                id = "diario",
                titulo = "Diario Personal",
                icono = Icons.Default.Note,
                descripcionCorta = "Notas y fotos diarias",
                explicacionDetallada = """📝 DIARIO PERSONAL

Que es:
Tu espacio privado para escribir notas, registrar eventos o adjuntar fotos importantes del dia a dia.

Como usarlo:
1. Ve al modulo Diario
2. Toca '+' para nueva entrada
3. Escribe tus notas del dia
4. Puedes adjuntar fotos (hasta 10 por entrada)
5. Guarda la entrada

Funciones:
• Entradas con fecha automática
• Adjunta fotos desde camara o galería
• Buscador por palabras clave
• Filtrar por fechas
• Editar entradas existentes
• Exportar diario completo
• Respaldo en la nube

Usos comunes:
• Registrar síntomas o como te sientes
• Guardar fotos de documentos importantes
• Notas de citas o controles
• Recordatorios personales
• Diario de embarazo
• Seguimiento de recuperacion

Privacidad:
Tus notas son privadas y se almacenan localmente en tu dispositivo. Puedes activar respaldo opcional."""
            ),
            ModuloDirectorio(
                id = "respaldo",
                titulo = "Respaldo y Restauracion",
                icono = Icons.Default.Backup,
                descripcionCorta = "Copia de seguridad de tus datos",
                explicacionDetallada = """💾 RESPALDO Y RESTAURACION

Que es:
Sistema para crear copias de seguridad de todos tus datos y restaurarlos cuando necesites.

Como crear backup:
1. Ve a Configuración > Respaldo
2. Toca 'Crear copia de seguridad'
3. Elige donde guardar (almacenamiento, WhatsApp, email, etc)
4. Se genera archivo .cmed con todos tus datos

Que incluye el respaldo:
• Todos los perfiles y sus fotos
• Inventario completo
• Registro de tomas e historial
• Alarmas configuradas
• Citas y contactos
• Metricas registradas
• Diario con fotos
• Configuraciónes de la app

Como restaurar:
1. Ve a Configuración > Restaurar
2. Selecciona archivo .cmed
3. Verifica el contenido antes de confirmar
4. Toca 'Restaurar datos'
5. Los datos se integran con los actuales

Precaución:
• Los backups son compatibles entre versiones
• Guarda tus archivos .cmed en lugar seguro
• Puedes crear backups programados automáticos
• El archivo puede ser grande si tienes muchas fotos

Recomendación: Crea un backup al menos una vez por mes."""
            ),
            ModuloDirectorio(
                id = "vacunas",
                titulo = "Vacunas",
                icono = Icons.Default.Vaccines,
                descripcionCorta = "Registro de vacunas",
                explicacionDetallada = """💉 VACUNAS

Que es:
Modulo para registrar y hacer seguimiento de vacunas, tanto para adultos como para perfiles infantiles.

Como usarlo:
1. Selecciona un perfil activo
2. Ve a Vacunas desde el menu hamburguesa
3. Toca '+' para registrar una nueva vacuna
4. Ingresa nombre, fecha de aplicacion y próxima dosis
5. Guarda el registro

Funciones:
• Calendario de vacunación para perfiles infantiles
• Alertas de próximas dosis
• Historial completo de vacunas aplicadas
• Registro de lugar de aplicacion
• Notas adicionales

Para que sirve:
Mantener un control actualizado de la inmunización y no perder fechas de refuerzos."""
            ),
            ModuloDirectorio(
                id = "galería",
                titulo = "Galería",
                icono = Icons.Default.PhotoLibrary,
                descripcionCorta = "Fotos y documentos adjuntos",
                explicacionDetallada = """📷 GALERIA

Que es:
Espacio centralizado donde puedes ver todas las fotos y documentos adjuntos a perfiles, registros, citas, visitas o diario.

Como usarlo:
1. Selecciona un perfil activo
2. Abre Galería desde el menu hamburguesa
3. Explora las imagenes vinculadas a ese perfil

Funciones:
• Visualizacion de fotos adjuntas
• Miniaturas organizadas por fecha
• Acceso rápido a la fuente del registro
• Compartir imagenes

Para que sirve:
Tener a mano documentos, recetas, resultados, fotos de visitas o cualquier imagen relacionada con tus registros."""
            ),
            ModuloDirectorio(
                id = "actividad",
                titulo = "Actividad Fisica",
                icono = Icons.Default.DirectionsRun,
                descripcionCorta = "Seguimiento de ejercicio",
                explicacionDetallada = """🏃 ACTIVIDAD FISICA

Que es:
Modulo para registrar y seguir tu actividad fisica diaria, como pasos, distancia, calorias y tiempo.

Como usarlo:
1. Abre Actividad Fisica desde el menu hamburguesa
2. Elige el tipo de actividad: caminar, correr, ciclismo, etc.
3. Registra duración, distancia o pasos
4. Guarda el registro

Funciones:
• Contador de pasos
• Registro de distancia y calorias
• Historial de actividades
• Estadísticas semanales y mensuales
• Objetivos diarios

Para que sirve:
Llevar un control de tu actividad fisica y mantener habitos saludables."""
            ),
            ModuloDirectorio(
                id = "anticonceptivos",
                titulo = "Anticonceptivos",
                icono = Icons.Default.Medication,
                descripcionCorta = "Seguimiento de metodos anticonceptivos",
                explicacionDetallada = """💊 ANTICONCEPTIVOS

Que es:
Modulo para registrar y seguir metodos anticonceptivos, con recordatorios de tomas y control de suministro.

Como usarlo:
1. Selecciona un perfil de mujer activo
2. Abre Anticonceptivos desde el menu hamburguesa
3. Registra el metodo activo y la fecha de inicio
4. Configura recordatorios si aplica

Funciones:
• Recordatorios de tomas diarias
• Control de suministro
• Historial de uso
• Alertas de próxima dosis

Para que sirve:
No olvidar tomas de anticonceptivos y llevar un control del metodo utilizado."""
            ),
            ModuloDirectorio(
                id = "exportar",
                titulo = "Exportar Resumen",
                icono = Icons.Default.Download,
                descripcionCorta = "Genera un resumen en Word",
                explicacionDetallada = """📄 EXPORTAR RESUMEN

Que es:
Herramienta para generar un documento Word (.docx) con un resumen de la información del perfil activo.

Como usarlo:
1. Selecciona un perfil activo
2. Ve a Exportar Resumen desde el menu hamburguesa
3. Elige que secciones incluir: metricas, inventario, citas, etc.
4. Toca 'Exportar'
5. Guarda o comparte el archivo .docx

Funciones:
• Seleccion de secciones a incluir
• Formato Word editable
• Listo para compartir o imprimir

Para que sirve:
Llevar un resumen completo de la información a una consulta, reunion o para tenerlo en archivo."""
            ),
            ModuloDirectorio(
                id = "estadísticas",
                titulo = "Estadísticas de Uso",
                icono = Icons.Default.BarChart,
                descripcionCorta = "Gráficos e historial de consumo",
                explicacionDetallada = """📊 ESTADISTICAS DE USO

Que es:
Panel con gráficos e informes sobre el consumo de artículos del inventario.

Como usarlo:
1. Selecciona un perfil activo
2. Abre Estadísticas de Uso desde el menu hamburguesa
3. Explora los gráficos por periodo

Funciones:
• Gráfico de consumo por artículo
• Filtrado por fechas
• Historial de tomas
• Tendencias de uso

Para que sirve:
Analizar patrones de consumo, detectar olvidos frecuentes o verificar el uso correcto de artículos.

Tip: Mantener presionado el item del menu abre el verificador de tomas pasadas, util para marcar olvidos."""
            ),
            ModuloDirectorio(
                id = "verificador",
                titulo = "Verificador de Tomas Pasadas",
                icono = Icons.Default.History,
                descripcionCorta = "Revisa y registra tomas no confirmadas",
                explicacionDetallada = """VERIFICADOR DE TOMAS PASADAS

Que es:
Herramienta que muestra tomas que debieron realizarse en los ultimos 30 dias y aun no han sido confirmadas en el sistema.

Como acceder:
1. Manten presionado el icono de Estadisticas de Uso en el menu hamburguesa

Que muestra:
â€¢ Lista de tomas no confirmadas de los ultimos 30 dias
â€¢ Nombre del medicamento, fecha y hora programada
â€¢ Dosis correspondiente por toma

Acciones disponibles:
â€¢ Tomado: registra la toma como realizada (con hora real o personalizada)
â€¢ No tomado: registra que no se tomo
â€¢ Omitir: deja la toma sin registrar

Para que sirve:
Mantener el historial completo de cumplimiento, detectar olvidos y corregirlos con la hora real sin alterar registros pasados."""
            ),
            ModuloDirectorio(
                id = "suspendidos",
                titulo = "Medicamentos Suspendidos",
                icono = Icons.Default.PauseCircle,
                descripcionCorta = "Gestion de medicamentos inactivos",
                explicacionDetallada = """MEDICAMENTOS SUSPENDIDOS

Que es:
Los medicamentos pueden estar Activos o Suspendidos. Los suspendidos no aparecen en el escritorio ni generan alarmas, pero se conservan en el inventario con todo su historial.

Como suspender:
1. Ve al inventario
2. Abre el medicamento que deseas suspender
3. Cambia su estado a Suspendido
4. Desaparecera del escritorio automaticamente

Donde verlos:
â€¢ Solo aparecen en la seccion Inventario
â€¢ No aparecen en el escritorio ni en las alarmas del dia

Para que sirve:
â€¢ Pausar temporalmente un tratamiento sin perder el historial
â€¢ Mantener el escritorio limpio con solo lo activo
â€¢ Reactivar facilmente cuando sea necesario"""
            ),
            ModuloDirectorio(
                id = "alertacaidas",
                titulo = "Alerta de Caidas",
                icono = Icons.Default.Warning,
                descripcionCorta = "Deteccion automatica de caidas con acelerometro",
                explicacionDetallada = """ALERTA DE CAIDAS

Que es:
Sistema de deteccion automatica de caidas usando el acelerometro y giroscopio del telefono. Al detectar una caida, lanza una alarma sonora y puede enviar un SMS de emergencia a los contactos configurados.

Como activarlo:
1. Ve al menu hamburguesa y selecciona Alerta de Caidas
2. Agrega hasta 4 contactos de emergencia
3. Ajusta la sensibilidad de deteccion segun tu uso
4. Activa el interruptor principal

Como funciona:
El algoritmo detecta caidas en 3 fases: caida libre (magnitud baja), impacto fuerte (pico alto) e inmovilidad tras el golpe. Solo confirma la caida cuando se cumplen las 3 fases en secuencia. Gracias a esto no se activa por golpes simples como dejar el telefono en una mesa.

Sensibilidad:
• Baja (20-40%): solo caidas muy fuertes. Recomendada para uso diario.
• Media (50-60%): balance deteccion/falsas alarmas. Valor por defecto.
• Alta (70-100%): detecta caidas leves pero puede dar falsas alarmas.

Alarma:
Al detectar una caida se abre una pantalla con cuenta regresiva. Si no se cancela, envia el SMS de emergencia con ubicacion GPS.

Consejo:
Si la alerta se activa al dejar el telefono sobre una mesa, baja la sensibilidad al 30-40%. Con sensibilidad mas baja el umbral de impacto es mayor y los golpes suaves no la activan."""
            ),
            ModuloDirectorio(
                id = "hidratacion",
                titulo = "Control de Hidratacion",
                icono = Icons.Default.WaterDrop,
                descripcionCorta = "Registro diario de ingesta de agua",
                explicacionDetallada = """CONTROL DE HIDRATACION

Que es:
Modulo para registrar y controlar la cantidad de agua o liquidos que consumes a diario, con meta personalizable y recordatorios periodicos.

Como usarlo:
1. Ve al menu hamburguesa y selecciona Control de Hidratacion
2. Configura tu meta diaria de agua (en ml o vasos)
3. Registra cada vez que bebes agua tocando el boton de ingesta
4. Consulta tu progreso en la barra de hidratacion del dia

Funciones:
• Meta diaria personalizable en ml o vasos
• Historial diario de consumo
• Recordatorios periodicos para beber agua
• Estadisticas de cumplimiento semanal

Para que sirve:
Mantener una hidratacion adecuada es esencial para la salud. Especialmente util para personas mayores o con indicacion medica de controlar la ingesta de liquidos."""
            ),
            ModuloDirectorio(
                id = "dental",
                titulo = "Módulo Dental",
                icono = Icons.Default.MedicalServices,
                descripcionCorta = "Control de salud bucal",
                explicacionDetallada = """🦷 MÓDULO DENTAL

Que es:
Modulo para llevar el control completo de la salud bucal: citas, odontograma, ortodoncia, seguimiento de la sonrisa y gastos dentales.

Como usarlo:
1. Selecciona un perfil activo
2. Abre el Modulo Dental desde el menu hamburguesa
3. Explora las pestanas: Panel, Citas, Odontograma, Ortodoncia, Sonrisa, Finanzas y Directorio

Pestanas:
• Panel: resumen con proximas citas, pendientes y dentistas registrados
• Citas: agenda de visitas al dentista con recordatorios
• Odontograma: mapa interactivo de los dientes, con estado y expediente por diente
• Ortodoncia: seguimiento de tratamientos, ajustes, incidencias y elasticos
• Sonrisa: diario de fotos antes y despues del tratamiento
• Finanzas: ingresos y gastos dentales con saldo y exportacion a PDF
• Directorio: contactos de dentistas y especialistas

Para que sirve:
Tener toda la informacion dental de la familia organizada en un solo lugar."""
            ),
            ModuloDirectorio(
                id = "sedentarismo",
                titulo = "Sedentarismo",
                icono = Icons.Default.Watch,
                descripcionCorta = "Alertas de inactividad prolongada",
                explicacionDetallada = """🪑 SEDENTARISMO

Que es:
Modulo que te ayuda a reducir el tiempo sentado o inactivo. Te envia recordatorios para levantarte y moverte despues de periodos prolongados de inactividad.

Como usarlo:
1. Ve al menu hamburguesa y selecciona Sedentarismo
2. Configura el tiempo maximo que quieres estar sin moverte
3. Activa las alertas
4. La app detecta inactividad y te avisa para que cambies de postura o camines

Funciones:
• Alertas despues de periodos configurables de inactividad
• Historial de alertas recibidas
• Configuracion de horarios activos
• Posposicion de recordatorios

Para que sirve:
Prevenir los riesgos de la vida sedentaria: dolor de espalda, contracturas, problemas circulatorios y cansancio. Ideal para quienes trabajan sentados o estudian mucho tiempo."""
            ),
            ModuloDirectorio(
                id = "medicos",
                titulo = "Médicos y especialistas",
                icono = Icons.Default.AccountCircle,
                descripcionCorta = "Directorio de contactos de salud",
                explicacionDetallada = """👨‍⚕️ MÉDICOS Y ESPECIALISTAS

Que es:
Directorio de medicos, dentistas, especialistas y otros contactos de salud que atienden a cada perfil.

Como usarlo:
1. Selecciona un perfil activo
2. Abre Médicos y especialistas desde el menu hamburguesa
3. Toca '+' para agregar un nuevo contacto
4. Ingresa nombre, especialidad, telefono, direccion y notas
5. Asocia el contacto a citas o informes si deseas

Funciones:
• Lista de contactos por perfil
• Busqueda rapida por nombre o especialidad
• Llamada directa desde la ficha
• Historial de citas vinculadas
• Notas personalizadas

Para que sirve:
Tener siempre a mano los datos de los profesionales de salud y poder contactarlos rapidamente en caso necesario."""
            ),
            ModuloDirectorio(
                id = "informes",
                titulo = "Informes médicos",
                icono = Icons.Default.Description,
                descripcionCorta = "Documentos y estudios adjuntos",
                explicacionDetallada = """📄 INFORMES MÉDICOS

Que es:
Modulo para adjuntar y organizar documentos, estudios, recetas, resultados de laboratorio o cualquier archivo medico relacionado con un perfil.

Como usarlo:
1. Selecciona un perfil activo
2. Abre Informes médicos desde el menu hamburguesa
3. Toca '+' para agregar un nuevo documento
4. Escribe titulo y descripcion
5. Adjunta fotos desde la camara o galeria

Funciones:
• Lista de documentos por perfil
• Escaneo de documentos con la camara
• Adjuntar imagenes desde galeria
• Vincular con profesionales o citas
• Visualizar documentos en cualquier momento

Para que sirve:
Tener toda la documentacion medica organizada y accesible para consultas, segundas opiniones o seguimientos de tratamientos."""
            )
        )
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            val mensajeBienvenida = if (perfilActivoNombre != null) {
                "¡Hola ${perfilActivoNombre}! Soy tu asistente virtual de Control médicamentos. Estoy aquí para ayudarte con tu perfil activo. Selecciona un módulo del directorio para ver la guía completa, o pregúntame lo que necesites."
            } else {
                "¡Hola! Soy tu asistente virtual de Control médicamentos. Para una experiencia personalizada, crea o selecciona un perfil primero. Mientras tanto, selecciona un modulo del directorio para ver la guia completa."
            }
            messages = listOf(
                ChatMessage(
                    id = "welcome",
                    content = mensajeBienvenida,
                    isUser = false
                )
            )
            delay(500)
            speakText(mensajeBienvenida)
        }
    }

    // Effect to speak new AI messages
    LaunchedEffect(messages.size) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser && lastMessage.id != "welcome") {
            delay(300)
            speakText(lastMessage.content)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF120F26))
    ) {
        TopAppBar(
            title = {
                Text(
                    if (moduloActivo != null) moduloActivo!!.titulo else "Asistente IA - Directorio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (moduloActivo != null) {
                        moduloActivo = null
                        mostrarDirectorio = true
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // TTS Controls Button - opens expanded controls
                IconButton(onClick = { showTtsControls = !showTtsControls }) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Controles de voz",
                        tint = if (showTtsControls) Color(0xFF4CAF50) else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF673AB7)
            )
        )

        // TTS Controls Panel
        if (showTtsControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF120F26))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Button
                    IconButton(
                        onClick = { stopSpeaking() },
                        enabled = isSpeaking || isPaused
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener",
                            tint = if (isSpeaking || isPaused) Color(0xFFEF5350) else Color(0xFF666666),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Repetir Button
                    IconButton(
                        onClick = { 
                            if (lastSpokenMessage != null) {
                                speakText(lastSpokenMessage!!)
                            }
                        },
                        enabled = lastSpokenMessage != null && ttsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = if (lastSpokenMessage != null && ttsEnabled) Color(0xFF4CAF50) else Color(0xFF666666),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Pause/Resume Button
                    IconButton(
                        onClick = { togglePauseResume() },
                        enabled = isSpeaking || isPaused
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Continuar" else "Pausar",
                            tint = if (isSpeaking || isPaused) Color(0xFFFFB74D) else Color(0xFF666666),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Enable/Disable TTS Toggle
                    IconButton(onClick = { ttsEnabled = !ttsEnabled }) {
                        Icon(
                            imageVector = if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (ttsEnabled) "Voz activada" else "Voz desactivada",
                            tint = if (ttsEnabled && ttsReady) Color(0xFF4CAF50) else Color(0xFFAAAAAA),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Speed Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = "Lento",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                    )
                    
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.5f..1.5f,
                        steps = 9,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4CAF50),
                            activeTrackColor = Color(0xFF4CAF50),
                            inactiveTrackColor = Color(0xFF444444)
                        )
                    )
                    
                    Text(
                        text = "Rápido",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                    )
                    
                    // Speed indicator
                    Text(
                        text = "${(speechRate * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp)
                    )
                }
            }
        }

        if (mostrarDirectorio && moduloActivo == null) {
            // MODO DIRECTORIO
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "📚 Directorio de Modulos",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Toca cualquier módulo para ver la explicación detallada:",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(modulos) { modulo ->
                    ModuloCard(
                        modulo = modulo,
                        onClick = {
                            moduloActivo = modulo
                            mostrarDirectorio = false
                            messages = messages + ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                content = "Seleccionaste: ${modulo.titulo}\n\n${modulo.explicacionDetallada}",
                                isUser = false
                            )
                        }
                    )
                }
            }
        } else {
            // MODO CHAT
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }

                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }

                if (moduloActivo != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                mostrarDirectorio = true
                                moduloActivo = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF673AB7)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Volver al Directorio")
                        }
                    }
                }
            }
        }

        // Input de mensaje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = currentMessage,
                onValueChange = { currentMessage = it },
                placeholder = {
                    Text(
                        if (moduloActivo != null) "Pregunta sobre este módulo..." else "Escribe tu pregunta...",
                        color = Color(0xFFAAAAAA)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF666666)
                ),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (currentMessage.isNotBlank() && !isLoading) {
                        mostrarDirectorio = false
                        scope.launch {
                            val contextoModulo = moduloActivo?.let { "Contexto: ${it.titulo}. " } ?: ""
                            sendMessage(
                                message = currentMessage,
                                contexto = contextoModulo,
                                onMessageAdded = { newMessage ->
                                    messages = messages + newMessage
                                },
                                onResponseReceived = { response ->
                                    messages = messages + response
                                },
                                onLoadingChanged = { loading ->
                                    isLoading = loading
                                }
                            )
                            currentMessage = ""
                        }
                    }
                },
                enabled = !isLoading && currentMessage.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (currentMessage.isNotBlank() && !isLoading)
                        Color(0xFF4CAF50) else Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser)
            Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    Color(0xFF4CAF50) else Color(0xFF2A2A2A)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 100.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Escribiendo",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                repeat(3) { index ->
                    Text(
                        text = ".",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModuloCard(
    modulo: ModuloDirectorio,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = modulo.icono,
                contentDescription = null,
                tint = Color(0xFF673AB7),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = modulo.titulo,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = modulo.descripcionCorta,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private suspend fun sendMessage(
    message: String,
    contexto: String = "",
    onMessageAdded: (ChatMessage) -> Unit,
    onResponseReceived: (ChatMessage) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    val userMessage = ChatMessage(
        id = System.currentTimeMillis().toString(),
        content = message,
        isUser = true
    )
    onMessageAdded(userMessage)

    onLoadingChanged(true)

    try {
        delay(1000)

        val responseText = when {
            message.contains("hola", ignoreCase = true) ->
                "¡Hola! ¿En qué puedo ayudarte hoy? Puedo explicarte cómo funcionan los recordatorios, perfiles de usuario, citas y alarmas. Selecciona un módulo del directorio para ver la guía completa."

            message.contains("artículo", ignoreCase = true) || message.contains("inventario", ignoreCase = true) || message.contains("producto", ignoreCase = true) ->
                "El módulo de Inventario te permite: Registrar artículos con nombre, presentación y cantidad. Agregar fotos para identificarlos. Configurar stock y alertas. Buscar y filtrar elementos. Ver detalles completos de cada artículo."

            message.contains("perfil", ignoreCase = true) || message.contains("usuario", ignoreCase = true) || message.contains("persona", ignoreCase = true) ->
                "El modulo de Perfiles te permite: Registrar usuarios con datos personales y contacto. Agregar foto. Cada usuario tiene su propio inventario y registros. Puedes cambiar entre perfiles fácilmente. Editar y eliminar perfiles."

            message.contains("toma", ignoreCase = true) || message.contains("registro", ignoreCase = true) || message.contains("uso", ignoreCase = true) ->
                "El Registro de Consumo te permite: Marcar cuándo usas un artículo del inventario. Ver historial por fecha. Filtrar por artículo o período. Exportar tu registro. Estadísticas de uso."

            message.contains("alarma", ignoreCase = true) || message.contains("recordatorio", ignoreCase = true) || message.contains("notificación", ignoreCase = true) ->
                "El sistema de Alarmas te permite: Configurar horarios para recordatorios. Recibir notificaciónes en el momento indicado. Configurar repeticion (diaria, por intervalos, etc). Activar o desactivar alarmas individuales. Ver lista de próximas alertas."

            message.contains("cita", ignoreCase = true) || message.contains("agenda", ignoreCase = true) || message.contains("reunion", ignoreCase = true) ->
                "El módulo de Agenda te permite: Registrar reuniones y eventos. Guardar fecha, hora y lugar. Agregar notas. Recibir recordatorios antes del evento. Ver historial de citas. Sincronizar con calendarios externos."

            message.contains("reporte", ignoreCase = true) || message.contains("estadistica", ignoreCase = true) || message.contains("resumen", ignoreCase = true) ->
                "Los Reportes generan: Historial de consumo. Estadísticas de cumplimiento. Gráficos por período. Exportación en Word (.docx). Resumen por perfil de usuario."

            message.contains("presion", ignoreCase = true) || message.contains("frecuencia", ignoreCase = true) || message.contains("metrica", ignoreCase = true) ->
                "El módulo de Métricas Diarias te permite: Registrar valores como presión, frecuencia cardíaca, temperatura, peso. Ver gráficos de tendencia. Comparativas por período. Alertas si los valores están fuera de rango."

            message.contains("embarazo", ignoreCase = true) || message.contains("gestacion", ignoreCase = true) ->
                "El modulo de Gestacion te permite: Seguimiento semana a semana. Registro de visitas de control. Peso y valores registrados. Cálculo de fecha. Alertas de citas. Al finalizar, crea automáticamente el perfil del bebé."

            message.contains("ciclo", ignoreCase = true) || message.contains("regla", ignoreCase = true) || message.contains("menstrual", ignoreCase = true) ->
                "El modulo de Ciclo te permite: Registrar inicio y fin del periodo. Marcar intensidad y síntomas. Cálculo automático de duración. Predicción del siguiente ciclo. Estadísticas de regularidad."

            message.contains("infantil", ignoreCase = true) || message.contains("bebe", ignoreCase = true) || message.contains("nino", ignoreCase = true) || message.contains("vacuna", ignoreCase = true) ->
                "El módulo Infantil incluye: Seguimiento de crecimiento con gráficos. Registro de peso y estatura. Calendario de vacunación. Controles programados. Datos desde el nacimiento."

            message.contains("compra", ignoreCase = true) || message.contains("carrito", ignoreCase = true) || message.contains("lista", ignoreCase = true) ->
                "El módulo de Compras te permite: Crear lista de artículos pendientes. Comparar precios entre compras. Registrar dónde compraste. Historial de gastos. Agregar al inventario automáticamente al comprar."

            message.contains("diario", ignoreCase = true) || message.contains("nota", ignoreCase = true) || message.contains("entrada", ignoreCase = true) ->
                "El Diario Personal te permite: Escribir notas con fecha. Adjuntar fotos. Buscador por palabras clave. Exportar entradas. Respaldo incluido en la copia de seguridad."

            message.contains("backup", ignoreCase = true) || message.contains("respaldo", ignoreCase = true) || message.contains("restaurar", ignoreCase = true) ->
                "El sistema de Respaldo permite: Crear copia de seguridad en archivo .cmed. Guarda todo: perfiles con fotos, inventario, registros, citas, diario con imágenes y más. Restaurar desde archivo. Compartir por WhatsApp, email, etc."

            message.contains("dental", ignoreCase = true) || message.contains("odontograma", ignoreCase = true) || message.contains("ortodoncia", ignoreCase = true) || message.contains("dentista", ignoreCase = true) ->
                "El Módulo Dental incluye: Panel de resumen, agenda de citas, odontograma interactivo por diente, seguimiento de ortodoncia, diario de sonrisa con fotos, control de finanzas dentales y directorio de dentistas. Toca el módulo en el Directorio para ver la guía completa."

            message.contains("sedentarismo", ignoreCase = true) || message.contains("sedentario", ignoreCase = true) || message.contains("inactividad", ignoreCase = true) ->
                "El módulo Sedentarismo te ayuda a reducir el tiempo inactivo: configura el tiempo máximo sin movimiento, recibe alertas para levantarte y muevete, consulta historial de alertas y posponé recordatorios cuando sea necesario."

            message.contains("medico", ignoreCase = true) || message.contains("especialista", ignoreCase = true) || message.contains("contacto", ignoreCase = true) ->
                "Médicos y especialistas es el directorio de contactos de salud: guarda nombre, especialidad, teléfono, dirección y notas, busca por nombre o especialidad, y vincula contactos a citas e informes médicos."

            message.contains("informe", ignoreCase = true) || message.contains("documento", ignoreCase = true) || message.contains("estudio", ignoreCase = true) ->
                "Informes médicos te permite adjuntar y organizar documentos, estudios, recetas y resultados de laboratorio. Puedes escanear con la cámara, adjuntar desde la galería, vincular con profesionales o citas y visualizarlos en cualquier momento."

            message.contains("como funciona", ignoreCase = true) || message.contains("funcionamiento", ignoreCase = true) ->
                "La app funciona asi: 1) Crea perfiles de usuario. 2) Agrega artículos al inventario de cada perfil. 3) Configura recordatorios con alarmas. 4) Cuando suena la alarma, registra el uso. 5) Consulta reportes y estadísticas. 6) Usa también la agenda, metricas y diario. Que modulo quieres explorar?"

            message.contains("empezar", ignoreCase = true) || message.contains("inicio", ignoreCase = true) || message.contains("configurar", ignoreCase = true) ->
                "Para empezar: 1) Ve a Perfiles y crea tu primer usuario. 2) Ve a Inventario y agrega los artículos para ese perfil. 3) Configura recordatorios para cada artículo. 4) ¡Listo! Recibirás alertas cuando corresponda."

            message.contains("ayuda", ignoreCase = true) || message.contains("que puedes hacer", ignoreCase = true) ->
                "Puedo ayudarte con: Perfiles de Usuario, Inventario, Registros de Consumo, Alarmas, Agenda, Médicos y especialistas, Informes médicos, Métricas Diarias, Gestación, Ciclo, Infantil, Compras, Diario, Respaldo, Galería, Actividad Física, Hidratación, Sedentarismo, Alerta de Caídas, Vacunas, Anticonceptivos, Estadísticas, Exportar Resumen, Verificador, Medicamentos Suspendidos y Módulo Dental. También puedes usar el Directorio tocando cualquier módulo para ver la guía completa."

            contexto.isNotEmpty() ->
                "Entiendo tu pregunta sobre $contexto Estoy aqui para aclarar cualquier duda sobre lo que lei en la explicacion. Si algo no quedo claro, preguntame específicamente sobre ese punto y te ayudo."

            else ->
                "Entiendo tu consulta. ¿Te gustaría que te explique cómo funciona algún módulo específico? Puedo ayudarte con Inventario, Perfiles, Alarmas, Agenda, Médicos y especialistas, Informes médicos, Métricas, Gestación, Ciclo, Infantil, Compras, Diario, Respaldo, Galería, Actividad Física, Hidratación, Sedentarismo, Alerta de Caídas, Vacunas, Anticonceptivos, Estadísticas, Exportar Resumen, Verificador, Medicamentos Suspendidos o Módulo Dental. También puedes usar el Directorio tocando el módulo que te interese!"
        }

        val aiMessage = ChatMessage(
            id = (System.currentTimeMillis() + 1).toString(),
            content = responseText,
            isUser = false
        )
        onResponseReceived(aiMessage)

    } catch (e: Exception) {
        val errorMessage = ChatMessage(
            id = (System.currentTimeMillis() + 1).toString(),
            content = "Lo siento, hubo un error procesando tu consulta. Por favor, intentalo de nuevo.",
            isUser = false
        )
        onResponseReceived(errorMessage)
    } finally {
        onLoadingChanged(false)
    }
}
