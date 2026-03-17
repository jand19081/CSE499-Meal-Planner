package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.*
import io.github.and19081.mealplanner.kitchen.*
import kotlin.math.max
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MealsUiState(
    val groupedMeals: Map<String, List<FoodItem>>,
    val searchQuery: String,
    val allItems: List<FoodItem>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val errorMessage: String? = null,
    val mealWarnings: Map<Uuid, List<DataWarning>> = emptyMap(),
)

class MealsViewModel(
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  private val _sortByAlpha = MutableStateFlow(true)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState =
      combine(
              foodItemRepository.foodItems,
              foodItemRepository.packages,
              foodItemRepository.conversions,
              unitRepository.units,
              _searchQuery,
              _sortByAlpha,
              _errorMessage,
          ) { args: Array<Any?> ->
            val allItems = args[0] as List<FoodItem>
            val packages = args[1] as List<Package>
            val bridges = args[2] as List<BridgeConversion>
            val units = args[3] as List<UnitModel>
            val query = args[4] as String
            val isAlpha = args[5] as Boolean
            val error = args[6] as String?
            
            val allMeals = allItems.filter { it.isRecipe }
            val itemsById = allItems.associateBy { it.id }

            val warningsMap =
                allMeals.associate { meal ->
                  meal.id to
                      DataQualityValidator.validateFoodItem(
                          meal,
                          itemsById,
                          packages,
                          bridges,
                          units,
                      )
                }

            val filtered =
                if (query.isBlank()) allMeals
                else {
                  allMeals.filter { it.name.contains(query, ignoreCase = true) }
                }

            val sorted =
                if (isAlpha) filtered.sortedBy { it.name }
                else filtered.sortedByDescending { it.name }

            val grouped = mapOf("All Meals" to sorted)

            MealsUiState(
                groupedMeals = grouped,
                searchQuery = query,
                allItems = allItems,
                allPackages = packages,
                allBridges = bridges,
                allUnits = units,
                errorMessage = error,
                mealWarnings = warningsMap,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              MealsUiState(emptyMap(), "", emptyList(), emptyList(), emptyList(), emptyList(), null, emptyMap()),
          )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
    _errorMessage.value = null
  }

  fun clearError() {
    _errorMessage.value = null
  }

  fun saveMeal(meal: FoodItem) {
    val nameVal = Validators.validateMealName(meal.name)
    if (nameVal.isFailure) {
      _errorMessage.value = nameVal.exceptionOrNull()?.message
      return
    }

    _errorMessage.value = null
    viewModelScope.launch { 
        foodItemRepository.saveFoodItem(
            meal,
            instructions = meal.recipeInfo?.instructions ?: emptyList(),
            requirementGroups = listOf(
                FoodItemRequirementGroup(
                    requirements = meal.recipeInfo?.requirements ?: emptyList()
                )
            )
        ) 
    }
  }

  fun deleteMeal(meal: FoodItem) {
    viewModelScope.launch { foodItemRepository.deleteFoodItem(meal.id) }
  }

    fun createMakeTransaction(
        meal: FoodItem,
        multiplier: Double = 1.0,
        yieldFoodItemId: Uuid? = null,
        yieldQuantity: Double? = null,
        yieldUnitId: Uuid? = null
    ): KitchenTransaction {
        val changes = mutableListOf<InventoryChange>()
        val allUnits = uiState.value.allUnits
        val itemsMap = uiState.value.allItems.associateBy { it.id }
        val recipeInfo = meal.recipeInfo ?: return KitchenTransaction(type = TransactionType.Production, title = "Error", changes = emptyList())

        fun addChange(itemId: Uuid, qty: Double, unitId: Uuid?) {
            val item = itemsMap[itemId]
            if (item != null && !item.isRecipe) {
                val unit = allUnits.find { it.id == unitId }
                changes.add(
                    InventoryChange(
                        foodItemId = itemId,
                        ingredientName = item.name,
                        quantity = qty,
                        unitId = unitId ?: item.preferredUnitId,
                        unitAbbreviation = unit?.abbreviation ?: "?",
                        direction = TransactionDirection.OUT
                    )
                )
            }
        }

        recipeInfo.requirements.forEach { req ->
            addChange(req.foodItemId, req.quantity * multiplier, req.unitId)
        }

        if (yieldFoodItemId != null && yieldQuantity != null) {
            val producedItem = itemsMap[yieldFoodItemId]
            if (producedItem != null) {
                val yieldUnit = allUnits.find { it.id == yieldUnitId }
                changes.add(
                    InventoryChange(
                        foodItemId = yieldFoodItemId,
                        ingredientName = producedItem.name,
                        quantity = yieldQuantity,
                        unitId = yieldUnit?.id,
                        unitAbbreviation = yieldUnit?.abbreviation ?: "each",
                        direction = TransactionDirection.IN
                    )
                )
            }
        }

        return KitchenTransaction(
            type = TransactionType.Production,
            title = "Making: ${meal.name}",
            changes = changes
        )
    }

  fun commitTransaction(transaction: KitchenTransaction) {
    viewModelScope.launch {
      val allUnits = uiState.value.allUnits
      val pantryItems = pantryRepository.pantryItems.value

      transaction.changes.forEach { change ->
        val currentPantryItem = pantryItems.find { it.foodItemId == change.foodItemId }
        val currentQty = currentPantryItem?.quantity ?: 0.0
        val currentUnitId = currentPantryItem?.unitId ?: change.unitId

        if (currentUnitId != null) {
            val convertedChangeQty = UnitConverter.convert(
                amount = change.quantity ?: 0.0,
                fromUnitId = change.unitId ?: Uuid.NIL,
                toUnitId = currentUnitId,
                allUnits = allUnits.associateBy { it.id }
            ) ?: 0.0

            val newQty = if (change.direction == TransactionDirection.IN) {
                currentQty + convertedChangeQty
            } else {
                max(0.0, currentQty - convertedChangeQty)
            }

            pantryRepository.updateQuantity(change.foodItemId, newQty, currentUnitId)
        }
      }
    }
  }
}


