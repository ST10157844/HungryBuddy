package com.example.hungrybuddy.data.remote

import com.example.hungrybuddy.data.local.FavoriteRecipeEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class RemoteFavoriteRecipe(
    val userId: String,
    val mealId: String,
    val mealName: String,
    val category: String,
    val area: String,
    val imageUrl: String,
    val recipeJson: String,
    val updatedAt: Long
)

class FirestoreFavoriteRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private fun favoritesCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("favourites")

    // Upload a favourite recipe to Firestore.
    suspend fun uploadFavorite(favorite: FavoriteRecipeEntity) {

        val recipeData = hashMapOf(
            "userId" to favorite.userId,
            "mealId" to favorite.mealId,
            "mealName" to favorite.mealName,
            "category" to favorite.category,
            "area" to favorite.area,
            "imageUrl" to favorite.imageUrl,
            "recipeJson" to favorite.recipeJson,
            "updatedAt" to favorite.updatedAt
        )

        favoritesCollection(favorite.userId)
            .document(favorite.mealId)
            .set(recipeData)
            .await()
    }

    // Retrieve all favourites belonging to a user.
    suspend fun getFavorites(
        userId: String
    ): List<RemoteFavoriteRecipe> {

        val snapshot = favoritesCollection(userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->

            val mealName = document.getString("mealName")
                ?: return@mapNotNull null

            val recipeJson = document.getString("recipeJson")
                ?: return@mapNotNull null

            RemoteFavoriteRecipe(
                userId = document.getString("userId") ?: userId,
                mealId = document.getString("mealId")
                    ?: document.id,
                mealName = mealName,
                category = document.getString("category") ?: "",
                area = document.getString("area") ?: "",
                imageUrl = document.getString("imageUrl") ?: "",
                recipeJson = recipeJson,
                updatedAt = document.getLong("updatedAt") ?: 0L
            )
        }
    }

    // Delete a favourite from Firestore.
    suspend fun deleteFavorite(
        userId: String,
        mealId: String
    ) {
        favoritesCollection(userId)
            .document(mealId)
            .delete()
            .await()
    }
}