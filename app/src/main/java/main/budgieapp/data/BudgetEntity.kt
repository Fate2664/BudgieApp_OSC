package main.budgieapp.data


data class BudgetEntity(
    val id: String = "",
    val name: String = "",
    val amountCents: Long = 0L,
    val categoryId: String = "",
    val period: String = "",
    val createdAtMillis: Long = 0L
)
