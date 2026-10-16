package com.example.hungrybuddy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hungrybuddy.R
import com.example.hungrybuddy.model.Recipe

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(
    DIFF_CALLBACK
) {

    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Recipe>() {

                override fun areItemsTheSame(
                    oldItem: Recipe,
                    newItem: Recipe
                ): Boolean {
                    return oldItem.idMeal != null &&
                            oldItem.idMeal == newItem.idMeal
                }

                override fun areContentsTheSame(
                    oldItem: Recipe,
                    newItem: Recipe
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    inner class RecipeViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val recipeImage: ImageView =
            itemView.findViewById(R.id.ivRecipeImage)

        private val recipeName: TextView =
            itemView.findViewById(R.id.tvRecipeName)

        private val recipeCategory: TextView =
            itemView.findViewById(R.id.tvRecipeCategory)

        fun bind(recipe: Recipe) {

            // Display recipe name.
            recipeName.text =
                recipe.strMeal ?: "Unknown Recipe"

            // Display category and country.
            val category = recipe.strCategory
                ?.takeIf { it.isNotBlank() }

            val area = recipe.strArea
                ?.takeIf { it.isNotBlank() }

            val details = listOfNotNull(
                category,
                area
            ).joinToString(" • ")

            recipeCategory.text =
                details.ifBlank { "Recipe" }

            // Load recipe image using Glide.
            Glide.with(itemView)
                .load(recipe.strMealThumb)
                .placeholder(
                    android.R.drawable.ic_menu_gallery
                )
                .error(
                    android.R.drawable.ic_menu_report_image
                )
                .centerCrop()
                .into(recipeImage)

            recipeImage.contentDescription =
                "${recipe.strMeal ?: "Recipe"} image"

            // Handle recipe selection.
            itemView.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecipeViewHolder {

        val view = LayoutInflater.from(
            parent.context
        ).inflate(
            R.layout.item_recipe,
            parent,
            false
        )

        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RecipeViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }
}
