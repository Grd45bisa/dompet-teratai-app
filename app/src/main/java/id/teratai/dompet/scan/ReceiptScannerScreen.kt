package id.teratai.dompet.scan

import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import id.teratai.dompet.ml.ModelInstaller
import id.teratai.dompet.ml.ModelStore
import id.teratai.dompet.ml.TotalLineModel

@Composable
fun ReceiptScannerScreen(vm: ReceiptScannerViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val uiState by vm.uiState.collectAsStateWithLifecycleCompat()

    val modelPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = ModelInstaller.installTotalLineModel(context, uri)
            if (ok) {
                TotalLineModel.invalidate()
                // rerun OCR to reflect new model if there is a current image
                vm.reOcr()
            }
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) vm.rerunOcrForUri(uri)
        }
        // if cancelled or failed, do nothing (no error popups)
    }

    var showReview by rememberSaveable { mutableStateOf(false) }
    var showOcr by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(previewView) {
        val pv = previewView ?: return@LaunchedEffect
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pv.surfaceProvider)
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
    }

    if (showReview && uiState is ReceiptScanUiState.Done) {
        val done = uiState as ReceiptScanUiState.Done
        ReceiptReviewScreen(
            initial = done.draft,
            ocrText = done.ocrText,
            imageUri = done.imageUri,
            onBack = { showReview = false },
            onSave = { saved ->
                vm.saveDraft(saved)
                showReview = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan Struk", style = MaterialTheme.typography.titleLarge)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { pv -> previewView = pv }
                }
            )

            when (uiState) {
                ReceiptScanUiState.Capturing,
                ReceiptScanUiState.OcrRunning -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(42.dp))
                        Text(
                            when (uiState) {
                                ReceiptScanUiState.Capturing -> "Capturing…"
                                else -> "Running OCR…"
                            }
                        )
                    }
                }

                else -> Unit
            }
        }

        // Actions row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { vm.captureAndOcr(imageCapture) },
                enabled = uiState !is ReceiptScanUiState.Capturing && uiState !is ReceiptScanUiState.OcrRunning
            ) {
                Text("Scan")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { vm.reset() },
                enabled = uiState is ReceiptScanUiState.Done || uiState is ReceiptScanUiState.Error
            ) {
                Text("Ulangi")
            }
        }

        Text("Model TOTAL-line: " + if (ModelStore.hasTotalLineModel(context)) "Terpasang" else "Belum ada", style = MaterialTheme.typography.bodySmall)

        OutlinedButton(onClick = { modelPickerLauncher.launch(arrayOf("application/octet-stream", "application/x-tflite", "*/*")) }) {
            Text("Pasang Model (.tflite)")
        }

        // Actions row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    val done = uiState as? ReceiptScanUiState.Done
                    val uri = done?.imageUri
                    if (uri != null) {
                        cropLauncher.launch(
                            CropImageContractOptions(
                                uri,
                                CropImageOptions(
                                    guidelines = CropImageView.Guidelines.ON
                                )
                            )
                        )
                    }
                },
                enabled = uiState is ReceiptScanUiState.Done
            ) {
                Text("Crop")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { vm.rotate90AndRerunOcr() },
                enabled = uiState is ReceiptScanUiState.Done
            ) {
                Text("Rotate")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { vm.reOcr() },
                enabled = uiState is ReceiptScanUiState.Done
            ) {
                Text("Re-OCR")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = { showReview = true },
                enabled = uiState is ReceiptScanUiState.Done
            ) {
                Text("Lanjut")
            }
        }

        when (val s = uiState) {
            is ReceiptScanUiState.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
            }

            is ReceiptScanUiState.Done -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hasil parsing (baseline)", style = MaterialTheme.typography.titleMedium)
                    Text(s.parsedSummary, style = MaterialTheme.typography.bodySmall)

                    if (s.totalModelScore != null) {
                        Text("Skor model TOTAL: ${String.format("%.3f", s.totalModelScore)}", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Teks OCR", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { showOcr = !showOcr }) {
                            Text(if (showOcr) "Sembunyikan" else "Tampilkan")
                        }
                    }
                    AnimatedVisibility(visible = showOcr) {
                        Text(s.ocrText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> {
                Text("Arahkan kamera ke struk, lalu tekan Scan.")
            }
        }
    }
}
