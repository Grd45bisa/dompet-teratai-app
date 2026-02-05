package id.teratai.dompet.scan

import android.net.Uri

sealed class ReceiptScanUiState {
    data object Idle : ReceiptScanUiState()
    data object Capturing : ReceiptScanUiState()
    data object OcrRunning : ReceiptScanUiState()

    data class Done(
        val imageUri: Uri?,
        val ocrText: String,
        val draft: ReceiptDraft,
        val parsedSummary: String,
    ) : ReceiptScanUiState()

    data class Error(val message: String) : ReceiptScanUiState()
}
