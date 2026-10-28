
package com.example.hungrybuddy.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hungrybuddy.databinding.ActivityLoginBinding
import com.example.hungrybuddy.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check whether Firebase already has a signed-in user.
        if (auth.currentUser != null) {

            // Skip Login and open the Home screen.
            val intent = Intent(
                this,
                HomeActivity::class.java
            )

            // Remove Login from the navigation back stack.
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
            return
        }


        binding.btnLogin.setOnClickListener {
            loginUser()
        }


        binding.tvRegister.setOnClickListener {

            val intent = Intent(
                this,
                RegisterActivity::class.java
            )

            startActivity(intent)
        }

    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        when {
            email.isBlank() -> {
                binding.etEmail.error = "Please enter your email."
                return
            }

            !ValidationUtils.isValidEmail(email) -> {
                binding.etEmail.error =
                    "Please enter a valid email address."
                return
            }

            password.isBlank() -> {
                binding.etPassword.error = "Please enter your password."
                return
            }
        }

        binding.btnLogin.isEnabled = false
        binding.tvRegister.isEnabled = false
        binding.btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                binding.btnLogin.isEnabled = true
                binding.tvRegister.isEnabled = true
                binding.btnLogin.text = "Login"


                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this,
                        HomeActivity::class.java
                    )

                    startActivity(intent)
                    finish()



                // Home screen navigation will be added
                    // when we migrate HomeScreen.kt.

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Login failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
