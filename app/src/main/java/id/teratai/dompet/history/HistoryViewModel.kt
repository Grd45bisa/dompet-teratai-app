package id.teratai.dompet.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.teratai.dompet.data.DatabaseProvider
import id.teratai.dompet.data.TransactionEntity
import id.teratai.dompet.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: TransactionRepository by lazy {
        val db = DatabaseProvider.get(getApplication())
        TransactionRepository(db.transactionDao())
    }

    val items: StateFlow<List<TransactionEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
