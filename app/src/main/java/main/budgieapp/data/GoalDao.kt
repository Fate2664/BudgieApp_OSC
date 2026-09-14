package main.budgieapp.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY targetDateMillis ASC")
    suspend fun getAll(): List<GoalEntity>

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}