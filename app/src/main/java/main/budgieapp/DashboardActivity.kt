package main.budgieapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import main.budgieapp.widgets.CashFlowPeriod
import main.budgieapp.widgets.CashFlowWidget

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

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
                CashFlowPeriod("Sep", 5600f, 4300f),
            )
        )

    }
}