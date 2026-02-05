package id.teratai.dompet.scan

data class ReceiptDraft(
    val merchant: String = "",
    val dateIso: String = "",
    val total: String = "",
)
