@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carlos.controlmedicamentos

import com.carlos.controlmedicamentos.BuildConfig
import com.carlos.controlmedicamentos.backup.BackupSelection
import com.carlos.controlmedicamentos.license.LicenseGate
import com.carlos.controlmedicamentos.license.LicenseViewModel
import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.RingtoneManager
import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.carlos.controlmedicamentos.backup.BackupManager
import com.carlos.controlmedicamentos.backup.AutoBackupScheduler
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.CicloMenstrual
import com.carlos.controlmedicamentos.data.local.BebeRecienNacido
import com.carlos.controlmedicamentos.data.local.ControlEmbarazo
import com.carlos.controlmedicamentos.data.local.NinoEntity
import com.carlos.controlmedicamentos.data.local.ProtocoloVacunacion
import com.carlos.controlmedicamentos.data.local.ControlPediatricoEntity
import com.carlos.controlmedicamentos.data.local.EnfermedadEntity
import com.carlos.controlmedicamentos.data.local.VacunaEntity
import com.carlos.controlmedicamentos.data.local.MedicalAppointment
import com.carlos.controlmedicamentos.data.local.MedicalPractitioner
import com.carlos.controlmedicamentos.data.local.MedicalReport
import com.carlos.controlmedicamentos.data.local.Medication
import com.carlos.controlmedicamentos.data.local.MedicationIntake
import com.carlos.controlmedicamentos.data.local.unidadesPorToma
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_NOT_TAKEN
import com.carlos.controlmedicamentos.data.local.MEDICATION_INTAKE_STATUS_TAKEN
import com.carlos.controlmedicamentos.data.local.MetodoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.TipoAnticonceptivo
import com.carlos.controlmedicamentos.data.local.AnticonceptivoIntake
import com.carlos.controlmedicamentos.data.local.RestockSource
import com.carlos.controlmedicamentos.data.local.PatientProfile
import com.carlos.controlmedicamentos.data.local.SignosVitales
import com.carlos.controlmedicamentos.data.local.VisitaPrenatal
import com.carlos.controlmedicamentos.data.remote.FakeVademecumRepository
import com.carlos.controlmedicamentos.data.remote.MedicalAiConfig
import com.carlos.controlmedicamentos.data.remote.MedicalAiSettings
import com.carlos.controlmedicamentos.data.remote.VademecumMedication
import com.carlos.controlmedicamentos.notifications.CriticalAlertConfig
import com.carlos.controlmedicamentos.notifications.CriticalAlertSettings
import com.carlos.controlmedicamentos.notifications.AnticonceptivoScheduler
import com.carlos.controlmedicamentos.notifications.MedicalAppointmentScheduler
import com.carlos.controlmedicamentos.notifications.MedicationScheduler
import com.carlos.controlmedicamentos.notifications.NotificacionHelper
import com.carlos.controlmedicamentos.notifications.CampaignNotifications
import com.carlos.controlmedicamentos.notifications.FcmTokenRepository
import com.carlos.controlmedicamentos.notifications.SignosVitalesScheduler
import com.carlos.controlmedicamentos.notifications.HidratacionScheduler
import com.carlos.controlmedicamentos.notifications.SedentarismoScheduler
import com.carlos.controlmedicamentos.notifications.VaccinationScheduler
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme
import com.carlos.controlmedicamentos.printer.PrinterSettingsScreen
import com.carlos.controlmedicamentos.ui.screens.AIChatScreen
import com.carlos.controlmedicamentos.ui.screens.StatisticsScreen
import com.carlos.controlmedicamentos.inicioDeLaSemana
import com.carlos.controlmedicamentos.finDeLaSemana
import com.carlos.controlmedicamentos.inicioDelMes
import com.carlos.controlmedicamentos.finDelMes
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var securityThreat by mutableStateOf<SecurityManager.ThreatLevel?>(
        if (BuildConfig.DEBUG) SecurityManager.ThreatLevel.SAFE else null
    )
    private var appUpdateCheck by mutableStateOf<AppUpdateCheck?>(null)
    private var updateDialogDismissed by mutableStateOf(false)

    // Solicitud centralizada de múltiples permisos al inicio
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Aquí puedes verificar si el usuario aceptó o denegó cada uno
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Todos los permisos fueron concedidos
            if (BuildConfig.DEBUG) {
                Log.d("MainActivity", "Todos los permisos han sido concedidos")
            }
        } else {
            // Algunos permisos fueron denegados
            if (BuildConfig.DEBUG) {
                val deniedPermissions = permissions.entries
                    .filter { !it.value }
                    .map { it.key }
                Log.w("MainActivity", "Permisos denegados: $deniedPermissions")
            }
        }
    }

    private fun applyCriticalAlertWindowState(intent: Intent?) {
        val shouldWakeForCriticalAlert = intent?.getBooleanExtra(
            NotificacionHelper.EXTRA_LAUNCH_CRITICAL_ALERT,
            false
        ) == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(shouldWakeForCriticalAlert)
            setTurnScreenOn(shouldWakeForCriticalAlert)
        }

        if (shouldWakeForCriticalAlert) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permisos que requieren solicitud en tiempo de ejecución (Android 6+)
        // Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ubicación (Android 6+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Cámara (Android 6+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Reconocimiento de actividad (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // Sensores del cuerpo (Android 6+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }

        // SMS (Android 6+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }

        // Contactos (Android 6+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        // Bluetooth (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        // Si hay permisos que solicitar, lanza la solicitud
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampaignNotifications.ensureChannel(this)
        
        // Solicitar todos los permisos necesarios al inicio
        checkAndRequestPermissions()
        lifecycleScope.launch { FcmTokenRepository.syncCurrentToken(this@MainActivity) }

        lifecycleScope.launch {
            val updateCheck = AppUpdateRemoteConfig.fetchAndCheck()
            appUpdateCheck = updateCheck
            updateDialogDismissed = false
            if (BuildConfig.DEBUG) {
                Log.d(
                    "MainActivity",
                    "Remote Config: instalada=${BuildConfig.VERSION_NAME}, " +
                        "disponible=${updateCheck.config.latestVersion}, " +
                        "hayActualizacion=${updateCheck.updateAvailable}, " +
                        "forzar=${updateCheck.forceUpdate}"
                )
            }
            // Fase 2: usar updateCheck para mostrar el aviso o bloqueo de actualizaciÃ³n.
        }

        if (!BuildConfig.DEBUG) {
            lifecycleScope.launch(Dispatchers.Default) {
                val threat = SecurityManager.assess(
                    this@MainActivity,
                    skipRootEmulator = true,
                    skipAllChecks = false
                )
                withContext(Dispatchers.Main.immediate) {
                    securityThreat = threat
                }
            }
        }

        applyCriticalAlertWindowState(intent)
        enableEdgeToEdge()
        setContent {
            ControlMedicamentosTheme {
                when (val threat = securityThreat) {
                    null -> SecurityCheckLoadingScreen()
                    SecurityManager.ThreatLevel.SAFE -> {
                        MainActivityContent(intent)
                        appUpdateCheck
                            ?.takeIf { it.updateAvailable && !updateDialogDismissed }
                            ?.let { update ->
                                AppUpdateDialog(
                                    update = update,
                                    onDismiss = { updateDialogDismissed = true }
                                )
                            }
                    }
                    else -> SecurityBlockedScreen(threat, onClose = ::finish)
            }
        }
    }
}

@Composable
private fun MainActivityContent(intent: Intent?) {
    val licenseViewModel: LicenseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    LicenseGate(
        viewModel = licenseViewModel,
        lemonSqueezyUrl = LicenseManager.URL_LICENCIA
    ) {
        var mostrarPortadaInicial by remember { mutableStateOf(true) }
        var birthdayPreviewRequest by remember { mutableStateOf<BirthdayCelebrationRequest?>(null) }

        val context = LocalContext.current
        val database = remember(context) { AppDatabase.getDatabase(context) }
        val pacienteActivo by database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)
        val mostrarPanelAlertaCaidas = remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            MedicamentoForm(
                modifier = Modifier.fillMaxSize(),
                onRequestBirthdayPreview = { birthdayPreviewRequest = it },
                launchIntent = intent,
                fallAlertPanelState = mostrarPanelAlertaCaidas
            )

            FallAlertPanelManager(
                mostrar = mostrarPanelAlertaCaidas,
                patientId = pacienteActivo?.id ?: 0,
                database = database,
                onVolver = { mostrarPanelAlertaCaidas.value = false }
            )

            BirthdayCelebrationHost(
                modifier = Modifier.fillMaxSize(),
                enabled = !mostrarPortadaInicial,
                previewRequest = birthdayPreviewRequest,
                onPreviewConsumed = { birthdayPreviewRequest = null }
            )

            if (mostrarPortadaInicial) {
                StartupOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onDismiss = { mostrarPortadaInicial = false }
                )
            }
        }
    }
}

@Composable
private fun SecurityCheckLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SecurityBlockedScreen(
    threat: SecurityManager.ThreatLevel,
    onClose: () -> Unit
) {
    val (title, message) = when (threat) {
        SecurityManager.ThreatLevel.DEBUGGER_DETECTED ->
            "Aplicacion bloqueada" to "Se detecto un depurador adjunto. La aplicacion no puede ejecutarse en este entorno."
        SecurityManager.ThreatLevel.TAMPERED ->
            "Aplicacion comprometida" to "Esta aplicacion parece haber sido modificada. Por favor, reinstala desde una fuente oficial."
        else -> "Error de seguridad" to "Se detecto un problema de seguridad. La aplicacion se cerrara."
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onClose) { Text("Aceptar") } }
    )
}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyCriticalAlertWindowState(intent)
    }
}

@Composable
private fun StartupOverlay(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    var heartRevealActive by remember { mutableStateOf(true) }
    val revealScale = remember { Animatable(0.18f) }
    val beatScale = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }
    var showAuthor by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        showAuthor = false
    }
    val heartShape = remember {
        GenericShape { size, _ ->
            moveTo(size.width / 2f, size.height * 0.92f)
            cubicTo(
                size.width * 0.08f,
                size.height * 0.72f,
                size.width * 0.02f,
                size.height * 0.34f,
                size.width * 0.26f,
                size.height * 0.20f
            )
            cubicTo(
                size.width * 0.40f,
                size.height * 0.08f,
                size.width * 0.50f,
                size.height * 0.16f,
                size.width / 2f,
                size.height * 0.24f
            )
            cubicTo(
                size.width * 0.50f,
                size.height * 0.16f,
                size.width * 0.60f,
                size.height * 0.08f,
                size.width * 0.74f,
                size.height * 0.20f
            )
            cubicTo(
                size.width * 0.98f,
                size.height * 0.34f,
                size.width * 0.92f,
                size.height * 0.72f,
                size.width / 2f,
                size.height * 0.92f
            )
            close()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        launch {
            repeat(6) {
                beatScale.animateTo(
                    targetValue = 1.10f,
                    animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
                )
                beatScale.animateTo(
                    targetValue = 0.97f,
                    animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
                )
                beatScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                )
            }
        }
        revealScale.animateTo(
            targetValue = 1.35f,
            animationSpec = tween(durationMillis = 2280, easing = FastOutSlowInEasing)
        )
        heartRevealActive = false
        revealScale.snapTo(1f)
        beatScale.snapTo(1f)
    }

    BlueMetalBackground(
        modifier = modifier
            .clickable(onClick = onDismiss)
            .graphicsLayer(
                alpha = alpha.value,
                scaleX = if (heartRevealActive) revealScale.value * beatScale.value else 1f,
                scaleY = if (heartRevealActive) revealScale.value * beatScale.value else 1f,
                transformOrigin = TransformOrigin.Center,
                shape = heartShape,
                clip = heartRevealActive
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .zIndex(3f)
                .background(
                    color = Color(0xE8F0C15A),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xCC7D5A16),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CONTROL DE\nMEDICAMENTOS",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "v${com.carlos.controlmedicamentos.BuildConfig.VERSION_NAME}",
                    color = Color(0xFF5A3A00),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        StartupMonitorCard(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 66.dp)
        )

        StartupBottleShelf(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 24.dp)
        )

        StartupAiBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 112.dp, top = 132.dp)
        )

        StartupClinicianGroup(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        )

        StartupAiBadge(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 58.dp)
        )

        StartupLabShowcase(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 74.dp)
        )

        StartupBottleShelf(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 60.dp)
        )

        AnimatedVisibility(
            visible = showAuthor,
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .zIndex(3f)
                    .background(
                        color = Color(0xE8FFFFFF),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Create by Carlos Gamboa G7 Enterprise",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}

@Composable
private fun StartupMonitorCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(190.dp)
            .height(158.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD6F5F7).copy(alpha = 0.70f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                val line1 = size.height * 0.24f
                val line2 = size.height * 0.56f
                val line3 = size.height * 0.84f
                val stroke = 3.dp.toPx()

                drawLine(Color.White.copy(alpha = 0.38f), Offset(0f, line1), Offset(size.width, line1), stroke)
                drawLine(Color.White.copy(alpha = 0.38f), Offset(0f, line2), Offset(size.width, line2), stroke)

                drawLine(Color(0xFF57A6A8), Offset(0f, line1), Offset(size.width * 0.18f, line1 + 2.dp.toPx()), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.18f, line1 + 2.dp.toPx()), Offset(size.width * 0.28f, line1 - 16.dp.toPx()), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.28f, line1 - 16.dp.toPx()), Offset(size.width * 0.39f, line1 + 10.dp.toPx()), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.39f, line1 + 10.dp.toPx()), Offset(size.width * 0.54f, line1), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.54f, line1), Offset(size.width * 0.68f, line1 - 10.dp.toPx()), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.68f, line1 - 10.dp.toPx()), Offset(size.width * 0.82f, line1 + 6.dp.toPx()), stroke)
                drawLine(Color(0xFF57A6A8), Offset(size.width * 0.82f, line1 + 6.dp.toPx()), Offset(size.width, line1), stroke)

                drawLine(Color(0xFFEE8758), Offset(0f, line3), Offset(size.width * 0.12f, line3 + 4.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.12f, line3 + 4.dp.toPx()), Offset(size.width * 0.22f, line3 - 14.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.22f, line3 - 14.dp.toPx()), Offset(size.width * 0.34f, line3 + 5.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.34f, line3 + 5.dp.toPx()), Offset(size.width * 0.48f, line3 - 10.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.48f, line3 - 10.dp.toPx()), Offset(size.width * 0.62f, line3 + 3.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.62f, line3 + 3.dp.toPx()), Offset(size.width * 0.78f, line3 - 7.dp.toPx()), stroke)
                drawLine(Color(0xFFEE8758), Offset(size.width * 0.78f, line3 - 7.dp.toPx()), Offset(size.width, line3 + 2.dp.toPx()), stroke)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(50))
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.60f), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun StartupBottleShelf(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(258.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC6F1F4).copy(alpha = 0.46f)),
        border = BorderStroke(1.dp, Color(0xFF7FD4D8).copy(alpha = 0.55f)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { index ->
                    StartupBottle(
                        modifier = Modifier.weight(1f),
                        accent = if (index % 2 == 0) Color(0xFFF4B13E) else Color(0xFFE86E5A)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { index ->
                    StartupBottle(
                        modifier = Modifier.weight(1f),
                        accent = if (index % 2 == 0) Color(0xFF58B5F2) else Color(0xFFF0A243)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupBottle(
    modifier: Modifier = Modifier,
    accent: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(10.dp)
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFFFF6E4),
                            Color(0xFFF5C56A)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(24.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.86f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color(0xFFF0A526).copy(alpha = 0.88f))
            )
        }
    }
}

@Composable
private fun StartupAiBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFFFF3C0),
                        Color(0xFF9ADBE3)
                    )
                )
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF2B7DA0).copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IA",
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun StartupClinicianGroup(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        StartupClinicianFigure(
            height = 210.dp,
            skin = Color(0xFF7E523D),
            hair = Color(0xFF1B1A1A),
            bodyBrush = Brush.verticalGradient(listOf(Color(0xFF4CC2C7), Color(0xFF178896))),
            accent = Color(0xFF0E5861)
        )
        StartupClinicianFigure(
            height = 240.dp,
            skin = Color(0xFFEBBE9E),
            hair = Color(0xFF1E1C1C),
            bodyBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE6EEF3))),
            accent = Color(0xFF747E85),
            tablet = true
        )
        StartupClinicianFigure(
            height = 228.dp,
            skin = Color(0xFFD4A788),
            hair = Color(0xFF1A1A1B),
            bodyBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE5EDF1))),
            accent = Color(0xFF84B9D8)
        )
    }
}

@Composable
private fun StartupClinicianFigure(
    height: androidx.compose.ui.unit.Dp,
    skin: Color,
    hair: Color,
    bodyBrush: Brush,
    accent: Color,
    tablet: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(skin, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(74.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topStart = 37.dp, topEnd = 37.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(hair)
            )
        }
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(bodyBrush)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .width(42.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(alpha = 0.34f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .width(72.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.38f))
            )
            if (tablet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(52.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF565D68))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(42.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF8A949F))
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupLabShowcase(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(208.dp)
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFE76C4E), Color(0xFFC94E3D), Color(0xFFF0984A))))
        )
        Card(
            modifier = Modifier
                .width(260.dp)
                .height(172.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCF9FF).copy(alpha = 0.28f)),
            border = BorderStroke(1.dp, Color(0xFF7ACDD6).copy(alpha = 0.58f)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF275372), CircleShape))
                    Box(modifier = Modifier.size(18.dp).background(Color(0xFF5EAECC), CircleShape))
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFF0AE46), CircleShape))
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFF4E7895), CircleShape))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(90.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = Color(0xFF2D6581),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlueMetalBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.drawWithCache {
            val baseGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF06174B),
                    Color(0xFF0D4FA8),
                    Color(0xFF1AA3F5),
                    Color(0xFF0C4AA2),
                    Color(0xFF040F32)
                )
            )
            val topShade = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.22f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.30f)
                )
            )
            val strongLineColor = Color.White.copy(alpha = 0.11f)
            val softLineColor = Color(0xFF78C8FF).copy(alpha = 0.08f)
            val thinStroke = 1.dp.toPx()
            val softStroke = 0.6.dp.toPx()
            val spacing = 6.dp.toPx()
            val secondarySpacing = 3.dp.toPx()

            onDrawBehind {
                drawRect(brush = baseGradient)

                var y = 0f
                var index = 0
                while (y < size.height) {
                    drawLine(
                        color = if (index % 3 == 0) strongLineColor else softLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (index % 3 == 0) thinStroke else softStroke
                    )
                    if (index % 5 == 0) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.06f),
                            start = Offset(size.width * 0.08f, y + secondarySpacing),
                            end = Offset(size.width * 0.92f, y + secondarySpacing),
                            strokeWidth = softStroke
                        )
                    }
                    y += spacing
                    index += 1
                }

                drawRect(brush = topShade)
            }
        },
        content = content
    )
}

@Composable
internal fun MetallicMedicationCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 16,
    verticalSpacing: Int = 8,
    expandVertically: Boolean = false,
    isInss: Boolean = false,
    isStockCritical: Boolean = false,
    isSuspended: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = if (isInss) Color(0xFF3A1A00) else if (isSuspended) Color(0xFF333333) else Color.White),
        border = BorderStroke(1.dp, when {
            isStockCritical -> Color(0xFFFF5252).copy(alpha = 0.70f)
            isSuspended -> Color(0xFF808080).copy(alpha = 0.70f)
            isInss -> Color(0xFFFFD700).copy(alpha = 0.70f)
            else -> Color(0xFF1F8EF1).copy(alpha = 0.45f)
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expandVertically) Modifier.fillMaxHeight() else Modifier)
                .clip(shape)
                .drawWithCache {
                    val baseGradient = when {
                        isStockCritical -> Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4A0000),
                                Color(0xFF8B0000),
                                Color(0xFFD32F2F),
                                Color(0xFF8B0000),
                                Color(0xFF4A0000)
                            )
                        )
                        isSuspended -> Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFB0B0B0),
                                Color(0xFFC0C0C0),
                                Color(0xFFD0D0D0),
                                Color(0xFFC0C0C0),
                                Color(0xFFB0B0B0)
                            )
                        )
                        isInss -> Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF7A3A00),
                                Color(0xFFCC7A00),
                                Color(0xFFFFD000),
                                Color(0xFFFF9500),
                                Color(0xFF7A3A00)
                            )
                        )
                        else -> Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF071937),
                                Color(0xFF0E427A),
                                Color(0xFF19A0F2),
                                Color(0xFF0D4E95),
                                Color(0xFF051126)
                            )
                        )
                    }
                    val sheenGradient = when {
                        isStockCritical -> Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFF6666).copy(alpha = 0.30f),
                                Color.Transparent,
                                Color(0xFFFF4444).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                        isSuspended -> Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFFFFF).copy(alpha = 0.08f),
                                Color.Transparent,
                                Color(0xFF000000).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                        isInss -> Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFF0A0).copy(alpha = 0.30f),
                                Color.Transparent,
                                Color(0xFFFFD700).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                        else -> Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF84E7FF).copy(alpha = 0.10f),
                                Color.Transparent,
                                Color(0xFF2E8CFF).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    }
                    val overlayShade = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                    val strongLineColor = when {
                        isStockCritical -> Color(0xFFFF9999).copy(alpha = 0.12f)
                        isSuspended -> Color(0xFFAAAAAA).copy(alpha = 0.08f)
                        else -> Color.White.copy(alpha = 0.12f)
                    }
                    val softLineColor = when {
                        isStockCritical -> Color(0xFFFF4444).copy(alpha = 0.08f)
                        isSuspended -> Color(0xFF888888).copy(alpha = 0.05f)
                        else -> Color(0xFF89D8FF).copy(alpha = 0.08f)
                    }
                    val lineSpacing = 5.dp.toPx()
                    val thinStroke = 0.8.dp.toPx()
                    val topGlowHeight = size.height * 0.18f

                    onDrawBehind {
                        drawRect(brush = baseGradient)
                        drawRect(brush = sheenGradient)

                        var y = 0f
                        var index = 0
                        while (y < size.height) {
                            drawLine(
                                color = if (index % 4 == 0) strongLineColor else softLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = thinStroke
                            )
                            y += lineSpacing
                            index += 1
                        }

                        drawRect(brush = overlayShade)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    when {
                                        isStockCritical -> Color(0xFFFFAAAA).copy(alpha = 0.26f)
                                        isSuspended -> Color(0xFFFFFFFF).copy(alpha = 0.10f)
                                        else -> Color(0xFFB7FFFF).copy(alpha = 0.26f)
                                    },
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, topGlowHeight)
                        )
                    }
                }
                .padding(contentPadding.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
            content = content
        )
    }
}

@Composable
internal fun MetallicProfileCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0x220F2C1C)),
        border = BorderStroke(1.dp, Color(0xFFB6FFD0).copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

@Composable
internal fun MetallicGreenHeaderCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF7FFFB0).copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .drawWithCache {
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF051A0F),
                            Color(0xFF0B5130),
                            Color(0xFF6BFF9B),
                            Color(0xFF0D5A36),
                            Color(0xFF041108)
                        )
                    )
                    val sheenGradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFC8FFD6).copy(alpha = 0.14f),
                            Color.Transparent,
                            Color(0xFF7AFFA5).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                    val overlayShade = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.26f)
                        )
                    )
                    val strongLineColor = Color.White.copy(alpha = 0.10f)
                    val softLineColor = Color(0xFFA3FFC3).copy(alpha = 0.07f)
                    val lineSpacing = 5.dp.toPx()
                    val thinStroke = 0.8.dp.toPx()

                    onDrawBehind {
                        drawRect(brush = baseGradient)
                        drawRect(brush = sheenGradient)

                        var y = 0f
                        var index = 0
                        while (y < size.height) {
                            drawLine(
                                color = if (index % 4 == 0) strongLineColor else softLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = thinStroke
                            )
                            y += lineSpacing
                            index += 1
                        }

                        drawRect(brush = overlayShade)
                    }
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun TransparentFormSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
internal fun MetallicRedVitalSignsCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 16,
    verticalSpacing: Int = 10,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val vitalSignsContentColor = Color(0xFFFFE45E)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFFF7A7A).copy(alpha = 0.34f))
    ) {
        CompositionLocalProvider(LocalContentColor provides vitalSignsContentColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .drawWithCache {
                        val baseGradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF450000),
                                Color(0xFF8F0000),
                                Color(0xFFFF1212),
                                Color(0xFF8C0000),
                                Color(0xFF2B0000)
                            )
                        )
                        val sheenGradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f),
                                Color(0xFFFFB0B0).copy(alpha = 0.18f),
                                Color.Transparent,
                                Color(0xFFFF5A5A).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                        val overlayShade = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.28f)
                            )
                        )
                        val strongLineColor = Color.White.copy(alpha = 0.08f)
                        val softLineColor = Color(0xFFFF8B8B).copy(alpha = 0.05f)
                        val lineSpacing = 5.dp.toPx()
                        val thinStroke = 0.7.dp.toPx()

                        onDrawBehind {
                            drawRect(brush = baseGradient)
                            drawRect(brush = sheenGradient)

                            var y = 0f
                            var index = 0
                            while (y < size.height) {
                                drawLine(
                                    color = if (index % 4 == 0) strongLineColor else softLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = thinStroke
                                )
                                y += lineSpacing
                                index += 1
                            }

                            drawRect(brush = overlayShade)
                        }
                    }
                    .padding(contentPadding.dp),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun VitalSignsMetallicButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFFF2F2)
        ),
        border = BorderStroke(1.dp, Color(0xFFFF9D9D).copy(alpha = 0.36f)),
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val baseGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C0000),
                        Color(0xFF790000),
                        Color(0xFFFF2323),
                        Color(0xFF650000),
                        Color(0xFF190000)
                    ),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f)
                )
                val sheenGradient = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color(0xFFFFD1D1).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * 0.15f, size.height),
                    end = Offset(size.width * 0.8f, 0f)
                )
                val shadowStripes = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.34f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    start = Offset(0f, size.height * 0.95f),
                    end = Offset(size.width, 0f)
                )

                onDrawBehind {
                    drawRect(brush = baseGradient)
                    drawRect(brush = shadowStripes)
                    drawRect(brush = sheenGradient)
                }
            }
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BirthdayCelebrationHost(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    previewRequest: BirthdayCelebrationRequest?,
    onPreviewConsumed: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val pacienteActivo by database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)
    var ultimoSaludoKey by remember { mutableStateOf<String?>(null) }
    var mostrarSaludo by remember { mutableStateOf(false) }
    val pacienteConCumplePersistido = remember(pacienteActivo, context) {
        applyPersistedBirthdayFallback(context, pacienteActivo)
    }
    val saludoKey = remember(pacienteConCumplePersistido, enabled) {
        if (!enabled) {
            null
        } else {
            birthdayCelebrationKey(pacienteConCumplePersistido)
        }
    }

    LaunchedEffect(enabled, saludoKey) {
        if (!enabled || saludoKey == null) {
            return@LaunchedEffect
        }
        if (ultimoSaludoKey != saludoKey) {
            ultimoSaludoKey = saludoKey
            mostrarSaludo = true
        }
    }

    if (enabled && previewRequest != null) {
        BirthdayCelebrationOverlay(
            modifier = modifier,
            patientName = previewRequest.patientName,
            age = previewRequest.age,
            seed = previewRequest.seed,
            onComplete = onPreviewConsumed
        )
    } else if (enabled && mostrarSaludo && pacienteConCumplePersistido != null) {
        BirthdayCelebrationOverlay(
            modifier = modifier,
            patientName = pacienteConCumplePersistido!!.nombre.ifBlank { pacienteConCumplePersistido!!.apellidos }.ifBlank { "" },
            age = calcularEdadDesdeNacimiento(pacienteConCumplePersistido!!.fechaNacimiento),
            seed = pacienteConCumplePersistido!!.id,
            onComplete = { mostrarSaludo = false }
        )
    }
}

@Composable
private fun BirthdayCelebrationOverlay(
    modifier: Modifier = Modifier,
    patientName: String,
    age: Int,
    seed: Int,
    onComplete: () -> Unit
) {
    val durationMillis = 10_000L
    var elapsedMillis by remember { mutableStateOf(0L) }
    val bursts = remember(seed) { generateBirthdayFireworkBursts(seed) }

    BackHandler(enabled = true) {
    }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        do {
            val frameTime = withFrameNanos { it }
            elapsedMillis = ((frameTime - start) / 1_000_000L).coerceAtMost(durationMillis)
        } while (elapsedMillis < durationMillis)
        delay(150L)
        onComplete()
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040814),
                        Color(0xFF0B1E54),
                        Color(0xFF2B1254),
                        Color(0xFF070B18)
                    )
                )
            )
            .zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBirthdayFireworks(
                elapsedMillis = elapsedMillis.toFloat(),
                bursts = bursts,
                width = size.width,
                height = size.height
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Feliz dia en tu cumpleaños",
                color = Color(0xFFFFF4D6),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFFFB74D).copy(alpha = 0.8f),
                        blurRadius = 22f
                    )
                )
            )
            Text(
                text = patientName.ifBlank { "Hoy es tu dia" },
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF7FDBFF).copy(alpha = 0.9f),
                        blurRadius = 24f
                    )
                )
            )
            Text(
                text = if (age > 0) "$age años hoy. Que cumplas muchos mas." else "Que cumplas muchos mas.",
                color = Color(0xFFEAF6FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun drawBirthdayFireworks(
    elapsedMillis: Float,
    bursts: List<BirthdayFireworkBurst>,
    width: Float,
    height: Float
) {
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBirthdayFireworks(
    elapsedMillis: Float,
    bursts: List<BirthdayFireworkBurst>,
    width: Float,
    height: Float
) {
    bursts.forEach { burst ->
        val launchDuration = 650f
        val explosionDuration = 1150f
        val localElapsed = elapsedMillis - burst.startMillis
        if (localElapsed <= 0f || localElapsed > launchDuration + explosionDuration) {
            return@forEach
        }

        val center = Offset(width * burst.centerXFraction, height * burst.centerYFraction)
        val launchStart = Offset(width * burst.centerXFraction, height * 1.05f)

        if (localElapsed < launchDuration) {
            val progress = localElapsed / launchDuration
            val currentY = launchStart.y + (center.y - launchStart.y) * progress
            val rocket = Offset(center.x, currentY)
            drawLine(
                color = burst.color.copy(alpha = 0.75f),
                start = Offset(rocket.x, rocket.y + 32f),
                end = rocket,
                strokeWidth = 4f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 7f,
                center = rocket
            )
        } else {
            val progress = ((localElapsed - launchDuration) / explosionDuration).coerceIn(0f, 1f)
            val alpha = (1f - progress).coerceAtLeast(0f)
            val radius = burst.spread * width * (0.05f + 0.17f * progress)

            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.9f),
                radius = 18f * (1f - progress * 0.45f),
                center = center
            )

            repeat(burst.particleCount) { index ->
                val angle = burst.angleOffset + ((PI.toFloat() * 2f) / burst.particleCount) * index
                val wobble = 0.72f + ((index % 4) * 0.08f)
                val distance = radius * wobble
                val x = center.x + cos(angle) * distance
                val y = center.y + sin(angle) * distance + progress * progress * height * 0.08f
                drawCircle(
                    color = burst.color.copy(alpha = alpha),
                    radius = 6.5f * (1f - progress * 0.35f),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.75f),
                    radius = 2.4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun generateBirthdayFireworkBursts(seed: Int): List<BirthdayFireworkBurst> {
    val random = Random(seed.coerceAtLeast(1) * 37)
    val palette = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFD166),
        Color(0xFF7AE582),
        Color(0xFF70D6FF),
        Color(0xFFC77DFF),
        Color(0xFFFF8FAB)
    )

    return List(15) { index ->
        BirthdayFireworkBurst(
            startMillis = 250f + index * 620f + random.nextInt(0, 220),
            centerXFraction = 0.15f + random.nextFloat() * 0.7f,
            centerYFraction = 0.16f + random.nextFloat() * 0.4f,
            color = palette[index % palette.size],
            particleCount = 16 + random.nextInt(0, 8),
            spread = 0.72f + random.nextFloat() * 0.42f,
            angleOffset = random.nextFloat() * (PI.toFloat() * 2f)
        )
    }
}

private fun birthdayCelebrationKey(
    patient: PatientProfile?,
    reference: Long = System.currentTimeMillis()
): String? {
    val birthDate = patient?.fechaNacimiento?.takeIf { it > 0L } ?: return null
    if (!isBirthdayToday(birthDate, reference)) {
        return null
    }
    return "${patient.id}-${inicioDelDia(reference)}"
}

private fun isBirthdayToday(
    birthDate: Long,
    reference: Long = System.currentTimeMillis()
): Boolean {
    val birthCalendar = Calendar.getInstance().apply { timeInMillis = birthDate }
    val todayCalendar = Calendar.getInstance().apply { timeInMillis = reference }
    return birthCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH) &&
        birthCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH)
}

private data class BirthdayFireworkBurst(
    val startMillis: Float,
    val centerXFraction: Float,
    val centerYFraction: Float,
    val color: Color,
    val particleCount: Int,
    val spread: Float,
    val angleOffset: Float
)

data class BirthdayCelebrationRequest(
    val patientName: String,
    val age: Int,
    val seed: Int
)

internal data class CarritoItem(
    val medication: Medication,
    val unidadesSolicitadas: Int,
    val carritoRowId: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentoForm(
    modifier: Modifier = Modifier,
    onRequestBirthdayPreview: (BirthdayCelebrationRequest) -> Unit = {},
    launchIntent: android.content.Intent? = null,
    fallAlertPanelState: MutableState<Boolean>
) {
    val _s = rememberMedicamentoFormState()
    var nombre by _s.nombreState
    var cantidad by _s.cantidadState
    var alarmaActiva by _s.alarmaActivaState
    var esCicloCorto by _s.esCicloCortoState
    var estaActivo by _s.estaActivoState
    var editingMedicationId by _s.editingMedicationIdState
    var controlarExistencias by _s.controlarExistenciasState
    var stockActual by _s.stockActualState
    var stockMinimo by _s.stockMinimoState
    var precioPorUnidad by _s.precioPorUnidadState
    var telefonoPedidoWhatsapp by _s.telefonoPedidoWhatsappState
    var dispensacionGratuita by _s.dispensacionGratuitaState
    var origenReposicion by _s.origenReposicionState
    var expandedOrigenReposicion by _s.expandedOrigenReposicionState
    var selectedMedication by _s.selectedMedicationState
    var fechaInicio by _s.fechaInicioState
    var fechaFin by _s.fechaFinState
    var horaTomaSeleccionada by _s.horaTomaSeleccionadaState
    var medicationToDelete by _s.medicationToDeleteState
    var insumoARecargar by _s.insumoARecargarState
    var inputRecargarStock by _s.inputRecargarStockState
    var insumoAPedir by _s.insumoAPedirState
    var inputUnidadesPedido by _s.inputUnidadesPedidoState
    var itemCarritoAConfirmar by _s.itemCarritoAConfirmarState
    var itemCarritoAEliminar by _s.itemCarritoAEliminarState
    var confirmarRecepcionTotal by _s.confirmarRecepcionTotalState
    var pedidoAEliminar by _s.pedidoAEliminarState
    var pedidoAEditar by _s.pedidoAEditarState
    var inputEditarResumen by _s.inputEditarResumenState
    var inputEditarTotal by _s.inputEditarTotalState
    var inputUnidadesRecibidas by _s.inputUnidadesRecibidasState
    var inputPrecioActualizado by _s.inputPrecioActualizadoState
    var duplicateMedication by _s.duplicateMedicationState
    val _mostrarFormulario = _s.mostrarFormularioState
    var mostrarFormulario by _mostrarFormulario
    val _mostrarFichaPaciente = _s.mostrarFichaPacienteState
    var mostrarFichaPaciente by _mostrarFichaPaciente
    val _mostrarMenuHamburguesa = _s.mostrarMenuHamburguesaState
    var mostrarMenuHamburguesa by _mostrarMenuHamburguesa
    val _mostrarFormularioInforme = _s.mostrarFormularioInformeState
    var mostrarFormularioInforme by _mostrarFormularioInforme
    var mostrarFormularioProfesional by _s.mostrarFormularioProfesionalState
    val _mostrarPanelPacientes = _s.mostrarPanelPacientesState
    var mostrarPanelPacientes by _mostrarPanelPacientes
    var mostrarPanelProfesionales by _s.mostrarPanelProfesionalesState
    val _mostrarPanelInformes = _s.mostrarPanelInformesState
    var mostrarPanelInformes by _mostrarPanelInformes
    val _mostrarListaInsumos = _s.mostrarListaInsumosState
    var mostrarListaInsumos by _mostrarListaInsumos
    val _mostrarPanelBackups = _s.mostrarPanelBackupsState
    var mostrarPanelBackups by _mostrarPanelBackups
    var mostrarPanelPedidos by _s.mostrarPanelPedidosState
    var mostrarPanelPodometro by _s.mostrarPanelPodometroState
    val _mostrarPanelConfiguracionAlertas = _s.mostrarPanelConfiguracionAlertasState
    var mostrarPanelConfiguracionAlertas by _mostrarPanelConfiguracionAlertas
    val _mostrarPanelSignosVitales = _s.mostrarPanelSignosVitalesState
    var mostrarPanelSignosVitales by _mostrarPanelSignosVitales
    val _mostrarPanelConfiguracionIa = _s.mostrarPanelConfiguracionIaState
    var mostrarPanelConfiguracionIa by _mostrarPanelConfiguracionIa
    var mostrarPanelAsistenteIa by _s.mostrarPanelAsistenteIaState
    var mostrarPanelCicloMenstrual by _s.mostrarPanelCicloMenstrualState
    var mostrarPanelEmbarazo by _s.mostrarPanelEmbarazoState
    var mostrarPanelAnticonceptivos by _s.mostrarPanelAnticonceptivosState
    var mostrarPanelPediatrico by _s.mostrarPanelPediatricoState
    var mostrarPanelReporteClinico by _s.mostrarPanelReporteClinicoState
    var mostrarPanelEstadisticas by _s.mostrarPanelEstadisticasState
    var mostrarPanelDiario by _s.mostrarPanelDiarioState
    var mostrarPanelVerificadorTomas by _s.mostrarPanelVerificadorTomasState
    var mostrarPanelHidratacion by _s.mostrarPanelHidratacionState
    var mostrarPanelSedentarismo by _s.mostrarPanelSedentarismoState
    var mostrarPanelDentista by _s.mostrarPanelDentistaState
    var insumoSeleccionadoEnInventario by _s.insumoSeleccionadoEnInventarioState
    val _editingPatientId = _s.editingPatientIdState
    var editingPatientId by _editingPatientId
    var editingReportId by _s.editingReportIdState
    var practitionerIdInforme by _s.practitionerIdInformeState
    var editingPractitionerId by _s.editingPractitionerIdState
    var profesionalSeleccionadoId by _s.profesionalSeleccionadoIdState
    val _citaMedicaSeleccionadaId = _s.citaMedicaSeleccionadaIdState
    var citaMedicaSeleccionadaId by _citaMedicaSeleccionadaId
    val _editandoFichaPaciente = _s.editandoFichaPacienteState
    var editandoFichaPaciente by _editandoFichaPaciente
    var editingAppointmentId by _s.editingAppointmentIdState
    val _nombrePaciente = _s.nombrePacienteState
    var nombrePaciente by _nombrePaciente
    val _apellidosPaciente = _s.apellidosPacienteState
    var apellidosPaciente by _apellidosPaciente
    val _fechaNacimientoPaciente = _s.fechaNacimientoPacienteState
    var fechaNacimientoPaciente by _fechaNacimientoPaciente
    val _edadPaciente = _s.edadPacienteState
    var edadPaciente by _edadPaciente
    val _pesoPaciente = _s.pesoPacienteState
    var pesoPaciente by _pesoPaciente
    val _pesoUnidadPaciente = _s.pesoUnidadPacienteState
    var pesoUnidadPaciente by _pesoUnidadPaciente
    val _estaturaPaciente = _s.estaturaPacienteState
    var estaturaPaciente by _estaturaPaciente
    val _estaturaUnidadPaciente = _s.estaturaUnidadPacienteState
    var estaturaUnidadPaciente by _estaturaUnidadPaciente
    val _sexoPaciente = _s.sexoPacienteState
    var sexoPaciente by _sexoPaciente
    val _paisPaciente = _s.paisPacienteState
    var paisPaciente by _paisPaciente
    val _monedaPaciente = _s.monedaPacienteState
    var monedaPaciente by _monedaPaciente
    val _enfermedadesPaciente = _s.enfermedadesPacienteState
    var enfermedadesPaciente by _enfermedadesPaciente
    val _prescripcionesPaciente = _s.prescripcionesPacienteState
    var prescripcionesPaciente by _prescripcionesPaciente
    val _fotoPerfilPaciente = _s.fotoPerfilPacienteState
    var fotoPerfilPaciente by _fotoPerfilPaciente
    val _cameraPermissionPerfilPending = _s.cameraPermissionPerfilPendingState
    var cameraPermissionPerfilPending by _cameraPermissionPerfilPending
    val estudiosAdjuntos = _s.estudiosAdjuntos
    var tituloInforme by _s.tituloInformeState
    var descripcionInforme by _s.descripcionInformeState
    var nombreProfesional by _s.nombreProfesionalState
    var especialidadProfesional by _s.especialidadProfesionalState
    var telefonoProfesional by _s.telefonoProfesionalState
    var tituloCitaMedica by _s.tituloCitaMedicaState
    val _profesionalCitaMedica = _s.profesionalCitaMedicaState
    var profesionalCitaMedica by _profesionalCitaMedica
    val _lugarCitaMedica = _s.lugarCitaMedicaState
    var lugarCitaMedica by _lugarCitaMedica
    val _notasCitaMedica = _s.notasCitaMedicaState
    var notasCitaMedica by _notasCitaMedica
    val _fechaCitaMedica = _s.fechaCitaMedicaState
    var fechaCitaMedica by _fechaCitaMedica
    val _recordatorioCitaMinutos = _s.recordatorioCitaMinutosState
    var recordatorioCitaMinutos by _recordatorioCitaMinutos
    val _alarmaCitaMedicaActiva = _s.alarmaCitaMedicaActivaState
    var alarmaCitaMedicaActiva by _alarmaCitaMedicaActiva
    val _ejecutandoBackupManual = _s.ejecutandoBackupManualState
    var ejecutandoBackupManual by _ejecutandoBackupManual
    val _restaurandoBackup = _s.restaurandoBackupState
    var restaurandoBackup by _restaurandoBackup
    val _backupSelection = _s.backupSelectionState
    var backupSelection by _backupSelection
    val _restoreSelection = _s.restoreSelectionState
    var restoreSelection by _restoreSelection
    val _backupPatientId = _s.backupPatientIdState
    var backupPatientId by _backupPatientId
    val _restorePatientId = _s.restorePatientIdState
    var restorePatientId by _restorePatientId
    var backupPatientDropdownExpanded by _s.backupPatientDropdownExpandedState
    var restorePatientDropdownExpanded by _s.restorePatientDropdownExpandedState
    val _cameraPermissionPending = _s.cameraPermissionPendingState
    var cameraPermissionPending by _cameraPermissionPending
    val _mostrarDialogoBackupManual = _s.mostrarDialogoBackupManualState
    var mostrarDialogoBackupManual by _mostrarDialogoBackupManual
    val _mostrarDialogoRestoreSeleccion = _s.mostrarDialogoRestoreSeleccionState
    var mostrarDialogoRestoreSeleccion by _mostrarDialogoRestoreSeleccion
    val _mostrarDialogoProgramarBackup = _s.mostrarDialogoProgramarBackupState
    var mostrarDialogoProgramarBackup by _mostrarDialogoProgramarBackup
    var mostrarDialogoCerrarInformeSinGuardar by _s.mostrarDialogoCerrarInformeSinGuardarState
    val _mensajeBackup = _s.mensajeBackupState
    var mensajeBackup by _mensajeBackup
    val _visorAdjuntos = _s.visorAdjuntosState
    var visorAdjuntos by _visorAdjuntos
    val adjuntosPendientesReemplazo = _s.adjuntosPendientesReemplazo
    var borradorInformeInicial by _s.borradorInformeInicialState
    var sistolicaInput by _s.sistolicaInputState
    var diastolicaInput by _s.diastolicaInputState
    var comentarioPresionInput by _s.comentarioPresionInputState
    var latidosInput by _s.latidosInputState
    var comentarioLatidosInput by _s.comentarioLatidosInputState
    var glucemiaInput by _s.glucemiaInputState
    var comentarioGlucemiaInput by _s.comentarioGlucemiaInputState
    var temperaturaInput by _s.temperaturaInputState
    var comentarioTemperaturaInput by _s.comentarioTemperaturaInputState
    val _pesoInput = _s.pesoInputState
    var pesoInput by _pesoInput
    val _pesoUnidadKg = _s.pesoUnidadKgState
    var pesoUnidadKg by _pesoUnidadKg
    val _tomaPendienteDeEliminar = _s.tomaPendienteDeEliminarState
    var tomaPendienteDeEliminar by _tomaPendienteDeEliminar
    val _perfilPendienteDeEliminar = _s.perfilPendienteDeEliminarState
    var perfilPendienteDeEliminar by _perfilPendienteDeEliminar
    val _mostrarFormularioCitaMedica = _s.mostrarFormularioCitaMedicaState
    var mostrarFormularioCitaMedica by _mostrarFormularioCitaMedica
    val _mostrarPanelCitasMedicas = _s.mostrarPanelCitasMedicasState
    var mostrarPanelCitasMedicas by _mostrarPanelCitasMedicas
    val _citaPendienteDeEliminar = _s.citaPendienteDeEliminarState
    var citaPendienteDeEliminar by _citaPendienteDeEliminar
    val _reportePendienteDeEliminar = _s.reportePendienteDeEliminarState
    var reportePendienteDeEliminar by _reportePendienteDeEliminar
    val _mostrarDialogoPesoSincronizado = _s.mostrarDialogoPesoSincronizadoState
    var mostrarDialogoPesoSincronizado by _mostrarDialogoPesoSincronizado
    val _mostrarDialogoMedia = _s.mostrarDialogoMediaState
    var mostrarDialogoMedia by _mostrarDialogoMedia
    var expandedNombre by _s.expandedNombreState
    var expandedFormato by _s.expandedFormatoState
    var expandedConcentracion by _s.expandedConcentracionState
    var mostrarConcentracionLibre by _s.mostrarConcentracionLibreState
    var expandedCiclo by _s.expandedCicloState
    var expandedToma by _s.expandedTomaState
    val _expandedPesoUnidad = _s.expandedPesoUnidadState
    var expandedPesoUnidad by _expandedPesoUnidad
    val _expandedEstaturaUnidad = _s.expandedEstaturaUnidadState
    var expandedEstaturaUnidad by _expandedEstaturaUnidad
    val _expandedPaisPaciente = _s.expandedPaisPacienteState
    var expandedPaisPaciente by _expandedPaisPaciente
    var expandedFrecuenciaBackup by _s.expandedFrecuenciaBackupState
    var expandedReintentoCritico by _s.expandedReintentoCriticoState
    var expandedIntentosCriticos by _s.expandedIntentosCriticosState
    val _expandedRecordatorioCita = _s.expandedRecordatorioCitaState
    var expandedRecordatorioCita by _expandedRecordatorioCita
    var expandedProfesionalInforme by _s.expandedProfesionalInformeState
    val _expandedFiltroProfesionalInformes = _s.expandedFiltroProfesionalInformesState
    var expandedFiltroProfesionalInformes by _expandedFiltroProfesionalInformes
    val _filtroProfesionalInformesId = _s.filtroProfesionalInformesIdState
    var filtroProfesionalInformesId by _filtroProfesionalInformesId
    var formatoSeleccionado by _s.formatoSeleccionadoState
    var formaInsumoSeleccionada by _s.formaInsumoSeleccionadaState
    var colorInsumoSeleccionado by _s.colorInsumoSeleccionadoState
    var colorInsumo2Seleccionado by _s.colorInsumo2SeleccionadoState
    var presentacionPersistida by _s.presentacionPersistidaState
    var concentracionSeleccionada by _s.concentracionSeleccionadaState
    var cicloSeleccionado by _s.cicloSeleccionadoState
    var tomaSeleccionada by _s.tomaSeleccionadaState
    val horasTomas = _s.horasTomas

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(context) {
        _s.alarmaSonidoUriState.value = CriticalAlertSettings.getSoundUri(context)
        _s.alarmaSonidoNombreState.value = resolveAlarmSoundLabel(context, CriticalAlertSettings.getSoundUri(context))
        _s.intervaloReintentoSeleccionadoState.value = CriticalAlertSettings.getRetryIntervalMinutes(context)
        _s.numeroIntentosCriticosSeleccionadoState.value = CriticalAlertSettings.getMaxRetryCount(context)
        _s.tienePermisoNotificacionesState.value = notificationPermissionGranted(context)
        _s.tienePermisoAlarmaExactaState.value = exactAlarmPermissionGranted(context)
        _s.tienePermisoPantallaCompletaState.value = fullScreenIntentPermissionGranted(context)
        _s.tieneAccesoNoMolestarState.value = notificationPolicyAccessGranted(context)
        _s.tienePermisoCamaraState.value = cameraPermissionGranted(context)
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            // Reprogramar todas las alarmas al iniciar la app.
            // Al reinstalar la app, Android borra todas las alarmas de AlarmManager,
            // por lo que es necesario volver a registrarlas aquí.
            try {
                val now = System.currentTimeMillis()
                val db = AppDatabase.getDatabase(context)
                val medScheduler = MedicationScheduler(context)
                db.medicationDao().obtenerActivosConAlarma().forEach { medication ->
                    medScheduler.programarAlarmas(medication)
                }

                val appointmentScheduler = MedicalAppointmentScheduler(context)
                db.medicalAppointmentDao().obtenerPendientesConAlarma(now).forEach { appointment ->
                    if (!appointment.isCompleted && appointment.scheduledAt > now) {
                        appointmentScheduler.programar(appointment)
                    }
                }

                val vaccinationScheduler = VaccinationScheduler(context)
                db.vaccinationRecordDao().obtenerPendientesConAlarma(now).forEach { record ->
                    if (record.nextDoseAt != null && record.nextDoseAt > now) {
                        vaccinationScheduler.programar(record)
                    }
                }

                val anticonceptivoScheduler = AnticonceptivoScheduler(context)
                db.metodoAnticonceptivoDao().obtenerActivos().forEach { metodo ->
                    anticonceptivoScheduler.programarAlarma(metodo)
                }

                val reminderSettings = SignosVitalesScheduler.loadSettings(context)
                val (savedPatientId, savedPatientName) = SignosVitalesScheduler.loadPatientInfo(context)
                if (reminderSettings.third && savedPatientId > 0) {
                    SignosVitalesScheduler(context).programar(savedPatientId, savedPatientName)
                }

                // Reprogramar recordatorios de hidratacion
                val (hidPatientId, hidPatientName) = HidratacionScheduler.loadPatientInfo(context)
                val hidSettings = HidratacionScheduler.loadSettings(context)
                if (hidSettings.enabled && hidPatientId > 0) {
                    HidratacionScheduler(context).programar(hidPatientId, hidPatientName)
                }

                // Reprogramar sedentarismo
                val activeProfileId = db.patientProfileDao().obtenerTodosLista().find { it.isActive }?.id ?: 0
                SedentarismoScheduler(context).programar(patientId = activeProfileId)
            } catch (_: Exception) {
            }

            NotificacionHelper.verificarYNotificarStockBajo(context)
            NotificacionHelper.verificarYNotificarTomasOlvidadas(context)
        }
    }

    val aiConfigInicial = remember(context) { MedicalAiSettings.load(context) }
    var urlServicioIa by remember(context) { mutableStateOf(aiConfigInicial.endpointUrl) }
    var modeloServicioIa by remember(context) { mutableStateOf(aiConfigInicial.modelName) }
    val _alarmaSonidoUri = _s.alarmaSonidoUriState
    var alarmaSonidoUri by _alarmaSonidoUri
    val _alarmaSonidoNombre = _s.alarmaSonidoNombreState
    var alarmaSonidoNombre by _alarmaSonidoNombre
    val _intervaloReintentoSeleccionado = _s.intervaloReintentoSeleccionadoState
    var intervaloReintentoSeleccionado by _intervaloReintentoSeleccionado
    val _numeroIntentosCriticosSeleccionado = _s.numeroIntentosCriticosSeleccionadoState
    var numeroIntentosCriticosSeleccionado by _numeroIntentosCriticosSeleccionado
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val perfilesPacientes by database.patientProfileDao().observarTodos().collectAsState(initial = emptyList())
    val pacienteActivo by database.patientProfileDao().observarPerfilActivo().collectAsState(initial = null)
    val monedaActiva = CountryCurrencyCatalog.symbolFor(
        pacienteActivo?.pais.orEmpty(),
        pacienteActivo?.moneda.orEmpty()
    )
    val embarazoActivo by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) flowOf(null)
        else database.controlEmbarazoDao().observarEmbarazoActivo(pacienteActivo!!.id)
    }.collectAsState(initial = null)
    val ninosDelPaciente by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) flowOf(emptyList<NinoEntity>())
        else database.ninoDao().getNinosByPatient(pacienteActivo!!.id)
    }.collectAsState(initial = emptyList())
    val insumosGuardadosRaw by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) {
            flowOf(emptyList())
        } else {
            database.medicationDao().observarTodosPorPaciente(pacienteActivo!!.id)
        }
    }.collectAsState(initial = emptyList())
    val insumosGuardados = insumosGuardadosRaw

    val carritoItems by remember(pacienteActivo?.id, insumosGuardados) {
        if (pacienteActivo == null) flowOf(emptyList<CarritoItem>())
        else database.carritoPendienteDao().observarPorPaciente(pacienteActivo!!.id)
            .map { pendingItems ->
                pendingItems.mapNotNull { pending ->
                    val med = insumosGuardados.find { it.id == pending.medicationId }
                    if (med != null) {
                        CarritoItem(
                            medication = med,
                            unidadesSolicitadas = pending.unidadesSolicitadas,
                            carritoRowId = pending.id
                        )
                    } else null
                }
            }
    }.collectAsState(initial = emptyList())
    val reportesSalud by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) {
            flowOf(emptyList())
        } else {
            database.medicalReportDao().observarPorPaciente(pacienteActivo!!.id)
        }
    }.collectAsState(initial = emptyList())
    val citasMedicas by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) {
            flowOf(emptyList())
        } else {
            database.medicalAppointmentDao().observarPorPaciente(pacienteActivo!!.id)
        }
    }.collectAsState(initial = emptyList())
    val profesionalesHabituales by remember(pacienteActivo?.id) {
        if (pacienteActivo == null) {
            flowOf(emptyList())
        } else {
            database.medicalPractitionerDao().observarPorPaciente(pacienteActivo!!.id)
        }
    }.collectAsState(initial = emptyList())
    val signosVitales by database.signosVitalesDao().obtenerTodos().collectAsState(initial = emptyList())
    val sugerencias = remember(nombre) { FakeVademecumRepository.buscarPorNombre(nombre) }
    val opcionesToma = listOf("En una sola toma", "En diferentes horarios")
    val ciclos = listOf("Diario", "Semanal", "Mensual", "Cada 8 horas", "Cada 12 horas", "Personalizado")
    val opcionesPesoUnidad = listOf("kg", "lb")
    val opcionesEstaturaUnidad = listOf("cm", "in")
    val opcionesFrecuenciaBackup = listOf("Solo manual", "Diario", "Semanal")
    val opcionesHoraBackup = remember {
        (0..23).flatMap { hour ->
            listOf(0, 30).map { minute ->
                String.format("%02d:%02d", hour, minute)
            }
        }
    }
    val opcionesReintentoCritico = listOf(5, 10, 15)
    val opcionesIntentosCriticos = (0..CriticalAlertSettings.MAX_ALLOWED_RETRY_COUNT).toList()
    val opcionesRecordatorioCita = listOf(15, 30, 60, 120, 1440)
    var fechaEscritorioSeleccionada by remember { mutableStateOf(inicioDelDia(System.currentTimeMillis())) }
    var frecuenciaBackupSeleccionada by remember { mutableStateOf(AutoBackupScheduler.getFrequency(context)) }
    val (horaBackupInicial, minutoBackupInicial) = remember { AutoBackupScheduler.getTime(context) }
    var horaBackupSeleccionada by remember { mutableStateOf(horaBackupInicial) }
    var minutoBackupSeleccionado by remember { mutableStateOf(minutoBackupInicial) }
    var expandedHoraBackup by remember { mutableStateOf(false) }
    var mostrarSelectorHoraBackup by remember { mutableStateOf(false) }
    val _refrescoBackup = remember { mutableStateOf(0) }
    var refrescoBackup by _refrescoBackup
    val _exportandoTomas = remember { mutableStateOf(false) }
    var exportandoTomas by _exportandoTomas
    val _periodoExportacionPendiente = remember { mutableStateOf<IntakeExportPeriod?>(null) }
    var periodoExportacionPendiente by _periodoExportacionPendiente
    val _expandedFiltroExportacionSignos = _s.expandedFiltroExportacionSignosState
    var expandedFiltroExportacionSignos by _expandedFiltroExportacionSignos
    val _filtroExportacionSignos = _s.filtroExportacionSignosState
    var filtroExportacionSignos by _filtroExportacionSignos
    val _fechaInicioExportacionSignos = _s.fechaInicioExportacionSignosState
    var fechaInicioExportacionSignos by _fechaInicioExportacionSignos
    val _fechaFinExportacionSignos = _s.fechaFinExportacionSignosState
    var fechaFinExportacionSignos by _fechaFinExportacionSignos
    val _exportandoSignosVitales = _s.exportandoSignosVitalesState
    var exportandoSignosVitales by _exportandoSignosVitales
    val _exportacionSignosPendiente = remember { mutableStateOf<VitalSignsExportRequest?>(null) }
    var exportacionSignosPendiente by _exportacionSignosPendiente
    val _restaurandoSignosVitales = remember { mutableStateOf(false) }
    var restaurandoSignosVitales by _restaurandoSignosVitales
    // Configuracion recordatorio metricas diarias
    val svReminderSettings = remember { SignosVitalesScheduler.loadSettings(context) }
    var recordatorioSignosActivo by remember { mutableStateOf(svReminderSettings.third) }
    var recordatorioSignosHora by remember { mutableStateOf(svReminderSettings.first) }
    var recordatorioSignosMinuto by remember { mutableStateOf(svReminderSettings.second) }
    var mostrarTimePickerSignos by remember { mutableStateOf(false) }
    val _exportandoReporteClinico = remember { mutableStateOf(false) }
    var exportandoReporteClinico by _exportandoReporteClinico
    val _mostrarListadoSignosGuardados = remember { mutableStateOf(false) }
    var mostrarListadoSignosGuardados by _mostrarListadoSignosGuardados
    val _mostrarListadoSignosPanel = remember { mutableStateOf(false) }
    var mostrarListadoSignosPanel by _mostrarListadoSignosPanel
    val mesesExpandidosSignos = remember { mutableStateListOf<String>() }
    val _mostrarVistaPreviaSignosSeleccionados = _s.mostrarVistaPreviaSignosSeleccionadosState
    var mostrarVistaPreviaSignosSeleccionados by _mostrarVistaPreviaSignosSeleccionados
    val registrosSignosSeleccionados = remember { mutableStateListOf<Int>() }
    val signosVitalesSeleccionados = signosVitales
        .filter { registrosSignosSeleccionados.contains(it.id) }
        .sortedBy { it.fechaRegistro }
    val fechaBaseEscritorio = remember { inicioDelDia(System.currentTimeMillis()) }
    val paginaBaseEscritorio = remember { 5_000 }
    val pagerEscritorioState = rememberPagerState(
        initialPage = paginaBaseEscritorio,
        pageCount = { 10_000 }
    )
    val fechaActualTexto = remember(fechaEscritorioSeleccionada) { formatDashboardDate(fechaEscritorioSeleccionada) }
    val fechaResumenEscritorioTexto = remember(fechaEscritorioSeleccionada) { formatDashboardDateSummary(fechaEscritorioSeleccionada) }
    val escritorioEsHoy = remember(fechaEscritorioSeleccionada) {
        inicioDelDia(fechaEscritorioSeleccionada) == inicioDelDia(System.currentTimeMillis())
    }
    val fechaNacimientoTexto by derivedStateOf {
        fechaNacimientoPaciente?.let { formatDate(it) }.orEmpty()
    }
    val edadCalculadaPaciente = remember(fechaNacimientoPaciente) {
        fechaNacimientoPaciente?.let { calcularEdadDesdeNacimiento(it).toString() }.orEmpty()
    }
    val ultimoBackupAutomatico = remember(refrescoBackup) {
        BackupManager.latestAutomaticBackupFile(context)
    }
    LaunchedEffect(pagerEscritorioState.currentPage, fechaBaseEscritorio, paginaBaseEscritorio) {
        val fechaDesdePager = moverFecha(
            fechaBaseEscritorio,
            pagerEscritorioState.currentPage - paginaBaseEscritorio
        )
        if (inicioDelDia(fechaEscritorioSeleccionada) != inicioDelDia(fechaDesdePager)) {
            fechaEscritorioSeleccionada = fechaDesdePager
        }
    }

    LaunchedEffect(launchIntent) {
        if (launchIntent?.getBooleanExtra(NotificacionHelper.EXTRA_OPEN_SIGNOS_VITALES, false) == true) {
            cerrarPanelesSecundariosWithState(_s, { mostrarListadoSignosPanel = it }, mesesExpandidosSignos, fallAlertPanelState)
            mostrarPanelSignosVitales = true
        }

        val pedirMedicationId = launchIntent?.getIntExtra(NotificacionHelper.EXTRA_PEDIR_MEDICATION_ID, 0) ?: 0
        if (pedirMedicationId > 0) {
            cerrarPanelesSecundariosWithState(_s, { mostrarListadoSignosPanel = it }, mesesExpandidosSignos, fallAlertPanelState)
            mostrarListaInsumos = true
            coroutineScope.launch(Dispatchers.IO) {
                val med = database.medicationDao().findById(pedirMedicationId)
                if (med != null) {
                    withContext(Dispatchers.Main) {
                        inputUnidadesPedido = ""
                        insumoAPedir = med
                    }
                }
            }
        } else if (launchIntent?.getBooleanExtra(NotificacionHelper.EXTRA_OPEN_LISTA_INSUMOS, false) == true) {
            cerrarPanelesSecundariosWithState(_s, { mostrarListadoSignosPanel = it }, mesesExpandidosSignos, fallAlertPanelState)
            mostrarListaInsumos = true
        }
    }
    val _tienePermisoNotificaciones = _s.tienePermisoNotificacionesState
    var tienePermisoNotificaciones by _tienePermisoNotificaciones
    val _tienePermisoAlarmaExacta = _s.tienePermisoAlarmaExactaState
    var tienePermisoAlarmaExacta by _tienePermisoAlarmaExacta
    val _tienePermisoPantallaCompleta = _s.tienePermisoPantallaCompletaState
    var tienePermisoPantallaCompleta by _tienePermisoPantallaCompleta
    val _tienePermisoCamara = _s.tienePermisoCamaraState
    var tienePermisoCamara by _tienePermisoCamara
    val _tieneAccesoNoMolestar = _s.tieneAccesoNoMolestarState
    var tieneAccesoNoMolestar by _tieneAccesoNoMolestar
    val documentScannerOptions = remember {
        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setGalleryImportAllowed(false)
            .build()
    }
    val documentScanner = remember { com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(documentScannerOptions) }
    val launchers = rememberMedicamentoFormLaunchers(
        context = context,
        coroutineScope = coroutineScope,
        tienePermisoNotificacionesState = _s.tienePermisoNotificacionesState,
        tienePermisoAlarmaExactaState = _s.tienePermisoAlarmaExactaState,
        tienePermisoPantallaCompletaState = _s.tienePermisoPantallaCompletaState,
        tienePermisoCamaraState = _s.tienePermisoCamaraState,
        tieneAccesoNoMolestarState = _s.tieneAccesoNoMolestarState,
        fotoPerfilPacienteState = _fotoPerfilPaciente,
        cameraPermissionPendingState = _cameraPermissionPending,
        cameraPermissionPerfilPendingState = _cameraPermissionPerfilPending,
        estudiosAdjuntos = estudiosAdjuntos,
        adjuntosPendientesReemplazo = adjuntosPendientesReemplazo,
        ejecutandoBackupManualState = _ejecutandoBackupManual,
        restaurandoBackupState = _restaurandoBackup,
        backupSelectionState = _backupSelection,
        backupPatientIdState = _backupPatientId,
        restoreSelectionState = _restoreSelection,
        restorePatientIdState = _restorePatientId,
        refrescoBackupState = _refrescoBackup,
        mensajeBackupState = _mensajeBackup,
        mostrarFichaPacienteState = _mostrarFichaPaciente,
        mostrarFormularioInformeState = _mostrarFormularioInforme,
        formularioInformeAutoAbiertoState = _s.formularioInformeAutoAbiertoState,
        mostrarPanelPacientesState = _mostrarPanelPacientes,
        mostrarPanelInformesState = _mostrarPanelInformes,
        mostrarListaInsumosState = _mostrarListaInsumos,
        mostrarPanelBackupsState = _mostrarPanelBackups,
        mostrarPanelConfiguracionAlertasState = _mostrarPanelConfiguracionAlertas,
        mostrarPanelSignosVitalesState = _mostrarPanelSignosVitales,
        mostrarPanelConfiguracionIaState = _mostrarPanelConfiguracionIa,
        mostrarFormularioState = _mostrarFormulario,
        intervaloReintentoSeleccionadoState = _intervaloReintentoSeleccionado,
        numeroIntentosCriticosSeleccionadoState = _numeroIntentosCriticosSeleccionado,
        alarmaSonidoUriState = _alarmaSonidoUri,
        alarmaSonidoNombreState = _alarmaSonidoNombre,
        periodoExportacionPendienteState = _periodoExportacionPendiente,
        exportandoTomasState = _exportandoTomas,
        exportacionSignosPendienteState = _exportacionSignosPendiente,
        exportandoSignosVitalesState = _exportandoSignosVitales,
        exportandoReporteClinicoState = _exportandoReporteClinico,
        restaurandoSignosVitalesState = _restaurandoSignosVitales,
        pacienteActivo = pacienteActivo,
        database = database,
        fechaEscritorioSeleccionada = fechaEscritorioSeleccionada,
        documentScannerInstance = documentScanner
    )
    val notificationPermissionLauncher = launchers.notificationPermissionLauncher
    val exactAlarmPermissionLauncher = launchers.exactAlarmPermissionLauncher
    val fullScreenIntentPermissionLauncher = launchers.fullScreenIntentPermissionLauncher
    val notificationPolicyAccessLauncher = launchers.notificationPolicyAccessLauncher
    val pickStudyImagesLauncher = launchers.pickStudyImagesLauncher
    val scannerLauncher = launchers.scannerLauncher
    val takeStudyPictureLauncher = launchers.takeStudyPictureLauncher
    val cameraPermissionLauncher = launchers.cameraPermissionLauncher
    val pickFotoPerfilLauncher = launchers.pickFotoPerfilLauncher
    val pickRestoreSignosLauncher = launchers.pickRestoreSignosLauncher
    val takeFotoPerfilLauncher = launchers.takeFotoPerfilLauncher
    val cameraPermissionPerfilLauncher = launchers.cameraPermissionPerfilLauncher
    val createBackupDocumentLauncher = launchers.createBackupDocumentLauncher
    val restoreBackupDocumentLauncher = launchers.restoreBackupDocumentLauncher
    val exportMedicationReportLauncher = launchers.exportMedicationReportLauncher
    val exportVitalSignsReportLauncher = launchers.exportVitalSignsReportLauncher
    val exportClinicalReportLauncher = launchers.exportClinicalReportLauncher
    val ringtonePickerLauncher = launchers.ringtonePickerLauncher

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tienePermisoNotificaciones = notificationPermissionGranted(context)
                tienePermisoAlarmaExacta = exactAlarmPermissionGranted(context)
                tienePermisoPantallaCompleta = fullScreenIntentPermissionGranted(context)
                tienePermisoCamara = cameraPermissionGranted(context)
                tieneAccesoNoMolestar = notificationPolicyAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun launchDocumentScanner() {
        val activity = context as? androidx.activity.ComponentActivity
        if (activity != null) {
            documentScanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    launchers.scannerLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(context, "Escaner no disponible", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun resetForm() = resetFormWithState(_s)

    fun cerrarPanelesSecundarios() = cerrarPanelesSecundariosWithState(
        _s,
        setMostrarListadoSignosPanel = { mostrarListadoSignosPanel = it },
        mesesExpandidosSignos = mesesExpandidosSignos,
        fallAlertPanelState = fallAlertPanelState
    )

    fun guardarConfiguracionAlertasCriticas() = guardarConfiguracionAlertasCriticasAction(
        context = context,
        intervaloReintentoSeleccionado = intervaloReintentoSeleccionado,
        numeroIntentosCriticosSeleccionado = numeroIntentosCriticosSeleccionado,
        alarmaSonidoUri = alarmaSonidoUri,
        setAlarmaSonidoNombre = { alarmaSonidoNombre = it },
        resolveAlarmSoundLabel = ::resolveAlarmSoundLabel
    )

    fun guardarConfiguracionIa() = guardarConfiguracionIaAction(context, urlServicioIa, modeloServicioIa)

    fun resetFichaPaciente() = resetFichaPacienteWithState(_s)

    fun resetCitaMedica() = resetCitaMedicaWithState(_s)

    fun resetMedicoHabitual() = resetMedicoHabitualWithState(_s)

    fun cargarMedicoHabitual(practitioner: MedicalPractitioner) {
        editingPractitionerId = practitioner.id
        nombreProfesional = practitioner.name
        especialidadProfesional = practitioner.specialty
        telefonoProfesional = practitioner.phone
    }

    fun abrirFormularioMedico(practitioner: MedicalPractitioner? = null) {
        if (practitioner == null) {
            resetMedicoHabitual()
        } else {
            cargarMedicoHabitual(practitioner)
            profesionalSeleccionadoId = practitioner.id
        }
        mostrarPanelProfesionales = false
        mostrarFormulario = false
        mostrarFormularioInforme = false
        mostrarPanelInformes = false
        mostrarPanelCitasMedicas = false
        mostrarFormularioInforme = false
        mostrarListaInsumos = false
        mostrarPanelPacientes = false
        mostrarFormularioProfesional = true
    }

    fun guardarMedicoHabitualActual() = guardarMedicoHabitualActualAction(
        context = context, pacienteActivo = pacienteActivo,
        nombreProfesional = nombreProfesional,
        especialidadProfesional = especialidadProfesional,
        telefonoProfesional = telefonoProfesional,
        editingPractitionerId = editingPractitionerId,
        profesionalesHabituales = profesionalesHabituales,
        database = database, coroutineScope = coroutineScope,
        setProfesionalSeleccionadoId = { profesionalSeleccionadoId = it },
        setEditingPractitionerId = { editingPractitionerId = it },
        setNombreProfesional = { nombreProfesional = it },
        setEspecialidadProfesional = { especialidadProfesional = it },
        setTelefonoProfesional = { telefonoProfesional = it },
        setMostrarFormularioProfesional = { mostrarFormularioProfesional = it },
        setMostrarPanelProfesionales = { mostrarPanelProfesionales = it }
    )

    fun cargarCitaMedica(appointment: MedicalAppointment) {
        editingAppointmentId = appointment.id
        citaMedicaSeleccionadaId = appointment.id
        tituloCitaMedica = appointment.title
        profesionalCitaMedica = appointment.doctorName
        lugarCitaMedica = appointment.location
        notasCitaMedica = appointment.notes
        fechaCitaMedica = appointment.scheduledAt
        recordatorioCitaMinutos = appointment.reminderMinutes
        alarmaCitaMedicaActiva = appointment.alarmEnabled
        expandedRecordatorioCita = false
    }

    fun abrirFormularioCitaMedica(appointment: MedicalAppointment? = null) {
        if (appointment == null) {
            resetCitaMedica()
        } else {
            cargarCitaMedica(appointment)
        }
        mostrarPanelCitasMedicas = false
        mostrarFormularioCitaMedica = true
    }

    fun guardarCitaMedicaActual() = guardarCitaMedicaActualAction(
        context = context, pacienteActivo = pacienteActivo,
        profesionalCitaMedica = profesionalCitaMedica,
        profesionalesHabituales = profesionalesHabituales,
        citasMedicas = citasMedicas, editingAppointmentId = editingAppointmentId,
        tituloCitaMedica = tituloCitaMedica, lugarCitaMedica = lugarCitaMedica,
        notasCitaMedica = notasCitaMedica, fechaCitaMedica = fechaCitaMedica,
        recordatorioCitaMinutos = recordatorioCitaMinutos,
        alarmaCitaMedicaActiva = alarmaCitaMedicaActiva,
        database = database, coroutineScope = coroutineScope,
        onResetCitaMedica = { resetCitaMedica() },
        setMostrarFormularioCitaMedica = { mostrarFormularioCitaMedica = it },
        setMostrarPanelCitasMedicas = { mostrarPanelCitasMedicas = it }
    )

    fun cargarFichaPaciente(profile: PatientProfile) = cargarFichaPacienteWithState(_s, profile, ::calcularEdadDesdeNacimiento)

    fun abrirFichaPaciente(profile: PatientProfile, enEdicion: Boolean = false) {
        cargarFichaPaciente(profile)
        cerrarPanelesSecundarios()
        editandoFichaPaciente = enEdicion
        mostrarFichaPaciente = true
    }

    fun abrirNuevaFichaPaciente() {
        cerrarPanelesSecundarios()
        resetFichaPaciente()
        editandoFichaPaciente = true
        mostrarFichaPaciente = true
    }

    fun cancelarFichaPaciente() {
        val profileActual = perfilesPacientes.firstOrNull { it.id == editingPatientId }
        if (profileActual != null) {
            cargarFichaPaciente(profileActual)
            editandoFichaPaciente = false
            mostrarFichaPaciente = true
        } else {
            resetFichaPaciente()
            cerrarPanelesSecundarios()
        }
    }

    fun volverPantallaAnteriorDesdeFichaPaciente() {
        val profileActual = perfilesPacientes.firstOrNull { it.id == editingPatientId }
        if (profileActual != null) {
            cargarFichaPaciente(profileActual)
        } else {
            resetFichaPaciente()
        }
        editandoFichaPaciente = false
        cerrarPanelesSecundarios()
    }

    fun resetInformeMedico() = resetInformeMedicoAction(
        setEditingReportId = { editingReportId = it },
        setPractitionerIdInforme = { practitionerIdInforme = it },
        setTituloInforme = { tituloInforme = it },
        setDescripcionInforme = { descripcionInforme = it },
        estudiosAdjuntos = estudiosAdjuntos,
        setBorradorInformeInicial = { borradorInformeInicial = it }
    )

    fun cargarInformeMedico(report: MedicalReport) {
        editingReportId = report.id
        practitionerIdInforme = report.practitionerId
        tituloInforme = report.titulo
        descripcionInforme = report.descripcion
        estudiosAdjuntos.clear()
        estudiosAdjuntos.addAll(report.adjuntos.split("|").filter { it.isNotBlank() })
        borradorInformeInicial = ReportDraftSnapshot(
            reportId = report.id,
            practitionerId = report.practitionerId,
            titulo = report.titulo,
            descripcion = report.descripcion,
            adjuntos = estudiosAdjuntos.toList()
        )
        mostrarPanelInformes = false
        mostrarFormularioInforme = true
    }

    fun informeMedicoTieneCambiosSinGuardar(): Boolean {
        return ReportDraftSnapshot(
            reportId = editingReportId,
            practitionerId = practitionerIdInforme,
            titulo = tituloInforme,
            descripcion = descripcionInforme,
            adjuntos = estudiosAdjuntos.toList()
        ) != borradorInformeInicial
    }

    fun cerrarFormularioInforme() {
        resetInformeMedico()
        mostrarDialogoCerrarInformeSinGuardar = false
        mostrarFormularioInforme = false
        mostrarPanelInformes = true
    }

    fun guardarInformeMedicoActual() = guardarInformeMedicoActualAction(
        context = context, pacienteActivo = pacienteActivo,
        editingReportId = editingReportId, practitionerIdInforme = practitionerIdInforme,
        tituloInforme = tituloInforme, descripcionInforme = descripcionInforme,
        estudiosAdjuntos = estudiosAdjuntos,
        database = database, coroutineScope = coroutineScope,
        onCerrarFormularioInforme = { cerrarFormularioInforme() }
    )

    fun cargarMedicamentoEnFormulario(medication: Medication) = cargarMedicamentoEnFormularioWithState(_s, medication)
    val calendar = remember { Calendar.getInstance() }

    LaunchedEffect(nombre) {
        val exacto = FakeVademecumRepository.obtenerExacto(nombre)
        if (exacto != null) {
            selectedMedication = exacto
            if (editingMedicationId == null && formatoSeleccionado !in exacto.formatos) {
                formatoSeleccionado = exacto.formatos.firstOrNull().orEmpty()
            }
            if (presentacionPersistida.isBlank()) {
                presentacionPersistida = exacto.presentaciones.firstOrNull().orEmpty()
            }
            if (editingMedicationId == null && concentracionSeleccionada !in exacto.concentraciones) {
                concentracionSeleccionada = exacto.concentraciones.firstOrNull().orEmpty()
            }
        } else {
            selectedMedication = null
            if (editingMedicationId == null) {
                formatoSeleccionado = ""
                concentracionSeleccionada = ""
            }
        }
    }

    LaunchedEffect(cantidad, tomaSeleccionada) {
        val cantidadInt = cantidad.toIntOrNull() ?: 0
        val target = if (cantidadInt > 1 && tomaSeleccionada == "En diferentes horarios") {
            cantidadInt.coerceAtMost(5)
        } else {
            0
        }

        while (horasTomas.size < target) {
            horasTomas.add("")
        }
        while (horasTomas.size > target) {
            horasTomas.removeAt(horasTomas.lastIndex)
        }
    }

    LaunchedEffect(pacienteActivo?.id, mostrarFichaPaciente, editandoFichaPaciente) {
        if (!mostrarFichaPaciente && pacienteActivo != null) {
            cargarFichaPaciente(pacienteActivo!!)
        }
    }

    LaunchedEffect(fechaNacimientoPaciente) {
        if (editandoFichaPaciente) {
            edadPaciente = fechaNacimientoPaciente?.let { calcularEdadDesdeNacimiento(it).toString() }.orEmpty()
        }
    }

    LaunchedEffect(pacienteActivo?.id) {
        resetCitaMedica()
    }

    LaunchedEffect(insumosGuardados) {
        val duplicados = insumosGuardados
            .groupBy { duplicateSignature(it) }
            .values
            .flatMap { grupo -> grupo.sortedBy { it.id }.drop(1) }

        if (duplicados.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                duplicados.forEach { medication ->
                    database.medicationDao().eliminar(medication)
                    MedicationScheduler(context).cancelarAlarma(medication.id)
                }
            }
            Toast.makeText(
                context,
                "Se eliminaron ${duplicados.size} medicamentos duplicados",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    MedicamentoFormBody(
        modifier = modifier,
        s = _s,
        fallAlertPanelState = fallAlertPanelState,
        mostrarListadoSignosPanelState = _mostrarListadoSignosPanel,
        mesesExpandidosSignos = mesesExpandidosSignos,
        registrosSignosSeleccionados = registrosSignosSeleccionados,
        mostrarListadoSignosGuardadosState = _mostrarListadoSignosGuardados,
        perfilesPacientes = perfilesPacientes,
        pacienteActivo = pacienteActivo,
        insumosGuardados = insumosGuardados,
        carritoItems = carritoItems,
        reportesSalud = reportesSalud,
        citasMedicas = citasMedicas,
        profesionalesHabituales = profesionalesHabituales,
        signosVitales = signosVitales,
        embarazoActivo = embarazoActivo,
        ninosDelPaciente = ninosDelPaciente,
        monedaActiva = monedaActiva,
        sugerencias = sugerencias,
        pagerEscritorioState = pagerEscritorioState,
        paginaBaseEscritorio = paginaBaseEscritorio,
        fechaBaseEscritorio = fechaBaseEscritorio,
        fechaResumenEscritorioTexto = fechaResumenEscritorioTexto,
        escritorioEsHoy = escritorioEsHoy,
        edadCalculadaPaciente = edadCalculadaPaciente,
        ultimoBackupAutomatico = ultimoBackupAutomatico,
        opcionesToma = opcionesToma,
        ciclos = ciclos,
        opcionesPesoUnidad = opcionesPesoUnidad,
        opcionesEstaturaUnidad = opcionesEstaturaUnidad,
        opcionesFrecuenciaBackup = opcionesFrecuenciaBackup,
        opcionesHoraBackup = opcionesHoraBackup,
        opcionesReintentoCritico = opcionesReintentoCritico,
        opcionesIntentosCriticos = opcionesIntentosCriticos,
        opcionesRecordatorioCita = opcionesRecordatorioCita,
        frecuenciaBackupSeleccionada = frecuenciaBackupSeleccionada,
        horaBackupSeleccionada = horaBackupSeleccionada,
        minutoBackupSeleccionado = minutoBackupSeleccionado,
        expandedHoraBackup = expandedHoraBackup,
        urlServicioIa = urlServicioIa,
        modeloServicioIa = modeloServicioIa,
        recordatorioSignosActivo = recordatorioSignosActivo,
        recordatorioSignosHora = recordatorioSignosHora,
        recordatorioSignosMinuto = recordatorioSignosMinuto,
        mostrarTimePickerSignos = mostrarTimePickerSignos,
        fechaEscritorioSeleccionada = fechaEscritorioSeleccionada,
        signosVitalesSeleccionados = signosVitalesSeleccionados,
        exportandoTomasState = _exportandoTomas,
        periodoExportacionPendienteState = _periodoExportacionPendiente,
        exportacionSignosPendienteState = _exportacionSignosPendiente,
        database = database,
        coroutineScope = coroutineScope,
        launchers = launchers,
        callbacks = MedicamentoFormCallbacks(
            onCerrarPanelesSecundarios = { cerrarPanelesSecundarios() },
            onAbrirNuevaFichaPaciente = { abrirNuevaFichaPaciente() },
            onAbrirFichaPaciente = { p, e -> abrirFichaPaciente(p, enEdicion = e) },
            onCargarFichaPaciente = { cargarFichaPaciente(it) },
            onVolverPantallaAnteriorDesdeFichaPaciente = { volverPantallaAnteriorDesdeFichaPaciente() },
            onResetForm = { resetForm() },
            onResetFichaPaciente = { resetFichaPaciente() },
            onCargarMedicamentoEnFormulario = { cargarMedicamentoEnFormulario(it) },
            onGuardarInformeMedicoActual = { guardarInformeMedicoActual() },
            onCerrarFormularioInforme = { cerrarFormularioInforme() },
            onDeleteAttachmentFile = { deleteAttachmentFile(it) },
            onGuardarConfiguracionAlertasCriticas = { guardarConfiguracionAlertasCriticas() },
            onGuardarConfiguracionIa = { guardarConfiguracionIa() },
            onTimestampArchivo = { timestampArchivo() },
            onCalcularEdadDesdeNacimiento = { calcularEdadDesdeNacimiento(it) },
            onSavePersistedBirthday = { ctx, id, ts -> savePersistedBirthday(ctx, id, ts) },
            onLoadPersistedBirthday = { ctx, id -> loadPersistedBirthday(ctx, id) },
            onClearPersistedBirthday = { ctx, id -> clearPersistedBirthday(ctx, id) },
            onRequestBirthdayPreview = onRequestBirthdayPreview,
            onResetInformeMedico = { resetInformeMedico() },
            onCargarInformeMedico = { cargarInformeMedico(it) },
            onGuardarMedicoHabitualActual = { guardarMedicoHabitualActual() },
            onResetMedicoHabitual = { resetMedicoHabitual() },
            onAbrirFormularioCitaMedica = { abrirFormularioCitaMedica(it) },
            onFormatDate = { formatDate(it) },
            onFormatReminderMinutesLabel = { formatReminderMinutesLabel(it) },
            onInformeMedicoTieneCambiosSinGuardar = { informeMedicoTieneCambiosSinGuardar() },
            onLaunchDocumentScanner = { launchDocumentScanner() },
            onFormatHour = { formatHour(it) },
            onMoverFecha = { base, delta -> moverFecha(base, delta) },
            onResolveAlarmSoundLabel = { ctx, uri -> resolveAlarmSoundLabel(ctx, uri) },
            onMostrarPanelSignosVitalesInit = {
                _s.sistolicaInputState.value = ""; _s.diastolicaInputState.value = ""
                _s.comentarioPresionInputState.value = ""; _s.latidosInputState.value = ""
                _s.comentarioLatidosInputState.value = ""; _s.glucemiaInputState.value = ""
                _s.comentarioGlucemiaInputState.value = ""; _s.temperaturaInputState.value = ""
                _s.comentarioTemperaturaInputState.value = ""
                _s.filtroExportacionSignosState.value = VitalSignsExportFilter.TODAY
                _s.fechaInicioExportacionSignosState.value = inicioDelDia(System.currentTimeMillis())
                _s.fechaFinExportacionSignosState.value = finDelDia(System.currentTimeMillis())
                registrosSignosSeleccionados.clear()
                _mostrarListadoSignosGuardados.value = false
                _s.mostrarPanelSignosVitalesState.value = true
            },
            onFrecuenciaBackupChange = { frecuenciaBackupSeleccionada = it },
            onExpandedHoraBackupChange = { expandedHoraBackup = it },
            onHoraBackupChange = { horaBackupSeleccionada = it },
            onMinutoBackupChange = { minutoBackupSeleccionado = it },
            onUrlServicioIaChange = { urlServicioIa = it },
            onModeloServicioIaChange = { modeloServicioIa = it },
            onRecordatorioSignosActivoChange = { recordatorioSignosActivo = it },
            onRecordatorioSignosHoraChange = { recordatorioSignosHora = it },
            onRecordatorioSignosMinutoChange = { recordatorioSignosMinuto = it },
            onMostrarTimePickerSignosChange = { mostrarTimePickerSignos = it }
        )
    )
}


