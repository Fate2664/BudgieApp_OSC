package main.budgieapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.widgets.BudgetsWidget
import main.budgieapp.widgets.CashFlowPeriod
import main.budgieapp.widgets.CashFlowTimeRange
import main.budgieapp.widgets.CashFlowWidget
import main.budgieapp.widgets.ExpenseStructureWidget
import main.budgieapp.widgets.GoalsWidget
import main.budgieapp.widgets.IncomeVsExpensesWidget
import java.time.ZoneId
import androidx.core.content.edit

class DashboardActivity : AppCompatActivity() {

    private var accountsPage: View? = null
    private var budgetsPage: View? = null
    private var budgetsRefreshJob: Job? = null
    private lateinit var fabDashboardActions: FloatingActionButton
    private lateinit var fabAddExpense: ExtendedFloatingActionButton
    private lateinit var fabAddIncome: ExtendedFloatingActionButton
    private var actionsOpen = false
    private var cashFlowWidget: CashFlowWidget? = null
    private var cashFlowRefreshJob: Job? = null
    private var selectedCashFlowRange = CashFlowTimeRange.LAST_30_DAYS

    private var expenseStructureWidget: ExpenseStructureWidget? = null
    private var expenseStructureRefreshJob: Job? = null
    private var selectedExpenseRange = CashFlowTimeRange.LAST_30_DAYS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        fabDashboardActions = findViewById(R.id.fabDashboardActions)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        fabAddIncome = findViewById(R.id.fabAddIncome)

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsMenuActivity::class.java))
        }

        fabDashboardActions.setOnClickListener {
            if (actionsOpen) {
                closeActionMenu()
            } else {
                openActionMenu()
            }
        }

        fabAddExpense.setOnClickListener {
            openTransaction(TransactionType.EXPENSE)
        }

        fabAddIncome.setOnClickListener {
            openTransaction(TransactionType.INCOME)
        }

        lifecycleScope.launch {
            loadSampleTransactionsOnce()
            setupDashboardPager()
        }

    }

    override fun onStart() {
        super.onStart()
        refreshAccountsPage()
        refreshBudgetsPage()
    }

    private suspend fun loadSampleTransactionsOnce() {
        val preferences = getSharedPreferences("sample_data", MODE_PRIVATE)

        if (preferences.getBoolean("initial_samples_loaded", false)) {
            return
        }

        try {
            BudgieDatabase.getInstance(applicationContext)
                .transactionDao()
                .insertSample(SampleTransactions.create(applicationContext)
                )

            preferences.edit {
                putBoolean("initial_samples_loaded", true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("DashboardActivity", "Could not load sample transactions", error)
        }
    }

    private fun setupDashboardPager() {
        val pager = findViewById<ViewPager2>(R.id.dashboardPager)
        val accountsTab = findViewById<View>(R.id.tabAccounts)
        val budgetsTab = findViewById<View>(R.id.tabBudgets)

        pager.adapter = DashboardPagerAdapter(
            onAccountsCreated = { page ->
                accountsPage = page
                setupAccountsPage(page)
                refreshAccountsPage()
            },
            onBudgetsCreated = { page ->
                budgetsPage = page
                refreshBudgetsPage()
            }
        )

        pager.offscreenPageLimit = 1

        accountsTab.setOnClickListener { pager.setCurrentItem(0, true) }
        budgetsTab.setOnClickListener { pager.setCurrentItem(1, true) }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateSelectedTab(position)
                if (position == 1) {
                    refreshBudgetsPage()
                }

                if (actionsOpen) closeActionMenu()
            }
        })

        updateSelectedTab(pager.currentItem)
    }

    private fun updateSelectedTab(position: Int) {
        findViewById<View>(R.id.indicatorAccounts).visibility =
            if (position == 0) View.VISIBLE else View.INVISIBLE

        findViewById<View>(R.id.indicatorBudgets).visibility =
            if (position == 1) View.VISIBLE else View.INVISIBLE

        findViewById<View>(R.id.tabAccounts).isSelected = position == 0
        findViewById<View>(R.id.tabBudgets).isSelected = position == 1
    }

    private fun setupAccountsPage(page: View) {
        IncomeVsExpensesWidget.bindNavigation(
            page.findViewById(R.id.incomeVsExpensesWidget)
        )

        cashFlowWidget = CashFlowWidget(
            page.findViewById(R.id.cashFlowWidget)
        ).also { widget ->
            widget.setupTimePeriodDropdown(selectedCashFlowRange) { range ->
                selectedCashFlowRange = range
                refreshCashFlow()
            }
        }

        expenseStructureWidget = ExpenseStructureWidget(
            page.findViewById(R.id.expenseStructureWidget)
        ).also { widget ->
            widget.setupTimePeriodDropdown(selectedExpenseRange) { range ->
                selectedExpenseRange = range
                refreshExpenseStructure()
            }
        }
    }

    private fun refreshAccountsPage() {
        val page = accountsPage ?: return

        refreshCashFlow()
        refreshExpenseStructure()

        lifecycleScope.launch {
            IncomeVsExpensesWidget.refresh(
                page.findViewById(R.id.incomeVsExpensesWidget)
            )
        }
    }

    private fun refreshBudgetsPage() {
        val page = budgetsPage ?: return

        budgetsRefreshJob?.cancel()
        budgetsRefreshJob = lifecycleScope.launch {
            launch {
                BudgetsWidget.refresh(
                    page.findViewById<View>(R.id.budgetsWidget)
                )
            }

            launch {
                GoalsWidget.refresh(
                    page.findViewById<View>(R.id.goalsWidget)
                )
            }
        }
    }

    private fun refreshCashFlow() {
        val widget = cashFlowWidget ?: return
        cashFlowRefreshJob?.cancel()

        cashFlowRefreshJob = lifecycleScope.launch {
            widget.showMessage("Loading cash flow...")


            try {
                val range = selectedCashFlowRange
                val zone = ZoneId.systemDefault()
                val nowMillis = System.currentTimeMillis()
                val today = java.time.Instant.ofEpochMilli(nowMillis)
                    .atZone(zone)
                    .toLocalDate()
                val startMillis = range.startDate(today)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val transactions = BudgieDatabase
                    .getInstance(applicationContext)
                    .transactionDao()
                    .getInRange(startMillis, nowMillis)

                widget.setTransactions(
                    transactions = transactions,
                    range = range,
                    today = today,
                    zone = zone
                )

            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("DashboardActivity", "Could not load cash flow", error)
                widget.showMessage("Could not load cash flow")
            }
        }
    }

    private fun refreshExpenseStructure() {
        val widget = expenseStructureWidget ?: return
        expenseStructureRefreshJob?.cancel()

        expenseStructureRefreshJob = lifecycleScope.launch {
            widget.showMessage("Loading expenses...")
            try {
                val range = selectedExpenseRange
                val zone = ZoneId.systemDefault()
                val nowMillis = System.currentTimeMillis()
                val today = java.time.Instant.ofEpochMilli(nowMillis)
                    .atZone(zone)
                    .toLocalDate()
                val startMillis = range.startDate(today)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val transactions = BudgieDatabase
                    .getInstance(applicationContext)
                    .transactionDao()
                    .getInRange(startMillis, nowMillis)

                widget.setTransactions(transactions)

            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(
                    "DashboardActivity",
                    "Could not load expense structure",
                    error
                )
                widget.showMessage("Could not load expenses")
            }
        }
    }

    private fun openTransaction(type: TransactionType) {
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            putExtra(AddTransactionActivity.EXTRA_TRANSACTION_TYPE, type.name)
        }
        startActivity(intent)
        closeActionMenu()
    }

    private fun openActionMenu() {
        actionsOpen = true

        listOf(fabAddExpense, fabAddIncome).forEach { button ->
            button.animate().cancel()
            button.visibility = View.VISIBLE
            button.alpha = 0f
            button.translationY = 20f

            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .withEndAction(null)
                .start()
        }

        fabDashboardActions.setImageResource(R.drawable.baseline_close_24)
    }

    private fun closeActionMenu() {
        actionsOpen = false

        listOf(fabAddExpense, fabAddIncome).forEach { button ->
            button.animate().cancel()

            button.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(150)
                .withEndAction { button.visibility = View.GONE }
                .start()
        }

        fabDashboardActions.setImageResource(R.drawable.baseline_add_24)
    }

}
