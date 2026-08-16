package com.carlos.controlmedicamentos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_LATEST_RELEASE_API =
    "https://api.github.com/repos/G7-Enterprise/Control-Medicamentos-App/releases/latest"
private const val GITHUB_LATEST_RELEASE_PAGE =
    "https://github.com/G7-Enterprise/Control-Medicamentos-App/releases/latest"

/**
 * Abre WhatsApp (o WhatsApp Business) directamente con el mensaje de recomendación de la app.
 * Si no hay ninguna versión instalada, delega al selector genérico de compartir.
 */
fun compartirAppPorWhatsApp(context: Context) {
    Toast.makeText(context, "Preparando enlace de descarga...", Toast.LENGTH_SHORT).show()
    CoroutineScope(Dispatchers.IO).launch {
        val downloadUrl = runCatching {
            fetchLatestReleaseApkUrl()
        }.getOrElse {
            Log.w("ShareAppHelper", "No se pudo obtener el enlace dinámico", it)
            GITHUB_LATEST_RELEASE_PAGE
        }
        val message = "¡Hola! Te recomiendo Control de Medicamentos. " +
            "Descárgala gratis desde aquí: $downloadUrl"
        withContext(Dispatchers.Main) {
            openShareIntent(context, message)
        }
    }
}

private fun openShareIntent(context: Context, message: String) {
    val whatsappPackage = resolveWhatsappPackage(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (whatsappPackage != null) setPackage(whatsappPackage)
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay ninguna app disponible para compartir", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("ShareAppHelper", "Error abriendo WhatsApp", e)
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

private fun fetchLatestReleaseApkUrl(): String {
    val connection = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "ControlMedicamentos-App")
    }
    return try {
        if (connection.responseCode !in 200..299) error("GitHub respondió ${connection.responseCode}")
        val release = JsonParser.parseString(connection.inputStream.bufferedReader().use { reader -> reader.readText() })
            .asJsonObject
        release.getAsJsonArray("assets")
            .firstOrNull { asset ->
                asset.asJsonObject.get("name")?.asString?.endsWith(".apk", ignoreCase = true) == true
            }
            ?.asJsonObject
            ?.get("browser_download_url")
            ?.asString
            ?: error("El último Release no contiene un APK")
    } finally {
        connection.disconnect()
    }
}

private fun resolveWhatsappPackage(context: Context): String? {
    return listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { packageName ->
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
