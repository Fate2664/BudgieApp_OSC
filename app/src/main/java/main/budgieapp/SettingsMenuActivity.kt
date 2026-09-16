package main.budgieapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import main.budgieapp.data.BudgieDatabase

class SettingsMenuActivity : AppCompatActivity() {
    private val database by lazy {
        BudgieDatabase.getInstance(applicationContext)
    }

    private lateinit var clearButton: View
    private lateinit var sampleButton: View
    private var isBusy = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        clearButton = findViewById(R.id.btnClearTransactions)
        clearButton.setOnClickListener {
            clearTransactions()
        }

        sampleButton = findViewById(R.id.btnLoadTransactions)
        sampleButton.setOnClickListener {
            loadSampleTransactions()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    fun loadSampleTransactions() {
        if (isBusy) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Load sample transactions?")
            .setMessage(
                "Adds demo income and expenses alongside your existing " +
                        "transactions. Samples already loaded will be skipped."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Load") { _, _ ->
                runDataAction {
                    database.transactionDao().insertSample(
                        SampleTransactions.create(applicationContext)
                    )
                    "Sample transactions are available"
                }
            }.show()
    }

    private fun clearTransactions() {
        if (isBusy) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Clear all transactions?")
            .setMessage(
                "Permanently deletes all transactions stored in this app, " +
                        "including samples and goal contributions. " +
                        "Budgets, goals and accounts remain. This cannot be undone."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear all") { _, _ ->
                runDataAction {
                    val deleted = database.transactionDao().deleteAll()
                    if (deleted == 0) {
                        "No transaction to clear"
                    } else {
                        "Clear $deleted transaction"
                    }
                }
            }.show()
    }

    private fun runDataAction(action: suspend () -> String) {
        if (isBusy) return

        isBusy = true
        clearButton.isEnabled = false
        sampleButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val message = action()
                Toast.makeText(this@SettingsMenuActivity, message, Toast.LENGTH_SHORT).show()
            } catch (cancelled: Exception) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("SettingsMenuActivity", "Data action failed", error)
                Toast.makeText(
                    this@SettingsMenuActivity,
                    "Could not complete the action. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isBusy = false
                clearButton.isEnabled = true
                sampleButton.isEnabled = true
            }
        }
    }
}