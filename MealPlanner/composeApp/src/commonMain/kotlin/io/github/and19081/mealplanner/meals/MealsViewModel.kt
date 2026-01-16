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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MealsViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)

    val uiState = combine(
        MealPlannerRepository.meals,
        MealPlannerRepository.mealComponents,
        RecipeRepository.recipes,
        _searchQuery,
        _sortByAlpha
    ) { meals, components, recipes, query, isAlpha ->

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
            allIngredients = MealPlannerRepository.ingredients.toList()
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealsUiState(emptyMap(), "", emptyList(), emptyList(), emptyList()))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveMeal(meal: Meal, components: List<MealComponent>) {
        // Update Meal
        val currentMeals = MealPlannerRepository.meals.value.toMutableList()
        currentMeals.removeAll { it.id == meal.id }
        currentMeals.add(meal)
        MealPlannerRepository.meals.value = currentMeals

        // Update Components
        val currentComps = MealPlannerRepository.mealComponents.value.toMutableList()
        currentComps.removeAll { it.mealId == meal.id }
        currentComps.addAll(components)
        MealPlannerRepository.mealComponents.value = currentComps
    }

    fun deleteMeal(meal: Meal) {
        val currentMeals = MealPlannerRepository.meals.value.toMutableList()
        currentMeals.removeAll { it.id == meal.id }
        MealPlannerRepository.meals.value = currentMeals
        
        // Cleanup components
        val currentComps = MealPlannerRepository.mealComponents.value.toMutableList()
        currentComps.removeAll { it.mealId == meal.id }
        MealPlannerRepository.mealComponents.value = currentComps
    }
}

data class MealsUiState(
    val groupedMeals: Map<String, List<Meal>>,
    val searchQuery: String,
    val allComponents: List<MealComponent>,
    val allRecipes: List<Recipe>,
    val allIngredients: List<Ingredient>
)