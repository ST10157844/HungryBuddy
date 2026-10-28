
package com.example.hungrybuddy.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hungrybuddy.databinding.ActivitySearchBinding
import com.example.hungrybuddy.ui.adapters.RecipeAdapter
import com.example.hungrybuddy.viewmodel.RecipeViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.hungrybuddy.ui.navigation.BottomNavigationHelper

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    private val viewModel: RecipeViewModel by viewModels()

    private lateinit var recipeAdapter: RecipeAdapter

    private var hasSearched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hasSearched =
            savedInstanceState?.getBoolean("has_searched")
                ?: false


        setupRecyclerView()
        setupButtons()
        observeSearchResults()

// Enable XML bottom navigation.
        BottomNavigationHelper.setup(this)

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
        }

        binding.recyclerSearchRecipes.apply {
            layoutManager = LinearLayoutManager(
                this@SearchActivity
            )
            adapter = recipeAdapter
        }
    }

    private fun setupButtons() {

        // Return to Home.
        binding.btnSearchBack.setOnClickListener {
            finish()
        }

        // Search using the button.
        binding.btnRunSearch.setOnClickListener {
            performSearch()
        }

        // Search using the keyboard action.
        binding.etSearchQuery.setOnEditorActionListener {
                _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {

        val query = binding.etSearchQuery.text
            .toString()
            .trim()

        if (query.isBlank()) {

            binding.etSearchQuery.error =
                "Please enter a recipe name."

            return
        }

        hasSearched = true

        // Reuse existing Retrofit and repository logic.
        viewModel.searchRecipes(query)
    }

    private fun observeSearchResults() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                combine(
                    viewModel.recipes,
                    viewModel.isLoading,
                    viewModel.errorMessage
                ) { recipes, loading, error ->

                    Triple(recipes, loading, error)

                }.collect { (recipes, loading, error) ->

                    recipeAdapter.submitList(recipes)

                    binding.progressSearch.visibility =
                        if (loading) View.VISIBLE else View.GONE

                    val showRecipes =
                        !loading &&
                                error == null &&
                                recipes.isNotEmpty()

                    binding.recyclerSearchRecipes.visibility =
                        if (showRecipes) View.VISIBLE
                        else View.GONE

                    val showStatus =
                        !loading && !showRecipes

                    binding.tvSearchStatus.visibility =
                        if (showStatus) View.VISIBLE
                        else View.GONE

                    binding.tvSearchStatus.text = when {

                        error != null -> error

                        hasSearched ->
                            "No recipes found. Try another search."

                        else ->
                            "Enter a meal name to get started."
                    }

                    binding.tvSearchStatus.setTextColor(
                        Color.parseColor(
                            if (error != null) "#D32F2F"
                            else "#777777"
                        )
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("has_searched", hasSearched)
        super.onSaveInstanceState(outState)
    }
}
