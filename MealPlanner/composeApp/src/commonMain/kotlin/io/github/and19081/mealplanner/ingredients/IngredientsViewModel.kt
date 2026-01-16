package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.MealPlannerRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class IngredientsViewModel : ViewModel() {

    // --- State ---
    private val _searchQuery = MutableStateFlow("")
    private val _sortByCategory = MutableStateFlow(true) // false = A-Z
    private val _repoTrigger = MutableStateFlow(0) // increment to force refresh

    val uiState = combine(
        _searchQuery,
        _sortByCategory,
        _repoTrigger
    ) { query, isGrouped, _ ->
        val allIngredients = MealPlannerRepository.ingredients

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
            allStores = MealPlannerRepository.stores.toList(),
            // Derive unique categories from existing ingredients + defaults
            allCategories = (MealPlannerRepository.ingredients.map { it.category } + "Produce" + "Dairy" + "Pantry").distinct().sorted(),
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

    fun refreshData() {
        _repoTrigger.update { it + 1 }
    }

    // --- CRUD ---

    /**
     * Saves an ingredient (New or Existing).
     * Now accepts the full object since the Dialog constructs it.
     */
    fun saveIngredient(ingredient: Ingredient) {
        // Remove old if exists
        MealPlannerRepository.ingredients.removeAll { it.id == ingredient.id }
        MealPlannerRepository.ingredients.add(ingredient)
        refreshData()
    }

    // --- Store Management ---

    fun addStore(name: String) {
        if (MealPlannerRepository.stores.none { it.name.equals(name, ignoreCase = true) }) {
            MealPlannerRepository.stores.add(Store(name = name))
            refreshData()
        }
    }

    fun deleteStore(storeId: Uuid) {
        // Delete the store
        MealPlannerRepository.deleteStore(storeId)

        // Cascade Delete all PurchaseOptions in ALL ingredients that use this store
        MealPlannerRepository.ingredients.forEach { ingredient ->
            val updatedOptions = ingredient.purchaseOptions.filter { it.storeId != storeId }
            if (updatedOptions.size != ingredient.purchaseOptions.size) {  }
        }

        // Map the list to new ingredients with filtered options
        val curList = MealPlannerRepository.ingredients.toList()
        MealPlannerRepository.ingredients.clear()
        MealPlannerRepository.ingredients.addAll(curList.map { ing ->
            ing.copy(purchaseOptions = ing.purchaseOptions.filter { it.storeId != storeId })
        })

        refreshData()
    }

    // --- Category Management ---

    fun deleteCategory(categoryName: String) {
        MealPlannerRepository.ingredients.removeAll { it.category == categoryName }
        refreshData()
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