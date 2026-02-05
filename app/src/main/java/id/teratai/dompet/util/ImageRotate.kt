package id.teratai.dompet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageRotate {

    fun rotate90AndSave(context: Context, uri: Uri): Uri? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bmp = input.use { BitmapFactory.decodeStream(it) } ?: return null

            val m = Matrix().apply { postRotate(90f) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)

            val outDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val outFile = File(outDir, "receipt_rot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            Uri.fromFile(outFile)
        } catch (_: Throwable) {
            null
        }
    }
}
