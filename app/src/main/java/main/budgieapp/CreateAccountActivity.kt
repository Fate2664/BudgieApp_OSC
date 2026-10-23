package main.budgieapp

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale


class CreateAccountActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var isSaving = false
    private var pendingUser: FirebaseUser? = null
    private var pendingUsername: String? = null

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

        val loginLink = findViewById<TextView>(R.id.txtAlreadyHaveAccount)
        val originalFlags = loginLink.paintFlags

        loginLink.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    loginLink.paintFlags = originalFlags or
                            Paint.UNDERLINE_TEXT_FLAG

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    loginLink.paintFlags = originalFlags
            }
            view.invalidate()
            false
        }

        findViewById<TextView>(R.id.txtAlreadyHaveAccount).setOnClickListener {
            if (!isSaving) {
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }

    }

    private fun createAccount() {
        if (isSaving) return

        pendingUser?.let { user ->
            saveUsername(user, requireNotNull(pendingUsername))
            return
        }

        val usernameInput = findViewById<EditText>(R.id.etxtUsername)
        val emailInput = findViewById<EditText>(R.id.etxtEmail)
        val passwordInput = findViewById<EditText>(R.id.etxtPassword)
        val createButton = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val loginLink = findViewById<TextView>(R.id.txtAlreadyHaveAccount)

        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)
        val password = passwordInput.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        }

        if (!username.matches(Regex("[A-Za-z0-9_]{3,30}"))) {
            usernameInput.error = "Use 3–30 letters, numbers, or underscores"
            usernameInput.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters long"
            passwordInput.requestFocus()
            return
        }

        setSignupSaving(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (isFinishing || isDestroyed)
                    return@addOnCompleteListener

                val user = if (task.isSuccessful) task.result?.user else null

                if (user == null) {
                    setSignupSaving(false)
                    Toast.makeText(
                        this,
                        "Could not create account. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                pendingUser = user
                pendingUsername = username
                passwordInput.text.clear()

                saveUsername(user, username)
            }

    }

    private fun setSignupSaving(saving: Boolean) {
        isSaving = saving

        findViewById<MaterialButton>(R.id.btnCreateAccount).isEnabled = !saving
        val canEdit = !saving && pendingUser == null

        findViewById<EditText>(R.id.etxtUsername).isEnabled = canEdit
        findViewById<EditText>(R.id.etxtEmail).isEnabled = canEdit
        findViewById<EditText>(R.id.etxtPassword).isEnabled = canEdit
        findViewById<TextView>(R.id.txtAlreadyHaveAccount).isEnabled = canEdit
    }

    private fun saveUsername(user: FirebaseUser, username: String) {
        setSignupSaving(true)

        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf("username" to username),
                SetOptions.merge()
            )
            .addOnCompleteListener(this) { task ->
                if (isFinishing || isDestroyed) {
                    return@addOnCompleteListener
                }

                if (task.isSuccessful) {
                    pendingUser = null
                    pendingUsername = null

                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(
                            this,
                            DashboardActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    finish()
                } else {
                    setSignupSaving(false)

                    findViewById<MaterialButton>(R.id.btnCreateAccount).text =
                        "Retry saving username"

                    Toast.makeText(
                        this,
                        "Account created, but the username could not be saved. Tap retry.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

}
