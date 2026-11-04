package com.example.hungrybuddy.data.local

import android.util.Log
import com.example.hungrybuddy.model.Recipe
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRecipeRepository(
    private val favoriteDao: FavoriteRecipeDao,
    private val gson: Gson = Gson()
) {

    // Observe saved recipes belonging to the signed-in user.
    fun observeFavoriteRecipes(userId: String): Flow<List<Recipe>> {

        require(userId.isNotBlank()) {
            "A valid user ID is required."
        }

        return favoriteDao.observeFavorites(userId).map { favorites ->

            favorites.mapNotNull { favorite ->
                decodeRecipe(favorite)
            }
        }
    }

    // Save a recipe locally for offline access.
    suspend fun addFavorite(
        userId: String,
        recipe: Recipe
    ) {

        require(userId.isNotBlank()) {
            "A valid user ID is required."
        }

        val mealId = recipe.idMeal?.trim()

        require(!mealId.isNullOrBlank()) {
            "Recipe ID cannot be empty."
        }

        val favorite = FavoriteRecipeEntity(
            userId = userId,
            mealId = mealId,
            mealName = recipe.strMeal ?: "Unknown Recipe",
            category = recipe.strCategory ?: "",
            area = recipe.strArea ?: "",
            imageUrl = recipe.strMealThumb ?: "",
            recipeJson = gson.toJson(recipe),
            syncStatus = FavoriteSyncStatus.PENDING_UPLOAD,
            isDeleted = false,
            updatedAt = System.currentTimeMillis()
        )

        favoriteDao.saveFavorite(favorite)

        Log.i(TAG, "Favourite saved locally: $mealId")
    }

    // Mark a favourite for deletion and later synchronisation.
    suspend fun removeFavorite(
        userId: String,
        mealId: String
    ) {

        require(userId.isNotBlank()) {
            "A valid user ID is required."
        }

        require(mealId.isNotBlank()) {
            "Recipe ID cannot be empty."
        }

        val updated = favoriteDao.markAsDeleted(
            userId = userId,
            mealId = mealId,
            updatedAt = System.currentTimeMillis()
        )

        if (updated > 0) {
            Log.i(TAG, "Favourite marked for removal: $mealId")
        } else {
            Log.d(TAG, "Favourite was not found: $mealId")
        }
    }

    // Retrieve a complete saved recipe without using the API.
    suspend fun getFavoriteRecipe(
        userId: String,
        mealId: String
    ): Recipe? {

        val favorite = favoriteDao.getFavorite(
            userId = userId,
            mealId = mealId
        ) ?: return null

        if (favorite.isDeleted) {
            return null
        }

        return decodeRecipe(favorite)
    }

    // Return changes waiting to be synchronised.
    suspend fun getPendingChanges(
        userId: String
    ): List<FavoriteRecipeEntity> {

        return favoriteDao.getPendingChanges(userId)
    }

    // Mark a successfully uploaded favourite as synchronised.
    suspend fun markAsSynced(
        favorite: FavoriteRecipeEntity
    ): Int {

        return favoriteDao.markAsSynced(
            userId = favorite.userId,
            mealId = favorite.mealId,
            expectedStatus = favorite.syncStatus,
            expectedUpdatedAt = favorite.updatedAt
        )
    }

    // Remove a local deletion record after remote deletion succeeds.
    suspend fun deleteAfterRemoteRemoval(
        favorite: FavoriteRecipeEntity
    ): Int {

        return favoriteDao.deleteAfterRemoteRemoval(
            userId = favorite.userId,
            mealId = favorite.mealId,
            expectedUpdatedAt = favorite.updatedAt
        )
    }

    // Convert saved JSON back into the existing Recipe model.
    private fun decodeRecipe(
        favorite: FavoriteRecipeEntity
    ): Recipe? {

        return try {

            gson.fromJson(
                favorite.recipeJson,
                Recipe::class.java
            )

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unable to restore saved recipe: ${favorite.mealId}",
                exception
            )

            null
        }
    }

    companion object {
        private const val TAG = "FavoriteRepository"
    }
}