package com.example.hungrybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteRecipeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HungryBuddyDatabase : RoomDatabase() {

    abstract fun favoriteRecipeDao(): FavoriteRecipeDao

    companion object {

        @Volatile
        private var INSTANCE: HungryBuddyDatabase? = null

        fun getDatabase(context: Context): HungryBuddyDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HungryBuddyDatabase::class.java,
                    "hungry_buddy_database"
                ).build().also { database ->

                    INSTANCE = database
                }
            }
        }
    }
}