package com.carlos.controlmedicamentos

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.carlos.controlmedicamentos.data.local.TransaccionDental
import java.text.SimpleDateFormat
import java.util.*

object DentistaPdfExporter {

    fun exportarFinanzasAPdf(
        context: Context,
        uri: Uri,
        patientName: String,
        transacciones: List<TransaccionDental>
    ): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

            val titlePaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            var y = margin + 20f

            canvas.drawText("Resumen financiero dental", margin, y, titlePaint)
            y += 28f
            canvas.drawText("Paciente: $patientName", margin, y, headerPaint)
            y += 18f
            canvas.drawText("Generado: ${fmt.format(Date())}", margin, y, textPaint)
            y += 30f

            val totalIngresos = transacciones.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
            val totalGastos = transacciones.filter { it.tipo == "GASTO" }.sumOf { it.monto }
            val saldo = totalIngresos - totalGastos

            canvas.drawText("Ingresos: $${String.format("%.2f", totalIngresos)}", margin, y, headerPaint)
            y += 16f
            canvas.drawText("Gastos: $${String.format("%.2f", totalGastos)}", margin, y, headerPaint)
            y += 16f
            canvas.drawText("Saldo: $${String.format("%.2f", saldo)}", margin, y, headerPaint)
            y += 28f

            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f

            // Headers
            val xFecha = margin
            val xConcepto = margin + 80f
            val xCategoria = margin + 240f
            val xDiente = margin + 340f
            val xMonto = margin + 400f

            canvas.drawText("Fecha", xFecha, y, headerPaint)
            canvas.drawText("Concepto", xConcepto, y, headerPaint)
            canvas.drawText("Categoría", xCategoria, y, headerPaint)
            canvas.drawText("Diente", xDiente, y, headerPaint)
            canvas.drawText("Monto", xMonto, y, headerPaint)
            y += 18f
            canvas.drawLine(margin, y - 6f, pageWidth - margin, y - 6f, linePaint)

            transacciones.forEach { t ->
                if (y > pageHeight - margin - 30f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }

                val montoStr = "${if (t.tipo == "INGRESO") "+" else "-"}$${String.format("%.2f", t.monto)}"
                canvas.drawText(fmt.format(Date(t.fecha)), xFecha, y, textPaint)
                canvas.drawText(t.concepto.take(25), xConcepto, y, textPaint)
                canvas.drawText(t.categoria, xCategoria, y, textPaint)
                canvas.drawText(if (t.numeroDiente > 0) "#${t.numeroDiente}" else "-", xDiente, y, textPaint)
                canvas.drawText(montoStr, xMonto, y, textPaint)
                y += 14f
            }

            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
