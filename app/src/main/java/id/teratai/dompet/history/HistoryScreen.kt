package id.teratai.dompet.history

import id.teratai.dompet.util.Files
import id.teratai.dompet.export.ExportCsv
import id.teratai.dompet.export.ExportJson
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedButton
import androidx.core.content.FileProvider
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.util.DateFmt
import id.teratai.dompet.util.Money
import id.teratai.dompet.util.TimeFmt

@Composable
fun HistoryScreen(
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycleCompat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Riwayat", style = MaterialTheme.typography.titleLarge)

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

        if (items.isEmpty()) {
            Text("Belum ada transaksi tersimpan.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { tx ->
                    TransactionRow(tx = tx, onClick = { onOpen(tx.id) })
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
