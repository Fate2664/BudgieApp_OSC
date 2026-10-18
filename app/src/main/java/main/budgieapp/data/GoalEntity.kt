package main.budgieapp.data


data class GoalEntity(
    val id: String = "",
    val name: String = "",
    val targetAmountCents: Long = 0L,
    val savedAmountCents: Long = 0L,
    val targetDateMillis: Long = 0L,
    val color: Int = 0,
    val iconKey: String = ""
)
