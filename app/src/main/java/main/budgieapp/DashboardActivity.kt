package main.budgieapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import main.budgieapp.widgets.BudgetsWidget
import main.budgieapp.widgets.CashFlowPeriod
import main.budgieapp.widgets.CashFlowWidget
import main.budgieapp.widgets.IncomeVsExpensesWidget

class DashboardActivity : AppCompatActivity() {

    private var accountsPage: View? = null
    private var budgetsPage: View? = null
    private var budgetsRefreshJob: Job? = null
    private lateinit var fabDashboardActions: FloatingActionButton
    private lateinit var fabAddExpense: ExtendedFloatingActionButton
    private lateinit var fabAddIncome: ExtendedFloatingActionButton
    private var actionsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        fabDashboardActions = findViewById(R.id.fabDashboardActions)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        fabAddIncome = findViewById(R.id.fabAddIncome)

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

        setupDashboardPager()

    }

    override fun onStart() {
        super.onStart()
        refreshAccountsPage()
        refreshBudgetsPage()
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
                if (position == 1){
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
        IncomeVsExpensesWidget.bindNavigation(page.findViewById(R.id.incomeVsExpensesWidget))

        val cashFlowWidget = CashFlowWidget(page.findViewById(R.id.cashFlowWidget))

        cashFlowWidget.SetData(
            listOf(
                CashFlowPeriod("Jan", 5000f, 3200f),
                CashFlowPeriod("Feb", 5200f, 4100f),
                CashFlowPeriod("Mar", 4800f, 5300f),
                CashFlowPeriod("Apr", 6100f, 3900f),
                CashFlowPeriod("May", 5500f, 4200f),
                CashFlowPeriod("Jun", 6400f, 4600f),
                CashFlowPeriod("Jul", 4500f, 4000f),
                CashFlowPeriod("Aug", 5100f, 4400f),
                CashFlowPeriod("Sep", 5600f, 4300f)
            )
        )
    }

    private fun refreshAccountsPage(){
        val page = accountsPage ?: return

        lifecycleScope.launch {
            IncomeVsExpensesWidget.refresh(page.findViewById(R.id.incomeVsExpensesWidget))
        }
    }

    private fun refreshBudgetsPage(){
        val page = budgetsPage ?: return

        budgetsRefreshJob?.cancel()
        budgetsRefreshJob = lifecycleScope.launch {
            BudgetsWidget.refresh(
                page.findViewById<View>(R.id.budgetsWidget)
            )
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
