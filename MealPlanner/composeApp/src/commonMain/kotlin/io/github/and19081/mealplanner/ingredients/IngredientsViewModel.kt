package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
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
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        _searchQuery,
        _sortByCategory,
        _errorMessage,
        IngredientRepository.ingredients,
        StoreRepository.stores
    ) { query, isGrouped, error, allIngredients, allStores ->

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
            doesExactMatchExist = exactMatch,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IngredientsUiState(emptyMap(), true, "", emptyList(), emptyList()))

    // --- Actions ---

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun toggleSortMode() {
        _sortByCategory.update { !it }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- CRUD ---

    /**
     * Saves an ingredient (New or Existing).
     */
    fun saveIngredient(ingredient: Ingredient) {
        val validation = io.github.and19081.mealplanner.domain.Validators.validateIngredientName(ingredient.name)
        
        if (validation.isFailure) {
            _errorMessage.value = validation.exceptionOrNull()?.message
            return
        }

        _errorMessage.value = null
        val exists = IngredientRepository.ingredients.value.any { it.id == ingredient.id }
        if (exists) {
            IngredientRepository.updateIngredient(ingredient)
        } else {
            IngredientRepository.addIngredient(ingredient)
        }
    }

    fun deleteIngredient(id: Uuid) {
        IngredientRepository.removeIngredient(id)
    }

    // --- Store Management ---

    fun addStore(name: String) {
        if (StoreRepository.stores.value.none { it.name.equals(name, ignoreCase = true) }) {
            StoreRepository.addStore(Store(name = name))
        }
    }

    fun deleteStore(storeId: Uuid) {
        StoreRepository.deleteStore(storeId)
        IngredientRepository.removePurchaseOptionsForStore(storeId)
    }

    // --- Category Management ---

    fun deleteCategory(categoryName: String) {
        val toDelete = IngredientRepository.ingredients.value.filter { it.category == categoryName }
        toDelete.forEach { 
            IngredientRepository.removeIngredient(it.id)
        }
    }
}

data class IngredientsUiState(
    val groupedIngredients: Map<String, List<Ingredient>>,
    val isSortByCategory: Boolean,
    val searchQuery: String,
    val allStores: List<Store>,
    val allCategories: List<String>,
    val doesExactMatchExist: Boolean = false,
    val errorMessage: String? = null
)
