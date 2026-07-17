package com.carlos.controlmedicamentos

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.carlos.controlmedicamentos.data.local.DiarioEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiarioPdfExporter {

    suspend fun exportarDiarioAPdf(context: Context, entry: DiarioEntry) {
        withContext(Dispatchers.IO) {
            val fechaStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.fecha))
            val texto = entry.texto
            val bitmapOriginal = entry.rutaImagen?.let { ruta ->
                try { BitmapFactory.decodeFile(ruta) } catch (_: Exception) { null }
            }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitulo = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
                color = android.graphics.Color.parseColor("#0D47A1")
            }

            val paintSubtitulo = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = android.graphics.Color.parseColor("#555555")
            }

            val paintTexto = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 12f
                color = android.graphics.Color.BLACK
            }

            var yPos = 50f

            // Encabezado
            canvas.drawText("Control medicamentos - Diario Personal", 40f, yPos, paintTitulo)
            yPos += 30f
            canvas.drawText("Fecha: $fechaStr", 40f, yPos, paintSubtitulo)
            yPos += 40f

            // Texto línea por línea
            val lineas = texto.split("\n")
            for (linea in lineas) {
                if (yPos > 780f) break
                canvas.drawText(linea, 40f, yPos, paintTexto)
                yPos += 20f
            }

            yPos += 20f

            // Imagen si existe
            bitmapOriginal?.let { bitmap ->
                if (yPos < 720f) {
                    val anchoMaximo = 515
                    val altoMaximo = 300
                    val escalaAncho = anchoMaximo.toFloat() / bitmap.width
                    val escalaAlto = altoMaximo.toFloat() / bitmap.height
                    val escalaFinal = minOf(escalaAncho, escalaAlto)

                    val nuevoAncho = (bitmap.width * escalaFinal).toInt()
                    val nuevoAlto = (bitmap.height * escalaFinal).toInt()

                    val bitmapEscalado = Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
                    canvas.drawBitmap(bitmapEscalado, 40f, yPos, null)
                }
            }

            // Pie
            val paintPie = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 10f
                color = android.graphics.Color.parseColor("#888888")
            }
            canvas.drawText("Generado por Control medicamentos", 40f, 820f, paintPie)

            pdfDocument.finishPage(page)

            val nombreArchivo = "Diario_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(entry.fecha))}.pdf"
            var outputStream: OutputStream? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        outputStream = resolver.openOutputStream(uri)
                    }
                } else {
                    val directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val archivo = File(directorio, nombreArchivo)
                    outputStream = FileOutputStream(archivo)
                }

                outputStream?.use { stream ->
                    pdfDocument.writeTo(stream)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF guardado en Descargas: $nombreArchivo", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pdfDocument.close()
            }
        }
    }
}
