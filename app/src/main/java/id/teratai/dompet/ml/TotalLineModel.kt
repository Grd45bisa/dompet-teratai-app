package id.teratai.dompet.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TotalLineModel private constructor(private val interpreter: Interpreter) {

    fun score(line: String): Float {
        return try {
            val input = arrayOf(line)
            val output = Array(1) { FloatArray(1) }
            interpreter.run(input, output)
            output[0][0]
        } catch (t: Throwable) {
            Log.e(TAG, "TFLite run failed", t)
            0f
        }
    }

    companion object {
        private const val TAG = "TotalLineModel"
        private const val ASSET_NAME = "total_line_model.tflite"

        @Volatile private var instance: TotalLineModel? = null

        fun getOrNull(context: Context): TotalLineModel? {
            return instance ?: synchronized(this) {
                instance ?: load(context)?.also { instance = it }
            }
        }

        private fun load(context: Context): TotalLineModel? {
            return try {
                val buf = loadAsset(context, ASSET_NAME)
                val opts = Interpreter.Options()
                // Model may require Select TF Ops (Flex)
                opts.setUseXNNPACK(true)
                val interpreter = Interpreter(buf, opts)
                TotalLineModel(interpreter)
            } catch (t: Throwable) {
                Log.w(TAG, "Model not available yet (asset missing or incompatible): ${t.message}")
                null
            }
        }

        private fun loadAsset(context: Context, name: String): MappedByteBuffer {
            context.assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { ch: FileChannel ->
                    return ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        }
    }
}
