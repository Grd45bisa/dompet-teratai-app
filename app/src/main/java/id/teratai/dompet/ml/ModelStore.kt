package id.teratai.dompet.ml

import android.content.Context
import java.io.File

object ModelStore {
    private const val DIR = "models"
    private const val TOTAL_LINE = "total_line_model.tflite"

    fun totalLineFile(context: Context): File = File(File(context.filesDir, DIR).apply { mkdirs() }, TOTAL_LINE)

    fun hasTotalLineModel(context: Context): Boolean = totalLineFile(context).exists()
}
