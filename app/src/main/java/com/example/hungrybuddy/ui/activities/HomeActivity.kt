package com.example.hungrybuddy.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hungrybuddy.databinding.ActivityHomeBinding
import com.example.hungrybuddy.ui.adapters.RecipeAdapter
import com.example.hungrybuddy.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch
import android.content.Intent

class HomeActivity : ComponentActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: RecipeViewModel by viewModels()

    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect Activity to the XML layout.
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        observeRecipes()

        // Load initial recipes when Home opens.
        // "chicken" is a starter search query.
        if (savedInstanceState == null) {
            viewModel.searchRecipes("chicken")
        }
    }

    private fun setupRecyclerView() {


        recipeAdapter = RecipeAdapter { recipe ->

            val recipeId = recipe.idMeal

            if (!recipeId.isNullOrBlank()) {

                val intent = Intent(
                    this,
                    RecipeDetailsActivity::class.java
                )

                intent.putExtra(
                    RecipeDetailsActivity.EXTRA_RECIPE_ID,
                    recipeId
                )

                startActivity(intent)

            } else {

                Toast.makeText(
                    this,
                    "Unable to open this recipe.",
                    Toast.LENGTH_SHORT
                ).show()
            }



        // We will open RecipeDetailsActivity here
            // after migrating the details screen.
        }

        binding.recyclerRecipes.apply {
            layoutManager = LinearLayoutManager(
                this@HomeActivity
            )
            adapter = recipeAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupButtons() {

        binding.btnSearchRecipes.setOnClickListener {
            showSearchDialog()
        }


        binding.btnSettings.setOnClickListener {

            val intent = Intent(
                this,
                SettingsActivity::class.java
            )

            startActivity(intent)
        }

    }

    private fun showSearchDialog() {

        val searchInput = EditText(this).apply {
            hint = "Enter a meal name"
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Search Recipes")
            .setView(searchInput)
            .setPositiveButton("Search") { _, _ ->

                val query = searchInput.text
                    .toString()
                    .trim()

                if (query.isNotBlank()) {
                    viewModel.searchRecipes(query)
                } else {
                    Toast.makeText(
                        this,
                        "Please enter a recipe name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeRecipes() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // Observe recipe data.
                launch {
                    viewModel.recipes.collect { recipes ->

                        recipeAdapter.submitList(recipes)

                        updateRecipeUi()
                    }
                }

                // Observe loading progress.
                launch {
                    viewModel.isLoading.collect {
                        updateRecipeUi()
                    }
                }

                // Observe API errors.
                launch {
                    viewModel.errorMessage.collect {
                        updateRecipeUi()
                    }
                }
            }
        }
    }

    private fun updateRecipeUi() {

        val recipes = viewModel.recipes.value
        val loading = viewModel.isLoading.value
        val error = viewModel.errorMessage.value

        binding.progressRecipes.visibility =
            if (loading) View.VISIBLE else View.GONE

        binding.recyclerRecipes.visibility =
            if (recipes.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvEmptyRecipes.visibility =
            if (!loading && recipes.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvEmptyRecipes.text =
            error ?: "No recipes found. Try another search."
    }
}
