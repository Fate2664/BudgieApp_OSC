package main.budgieapp

import android.R.attr.horizontalSpacing
import android.R.attr.numColumns
import android.R.attr.shape
import android.R.attr.stretchMode
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.GoalEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class ImplementNewGoalActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GOAL_NAME = "extra_goal_name"
        const val EXTRA_GOAL_ICON = "extra_goal_icon"
        const val EXTRA_GOAL_COLOR = "extra_goal_color"
    }

    private val database by lazy {
        BudgieDatabase.getInstance(applicationContext)
    }

    private var goalId = UUID.randomUUID().toString()
    private var isSaving = false
    private val targetDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var selectedColor = 0
    private var selectedIconKey = "savings"
    private val iconOptions = GoalIcons.options

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_implement_new_goal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        goalId = savedInstanceState?.getString("goal_id") ?: goalId
        targetDate.timeInMillis = savedInstanceState?.getLong(
            "goal_date",
            targetDate.timeInMillis
        ) ?: targetDate.timeInMillis

        val initialColor = intent.getIntExtra(
            EXTRA_GOAL_COLOR, getColor(R.color.light_blue)
        )

        selectedColor = savedInstanceState?.getInt(
            "goal_color",
            initialColor
        ) ?: initialColor


        selectedIconKey =
            savedInstanceState?.getString("goal_icon")
                ?: intent.getStringExtra(EXTRA_GOAL_ICON)
                        ?: "savings"

        if (savedInstanceState == null) {
            findViewById<EditText>(R.id.etxtName).setText(
                intent.getStringExtra(EXTRA_GOAL_NAME).orEmpty()
            )
        }

        setupDatePicker()
        setupColorPicker()
        setupIconPicker()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnConfirm).setOnClickListener {
            saveGoal()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun setupDatePicker() {
        val dateView = findViewById<TextView>(R.id.txtDate)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateView.text = formatter.format(targetDate.time)
        dateView.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    targetDate.clear()
                    targetDate.set(year, month, day)
                    dateView.text = formatter.format(targetDate.time)
                },
                targetDate.get(Calendar.YEAR),
                targetDate.get(Calendar.MONTH),
                targetDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupColorPicker() {
        val button = findViewById<MaterialButton>(R.id.btnGoalColor)
        val colors = listOf(
            "Light blue" to getColor(R.color.light_blue),
            "Orange" to getColor(R.color.orange),
            "Green" to getColor(R.color.light_green),
            "Blue" to getColor(R.color.normal_blue),
            "Purple" to getColor(R.color.purple),
            "Red" to getColor(R.color.light_red)
        )

        fun updateButton() {
            button.backgroundTintList = ColorStateList.valueOf(selectedColor)
            val colorName = colors.firstOrNull() {
                it.second == selectedColor
            }?.first ?: "Custom"
            button.contentDescription = "Goal color: $colorName"
        }
        updateButton()

        button.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
                .setTitle("Choose a goal color")
            val adapter = object : ArrayAdapter<String>(
                builder.context,
                android.R.layout.simple_list_item_single_choice,
                colors.map { it.first }
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val row = super.getView(position, convertView, parent) as CheckedTextView
                    val swatch = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colors[position].second)
                        setStroke(dp(1), getColor(R.color.soft_black))
                        setBounds(0, 0, dp(28), dp(28))
                    }

                    row.setCompoundDrawablesRelative(swatch, null, null, null)
                    row.compoundDrawablePadding = dp(16)

                    return row
                }
            }

            builder
                .setSingleChoiceItems(
                    adapter,
                    colors.indexOfFirst { it.second == selectedColor }
                ) { dialog, which ->
                    selectedColor = colors[which].second
                    updateButton()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupIconPicker() {
        val button = findViewById<ImageView>(R.id.btnIcon)

        fun updateButton() {
            val option = iconOptions.firstOrNull() {
                it.first == selectedIconKey
            } ?: iconOptions.first()

            selectedIconKey = option.first
            button.setImageResource(option.third)
            button.contentDescription = "Goal icon: ${option.second}"
        }

        updateButton()

        button.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
                .setTitle("Choose a goal icon")

            val grid = GridView(builder.context).apply {
                numColumns = 3
                stretchMode = GridView.STRETCH_COLUMN_WIDTH
                horizontalSpacing = dp(8)
                verticalSpacing = dp(8)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                clipToPadding = false
            }

            grid.adapter = object : BaseAdapter() {
                override fun getCount(): Int = iconOptions.size
                override fun getItem(position: Int): Triple<String, String, Int> =
                    iconOptions[position]

                override fun getItemId(position: Int): Long = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val image = convertView as? ImageView
                        ?: ImageView(builder.context)

                    val option = getItem(position)
                    val selected = option.first == selectedIconKey

                    image.apply {
                        layoutParams = android.widget.AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(72)
                        )

                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setPadding(dp(18), dp(18), dp(18), dp(18))
                        setImageResource(option.third)

                        imageTintList = ColorStateList.valueOf(
                            getColor(R.color.soft_black)
                        )

                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(12).toFloat()
                            setColor(Color.WHITE)
                            setStroke(
                                dp(if (selected) 3 else 1),
                                if (selected) {
                                    getColor(R.color.background_header)
                                } else {
                                    Color.LTGRAY
                                }
                            )
                        }

                        isSelected = selected
                        contentDescription = if (selected) {
                            "${option.second}, selected"
                        } else {
                            option.second
                        }
                    }

                    return image
                }
            }

            val dialog = builder
                .setView(grid)
                .setNegativeButton("Cancel", null)
                .create()

            grid.setOnItemClickListener { _, _, position, _ ->
                selectedIconKey = iconOptions[position].first
                updateButton()
                dialog.dismiss()
            }

            dialog.show()

            grid.layoutParams = grid.layoutParams.apply {
                height = dp(184)
            }
        }
    }

    private fun readAmountCents(
        input: EditText,
        allowZero: Boolean,
        blankAsZero: Boolean = false
    ): Long? {
        val text = input.text.toString().trim()
        input.error = null

        if (blankAsZero && text.isBlank()) return 0L
        if (!text.matches(Regex("""\d+(\.\d{1,2})?"""))) {
            input.error = "Enter an amount with at most 2 decimal places"
            input.requestFocus()
            return null
        }

        val cents = try {
            text.toBigDecimal().movePointRight(2).longValueExact()
        } catch (error: ArithmeticException) {
            input.error = "This amount is too large"
            input.requestFocus()
            return null
        }

        if (!allowZero && cents == 0L) {
            input.error = "Enter an amount greater than zero"
            input.requestFocus()
            return null
        }

        return cents
    }

    private fun saveGoal() {
        if (isSaving) return

        val nameInput = findViewById<EditText>(R.id.etxtName)
        val name = nameInput.text.toString().trim()

        if (name.isBlank()) {
            nameInput.error = "Enter a goal name"
            nameInput.requestFocus()
            return
        }

        val targetAmountCents = readAmountCents(
            input = findViewById<EditText>(R.id.etxtTargetAmount),
            allowZero = false, blankAsZero = false
        ) ?: return

        val savedAmountCents = readAmountCents(
            input = findViewById<EditText>(R.id.etxtSavedAlready),
            allowZero = true, blankAsZero = true
        ) ?: return

        val goal = GoalEntity(
            id = goalId,
            name = name,
            targetAmountCents = targetAmountCents,
            savedAmountCents = savedAmountCents,
            targetDateMillis = targetDate.timeInMillis,
            color = selectedColor,
            iconKey = selectedIconKey
        )

        val confirmButton = findViewById<ImageButton>(R.id.btnConfirm)
        isSaving = true
        confirmButton.isEnabled = false


        lifecycleScope.launch {
            try {
                if (database.goalDao().findById(goalId) == null) {
                    database.goalDao().insert(goal)
                }

                Toast.makeText(
                    this@ImplementNewGoalActivity,
                    "Goal saved",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(android.app.Activity.RESULT_OK)
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e(
                    "ImplementNewGoalActivity",
                    "Could not save goal",
                    error
                )

                Toast.makeText(
                    this@ImplementNewGoalActivity,
                    "Could not save goal. Please try again",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSaving = false
                confirmButton.isEnabled = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("goal_id", goalId)
        outState.putLong("goal_date", targetDate.timeInMillis)
        outState.putInt("goal_color", selectedColor)
        outState.putString("goal_icon", selectedIconKey)

        super.onSaveInstanceState(outState)
    }
}