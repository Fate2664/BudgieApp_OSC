package main.budgieapp.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateTimeMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("""SELECT * FROM transactions WHERE type = :type ORDER BY dateTimeMillis DESC""")
    suspend fun getByType(type: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(transaction: TransactionEntity): Long

    @Query("""
         SELECT * FROM transactions
         WHERE dateTimeMillis >= :startMillis
           AND dateTimeMillis <= :endMillis
         ORDER BY dateTimeMillis ASC
     """)
    suspend fun getInRange(
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSample(transaction: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int

}