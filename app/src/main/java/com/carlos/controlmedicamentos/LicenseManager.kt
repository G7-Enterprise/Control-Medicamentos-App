package com.carlos.controlmedicamentos

import android.content.Context

/**
 * Gestiona la caducidad de la aplicación a partir de la fecha de compilación.
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

    /** Fecha de caducidad de este APK (timestamp en ms). */
    fun expirationDateMillis(): Long {
        return BuildConfig.BUILD_TIMESTAMP + (BuildConfig.APP_EXPIRATION_DAYS * MS_PER_DAY)
    }

    /** Devuelve true si el periodo de uso ha expirado. */
    fun isExpired(context: Context): Boolean {
        return System.currentTimeMillis() > expirationDateMillis()
    }

    /** Días restantes de uso (0 si ya expiró). */
    fun remainingDays(context: Context): Long {
        val rem = expirationDateMillis() - System.currentTimeMillis()
        return (rem / MS_PER_DAY).coerceAtLeast(0L)
    }
}
