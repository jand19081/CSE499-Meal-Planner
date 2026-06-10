package io.github.and19081.mealplanner.feature.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.util.DataQualityValidator
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.Validators
import io.github.and19081.mealplanner.domain.logic.PriceCalculator
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.model.Meal
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import io.github.and19081.mealplanner.domain.model.RecipeInfo
import io.github.and19081.mealplanner.domain.model.isMeal
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.domain.model.recipeInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.PantryUpdate
import io.github.and19081.mealplanner.feature.kitchen.InventoryChange
import io.github.and19081.mealplanner.feature.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.feature.kitchen.TransactionDirection
import io.github.and19081.mealplanner.feature.kitchen.TransactionType
import kotlin.math.max
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MealsUiState(
    val groupedMeals: Map<String, List<FoodItem>>,
    val searchQuery: String,
    val allItems: List<FoodItem>,
    val allPurchaseOptions: List<PurchaseOption>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val errorMessage: String? = null,
    val mealWarnings: Map<Uuid, List<DataWarning>> = emptyMap(),
    val mealCosts: Map<Uuid, Long?> = emptyMap(),
)

private data class CoreDataState(
    val items: List<FoodItem>,
    val purchaseOptions: List<PurchaseOption>,
    val bridges: List<BridgeConversion>,
    val units: List<UnitModel>,
)

private data class FilterState(val query: String, val isAlpha: Boolean, val error: String?)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MealsViewModel(
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Draft Form State
    private val _draftMealId = MutableStateFlow<Uuid?>(null)
    val draftName = MutableStateFlow("")
    val draftMealType = MutableStateFlow(RecipeMealType.Other)
    val draftRequirements = MutableStateFlow<List<FoodItemRequirement>>(emptyList())

    fun initializeDraft(meal: FoodItem?) {
        _draftMealId.value = meal?.id ?: Uuid.random()
        draftName.value = meal?.name ?: ""
        draftMealType.value = meal?.recipeInfo?.mealType ?: RecipeMealType.Other
        draftRequirements.value =
            meal?.recipeInfo?.requirementGroups?.flatMap { it.requirements } ?: emptyList()
    }

    fun updateDraftName(name: String) {
        draftName.value = name
    }

    fun updateDraftMealType(type: RecipeMealType) {
        draftMealType.value = type
    }

    fun updateDraftRequirements(requirements: List<FoodItemRequirement>) {
        draftRequirements.value = requirements
    }

    fun saveDraft() {
        val mealId = _draftMealId.value ?: return
        val finalMeal =
            Meal(
                id = mealId,
                name = draftName.value,
                recipeInfo =
                    RecipeInfo(
                        mealType = draftMealType.value,
                        requirementGroups =
                            listOf(FoodItemRequirementGroup(requirements = draftRequirements.value)),
                    ),
            )
        saveMeal(finalMeal)
    }

    private val coreDataFlow =
        combine(
            foodItemRepository.foodItems,
            foodItemRepository.purchaseOptions,
            foodItemRepository.conversions,
            unitRepository.units,
        ) { items, purchaseOptions, bridges, units ->
            CoreDataState(items, purchaseOptions, bridges, units)
        }

    private val filterFlow =
        combine(_searchQuery, _sortByAlpha, _errorMessage) { query, isAlpha, error ->
            FilterState(query, isAlpha, error)
        }

    private val costsFlow = coreDataFlow.flatMapLatest { data ->
        flow {
            val allMeals = data.items.filter { it.isRecipe() || it.isMeal() }
            val unitsMap = data.units.associateBy { it.id }
            val costs = allMeals.associate { meal ->
                meal.id to PriceCalculator.calculateFoodItemCost(
                    meal,
                    foodItemRepository,
                    unitsMap
                )
            }
            emit(costs)
        }
    }

    val uiState =
        combine(coreDataFlow, filterFlow, costsFlow) { data, filter, costs ->
            val allMeals = data.items.filter { it.isRecipe() || it.isMeal() }
            val itemsById = data.items.associateBy { it.id }

            val warningsMap = allMeals.associate { meal ->
                meal.id to
                        DataQualityValidator.validateFoodItem(
                            meal,
                            itemsById,
                            data.purchaseOptions,
                            data.bridges,
                            data.units,
                        )
            }

            val filtered =
                if (filter.query.isBlank()) allMeals
                else {
                    allMeals.filter { it.name.contains(filter.query, ignoreCase = true) }
                }

            val sorted =
                if (filter.isAlpha) filtered.sortedBy { it.name }
                else filtered.sortedByDescending { it.name }
            val grouped = mapOf("All Meals" to sorted)

            MealsUiState(
                groupedMeals = grouped,
                searchQuery = filter.query,
                allItems = data.items,
                allPurchaseOptions = data.purchaseOptions,
                allBridges = data.bridges,
                allUnits = data.units,
                errorMessage = filter.error,
                mealWarnings = warningsMap,
                mealCosts = costs,
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                MealsUiState(
                    emptyMap(),
                    "",
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    null,
                    emptyMap(),
                ),
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

        // Check for circular dependencies before saving
        val itemsMap = uiState.value.allItems.associateBy { it.id }
        val validationWarnings = DataQualityValidator.validateFoodItem(
            meal,
            itemsMap,
            uiState.value.allPurchaseOptions,
            uiState.value.allBridges,
            uiState.value.allUnits,
        )

        val hasCircularDependency = validationWarnings.any { it is DataWarning.CircularDependency }
        if (hasCircularDependency) {
            _errorMessage.value = "Cannot save: Circular dependency detected in recipe structure"
            return
        }

        _errorMessage.value = null
        viewModelScope.launch {
            try {
                foodItemRepository.saveFoodItem(
                    meal,
                    instructions = meal.recipeInfo?.instructions ?: emptyList(),
                    requirementGroups = meal.recipeInfo?.requirementGroups ?: emptyList(),
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save meal: ${e.message}"
            }
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
        yieldUnitId: Uuid? = null,
    ): KitchenTransaction {
        val changes = mutableListOf<InventoryChange>()
        val allUnits = uiState.value.allUnits
        val itemsMap = uiState.value.allItems.associateBy { it.id }
        val recipeInfo =
            meal.recipeInfo
                ?: return KitchenTransaction(
                    type = TransactionType.Production,
                    title = "Error",
                    changes = emptyList(),
                )

        fun addChange(itemId: Uuid, qty: Double, unitId: Uuid?) {
            val item = itemsMap[itemId]
            if (item != null && !item.isRecipe()) {
                val unit = allUnits.find { it.id == unitId }
                changes.add(
                    InventoryChange(
                        measurement =
                            ItemMeasurement(
                                foodItemId = itemId,
                                quantity = qty,
                                unitId = unitId ?: item.preferredUnitId,
                            ),
                        ingredientName = item.name,
                        unitAbbreviation = unit?.abbreviation ?: "?",
                        direction = TransactionDirection.OUT,
                    )
                )
            }
        }

        fun processRecipe(recipe: FoodItem, recipeMultiplier: Double) {
            val recipeRecipeInfo = recipe.recipeInfo ?: return
            recipeRecipeInfo.requirementGroups
                .flatMap { it.requirements }
                .forEach { req ->
                    val subItem = itemsMap[req.measurement.foodItemId]
                    if (subItem?.isRecipe() == true) {
                        // Recursively process sub-recipe
                        processRecipe(subItem, req.measurement.quantity * recipeMultiplier)
                    } else {
                        addChange(
                            req.measurement.foodItemId ?: Uuid.NIL,
                            req.measurement.quantity * recipeMultiplier,
                            req.measurement.unitId,
                        )
                    }
                }
        }

        processRecipe(meal, multiplier)

        if (yieldFoodItemId != null && yieldQuantity != null) {
            val producedItem = itemsMap[yieldFoodItemId]
            if (producedItem != null) {
                val yieldUnit = allUnits.find { it.id == yieldUnitId }
                changes.add(
                    InventoryChange(
                        measurement =
                            ItemMeasurement(
                                foodItemId = yieldFoodItemId,
                                quantity = yieldQuantity,
                                unitId = yieldUnit?.id,
                            ),
                        ingredientName = producedItem.name,
                        unitAbbreviation = yieldUnit?.abbreviation ?: "each",
                        direction = TransactionDirection.IN,
                    )
                )
            }
        }

        return KitchenTransaction(
            type = TransactionType.Production,
            title = "Making: ${meal.name}",
            changes = changes,
        )
    }

    fun commitTransaction(transaction: KitchenTransaction) {
        viewModelScope.launch {
            val allUnits = uiState.value.allUnits
            val pantryItems = pantryRepository.pantryItems.value

            val updates =
                transaction.changes.mapNotNull { change ->
                    val currentPantryItem = pantryItems.find {
                        it.measurement.foodItemId == change.measurement.foodItemId
                    }
                    val currentQty = currentPantryItem?.measurement?.quantity ?: 0.0
                    val currentUnitId = currentPantryItem?.measurement?.unitId ?: change.measurement.unitId

                    if (currentUnitId != null) {
                        val convertedChangeQty =
                            UnitConverter.convert(
                                amount = change.measurement.quantity,
                                fromUnitId = change.measurement.unitId ?: Uuid.NIL,
                                toUnitId = currentUnitId,
                                allUnits = allUnits.associateBy { it.id },
                            ) ?: 0.0

                        val newQty =
                            if (change.direction == TransactionDirection.IN) {
                                currentQty + convertedChangeQty
                            } else {
                                max(0.0, currentQty - convertedChangeQty)
                            }
                        PantryUpdate(change.measurement.foodItemId ?: Uuid.NIL, newQty, currentUnitId)
                    } else null
                }

            if (updates.isNotEmpty()) {
                pantryRepository.updateQuantities(updates)
            }
        }
    }
}
