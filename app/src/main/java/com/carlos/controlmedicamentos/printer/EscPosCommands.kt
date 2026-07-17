package com.carlos.controlmedicamentos.printer

import java.nio.charset.Charset

/**
 * Generador de comandos ESC/POS para impresoras térmicas.
 * Compatible con la mayoría de impresoras Bluetooth 58mm y 80mm.
 */
object EscPosCommands {

    private const val ESC = 0x1B.toByte()
    private const val GS = 0x1D.toByte()
    private const val LF = 0x0A.toByte()
    private const val NUL = 0x00.toByte()

    fun initPrinter(): ByteArray = byteArrayOf(ESC, 0x40)

    fun feedLines(lines: Int = 3): ByteArray = ByteArray(lines) { LF }

    fun cutPaper(): ByteArray = byteArrayOf(GS, 0x56, 0x41, 0x00)

    fun alignLeft(): ByteArray = byteArrayOf(ESC, 0x61, 0x00)
    fun alignCenter(): ByteArray = byteArrayOf(ESC, 0x61, 0x01)
    fun alignRight(): ByteArray = byteArrayOf(ESC, 0x61, 0x02)

    fun boldOn(): ByteArray = byteArrayOf(ESC, 0x45, 0x01)
    fun boldOff(): ByteArray = byteArrayOf(ESC, 0x45, 0x00)

    fun doubleHeightOn(): ByteArray = byteArrayOf(ESC, 0x21, 0x10)
    fun doubleWidthOn(): ByteArray = byteArrayOf(ESC, 0x21, 0x20)
    fun doubleSizeOn(): ByteArray = byteArrayOf(ESC, 0x21, 0x30)
    fun normalSize(): ByteArray = byteArrayOf(ESC, 0x21, 0x00)

    fun underlineOn(): ByteArray = byteArrayOf(ESC, 0x2D, 0x01)
    fun underlineOff(): ByteArray = byteArrayOf(ESC, 0x2D, 0x00)

    fun text(text: String, charset: Charset = Charsets.ISO_8859_1): ByteArray =
        text.toByteArray(charset)

    fun line(text: String = "", charset: Charset = Charsets.ISO_8859_1): ByteArray =
        text.toByteArray(charset) + byteArrayOf(LF)

    fun separator(char: Char = '-', width: Int = 32): ByteArray =
        line(char.toString().repeat(width))

    fun qrCode(data: String, size: Int = 6): ByteArray {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val len = bytes.size
        val pL = (len and 0xFF).toByte()
        val pH = ((len shr 8) and 0xFF).toByte()
        return byteArrayOf(
            GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte(), // Tamaño módulo
            GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30,        // Error correction L
            GS, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30              // Almacenar datos
        ) + bytes + byteArrayOf(
            GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30,        // Imprimir
            LF
        )
    }

    fun buildTicketContent(
        title: String,
        lines: List<String>,
        footer: String = "",
        includeCut: Boolean = true
    ): ByteArray {
        val sb = ArrayList<Byte>()
        fun add(bytes: ByteArray) { bytes.forEach { sb.add(it) } }

        add(initPrinter())
        add(alignCenter())
        add(doubleSizeOn())
        add(text(title))
        add(normalSize())
        add(byteArrayOf(LF))
        add(alignLeft())
        add(separator('-', 32))

        lines.forEach { line ->
            add(text(line))
            add(byteArrayOf(LF))
        }

        add(separator('-', 32))
        if (footer.isNotBlank()) {
            add(alignCenter())
            add(text(footer))
            add(byteArrayOf(LF))
        }
        add(feedLines(3))
        if (includeCut) add(cutPaper())

        return sb.toByteArray()
    }
}

private fun ArrayList<Byte>.toByteArray(): ByteArray {
    val arr = ByteArray(size)
    forEachIndexed { i, b -> arr[i] = b }
    return arr
}
