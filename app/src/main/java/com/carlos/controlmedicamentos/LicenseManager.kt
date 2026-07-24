package com.carlos.controlmedicamentos

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Gestiona la caducidad de la aplicación a partir de la fecha de instalación.
 * La duración se configura en build.gradle.kts mediante la propiedad
 * APP_EXPIRATION_DAYS (por defecto 3650 días, es decir, ~10 años).
 *
 * Para generar un APK de prueba de 6 meses:
 *   ./gradlew assembleRelease -PAPP_EXPIRATION_DAYS=180
 *
 * Para generar el APK de pago con 1 año de uso:
 *   ./gradlew assembleRelease -PAPP_EXPIRATION_DAYS=365
 *
 * Al instalar la actualización de pago sobre la versión de prueba, la base de datos
 * y todos los datos se conservan porque comparten el mismo applicationId.
 */
object LicenseManager {

    private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

    /** Fecha de instalación del APK (timestamp en ms). */
    private fun installDateMillis(context: Context): Long {
        return try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            BuildConfig.BUILD_TIMESTAMP
        }
    }

    /** Fecha de caducidad de este APK (timestamp en ms). */
    fun expirationDateMillis(context: Context): Long {
        return installDateMillis(context) + (BuildConfig.APP_EXPIRATION_DAYS * MS_PER_DAY)
    }

    /** Devuelve true si el periodo de uso ha expirado. */
    fun isExpired(context: Context): Boolean {
        return System.currentTimeMillis() > expirationDateMillis(context)
    }

    /** Días restantes de uso (0 si ya expiró). */
    fun remainingDays(context: Context): Long {
        val rem = expirationDateMillis(context) - System.currentTimeMillis()
        return (rem / MS_PER_DAY).coerceAtLeast(0L)
    }
}
