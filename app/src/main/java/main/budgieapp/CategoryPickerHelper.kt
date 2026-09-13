package main.budgieapp

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import androidx.core.graphics.toColorInt


class CategoryPickerHelper(
    private val activity: AppCompatActivity,
    private val button: MaterialButton,
    private val onSelected: (Category?) -> Unit
) {
    private val categoryColors = listOf(
        "Red" to activity.getColor(R.color.category_red),
        "Blue" to activity.getColor(R.color.category_blue),
        "Purple" to activity.getColor(R.color.category_purple),
        "Orange" to activity.getColor(R.color.category_orange),
        "Green" to activity.getColor(R.color.category_green),
        "Teal" to activity.getColor(R.color.category_teal),
        "Pink" to activity.getColor(R.color.category_pink),
        "Yellow" to activity.getColor(R.color.category_yellow)
    )

    private val categories =
        mutableListOf(
            Category(
                "food",
                "Food",
                activity.getColor(R.color.category_red)
            ),
            Category(
                "transport",
                "Transport",
                activity.getColor(R.color.category_blue)
            ),
            Category(
                "shopping",
                "Shopping",
                activity.getColor(R.color.category_purple)
            ),
            Category(
                "bills",
                "Bills",
                activity.getColor(R.color.category_orange)
            )
        )

    private var selectedCategory: Category? = null

    fun bind(selectedCategoryId: String? = null) {
        loadCategories()
        selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
        val restored = selectedCategory
        if (restored != null) {
            selectCategory(restored)
        } else {
            button.text = "Select Category"
            button.icon = null
            onSelected(null)
        }
        button.setOnClickListener { showCategorySheet() }
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()

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
        onSelected(category)
        button.apply {
            text = category.name
            icon = categoryDot(category.color)
            iconTint = null
        }
    }

    private fun showCategorySheet() {
        val sheet = BottomSheetDialog(activity)
        val sheetView = activity.layoutInflater.inflate(R.layout.bottom_sheet_categories, null)

        sheet.setContentView(sheetView)
        sheet.setTitle("Select Category")

        val listCategories = sheetView.findViewById<ListView>(R.id.listCategories)
        val adapter = object : ArrayAdapter<Category>(
            activity,
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
        val dialogView = activity.layoutInflater.inflate(R.layout.add_category, null)
        val input = dialogView.findViewById<EditText>(R.id.etxtCategoryName)
        val colorGroup = dialogView.findViewById<RadioGroup>(R.id.groupCategoryColors)
        var selectedColor = categoryColors.first().second
        val colorsByViewID = mutableMapOf<Int, Int>()

        categoryColors.forEachIndexed { index, (name, color) ->
            val option = RadioButton(activity).apply {
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

        val dialog = MaterialAlertDialogBuilder(activity)
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

        activity.getSharedPreferences("categories", Context.MODE_PRIVATE)
            .edit()
            .putString("categories_json", array.toString())
            .remove("names")
            .apply()
    }

    private fun loadCategories() {
        val preferences = activity.getSharedPreferences("categories", Context.MODE_PRIVATE)
        val savedJson = preferences.getString("categories_json", null)

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
                    activity,
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

}

