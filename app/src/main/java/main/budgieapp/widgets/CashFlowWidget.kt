package main.budgieapp.widgets

import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButtonToggleGroup
import main.budgieapp.R
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

//Data class for the Cash Flow Periods
data class CashFlowPeriod(
    val label: String,  //Month
    val income: Float,
    val expense: Float
)

class CashFlowWidget(private val rootView: View) {
    //Find the combined chart component - combined charts can display bars and lines together
    private val chart: CombinedChart = rootView.findViewById(R.id.cashFlowChart)
    //Find the text view that displays the date range
    private var periodsLabel: TextView = rootView.findViewById(R.id.txtTimePeriod)

    //Declare colors that will be used
    private val incomeColor = Color.rgb(31, 195, 137)
    private val expenseColor = Color.rgb(244, 67, 54)
    private val cashFlowColor = Color.rgb(33, 33, 33)
    private val greyColor = Color.rgb(130, 130, 130)

    //Store the data in a list
    private var periods: List<CashFlowPeriod> = emptyList()
    //Cumulative boolean check
    private var cumulative = false

    //Init runs automatically when a CashFlowWidget is created
    init {
        ConfigureChart()
    }

    //Method to set up appearance that does not depend on financial data
    private fun ConfigureChart() {
        //apply lets us access the chart's properties
        chart.apply {
            description.isEnabled = false
            setNoDataText("No cash flow data available")
            setDrawGridBackground(false)    //Disable grid background
            setDrawBarShadow(true)
            setTouchEnabled(false)          //Disable chart gestures

            //Draw the bars first then the line on top
            drawOrder = arrayOf(
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
            )

            axisRight.isEnabled = false //Use only the left axis

            //Format the Y axis
            axisLeft.apply {
                setDrawGridLines(false)     //Disable grid lines
                setDrawAxisLine(false)      //Disable vertical axis border
                setDrawZeroLine(true)       //Keep a horizontal line at zero
                zeroLineColor = Color.LTGRAY
                zeroLineWidth = 0.7f
                textColor = greyColor
                textSize = 11f
                //Set approximately 5 labels and false lets the library choose sensible intervals
                setLabelCount(5, false)

                //Customize how numbers appear on the axis
                //"object" allows us to create anonymous subclass with our own formatting function
                valueFormatter = object : ValueFormatter() {
                    private val format = DecimalFormat("0.#")   //Only show 1 decimal place

                    override fun getFormattedValue(value: Float): String? {
                        //Ignore the sign and use "k" format. E.g. 1500 -> 1.5k
                        return if (abs(value) >= 1000f){
                            "${format.format(value / 1000f)}k"
                        }else {
                            format.format(value)
                        }
                    }
                }
            }

            //Format the X axis
            xAxis.apply {
                //Place the period labels at the bottom of the chart
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = greyColor
                textSize = 11f
                yOffset = 8f    //Add space between the chart and its bottom labels
            }

            //The legend explains what each color represents (The blocks underneath the chart)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                textColor = cashFlowColor
                textSize = 11f
                formSize = 10f
                xEntrySpace = 10f
                yOffset = 12f

                //Create the labels and ordering. We use the helper function that we made
                setCustom(listOf(LegendEntry("Cash flow", cashFlowColor),
                    LegendEntry("Incomes", incomeColor),
                    LegendEntry("Expenses", expenseColor)
                )
                )
            }

            //Add some extra spacing
            setExtraOffsets(4f, 12f, 4f, 8f)
        }
    }

    //This method creates a legend item which is just a label + a square
    private fun LegendEntry(label: String, color: Int): LegendEntry {
        return LegendEntry().apply {
            this.label = label
            form = Legend.LegendForm.SQUARE
            formColor = color
        }
    }

    //Public function called by the Dashboard Activity to supply data
    fun SetData(data: List<CashFlowPeriod>) {
        periods = data.toList()

        periodsLabel.text = when {
            periods.isEmpty() -> "No Data"
            periods.size == 1 -> periods.first().label
            else -> "${periods.first().label} - ${periods.last().label}"
        }
        render()
    }

    //Convert the financial data into chart entries and display them.
    private fun render() {
        if (periods.isEmpty()) {
            chart.clear()
            return
        }

        var incomeTotal = 0f
        var expenseTotal = 0f
        var largestIncome = 0f
        var largestExpense = 0f

        //BarEntry describes a bar at an X position
        var barEntries = mutableListOf<BarEntry>()
        //Entry describes an (X, Y) point for the cash flow line
        var cashFlowEntries = mutableListOf<Entry>()

        periods.forEachIndexed { index, period ->
            incomeTotal += period.income
            expenseTotal += period.expense

            val income = if(cumulative) incomeTotal else period.income
            val expense = if(cumulative) expenseTotal else period.expense
            val x = index.toFloat()

            //Each bar contains two values: income is positive and expense is negative
            barEntries.add(BarEntry(x, floatArrayOf(income, -expense)))
            //Net cash flow is income minus expense
            cashFlowEntries.add(Entry(x, income - expense))

            largestIncome = max(largestIncome, income)
            largestExpense = max(largestExpense, expense)
        }

        //The dataset combines entries with their visual styling
        val bars = BarDataSet(barEntries, "").apply {
            //set colors in the order of the floarArrayOf
            setColors(incomeColor, expenseColor)
            stackLabels = arrayOf("Incomes", "Expenses")
            barShadowColor = Color.rgb(233, 234, 237)
            setDrawValues(false)
            isHighlightEnabled = false
        }

        val cashFlow = LineDataSet(cashFlowEntries, "Cash flow").apply {
            color = cashFlowColor
            lineWidth = 2f
            //Only draw the line
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(false)
            //Connect the data points with a smooth curve
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            isHighlightEnabled = false
        }

        chart.xAxis.apply {
            //map the months to the corresponding index
            valueFormatter = IndexAxisValueFormatter(periods.map {it.label})
            axisMinimum = -0.5f
            axisMaximum = periods.size - 0.5f
            setLabelCount(periods.size, false)
        }

        //Adds more above the largest income and lower than minimum
        chart.axisLeft.apply {
            axisMaximum= max(largestIncome * 1.5f, 1f)
            axisMinimum= -max(largestExpense * 1.5f, 1f)
        }

        //Combined data holds both kinds of chart data
        chart.data = CombinedData().apply {
            setData(BarData(bars).apply {
                barWidth = 0.8f
            })
            setData((LineData(cashFlow)))
        }

        //Recalculate the chart after adding its data
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

}