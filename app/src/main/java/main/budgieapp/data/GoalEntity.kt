package main.budgieapp.data


data class GoalEntity(
    val id: String,
    val name: String,
    val targetAmountCents: Long,
    val savedAmountCents: Long,
    val targetDateMillis: Long,
    val color: Int,
    val iconKey: String
)
