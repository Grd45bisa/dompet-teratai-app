package id.teratai.dompet.scan

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.teratai.dompet.data.DatabaseProvider
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.data.TransactionRepository
import id.teratai.dompet.parse.ReceiptHeuristicParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ReceiptScannerViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    private val repo: TransactionRepository by lazy {
        val db = DatabaseProvider.get(getApplication())
        TransactionRepository(db.transactionDao())
    }

    fun reset() {
        _uiState.value = ReceiptScanUiState.Idle
    }

    fun captureAndOcr(imageCapture: ImageCapture) {
        val context = getApplication<Application>()
        _uiState.value = ReceiptScanUiState.Capturing

        val outDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val outFile = File(outDir, "receipt_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()

        imageCapture.takePicture(
            outputOptions,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(outFile)
                    _uiState.value = ReceiptScanUiState.OcrRunning

                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val image = InputImage.fromFilePath(context, uri)
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            viewModelScope.launch(Dispatchers.Default) {
                                val parsed = ReceiptHeuristicParser.parse(result.text)
                                val draft = ReceiptDraft(
                                    merchant = parsed.merchant.orEmpty(),
                                    dateIso = parsed.date.orEmpty(),
                                    total = parsed.total.orEmpty()
                                )
                                _uiState.value = ReceiptScanUiState.Done(
                                    imageUri = uri,
                                    ocrText = result.text,
                                    draft = draft,
                                    parsedSummary = parsed.pretty()
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReceiptScanner", "OCR failed", e)
                            _uiState.value = ReceiptScanUiState.Error("OCR error: ${e.message}")
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("ReceiptScanner", "Capture failed", exception)
                    _uiState.value = ReceiptScanUiState.Error("Capture error: ${exception.message}")
                }
            }
        )
    }

    fun saveDraft(draft: ReceiptDraft) {
        val state = _uiState.value
        if (state !is ReceiptScanUiState.Done) return

        val merchant = draft.merchant.ifBlank { "Unknown" }
        val dateIso = draft.dateIso.ifBlank { "" }
        val total = draft.total.ifBlank { "0" }

        viewModelScope.launch(Dispatchers.IO) {
            repo.insert(
                TransactionEntity(
                    merchant = merchant,
                    dateIso = dateIso,
                    total = total,
                    rawOcrText = state.ocrText,
                    imageUri = state.imageUri?.toString(),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            // after save, reset scan
            _uiState.value = ReceiptScanUiState.Idle
        }
    }
}
