package main.budgieapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.TransactionAdapter
import org.w3c.dom.Text
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class TransactionsListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }

    private lateinit var transactionType: TransactionType
    private lateinit var recycler: RecyclerView
    private lateinit var message: TextView
    private lateinit var total: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transactions_list)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val typeName = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
        transactionType =
            TransactionType.entries.firstOrNull() { it.name == typeName } ?: TransactionType.EXPENSE
        val isIncome = transactionType == TransactionType.INCOME

        findViewById<MaterialToolbar>(R.id.topBar).apply {
            setTitle(
                if (isIncome) R.string.transactions_income_title
                else R.string.transactions_expense_title
            )
            setNavigationOnClickListener { finish() }
        }

        findViewById<TextView>(R.id.txtTransactionTotalLabel).setText(
            if (isIncome) R.string.transactions_total_income
            else R.string.transactions_total_expenses
        )

        total = findViewById(R.id.txtTransactionTotal)
        total.setTextColor(
            getColor(
                if (isIncome) R.color.light_green
                else R.color.light_red
            )
        )

        message = findViewById(R.id.txtTransactionsMessage)
        recycler = findViewById(R.id.recyclerTransactions)
        recycler.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        loadTransactions()
    }

    private fun loadTransactions() {
        message.setText(R.string.transactions_loading)
        message.isVisible = true
        recycler.isVisible = false
        total.text = ""

        lifecycleScope.launch {
            try {
                val transactions = BudgieDatabase
                    .getInstance(applicationContext)
                    .transactionDao()
                    .getByType(transactionType.name)

                recycler.adapter = TransactionAdapter(transactions)
                recycler.isVisible = transactions.isNotEmpty()

                message.setText(R.string.transactions_empty)
                message.isVisible = transactions.isEmpty()

                val totalAmount = transactions.fold(BigDecimal.ZERO) { sum, transaction ->
                    sum.add(BigDecimal.valueOf(transaction.amountCents, 2))
                }
                total.text = NumberFormat
                    .getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
                    .format(totalAmount)
            }catch (cancelled: CancellationException){
                throw cancelled
            }catch (error: Exception){
                Log.e("TransactionsList", "Could not load transactions", error)
                message.setText(R.string.transactions_load_error)
                message.isVisible = true
            }
        }
    }

}
