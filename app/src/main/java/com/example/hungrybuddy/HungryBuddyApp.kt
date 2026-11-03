package com.example.hungrybuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.hungrybuddy.utils.SettingsPreferences
import com.example.hungrybuddy.data.sync.FavoriteSyncScheduler
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.hungrybuddy.data.notifications.InactivityReminderScheduler

/**
 * Applies the saved appearance before the app's Activities are displayed.
 */
class HungryBuddyApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()

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

        // Track whether HungryBuddy is in the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Cancel an inactivity reminder when the user returns.
    override fun onStart(owner: LifecycleOwner) {

        InactivityReminderScheduler.setAppForeground(this, true)

        InactivityReminderScheduler.cancel(this)
    }

    // Start a new 24-hour reminder when the entire app goes into the background.
    override fun onStop(owner: LifecycleOwner) {

        InactivityReminderScheduler.setAppForeground(this, false)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (!userId.isNullOrBlank()) {
            InactivityReminderScheduler.schedule(this, userId)
        } else {
            InactivityReminderScheduler.cancel(this)
        }
    }
}