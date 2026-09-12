package main.budgieapp.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateTimeMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("""SELECT * FROM transactions WHERE type = :type ORDER BY dateTimeMillis DESC""")
    suspend fun getByType(type: String): List<TransactionEntity>

}