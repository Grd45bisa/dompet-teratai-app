package id.teratai.dompet.history

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import id.teratai.dompet.data.DatabaseProvider
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.util.DateFmt
import id.teratai.dompet.util.Money
import id.teratai.dompet.util.TimeFmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = DatabaseProvider.get(getApplication()).transactionDao()

    private val _tx = MutableStateFlow<TransactionEntity?>(null)
    val tx: StateFlow<TransactionEntity?> = _tx.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _tx.value = dao.getById(id)
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteById(id)
            onDone()
        }
    }
}

@Composable
fun TransactionDetailScreen(
    id: Long,
    onBack: () -> Unit,
    vm: TransactionDetailViewModel = viewModel(),
) {
    var loaded by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showOcr by remember { mutableStateOf(false) }

    if (!loaded) {
        loaded = true
        vm.load(id)
    }

    val tx by vm.tx.collectAsStateWithLifecycleCompat()

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Hapus transaksi?") },
            text = { Text("Aksi ini tidak bisa dibatalkan.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDelete = false
                    vm.delete(id) { onBack() }
                }) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDelete = false }) { Text("Batal") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detail transaksi", style = MaterialTheme.typography.titleLarge)

        if (tx == null) {
            Text("Data tidak ditemukan.")
            OutlinedButton(onClick = onBack) { Text("Kembali") }
        } else {
            val t = tx!!

            Text("Merchant", style = MaterialTheme.typography.labelMedium)
            Text(t.merchant)

            Text("Tanggal", style = MaterialTheme.typography.labelMedium)
            Text(DateFmt.formatIso(t.dateIso))

            Text("Total", style = MaterialTheme.typography.labelMedium)
            Text(Money.formatIdr(t.total))

            Text("Disimpan", style = MaterialTheme.typography.labelMedium)
            Text(TimeFmt.formatCreatedAt(t.createdAtMs))

            if (!t.imageUri.isNullOrBlank()) {
                val uri = try { Uri.parse(t.imageUri) } catch (_: Throwable) { null }
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Receipt image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Teks OCR (raw)", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showOcr = !showOcr }) { Text(if (showOcr) "Sembunyikan" else "Tampilkan") }
            }
            AnimatedVisibility(visible = showOcr) {
                Text(t.rawOcrText, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Kembali") }
                Button(onClick = { showConfirmDelete = true }) { Text("Hapus") }
            }
        }
    }
}
