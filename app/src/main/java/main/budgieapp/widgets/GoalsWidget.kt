package main.budgieapp.widgets

import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CancellationException
import main.budgieapp.AddTransactionActivity
import main.budgieapp.GoalIcons
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.data.BudgieDatabase
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoalsWidget {
    suspend fun refresh(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.goalListContainer)

        fun showMessage(message: String) {
            container.removeAllViews()
            container.addView(TextView(root.context).apply {
                text = message
                textSize = 15f
                setTextColor(root.context.getColor(R.color.soft_black))
            })
        }

        showMessage("Loading goals...")

        try {
            val database = BudgieDatabase.getInstance(root.context)
            val goals = database.goalDao().getAll()

            val contributionsByGoal = database.transactionDao()
                .getByType("EXPENSE")
                .filter { it.goalId != null && it.status == "Cleared" }
                .groupBy { it.goalId }
                .mapValues { (_, transactions) ->
                    transactions.fold(BigDecimal.ZERO) { total, transaction
                        ->
                        total.add(BigDecimal.valueOf(transaction.amountCents))
                    }
                }

            container.removeAllViews()

            if (goals.isEmpty()) {
                showMessage("No goals yet. Tap + to create one")
                return
            }

            val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            fun money(cents: Long): String = currency.format(BigDecimal.valueOf(cents, 2))

            for (goal in goals) {
                val row = LayoutInflater.from(root.context)
                    .inflate(R.layout.item_goal, container, false)

                val totalSavedCents = BigDecimal.valueOf(goal.savedAmountCents)
                    .add(contributionsByGoal[goal.id] ?: BigDecimal.ZERO)

                val percentage = if (goal.targetAmountCents > 0L) {
                    totalSavedCents
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                            BigDecimal.valueOf(goal.targetAmountCents),
                            0,
                            RoundingMode.HALF_UP
                        )
                } else {
                    BigDecimal.ZERO
                }

                row.findViewById<TextView>(R.id.txtGoalName).text = goal.name
                row.findViewById<TextView>(R.id.txtGoalTargetDate).text =
                    "Target date: ${dateFormatter.format(Date(goal.targetDateMillis))}"
                row.findViewById<TextView>(R.id.txtGoalSavedAmount).text =
                    "${currency.format(totalSavedCents.movePointLeft(2))} / " +
                            money(goal.targetAmountCents)
                row.findViewById<TextView>(R.id.txtGoalPercentage).text = "$percentage%"
                row.findViewById<MaterialCardView>(R.id.cardGoalIcon)
                    .setCardBackgroundColor(goal.color)
                row.findViewById<ImageView>(R.id.imgGoalIcon).apply {
                    setImageResource(GoalIcons.drawableFor(goal.iconKey))
                    contentDescription = "${goal.name} goal"
                }
                row.findViewById<ProgressBar>(R.id.goalProgress).apply {
                    max = 100
                    progress = percentage.coerceIn(BigDecimal.ZERO, BigDecimal.valueOf(100)).toInt()
                    progressTintList = ColorStateList.valueOf(goal.color)
                }

                row.findViewById<View>(R.id.btnAddMoney).setOnClickListener {
                    val intent = Intent(root.context,
                        AddTransactionActivity::class.java).apply {
                        putExtra(
                            AddTransactionActivity.EXTRA_TRANSACTION_TYPE,
                            TransactionType.EXPENSE.name
                        )
                        putExtra(AddTransactionActivity.EXTRA_GOAL_ID, goal.id)
                        putExtra(
                            AddTransactionActivity.EXTRA_GOAL_NAME,
                            goal.name
                        )
                    }

                    root.context.startActivity(intent)
                }

                container.addView(row)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("GoalsWidget", "Could not load goals", error)
            showMessage("Could not load goals. Please try again.")
        }
    }
}