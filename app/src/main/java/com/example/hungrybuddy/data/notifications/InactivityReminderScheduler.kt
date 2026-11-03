package com.example.hungrybuddy.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object InactivityReminderScheduler {

    private const val WORK_NAME = "hungry_buddy_inactivity_reminder"

    const val PREFS_NAME = "hungry_buddy_activity"
    const val KEY_APP_FOREGROUND = "app_in_foreground"

    fun schedule(context: Context, userId: String) {

        if (userId.isBlank()) return

        val request =
            OneTimeWorkRequestBuilder<InactivityReminderWorker>()
                .setInitialDelay(24, TimeUnit.HOURS)

                .setInputData(
                    workDataOf(
                        InactivityReminderWorker.KEY_USER_ID to userId
                    )
                )
                .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WORK_NAME)
    }

    fun setAppForeground(context: Context, foreground: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_FOREGROUND, foreground)
            .apply()
    }
}