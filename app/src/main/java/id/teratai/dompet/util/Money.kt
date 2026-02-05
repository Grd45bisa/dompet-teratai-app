package id.teratai.dompet.util

import java.text.NumberFormat
import java.util.Locale

object Money {

    /**
     * Accepts inputs like:
     * - 12000
     * - 12.000
     * - 12,000
     * - 12.000,50
     * - 12000,50
     * Returns normalized decimal with dot separator, e.g. 12000.50 or 12000
     */
    fun normalize(input: String): String {
        val s = input.trim()
        if (s.isBlank()) return ""

        // keep digits and separators
        val cleaned = s.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.isBlank()) return ""

        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')

        return when {
            // both separators exist: decide decimal by the last separator
            lastComma != -1 && lastDot != -1 -> {
                if (lastComma > lastDot) {
                    // decimal comma: 1.234,56 -> 1234.56
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // decimal dot: 1,234.56 -> 1234.56
                    cleaned.replace(",", "")
                }
            }
            // only comma
            lastComma != -1 -> {
                val parts = cleaned.split(',')
                if (parts.size == 2 && parts[1].length in 1..2) {
                    // treat as decimal comma
                    parts[0].replace(".", "") + "." + parts[1]
                } else {
                    // treat as thousand sep
                    cleaned.replace(",", "")
                }
            }
            // only dot
            lastDot != -1 -> {
                val parts = cleaned.split('.')
                if (parts.size == 2 && parts[1].length in 1..2) {
                    // likely decimal dot
                    cleaned.replace(",", "")
                } else {
                    // thousand sep
                    cleaned.replace(".", "")
                }
            }
            else -> cleaned
        }
    }

    fun formatIdr(normalized: String): String {
        val n = normalized.toDoubleOrNull() ?: return normalized
        val nf = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        // default may show decimal .00; keep it for now
        return nf.format(n)
    }
}
