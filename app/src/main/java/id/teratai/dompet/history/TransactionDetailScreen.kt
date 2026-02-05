package id.teratai.dompet.history

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import id.teratai.dompet.data.DatabaseProvider
import id.teratai.dompet.data.TransactionEntity
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
    if (!loaded) {
        loaded = true
        vm.load(id)
    }

    val tx by vm.tx.collectAsStateWithLifecycleCompat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detail", style = MaterialTheme.typography.titleLarge)

        if (tx == null) {
            Text("Data tidak ditemukan.")
            OutlinedButton(onClick = onBack) { Text("Back") }
        } else {
            val t = tx!!
            Text("Merchant: ${t.merchant}")
            Text("Date: ${t.dateIso}")
            Text("Total: ${t.total}")

            Text("OCR Raw", style = MaterialTheme.typography.titleMedium)
            Text(t.rawOcrText, style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(
                    onClick = {
                        vm.delete(id) { onBack() }
                    }
                ) { Text("Hapus") }
            }
        }
    }
}
