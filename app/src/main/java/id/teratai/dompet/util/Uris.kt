package id.teratai.dompet.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Uris {
    fun forFile(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
}
