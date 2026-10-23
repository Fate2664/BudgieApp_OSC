package main.budgieapp.widgets

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import main.budgieapp.AddTransactionActivity
import main.budgieapp.GoalIcons
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.data.GoalRepo
import main.budgieapp.data.TransactionRepo
import java.math.BigDecimal
import java.math.RoundingMode
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
            val transactionRepo = TransactionRepo.forCurrentUser()
            val goalRepo = GoalRepo.forCurrentUser()
            val goals = goalRepo.getAll()

            val contributionsByGoal = transactionRepo.getByType("EXPENSE")
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

                renderMilestones(
                    row = row,
                    savedCents = totalSavedCents,
                    targetCents = goal.targetAmountCents,
                    goalColor = goal.color,
                    currency = currency
                )

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
                    val intent = Intent(
                        root.context,
                        AddTransactionActivity::class.java
                    ).apply {
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

                row.findViewById<View>(R.id.btnOptions).apply {
                    contentDescription = "Options for ${goal.name}"
                    setOnClickListener { anchor ->
                        val popup = PopupMenu(root.context, anchor)
                        popup.menu.add("Delete goal").setOnMenuItemClickListener {
                            MaterialAlertDialogBuilder(root.context)
                                .setTitle("Delete goal?")
                                .setMessage("Delete \"${goal.name}\"? This cannot be undone. " + "Existing transactions will be kept.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Confirm") { _, _ ->
                                    val lifecycleOwner = root.findViewTreeLifecycleOwner()

                                    if (lifecycleOwner == null) {
                                        Toast.makeText(
                                            root.context, "Could not delete goal. Please try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        lifecycleOwner.lifecycleScope.launch {
                                            anchor.isEnabled = false

                                            try {
                                                goalRepo.deleteById(goal.id)
                                                refresh(root)
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (error: Exception) {
                                                Log.e(
                                                    "GoalsWidget",
                                                    "Could not delete goal",
                                                    error
                                                )

                                                Toast.makeText(
                                                    root.context,
                                                    "Could not delete goal. Please try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                anchor.isEnabled = true
                                            }
                                        }
                                    }
                                }
                                .show()

                            true
                        }

                        popup.show()
                    }
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

    private fun renderMilestones(
        row: View,
        savedCents: BigDecimal,
        targetCents: Long,
        goalColor: Int,
        currency: NumberFormat
    ) {
        val milestones = listOf(
            25 to row.findViewById<TextView>(R.id.milestone25),
            50 to row.findViewById<TextView>(R.id.milestone50),
            75 to row.findViewById<TextView>(R.id.milestone75),
            100 to row.findViewById<TextView>(R.id.milestone100)
        )

        val message = row.findViewById<TextView>(R.id.txtGoalMilestoneContext)
        val fill = row.findViewById<View>(R.id.goalMilestoneFill)
        val validTarget = targetCents > 0L
        val saved = savedCents.max(BigDecimal.ZERO)
        var target = BigDecimal.valueOf(targetCents)
        val milestoneColor =
            Color.argb(255, Color.red(goalColor), Color.green(goalColor), Color.blue(goalColor))

        fun requireCents(percent: Int): BigDecimal =
            target.multiply(
                BigDecimal
                    .valueOf(percent.toLong())
            )
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)

        val nextIndex = if (validTarget) {
            milestones.indexOfFirst { (percent, _) -> saved < requireCents(percent) }
        } else {
            -1
        }

        val density = row.resources.displayMetrics.density
        val strokeWidth = (2 * density).toInt().coerceAtLeast(1)
        val grey = Color.rgb(180, 180, 190)

        milestones.forEachIndexed { index, (percent, circle) ->
            val reached = validTarget && saved >= requireCents(percent)
            val next = validTarget && index == nextIndex

            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (reached) milestoneColor else Color.WHITE)
                setStroke(strokeWidth, if (reached || next) milestoneColor else grey)
            }

            circle.text = if (reached) "\u2713" else ""
            circle.setTextColor(Color.WHITE)
            circle.contentDescription = when {
                reached -> "$percent percent milestone reached"
                next -> "$percent percent milestone next"
                else -> "$percent percent milestone"
            }
        }

        message.text = when {
            !validTarget -> "Set a target amount to track milestones"
            nextIndex == -1 -> "Goal reached!"
            else -> {
                val nextPercentage = milestones[nextIndex].first
                val needed = requireCents(nextPercentage)
                    .subtract(saved)
                    .max(BigDecimal.ZERO)
                val destination = when (nextPercentage) {
                    50 -> "halfway"
                    100 -> "your goal"
                    else -> "$nextPercentage%"
                }

                "${currency.format(needed.movePointLeft(2))} until $destination"
            }
        }

        fill.setBackgroundColor(milestoneColor)
        fill.post {
            val firstCircle = milestones.first().second
            val endIndex = when {
                !validTarget -> 0
                nextIndex == -1 -> milestones.lastIndex
                else -> nextIndex
            }
            val endCircle = milestones[endIndex].second
            val startX = firstCircle.left + firstCircle.width / 2f
            val endX = endCircle.left + endCircle.width / 2f

            fill.layoutParams = fill.layoutParams.apply {
                width = (endX - startX).toInt().coerceAtLeast(0)
            }
        }
    }
}