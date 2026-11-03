package com.example.hungrybuddy.data.local

import androidx.room.Entity
import androidx.room.Index

// Tracks whether a favourite still needs synchronisation.
object FavoriteSyncStatus {
    const val PENDING_UPLOAD = "PENDING_UPLOAD"
    const val PENDING_DELETE = "PENDING_DELETE"
    const val SYNCED = "SYNCED"
}

@Entity(
    tableName = "favorite_recipes",
    primaryKeys = ["userId", "mealId"],
    indices = [
        Index(value = ["userId", "isDeleted"]),
        Index(value = ["userId", "syncStatus"])
    ]
)
data class FavoriteRecipeEntity(

    // Firebase account that owns this favourite.
    val userId: String,

    // Unique recipe ID from TheMealDB.
    val mealId: String,

    val mealName: String,
    val category: String,
    val area: String,
    val imageUrl: String,

    // Stores the complete recipe data for offline access.
    val recipeJson: String,

    // New favourites require uploading to Firestore.
    val syncStatus: String = FavoriteSyncStatus.PENDING_UPLOAD,

    // Allows removals to be synchronised later.
    val isDeleted: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)