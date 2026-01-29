package io.github.and19081.mealplanner.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
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

    val uiState = combine(
        RecipeRepository.recipes,
        _searchQuery,
        _sortByAlpha,
        _filterCanMakeNow,
        IngredientRepository.ingredients,
        RecipeRepository.recipeIngredients,
        PantryRepository.pantryItems
    ) { args: Array<Any> ->
        val recipes = args[0] as List<Recipe>
        val query = args[1] as String
        val isAlpha = args[2] as Boolean
        val canMakeNow = args[3] as Boolean
        val ingredients = args[4] as List<Ingredient>
        val recipeIngredients = args[5] as List<RecipeIngredient>
        val pantry = args[6] as List<PantryItem>

        // Pre-calculate Maps for O(1) lookups
        val pantryMap = pantry.associateBy { it.ingredientId }
        val recipeIngredientsMap = recipeIngredients.groupBy { it.recipeId }

        // 1. Filter by Search Query
        var filtered = if (query.isBlank()) recipes else {
            recipes.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Filter by "Can Make Now"
        if (canMakeNow) {
            filtered = filtered.filter { recipe ->
                val needed = recipeIngredientsMap[recipe.id] ?: emptyList()
                needed.all { ri ->
                    val pantryItem = pantryMap[ri.ingredientId]
                    if (pantryItem == null) false
                    else {
                        // Check quantity
                        val (pantryBase, _) = UnitConverter.toStandard(pantryItem.quantityOnHand.amount, pantryItem.quantityOnHand.unit)
                        val (riBase, _) = UnitConverter.toStandard(ri.quantity.amount, ri.quantity.unit)
                        pantryBase >= riBase
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
            allRecipeIngredients = recipeIngredients,
            isCanMakeNowFilterActive = canMakeNow
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipesUiState(emptyMap(), "", false, emptyList(), emptyList(), false))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleCanMakeNowFilter() {
        _filterCanMakeNow.update { !it }
    }

    fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        RecipeRepository.upsertRecipe(recipe)
        
        RecipeRepository.removeRecipeIngredients(recipe.id)
        ingredients.forEach { 
            RecipeRepository.addRecipeIngredient(it)
        }
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
    val allRecipeIngredients: List<RecipeIngredient>,
    val isCanMakeNowFilterActive: Boolean
)