
package com.example.hungrybuddy.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.hungrybuddy.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.hungrybuddy.ui.navigation.BottomNavigationHelper

class SettingsActivity : ComponentActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect Kotlin with the Settings XML layout.
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication.
        auth = FirebaseAuth.getInstance()

        // Protect the screen from unauthenticated access.
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }


        setupAccount()
        setupLanguage()
        setupDarkMode()
        setupButtons()

// Initialize bottom navigation.
        BottomNavigationHelper.setup(this)

    }

    private fun setupAccount() {

        // Get the email of the authenticated user.
        val userEmail = auth.currentUser?.email

        binding.tvUserEmail.text =
            userEmail ?: "No email available"
    }

    private fun setupLanguage() {

        // Display current supported language.
        binding.tvLanguage.text = "English"
    }

    private fun setupDarkMode() {

        // Dark mode requires a separate DayNight theme
        // implementation and replacement of fixed XML colors.
        // Disable the switch until that implementation is ready.
        binding.switchDarkMode.isChecked = false
        binding.switchDarkMode.isEnabled = false

        binding.tvMoreSettings.text =
            "Dark mode and additional language options are coming soon."
    }

    private fun setupButtons() {

        // Return to HomeActivity.
        binding.btnSettingsBack.setOnClickListener {
            finish()
        }

        // Confirm before signing out.
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage(
                "Are you sure you want to log out of HungryBuddy?"
            )
            .setPositiveButton("Logout") { _, _ ->

                // Sign out from Firebase Authentication.
                auth.signOut()

                Toast.makeText(
                    this,
                    "Logged out successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        // Clear authenticated screens from the back stack.
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
