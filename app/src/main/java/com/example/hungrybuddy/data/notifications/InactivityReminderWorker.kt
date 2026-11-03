package com.example.hungrybuddy.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hungrybuddy.R
import com.example.hungrybuddy.ui.activities.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class InactivityReminderWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {

        val requestedUserId = inputData.getString(KEY_USER_ID)

        if (requestedUserId.isNullOrBlank()) {
            return Result.failure()
        }

        // Do not notify a user who is no longer signed in.
        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != requestedUserId) {
            Log.i(TAG, "Skipping reminder: account has changed.")
            return Result.success()
        }

        // Avoid sending a reminder while the user is using the app.
        val preferences = applicationContext.getSharedPreferences(
            InactivityReminderScheduler.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        if (
            preferences.getBoolean(
                InactivityReminderScheduler.KEY_APP_FOREGROUND,
                false
            )
        ) {
            Log.i(TAG, "Skipping reminder: app is currently active.")
            return Result.success()
        }

        // Android 13 and later require notification permission.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission has not been granted.")
            return Result.success()
        }

        return try {

            showReminder()
            Log.i(TAG, "Inactivity reminder displayed.")

            Result.success()

        } catch (exception: SecurityException) {

            Log.e(TAG, "Notification permission denied.", exception)
            Result.success()

        } catch (exception: Exception) {

            Log.e(TAG, "Could not display reminder.", exception)
            Result.retry()
        }
    }

    private fun showReminder() {

        val manager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recipe reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to discover recipes in HungryBuddy."
        }

        manager.createNotificationChannel(channel)

        val openAppIntent = Intent(
            applicationContext,
            HomeActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2401,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to discover something delicious!")
            .setContentText(
                "Come back to HungryBuddy and explore a new recipe today."
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your next favourite meal could be waiting. " +
                        "Open HungryBuddy and discover something delicious!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_USER_ID = "inactivity_user_id"

        private const val TAG = "InactivityReminderWorker"
        private const val CHANNEL_ID = "hungry_buddy_recipe_reminders"
        private const val NOTIFICATION_ID = 2401
    }
}