package io.github.and19081.mealplanner.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListItem
import io.github.and19081.mealplanner.core.util.DataQualityValidator
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.Validators
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.PurchasableInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.ShoppingListItemRepository
import io.github.and19081.mealplanner.feature.meals.PantryItem
import kotlinx.coroutines.Dispatchers
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StockWarning(
    val itemName: String,
    val totalInStock: Double,
    val requiredQuantity: Double,
    val unitAbbreviation: String,
    val isSufficient: Boolean,
    val isPartial: Boolean
)

data class RecipeDraftState(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val servingsStr: String = "4.0",
    val prepTimeStr: String = "0",
    val cookTimeStr: String = "0",
    val description: String = "",
    val mealType: RecipeMealType = RecipeMealType.Dinner,
    val requirementGroups: List<FoodItemRequirementGroup> = emptyList(),
    val instructions: List<String> = emptyList()
)

data class RecipesUiState(
    val groupedRecipes: Map<String, List<FoodItem>>,
    val allRecipes: List<FoodItem>,
    val searchQuery: String,
    val doesExactMatchExist: Boolean,
    val allItems: List<FoodItem>,
    val pantryItems: List<PantryItem>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val isSortByAlpha: Boolean,
    val isCanMakeNowFilterActive: Boolean,
    val errorMessage: String? = null,
    val recipeWarnings: Map<Uuid, List<DataWarning>> = emptyMap(),
)

class RecipesViewModel(
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortByAlpha = MutableStateFlow(true)
    private val _filterCanMakeNow = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _priceCalculator: io.github.and19081.mealplanner.domain.logic.PriceCalculator? = null

    // --- Draft State Management ---
    private val _draftState = MutableStateFlow<RecipeDraftState?>(null)
    val draftState = _draftState.asStateFlow()

    val draftStockWarnings = combine(
        _draftState.filterNotNull(),
        foodItemRepository.foodItems,
        pantryRepository.pantryItems,
        unitRepository.units,
        foodItemRepository.conversions
    ) { draft, allItems, pantryItems, allUnits, allBridges ->
        val pantryByItem = pantryItems.groupBy { it.measurement.foodItemId }

        draft.requirementGroups.flatMap { it.requirements }.map { req ->
            val item = allItems.find { it.id == req.measurement.foodItemId }
            val itemName = item?.name ?: "Unknown Item"
            val pItems = pantryByItem[req.measurement.foodItemId] ?: emptyList()
            val targetUnit = req.measurement.unitId ?: item?.preferredUnitId ?: Uuid.NIL
            val unitAbbr = allUnits.find { it.id == req.measurement.unitId }?.abbreviation ?: ""

            var totalInStock = 0.0
            for (pItem in pItems) {
                totalInStock += UnitConverter.convert(
                    amount = pItem.measurement.quantity,
                    fromUnitId = pItem.measurement.unitId ?: kotlin.uuid.Uuid.NIL,
                    toUnitId = targetUnit,
                    allUnits = allUnits.associateBy { it.id },
                    bridges = allBridges,
                ) ?: 0.0
            }

            StockWarning(
                itemName = itemName,
                totalInStock = totalInStock,
                requiredQuantity = req.measurement.quantity,
                unitAbbreviation = unitAbbr,
                isSufficient = totalInStock >= req.measurement.quantity,
                isPartial = totalInStock > 0 && totalInStock < req.measurement.quantity
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState =
        combine(
            foodItemRepository.foodItems,
            _searchQuery,
            _sortByAlpha,
            _filterCanMakeNow,
            pantryRepository.pantryItems,
            foodItemRepository.packages,
            foodItemRepository.conversions,
            unitRepository.units,
            _errorMessage,
        ) { args: Array<Any?> ->
            val allItems = args[0] as List<FoodItem>
            val query = args[1] as String
            val isAlpha = args[2] as Boolean
            val canMakeNow = args[3] as Boolean
            val pantry = args[4] as List<PantryItem>
            val packages = args[5] as List<Package>
            val bridges = args[6] as List<BridgeConversion>
            val allUnits = args[7] as List<UnitModel>
            val error = args[8] as String?

            val allRecipes = allItems.filter { it.isRecipe }
            val itemsMap = allItems.associateBy { it.id }
            val pantryMap = pantry.associateBy { it.measurement.foodItemId }

            val warningsMap =
                allRecipes.associate { recipe ->
                    recipe.id to
                            DataQualityValidator.validateFoodItem(
                                recipe,
                                itemsMap,
                                packages,
                                bridges,
                                allUnits,
                            )
                }

            var filtered =
                if (query.isBlank()) allRecipes
                else {
                    allRecipes.filter { it.name.contains(query, ignoreCase = true) }
                }

            if (canMakeNow) {
                filtered =
                    filtered.filter { recipe ->
                        val groups = recipe.recipeInfo?.requirementGroups ?: emptyList()
                        groups.all { group ->
                            group.requirements.all { req ->
                                val subItem = itemsMap[req.measurement.foodItemId] ?: return@all true
                                if (subItem.isRecipe) return@all true

                                val pantryItem = pantryMap[req.measurement.foodItemId] ?: return@all false

                                val ingredientBridges = bridges.filter { it.foodItemId == req.measurement.foodItemId }
                                val pantryInReqUnit = UnitConverter.convert(
                                    amount = pantryItem.measurement.quantity,
                                    fromUnitId = pantryItem.measurement.unitId ?: kotlin.uuid.Uuid.NIL,
                                    toUnitId = req.measurement.unitId ?: subItem.preferredUnitId ?: Uuid.NIL,
                                    allUnits = allUnits.associateBy { it.id },
                                    bridges = ingredientBridges
                                ) ?: 0.0
                                pantryInReqUnit >= req.measurement.quantity
                            }
                        }
                    }
            }

            val grouped: Map<String, List<FoodItem>> =
                if (isAlpha) {
                    filtered
                        .sortedBy { it.name }
                        .groupBy { if (it.name.isNotEmpty()) it.name.first().uppercase() else "?" }
                        .toSortedMap()
                } else {
                    filtered
                        .sortedBy { it.name }
                        .groupBy { it.recipeInfo?.mealType?.name ?: "Uncategorized" }
                        .toSortedMap()
                }

            RecipesUiState(
                groupedRecipes = grouped,
                allRecipes = allRecipes,
                searchQuery = query,
                doesExactMatchExist = allRecipes.any { it.name.equals(query, ignoreCase = true) },
                allItems = allItems,
                pantryItems = pantry,
                allPackages = packages,
                allBridges = bridges,
                allUnits = allUnits,
                isSortByAlpha = isAlpha,
                isCanMakeNowFilterActive = canMakeNow,
                errorMessage = error,
                recipeWarnings = warningsMap,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                RecipesUiState(
                    emptyMap(),
                    emptyList(),
                    "",
                    false,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    true,
                    false,
                    null,
                    emptyMap()
                ),
            )

    // --- Intents ---

    fun startEditing(recipe: FoodItem?, initialName: String = "") {
        if (recipe == null) {
            _draftState.value = RecipeDraftState(name = initialName)
        } else {
            _draftState.value = RecipeDraftState(
                id = recipe.id,
                name = recipe.name,
                servingsStr = recipe.recipeInfo?.servings?.toString() ?: "4.0",
                prepTimeStr = recipe.recipeInfo?.prepTimeMinutes?.toString() ?: "0",
                cookTimeStr = recipe.recipeInfo?.cookTimeMinutes?.toString() ?: "0",
                description = recipe.recipeInfo?.description ?: "",
                mealType = recipe.recipeInfo?.mealType ?: RecipeMealType.Dinner,
                requirementGroups = recipe.recipeInfo?.requirementGroups ?: emptyList(),
                instructions = recipe.recipeInfo?.instructions ?: emptyList()
            )
        }
    }

    fun updateDraft(update: (RecipeDraftState) -> RecipeDraftState) {
        _draftState.update { it?.let(update) }
    }

    fun clearDraft() {
        _draftState.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun toggleCanMakeNowFilter() {
        _filterCanMakeNow.update { !it }
    }

    fun setCanMakeNowFilter(active: Boolean) {
        _filterCanMakeNow.value = active
    }

    fun toggleSortMode() {
        _sortByAlpha.update { !it }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun hasCircularDependency(
        item: FoodItem,
        allItemsMap: Map<Uuid, FoodItem>,
        visited: Set<Uuid> = emptySet(),
    ): Boolean {
        val recipeInfo = item.recipeInfo ?: return false
        if (visited.contains(item.id)) return true
        val newVisited = visited + item.id
        for (group in recipeInfo.requirementGroups) {
            for (req in group.requirements) {
                val subItem = allItemsMap[req.measurement.foodItemId]
                if (subItem != null && subItem.isRecipe) {
                    if (hasCircularDependency(subItem, allItemsMap, newVisited)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun saveRecipe(recipe: FoodItem) {
        val validation = Validators.validateRecipeName(recipe.name)
        if (validation.isFailure) {
            _errorMessage.value = validation.exceptionOrNull()?.message
            return
        }

        val servings = recipe.recipeInfo?.servings ?: 1.0
        val servingsVal = Validators.validateServings(servings)
        if (servingsVal.isFailure) {
            _errorMessage.value = servingsVal.exceptionOrNull()?.message
            return
        }

        val allItemsMap = foodItemRepository.foodItems.value.associateBy { it.id }
        if (hasCircularDependency(recipe, allItemsMap)) {
            _errorMessage.value = "Circular dependency detected."
            return
        }

        _errorMessage.value = null
        viewModelScope.launch {
            foodItemRepository.saveFoodItem(
                recipe,
                instructions = recipe.recipeInfo?.instructions ?: emptyList(),
                requirementGroups = recipe.recipeInfo?.requirementGroups ?: emptyList()
            )
        }
    }

    fun deleteRecipe(id: Uuid) {
        viewModelScope.launch { foodItemRepository.deleteFoodItem(id) }
    }

    fun addIngredient(name: String) {
        viewModelScope.launch {
            val categories = foodItemRepository.categories.value
            var miscCategory = categories.find { it.name.equals("Miscellaneous", ignoreCase = true) }

            if (miscCategory == null) {
                val newCat = Category(name = "Miscellaneous")
                foodItemRepository.saveCategory(newCat)
                miscCategory = newCat
            }

            val newIngredient = FoodItem(
                name = name,
                purchasableInfo = PurchasableInfo(categoryId = miscCategory.id)
            )
            foodItemRepository.saveFoodItem(newIngredient)
        }
    }

    fun addRecipeToShoppingList(recipe: FoodItem, batches: Double = 1.0) {
        viewModelScope.launch {
            shoppingListItemRepository.addItem(
                ShoppingListItem(
                    id = Uuid.random(),
                    customName = null,
                    storeId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
                    measurement = ItemMeasurement(
                        foodItemId = recipe.id,
                        unitId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
                        quantity = batches
                    ),
                    packageId = null,
                    isPurchased = false,
                    isPantryItem = true
                )
            )
        }
    }
}
