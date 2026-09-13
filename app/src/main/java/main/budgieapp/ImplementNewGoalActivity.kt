package main.budgieapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import main.budgieapp.data.BudgieDatabase
import java.util.Calendar
import java.util.UUID

class ImplementNewGoalActivity : AppCompatActivity() {

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
    private val iconOptions = listOf(
        Triple(
            "vehicle", "Vehicle", R.drawable.baseline_directions_car_24
        )
    ) // Add more icons

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

        selectedColor = savedInstanceState?.getInt(
            "goal_color",
            getColor(R.color.light_blue)
        ) ?: getColor(R.color.light_blue)

        selectedIconKey = savedInstanceState?.getString("goal_icon") ?: "savings"

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

    private fun setupDatePicker(){

    }

    private fun setupColorPicker(){

    }

    private fun setupIconPicker(){

    }

    private fun saveGoal(){

    }
}