package main.budgieapp

import android.media.Image
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgetEntity
import main.budgieapp.data.BudgieDatabase
import java.util.UUID


class AddBudgetActivity : AppCompatActivity() {
    private lateinit var categoryPicker: CategoryPickerHelper
    private var selectedCategory: Category? = null

    private val database by lazy {
        BudgieDatabase.getInstance(applicationContext)
    }

    private var isSaving = false
    private var budgetId = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_budget)

        //region Category
        categoryPicker = CategoryPickerHelper(
            activity = this,
            button = findViewById<MaterialButton>(R.id.btnChooseCategory),
            onSelected = { category -> selectedCategory = category }
        )

        categoryPicker.bind(savedInstanceState?.getString("selected_category_id"))
        //endregion

        //region Period
        val txtPeriod = findViewById<TextView>(R.id.txtPeriod)

        txtPeriod.setOnClickListener {
            val popupMenu = PopupMenu(this, txtPeriod)
            val periodOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")

            periodOptions.forEach { periodOption -> popupMenu.menu.add(periodOption) }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                txtPeriod.text = menuItem.title
                true
            }

            popupMenu.show()
        }
        //endregion

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        budgetId = savedInstanceState?.getString("budget_id") ?: budgetId

        savedInstanceState?.getString("budget_period")?.let {
            findViewById<TextView>(R.id.txtPeriod).text = it
        }

        findViewById<ImageButton>(R.id.btnConfirm).setOnClickListener {
            saveBudget()
        }
    }

    private fun saveBudget() {
        if (isSaving) return

        val nameInput = findViewById<EditText>(R.id.etxtName)
        val amountInput = findViewById<EditText>(R.id.etxtAmount)
        val name = nameInput.text.toString().trim()
        val amountText = amountInput.text.toString().trim()

        if (name.isBlank()) {
            nameInput.error = "Enter a budget name"
            nameInput.requestFocus()
            return
        }

        if (!amountText.matches(Regex("""\d+(\.\d{1,2})?"""))) {
            amountInput.error = "Enter an amount with at most 2 decimal places"
            amountInput.requestFocus()
            return
        }

        val amountCents = try {
            amountText.toBigDecimal()
                .movePointRight(2)
                .longValueExact()
        } catch (error: ArithmeticException) {
            amountInput.error = "This amount is too large"
            return
        }

        if (amountCents <= 0) {
            amountInput.error = "Enter an amount greater than zero"
            return
        }

        val category = selectedCategory
        if (category == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val period = findViewById<TextView>(R.id.txtPeriod).text.toString()
        if (period !in listOf("Daily", "Weekly", "Monthly", "Yearly")) {
            Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show()
            return
        }

        val budget = BudgetEntity(
            id = budgetId,
            name = name,
            amountCents = amountCents,
            categoryId = category.id,
            period = period
        )

        val confirmButton = findViewById<ImageButton>(R.id.btnConfirm)
        isSaving = true
        confirmButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val existing = database.budgetDao()
                    .findForCategory(category.id, period)

                if (existing != null && existing.id != budgetId) {
                    Toast.makeText(
                        this@AddBudgetActivity,
                        "This category already has a $period budget",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                if (existing == null) {
                    database.budgetDao().insert(budget)
                }

                Toast.makeText(this@AddBudgetActivity, "Budget saved", Toast.LENGTH_SHORT).show()
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("AddBudgetActivity", "Could not save budget", error)
                Toast.makeText(
                    this@AddBudgetActivity,
                    "Could not save budget. Please try again",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSaving = false
                confirmButton.isEnabled = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("budget_id", budgetId)
        outState.putString("selected_category_id", selectedCategory?.id)
        outState.putString("budget_period", findViewById<TextView>(R.id.txtPeriod).text.toString())

        super.onSaveInstanceState(outState)
    }

}