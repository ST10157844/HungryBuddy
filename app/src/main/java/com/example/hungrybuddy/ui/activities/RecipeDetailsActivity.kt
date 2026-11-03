package com.example.hungrybuddy.ui.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.hungrybuddy.databinding.ActivityRecipeDetailsBinding
import com.example.hungrybuddy.model.Recipe
import com.example.hungrybuddy.viewmodel.RecipeViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.util.Log
import com.example.hungrybuddy.data.local.FavoriteRecipeRepository
import com.example.hungrybuddy.data.local.HungryBuddyDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.example.hungrybuddy.R
import com.example.hungrybuddy.data.sync.FavoriteSyncScheduler

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding

    private val viewModel: RecipeViewModel by viewModels()
private lateinit var favoriteRepository: FavoriteRecipeRepository

private var currentUserId: String? = null

private var isCurrentRecipeFavorite = false

    companion object {
         const val EXTRA_RECIPE_ID = "extra_recipe_id"
    const val EXTRA_RECIPE_JSON = "extra_recipe_json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
// Initialise local favourites storage.
favoriteRepository = FavoriteRecipeRepository(
    HungryBuddyDatabase.getDatabase(applicationContext)
        .favoriteRecipeDao()
)

// Identify the currently signed-in Firebase user.
currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Return to the previous screen.

        binding.btnBack.setOnClickListener {
            finish()
        }
// Restore a saved recipe directly from local data.
// This allows favourites to open without an API request.
val offlineRecipeJson = intent.getStringExtra(EXTRA_RECIPE_JSON)

if (!offlineRecipeJson.isNullOrBlank()) {

    val offlineRecipe = try {
        Gson().fromJson(
            offlineRecipeJson,
            Recipe::class.java
        )
    } catch (exception: Exception) {
        Log.e("RecipeDetails", "Could not restore offline recipe", exception)
        null
    }

    if (offlineRecipe != null && !offlineRecipe.idMeal.isNullOrBlank()) {
        displayRecipe(offlineRecipe)
        return
    }
}


        // Get the selected recipe ID from HomeActivity.
        val recipeId = intent.getStringExtra(EXTRA_RECIPE_ID)

        if (recipeId.isNullOrBlank()) {
            showError("Invalid recipe ID.")
            return
        }

        observeRecipeDetails()

        // Reuse the existing API-loading function.
        if (
            viewModel.selectedRecipe.value == null &&
            !viewModel.isLoadingDetails.value
        ) {
            viewModel.loadRecipeDetails(recipeId)
        }
    }

    private fun observeRecipeDetails() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                combine(
                    viewModel.selectedRecipe,
                    viewModel.isLoadingDetails,
                    viewModel.errorMessage
                ) { recipe, loading, error ->

                    Triple(recipe, loading, error)

                }.collect { (recipe, loading, error) ->

                    binding.progressRecipeDetails.visibility =
                        if (loading) View.VISIBLE else View.GONE

                    when {

                        loading -> {
                            binding.recipeContent.visibility = View.GONE
                            binding.tvRecipeError.visibility = View.GONE
                        }

                        recipe != null -> {
                            displayRecipe(recipe)
                        }

                        else -> {
                            showError(
                                error ?: "Unable to load this recipe."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun displayRecipe(recipe: Recipe) {

        binding.recipeContent.visibility = View.VISIBLE
        binding.tvRecipeError.visibility = View.GONE

        // Display recipe name.
        binding.tvRecipeDetailName.text =
            recipe.strMeal ?: "Unknown Recipe"

        // Display category.
        binding.tvRecipeDetailCategory.text =
            "Category: ${recipe.strCategory ?: "Unknown"}"

        // Display cuisine.
        binding.tvRecipeDetailArea.text =
            "Cuisine: ${recipe.strArea ?: "Unknown"}"

        // Load recipe image.
        Glide.with(binding.ivRecipeDetailImage)
            .load(recipe.strMealThumb)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(binding.ivRecipeDetailImage)

        // Display all available ingredients.
        displayIngredients(recipe)

        // Display cooking instructions.
        binding.tvRecipeInstructions.text =
            recipe.strInstructions
                ?.takeIf { it.isNotBlank() }
                ?: "No instructions available."

        // Display optional YouTube button.
        setupVideoButton(recipe.strYoutube)
setupFavoriteButton(recipe)
    }

private fun setupFavoriteButton(recipe: Recipe) {

    val button = binding.btnToggleFavorite

    val userId = currentUserId

    val mealId = recipe.idMeal?.trim()

    // Require an authenticated user and valid recipe ID.
    if (userId.isNullOrBlank() || mealId.isNullOrBlank()) {
        button.isEnabled = false
        button.setText(R.string.favourites_sign_in_message)
        return
    }

    // Prevent interaction until the saved status is checked.
    button.isEnabled = false

    lifecycleScope.launch {

        try {

            isCurrentRecipeFavorite =
                favoriteRepository.getFavoriteRecipe(
                    userId,
                    mealId
                ) != null

            button.setText(
                if (isCurrentRecipeFavorite) {
                    R.string.favourites_remove_button
                } else {
                    R.string.favourites_save_button
                }
            )

        } catch (exception: Exception) {

            Log.e(
                "RecipeDetails",
                "Could not check favourite status",
                exception
            )

            isCurrentRecipeFavorite = false
            button.setText(R.string.favourites_save_button)

        } finally {
            button.isEnabled = true
        }
    }

    // Save or remove the recipe locally.
    button.setOnClickListener {

        button.isEnabled = false

        lifecycleScope.launch {

            try {

                if (isCurrentRecipeFavorite) {

                    favoriteRepository.removeFavorite(
                        userId,
                        mealId
                    )

                    // Queue synchronisation after removing a favourite.
                    FavoriteSyncScheduler.scheduleSync(
                        this@RecipeDetailsActivity,
                        userId
                    )

                    isCurrentRecipeFavorite = false

                    Toast.makeText(
                        this@RecipeDetailsActivity,
                        R.string.favourites_removed_message,
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    favoriteRepository.addFavorite(
                        userId,
                        recipe
                    )

                    // Queue synchronisation after saving a favourite.
                    FavoriteSyncScheduler.scheduleSync(
                        this@RecipeDetailsActivity,
                        userId
                    )

                    isCurrentRecipeFavorite = true

                    Toast.makeText(
                        this@RecipeDetailsActivity,
                        R.string.favourites_saved_message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                button.setText(
                    if (isCurrentRecipeFavorite) {
                        R.string.favourites_remove_button
                    } else {
                        R.string.favourites_save_button
                    }
                )

            } catch (exception: Exception) {

                Log.e(
                    "RecipeDetails",
                    "Could not update favourite",
                    exception
                )

                Toast.makeText(
                    this@RecipeDetailsActivity,
                    R.string.favourites_error_message,
                    Toast.LENGTH_SHORT
                ).show()

            } finally {
                button.isEnabled = true
            }
        }
    }
}
    private fun displayIngredients(recipe: Recipe) {

        // Match each ingredient with its measurement.
        val ingredients = listOf(
            recipe.strIngredient1 to recipe.strMeasure1,
            recipe.strIngredient2 to recipe.strMeasure2,
            recipe.strIngredient3 to recipe.strMeasure3,
            recipe.strIngredient4 to recipe.strMeasure4,
            recipe.strIngredient5 to recipe.strMeasure5,
            recipe.strIngredient6 to recipe.strMeasure6,
            recipe.strIngredient7 to recipe.strMeasure7,
            recipe.strIngredient8 to recipe.strMeasure8,
            recipe.strIngredient9 to recipe.strMeasure9,
            recipe.strIngredient10 to recipe.strMeasure10,
            recipe.strIngredient11 to recipe.strMeasure11,
            recipe.strIngredient12 to recipe.strMeasure12,
            recipe.strIngredient13 to recipe.strMeasure13,
            recipe.strIngredient14 to recipe.strMeasure14,
            recipe.strIngredient15 to recipe.strMeasure15,
            recipe.strIngredient16 to recipe.strMeasure16,
            recipe.strIngredient17 to recipe.strMeasure17,
            recipe.strIngredient18 to recipe.strMeasure18,
            recipe.strIngredient19 to recipe.strMeasure19,
            recipe.strIngredient20 to recipe.strMeasure20
        )

        // Remove old ingredient views.
        binding.ingredientsContainer.removeAllViews()

        ingredients.forEach { (ingredient, measure) ->

            if (!ingredient.isNullOrBlank()) {

                val ingredientText = TextView(this).apply {

                    text = buildString {
                        append("• ")
                        append(ingredient.trim())

                        if (!measure.isNullOrBlank()) {
                            append(" — ")
                            append(measure.trim())
                        }
                    }

                    textSize = 16f
                    setTextColor(Color.parseColor("#444444"))

                    val bottomPadding =
                        (8 * resources.displayMetrics.density).toInt()

                    setPadding(0, 0, 0, bottomPadding)
                }

                binding.ingredientsContainer.addView(
                    ingredientText
                )
            }
        }
    }

    private fun setupVideoButton(url: String?) {

        val videoUrl = url?.trim().orEmpty()
        val videoUri = Uri.parse(videoUrl)
        val host = videoUri.host?.lowercase()

        val isValidYouTubeLink =
            (videoUri.scheme == "https" ||
                    videoUri.scheme == "http") &&
                    (host == "youtube.com" ||
                            host == "youtu.be" ||
                            host?.endsWith(".youtube.com") == true)

        binding.btnWatchVideo.visibility =
            if (isValidYouTubeLink) View.VISIBLE else View.GONE

        if (isValidYouTubeLink) {

            binding.btnWatchVideo.setOnClickListener {

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    videoUri
                )

                try {
                    startActivity(intent)
                } catch (exception: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "No app available to open this video.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showError(message: String) {

        binding.recipeContent.visibility = View.GONE
        binding.progressRecipeDetails.visibility = View.GONE

        binding.tvRecipeError.apply {
            text = message
            visibility = View.VISIBLE
        }
    }
}
