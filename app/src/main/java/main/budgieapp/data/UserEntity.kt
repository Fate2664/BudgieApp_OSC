package main.budgieapp.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey


@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordIterations: Int
)
