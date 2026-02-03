package io.github.and19081.mealplanner.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class RecipesViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _filterCanMakeNow = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        RecipeRepository.recipes,
        _searchQuery,
        _sortByAlpha,
        _filterCanMakeNow,
        IngredientRepository.ingredients,
        PantryRepository.pantryItems,
        IngredientRepository.packages,
        IngredientRepository.bridges,
        UnitRepository.units,
        _errorMessage
    ) { args: Array<Any?> ->
        val recipes = args[0] as List<Recipe>
        val query = args[1] as String
        val isAlpha = args[2] as Boolean
        val canMakeNow = args[3] as Boolean
        val ingredients = args[4] as List<Ingredient>
        val pantry = args[5] as List<PantryItem>
        val packages = args[6] as List<Package>
        val bridges = args[7] as List<BridgeConversion>
        val allUnits = args[8] as List<UnitModel>
        val error = args[9] as String?

        // Pre-calculate Maps for O(1) lookups
        val pantryMap = pantry.associateBy { it.ingredientId }
        val ingredientsMap = ingredients.associateBy { it.id }

        // 1. Filter by Search Query
        var filtered = if (query.isBlank()) recipes else {
            recipes.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Filter by "Can Make Now"
        if (canMakeNow) {
            filtered = filtered.filter { recipe ->
                val needed = recipe.ingredients
                needed.all { ri ->
                    val pantryItem = pantryMap[ri.ingredientId]
                    if (pantryItem == null) false
                    else {
                        // Check quantity
                        val ingredientBridges = bridges.filter { it.ingredientId == ri.ingredientId }
                        
                        val pantryInRiUnit = UnitConverter.convert(
                            amount = pantryItem.quantity,
                            fromUnitId = pantryItem.unitId,
                            toUnitId = ri.unitId,
                            allUnits = allUnits,
                            bridges = ingredientBridges
                        )
                        pantryInRiUnit >= ri.quantity
                    }
                }
            }
        }

        // 3. Group
        val grouped: Map<String, List<Recipe>> = if (isAlpha) {
            filtered.sortedBy { it.name }
                .groupBy { if (it.name.isNotEmpty()) it.name.first().uppercase() else "?" }
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
            allPackages = packages,
            allBridges = bridges,
            allUnits = allUnits,
            isCanMakeNowFilterActive = canMakeNow,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
        RecipesUiState(emptyMap(), "", false, emptyList(), emptyList(), emptyList(), emptyList(), false))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun toggleCanMakeNowFilter() {
        _filterCanMakeNow.update { !it }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveRecipe(recipe: Recipe) {
        val nameVal = io.github.and19081.mealplanner.domain.Validators.validateRecipeName(recipe.name)
        if (nameVal.isFailure) {
            _errorMessage.value = nameVal.exceptionOrNull()?.message
            return
        }

        val servingsVal = io.github.and19081.mealplanner.domain.Validators.validateServings(recipe.servings)
        if (servingsVal.isFailure) {
            _errorMessage.value = servingsVal.exceptionOrNull()?.message
            return
        }

        _errorMessage.value = null
        RecipeRepository.upsertRecipe(recipe)
    }

    fun deleteRecipe(id: Uuid) {
        RecipeRepository.removeRecipe(id)
    }
}

data class RecipesUiState(
    val groupedRecipes: Map<String, List<Recipe>>,
    val searchQuery: String,
    val doesExactMatchExist: Boolean,
    val allIngredients: List<Ingredient>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val isCanMakeNowFilterActive: Boolean,
    val errorMessage: String? = null
)