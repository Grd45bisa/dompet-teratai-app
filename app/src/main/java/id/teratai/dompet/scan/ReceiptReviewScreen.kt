package id.teratai.dompet.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ReceiptReviewScreen(
    initial: ReceiptDraft,
    onBack: () -> Unit,
    onSave: (ReceiptDraft) -> Unit,
) {
    var merchant by remember { mutableStateOf(initial.merchant) }
    var dateIso by remember { mutableStateOf(initial.dateIso) }
    var total by remember { mutableStateOf(initial.total) }

    val dateOk = dateIso.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(dateIso)
    val totalOk = total.isNotBlank() && total.toDoubleOrNull() != null

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review & Edit", style = MaterialTheme.typography.titleLarge)
        Text("Tanggal pakai format yyyy-MM-dd. Total isi angka (contoh 12345.67).")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Merchant") },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = dateIso,
            onValueChange = { dateIso = it.trim() },
            label = { Text("Date (yyyy-MM-dd)") },
            isError = !dateOk,
            supportingText = {
                if (!dateOk) Text("Format tanggal salah. Contoh: 2026-02-05")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = total,
            onValueChange = { total = it.trim() },
            label = { Text("Total") },
            isError = total.isNotBlank() && !totalOk,
            supportingText = {
                if (total.isNotBlank() && !totalOk) Text("Total harus angka. Contoh: 12345.67")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Button(
                enabled = totalOk && dateOk,
                onClick = {
                    onSave(
                        ReceiptDraft(
                            merchant = merchant.trim(),
                            dateIso = dateIso.trim(),
                            total = total.trim()
                        )
                    )
                }
            ) {
                Text("Simpan")
            }
        }
    }
}
