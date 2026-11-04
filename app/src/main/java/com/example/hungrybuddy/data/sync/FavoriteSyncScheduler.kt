package com.example.hungrybuddy.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object FavoriteSyncScheduler {

    private const val ONE_TIME_WORK =
        "hungry_buddy_favorite_sync_once"

    private const val PERIODIC_WORK =
        "hungry_buddy_favorite_sync_periodic"

    fun scheduleSync(
        context: Context,
        userId: String
    ) {

        require(userId.isNotBlank()) {
            "A valid Firebase user ID is required."
        }

        val appContext = context.applicationContext

        val workManager = WorkManager.getInstance(appContext)

        // Wait until an internet connection is available.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Pass the authenticated user's ID to the worker.
        val inputData = workDataOf(
            FavoriteSyncWorker.KEY_USER_ID to userId
        )

        // Run an initial synchronisation as soon as possible.
        val immediateRequest =
            OneTimeWorkRequestBuilder<FavoriteSyncWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )

        // Continue checking for changes periodically.
        val periodicRequest =
            PeriodicWorkRequestBuilder<FavoriteSyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    // Stop scheduled synchronisation when the user logs out.
    fun cancelSync(context: Context) {

        val workManager = WorkManager.getInstance(
            context.applicationContext
        )

        workManager.cancelUniqueWork(ONE_TIME_WORK)
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }
}