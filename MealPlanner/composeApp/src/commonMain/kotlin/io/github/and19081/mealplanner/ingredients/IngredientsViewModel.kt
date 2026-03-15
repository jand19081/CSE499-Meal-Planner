package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val errorMessage: String? = null,
)

class IngredientsViewModel(
    private val ingredientRepository: IngredientRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  // --- State ---
  private val _searchQuery = MutableStateFlow("")
  private val _sortByCategory = MutableStateFlow(true) // false = A-Z
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState =
      combine(
              listOf(
                  _searchQuery,
                  _sortByCategory,
                  _errorMessage,
                  ingredientRepository.ingredients,
                  ingredientRepository.categories,
                  ingredientRepository.packages,
                  ingredientRepository.bridges,
                  storeRepository.stores,
                  unitRepository.units,
              )
          ) { array ->
            val query = array[0] as String
            val isGrouped = array[1] as Boolean
            val error = array[2] as String?
            val allIngredients = array[3] as List<Ingredient>
            val allCategories = array[4] as List<Category>
            val allPackages = array[5] as List<Package>
            val allBridges = array[6] as List<BridgeConversion>
            val allStores = array[7] as List<Store>
            val allUnits = array[8] as List<UnitModel>

            // 1. Filter
            val filtered =
                if (query.isBlank()) allIngredients
                else {
                  allIngredients.filter { it.name.contains(query, ignoreCase = true) }
                }

            // 2. Sort & Group
            val grouped: Map<String, List<Ingredient>> =
                if (isGrouped) {
                  val catMap = allCategories.associateBy { it.id }
                  filtered
                      .sortedBy { it.name }
                      .groupBy { catMap[it.categoryId]?.name ?: "Uncategorized" }
                      .toSortedMap()
                } else {
                  // Group by First Letter for A-Z
                  filtered
                      .sortedBy { it.name }
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
                errorMessage = error,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              IngredientsUiState(
                  emptyMap(),
                  true,
                  "",
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
              ),
          )

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

  /** Saves an ingredient and all its related children atomically. */
  fun saveIngredient(
      ingredient: Ingredient,
      packages: List<Package>,
      bridges: List<BridgeConversion>,
  ) {
    val validation =
        io.github.and19081.mealplanner.domain.Validators.validateIngredientName(ingredient.name)

    if (validation.isFailure) {
      _errorMessage.value = validation.exceptionOrNull()?.message
      return
    }

    _errorMessage.value = null
    viewModelScope.launch {
      ingredientRepository.upsertIngredientWithDetails(ingredient, packages, bridges)
    }
  }

  fun deleteIngredient(id: Uuid) {
    viewModelScope.launch { ingredientRepository.removeIngredient(id) }
  }

  // --- Child Entities ---
  fun deletePackage(id: Uuid) {
    viewModelScope.launch { ingredientRepository.removePackage(id) }
  }

  fun deleteBridge(id: Uuid) {
    viewModelScope.launch { ingredientRepository.removeBridge(id) }
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
      toDelete.forEach { ingredientRepository.removeIngredient(it.id) }
    }
  }
}
