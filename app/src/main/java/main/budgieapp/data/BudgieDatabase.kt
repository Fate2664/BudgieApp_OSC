package main.budgieapp.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, GoalEntity::class, UserEntity::class],
    version = 5,
    exportSchema = false
)

abstract class BudgieDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: BudgieDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        categoryId TEXT NOT NULL,
                        period TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                ).use { it.step() }

                connection.prepare("""CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_period ON budgets(categoryId, period)""".trimIndent())
                    .use { it.step() }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    """
              CREATE TABLE IF NOT EXISTS goals (
                  id TEXT NOT NULL,
                  name TEXT NOT NULL,
                  targetAmountCents INTEGER NOT NULL,
                  savedAmountCents INTEGER NOT NULL,
                  targetDateMillis INTEGER NOT NULL,
                  color INTEGER NOT NULL,
                  iconKey TEXT NOT NULL,
                  PRIMARY KEY(id)
              )
              """.trimIndent()
                ).use { it.step() }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    "ALTER TABLE transactions ADD COLUMN goalId TEXT"
                ).use { it.step() }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    """
              CREATE TABLE IF NOT EXISTS users (
                  id TEXT NOT NULL,
                  email TEXT NOT NULL,
                  passwordHash TEXT NOT NULL,
                  passwordSalt TEXT NOT NULL,
                  passwordIterations INTEGER NOT NULL,
                  PRIMARY KEY(id)
              )
              """.trimIndent()
                ).use { it.step() }

                connection.prepare(
                    """
              CREATE UNIQUE INDEX IF NOT EXISTS index_users_email
              ON users(email)
              """.trimIndent()
                ).use { it.step() }
            }
        }

        fun getInstance(context: Context): BudgieDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<BudgieDatabase>(
                    context.applicationContext,
                    "budgie.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
