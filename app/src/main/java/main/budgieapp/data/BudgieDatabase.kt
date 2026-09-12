package main.budgieapp.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)

abstract class BudgieDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: BudgieDatabase? = null

        fun getInstance(context: Context): BudgieDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<BudgieDatabase>(
                    context.applicationContext,
                    "budgie.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { instance = it }
            }
        }
    }
}