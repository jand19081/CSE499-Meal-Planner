package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class IngredientsUiState(
    val groupedIngredients: Map<String, List<Ingredient>>, // Key is Category Name
    val isSortByCategory: Boolean,
    val searchQuery: String,
    val allStores: List<Store>,
    val allCategories: List<Category>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val doesExactMatchExist: Boolean = false,
    val errorMessage: String? = null
)

class IngredientsViewModel(
    private val ingredientRepository: IngredientRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository
) : ViewModel() {

    // --- State ---
    private val _searchQuery = MutableStateFlow("")
    private val _sortByCategory = MutableStateFlow(true) // false = A-Z
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        _searchQuery,
        _sortByCategory,
        _errorMessage,
        ingredientRepository.ingredients,
        ingredientRepository.categories,
        ingredientRepository.packages,
        ingredientRepository.bridges,
        storeRepository.stores,
        unitRepository.units
    ) { args: Array<Any?> ->
        val query = args[0] as String
        val isGrouped = args[1] as Boolean
        val error = args[2] as? String
        val allIngredients = args[3] as List<Ingredient>
        val allCategories = args[4] as List<Category>
        val allPackages = args[5] as List<Package>
        val allBridges = args[6] as List<BridgeConversion>
        val allStores = args[7] as List<Store>
        val allUnits = args[8] as List<UnitModel>

        // 1. Filter
        val filtered = if (query.isBlank()) allIngredients else {
            allIngredients.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Sort & Group
        val grouped: Map<String, List<Ingredient>> = if (isGrouped) {
            val catMap = allCategories.associateBy { it.id }
            filtered.groupBy { catMap[it.categoryId]?.name ?: "Uncategorized" }.toSortedMap()
        } else {
            // Group by First Letter for A-Z
            filtered.sortedBy { it.name }
                .groupBy { if (it.name.isNotEmpty()) it.name.first().uppercase() else "?" }
                .toSortedMap()
        }

        // Check for exact match (case-insensitive)
        val exactMatch = allIngredients.any { it.name.equals(query, ignoreCase = true) }

        IngredientsUiState(
            groupedIngredients = grouped,
            isSortByCategory = isGrouped,
            searchQuery = query,
            allStores = allStores,
            allCategories = allCategories,
            allPackages = allPackages,
            allBridges = allBridges,
            allUnits = allUnits,
            doesExactMatchExist = exactMatch,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
        IngredientsUiState(emptyMap(), true, "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

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
        viewModelScope.launch {
            val exists = ingredientRepository.ingredients.value.any { it.id == ingredient.id }
            if (exists) {
                ingredientRepository.updateIngredient(ingredient)
            } else {
                ingredientRepository.addIngredient(ingredient)
            }
        }
    }

    fun deleteIngredient(id: Uuid) {
        viewModelScope.launch {
            ingredientRepository.removeIngredient(id)
        }
    }

    // --- Child Entities ---
    fun savePackage(pkg: Package) {
        viewModelScope.launch {
            val exists = ingredientRepository.packages.value.any { it.id == pkg.id }
            if (exists) ingredientRepository.updatePackage(pkg) else ingredientRepository.addPackage(pkg)
        }
    }
    
    fun deletePackage(id: Uuid) {
        viewModelScope.launch {
            ingredientRepository.removePackage(id)
        }
    }
    
    fun saveBridge(bridge: BridgeConversion) {
        viewModelScope.launch {
            val currentBridges = ingredientRepository.bridges.value
            val existing = currentBridges.find { 
                it.ingredientId == bridge.ingredientId && 
                ((it.fromUnitId == bridge.fromUnitId && it.toUnitId == bridge.toUnitId) ||
                 (it.fromUnitId == bridge.toUnitId && it.toUnitId == bridge.fromUnitId))
            }

            if (existing != null) {
                val newList = currentBridges.filter { it.id != existing.id } + bridge.copy(id = existing.id)
                ingredientRepository.setBridges(newList)
            } else {
                ingredientRepository.addBridge(bridge)
            }
        }
    }

    // --- Store Management ---

    fun addStore(name: String) {
        viewModelScope.launch {
            if (storeRepository.stores.value.none { it.name.equals(name, ignoreCase = true) }) {
                storeRepository.addStore(Store(name = name))
            }
        }
    }

    fun deleteStore(storeId: Uuid) {
        viewModelScope.launch {
            storeRepository.deleteStore(storeId)
            ingredientRepository.removePackagesForStore(storeId)
        }
    }

    // --- Category Management ---
    
    fun addCategory(name: String) {
        viewModelScope.launch {
            if (ingredientRepository.categories.value.none { it.name.equals(name, ignoreCase = true) }) {
                ingredientRepository.addCategory(Category(name = name))
            }
        }
    }

    fun deleteCategory(categoryId: Uuid) {
        viewModelScope.launch {
            val toDelete = ingredientRepository.ingredients.value.filter { it.categoryId == categoryId }
            toDelete.forEach { 
                ingredientRepository.removeIngredient(it.id)
            }
        }
    }
}
