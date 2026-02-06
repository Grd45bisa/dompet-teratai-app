package id.teratai.dompet.util

import android.content.Context
import java.io.File

object StorageCleanup {
    fun deleteDirContents(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { f ->
            runCatching {
                if (f.isDirectory) {
                    deleteDirContents(f)
                    f.delete()
                } else {
                    f.delete()
                }
            }
        }
    }

    fun clearAllUserData(context: Context) {
        deleteDirContents(File(context.filesDir, "receipts"))
        deleteDirContents(File(context.filesDir, "exports"))
    }
}
