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


class CreateAccountActivity : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            createAccount()
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun createAccount(){
        if (isSaving) return

        val emailInput = findViewById<EditText>(R.id.etxtEmail)
        val passwordInput = findViewById<EditText>(R.id.etxtPassword)
        val createButton = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val loginButton = findViewById<MaterialButton>(R.id.btnLogin)

        val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)
        val password = passwordInput.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        }

        if (password.length < 6){
            passwordInput.error = "Password must be at least 6 characters long"
            passwordInput.requestFocus()
            return
        }

        isSaving = true
        createButton.isEnabled = false
        loginButton.isEnabled = false
        emailInput.isEnabled = false
        passwordInput.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (isFinishing || isDestroyed)
                    return@addOnCompleteListener

                isSaving = false
                createButton.isEnabled = true
                loginButton.isEnabled = true
                emailInput.isEnabled = true
                passwordInput.isEnabled = true

                if (task.isSuccessful){
                    passwordInput.text.clear()
                    Toast.makeText(this@CreateAccountActivity, "Account created", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(
                        Intent(
                            this@CreateAccountActivity,
                            DashboardActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    finish()
                } else {
                    Toast.makeText(
                        this@CreateAccountActivity,
                        "Could not create account. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


}
