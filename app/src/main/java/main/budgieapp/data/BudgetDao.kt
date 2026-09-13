package main.budgieapp.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import main.budgieapp.Category

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: BudgetEntity)

    @Query("SELECT * FROM budgets ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<BudgetEntity>

    @Query("""SELECT * FROM budgets WHERE categoryId = :categoryId AND period = :period LIMIT 1""")
    suspend fun findForCategory(categoryId: String, period: String): BudgetEntity?
}