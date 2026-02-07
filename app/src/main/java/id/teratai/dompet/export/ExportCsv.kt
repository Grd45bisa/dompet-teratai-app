package id.teratai.dompet.export

import id.teratai.dompet.data.TransactionEntity

object ExportCsv {
    fun toCsv(items: List<TransactionEntity>): String {
        val header = listOf("id", "merchant", "date", "total", "createdAtMs").joinToString(",")
        val rows = items.map { tx ->
            listOf(
                tx.id.toString(),
                tx.merchant.csvEscape(),
                tx.dateIso.csvEscape(),
                tx.total.csvEscape(),
                tx.createdAtMs.toString()
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun String.csvEscape(): String {
        val needs = contains(',') || contains('"') || contains('\n') || contains('\r')
        if (!needs) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }
}
