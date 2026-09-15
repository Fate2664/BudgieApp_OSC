package main.budgieapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.PasswordHasher
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private val database by lazy {
        BudgieDatabase.getInstance(applicationContext)
    }

    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            login()
        }
    }

    private fun login() {
        if (isLoggedIn) return

        val emailInput = findViewById<EditText>(R.id.etxtEmail)
        val passwordInput = findViewById<EditText>(R.id.etxtPassword)
        val loginButton = findViewById<MaterialButton>(R.id.btnLogin)
        val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)

        emailInput.error = null
        passwordInput.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        }

        if (passwordInput.text.isEmpty()) {
            passwordInput.error = "Enter your password"
            passwordInput.requestFocus()
            return
        }

        val passwordText = passwordInput.text
        val password = CharArray(passwordText.length) { index -> passwordText[index] }

        isLoggedIn = true
        loginButton.isEnabled = false
        emailInput.isEnabled = false
        passwordInput.isEnabled = false

        lifecycleScope.launch {
            try {
                val user = database.userDao().findByEmail(email)
                val passwordMatches = withContext(Dispatchers.Default) {
                    try {
                        if (user == null) {
                            PasswordHasher.hash(password)
                            false
                        } else {
                            PasswordHasher.verify(password, user)
                        }
                    } finally {
                        password.fill('\u0000')
                    }
                }

                if (user == null || !passwordMatches) {
                    passwordInput.error = "Incorrect email or password"
                    return@launch
                }
                passwordInput.text.clear()

                Toast.makeText(this@LoginActivity, "Logged in successfully", Toast.LENGTH_SHORT)
                    .show()
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Could not log in. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                password.fill('\u0000')
                isLoggedIn = false
                loginButton.isEnabled = true
                emailInput.isEnabled = true
                passwordInput.isEnabled = true
            }
        }
    }
}