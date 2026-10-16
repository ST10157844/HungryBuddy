package com.example.hungrybuddy.ui.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
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

class RecipeDetailsActivity : ComponentActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding

    private val viewModel: RecipeViewModel by viewModels()

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous screen.

        binding.btnBack.setOnClickListener {
            finish()
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
