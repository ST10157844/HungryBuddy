package com.example.hungrybuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.hungrybuddy.utils.SettingsPreferences

/**
 * Applies the saved appearance before the app's Activities are displayed.
 */
class HungryBuddyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val savedMode = if (
            SettingsPreferences.isDarkModeEnabled(this)
        ) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(savedMode)
    }
}