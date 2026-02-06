package id.teratai.dompet.dataset

import android.content.Context
import id.teratai.dompet.scan.ReceiptDraft
import java.io.File

object DatasetLogger {

    /**
     * Saves one training sample as JSONL.
     * We store OCR text + user-confirmed labels.
     */
    fun appendSample(
        context: Context,
        ocrText: String,
        draft: ReceiptDraft,
        meta: Map<String, String> = emptyMap(),
    ) {
        val dir = File(context.filesDir, "dataset").apply { mkdirs() }
        val file = File(dir, "receipt_labels.jsonl")

        val json = buildString {
            append("{")
            append("\"merchant\":").append(draft.merchant.jsonString()).append(',')
            append("\"dateIso\":").append(draft.dateIso.jsonString()).append(',')
            append("\"total\":").append(draft.total.jsonString()).append(',')
            append("\"ocrText\":").append(ocrText.jsonString())
            if (meta.isNotEmpty()) {
                append(',')
                append("\"meta\":{")
                meta.entries.forEachIndexed { idx, e ->
                    append(e.key.jsonString()).append(':').append(e.value.jsonString())
                    if (idx != meta.size - 1) append(',')
                }
                append('}')
            }
            append('}')
        }

        file.appendText(json + "\n")
    }

    private fun String.jsonString(): String {
        val s = this
        val escaped = buildString {
            for (c in s) {
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
