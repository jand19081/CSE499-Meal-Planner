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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RecipesViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _repoTrigger = MutableStateFlow(0) // To force refresh of ingredients

    val uiState = combine(
        RecipeRepository.recipes,
        _searchQuery,
        _sortByAlpha,
        _repoTrigger
    ) { recipes, query, isAlpha, _ ->

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
            allIngredients = MealPlannerRepository.ingredients.toList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipesUiState(emptyMap(), "", false, emptyList()))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveRecipe(recipe: Recipe) {
        val current = RecipeRepository.recipes.value.toMutableList()
        current.removeAll { it.id == recipe.id }
        current.add(recipe)
        RecipeRepository.recipes.value = current
    }

    fun deleteRecipe(id: Uuid) {
        val current = RecipeRepository.recipes.value.toMutableList()
        current.removeAll { it.id == id }
        RecipeRepository.recipes.value = current
    }
}

data class RecipesUiState(
    val groupedRecipes: Map<String, List<Recipe>>,
    val searchQuery: String,
    val doesExactMatchExist: Boolean,
    val allIngredients: List<Ingredient>
)
