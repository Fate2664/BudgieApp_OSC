package main.budgieapp

import android.content.Context
import main.budgieapp.data.TransactionEntity
import java.time.ZonedDateTime

object SampleTransactions {
    fun create(context: Context): List<TransactionEntity> {
        val now = ZonedDateTime.now()

        fun transaction(
            id: String,
            type: String,
            amountCents: Long,
            description: String,
            payee: String,
            categoryId: String,
            categoryName: String,
            colorRes: Int,
            daysAgo: Long
        ) = TransactionEntity(
            id = "demo-$id",
            type = type,
            amountCents = amountCents,
            description = description,
            payee = payee,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColor = context.getColor(colorRes),
            dateTimeMillis = now.minusDays(daysAgo).toInstant().toEpochMilli(),
            paymentType = "Card",
            status = "Cleared",
            attachmentUri = null,
            goalId = null
        )

        return buildList {
            repeat(4) { index ->
                val daysAgo = index * 4L + 1

                add(
                    transaction(
                        "salary-$index", "INCOME", 50_000L * (index + 1),
                        "Salary payment", "Demo employer",
                        "income", "Income", R.color.category_blue, daysAgo
                    )
                )

                add(
                    transaction(
                        "freelance-$index", "INCOME", 25_000L * (index + 1),
                        "Freelance work", "Demo client",
                        "income", "Income", R.color.category_blue, daysAgo + 3
                    )
                )
            }


            repeat(6) { index ->
                val daysAgo = index * 4L + 1

                add(
                    transaction(
                        "food-$index", "EXPENSE", 18_000L + index *
                                2_500L,
                        "Groceries", "Demo supermarket",
                        "food", "Food", R.color.category_red, daysAgo
                    )
                )

                add(
                    transaction(
                        "transport-$index", "EXPENSE", 6_000L + index *
                                500L,
                        "Travel costs", "Demo transport",
                        "transport", "Transport",
                        R.color.category_blue, daysAgo + 1
                    )
                )

                add(
                    transaction(
                        "shopping-$index", "EXPENSE", 12_000L + index *
                                3_000L,
                        "Household shopping", "Demo store",
                        "shopping", "Shopping",
                        R.color.category_purple, daysAgo + 2
                    )
                )

                add(
                    transaction(
                        "bills-$index", "EXPENSE", 25_000L + index *
                                1_000L,
                        "Household bill", "Demo provider",
                        "bills", "Bills", R.color.category_orange,
                        daysAgo + 3
                    )
                )
            }
        }
    }
}