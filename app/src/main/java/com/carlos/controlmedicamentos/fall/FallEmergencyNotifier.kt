package com.carlos.controlmedicamentos.fall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

object FallEmergencyNotifier {

    private const val TAG = "FallEmergencyNotifier"
    private const val PREFS_NAME = "fall_alert_prefs"
    private const val KEY_SMS_MESSAGE = "sms_message"
    private const val LOCATION_TIMEOUT_MS = 12_000L
    private const val MAX_LOCATION_AGE_MS = 3 * 60 * 1000L

    const val DEFAULT_SMS_MESSAGE =
        "ALERTA DE CA�DA\n\n" +
            "Se ha detectado una posible ca�da. Por favor verifica la situaci�n inmediatamente.\n\n" +
            "Abrir ubicaci�n en Google Maps:\n{maps}\n\n" +
            "Coordenadas: {lat}, {lon}\n" +
            "Magnitud del impacto: {magnitude} m/s�"

    fun buildGoogleMapsUrl(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/maps?q=$latitude,$longitude"
    }

    fun buildEmergencyMessage(
        customTemplate: String?,
        location: Location?,
        impactMagnitude: Float
    ): String {
        val lat = location?.let { "%.5f".format(it.latitude) } ?: "N/A"
        val lon = location?.let { "%.5f".format(it.longitude) } ?: "N/A"
        val magnitude = "%.2f".format(impactMagnitude)
        val mapsLink = location?.let { buildGoogleMapsUrl(it.latitude, it.longitude) }
            ?: "ubicaci�n no disponible"

        val template = customTemplate?.takeIf { it.isNotBlank() } ?: DEFAULT_SMS_MESSAGE
        var message = template
            .replace("{lat}", lat)
            .replace("{lon}", lon)
            .replace("{magnitude}", magnitude)
            .replace("{maps}", mapsLink)

        if (
            location != null &&
            !template.contains("{maps}") &&
            !message.contains("maps.google.com") &&
            !message.contains("google.com/maps")
        ) {
            message += "\n\nAbrir ubicaci�n en Google Maps:\n$mapsLink"
        }

        return message
    }

    suspend fun resolveLocationForAlert(
        context: Context,
        simulatedLatitude: Double? = null,
        simulatedLongitude: Double? = null
    ): Location? {
        if (simulatedLatitude != null && simulatedLongitude != null) {
            return Location("simulated").apply {
                latitude = simulatedLatitude
                longitude = simulatedLongitude
                accuracy = 10f
                time = System.currentTimeMillis()
            }
        }

        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Sin permiso de ubicaci�n")
            return null
        }

        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val lastLocation = fusedClient.lastLocation.await()
            if (lastLocation != null && isLocationFresh(lastLocation)) {
                Log.d(TAG, "Usando �ltima ubicaci�n conocida (${lastLocation.latitude}, ${lastLocation.longitude})")
                return lastLocation
            }

            Log.d(TAG, "Solicitando ubicaci�n GPS actual...")
            val currentLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                val cancellationToken = CancellationTokenSource()
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).await()
            }

            currentLocation ?: lastLocation ?: getLegacyLastKnownLocation(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ubicaci�n", e)
            getLegacyLastKnownLocation(context)
        }
    }

    @Suppress("MissingPermission")
    private fun getLegacyLastKnownLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            ) {
                return null
            }

            var bestLocation: Location? = null
            for (provider in locationManager.getProviders(true)) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.time > bestLocation.time) {
                    bestLocation = location
                }
            }
            bestLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error en ubicaci�n legacy", e)
            null
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        return System.currentTimeMillis() - location.time <= MAX_LOCATION_AGE_MS
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    fun sendSmsAlert(context: Context, phones: List<String>, message: String): Int {
        if (phones.isEmpty()) return 0

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return 0

        val parts = smsManager.divideMessage(message)
        Log.d(TAG, "Enviando SMS de ${message.length} caracteres en ${parts.size} parte(s)")

        var sentCount = 0
        for (phone in phones) {
            val cleanPhone = normalizePhoneNumber(phone)
            if (cleanPhone.isBlank()) continue
            try {
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                }
                sentCount++
                Log.d(TAG, "SMS enviado a $cleanPhone (${parts.size} parte(s))")
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando SMS a $cleanPhone", e)
            }
        }
        return sentCount
    }

    fun sendWhatsAppAlert(context: Context, phones: List<String>, message: String): Int {
        if (phones.isEmpty()) return 0

        val encodedMessage = Uri.encode(message)
        var sentCount = 0

        for (phone in phones) {
            val cleanPhone = normalizePhoneNumber(phone).replace("+", "")
            if (cleanPhone.isBlank()) continue

            val waUri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMessage")
            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                data = waUri
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(waIntent)
                sentCount++
                Thread.sleep(1500)
            } catch (e: Exception) {
                Log.e(TAG, "WhatsApp no disponible para $cleanPhone, intentando navegador", e)
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = waUri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    sentCount++
                    Thread.sleep(1500)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error enviando WhatsApp a $cleanPhone", e2)
                }
            }
        }
        return sentCount
    }

    fun loadCustomMessage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SMS_MESSAGE, null)
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.trim().replace(" ", "").replace("-", "")
    }
}
