
package com.example.hungrybuddy.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hungrybuddy.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.hungrybuddy.ui.navigation.BottomNavigationHelper
import androidx.appcompat.app.AppCompatDelegate
import com.example.hungrybuddy.utils.SettingsPreferences
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.LocaleListCompat
import com.example.hungrybuddy.R

class SettingsActivity : AppCompatActivity() {

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
            userEmail ?: getString(R.string.no_email_available)
    }

    private fun setupLanguage() {

    // These language tags match our Android resource folders:
    // values = English, values-zu = isiZulu, values-tn = Setswana.
    val languageTags = listOf("en", "zu", "tn")

    // Build the dropdown from the language names in strings.xml.
    val languageAdapter = ArrayAdapter.createFromResource(
        this,
        R.array.language_options,
        android.R.layout.simple_spinner_item
    ).apply {
        setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
    }

    binding.spinnerLanguage.adapter = languageAdapter

    // Restore the currently selected app language.
    val currentLanguageTag =
        AppCompatDelegate.getApplicationLocales()
            .get(0)?.language
            ?: SettingsPreferences.getLanguageTag(this)

    val selectedIndex = languageTags
        .indexOf(currentLanguageTag)
        .let { if (it >= 0) it else 0 }

    // Set the initial selection before attaching the listener.
    binding.spinnerLanguage.setSelection(selectedIndex, false)

    binding.spinnerLanguage.onItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTag =
                    languageTags.getOrElse(position) { "en" }

                val activeTag =
                    AppCompatDelegate.getApplicationLocales()
                        .get(0)?.language
                        ?: "en"

                if (selectedTag != activeTag) {

                    // Save the choice in HungryBuddy settings.
                    SettingsPreferences.setLanguageTag(
                        this@SettingsActivity,
                        selectedTag
                    )

                    // Apply the new locale to the app.
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(selectedTag)
                    )
                }
            }

            override fun onNothingSelected(
                parent: AdapterView<*>?
            ) = Unit
        }
}

    private fun setupDarkMode() {

    // Restore the user's previously saved appearance preference.
    val darkModeEnabled =
        SettingsPreferences.isDarkModeEnabled(this)

    // Display the saved state before registering the listener.
    binding.switchDarkMode.isChecked = darkModeEnabled
    binding.switchDarkMode.isEnabled = true

    binding.tvMoreSettings.text =
    if (darkModeEnabled) {
        getString(R.string.dark_mode_enabled)
    } else {
        getString(R.string.dark_mode_hint)
    }

    // Save the preference and apply the theme when the switch changes.
    binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

        SettingsPreferences.setDarkModeEnabled(
            this,
            isChecked
        )

       binding.tvMoreSettings.text =
    if (isChecked) {
        getString(R.string.dark_mode_enabled)
    } else {
        getString(R.string.light_mode_enabled)
    }

        val nightMode =
            if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
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
            .setTitle(getString(R.string.logout_confirmation_title))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->

                // Sign out from Firebase Authentication.
                auth.signOut()

                Toast.makeText(
                    this,
                   getString(R.string.logout_success),
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
