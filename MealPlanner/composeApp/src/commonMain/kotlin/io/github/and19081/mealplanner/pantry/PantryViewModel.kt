package io.github.and19081.mealplanner.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitRepository
import io.github.and19081.mealplanner.domain.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PantryViewModel(
    private val pantryRepository: PantryRepository,
    private val foodItemRepository: FoodItemRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")

  val uiState =
      combine(
              pantryRepository.pantryItems,
              foodItemRepository.foodItems,
              foodItemRepository.categories,
              unitRepository.units,
              _searchQuery,
              foodItemRepository.conversions,
          ) { args: Array<Any?> ->
            val pantryItems = args[0] as List<PantryItem>
            val allItems = args[1] as List<FoodItem>
            val allCategories = args[2] as List<Category>
            val allUnits = args[3] as List<UnitModel>
            val query = args[4] as String
            val bridges = args[5] as List<BridgeConversion>

            val itemsMap = allItems.associateBy { it.id }
            val categoryMap = allCategories.associateBy { it.id }
            val unitMap = allUnits.associateBy { it.id }

            val joinedPantry =
                pantryItems.mapNotNull { item ->
                  val foodItem = itemsMap[item.foodItemId]
                  val unit = unitMap[item.unitId]
                  if (foodItem != null && unit != null) {
                    val catId = foodItem.purchasableInfo?.categoryId
                    val catName = categoryMap[catId]?.name ?: "Uncategorized"

                    val displayUnit = foodItem.preferredUnitId?.let { unitMap[it] } ?: unit
                    val displayQty =
                        if (displayUnit.id != unit.id) {
                          UnitConverter.convert(
                              item.quantity,
                              unit.id,
                              displayUnit.id,
                              unitMap,
                              bridges,
                          ) ?: 0.0
                        } else {
                          item.quantity
                        }

                    PantryItemUi(
                        id = item.foodItemId,
                        batchId = item.id,
                        name = foodItem.name,
                        category = catName,
                        quantity = displayQty,
                        unit = displayUnit,
                    )
                  } else null
                }

            val joinedLeftovers =
                allItems.filter { it.isLeftover }.map { item ->
                  val info = item.leftoverInfo!!
                  LeftoverItemUi(
                      id = item.id,
                      recipeName = item.name,
                      remainingServings = info.remainingServings,
                      dateAdded = info.dateAdded,
                      expirationDate = info.expirationDate,
                  )
                }

            val filteredPantry =
                if (query.isBlank()) joinedPantry
                else {
                  joinedPantry.filter { it.name.contains(query, ignoreCase = true) }
                }

            val filteredLeftovers =
                if (query.isBlank()) joinedLeftovers
                else {
                  joinedLeftovers.filter { it.recipeName.contains(query, ignoreCase = true) }
                }

            PantryUiState(
                items = filteredPantry.sortedBy { it.name },
                leftovers = filteredLeftovers.sortedByDescending { it.dateAdded },
                allIngredients = allItems.filter { it.isIngredient }.sortedBy { it.name },
                allUnits = allUnits,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              PantryUiState(emptyList(), emptyList(), emptyList(), emptyList()),
          )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun updateQuantity(
      ingredientId: Uuid,
      newAmount: Double,
      unitId: Uuid,
  ) {
    viewModelScope.launch {
      pantryRepository.updateQuantity(ingredientId, newAmount, unitId)
    }
  }

  fun deleteItem(batchId: Uuid) {
    viewModelScope.launch { pantryRepository.removeBatch(batchId) }
  }

  fun updateLeftoverQuantity(id: Uuid, servings: Double) {
    viewModelScope.launch {
      val existing = foodItemRepository.getFoodItem(id) ?: return@launch
      val info = existing.leftoverInfo ?: return@launch
      
      val updated = existing.copy(
          leftoverInfo = info.copy(remainingServings = servings)
      )
      
      if (servings <= 0) {
          foodItemRepository.deleteFoodItem(id)
      } else {
          foodItemRepository.saveFoodItem(updated)
      }
    }
  }

  fun deleteLeftover(id: Uuid) {
    viewModelScope.launch { foodItemRepository.deleteFoodItem(id) }
  }
}

data class PantryUiState(
    val items: List<PantryItemUi>,
    val leftovers: List<LeftoverItemUi>,
    val allIngredients: List<FoodItem>,
    val allUnits: List<UnitModel>,
)

data class PantryItemUi(
    val id: Uuid,
    val batchId: Uuid,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: UnitModel,
)

data class LeftoverItemUi(
    val id: Uuid,
    val recipeName: String,
    val remainingServings: Double,
    val dateAdded: String,
    val expirationDate: String? = null,
)
