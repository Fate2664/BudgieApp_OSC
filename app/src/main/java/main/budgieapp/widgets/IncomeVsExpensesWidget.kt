package main.budgieapp.widgets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CancellationException
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.TransactionsListActivity
import main.budgieapp.data.BudgieDatabase
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class IncomeVsExpensesWidget : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.widget_income_vs_expenses)
        bindNavigation(findViewById(R.id.main))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        fun bindNavigation(root: View) {
            fun openTransactions(type: TransactionType) {
                root.context.startActivity(
                    Intent(root.context, TransactionsListActivity::class.java)
                        .putExtra(TransactionsListActivity.EXTRA_TRANSACTION_TYPE, type.name)
                )
            }

            root.findViewById<View>(R.id.incomeRow).setOnClickListener {
                openTransactions(TransactionType.INCOME)
            }
            root.findViewById<View>(R.id.expensesRow).setOnClickListener {
                openTransactions(TransactionType.EXPENSE)
            }
        }

        suspend fun refresh(root: View){
            val netText = root.findViewById<TextView>(R.id.txtNetTotal)
            val incomeText = root.findViewById<TextView>(R.id.txtIncomeAmount)
            val expenseText = root.findViewById<TextView>(R.id.txtExpensesAmount)
            val incomeBar = root.findViewById<ProgressBar>(R.id.incomeProgress)
            val expenseBar = root.findViewById<ProgressBar>(R.id.expensesProgress)

            try{
                val transactions = BudgieDatabase
                    .getInstance(root.context)
                    .transactionDao()
                    .getAll()

                var income = BigDecimal.ZERO
                var expense = BigDecimal.ZERO

                for(transaction in transactions){
                    val amount = BigDecimal.valueOf(transaction.amountCents, 2)
                    when (transaction.type){
                        TransactionType.INCOME.name -> income = income.add(amount)
                        TransactionType.EXPENSE.name -> expense = expense.add(amount)
                    }
                }

                val net = income.subtract(expense)
                val combined = income.add(expense)
                val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

                incomeText.text = currency.format(income)
                expenseText.text = currency.format(expense)
                netText.text = currency.format(net)

                val netColor = when {
                    net.signum() > 0 -> R.color.light_green
                    net.signum() < 0 -> R.color.light_red
                    else -> R.color.black
                }
                netText.setTextColor(root.context.getColor(netColor))

                val progressMax = 10_000
                incomeBar.max = progressMax
                expenseBar.max = progressMax

                if (combined.signum() > 0){
                    val incomeProgress = income
                        .multiply(BigDecimal.valueOf(progressMax.toLong()))
                        .divide(combined, 0, RoundingMode.HALF_UP)
                        .toInt()
                        .coerceIn(0, progressMax)

                    incomeBar.progress = incomeProgress
                    expenseBar.progress = progressMax - incomeProgress
                } else {
                    incomeBar.progress = 0
                    expenseBar.progress = 0
                }
            } catch (cancelled: CancellationException){
                throw cancelled
            }catch (error: Exception){
                Log.e("IncomeVsExpensesWidget", "Could not load totals", error)
                netText.text = "Unavailable"
                netText.setTextColor(root.context.getColor(R.color.black))
                incomeText.text = "-"
                expenseText.text = "-"
                incomeBar.progress = 0
                expenseBar.progress = 0
            }
        }
    }
}
