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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private var isLoggingIn = false

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null)
            openDashboard()

    }

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
        if (isLoggingIn) return

        val emailInput = findViewById<EditText>(R.id.etxtEmail)
        val passwordInput = findViewById<EditText>(R.id.etxtPassword)
        val loginButton = findViewById<MaterialButton>(R.id.btnLogin)

        val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)
        val password = passwordInput.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Enter your password"
            passwordInput.requestFocus()
            return
        }

        isLoggingIn = true
        loginButton.isEnabled = false
        emailInput.isEnabled = false
        passwordInput.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (isFinishing || isDestroyed)
                    return@addOnCompleteListener
                isLoggingIn = false
                loginButton.isEnabled = true
                emailInput.isEnabled = true
                passwordInput.isEnabled = true

                if (task.isSuccessful) {
                    passwordInput.text.clear()
                    Toast.makeText(this@LoginActivity, "Logged in successfully", Toast.LENGTH_SHORT)
                        .show()
                    openDashboard()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Could not log in. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun openDashboard(){
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

}