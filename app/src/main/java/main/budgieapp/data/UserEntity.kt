package main.budgieapp.data

data class UserEntity(
    val id: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordIterations: Int
)
