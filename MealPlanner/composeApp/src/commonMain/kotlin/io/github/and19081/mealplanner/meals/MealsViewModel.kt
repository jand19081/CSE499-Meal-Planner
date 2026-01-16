package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.Meal
import io.github.and19081.mealplanner.MealComponent
import io.github.and19081.mealplanner.MealPlannerRepository
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.RecipeRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MealsViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)

    val uiState = combine(
        MealPlannerRepository.meals,
        MealPlannerRepository.mealComponents,
        RecipeRepository.recipes,
        MealPlannerRepository.ingredients,
        _searchQuery,
        _sortByAlpha
    ) { args: Array<Any?> ->
        val meals = args[0] as List<Meal>
        val components = args[1] as List<MealComponent>
        val recipes = args[2] as List<Recipe>
        val ingredients = args[3] as List<Ingredient>
        val query = args[4] as String
        val isAlpha = args[5] as Boolean

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
            allIngredients = ingredients
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        MealsUiState(emptyMap(), "", emptyList(),
            emptyList(), emptyList())
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveMeal(meal: Meal, components: List<MealComponent>) {
        // Update Meal
        MealPlannerRepository.meals.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.id == meal.id }
            list.add(meal)
            list
        }

        // Update Components
        MealPlannerRepository.mealComponents.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.mealId == meal.id }
            list.addAll(components)
            list
        }
    }

    fun deleteMeal(meal: Meal) {
        MealPlannerRepository.meals.update { current ->
            current.filter { it.id != meal.id }
        }
        
        // Cleanup components
        MealPlannerRepository.mealComponents.update { current ->
            current.filter { it.mealId != meal.id }
        }
    }
}

data class MealsUiState(
    val groupedMeals: Map<String, List<Meal>>,
    val searchQuery: String,
    val allComponents: List<MealComponent>,
    val allRecipes: List<Recipe>,
    val allIngredients: List<Ingredient>
)