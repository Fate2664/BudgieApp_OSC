package main.budgieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DashboardPagerAdapter(
    private val onAccountsCreated: (View) -> Unit,
    private val onBudgetsCreated: (View) -> Unit
) :
    RecyclerView.Adapter<DashboardPagerAdapter.PageHolder>() {

    class PageHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int = 2
    override fun getItemViewType(position: Int): Int = position
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val layout = when (viewType) {
            0 -> R.layout.page_dashboard_accounts
            else -> R.layout.page_dashboard_budgets_goals
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)

        if (viewType == 0) {
            onAccountsCreated(view)
        } else {
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

        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
    }
}