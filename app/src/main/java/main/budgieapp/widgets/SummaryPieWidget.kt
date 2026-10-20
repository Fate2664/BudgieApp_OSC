package main.budgieapp.widgets

import android.graphics.Color
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
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

private data class SummarySlice(
    val name: String,
    val color: Int,
    val amountCents: Long
)

private enum class SummaryMode(val title: String) {
    SPENDING("Spending"),   // expense categories + available funds
    INCOME("Income")        // income categories
}

class SummaryPieWidget(private val rootView: View) {

    private val chart: PieChart = rootView.findViewById(R.id.summaryPieChart)
    private val periodLabel: TextView = rootView.findViewById(R.id.txtSummaryPeriod)
    private val modeLabel: TextView = rootView.findViewById(R.id.txtSummaryMode)
    private val totalLabel: TextView = rootView.findViewById(R.id.txtSummaryTotal)
    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val percentage = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }

    private var transactions: List<TransactionEntity> = emptyList()
    private var mode = SummaryMode.SPENDING

    private var slices: List<SummarySlice> = emptyList()
    private var incomeCents = 0L
    private var expenseCents = 0L
    private var pieTotalCents = 0L   // what the percentages are measured against

    init {
        configureChart()
        setupModeDropdown()
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

            setNoDataText("No data in this period")
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
                override fun onValueSelected(entry: Entry?, highlight: Highlight?) {
                    val index = highlight?.x?.toInt() ?: return
                    showSlice(index)
                }

                override fun onNothingSelected() {
                    showTotal()
                }
            }
        )
    }

    private fun setupModeDropdown() {
        modeLabel.text = mode.title
        modeLabel.setOnClickListener {
            val options = SummaryMode.entries
            val popup = PopupMenu(rootView.context, modeLabel)

            options.forEachIndexed { index, option ->
                popup.menu.add(0, index, index, option.title)
            }

            popup.setOnMenuItemClickListener { item ->
                mode = options[item.itemId]
                modeLabel.text = mode.title
                rebuild()
                true
            }

            popup.show()
        }
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
        this.transactions = transactions
        rebuild()
    }

    /** Groups transactions of one type by category, biggest first. */
    private fun categorySlices(type: TransactionType): List<SummarySlice> {
        return transactions
            .filter { it.type == type.name }
            .groupBy { it.categoryId }
            .map { (_, entries) ->
                val latest = entries.maxByOrNull { it.dateTimeMillis }!!

                SummarySlice(
                    name = latest.categoryName.ifBlank { "Uncategorised" },
                    color = latest.categoryColor,
                    amountCents = entries.sumOf { it.amountCents }
                )
            }
            .filter { it.amountCents > 0L }
            .sortedByDescending { it.amountCents }
    }

    private fun rebuild() {
        val incomeCategories = categorySlices(TransactionType.INCOME)
        val expenseCategories = categorySlices(TransactionType.EXPENSE)

        incomeCents = incomeCategories.sumOf { it.amountCents }
        expenseCents = expenseCategories.sumOf { it.amountCents }
        totalLabel.text = formatMoney(incomeCents)

        slices = when (mode) {
            SummaryMode.INCOME -> incomeCategories
            SummaryMode.SPENDING -> {
                val availableCents = incomeCents - expenseCents
                val available = SummarySlice(
                    "Available",
                    rootView.context.getColor(R.color.light_green),
                    availableCents
                )
                // Available goes last; if overspent it is simply left out.
                if (availableCents > 0L) expenseCategories + available else expenseCategories
            }
        }

        pieTotalCents = when (mode) {
            SummaryMode.INCOME -> incomeCents
            SummaryMode.SPENDING -> maxOf(incomeCents, expenseCents)
        }

        if (slices.isEmpty()) {
            chart.centerText = ""
            chart.setNoDataText(
                if (mode == SummaryMode.INCOME) "No income in this period"
                else "No spending in this period"
            )
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = slices.map { slice ->
            PieEntry((slice.amountCents / 100.0).toFloat(), slice.name)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = slices.map { it.color }
            sliceSpace = 2f
            selectionShift = 8f

            setDrawValues(false)
            isHighlightEnabled = true
        }

        chart.data = PieData(dataSet)
        chart.notifyDataSetChanged()
        chart.highlightValues(null)
        showTotal()
        chart.invalidate()
    }

    fun showMessage(message: String) {
        transactions = emptyList()
        slices = emptyList()
        incomeCents = 0L
        expenseCents = 0L
        pieTotalCents = 0L
        totalLabel.text = "—"
        chart.centerText = ""
        chart.setNoDataText(message)
        chart.clear()
        chart.invalidate()
    }

    private fun showSlice(index: Int) {
        val slice = slices.getOrNull(index) ?: return
        if (pieTotalCents <= 0L) return

        val share = slice.amountCents.toDouble() / pieTotalCents

        chart.centerText =
            "${slice.name}\n${formatMoney(slice.amountCents)}\n${percentage.format(share)}"
        chart.invalidate()
    }

    private fun showTotal() {
        val overspent = expenseCents - incomeCents
        chart.centerText = when {
            mode == SummaryMode.SPENDING && overspent > 0L ->
                "Overspent by\n${formatMoney(overspent)}"
            mode == SummaryMode.SPENDING && incomeCents <= 0L ->
                "Total expenses\n${formatMoney(expenseCents)}"
            else ->
                "Total income\n${formatMoney(incomeCents)}"
        }
        chart.invalidate()
    }

    private fun formatMoney(cents: Long): String {
        return currency.format(BigDecimal.valueOf(cents, 2))
    }
}