package com.example.imilipocket.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.imilipocket.R
import com.example.imilipocket.auth.SupabaseAuthService
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var authService: SupabaseAuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        authService = SupabaseAuthService(this)

        if (authService.isSignedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnGoToSignUp = findViewById<Button>(R.id.btnGoToSignUp)

        btnSignIn.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignIn.isEnabled = false
            lifecycleScope.launch {
                val result = authService.signIn(email, password)
                btnSignIn.isEnabled = true

                if (result.success) {
                    Toast.makeText(this@SignInActivity, "Welcome back", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@SignInActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
