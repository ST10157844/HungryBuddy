package com.example.hungrybuddy.data.sync

import android.util.Log
import com.example.hungrybuddy.data.local.FavoriteRecipeDao
import com.example.hungrybuddy.data.local.FavoriteRecipeEntity
import com.example.hungrybuddy.data.local.FavoriteSyncStatus
import com.example.hungrybuddy.data.remote.FirestoreFavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FavoriteSyncResult(
    val uploadedToCloud: Int,
    val deletedFromCloud: Int,
    val downloadedToLocal: Int,
    val updatedFromCloud: Int,
    val removedStaleLocal: Int
)

class FavoriteSyncManager(
    private val favoriteDao: FavoriteRecipeDao,
    private val firestoreRepository: FirestoreFavoriteRepository
) {

    suspend fun syncFavorites(
        userId: String
    ): FavoriteSyncResult = withContext(Dispatchers.IO) {

        require(userId.isNotBlank()) {
            "A valid user ID is required for synchronisation."
        }

        var uploaded = 0
        var deleted = 0
        var downloaded = 0
        var updated = 0
        var removedLocally = 0

        Log.i(TAG, "Starting favourite synchronisation.")

        // STEP 1: Process pending local changes.
        val pendingChanges = favoriteDao.getPendingChanges(userId)

        for (favorite in pendingChanges) {

            when {

                // Delete removed favourites from Firestore.
                favorite.isDeleted ||
                    favorite.syncStatus == FavoriteSyncStatus.PENDING_DELETE -> {

                    firestoreRepository.deleteFavorite(
                        userId = userId,
                        mealId = favorite.mealId
                    )

                    deleted++

                    favoriteDao.deleteAfterRemoteRemoval(
                        userId = favorite.userId,
                        mealId = favorite.mealId,
                        expectedUpdatedAt = favorite.updatedAt
                    )
                }

                // Upload newly saved favourites.
                favorite.syncStatus == FavoriteSyncStatus.PENDING_UPLOAD -> {

                    firestoreRepository.uploadFavorite(favorite)

                    favoriteDao.markAsSynced(
    userId = favorite.userId,
    mealId = favorite.mealId,
    expectedStatus = favorite.syncStatus,
    expectedUpdatedAt = favorite.updatedAt
)

                    uploaded++
                }

                else -> {
                    Log.w(
                        TAG,
                        "Unknown synchronisation status: ${favorite.syncStatus}"
                    )
                }
            }
        }

        // STEP 2: Retrieve the latest favourites from Firestore.
        val remoteFavorites =
            firestoreRepository.getFavorites(userId)

        val remoteMealIds = remoteFavorites
            .map { it.mealId }
            .toSet()

        // STEP 3: Import remote favourites into Room.
        for (remote in remoteFavorites) {

            val localFavorite = FavoriteRecipeEntity(
                userId = userId,
                mealId = remote.mealId,
                mealName = remote.mealName,
                category = remote.category,
                area = remote.area,
                imageUrl = remote.imageUrl,
                recipeJson = remote.recipeJson,
                syncStatus = FavoriteSyncStatus.SYNCED,
                isDeleted = false,
                updatedAt = remote.updatedAt
            )

            // Insert only if the local record does not already exist.
            val inserted = favoriteDao.insertRemoteFavoriteIfAbsent(
                localFavorite
            )

            if (inserted != -1L) {

                downloaded++

            } else {

                // Refresh only existing SYNCED records.
                // Pending local changes are protected.
                val refreshed = favoriteDao.updateSyncedFavoriteFromRemote(
                    userId = userId,
                    mealId = remote.mealId,
                    mealName = remote.mealName,
                    category = remote.category,
                    area = remote.area,
                    imageUrl = remote.imageUrl,
                    recipeJson = remote.recipeJson,
                    updatedAt = remote.updatedAt
                )

                if (refreshed > 0) {
                    updated++
                }
            }
        }

        // STEP 4: Remove local synced favourites missing from Firestore.
        val localFavorites = favoriteDao.getActiveFavorites(userId)

        for (local in localFavorites) {

            if (
                local.syncStatus == FavoriteSyncStatus.SYNCED &&
                local.mealId !in remoteMealIds
            ) {

                val removed = favoriteDao.deleteSyncedFavoriteIfUnchanged(
                    userId = userId,
                    mealId = local.mealId,
                    expectedUpdatedAt = local.updatedAt
                )

                if (removed > 0) {
                    removedLocally++
                }
            }
        }

        Log.i(
            TAG,
            "Synchronisation completed. " +
                "Uploaded: $uploaded, " +
                "Deleted remotely: $deleted, " +
                "Downloaded: $downloaded, " +
                "Updated: $updated, " +
                "Removed locally: $removedLocally"
        )

        FavoriteSyncResult(
            uploadedToCloud = uploaded,
            deletedFromCloud = deleted,
            downloadedToLocal = downloaded,
            updatedFromCloud = updated,
            removedStaleLocal = removedLocally
        )
    }

    companion object {
        private const val TAG = "FavoriteSyncManager"
    }
}