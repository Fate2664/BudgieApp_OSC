package main.budgieapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import main.budgieapp.widgets.BudgetsWidget
import main.budgieapp.widgets.CashFlowTimeRange
import main.budgieapp.widgets.CashFlowWidget
import main.budgieapp.widgets.ExpenseStructureWidget
import main.budgieapp.widgets.GoalsWidget
import main.budgieapp.widgets.IncomeVsExpensesWidget
import java.time.ZoneId
import main.budgieapp.data.TransactionEntity
import main.budgieapp.data.TransactionRepo
import main.budgieapp.data.AchievementRepo
import main.budgieapp.widgets.AchievementPopup
import main.budgieapp.widgets.AchievementProgressWidget
import main.budgieapp.widgets.SummaryPieWidget

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
    private var aiChatController: AIChatController? = null
    private lateinit var achievementPopup: AchievementPopup


    private val transactionRepo by lazy {
        TransactionRepo.forCurrentUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        achievementPopup = AchievementPopup(findViewById<ConstraintLayout>(R.id.main))
        observeAchievementNotifications()

        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, inserts ->
            val bars = inserts.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            inserts
        }

        ViewCompat.requestApplyInsets(root)

        setupSidebar()

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

        setupDashboardPager()

    }

    override fun onStart() {
        super.onStart()
        refreshAccountsPage()
        refreshBudgetsPage()
    }

    private fun setupDashboardPager() {
        val pager = findViewById<ViewPager2>(R.id.dashboardPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.dashboardBottomNav)

        pager.adapter = DashboardPagerAdapter(
            onAccountsCreated = { page ->
                accountsPage = page
                setupAccountsPage(page)
                refreshAccountsPage()
            },
            onBudgetsCreated = { page ->
                budgetsPage = page
                AchievementProgressWidget.bindNavigation(page.findViewById<View>(R.id.achievementsWidget))
                refreshBudgetsPage()
            },
            onAIChatCreated = { page ->
                aiChatController = AIChatController(
                    host = this,
                    root = page
                )
            }
        )

        pager.offscreenPageLimit = 2

        bottomNav.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {

                R.id.tabAccounts -> 0
                R.id.tabBudgets -> 1
                R.id.tabAIChat -> 2
                else -> return@setOnItemSelectedListener false
            }
            if (pager.currentItem != position) {
                pager.setCurrentItem(position, true)
            }
            true
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateSelectedTab(position)
                if (position == 1) {
                    refreshBudgetsPage()
                }

                if (actionsOpen) closeActionMenu()

                fabDashboardActions.visibility = if (position == 2) View.GONE else View.VISIBLE
            }
        })

        updateSelectedTab(pager.currentItem)
        fabDashboardActions.visibility = if (pager.currentItem == 2) View.GONE else View.VISIBLE
    }

    private fun setupSidebar() {
        val drawer = findViewById<DrawerLayout>(R.id.dashboardDrawer)
        val sidebar = findViewById<NavigationView>(R.id.dashboardSidebar)

        ViewCompat.setOnApplyWindowInsetsListener(sidebar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(sidebar)

        val user = FirebaseAuth.getInstance().currentUser
        val header = sidebar.getHeaderView(0)

        val usernameView = header.findViewById<TextView>(R.id.sidbarName)
        usernameView.text = "My account"
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener(this) { profile ->
                    if (FirebaseAuth.getInstance().currentUser?.uid == user.uid) {
                        usernameView.text = profile.getString("username")
                            ?.takeIf { it.isNotBlank() } ?: "My account"
                    }
                }
                .addOnFailureListener(this) { error ->
                    Log.w("DashboardActivity", "Could not load sidebar username", error)
                }
        }
        header.findViewById<TextView>(R.id.sidebarEmail).text = user?.email.orEmpty()
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            if (actionsOpen) closeActionMenu()
            drawer.openDrawer(GravityCompat.START)
        }

        sidebar.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.sidebarSettings -> {
                    drawer.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, SettingsMenuActivity::class.java))
                    true
                }

                R.id.sidebarSignOut -> {
                    drawer.closeDrawer(GravityCompat.START)
                    confirmSignOut()
                    true
                }

                else -> false
            }
        }

        val drawerBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                drawer.closeDrawer(GravityCompat.START)
            }
        }

        onBackPressedDispatcher.addCallback(this, drawerBackCallback)
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                drawerBackCallback.isEnabled = slideOffset > 0f
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerBackCallback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerBackCallback.isEnabled = false
            }
        })

        drawerBackCallback.isEnabled = drawer.isDrawerVisible(GravityCompat.START)
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign out?")
            .setMessage("You can sign in again to access your account.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }.show()
    }

    private fun updateSelectedTab(position: Int) {
        val itemId = when (position) {
            0 -> R.id.tabAccounts
            1 -> R.id.tabBudgets
            2 -> R.id.tabAIChat
            else -> return
        }

        findViewById<BottomNavigationView>(R.id.dashboardBottomNav).menu.findItem(itemId).isChecked =
            true
    }

    private fun setupAccountsPage(page: View) {
        val greeting = page.findViewById<TextView>(R.id.txtDashboardGreeting)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener(this) { profile ->
                    if (FirebaseAuth.getInstance().currentUser?.uid != user.uid)
                        return@addOnSuccessListener

                    val username = profile.getString("username")?.takeIf { it.isNotBlank() }
                    greeting.text = if (username != null) {
                        getString(R.string.dashboard_banner_greeting_username, username)
                    } else {
                        getString(R.string.dashboard_banner_greeting)
                    }
                }
                .addOnFailureListener(this) { error ->
                    Log.w("DashboardActivity", "Could not load banner username", error)
                }
        }
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

            launch {
                AchievementProgressWidget.refresh(
                    page.findViewById<View>(R.id.achievementsWidget)
                )
            }
        }
    }

    private fun observeAchievementNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: return@repeatOnLifecycle
                try {
                    val repo = AchievementRepo.forCurrentUser()
                    val earned = repo.refreshAndGetEarned(forceRefresh = true)
                    budgetsPage?.let { page ->
                        AchievementProgressWidget.showEarned(
                            page.findViewById(R.id.achievementsWidget), earned
                        )
                    }
                    repo.observePendingNotifications().collect { pending ->
                        if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
                            if (pending.isNotEmpty()) {
                                // Pending IDs are already confirmed by the server listener.
                                // Avoid another full calculation before displaying the popup.
                                val updatedEarned = (repo.getCachedEarned() ?: earned) + pending
                                budgetsPage?.let { page ->
                                    AchievementProgressWidget.showEarned(
                                        page.findViewById(R.id.achievementsWidget), updatedEarned
                                    )
                                }
                            }
                            achievementPopup.show(uid, pending)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("DashboardActivity", "Could not load achievement notifications", error)
                }
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
                val transactionRepo =
                    TransactionRepo.forCurrentUser().getInRange(startMillis, nowMillis)

                widget.setTransactions(
                    transactions = transactionRepo,
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
                val customDates = widget.customDateRange
                val zone = ZoneId.systemDefault()
                val nowMillis = System.currentTimeMillis()
                val today = java.time.Instant.ofEpochMilli(nowMillis)
                    .atZone(zone)
                    .toLocalDate()
                val startMillis = (customDates?.first ?: range.startDate(today))
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val endMillis = customDates?.second?.plusDays(1)
                    ?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()?.minus(1)
                    ?: nowMillis
                val transactions = transactionRepo.getInRange(startMillis, endMillis)

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
