
package com.example.hungrybuddy.ui.navigation

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import com.example.hungrybuddy.R
import com.example.hungrybuddy.ui.activities.HomeActivity
import com.example.hungrybuddy.ui.activities.SearchActivity
import com.example.hungrybuddy.ui.activities.SettingsActivity

object BottomNavigationHelper {

    fun setup(activity: Activity) {

        val navHome = activity.findViewById<TextView>(
            R.id.navHome
        )

        val navSearch = activity.findViewById<TextView>(
            R.id.navSearch
        )

        val navSettings = activity.findViewById<TextView>(
            R.id.navSettings
        )

        // Highlight the currently selected screen.
        val selectedColor = Color.parseColor("#E66B31")
        val normalColor = Color.parseColor("#777777")

        navHome.setTextColor(
            if (activity is HomeActivity)
                selectedColor else normalColor
        )

        navSearch.setTextColor(
            if (activity is SearchActivity)
                selectedColor else normalColor
        )

        navSettings.setTextColor(
            if (activity is SettingsActivity)
                selectedColor else normalColor
        )

        // Navigate to Home.
        navHome.setOnClickListener {
            navigateTo(activity, HomeActivity::class.java)
        }

        // Navigate to Search.
        navSearch.setOnClickListener {
            navigateTo(activity, SearchActivity::class.java)
        }

        // Navigate to Settings.
        navSettings.setOnClickListener {
            navigateTo(activity, SettingsActivity::class.java)
        }
    }


    private fun navigateTo(
        activity: Activity,
        destination: Class<out Activity>
    ) {

        // Avoid navigating to the screen already displayed.
        if (destination.isInstance(activity)) {
            return
        }

        val intent = Intent(activity, destination)

        // Reuse an existing Activity instead of
        // creating a new instance every time.
        intent.addFlags(
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )

        activity.startActivity(intent)

        // IMPORTANT:
        // Do not call activity.finish() here.
        // Keeping SearchActivity alive preserves its
        // RecipeViewModel, search query and recipe list.
    }
}
