package com.example.hungrybuddy.utils

import android.content.Context

/**
 * Stores HungryBuddy settings across Activity and app restarts.
 */
object SettingsPreferences {

    private const val PREFS_NAME = "hungrybuddy_settings"

    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_LANGUAGE = "language"

    fun isDarkModeEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    fun getLanguageTag(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguageTag(context: Context, languageTag: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageTag)
            .apply()
    }
}