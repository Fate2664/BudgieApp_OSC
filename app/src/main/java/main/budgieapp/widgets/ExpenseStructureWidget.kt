package main.budgieapp.widgets

import android.app.DatePickerDialog
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
import com.google.android.material.checkbox.MaterialCheckBox
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.data.TransactionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.collections.plus

private data class CategoryExpense(
    val name: String,
    val color: Int,
    val amountCents: Long
)

class ExpenseStructureWidget(private val rootView: View) {

    private val chart: PieChart = rootView.findViewById(R.id.expensePieChart)
    private val periodLabel: TextView = rootView.findViewById(R.id.txtTimePeriod)
    private val totalLabel: TextView = rootView.findViewById(R.id.txtTotalExpenses)
    private val incomeTotalLabel: TextView = rootView.findViewById(R.id.txtExpenseTotalIncome)
    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val percentage = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }
    private var categories: List<CategoryExpense> = emptyList()
    private var totalCents = 0L
    private val includeIncomeCheckbox: MaterialCheckBox =
        rootView.findViewById(R.id.chkIncludeIncome)
    private var transactions: List<TransactionEntity> = emptyList()
    private var incomeCents = 0L
    private var expenseCents = 0L
    private val customDatesRow: View = rootView.findViewById(R.id.expenseCustomDates)
    private val fromDateButton: TextView = rootView.findViewById(R.id.btnExpenseFromDate)
    private val toDateButton: TextView = rootView.findViewById(R.id.btnExpenseToDate)
    private var customFromDate = LocalDate.now().minusDays(29)
    private var customToDate = LocalDate.now()
    private var useCustomDates = false

    val customDateRange: Pair<LocalDate, LocalDate>?
        get() = if (useCustomDates) customFromDate to customToDate else null

    init {
        configureChart()
        includeIncomeCheckbox.setOnCheckedChangeListener { _, _ -> rebuild() }
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
                xEntrySpace = 8f
                yEntrySpace = 6f
                yOffset = 12f
                maxSizePercent = 0.95f
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
        var selectedRange = initialRange
        customFromDate = initialRange.startDate(LocalDate.now())
        val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun updateDateFields() {
            fromDateButton.text = "From: ${customFromDate.format(dateFormat)}"
            toDateButton.text = "To: ${customToDate.format(dateFormat)}"
        }

        fun pickDate(isFrom: Boolean) {
            val date = if (isFrom) customFromDate else customToDate
            val picker = DatePickerDialog(
                rootView.context,
                { _, year, month, day ->
                    val selected = LocalDate.of(year, month + 1, day)
                    if (isFrom) customFromDate = selected else customToDate = selected
                    updateDateFields()
                    onRangeSelected(selectedRange)
                },
                date.year, date.monthValue - 1, date.dayOfMonth
            )
            val zone = ZoneId.systemDefault()
            if (isFrom) {
                picker.datePicker.maxDate = customToDate.atStartOfDay(zone)
                    .toInstant().toEpochMilli()
            } else {
                picker.datePicker.minDate = customFromDate.atStartOfDay(zone)
                    .toInstant().toEpochMilli()
            }
            picker.show()
        }

        updateDateFields()
        fromDateButton.setOnClickListener { pickDate(isFrom = true) }
        toDateButton.setOnClickListener { pickDate(isFrom = false) }
        periodLabel.text = initialRange.title
        periodLabel.setOnClickListener {
            val options = CashFlowTimeRange.entries
            val popup = PopupMenu(rootView.context, periodLabel)

            options.forEachIndexed { index, range ->
                popup.menu.add(0, index, index, range.title)
            }
            popup.menu.add(0, options.size, options.size, "Custom")

            popup.setOnMenuItemClickListener { item ->
                useCustomDates = item.itemId == options.size
                customDatesRow.visibility = if (useCustomDates) View.VISIBLE else View.GONE
                if (!useCustomDates) selectedRange = options[item.itemId]
                periodLabel.text = if (useCustomDates) "Custom" else selectedRange.title
                onRangeSelected(selectedRange)
                true
            }

            popup.show()
        }
    }

    fun setTransactions(transactions: List<TransactionEntity>) {
        this.transactions = transactions
        includeIncomeCheckbox.isEnabled = true
        rebuild()
    }

    private fun rebuild() {
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

        expenseCents = categories.sumOf { it.amountCents }
        incomeCents = transactions.filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amountCents }

        if (includeIncomeCheckbox.isChecked && incomeCents > 0L) {
            categories = categories + CategoryExpense(
                name = "Income",
                color = rootView.context.getColor(R.color.light_green),
                amountCents = incomeCents
            )
        }

        totalCents = categories.sumOf { it.amountCents }
        totalLabel.text = "Expense: ${formatMoney(expenseCents)}"
        incomeTotalLabel.text = "Income: ${formatMoney(incomeCents)}"
        incomeTotalLabel.visibility = if (includeIncomeCheckbox.isChecked) View.VISIBLE else View.GONE

        if (categories.isEmpty()) {
            chart.centerText = ""
            chart.setNoDataText(
                if (includeIncomeCheckbox.isChecked) {
                    "Net total: ${formatMoney(incomeCents - expenseCents)}\n" +
                        "No income or expenses in this period"
                } else {
                    "Expense total: ${formatMoney(expenseCents)}\n" +
                        "No expenses in this period"
                }
            )
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

        chart.highlightValues(null)
        showTotal()
        chart.invalidate()
    }

    fun showMessage(message: String) {
        incomeTotalLabel.text = "Income: —"
        incomeTotalLabel.visibility = if (includeIncomeCheckbox.isChecked) View.VISIBLE else View.GONE
        transactions = emptyList()
        incomeCents = 0L
        expenseCents = 0L
        includeIncomeCheckbox.isEnabled = false
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
        chart.centerText = if (includeIncomeCheckbox.isChecked) {
            "Net total\n${formatMoney(incomeCents - expenseCents)}"
        } else {
            "Expense total\n${formatMoney(expenseCents)}"
        }

        chart.invalidate()
    }

    private fun formatMoney(cents: Long): String {
        return currency.format(BigDecimal.valueOf(cents, 2))
    }
}
