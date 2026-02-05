package id.teratai.dompet.scan

import android.net.Uri
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.teratai.dompet.util.DateFmt
import id.teratai.dompet.util.Money

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    initial: ReceiptDraft,
    imageUri: Uri?,
    onBack: () -> Unit,
    onSave: (ReceiptDraft) -> Unit,
) {
    var merchant by remember { mutableStateOf(initial.merchant) }
    var dateIso by remember { mutableStateOf(initial.dateIso) }
    var total by remember { mutableStateOf(initial.total) }

    var showDatePicker by remember { mutableStateOf(false) }

    val initialDateMillis = remember(dateIso) {
        try {
            if (dateIso.isBlank()) null else {
                val d = LocalDate.parse(dateIso)
                d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        } catch (_: Throwable) {
            null
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    val dateOk = dateIso.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(dateIso)
    val totalNorm = Money.normalize(total)
    val totalOk = totalNorm.isNotBlank() && totalNorm.toDoubleOrNull() != null

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review transaksi", style = MaterialTheme.typography.titleLarge)

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val ms = datePickerState.selectedDateMillis
                        if (ms != null) {
                            val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                            dateIso = d.toString()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }


        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Receipt image",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            "Tips: kalau struk miring/kurang fokus, balik ke Scan → Crop/Rotate/Re-OCR.",
            style = MaterialTheme.typography.bodySmall
        )

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
            label = { Text("Tanggal (yyyy-MM-dd)") },
            isError = !dateOk,
            supportingText = {
                when {
                    !dateOk -> Text("Format salah. Contoh: 2026-02-05")
                    dateIso.isNotBlank() -> Text("Display: ${DateFmt.formatIso(dateIso)}")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        TextButton(onClick = { showDatePicker = true }) {
            Text("Pilih tanggal")
        }


        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = total,
            onValueChange = { total = it.trim() },
            label = { Text("Total") },
            isError = total.isNotBlank() && !totalOk,
            supportingText = {
                if (total.isNotBlank() && !totalOk) {
                    Text("Total harus angka. Contoh: 12000 / 12.000 / 12.000,50")
                } else if (totalOk) {
                    Text("Display: ${Money.formatIdr(totalNorm)}")
                }
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
                            total = totalNorm
                        )
                    )
                }
            ) {
                Text("Simpan")
            }
        }
    }
}
