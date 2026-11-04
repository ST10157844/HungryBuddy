package com.example.hungrybuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRecipeDao {

    // Observe active favourites belonging to one user.
    @Query("""
        SELECT * FROM favorite_recipes
        WHERE userId = :userId
        AND isDeleted = 0
        ORDER BY updatedAt DESC
    """)
    fun observeFavorites(
        userId: String
    ): Flow<List<FavoriteRecipeEntity>>

    // Find a particular favourite.
    @Query("""
        SELECT * FROM favorite_recipes
        WHERE userId = :userId
        AND mealId = :mealId
        LIMIT 1
    """)
    suspend fun getFavorite(
        userId: String,
        mealId: String
    ): FavoriteRecipeEntity?

    // Save or update a favourite.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorite(
        favorite: FavoriteRecipeEntity
    )

    // Mark a favourite for removal.
    @Query("""
        UPDATE favorite_recipes
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE userId = :userId
        AND mealId = :mealId
    """)
    suspend fun markAsDeleted(
        userId: String,
        mealId: String,
        updatedAt: Long
    ): Int

    // Get local changes waiting for synchronisation.
    @Query("""
        SELECT * FROM favorite_recipes
        WHERE userId = :userId
        AND syncStatus != 'SYNCED'
        ORDER BY updatedAt ASC
    """)
    suspend fun getPendingChanges(
        userId: String
    ): List<FavoriteRecipeEntity>

    // Mark an uploaded favourite as synchronised.
    // The timestamp check protects newer local changes.
    @Query("""
        UPDATE favorite_recipes
        SET syncStatus = 'SYNCED'
        WHERE userId = :userId
        AND mealId = :mealId
        AND syncStatus = :expectedStatus
        AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun markAsSynced(
        userId: String,
        mealId: String,
        expectedStatus: String,
        expectedUpdatedAt: Long
    ): Int

    // Remove a local deletion record after remote deletion succeeds.
    @Query("""
        DELETE FROM favorite_recipes
        WHERE userId = :userId
        AND mealId = :mealId
        AND isDeleted = 1
        AND syncStatus = 'PENDING_DELETE'
        AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun deleteAfterRemoteRemoval(
        userId: String,
        mealId: String,
        expectedUpdatedAt: Long
    ): Int

    // Retrieve all active local favourites for remote comparison.
    @Query("""
        SELECT * FROM favorite_recipes
        WHERE userId = :userId
        AND isDeleted = 0
        ORDER BY updatedAt DESC
    """)
    suspend fun getActiveFavorites(
        userId: String
    ): List<FavoriteRecipeEntity>

    // Import a remote favourite only if it does not already exist.
    // Existing local records are never replaced by this operation.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteFavoriteIfAbsent(
        favorite: FavoriteRecipeEntity
    ): Long

    // Update only an already-synchronised record when Firestore is newer.
    @Query("""
        UPDATE favorite_recipes
        SET mealName = :mealName,
            category = :category,
            area = :area,
            imageUrl = :imageUrl,
            recipeJson = :recipeJson,
            syncStatus = 'SYNCED',
            isDeleted = 0,
            updatedAt = :updatedAt
        WHERE userId = :userId
        AND mealId = :mealId
        AND syncStatus = 'SYNCED'
        AND isDeleted = 0
        AND updatedAt < :updatedAt
    """)
    suspend fun updateSyncedFavoriteFromRemote(
        userId: String,
        mealId: String,
        mealName: String,
        category: String,
        area: String,
        imageUrl: String,
        recipeJson: String,
        updatedAt: Long
    ): Int

    // Remove a synced local record if it no longer exists remotely.
    // The timestamp check protects changes made during synchronisation.
    @Query("""
        DELETE FROM favorite_recipes
        WHERE userId = :userId
        AND mealId = :mealId
        AND syncStatus = 'SYNCED'
        AND isDeleted = 0
        AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun deleteSyncedFavoriteIfUnchanged(
        userId: String,
        mealId: String,
        expectedUpdatedAt: Long
    ): Int
}