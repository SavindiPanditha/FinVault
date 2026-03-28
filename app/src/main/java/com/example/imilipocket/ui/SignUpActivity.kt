package com.example.imilipocket.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.imilipocket.R
import com.example.imilipocket.auth.SupabaseAuthService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var authService: SupabaseAuthService
    private var signUpCooldownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        authService = SupabaseAuthService(this)

        if (authService.isSignedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val nameEditText = findViewById<EditText>(R.id.etName)
        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvGoToSignIn = findViewById<Button>(R.id.tvGoToSignIn)

        btnSignUp.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            run {
                btnSignUp.isEnabled = false
                lifecycleScope.launch {
                    val result = authService.signUp(email, password)
                    btnSignUp.isEnabled = true

                    if (result.success && result.accessToken != null) {
                        Toast.makeText(this@SignUpActivity, "Account created", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                        finish()
                    } else if (result.success) {
                        Toast.makeText(this@SignUpActivity, result.message, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SignUpActivity, SignInActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@SignUpActivity, result.message, Toast.LENGTH_LONG).show()
                        if (result.message.contains("Too many signup attempts", ignoreCase = true)) {
                            startSignUpCooldown(btnSignUp)
                        }
                    }
                }
            }
        }

        tvGoToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }

    private fun startSignUpCooldown(signUpButton: Button) {
        signUpCooldownJob?.cancel()
        signUpCooldownJob = lifecycleScope.launch {
            for (remaining in 60 downTo 1) {
                signUpButton.isEnabled = false
                signUpButton.text = "Try again in ${remaining}s"
                delay(1000)
            }
            signUpButton.isEnabled = true
            signUpButton.text = "Sign Up"
        }
    }

    override fun onDestroy() {
        signUpCooldownJob?.cancel()
        super.onDestroy()
    }
}
