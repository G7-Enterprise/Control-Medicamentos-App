package com.carlos.controlmedicamentos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

private const val MENSAJE_COMPARTIR =
    "¡Hola! Te recomiendo esta excelente aplicación para llevar tu Control de Medicamentos. Descárgala gratis y de forma segura desde su repositorio oficial aquí: https://github.com/G7-Enterprise/Control-Medicamentos-App/releases/latest"

/**
 * Abre WhatsApp directamente con el mensaje de recomendación de la app.
 * Muestra un Toast si WhatsApp no está instalado.
 */
fun compartirAppPorWhatsApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, MENSAJE_COMPARTIR)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "WhatsApp no está instalado en este dispositivo", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("ShareAppHelper", "Error abriendo WhatsApp", e)
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
