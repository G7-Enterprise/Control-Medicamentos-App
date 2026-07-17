package com.carlos.controlmedicamentos

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatisticsPdfExporter {

    suspend fun exportMonthlyStatisticsPdf(
        context: Context,
        patientName: String,
        monthLabel: String,
        medicationUsage: List<Pair<String, Int>>,
        totalTomas: Int,
        generatedAt: Long = System.currentTimeMillis()
    ): String? {
        return withContext(Dispatchers.IO) {
            val fechaGeneracion = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(generatedAt))
            val nombreArchivo = "Estadisticas_${monthLabel.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(generatedAt))}.pdf"
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
                color = android.graphics.Color.parseColor("#0D47A1")
            }
            val subtitlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = android.graphics.Color.parseColor("#555555")
            }
            val textPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 12f
                color = android.graphics.Color.BLACK
            }
            val footerPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 10f
                color = android.graphics.Color.parseColor("#888888")
            }

            var y = 50f
            canvas.drawText("Estadísticas mensuales", 40f, y, titlePaint)
            y += 28f
            canvas.drawText("Usuario: $patientName", 40f, y, subtitlePaint)
            y += 18f
            canvas.drawText("Mes: $monthLabel", 40f, y, subtitlePaint)
            y += 18f
            canvas.drawText("Generado: $fechaGeneracion", 40f, y, subtitlePaint)
            y += 28f

            y = drawSectionTitle(canvas, "Resumen", 40f, y, textPaint)
            y += 6f
            y = drawMultilineText(canvas, "Total de tomas en el mes: $totalTomas", 40f, y, textPaint, 515f)
            y = drawMultilineText(canvas, "Medicamentos utilizados: ${medicationUsage.count { it.second > 0 }}", 40f, y, textPaint, 515f)
            y += 14f

            y = drawSectionTitle(canvas, "Consumo por medicamento", 40f, y, textPaint)
            y += 6f
            medicationUsage.forEach { (name, count) ->
                val line = "• $name: $count ${if (count == 1) "toma" else "tomas"}"
                y = drawMultilineText(canvas, line, 40f, y, textPaint, 515f)
                y += 4f
                if (y > 780f) return@withContext null
            }

            canvas.drawText("Generado por Control Medicamentos", 40f, 820f, footerPaint)
            document.finishPage(page)

            val uriString = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { document.writeTo(it) }
                        it.toString()
                    }
                } else {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloads, nombreArchivo)
                    FileOutputStream(file).use { document.writeTo(it) }
                    file.toURI().toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                document.close()
            }

            uriString
        }
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint): Float {
        canvas.drawText(text, x, y, paint)
        return y
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Float, yStart: Float, paint: Paint, maxWidth: Float): Float {
        var y = yStart
        val words = text.split(" ")
        var line = ""
        val lineHeight = paint.textSize + 6f
        for (word in words) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(next) <= maxWidth) {
                line = next
            } else {
                canvas.drawText(line, x, y, paint)
                y += lineHeight
                line = word
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }
}

