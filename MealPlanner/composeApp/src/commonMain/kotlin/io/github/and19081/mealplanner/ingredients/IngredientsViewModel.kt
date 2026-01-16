package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.MealPlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class IngredientsViewModel : ViewModel() {

    // --- State ---
    private val _searchQuery = MutableStateFlow("")
    private val _sortByCategory = MutableStateFlow(true) // false = A-Z

    val uiState = combine(
        _searchQuery,
        _sortByCategory,
        MealPlannerRepository.ingredients,
        MealPlannerRepository.stores
    ) { query, isGrouped, allIngredients, allStores ->

        // 1. Filter
        val filtered = if (query.isBlank()) allIngredients else {
            allIngredients.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Sort & Group
        val grouped: Map<String, List<Ingredient>> = if (isGrouped) {
            filtered.groupBy { it.category }.toSortedMap()
        } else {
            // Group by First Letter for A-Z
            filtered.sortedBy { it.name }
                .groupBy { it.name.first().uppercase() }
                .toSortedMap()
        }

        // Check for exact match (case-insensitive)
        val exactMatch = allIngredients.any { it.name.equals(query, ignoreCase = true) }

        IngredientsUiState(
            groupedIngredients = grouped,
            isSortByCategory = isGrouped,
            searchQuery = query,
            allStores = allStores,
            // Derive unique categories from existing ingredients + defaults
            allCategories = (allIngredients.map { it.category } + "Produce" + "Dairy" + "Pantry").distinct().sorted(),
            doesExactMatchExist = exactMatch
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IngredientsUiState(emptyMap(), true, "", emptyList(), emptyList()))

    // --- Actions ---

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSortMode() {
        _sortByCategory.update { !it }
    }

    // --- CRUD ---

    /**
     * Saves an ingredient (New or Existing).
     */
    fun saveIngredient(ingredient: Ingredient) {
        val exists = MealPlannerRepository.ingredients.value.any { it.id == ingredient.id }
        if (exists) {
            MealPlannerRepository.updateIngredient(ingredient)
        } else {
            MealPlannerRepository.addIngredient(ingredient)
        }
    }

    // --- Store Management ---

    fun addStore(name: String) {
        if (MealPlannerRepository.stores.value.none { it.name.equals(name, ignoreCase = true) }) {
            MealPlannerRepository.addStore(Store(name = name))
        }
    }

    fun deleteStore(storeId: Uuid) {
        MealPlannerRepository.deleteStore(storeId)
    }

    // --- Category Management ---

    fun deleteCategory(categoryName: String) {
        val toDelete = MealPlannerRepository.ingredients.value.filter { it.category == categoryName }
        toDelete.forEach { 
            MealPlannerRepository.removeIngredient(it.id)
        }
    }
}

data class IngredientsUiState(
    val groupedIngredients: Map<String, List<Ingredient>>,
    val isSortByCategory: Boolean,
    val searchQuery: String,
    val allStores: List<Store>,
    val allCategories: List<String>,
    val doesExactMatchExist: Boolean = false
)
