package com.carlos.controlmedicamentos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

private const val MENSAJE_COMPARTIR =
    "¡Hola! Te recomiendo esta excelente aplicación para llevar tu Control de Medicamentos. Descárgala gratis y de forma segura desde su repositorio oficial aquí: https://github.com/G7-Enterprise/Control-Medicamentos-App/releases/latest"

/**
 * Abre WhatsApp (o WhatsApp Business) directamente con el mensaje de recomendación de la app.
 * Si no hay ninguna versión instalada, delega al selector genérico de compartir.
 */
fun compartirAppPorWhatsApp(context: Context) {
    val whatsappPackage = resolveWhatsappPackage(context)
    val intent = if (whatsappPackage != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(whatsappPackage)
            putExtra(Intent.EXTRA_TEXT, MENSAJE_COMPARTIR)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, MENSAJE_COMPARTIR)
        }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay ninguna app disponible para compartir", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("ShareAppHelper", "Error abriendo WhatsApp", e)
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
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
