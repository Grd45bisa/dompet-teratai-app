package id.teratai.dompet.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateFmt {
    private val out = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("in", "ID"))

    fun formatIso(iso: String): String {
        if (iso.isBlank()) return "(no date)"
        return try {
            val d = LocalDate.parse(iso)
            d.format(out)
        } catch (_: DateTimeParseException) {
            iso
        }
    }
}
