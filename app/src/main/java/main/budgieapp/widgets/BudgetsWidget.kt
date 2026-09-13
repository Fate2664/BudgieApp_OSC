package main.budgieapp.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CancellationException
import main.budgieapp.Category
import main.budgieapp.R
import main.budgieapp.data.BudgieDatabase
import org.json.JSONArray
import org.json.JSONException
import org.w3c.dom.Text
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object BudgetsWidget {

    private fun periodStart(period: String, today: LocalDate): LocalDate = when (period) {
        "Daily" -> today
        "Weekly" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "Monthly" -> today.withDayOfMonth(1)
        "Yearly" -> today.withDayOfYear(1)
        else -> error("Unknown budget period: $period")
    }

    private fun percentage(spent: BigDecimal, limit: BigDecimal): Int {
        if (limit.signum() <= 0) return 0

        return spent.multiply(BigDecimal.valueOf(100))
            .divide(limit, 0, RoundingMode.HALF_UP)
            .coerceIn(BigDecimal.ZERO, BigDecimal.valueOf(100))
            .toInt()
    }

    private fun loadCategoriesById(context: Context): Map<String, Category> {
        val savedJson = context
            .getSharedPreferences("categories", Context.MODE_PRIVATE)
            .getString("categories_json", null) ?: return emptyMap()

        return try{
            val array = JSONArray(savedJson)

            buildMap {
                for (index in 0 until array.length()){
                    val item = array.getJSONObject(index)
                    val category = Category(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        color = item.getInt("color")
                    )
                    put(category.id, category)
                }
            }
        } catch (error: JSONException) {
            Log.e("BudgetsWidget", "Could not load category details", error)
            emptyMap()
        }
    }

    suspend fun refresh(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.budgetListContainer)
        val remainingText = root.findViewById<TextView>(R.id.txtBudgetRemaining)
        val spentText = root.findViewById<TextView>(R.id.txtBudgetSpent)
        val percentageText = root.findViewById<TextView>(R.id.txtBudgetPercentage)
        val summaryBar = root.findViewById<ProgressBar>(R.id.progressMonthlyBudget)

        root.findViewById<TextView>(R.id.txtTimePeriod).text = "MONTHLY BUDGETS · THIS MONTH"

        container.removeAllViews()
        remainingText.text = "Loading budgets…"
        spentText.text = ""
        percentageText.text = ""
        summaryBar.progress = 0

        try {
            val database = BudgieDatabase.getInstance(root.context)
            val budgets = database.budgetDao().getAll()
            val expenses = database.transactionDao().getByType("EXPENSE")
            val categoriesById = loadCategoriesById(root.context)
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val today = java.time.Instant.ofEpochMilli(now)
                .atZone(zone)
                .toLocalDate()
            val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

            fun money(cents: BigDecimal): String = currency.format(cents.movePointLeft(2))

            var monthlyLimit = BigDecimal.ZERO
            var monthlySpent = BigDecimal.ZERO

            for (budget in budgets) {
                val category = categoriesById[budget.categoryId]
                val categoryName = category?.name ?: "Unknown category"
                val categoryColor = category?.color ?: root.context.getColor(R.color.budget_blue)
                val startMillis = periodStart(budget.period, today)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()

                val spent = expenses.asSequence()
                    .filter {
                        it.categoryId == budget.categoryId &&
                                it.status == "Cleared" &&
                                it.dateTimeMillis >= startMillis &&
                                it.dateTimeMillis <= now
                    }.fold(BigDecimal.ZERO) { total, transaction ->
                        total.add(BigDecimal.valueOf(transaction.amountCents))
                    }

                val limit = BigDecimal.valueOf(budget.amountCents)
                val remaining = limit.subtract(spent)
                val row = LayoutInflater.from(root.context)
                    .inflate(R.layout.item_budget, container, false)

                row.findViewById<TextView>(R.id.txtBudgetName).text =
                    "${budget.name} \u00B7 $categoryName \u00B7 ${budget.period}"
                row.findViewById<TextView>(R.id.txtBudgetAmount).text =
                    "${money(spent)} / ${money(limit)}"
                row.findViewById<ProgressBar>(R.id.progressBudget).apply {
                    max = 100
                    progress = percentage(spent, limit)
                    progressTintList = ColorStateList.valueOf(categoryColor)
                }
                row.findViewById<TextView>(R.id.txtBudgetStatus).apply {
                    text = if (remaining.signum() >= 0) {
                        "${money(remaining)} remaining"
                    } else {
                        "${money(remaining.abs())} over limit"
                    }

                    setTextColor(
                        root.context.getColor(
                            if (remaining.signum() < 0) R.color.light_red
                            else R.color.soft_black
                        )
                    )
                }
                container.addView(row)

                if (budget.period == "Monthly") {
                    monthlyLimit = monthlyLimit.add(limit)
                    monthlySpent = monthlySpent.add(spent)
                }
            }

            if (budgets.isEmpty()) {
                container.addView(TextView(root.context).apply {
                    text = "No budgets yet. Tap + to create one."
                    setTextColor(root.context.getColor(R.color.soft_black))
                    textSize = 15f
                })
            }

            val remaining = monthlyLimit.subtract(monthlySpent)

            remainingText.text = when {
                monthlyLimit.signum() == 0 -> "No monthly budgets yet"
                remaining.signum() < 0 -> "${money(remaining.abs())} over limit"
                else -> "${money(remaining)} remaining"
            }

            spentText.text = "${money(monthlySpent)} spent of ${money(monthlyLimit)}"

            summaryBar.max = 100
            summaryBar.progress = percentage(monthlySpent, monthlyLimit)

            percentageText.text = if (monthlyLimit.signum() > 0) {
                val used = monthlySpent
                    .multiply(BigDecimal.valueOf(100))
                    .divide(monthlyLimit, 0, RoundingMode.HALF_UP)
                "$used% used"
            } else {
                ""
            }
        } catch (cancelled: CancellationException){
            throw cancelled
        }catch (error: Exception){
            Log.e("BudgetsWidget", "Could not load budgets", error)
            container.removeAllViews()
            remainingText.text = "Could not load budgets"
            spentText.text = ""
            percentageText.text = ""
            summaryBar.progress = 0
        }
    }
}

