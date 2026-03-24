package com.atruedev.bletoolkit.detail

object ValueFormatter {

    fun format(data: ByteArray, format: DisplayFormat): String = when (format) {
        DisplayFormat.HEX -> data.toHexString()
        DisplayFormat.UTF8 -> data.decodeToString()
        DisplayFormat.DECIMAL -> data.joinToString(" ") { (it.toInt() and 0xFF).toString() }
        DisplayFormat.BINARY -> data.joinToString(" ") {
            (it.toInt() and 0xFF).toString(2).padStart(8, '0')
        }
    }

    fun parseHexInput(input: String): ByteArray? {
        val cleaned = input.replace(" ", "").replace(":", "").replace("-", "")
        if (cleaned.length % 2 != 0) return null
        return try {
            cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
}
