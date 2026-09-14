package main.budgieapp.widgets

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.data.TransactionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private data class CategoryExpense(
    val name: String,
    val color: Int,
    val amountCents: Long
)

class ExpenseStructureWidget(private val rootView: View) {

    private val chart: PieChart = rootView.findViewById(R.id.expensePieChart)
    private val periodLabel: TextView = rootView.findViewById(R.id.txtTimePeriod)
    private val totalLabel: TextView = rootView.findViewById(R.id.txtTotalExpenses)
    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val percentage = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }
    private var categories: List<CategoryExpense> = emptyList()
    private var totalCents = 0L

    init {
        configureChart()
    }

    private fun configureChart() {
        chart.apply {
            description.isEnabled = false
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            holeRadius = 65f
            transparentCircleRadius = 65f

            setDrawCenterText(true)
            setCenterTextSize(15f)
            setCenterTextColor(Color.rgb(33, 33, 33))
            setCenterTextRadiusPercent(90f)

            setDrawEntryLabels(false)

            isRotationEnabled = false
            rotationAngle = 0f
            setTouchEnabled(true)
            isHighlightPerTapEnabled = true

            setNoDataText("No expenses in this period")
            setNoDataTextColor(Color.GRAY)
            setExtraOffsets(8f, 8f, 8f, 12f)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                form = Legend.LegendForm.CIRCLE
                formSize = 10f
                textSize = 12f
                textColor = Color.rgb(33, 33, 33)
                xEntrySpace = 12f
                yEntrySpace = 6f
                yOffset = 12f
                maxSizePercent = 0.4f
            }
        }

        chart.setOnChartValueSelectedListener(
            object : OnChartValueSelectedListener {
                override fun onValueSelected(
                    entry: Entry?,
                    highlight: Highlight?
                ) {
                    val index = highlight?.x?.toInt() ?: return
                    showCategory(index)
                }

                override fun onNothingSelected() {
                    showTotal()
                }
            }
        )
    }

    fun setupTimePeriodDropdown(
        initialRange: CashFlowTimeRange,
        onRangeSelected: (CashFlowTimeRange) -> Unit
    ) {
        periodLabel.text = initialRange.title
        periodLabel.setOnClickListener {
            val options = CashFlowTimeRange.entries
            val popup = PopupMenu(rootView.context, periodLabel)

            options.forEachIndexed { index, range ->
                popup.menu.add(0, index, index, range.title)
            }

            popup.setOnMenuItemClickListener { item ->
                val range = options[item.itemId]
                periodLabel.text = range.title
                onRangeSelected(range)
                true
            }

            popup.show()
        }
    }

    fun setTransactions(transactions: List<TransactionEntity>) {
        categories = transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryId }
            .map { (_, entries) ->
                val latest = entries.maxByOrNull { it.dateTimeMillis }!!

                CategoryExpense(
                    name = latest.categoryName.ifBlank { "Uncategorised" },
                    color = latest.categoryColor,
                    amountCents = entries.sumOf { it.amountCents }
                )
            }
            .filter { it.amountCents > 0L }
            .sortedByDescending { it.amountCents }

        totalCents = categories.sumOf { it.amountCents }
        totalLabel.text = formatMoney(totalCents)

        if (categories.isEmpty()) {
            chart.centerText = ""
            chart.setNoDataText("No expenses in this period")
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = categories.map { category ->
            PieEntry(
                (category.amountCents / 100.0).toFloat(),
                category.name
            )
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = categories.map { it.color }
            sliceSpace = 2f
            selectionShift = 8f

            setDrawValues(false)
            isHighlightEnabled = true
        }

        chart.data = PieData(dataSet)
        chart.notifyDataSetChanged()

        chart.highlightValue(0f, 0, false)
        showCategory(0)
        chart.invalidate()
    }

    fun showMessage(message: String) {
        categories = emptyList()
        totalCents = 0L
        totalLabel.text = "—"
        chart.centerText = ""
        chart.setNoDataText(message)
        chart.clear()
        chart.invalidate()
    }

    private fun showCategory(index: Int) {
        val category = categories.getOrNull(index) ?: return
        if (totalCents <= 0L) return

        val share = category.amountCents.toDouble() / totalCents

        chart.centerText =
            "${category.name}\n" + "${formatMoney(category.amountCents)}\n" + percentage.format(
                share
            )
        chart.invalidate()
    }

    private fun showTotal() {
        chart.centerText = "Total expenses\n${formatMoney(totalCents)}"
        chart.invalidate()
    }

    private fun formatMoney(cents: Long): String {
        return currency.format(BigDecimal.valueOf(cents, 2))
    }
}