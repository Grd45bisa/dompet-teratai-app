package id.teratai.dompet.scan

import android.view.ViewGroup
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

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

    var showReview by remember { mutableStateOf(false) }

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
            onBack = { showReview = false },
            onSave = {
                // Phase 1.3 will persist to Room.
                // For now, we just go back to scan.
                showReview = false
                vm.reset()
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.captureAndOcr(imageCapture) },
                enabled = uiState !is ReceiptScanUiState.Capturing && uiState !is ReceiptScanUiState.OcrRunning
            ) {
                Text("Scan")
            }

            OutlinedButton(
                onClick = { vm.reset() },
                enabled = uiState is ReceiptScanUiState.Done || uiState is ReceiptScanUiState.Error
            ) {
                Text("Ulangi")
            }

            Button(
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

                    Text("Teks OCR", style = MaterialTheme.typography.titleMedium)
                    Text(s.ocrText, style = MaterialTheme.typography.bodySmall)
                }
            }

            else -> {
                Text("Arahkan kamera ke struk, lalu tekan Scan.")
            }
        }
    }
}
