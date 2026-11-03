package com.example.hungrybuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.hungrybuddy.utils.SettingsPreferences
import com.example.hungrybuddy.data.sync.FavoriteSyncScheduler
import com.google.firebase.auth.FirebaseAuth

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
        // Resume favourite synchronisation for an existing signed-in user.
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FavoriteSyncScheduler.scheduleSync(this, userId)
        }
    }
}