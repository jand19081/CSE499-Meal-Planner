package io.github.and19081.mealplanner.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.MealPlannerRepository
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.RecipeRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class RecipesViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)

    val uiState = combine(
        RecipeRepository.recipes,
        _searchQuery,
        _sortByAlpha,
        MealPlannerRepository.ingredients,
        MealPlannerRepository.recipeIngredients
    ) { recipes, query, isAlpha, ingredients, recipeIngredients ->

        // 1. Filter
        val filtered = if (query.isBlank()) recipes else {
            recipes.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Group
        val grouped: Map<String, List<Recipe>> = if (isAlpha) {
            filtered.sortedBy { it.name }
                .groupBy { it.name.first().uppercase() }
                .toSortedMap()
        } else {
            mapOf("All Recipes" to filtered.sortedBy { it.name })
        }

        val exactMatch = recipes.any { it.name.equals(query, ignoreCase = true) }

        RecipesUiState(
            groupedRecipes = grouped,
            searchQuery = query,
            doesExactMatchExist = exactMatch,
            allIngredients = ingredients,
            allRecipeIngredients = recipeIngredients
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipesUiState(emptyMap(), "", false, emptyList(), emptyList()))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveRecipe(recipe: Recipe, ingredients: List<io.github.and19081.mealplanner.RecipeIngredient>) {
        // Save Recipe
        RecipeRepository.recipes.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.id == recipe.id }
            list.add(recipe)
            list
        }
        
        // Save Ingredients
        MealPlannerRepository.removeRecipeIngredients(recipe.id)
        ingredients.forEach { 
            MealPlannerRepository.addRecipeIngredient(it)
        }
    }

    fun deleteRecipe(id: Uuid) {
        RecipeRepository.recipes.update { current ->
            current.filter { it.id != id }
        }
    }
}

data class RecipesUiState(
    val groupedRecipes: Map<String, List<Recipe>>,
    val searchQuery: String,
    val doesExactMatchExist: Boolean,
    val allIngredients: List<Ingredient>,
    val allRecipeIngredients: List<io.github.and19081.mealplanner.RecipeIngredient>
)