package id.teratai.dompet.export

import id.teratai.dompet.data.TransactionEntity

object ExportJson {
    fun toJson(items: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        items.forEachIndexed { idx, tx ->
            sb.append("  {")
            sb.append("\"id\":").append(tx.id).append(',')
            sb.append("\"merchant\":").append(tx.merchant.jsonString()).append(',')
            sb.append("\"dateIso\":").append(tx.dateIso.jsonString()).append(',')
            sb.append("\"total\":").append(tx.total.jsonString()).append(',')
            sb.append("\"createdAtMs\":").append(tx.createdAtMs).append(',')
            sb.append("\"rawOcrText\":").append(tx.rawOcrText.jsonString()).append(',')
            sb.append("\"imageUri\":").append((tx.imageUri ?: "").jsonString())
            sb.append("}")
            if (idx != items.lastIndex) sb.append(',')
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun String.jsonString(): String {
        val escaped = buildString {
            for (c in this@jsonString) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
        }
        return '"' + escaped + '"'
    }
}
