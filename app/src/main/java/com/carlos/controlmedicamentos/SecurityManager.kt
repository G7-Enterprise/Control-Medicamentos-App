package com.carlos.controlmedicamentos

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Base64
import java.io.File
import java.security.MessageDigest

/**
 * Comprobaciones de seguridad para detectar:
 *  - Depuradores adjuntos (anti-debugging)
 *  - APK modificado / firmante incorrecto (anti-tampering)
 *  - Dispositivos con root (informativo, no bloquea)
 *  - Emuladores (informativo)
 *
 * IMPORTANTE: después de generar el APK/AAB de Release con tu keystore definitivo,
 * ejecuta el siguiente comando y pega el hash SHA-256 en EXPECTED_CERT_SHA256:
 *
 *   keytool -printcert -jarfile tu-release.aab | grep "SHA256"
 *
 * Mientras EXPECTED_CERT_SHA256 sea "CONFIGURE_ME" la comprobación de firma
 * queda desactivada para no bloquear builds de debug.
 */
object SecurityManager {

    // ---------------------------------------------------------------
    // CONFIGURA ESTE VALOR con el hash SHA-256 de tu release keystore
    // (Base64 de SHA-256 del certificado). Mientras sea "CONFIGURE_ME",
    // la comprobación de firma queda desactivada.
    private const val EXPECTED_CERT_SHA256 = "CONFIGURE_ME"
    private const val EXPECTED_PACKAGE     = "com.carlos.controlmedicamentos"

    // ---------------------------------------------------------------
    // Punto de entrada principal
    // ---------------------------------------------------------------

    enum class ThreatLevel { SAFE, DEBUGGER_DETECTED, TAMPERED, ROOTED, EMULATOR }

    /**
     * Ejecuta todas las comprobaciones y devuelve el primer problema encontrado.
     * Llama esto en onCreate() antes de mostrar el contenido real de la app.
     * @param skipRootEmulator Si true, no bloquea por root/emulador (para distribución general)
     * @param skipAllChecks Si true, desactiva todas las verificaciones (para builds VIP)
     */
    fun assess(context: Context, skipRootEmulator: Boolean = false, skipAllChecks: Boolean = false): ThreatLevel {
        if (skipAllChecks) return ThreatLevel.SAFE
        if (isDebuggerAttached())          return ThreatLevel.DEBUGGER_DETECTED
        if (isPackageTampered(context))    return ThreatLevel.TAMPERED
        if (!skipRootEmulator) {
            if (isRooted())                return ThreatLevel.ROOTED
            if (isEmulator())              return ThreatLevel.EMULATOR
        }
        return ThreatLevel.SAFE
    }

    // ---------------------------------------------------------------
    // Anti-debugging
    // ---------------------------------------------------------------

    fun isDebuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    // ---------------------------------------------------------------
    // Anti-tampering: verifica la firma del APK instalado
    // ---------------------------------------------------------------

    fun isPackageTampered(context: Context): Boolean {
        // Si aún no se ha configurado el hash esperado, no bloqueamos
        if (EXPECTED_CERT_SHA256 == "CONFIGURE_ME") return false

        return try {
            // Comprobación extra: el paquete debe coincidir exactamente
            if (context.packageName != EXPECTED_PACKAGE) return true

            val signatures = getAppSignatures(context) ?: return true
            if (signatures.isEmpty()) return true

            val md = MessageDigest.getInstance("SHA-256")
            val actualHash = Base64.encodeToString(
                md.digest(signatures[0].toByteArray()),
                Base64.NO_WRAP
            )

            actualHash != EXPECTED_CERT_SHA256
        } catch (_: Exception) {
            // En caso de error no bloqueamos (mejor falso negativo que falso positivo)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getAppSignatures(context: Context): Array<android.content.pm.Signature>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo?.apkContentsSigners
        } else {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            info.signatures
        }
    }

    // ---------------------------------------------------------------
    // Detección de root (informativo — no bloquea por defecto)
    // ---------------------------------------------------------------

    fun isRooted(): Boolean {
        val suPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su"
        )
        if (suPaths.any { File(it).exists() }) return true

        val buildTags = Build.TAGS
        if (!buildTags.isNullOrEmpty() && buildTags.contains("test-keys")) return true

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            process.inputStream.bufferedReader().readLine() != null
        } catch (_: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------
    // Detección de emulador (informativo — no bloquea por defecto)
    // ---------------------------------------------------------------

    fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk", ignoreCase = true)
        || Build.MODEL.contains("Emulator", ignoreCase = true)
        || Build.MODEL.contains("Android SDK built for x86", ignoreCase = true)
        || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || Build.PRODUCT in listOf("google_sdk", "sdk", "sdk_x86", "sdk_gphone_x86", "vbox86p")
}
