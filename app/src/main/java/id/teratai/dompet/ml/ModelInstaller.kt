package id.teratai.dompet.ml

import android.content.Context
import android.net.Uri

object ModelInstaller {

    /**
     * Copy a user-selected model file (via SAF) into internal storage.
     */
    fun installTotalLineModel(context: Context, uri: Uri): Boolean {
        return try {
            val out = ModelStore.totalLineFile(context)
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) return false
                out.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }
}
