package com.carlos.controlmedicamentos.printer

import android.content.Context

enum class PrinterType {
    TICKET_BLUETOOTH,
    LETTER_BLUETOOTH,
    LETTER_WIFI
}

data class PrinterConfig(
    val id: String,
    val name: String,
    val type: PrinterType,
    val address: String = "", // MAC address para Bluetooth, IP:puerto para WiFi
    val paperWidth: Int = 384, // Ancho en puntos (58mm=384, 80mm=576)
    val charactersPerLine: Int = 32,
    val enabled: Boolean = true
)

data class PrinterSettingsState(
    val ticketPrinter: PrinterConfig? = null,
    val letterPrinter: PrinterConfig? = null
)

object PrinterSettingsStorage {
    private const val PREFS_NAME = "printer_settings"
    private const val KEY_TICKET_NAME = "ticket_name"
    private const val KEY_TICKET_ADDRESS = "ticket_address"
    private const val KEY_TICKET_WIDTH = "ticket_width"
    private const val KEY_TICKET_CHARS = "ticket_chars"
    private const val KEY_LETTER_NAME = "letter_name"
    private const val KEY_LETTER_ADDRESS = "letter_address"
    private const val KEY_LETTER_TYPE = "letter_type"

    fun load(context: Context): PrinterSettingsState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ticketAddress = prefs.getString(KEY_TICKET_ADDRESS, "").orEmpty()
        val ticket = if (ticketAddress.isNotBlank()) {
            PrinterConfig(
                id = "ticket_default",
                name = prefs.getString(KEY_TICKET_NAME, "Impresora de tickets").orEmpty(),
                type = PrinterType.TICKET_BLUETOOTH,
                address = ticketAddress,
                paperWidth = prefs.getInt(KEY_TICKET_WIDTH, 384),
                charactersPerLine = prefs.getInt(KEY_TICKET_CHARS, 32),
                enabled = true
            )
        } else null

        val letterAddress = prefs.getString(KEY_LETTER_ADDRESS, "").orEmpty()
        val letter = if (letterAddress.isNotBlank()) {
            val typeOrdinal = prefs.getInt(KEY_LETTER_TYPE, PrinterType.LETTER_BLUETOOTH.ordinal)
            PrinterConfig(
                id = "letter_default",
                name = prefs.getString(KEY_LETTER_NAME, "Impresora de carta").orEmpty(),
                type = PrinterType.entries.getOrElse(typeOrdinal) { PrinterType.LETTER_BLUETOOTH },
                address = letterAddress,
                paperWidth = 576,
                charactersPerLine = 48,
                enabled = true
            )
        } else null

        return PrinterSettingsState(ticket, letter)
    }

    fun saveTicket(context: Context, config: PrinterConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TICKET_NAME, config.name)
            .putString(KEY_TICKET_ADDRESS, config.address)
            .putInt(KEY_TICKET_WIDTH, config.paperWidth)
            .putInt(KEY_TICKET_CHARS, config.charactersPerLine)
            .apply()
    }

    fun saveLetter(context: Context, config: PrinterConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LETTER_NAME, config.name)
            .putString(KEY_LETTER_ADDRESS, config.address)
            .putInt(KEY_LETTER_TYPE, config.type.ordinal)
            .apply()
    }

    fun clearTicket(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TICKET_NAME)
            .remove(KEY_TICKET_ADDRESS)
            .remove(KEY_TICKET_WIDTH)
            .remove(KEY_TICKET_CHARS)
            .apply()
    }

    fun clearLetter(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LETTER_NAME)
            .remove(KEY_LETTER_ADDRESS)
            .remove(KEY_LETTER_TYPE)
            .apply()
    }
}
