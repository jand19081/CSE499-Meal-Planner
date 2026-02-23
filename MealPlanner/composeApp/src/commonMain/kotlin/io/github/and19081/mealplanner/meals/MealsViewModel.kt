package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
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
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class MealsViewModel(
    private val mealRepository: MealRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val unitRepository: UnitRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        mealRepository.meals,
        recipeRepository.recipes,
        ingredientRepository.ingredients,
        ingredientRepository.packages,
        ingredientRepository.bridges,
        unitRepository.units,
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

        // Pre-calculate Maps
        val recipesById = recipes.associateBy { it.id }
        val ingredientsById = ingredients.associateBy { it.id }

        // Validate Meals
        val warningsMap = meals.associate { meal ->
            meal.id to DataQualityValidator.validateMeal(meal, recipesById, ingredientsById, packages, bridges, allUnits)
        }

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
            errorMessage = error,
            mealWarnings = warningsMap
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        MealsUiState(emptyMap(), "", emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), null, emptyMap())
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
        viewModelScope.launch {
            mealRepository.upsertMeal(meal)
        }
    }

    fun deleteMeal(meal: PrePlannedMeal) {
        viewModelScope.launch {
            mealRepository.removeMeal(meal.id)
        }
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
    val errorMessage: String? = null,
    val mealWarnings: Map<Uuid, List<DataWarning>> = emptyMap()
)
