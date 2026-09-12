package main.budgieapp.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val description: String,
    val payee: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColor: Int,
    val dateTimeMillis: Long,
    val paymentType: String,
    val status: String,
    val attachmentUri: String?
)
