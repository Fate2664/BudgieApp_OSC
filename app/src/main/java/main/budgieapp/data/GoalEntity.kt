package main.budgieapp.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountCents: Long,
    val savedAmountCents: Long,
    val targetDateMillis: Long,
    val color: Int,
    val iconKey: String
)
