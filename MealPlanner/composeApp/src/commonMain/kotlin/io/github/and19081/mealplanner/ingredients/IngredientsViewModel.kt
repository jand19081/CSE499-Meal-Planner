package io.github.and19081.mealplanner.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.*
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IngredientsUiState(
    val groupedIngredients: Map<String, List<FoodItem>>,
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
    private val foodItemRepository: FoodItemRepository,
    private val storeRepository: io.github.and19081.mealplanner.ingredients.StoreRepository,
    private val unitRepository: UnitRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  private val _sortByCategory = MutableStateFlow(true)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState =
      combine(
              _searchQuery,
              _sortByCategory,
              _errorMessage,
              foodItemRepository.foodItems,
              foodItemRepository.categories,
              foodItemRepository.packages,
              foodItemRepository.conversions,
              storeRepository.stores,
              unitRepository.units,
          ) { args: Array<Any?> ->
            val query = args[0] as String
            val isGrouped = args[1] as Boolean
            val error = args[2] as String?
            val allItems = args[3] as List<FoodItem>
            val allCategories = args[4] as List<Category>
            val allPackages = args[5] as List<Package>
            val allBridges = args[6] as List<BridgeConversion>
            val allStores = args[7] as List<Store>
            val allUnits = args[8] as List<UnitModel>
            
            val allIngredients = allItems.filter { it.isIngredient }
            
            val filtered =
                if (query.isBlank()) allIngredients
                else {
                  allIngredients.filter { it.name.contains(query, ignoreCase = true) }
                }

            val grouped: Map<String, List<FoodItem>> =
                if (isGrouped) {
                  val catMap = allCategories.associateBy { it.id }
                  filtered
                      .sortedBy { it.name }
                      .groupBy { item ->
                          val catId = item.purchasableInfo?.categoryId
                          catMap[catId]?.name ?: "Uncategorized"
                      }
                      .toSortedMap()
                } else {
                  filtered
                      .sortedBy { it.name }
                      .groupBy { if (it.name.isNotEmpty()) it.name.first().uppercase() else "?" }
                      .toSortedMap()
                }

            IngredientsUiState(
                groupedIngredients = grouped,
                isSortByCategory = isGrouped,
                searchQuery = query,
                allStores = allStores,
                allCategories = allCategories,
                allPackages = allPackages,
                allBridges = allBridges,
                allUnits = allUnits,
                doesExactMatchExist = allIngredients.any { it.name.equals(query, ignoreCase = true) },
                errorMessage = error,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              IngredientsUiState(emptyMap(), true, "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
          )

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

  fun saveIngredient(
      ingredient: FoodItem,
      packages: List<Package>,
      bridges: List<BridgeConversion>,
  ) {
    val validation = Validators.validateIngredientName(ingredient.name)
    if (validation.isFailure) {
      _errorMessage.value = validation.exceptionOrNull()?.message
      return
    }

    _errorMessage.value = null
    viewModelScope.launch {
      foodItemRepository.saveFoodItem(ingredient)
      packages.forEach { foodItemRepository.savePackage(it) }
      bridges.forEach { foodItemRepository.saveConversion(it) }
    }
  }

  fun deleteIngredient(id: Uuid) {
    viewModelScope.launch { foodItemRepository.deleteFoodItem(id) }
  }

  fun deletePackage(id: Uuid) {
    viewModelScope.launch { foodItemRepository.deletePackage(id) }
  }

  fun deleteBridge(id: Uuid) {
    viewModelScope.launch { foodItemRepository.deleteConversion(id) }
  }

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
      foodItemRepository.packages.value
          .filter { it.storeId == storeId }
          .forEach { foodItemRepository.deletePackage(it.id) }
    }
  }

  fun addCategory(name: String) {
    viewModelScope.launch {
      if (foodItemRepository.categories.value.none { it.name.equals(name, ignoreCase = true) }) {
        foodItemRepository.saveCategory(Category(name = name))
      }
    }
  }

  fun deleteCategory(categoryId: Uuid) {
    viewModelScope.launch {
        foodItemRepository.deleteCategory(categoryId)
    }
  }

  fun addIngredientToShoppingList(ingredient: FoodItem, quantity: Double, unitId: Uuid) {
    viewModelScope.launch {
        shoppingListItemRepository.addItem(
            ShoppingListItem(
                foodItemId = ingredient.id,
                neededQuantity = quantity,
                unitId = unitId,
                storeId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
                isPurchased = false,
                isPantryItem = true
            )
        )
    }
  }
}
