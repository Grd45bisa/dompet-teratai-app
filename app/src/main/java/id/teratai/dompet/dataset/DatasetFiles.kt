package id.teratai.dompet.dataset

import android.content.Context
import java.io.File

object DatasetFiles {
    fun datasetFile(context: Context): File = File(File(context.filesDir, "dataset"), "receipt_labels.jsonl")
}
