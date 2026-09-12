package main.budgieapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import main.budgieapp.widgets.CashFlowPeriod
import main.budgieapp.widgets.CashFlowWidget
import main.budgieapp.widgets.IncomeVsExpensesWidget

class DashboardActivity : AppCompatActivity() {

    private lateinit var fabDashboardActions: FloatingActionButton
    private lateinit var fabAddExpense: ExtendedFloatingActionButton
    private lateinit var fabAddIncome : ExtendedFloatingActionButton
    private var actionsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        IncomeVsExpensesWidget.bindNavigation(findViewById(R.id.incomeVsExpensesWidget))

        fabDashboardActions = findViewById(R.id.fabDashboardActions)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        fabAddIncome = findViewById(R.id.fabAddIncome)

        fabDashboardActions.setOnClickListener {
            if (actionsOpen){
                closeActionMenu()
            }else{
                openActionMenu()
            }
        }

        fabAddExpense.setOnClickListener {
            openTransaction(TransactionType.EXPENSE)
        }

        fabAddIncome.setOnClickListener {
            openTransaction(TransactionType.INCOME)
        }


        val cashFlowWidget = CashFlowWidget(findViewById(R.id.cashFlowWidget))

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

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            IncomeVsExpensesWidget.refresh(findViewById(R.id.incomeVsExpensesWidget))
        }
    }

    private fun openTransaction(type: TransactionType){
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            putExtra(AddTransactionActivity.EXTRA_TRANSACTION_TYPE, type.name)
        }
        startActivity(intent)
        closeActionMenu()
    }

    private fun openActionMenu(){
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

    private fun closeActionMenu(){
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
