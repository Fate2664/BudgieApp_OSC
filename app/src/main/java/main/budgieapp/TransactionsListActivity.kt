package main.budgieapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.TransactionAdapter
import org.w3c.dom.Text
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TransactionsListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }

    private lateinit var transactionType: TransactionType
    private lateinit var recycler: RecyclerView
    private lateinit var message: TextView
    private lateinit var total: TextView

    private lateinit var fromDateText: TextView
    private lateinit var toDateText: TextView

    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null
    private var loadJob: Job? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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
        fromDateText = findViewById(R.id.txtSelectedFromDate)
        toDateText = findViewById(R.id.txtSelectedToDate)

        fromDate = savedInstanceState?.getString("filter_from_date")?.let { LocalDate.parse(it) }
        toDate = savedInstanceState?.getString("filter_to_date")?.let { LocalDate.parse(it) }

        updateDateLabels()

        fromDateText.setOnClickListener {
            showDatePicker(isFromDate = true)
        }

        toDateText.setOnClickListener {
            showDatePicker(isFromDate = false)
        }
    }

    override fun onStart() {
        super.onStart()
        loadTransactions()
    }

    private fun loadTransactions() {
        loadJob?.cancel()
        val zone = ZoneId.systemDefault()
        val startMillis = fromDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        val endExclusiveMillis =
            toDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        message.setText(R.string.transactions_loading)
        message.isVisible = true
        recycler.isVisible = false
        total.text = ""

        loadJob = lifecycleScope.launch {
            try {
                val transactions = BudgieDatabase
                    .getInstance(applicationContext)
                    .transactionDao()
                    .getByType(transactionType.name)
                    .filter { transaction ->
                        val timestamp = transaction.dateTimeMillis
                        (startMillis == null || timestamp >= startMillis) &&
                                (endExclusiveMillis == null || timestamp < endExclusiveMillis)
                    }

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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("TransactionsList", "Could not load transactions", error)
                message.setText(R.string.transactions_load_error)
                message.isVisible = true
            }
        }
    }

    private fun updateDateLabels() {
        fromDateText.text = fromDate?.format(dateFormatter) ?: "Any date"
        toDateText.text = toDate?.format(dateFormatter) ?: "Any date"
    }

    private fun showDatePicker(isFromDate: Boolean) {
        val initialDate = if (isFromDate) {
            fromDate ?: toDate ?: LocalDate.now()
        } else {
            toDate ?: fromDate ?: LocalDate.now()
        }

        DatePickerDialog(
            this, { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                val invalidRange = if (isFromDate) {
                    toDate?.let { selectedDate.isAfter(it) } ?: false
                } else {
                    fromDate?.let { selectedDate.isBefore(it) } ?: false
                }

                if (invalidRange) {
                    Toast.makeText(
                        this,
                        "From date must be on or before To date.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (isFromDate) {
                        fromDate = selectedDate
                    } else {
                        toDate = selectedDate
                    }

                    updateDateLabels()
                    loadTransactions()
                }
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).apply {
            setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Clear") { _, _ ->
                if (isFromDate) {
                    fromDate = null
                } else {
                    toDate = null
                }

                updateDateLabels()
                loadTransactions()
            }
        }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("filter_from_date", fromDate?.toString())
        outState.putString("filter_to_date", toDate?.toString())
        super.onSaveInstanceState(outState)
    }

}
