package com.carlos.controlmedicamentos

import android.content.Context
import android.provider.Settings

/**
 * Constantes y utilidades relacionadas con la licencia de la aplicación.
 *
 * La validación principal ahora se realiza mediante [com.carlos.controlmedicamentos.license.LicenseRepository]
 * y Firestore. Este objeto conserva únicamente la URL de pago y helpers comunes.
 */
object LicenseManager {

    /** Enlace real de pago del producto en Lemon Squeezy. */
    const val URL_LICENCIA = "https://g7-enterprise.lemonsqueezy.com/checkout/buy/cc8aeac9-a916-40e1-bcad-bdaeed690925"

    /**
     * Identificador único del dispositivo basado en [Settings.Secure.ANDROID_ID].
     * No requiere permisos de telefonía.
     */
    fun deviceId(context: Context): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }
    }

    fun formatDate(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR)
        )
    }
}
