package main.budgieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DashboardPagerAdapter(
    private val onAccountsCreated: (View) -> Unit,
    private val onBudgetsCreated: (View) -> Unit,
    private val onAIChatCreated: (View) -> Unit
) :
    RecyclerView.Adapter<DashboardPagerAdapter.PageHolder>() {

    class PageHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int = 3
    override fun getItemViewType(position: Int): Int = position
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val layout = when (viewType) {
            0 -> R.layout.page_dashboard_accounts
            1 -> R.layout.page_dashboard_budgets_goals
            2 -> R.layout.page_dashboard_aichat
            else -> error("Unknown dashboard page")
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)

        when (viewType) {
            0 -> onAccountsCreated(view)
            1 -> {
                onBudgetsCreated(view)

                view.findViewById<View>(R.id.btnAddNewBudget)
                    .setOnClickListener {
                        view.context.startActivity(
                            android.content.Intent(
                                view.context,
                                AddBudgetActivity::class.java
                            )
                        )
                    }

                view.findViewById<View>(R.id.btnAddNewGoal)
                    .setOnClickListener {
                        view.context.startActivity(
                            android.content.Intent(
                                view.context,
                                AddNewGoalActivity::class.java
                            )
                        )
                    }
            }

            2 -> onAIChatCreated(view)
        }
        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) = Unit
}