package main.budgieapp.data

data class TransactionEntity(
    val id: String = "",
    val type: String = "",
    val amountCents: Long = 0L,
    val description: String = "",
    val payee: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val categoryColor: Int = 0,
    val dateTimeMillis: Long = 0L,
    val paymentType: String = "",
    val status: String = "",
    val attachmentUri: String? = null,
    val goalId: String? = null
)
