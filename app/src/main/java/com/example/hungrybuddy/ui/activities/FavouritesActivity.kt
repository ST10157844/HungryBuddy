package com.example.hungrybuddy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hungrybuddy.data.local.FavoriteRecipeRepository
import com.example.hungrybuddy.data.local.HungryBuddyDatabase
import com.example.hungrybuddy.databinding.ActivityFavouritesBinding
import com.example.hungrybuddy.ui.adapters.RecipeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.launch

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding

    private lateinit var favoriteRepository: FavoriteRecipeRepository

    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only authenticated users can access their favourites.
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId.isNullOrBlank()) {
            navigateToLogin()
            return
        }

        // Connect the screen to the local Room database.
        favoriteRepository = FavoriteRecipeRepository(
            HungryBuddyDatabase.getDatabase(applicationContext)
                .favoriteRecipeDao()
        )

        setupRecyclerView()
        observeFavorites(userId)

        // Initialise the shared bottom navigation.
        com.example.hungrybuddy.ui.navigation.BottomNavigationHelper
            .setup(this)
    }

    private fun setupRecyclerView() {

        recipeAdapter = RecipeAdapter { recipe ->

            // Pass the complete saved recipe to the details screen.
            // This avoids another API request when opening a favourite.
            val intent = Intent(
                this,
                RecipeDetailsActivity::class.java
            ).apply {

                putExtra(
                    RecipeDetailsActivity.EXTRA_RECIPE_ID,
                    recipe.idMeal
                )

                putExtra(
                    RecipeDetailsActivity.EXTRA_RECIPE_JSON,
                    Gson().toJson(recipe)
                )
            }

            startActivity(intent)
        }

        binding.rvFavoriteRecipes.apply {

            layoutManager = LinearLayoutManager(
                this@FavouritesActivity
            )

            adapter = recipeAdapter
            setHasFixedSize(false)
        }
    }

    private fun observeFavorites(userId: String) {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                favoriteRepository
                    .observeFavoriteRecipes(userId)
                    .collect { favorites ->

                        recipeAdapter.submitList(favorites)

                        val isEmpty = favorites.isEmpty()

                        binding.tvFavoritesEmpty.visibility =
                            if (isEmpty) View.VISIBLE else View.GONE

                        binding.rvFavoriteRecipes.visibility =
                            if (isEmpty) View.GONE else View.VISIBLE
                    }
            }
        }
    }

    private fun navigateToLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}