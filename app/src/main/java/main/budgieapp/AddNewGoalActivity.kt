package main.budgieapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddNewGoalActivity : AppCompatActivity() {

    private val goalFormLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_new_goal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnCreateGoal).setOnClickListener {
            val name = findViewById<EditText>(R.id.etxtGoalName).text.toString().trim()
            openGoal(name, "savings", R.color.light_blue)
        }

        bindPreset(R.id.houseGoal, "New House", "house", R.color.light_blue)
        bindPreset(R.id.vehicleGoal, "New Vehicle", "vehicle", R.color.orange)
        bindPreset(R.id.holidayGoal, "Holiday Trip", "holiday", R.color.light_green)
        bindPreset(R.id.educationGoal, "Education", "education", R.color.normal_blue)
        bindPreset(R.id.emergencyGoal, "Emergency Fund", "savings", R.color.purple)
        bindPreset(R.id.healthGoal, "Healthcare", "health", R.color.light_red)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun bindPreset(viewId: Int, name: String, iconKey: String, colorResource: Int){
        findViewById<View>(viewId).setOnClickListener {
            openGoal(name, iconKey, colorResource)
        }
    }

    private fun openGoal(name: String, iconKey: String, colorResource: Int){
        val intent = Intent(this, ImplementNewGoalActivity::class.java).apply{
            putExtra(ImplementNewGoalActivity.EXTRA_GOAL_NAME, name)
            putExtra(ImplementNewGoalActivity.EXTRA_GOAL_ICON, iconKey)
            putExtra(ImplementNewGoalActivity.EXTRA_GOAL_COLOR, getColor(colorResource))
        }

        goalFormLauncher.launch(intent)
    }
}