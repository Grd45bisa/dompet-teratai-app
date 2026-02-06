package id.teratai.dompet.history

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.export.ExportCsv
import id.teratai.dompet.export.ExportJson
import id.teratai.dompet.util.DateFmt
import id.teratai.dompet.util.Files
import id.teratai.dompet.util.Money
import id.teratai.dompet.util.TimeFmt
import id.teratai.dompet.dataset.DatasetFiles

@Composable
fun HistoryScreen(
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycleCompat()

    var query by remember { mutableStateOf("") }
    var showConfirmClear by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filtered = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) items
        else items.filter { tx ->
            tx.merchant.lowercase().contains(q) || tx.dateIso.lowercase().contains(q) || tx.total.contains(q)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Text("Riwayat", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val csv = ExportCsv.toCsv(items)
                val file = Files.writeExport(context, "dompet-teratai-export.csv", csv)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export CSV"))
            }) {
                Text("Export CSV")
            }

            OutlinedButton(onClick = {
                val json = ExportJson.toJson(items)
                val file = Files.writeExport(context, "dompet-teratai-export.json", json)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export JSON"))
            }) {
                Text("Export JSON")
            }

            OutlinedButton(onClick = {
                val file = DatasetFiles.datasetFile(context)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Dataset"))
                }
            }) {
                Text("Export Dataset")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showConfirmClear = true }) {
                Text("Reset data")
            }
        }


        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("Cari (merchant/tanggal/total)") }
        )

        if (showConfirmClear) {
            AlertDialog(
                onDismissRequest = { showConfirmClear = false },
                title = { Text("Reset semua data?") },
                text = { Text("Ini akan menghapus SEMUA transaksi + foto struk yang tersimpan di device.") },
                confirmButton = {
                    Button(onClick = {
                        showConfirmClear = false
                        vm.clearAll { scope.launch { snackbarHostState.showSnackbar("Data terhapus") } }
                    }) { Text("Hapus semua") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmClear = false }) { Text("Batal") }
                }
            )
        }


        if (filtered.isEmpty()) {
            Text("Tidak ada data yang cocok.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { tx ->
                    TransactionRow(tx = tx, onClick = { onOpen(tx.id) })
                }
            }
        }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tx.merchant.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(DateFmt.formatIso(tx.dateIso), style = MaterialTheme.typography.bodySmall)
                Text(Money.formatIdr(tx.total), style = MaterialTheme.typography.bodySmall)
            }
            Text("Disimpan: ${TimeFmt.formatCreatedAt(tx.createdAtMs)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
