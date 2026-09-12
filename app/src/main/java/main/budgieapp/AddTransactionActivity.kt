package main.budgieapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class Category(
    val id: String,
    val name: String,
    val color: Int
)

class AddTransactionActivity : AppCompatActivity() {

    private val database by lazy {
        BudgieDatabase.getInstance(applicationContext)
    }

    private var isSaving = false
    private var transactionId = UUID.randomUUID().toString()

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }

    private var transactionType = TransactionType.EXPENSE

    private val selectedDateTime = Calendar.getInstance()
    private var attachmentUri: Uri? = null
    private val attachmentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val fileName = getAttachmentName(uri)
                    attachmentUri = uri

                    findViewById<MaterialButton>(R.id.btnAttachment).text = fileName
                } catch (exception: SecurityException) {
                    Toast.makeText(
                        this,
                        "Unable to access this attachment. Please choose another file",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val categoryColors = listOf(
        "Red" to Color.parseColor("#E57373"),
        "Blue" to Color.parseColor("#64B5F6"),
        "Purple" to Color.parseColor("#BA68C8"),
        "Orange" to Color.parseColor("#FFB74D"),
        "Green" to Color.parseColor("#81C784"),
        "Teal" to Color.parseColor("#4DB6AC"),
        "Pink" to Color.parseColor("#F06292"),
        "Yellow" to Color.parseColor("#FFD54F")
    )

    private val categories =
        mutableListOf(
            Category(
                "food", "Food",
                Color.parseColor("#E57373")
            ),
            Category(
                "transport", "Transport",
                Color.parseColor("#64B5F6")
            ),
            Category(
                "shopping", "Shopping",
                Color.parseColor("#BA68C8")
            ),
            Category(
                "bills", "Bills",
                Color.parseColor("#FFB74D")
            )
        )

    private var selectedCategory: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)

        val typeName = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)

        transactionType =
            TransactionType.entries.firstOrNull { it.name == typeName } ?: TransactionType.EXPENSE

        findViewById<TextView>(R.id.txtTransactionTitle).text =
            when (transactionType) {
                TransactionType.INCOME -> "New Income Record"
                TransactionType.EXPENSE -> "New Expense Record"
            }

        //region Saving
        transactionId = savedInstanceState?.getString("transaction_id") ?: transactionId

        savedInstanceState?.let { state ->
            selectedDateTime.timeInMillis =
                state.getLong("transaction_date_time", selectedDateTime.timeInMillis)

            attachmentUri = state.getString("transaction_attachment_uri")?.let {
                Uri.parse(it)
            }
        }

        findViewById<ImageButton>(R.id.btnConfirm).setOnClickListener {
            saveTransaction()
        }
        //endregion

        //region Category
        loadCategories()
        val selectedID = savedInstanceState?.getString("selected_category_id")
        selectedCategory = categories.firstOrNull { it.id == selectedID }
        val btnCategory = findViewById<MaterialButton>(R.id.btnChooseCategory)

        selectedCategory?.let { selectCategory(it) }

        btnCategory.setOnClickListener { showCategorySheet() }
        //endregion

        //region Date + Time
        val txtDate = findViewById<TextView>(R.id.txtDate)
        val txtTime = findViewById<TextView>(R.id.txtTime)
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormatter = DateFormat.getTimeFormat(this)

        //Initially show the current date and time
        txtDate.text = dateFormatter.format(selectedDateTime.time)
        txtTime.text = timeFormatter.format(selectedDateTime.time)

        txtDate.setOnClickListener {
            DatePickerDialog(
                this, { _, year, month, day ->
                    selectedDateTime.set(Calendar.YEAR, year)
                    selectedDateTime.set(Calendar.MONTH, month)
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day)

                    txtDate.text = dateFormatter.format(selectedDateTime.time)
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        txtTime.setOnClickListener {
            TimePickerDialog(
                this, { _, hour, minute ->
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedDateTime.set(Calendar.MINUTE, minute)
                    selectedDateTime.set(Calendar.SECOND, 0)
                    selectedDateTime.set(Calendar.MILLISECOND, 0)

                    txtTime.text = timeFormatter.format(selectedDateTime.time)
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this)
            ).show()
        }
        //endregion

        //region Payment Type
        val txtPaymentType = findViewById<TextView>(R.id.txtPaymentType)

        txtPaymentType.setOnClickListener {
            val popupMenu = PopupMenu(this, txtPaymentType)
            val paymentTypes = listOf("Cash", "Card", "Bank Transfer")

            paymentTypes.forEach { paymentType -> popupMenu.menu.add(paymentType) }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                txtPaymentType.text = menuItem.title
                true
            }

            popupMenu.show()
        }
        //endregion

        //region Status
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        txtStatus.setOnClickListener {
            val popupMenu = PopupMenu(this, txtStatus)
            val statusOptions = listOf("Cleared", "Pending", "Canceled")

            statusOptions.forEach { statusOption -> popupMenu.menu.add(statusOption) }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                txtStatus.text = menuItem.title
                true
            }

            popupMenu.show()
        }
        //endregion

        //region Attachment
        val btnAttachment = findViewById<MaterialButton>(R.id.btnAttachment)
        btnAttachment.setOnClickListener {
            attachmentPicker.launch(arrayOf("*/*"))
        }
        //endregion

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        savedInstanceState?.getString("transaction_payment_type")?.let {
            findViewById<TextView>(R.id.txtPaymentType).text = it
        }

        savedInstanceState?.getString("transaction_status")?.let {
            findViewById<TextView>(R.id.txtStatus).text = it
        }

        attachmentUri?.let {
            findViewById<MaterialButton>(R.id.btnAttachment).text = "Attachment selected"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("transaction_id", transactionId)
        outState.putString("selected_category_id", selectedCategory?.id)
        outState.putLong("transaction_date_time", selectedDateTime.timeInMillis)
        outState.putString("transaction_attachment_uri", attachmentUri?.toString())
        outState.putString(
            "transaction_payment_type",
            findViewById<TextView>(R.id.txtPaymentType).text.toString()
        )
        outState.putString(
            "transaction_status",
            findViewById<TextView>(R.id.txtStatus).text.toString()
        )
        super.onSaveInstanceState(outState)
    }

    private fun getAttachmentName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    return cursor.getString(nameIndex) ?: "Selected attachment"
                }

            }
        return "Selected attachment"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun categoryDot(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(1), Color.DKGRAY)
            setSize(dp(20), dp(20))
            setBounds(0, 0, dp(20), dp(20))
        }
    }

    private fun selectCategory(category: Category) {
        selectedCategory = category
        findViewById<MaterialButton>(R.id.btnChooseCategory).apply {
            text = category.name
            icon = categoryDot(category.color)
            iconTint = null
        }
    }

    private fun showCategorySheet() {
        val sheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_categories, null)

        sheet.setContentView(sheetView)
        sheet.setTitle("Select Category")

        val listCategories = sheetView.findViewById<ListView>(R.id.listCategories)
        val adapter = object : ArrayAdapter<Category>(
            this,
            android.R.layout.simple_list_item_single_choice,
            categories
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent) as TextView
                val category = categories[position]

                row.text = category.name
                row.setCompoundDrawablesRelative(categoryDot(category.color), null, null, null)
                row.compoundDrawablePadding = dp(12)

                return row
            }
        }

        listCategories.adapter = adapter

        val selectedIndex = categories.indexOfFirst { it.id == selectedCategory?.id }

        if (selectedIndex >= 0) {
            listCategories.setItemChecked(selectedIndex, true)
        }

        listCategories.setOnItemClickListener { _, _, position, _ ->
            selectCategory(categories[position])
            sheet.dismiss()
        }

        sheetView.findViewById<MaterialButton>(R.id.btnAddNewCategory)
            .setOnClickListener {
                showAddCategoryDialog {
                    sheet.dismiss()
                }
            }
        sheet.show()
    }

    private fun showAddCategoryDialog(onCategoryAdded: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.add_category, null)
        val input = dialogView.findViewById<EditText>(R.id.etxtCategoryName)
        val colorGroup = dialogView.findViewById<RadioGroup>(R.id.groupCategoryColors)
        var selectedColor = categoryColors.first().second
        val colorsByViewID = mutableMapOf<Int, Int>()

        categoryColors.forEachIndexed { index, (name, color) ->
            val option = RadioButton(this).apply {
                id = View.generateViewId()
                text = name
                minHeight = dp(48)
                compoundDrawablePadding = dp(12)
                setCompoundDrawablesRelative(null, null, categoryDot(color), null)
            }

            colorsByViewID[option.id] = color
            colorGroup.addView(
                option, RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            if (index == 0) {
                colorGroup.check(option.id)
            }
        }

        colorGroup.setOnCheckedChangeListener { _, checkedID ->
            colorsByViewID[checkedID]?.let {
                selectedColor = it
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                when {
                    name.isBlank() -> {
                        input.error = "Enter a category name"
                    }

                    categories.any {
                        it.name.equals(name, ignoreCase = true)
                    } -> {
                        input.error = "This category already exists"
                    }

                    else -> {
                        val category = Category(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            color = selectedColor
                        )

                        categories.add(category)
                        categories.sortBy { it.name.lowercase(Locale.ROOT) }
                        saveCategories()

                        selectCategory(category)
                        dialog.dismiss()
                        onCategoryAdded()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun saveCategories() {
        val array = JSONArray()

        categories.forEach { category ->
            array.put(JSONObject().apply {
                put("id", category.id)
                put("name", category.name)
                put("color", category.color)
            })
        }

        getSharedPreferences("categories", MODE_PRIVATE)
            .edit()
            .putString("categories_json", array.toString())
            .remove("names")
            .apply()
    }

    private fun loadCategories() {
        val preferences = getSharedPreferences("categories", MODE_PRIVATE)
        val savedJson = preferences.getString("categories_json", null)
            ?: preferences.getString("categories_json", null)

        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                val loadedCategories = mutableListOf<Category>()

                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    loadedCategories.add(
                        Category(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            color = item.getInt("color")
                        )
                    )
                }

                categories.clear()
                categories.addAll(loadedCategories)
                categories.sortBy { it.name.lowercase(Locale.ROOT) }
            } catch (exception: org.json.JSONException) {
                Toast.makeText(
                    this,
                    "Could not load saved categories",
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        preferences.getStringSet("names", null)?.let { oldNames ->
            val migrated = oldNames.sorted().mapIndexed { index, name ->
                val matchingDefault = categories.firstOrNull {
                    it.name.equals(name, ignoreCase = true)
                }

                matchingDefault ?: Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = categoryColors[index % categoryColors.size].second
                )
            }

            categories.clear()
            categories.addAll(migrated)
        }

        saveCategories()
    }

    private fun saveTransaction() {
        if (isSaving) return

        val amountInput = findViewById<EditText>(R.id.etxtAmount)
        val amountText = amountInput.text.toString().trim()

        //Accept digits and decimal point
        if (!amountText.matches(Regex("""\d+(\.\d{1,2})?"""))) {
            amountInput.error = "Enter an amount with at most 2 decimal places"
            amountInput.requestFocus()
            return
        }

        val amountCents = try {
            amountText.toBigDecimal()
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (exception: ArithmeticException) {
            amountInput.error = "This amount is too large"
            amountInput.requestFocus()
            return
        }

        if (amountCents <= 0) {
            amountInput.error = "Enter an amount greater than zero"
            amountInput.requestFocus()
            return
        }

        val category = selectedCategory
        if (category == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = TransactionEntity(
            id = transactionId,
            type = transactionType.name,
            amountCents = amountCents,
            description = findViewById<EditText>(R.id.etxtDescription).text.toString().trim(),
            payee = findViewById<EditText>(R.id.etxtPayee).text.toString().trim(),
            categoryId = category.id,
            categoryName = category.name,
            categoryColor = category.color,
            dateTimeMillis = selectedDateTime.timeInMillis,
            paymentType = findViewById<TextView>(R.id.txtPaymentType).text.toString(),
            status = findViewById<TextView>(R.id.txtStatus).text.toString(),
            attachmentUri = attachmentUri?.toString()
        )

        val confirmButton = findViewById<ImageButton>(R.id.btnConfirm)
        isSaving = true
        confirmButton.isEnabled = false

        lifecycleScope.launch {
            try {
                database.transactionDao().insert(transaction)
                Toast.makeText(this@AddTransactionActivity, "Transaction saved", Toast.LENGTH_SHORT)
                    .show()
                finish()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Toast.makeText(
                    this@AddTransactionActivity,
                    "Could not save transaction. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSaving = false
                confirmButton.isEnabled = true
            }
        }
    }
}