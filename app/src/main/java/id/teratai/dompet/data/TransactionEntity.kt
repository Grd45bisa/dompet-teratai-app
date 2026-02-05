package id.teratai.dompet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    /** ISO yyyy-MM-dd (string for MVP simplicity) */
    val dateIso: String,
    /** normalized numeric string, e.g. 12345.67 */
    val total: String,
    val rawOcrText: String,
    /** Uri string pointing to cached image (optional) */
    val imageUri: String?,
    val createdAtMs: Long,
)
