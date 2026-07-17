package com.carlos.controlmedicamentos.printer

import android.content.Context
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager unificado para las dos impresoras configuradas:
 * - Tickets: impresora térmica Bluetooth con ESC/POS.
 * - Carta: impresora normal vía Android Print Framework (PDF generado desde HTML/WebView).
 */
class PrinterManager(private val context: Context) {

    private val btManager by lazy { BluetoothPrinterManager(context) }

    fun getSavedSettings(): PrinterSettingsState = PrinterSettingsStorage.load(context)

    fun getPairedBluetoothDevices(): List<android.bluetooth.BluetoothDevice> =
        btManager.getPairedDevices()

    fun isBluetoothEnabled(): Boolean = btManager.isBluetoothEnabled()

    fun hasBluetoothPermissions(): Boolean = btManager.hasBluetoothPermission()

    // ---------- Tickets (ESC/POS Bluetooth) ----------

    suspend fun printTicket(
        title: String,
        lines: List<String>,
        footer: String = ""
    ): Result<Unit> {
        val config = getSavedSettings().ticketPrinter
            ?: return Result.failure(IllegalStateException("No hay impresora de tickets configurada"))

        val data = EscPosCommands.buildTicketContent(title, lines, footer, includeCut = true)
        return btManager.printAndDisconnect(data, config.address)
    }

    suspend fun printTicketRaw(data: ByteArray): Result<Unit> {
        val config = getSavedSettings().ticketPrinter
            ?: return Result.failure(IllegalStateException("No hay impresora de tickets configurada"))
        return btManager.printAndDisconnect(data, config.address)
    }

    // ---------- Carta (Android Print / WebView -> PDF) ----------

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun printLetter(
        htmlContent: String,
        jobName: String = "Documento",
        callback: PrintCallback? = null
    ) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                doPrint(webView, jobName, callback)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun doPrint(
        webView: WebView,
        jobName: String,
        callback: PrintCallback?
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return

        val adapter: PrintDocumentAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.createPrintDocumentAdapter(jobName)
        } else {
            @Suppress("DEPRECATION")
            webView.createPrintDocumentAdapter()
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins(20, 20, 20, 20))
            .build()

        printManager.print(jobName, adapter, attributes)
        callback?.onPrintQueued()
    }

    fun printLetterHtml(
        title: String,
        bodyItems: List<String>,
        jobName: String = "Documento"
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            append("<style>")
            append("body{font-family:sans-serif;font-size:12pt;margin:20px;line-height:1.4;}")
            append("h1{font-size:16pt;text-align:center;margin-bottom:12px;}")
            append(".item{margin-bottom:6px;}")
            append(".footer{margin-top:24px;font-size:10pt;color:#555;text-align:center;}")
            append("</style></head><body>")
            append("<h1>")
            append(escapeHtml(title))
            append("</h1>")
            bodyItems.forEach {
                append("<div class='item'>")
                append(escapeHtml(it))
                append("</div>")
            }
            append("<div class='footer'>Generado por ControlMedicamentos</div>")
            append("</body></html>")
        }
        printLetter(html, jobName)
    }

    // ---------- Helpers ----------

    fun disconnect() = btManager.disconnect()

    interface PrintCallback {
        fun onPrintQueued()
    }
}

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
