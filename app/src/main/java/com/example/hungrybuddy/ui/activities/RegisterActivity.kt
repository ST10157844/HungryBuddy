package com.example.hungrybuddy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hungrybuddy.databinding.ActivityRegisterBinding
import com.example.hungrybuddy.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect Kotlin to the XML layout.
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication.
        auth = FirebaseAuth.getInstance()

        // Create Account button.
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        // Navigate back to Login.
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val email = binding.etRegisterEmail.text
            .toString().trim()

        val password = binding.etRegisterPassword.text
            .toString()

        val confirmPassword = binding.etConfirmPassword.text
            .toString()

        // Clear previous error messages.
        binding.tvRegisterError.visibility = View.GONE

        // Validate user input.
        when {

            email.isBlank() -> {
                showError("Please enter your email.")
                return
            }

            !ValidationUtils.isValidEmail(email) -> {
                showError("Please enter a valid email address.")
                return
            }

            password.isBlank() -> {
                showError("Please enter a password.")
                return
            }

            !ValidationUtils.isValidPassword(password) -> {
                showError(
                    "Password must be at least 6 characters."
                )
                return
            }

            confirmPassword.isBlank() -> {
                showError("Please confirm your password.")
                return
            }

            !ValidationUtils.doPasswordsMatch(
                password,
                confirmPassword
            ) -> {
                showError("Passwords do not match.")
                return
            }
        }

        // Show loading indicator.
        setLoading(true)

        // Register user using Firebase Authentication.
        auth.createUserWithEmailAndPassword(
            email,
            password
        ).addOnCompleteListener(this) { task ->

            setLoading(false)

            if (task.isSuccessful) {

                Toast.makeText(
                    this,
                    "Account created successfully!",
                    Toast.LENGTH_LONG
                ).show()

                // Firebase automatically signs in new users.
                // Temporarily sign out so they can test Login.
                auth.signOut()

                // Navigate to XML Login Activity.
                val intent = Intent(
                    this,
                    LoginActivity::class.java
                )

                startActivity(intent)
                finish()

            } else {

                showError(
                    task.exception?.localizedMessage
                        ?: "Registration failed. Try again."
                )
            }
        }
    }

    private fun showError(message: String) {
        binding.tvRegisterError.text = message
        binding.tvRegisterError.visibility = View.VISIBLE
    }

    private fun setLoading(isLoading: Boolean) {

        binding.progressRegister.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.btnRegister.isEnabled = !isLoading
        binding.tvLogin.isEnabled = !isLoading

        binding.btnRegister.text =
            if (isLoading) "Creating Account..."
            else "Create Account"
    }
}
