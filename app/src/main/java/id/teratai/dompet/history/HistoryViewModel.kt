package id.teratai.dompet.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.teratai.dompet.data.DatabaseProvider
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.data.TransactionRepository
import id.teratai.dompet.util.StorageCleanup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao by lazy { DatabaseProvider.get(getApplication()).transactionDao() }
    private val repo: TransactionRepository by lazy {
        TransactionRepository(dao)
    }

    val items: StateFlow<List<TransactionEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
            StorageCleanup.clearAllUserData(getApplication())
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
