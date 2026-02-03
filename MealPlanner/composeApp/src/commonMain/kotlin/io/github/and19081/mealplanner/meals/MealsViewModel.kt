package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MealsViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        MealRepository.meals,
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        IngredientRepository.packages,
        IngredientRepository.bridges,
        UnitRepository.units,
        _searchQuery,
        _sortByAlpha,
        _errorMessage
    ) { args: Array<Any?> ->
        val meals = args[0] as List<PrePlannedMeal>
        val recipes = args[1] as List<Recipe>
        val ingredients = args[2] as List<Ingredient>
        val packages = args[3] as List<Package>
        val bridges = args[4] as List<BridgeConversion>
        val allUnits = args[5] as List<UnitModel>
        val query = args[6] as String
        val isAlpha = args[7] as Boolean
        val error = args[8] as String?

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
            allRecipes = recipes,
            allIngredients = ingredients,
            allPackages = packages,
            allBridges = bridges,
            allUnits = allUnits,
            errorMessage = error
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        MealsUiState(emptyMap(), "", emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList())
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveMeal(meal: PrePlannedMeal) {
        val nameVal = io.github.and19081.mealplanner.domain.Validators.validateMealName(meal.name)
        if (nameVal.isFailure) {
            _errorMessage.value = nameVal.exceptionOrNull()?.message
            return
        }

        _errorMessage.value = null
        MealRepository.upsertMeal(meal)
    }

    fun deleteMeal(meal: PrePlannedMeal) {
        MealRepository.removeMeal(meal.id)
    }
}

data class MealsUiState(
    val groupedMeals: Map<String, List<PrePlannedMeal>>,
    val searchQuery: String,
    val allRecipes: List<Recipe>,
    val allIngredients: List<Ingredient>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val errorMessage: String? = null
)
