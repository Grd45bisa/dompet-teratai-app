package id.teratai.dompet.scan

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.teratai.dompet.parse.ReceiptHeuristicParser
import java.io.File

@Composable
fun ReceiptScannerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var status by remember { mutableStateOf("Ready") }
    var ocrText by remember { mutableStateOf("") }
    var parsedSummary by remember { mutableStateOf("") }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    LaunchedEffect(previewView) {
        val pv = previewView ?: return@LaunchedEffect
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pv.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            status = "Camera ready"
        } catch (t: Throwable) {
            status = "Camera error: ${t.message}"
            Log.e("ReceiptScanner", "bindToLifecycle failed", t)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan Struk", style = MaterialTheme.typography.titleLarge)
        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { pv ->
                    previewView = pv
                }
            }
        )

        Button(
            onClick = {
                status = "Capturing…"
                captureAndRunOcr(
                    context = context,
                    imageCapture = imageCapture,
                    onStatus = { status = it },
                    onOcrResult = { text ->
                        ocrText = text
                        val parsed = ReceiptHeuristicParser.parse(text)
                        parsedSummary = parsed.pretty()
                    }
                )
            }
        ) {
            Text("Capture + OCR")
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (parsedSummary.isNotBlank()) {
                Text("Hasil parsing (baseline)", style = MaterialTheme.typography.titleMedium)
                Text(parsedSummary, style = MaterialTheme.typography.bodySmall)
            }

            if (ocrText.isNotBlank()) {
                Text("Teks OCR", style = MaterialTheme.typography.titleMedium)
                Text(ocrText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun captureAndRunOcr(
    context: Context,
    imageCapture: ImageCapture,
    onStatus: (String) -> Unit,
    onOcrResult: (String) -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(context)

    val outDir = File(context.cacheDir, "captures").apply { mkdirs() }
    val outFile = File(outDir, "receipt_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onStatus("Running OCR…")
                val image = InputImage.fromFilePath(context, android.net.Uri.fromFile(outFile))
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result: Text ->
                        onStatus("OCR done")
                        onOcrResult(result.text)
                    }
                    .addOnFailureListener { e ->
                        onStatus("OCR error: ${e.message}")
                        Log.e("ReceiptScanner", "OCR failed", e)
                    }
            }

            override fun onError(exception: ImageCaptureException) {
                onStatus("Capture error: ${exception.message}")
                Log.e("ReceiptScanner", "Capture failed", exception)
            }
        }
    )
}


