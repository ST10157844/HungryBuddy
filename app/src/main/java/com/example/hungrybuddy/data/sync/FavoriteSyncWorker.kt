package com.example.hungrybuddy.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hungrybuddy.data.local.HungryBuddyDatabase
import com.example.hungrybuddy.data.remote.FirestoreFavoriteRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException

class FavoriteSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {

        val requestedUserId = inputData.getString(KEY_USER_ID)

        if (requestedUserId.isNullOrBlank()) {
            Log.e(TAG, "Missing Firebase user ID.")
            return Result.failure()
        }

        // Only synchronise for the currently authenticated account.
        val authenticatedUserId =
            FirebaseAuth.getInstance().currentUser?.uid

        if (authenticatedUserId != requestedUserId) {
            Log.i(TAG, "Skipping sync because the authenticated account changed.")
            return Result.success()
        }

        return try {

            Log.i(TAG, "Starting background favourite synchronisation.")

            val database = HungryBuddyDatabase.getDatabase(
                applicationContext
            )

            val favoriteDao = database.favoriteRecipeDao()

            val firestoreRepository = FirestoreFavoriteRepository()

            val syncManager = FavoriteSyncManager(
                favoriteDao = favoriteDao,
                firestoreRepository = firestoreRepository
            )

            syncManager.syncFavorites(requestedUserId)

            Log.i(TAG, "Background favourite synchronisation completed.")

            Result.success()

        } catch (exception: CancellationException) {

            throw exception

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Favourite synchronisation failed. WorkManager will retry.",
                exception
            )

            Result.retry()
        }
    }

    companion object {
        const val KEY_USER_ID = "favorite_sync_user_id"
        private const val TAG = "FavoriteSyncWorker"
    }
}