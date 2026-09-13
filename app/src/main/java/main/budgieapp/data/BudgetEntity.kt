package main.budgieapp.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "budgets", indices = [Index(value = ["categoryId", "period"], unique = true)])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String,
    val period: String
)
