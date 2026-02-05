package id.teratai.dompet.util

import android.content.Context
import java.io.File

object Files {
    fun writeExport(context: Context, filename: String, content: String): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(content)
        return file
    }
}
