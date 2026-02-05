package id.teratai.dompet.parse

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object ReceiptHeuristicParser {

    data class ParsedReceipt(
        val merchant: String?,
        val date: String?,
        val total: String?,
    ) {
        fun pretty(): String = buildString {
            appendLine("merchant: ${merchant ?: "-"}")
            appendLine("date: ${date ?: "-"}")
            appendLine("total: ${total ?: "-"}")
        }
    }

    fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = guessMerchant(lines)
        val date = guessDate(lines)
        val total = guessTotal(lines)

        return ParsedReceipt(
            merchant = merchant,
            date = date,
            total = total
        )
    }

    private fun guessMerchant(lines: List<String>): String? {
        // Heuristic: take the first non-numeric-ish line that isn't obviously a header keyword.
        val banned = setOf("NPWP", "TELP", "TEL", "PHONE", "CASH", "KASIR", "TAX")
        return lines.firstOrNull { ln ->
            val up = ln.uppercase(Locale.ROOT)
            val hasLetters = up.any { it.isLetter() }
            val tooNumeric = up.count { it.isDigit() } > (up.length / 2)
            hasLetters && !tooNumeric && banned.none { up.contains(it) }
        }
    }

    private fun guessDate(lines: List<String>): String? {
        // Match common receipt formats: dd/MM/yyyy, dd-MM-yyyy, dd/MM/yy, dd-MM-yy
        val patterns = listOf(
            Regex("(\\d{2})[/-](\\d{2})[/-](\\d{2,4})"),
            Regex("(\\d{4})[/-](\\d{2})[/-](\\d{2})")
        )

        val raw = lines.firstNotNullOfOrNull { ln ->
            patterns.firstNotNullOfOrNull { rx -> rx.find(ln)?.value }
        } ?: return null

        // Normalize to yyyy-MM-dd if possible
        val candidates = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd/MM/yy",
            "dd-MM-yy",
            "yyyy/MM/dd",
            "yyyy-MM-dd"
        )

        for (fmt in candidates) {
            try {
                val df = DateTimeFormatter.ofPattern(fmt)
                val d = LocalDate.parse(raw, df)
                return d.toString()
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return raw
    }

    private fun guessTotal(lines: List<String>): String? {
        // Find line containing TOTAL keyword; else take largest currency-like number.
        val moneyRx = Regex("(\\d{1,3}([.,]\\d{3})+|\\d+)([.,]\\d{2})?")
        val totalLine = lines.firstOrNull { it.uppercase(Locale.ROOT).contains("TOTAL") }
        if (totalLine != null) {
            val m = moneyRx.findAll(totalLine).map { it.value }.toList()
            if (m.isNotEmpty()) return normalizeMoney(m.last())
        }

        // fallback: pick max numeric value from all lines
        val all = lines.flatMap { ln -> moneyRx.findAll(ln).map { it.value }.toList() }
        if (all.isEmpty()) return null
        val best = all.maxByOrNull { toComparableMoney(it) } ?: return null
        return normalizeMoney(best)
    }

    private fun normalizeMoney(s: String): String {
        // normalize: remove thousand separators, use dot as decimal, return as string
        val cleaned = s.trim()
        val hasCommaDecimal = cleaned.count { it == ',' } == 1 && cleaned.contains('.')
        return if (hasCommaDecimal) {
            // e.g. 1.234,56 -> 1234.56
            cleaned.replace(".", "").replace(',', '.')
        } else {
            // e.g. 1,234 or 1.234 -> 1234 ; 1234,56 -> 1234.56
            val lastComma = cleaned.lastIndexOf(',')
            val lastDot = cleaned.lastIndexOf('.')
            when {
                lastComma > -1 && lastDot == -1 -> {
                    // could be decimal or thousand; assume decimal if two digits after
                    val parts = cleaned.split(',')
                    if (parts.last().length == 2) parts[0].replace(".", "").replace(",", "") + "." + parts[1]
                    else cleaned.replace(",", "")
                }
                lastDot > -1 && lastComma == -1 -> cleaned.replace(".", "")
                else -> cleaned
            }
        }
    }

    private fun toComparableMoney(s: String): Long {
        // crude comparable: strip non-digits
        val digits = s.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }
}
