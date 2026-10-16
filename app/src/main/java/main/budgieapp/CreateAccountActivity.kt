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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import main.budgieapp.data.BudgieDatabase
import main.budgieapp.data.PasswordHasher
import main.budgieapp.data.UserEntity
import java.util.Locale
import java.util.UUID

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

    private fun createAccount(email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful){
                    val user = auth.currentUser
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

    private fun createAccount() {
        if (isSaving) return

        val emailInput = findViewById<EditText>(R.id.etxtEmail)
        val passwordInput = findViewById<EditText>(R.id.etxtPassword)
        val createButton = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)

        emailInput.error = null
        passwordInput.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        }

        if (passwordInput.text.length < 6) {
            passwordInput.error = "Password requires at least 6 characters"
            passwordInput.requestFocus()
            return
        }

        val passwordText = passwordInput.text
        val password = CharArray(passwordText.length) { index -> passwordText[index] }

        isSaving = true
        createButton.isEnabled = false
        emailInput.isEnabled = false
        passwordInput.isEnabled = false

        lifecycleScope.launch {
            try {
                val credentials = withContext(Dispatchers.Default) {
                    try {
                        PasswordHasher.hash(password)
                    } finally {
                        password.fill('\u0000')
                    }
                }

                val user = UserEntity(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    passwordHash = credentials.hash,
                    passwordSalt = credentials.salt,
                    passwordIterations = credentials.iterations
                )

                val insertedRow = database.userDao().insert(user)

                if (insertedRow == -1L) {
                    emailInput.error = "An account with this email already exists"
                    return@launch
                }
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
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Toast.makeText(
                    this@CreateAccountActivity,
                    "Could not create account. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                password.fill('\u0000')
                isSaving = false
                createButton.isEnabled = true
                emailInput.isEnabled = true
                passwordInput.isEnabled = true
            }
        }
    }
}
