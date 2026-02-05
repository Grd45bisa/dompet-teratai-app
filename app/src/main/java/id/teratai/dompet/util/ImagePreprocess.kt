package id.teratai.dompet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImagePreprocess {

    /**
     * Creates a resized JPEG (max dimension = maxDim) in internal storage.
     * Returns Uri of new file.
     */
    fun resizeForOcr(context: Context, uri: Uri, maxDim: Int = 1600, jpegQuality: Int = 85): Uri? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bmp = input.use { BitmapFactory.decodeStream(it) } ?: return null

            val w = bmp.width
            val h = bmp.height
            val scale = minOf(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat(), 1f)
            val outBmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(bmp, (w * scale).toInt(), (h * scale).toInt(), true)
            } else {
                bmp
            }

            val outDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val outFile = File(outDir, "receipt_ocr_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                outBmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
            }
            android.net.Uri.fromFile(outFile)
        } catch (_: Throwable) {
            null
        }
    }
}
