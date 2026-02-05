package id.teratai.dompet.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()
    suspend fun insert(tx: TransactionEntity): Long = dao.insert(tx)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
