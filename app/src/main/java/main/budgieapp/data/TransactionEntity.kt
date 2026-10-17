package main.budgieapp.data

data class TransactionEntity(
    val id: String,
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
    val attachmentUri: String?,
    val goalId: String? = null
)
