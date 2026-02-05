package id.teratai.dompet.util

import android.content.Context
import java.io.File

object ReceiptRetention {

    /**
     * Deletes oldest receipt images leaving at most [keepMax] files.
     */
    fun cleanup(context: Context, keepMax: Int = 200) {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") } ?: return
        if (files.size <= keepMax) return

        val sorted = files.sortedBy { it.lastModified() }
        val toDelete = sorted.take(files.size - keepMax)
        toDelete.forEach { runCatching { it.delete() } }
    }
}
