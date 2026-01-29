package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MealsViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)

    val uiState = combine(
        MealRepository.meals,
        MealRepository.mealComponents,
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        _searchQuery,
        _sortByAlpha,
        RecipeRepository.recipeIngredients
    ) { args: Array<Any?> ->
        val meals = args[0] as List<Meal>
        val components = args[1] as List<MealComponent>
        val recipes = args[2] as List<Recipe>
        val ingredients = args[3] as List<Ingredient>
        val query = args[4] as String
        val isAlpha = args[5] as Boolean
        val recipeIngredients = args[6] as List<RecipeIngredient>

        // 1. Filter
        val filtered = if (query.isBlank()) meals else {
            meals.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Sort
        val sorted = if (isAlpha) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
        
        // Group (Just "All" for now)
        val grouped = mapOf("All Meals" to sorted)

        MealsUiState(
            groupedMeals = grouped,
            searchQuery = query,
            allComponents = components,
            allRecipes = recipes,
            allIngredients = ingredients,
            allRecipeIngredients = recipeIngredients
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        MealsUiState(emptyMap(), "", emptyList(),
            emptyList(), emptyList(), emptyList())
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveMeal(meal: Meal, components: List<MealComponent>) {
        // Update Meal
        MealRepository.upsertMeal(meal)

        // Update Components
        MealRepository.removeComponentsForMeal(meal.id)
        components.forEach { 
            MealRepository.addMealComponent(it)
        }
    }

    fun deleteMeal(meal: Meal) {
        MealRepository.removeMeal(meal.id)
    }
}

data class MealsUiState(
    val groupedMeals: Map<String, List<Meal>>,
    val searchQuery: String,
    val allComponents: List<MealComponent>,
    val allRecipes: List<Recipe>,
    val allIngredients: List<Ingredient>,
    val allRecipeIngredients: List<RecipeIngredient>
)