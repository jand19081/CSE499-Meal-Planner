package io.github.and19081.mealplanner.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.settings.SettingsRepository
import io.github.and19081.mealplanner.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid
import kotlin.math.ceil
import kotlin.math.max

class ShoppingListViewModel(
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val pantryRepository: PantryRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository
) : ViewModel() {

    private val _inCartItems = MutableStateFlow<Set<Uuid>>(emptySet())
    val inCartItems = _inCartItems.asStateFlow()

    // Helper data class to group static/repo data
    data class CoreData(
        val recipes: List<Recipe>,
        val allIngredients: List<Ingredient>,
        val allStores: List<Store>,
        val allPackages: List<Package>,
        val allBridges: List<BridgeConversion>,
        val allUnits: List<UnitModel>,
        val taxRate: Double
    )

    // Helper data class to group user/dynamic data
    data class UserData(
        val entries: List<ScheduledMeal>,
        val meals: List<PrePlannedMeal>,
        val overrides: Map<Uuid, ShoppingListOverride>,
        val pantryItems: List<PantryItem>,
        val customItems: List<ShoppingListItem>
    )

    // Helper for intermediate calculation result
    data class CalculatedList(
        val sections: List<ShoppingListSection>,
        val ownedItems: List<ShoppingListItemUi>,
        val allStores: List<Store>,
        val allUnits: List<UnitModel>,
        val taxRate: Double,
        val globalWarnings: List<DataWarning> = emptyList()
    )

    private val coreDataFlow = combine(
        recipeRepository.recipes,
        ingredientRepository.ingredients,
        storeRepository.stores,
        ingredientRepository.packages,
        ingredientRepository.bridges,
        unitRepository.units,
        settingsRepository.appSettings
    ) { args: Array<Any> ->
        CoreData(
            args[0] as List<Recipe>,
            args[1] as List<Ingredient>,
            args[2] as List<Store>,
            args[3] as List<Package>,
            args[4] as List<BridgeConversion>,
            args[5] as List<UnitModel>,
            (args[6] as AppSettings).defaultTaxRatePercentage
        )
    }

    private val userDataFlow = combine(
        mealPlanRepository.entries,
        mealRepository.meals,
        shoppingListRepository.overrides,
        pantryRepository.pantryItems,
        shoppingListItemRepository.items
    ) { args: Array<Any> ->
        UserData(
            entries = args[0] as List<ScheduledMeal>,
            meals = args[1] as List<PrePlannedMeal>,
            overrides = args[2] as Map<Uuid, ShoppingListOverride>,
            pantryItems = args[3] as List<PantryItem>,
            customItems = args[4] as List<ShoppingListItem>
        )
    }

    // Heavy Calculation: Depends on everything EXCEPT cart status
    private val calculatedListFlow = combine(coreDataFlow, userDataFlow) { core, user ->
        val (recipes, allIngredients, allStores, allPackages, allBridges, allUnits, taxRate) = core
        val (entries, meals, overrides, pantryItems, customItems) = user

        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = meals.associateBy { it.id }
        val recipesMap = recipes.associateBy { it.id }
        val ingredientsMap = allIngredients.associateBy { it.id }

        // 1. Calculate Requirements (Normalized to Base Unit)
        val grossRequirements = mutableMapOf<Uuid, Double>()
        val activeWarnings = mutableListOf<DataWarning>()

        entries.forEach { entry: ScheduledMeal ->
            val meal = entry.prePlannedMealId?.let { mealsMap[it] }
            if (meal != null) {
                activeWarnings.addAll(DataQualityValidator.validateMeal(meal, recipesMap, ingredientsMap, allPackages, allBridges, allUnits))

                fun add(ingId: Uuid, qty: Double, unitId: Uuid) {
                    val unit = allUnits.find { it.id == unitId } ?: return
                    val (baseQty, _) = UnitConverter.toStandard(qty, unit, allUnits)
                    grossRequirements[ingId] = (grossRequirements[ingId] ?: 0.0) + baseQty
                }

                // Recipes
                meal.recipes.forEach { rId ->
                    val recipe = recipesMap[rId]
                    if (recipe != null) {
                        val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                        recipe.ingredients.forEach { ri ->
                             ri.ingredientId?.let { add(it, ri.quantity * scale, ri.unitId) }
                        }
                    }
                }
                
                // Independent Ingredients
                meal.independentIngredients.forEach { item ->
                    // Assuming qty is per person
                    add(item.ingredientId, item.quantity * entry.peopleCount, item.unitId)
                }
            }
        }

        // 2. Apply Pantry
        val netRequirements = mutableMapOf<Uuid, Double>()
        val fullyOwnedIngredients = mutableListOf<Uuid>()

        grossRequirements.forEach { (ingId, reqQty) ->
            val pantryItem = pantryItems.find { it.ingredientId == ingId }
            val ownedQty = if (pantryItem != null) {
                val unit = allUnits.find { it.id == pantryItem.unitId }
                if (unit != null) UnitConverter.toStandard(pantryItem.quantity, unit, allUnits).first else 0.0
            } else 0.0

            val needed = max(0.0, reqQty - ownedQty)
            if (needed > 0.001) {
                netRequirements[ingId] = needed
            } else {
                fullyOwnedIngredients.add(ingId)
            }
        }

        // 3. Build Lists
        val shoppingLists = mutableMapOf<Uuid, ShoppingListSection>()
        
        allStores.forEach { store ->
            shoppingLists[store.id] = ShoppingListSection(store.id, store.name, emptyList(), 0, 0, 0)
        }
        val anyStoreId = Uuid.parse("00000000-0000-0000-0000-000000000000")
        if (!shoppingLists.containsKey(anyStoreId)) {
            shoppingLists[anyStoreId] = ShoppingListSection(anyStoreId, "Other / Custom", emptyList(), 0, 0, 0)
        }

        netRequirements.forEach { (ingId, baseQtyNeeded) ->
            val ing = ingredientsMap[ingId] ?: return@forEach
            val override = overrides[ingId]
            
            // If explicit override isOwned is true, treat as owned (legacy support or manual override)
            if (override?.inPantry == true) {
                fullyOwnedIngredients.add(ingId)
                return@forEach
            }

            val packages = allPackages.filter { it.ingredientId == ingId }
            
            val forcedStoreId = override?.forceStoreId
            val bestOption = if (forcedStoreId != null) {
                 packages.find { it.storeId == forcedStoreId }
                     ?: packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }
            } else {
                packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }
            }

            val targetStoreId = bestOption?.storeId ?: forcedStoreId ?: anyStoreId
            
            var purchaseQty = baseQtyNeeded
            var price = 0L
            var displayUnit = "Units"

            if (bestOption != null && bestOption.quantity > 0) {
                val optUnit = allUnits.find { it.id == bestOption.unitId }
                if (optUnit != null) {
                    val (baseOptionQty, _) = UnitConverter.toStandard(bestOption.quantity, optUnit, allUnits)
                    if (baseOptionQty > 0) {
                         val packs = ceil(baseQtyNeeded / baseOptionQty).toLong()
                         purchaseQty = packs * bestOption.quantity
                         price = packs * bestOption.priceCents.toLong()
                         displayUnit = optUnit.abbreviation
                    }
                }
            } else {
                // Safe fallback for unit
                val countUnit = allUnits.find { it.type == UnitType.Count }
                if (countUnit != null) {
                    val (_, stdUnit) = UnitConverter.toStandard(baseQtyNeeded, countUnit, allUnits)
                    displayUnit = stdUnit?.abbreviation ?: "?"
                }
            }

            val item = ShoppingListItemUi(
                id = ingId,
                name = ing.name,
                quantity = purchaseQty,
                requiredQuantity = baseQtyNeeded,
                unit = displayUnit,
                priceCents = price,
                isOwned = false,
                isCustom = false,
                isInCart = false // Placeholder, filled later
            )

            val currentSection = shoppingLists[targetStoreId]!!
            shoppingLists[targetStoreId] = currentSection.copy(
                items = currentSection.items + item,
                subtotalCents = currentSection.subtotalCents + price
            )
        }

        customItems.forEach { custom ->
            if (!custom.isPurchased) {
                val unit = allUnits.find { it.id == custom.unitId }
                val item = ShoppingListItemUi(
                    id = custom.id,
                    name = custom.customName ?: "Unknown",
                    quantity = custom.neededQuantity ?: 0.0,
                    requiredQuantity = custom.neededQuantity ?: 0.0,
                    unit = unit?.abbreviation ?: "?",
                    priceCents = 0,
                    isOwned = false,
                    isCustom = true,
                    isInCart = false // Placeholder
                )
                val section = shoppingLists[anyStoreId]!!
                shoppingLists[anyStoreId] = section.copy(items = section.items + item)
            }
        }

        val ownedItemsUi = fullyOwnedIngredients.distinct().mapNotNull { id ->
            val ing = ingredientsMap[id]
            if (ing != null) {
                ShoppingListItemUi(
                    id = id,
                    name = ing.name,
                    quantity = 0.0,
                    requiredQuantity = 0.0,
                    unit = "",
                    priceCents = 0,
                    isOwned = true,
                    isCustom = false,
                    isInCart = false
                )
            } else null
        } + customItems.filter { it.isPurchased }.map { item ->
             val unit = allUnits.find { it.id == item.unitId }
             ShoppingListItemUi(
                 id = item.id,
                 name = item.customName ?: "Unknown Item",
                 quantity = item.neededQuantity ?: 0.0,
                 requiredQuantity = item.neededQuantity ?: 0.0,
                 unit = unit?.abbreviation ?: "?",
                 priceCents = 0,
                 isOwned = true,
                 isCustom = true,
                 isInCart = false
             )
        }

        val finalSections = shoppingLists.values
            .filter { it.items.isNotEmpty() }
            .map { section ->
                val tax = (section.subtotalCents * taxRate).toLong()
                section.copy(taxCents = tax, totalCents = section.subtotalCents + tax)
            }
            .sortedBy { if(it.storeName == "Other / Custom") "ZZZ" else it.storeName }

        CalculatedList(finalSections, ownedItemsUi, allStores, allUnits, taxRate, activeWarnings.distinctBy { it.message })
    }

    // Merge Calculation with Cart Status (Fast)
    val uiState = combine(calculatedListFlow, _inCartItems) { list, inCartItems ->
        // Update Sections
        val updatedSections = list.sections.map { section ->
            section.copy(
                items = section.items.map { item ->
                    item.copy(isInCart = inCartItems.contains(item.id))
                }
            )
        }
        
        // Update Owned Items
        val updatedOwned = list.ownedItems.map { item ->
            item.copy(isInCart = inCartItems.contains(item.id))
        }

        ShoppingListUiState(updatedSections, updatedOwned, list.allStores, list.allUnits, list.taxRate, list.globalWarnings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingListUiState(emptyList(), emptyList(), emptyList(), emptyList(), 0.0))


    // ... ensureNeeded ...
    private suspend fun ensureNeeded(ingredientId: Uuid) {
         val entries = mealPlanRepository.entries.value
         val meals = mealRepository.meals.value
         val recipes = recipeRepository.recipes.value
         val allUnits = unitRepository.units.value
         
         var req = 0.0
         entries.forEach { entry: ScheduledMeal ->
            val meal = entry.prePlannedMealId?.let { id -> meals.find { it.id == id } }
            if (meal != null) {
                // Recipes
                meal.recipes.forEach { rId ->
                    val recipe = recipes.find { it.id == rId }
                    if (recipe != null) {
                         val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                         recipe.ingredients.filter { it.ingredientId == ingredientId }.forEach { ri ->
                             val unit = allUnits.find { it.id == ri.unitId }
                             if (unit != null) {
                                 val (base, _) = UnitConverter.toStandard(ri.quantity * scale, unit, allUnits)
                                 req += base
                             }
                         }
                    }
                }
                // Independent
                meal.independentIngredients.filter { it.ingredientId == ingredientId }.forEach { item ->
                    val unit = allUnits.find { it.id == item.unitId }
                    if (unit != null) {
                        val (base, _) = UnitConverter.toStandard(item.quantity * entry.peopleCount, unit, allUnits)
                        req += base
                    }
                }
            }
         }
         
         val currentPantryItem = pantryRepository.pantryItems.value.find { it.ingredientId == ingredientId }
         if (currentPantryItem != null) {
             val unit = allUnits.find { it.id == currentPantryItem.unitId }
             if (unit != null) {
                 val (currentBase, currentUnit) = UnitConverter.toStandard(currentPantryItem.quantity, unit, allUnits)
                 val newBase = max(0.0, currentBase - req)
                 if (currentUnit != null) {
                     pantryRepository.updateQuantity(ingredientId, newBase, currentUnit.id)
                 }
             }
         }
    }

    fun toggleCart(id: Uuid) {
        _inCartItems.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog = _showReceiptDialog.asStateFlow()

    private val _showDiscrepancyDialog = MutableStateFlow(false)
    val showDiscrepancyDialog = _showDiscrepancyDialog.asStateFlow()

        private val _pendingActualTotal = MutableStateFlow<Long?>(null)
        val pendingActualTotal = _pendingActualTotal.asStateFlow()
    
        private var _pendingTime: LocalTime? = null
        private var _pendingStoreId: Uuid? = null
    fun openReceiptDialog(storeId: Uuid? = null) {
        _pendingStoreId = storeId
        _showReceiptDialog.value = true
    }

    fun dismissReceiptDialog() {
        _showReceiptDialog.value = false
        _pendingStoreId = null
    }

    fun submitReceiptTotal(actualTotalCents: Long, time: LocalTime?, forcePriceReview: Boolean = false) {
        _showReceiptDialog.value = false
        _pendingTime = time
        
        val currentState = uiState.value
        val cartItems = if (_pendingStoreId == null) {
            currentState.sections.flatMap { it.items }.filter { it.isInCart }
        } else {
            currentState.sections.find { it.storeId == _pendingStoreId }?.items?.filter { it.isInCart } ?: emptyList()
        }
        
        val projectedTotal = cartItems.sumOf { it.priceCents }
        // Add tax to projected if applicable
        val tax = (projectedTotal * currentState.taxRate).toLong()
        val projectedWithTax = projectedTotal + tax

        val isDifferent = actualTotalCents != projectedWithTax
        
        if (forcePriceReview || isDifferent) { 
            _pendingActualTotal.value = actualTotalCents
            _showDiscrepancyDialog.value = true
        } else {
            finalizeTrip(actualTotalCents, _pendingTime, _pendingStoreId)
        }
    }

    fun updatePricesAndFinalize(updates: List<PriceUpdate>) {
        updates.forEach { update ->
            updatePrice(update.ingredientId, update.newPriceCents)
        }
        
        _showDiscrepancyDialog.value = false
        _pendingActualTotal.value?.let { finalizeTrip(it, _pendingTime, _pendingStoreId) }
        _pendingActualTotal.value = null
        _pendingTime = null
        _pendingStoreId = null
    }

    fun skipPriceUpdate() {
        _showDiscrepancyDialog.value = false
        _pendingActualTotal.value?.let { finalizeTrip(it, _pendingTime, _pendingStoreId) }
        _pendingActualTotal.value = null
        _pendingTime = null
        _pendingStoreId = null
    }

    fun finalizeTrip(totalPaidCents: Long, time: LocalTime?, storeId: Uuid? = null) {
        viewModelScope.launch {
            val currentState = uiState.value
            val allUnits = currentState.allUnits
            
            val finalTime = time ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

            // 1. Capture Trip Data
            val cartItems = if (storeId == null) {
                currentState.sections.flatMap { it.items }.filter { it.isInCart }
            } else {
                currentState.sections.find { it.storeId == storeId }?.items?.filter { it.isInCart } ?: emptyList()
            }
            
            if (cartItems.isEmpty()) return@launch

            val dominantStore = if (storeId != null) {
                currentState.sections.find { it.storeId == storeId }?.storeName ?: "Unknown Store"
            } else {
                currentState.sections.maxByOrNull { it.items.count { i -> i.isInCart } }?.storeName ?: "Mixed Stores"
            }
            
            val dominantStoreId = if (storeId != null) storeId else {
                 currentState.sections.maxByOrNull { it.items.count { i -> i.isInCart } }?.storeId ?: Uuid.random()
            }
            
            val subtotalCents = cartItems.sumOf { it.priceCents }
            val taxCents = max(0L, totalPaidCents - subtotalCents)

            val trip = ReceiptHistory(
                date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                time = finalTime,
                storeId = dominantStoreId,
                projectedTotalCents = subtotalCents.toInt(),
                actualTotalCents = totalPaidCents.toInt(),
                taxPaidCents = taxCents.toInt()
            )
            receiptHistoryRepository.addTrip(trip)

            // 2. Process Ingredients in Cart
            cartItems.filter { !it.isCustom }.forEach { item ->
                val unit = allUnits.find { it.abbreviation == item.unit }
                if (unit != null) {
                    val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit, allUnits)
                    
                    val currentPantryItem = pantryRepository.pantryItems.value.find { it.ingredientId == item.id }
                    val currentBaseQty = if (currentPantryItem != null) {
                        val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
                        if (pUnit != null) UnitConverter.toStandard(currentPantryItem.quantity, pUnit, allUnits).first else 0.0
                    } else 0.0
                    
                    val newQty = currentBaseQty + baseQtyToAdd
                    val (_, stdUnit) = UnitConverter.toStandard(0.0, unit, allUnits)
                    if (stdUnit != null) {
                        pantryRepository.updateQuantity(item.id, newQty, stdUnit.id)
                    }
                }
            }
            
            // 3. Process Custom Items in Cart
            cartItems.filter { it.isCustom }.forEach { item ->
                shoppingListItemRepository.toggleItem(item.id) // Marks as purchased
            }
            
            // 4. Clear Cart
            _inCartItems.value = emptySet()
        }
    }
    
    fun updatePrice(ingredientId: Uuid, newPriceCents: Int) {
        viewModelScope.launch {
            // Update the purchase option for this ingredient
            val packages = ingredientRepository.packages.value
            val pkg = packages.filter { it.ingredientId == ingredientId }.minByOrNull { it.priceCents }
            
            if (pkg != null) {
                ingredientRepository.updatePackage(pkg.copy(priceCents = newPriceCents))
            }
        }
    }

    fun markOwned(item: ShoppingListItemUi) {
        viewModelScope.launch {
            if (item.isCustom) {
                shoppingListItemRepository.toggleItem(item.id)
            } else {
                val allUnits = unitRepository.units.value
                val unit = allUnits.find { it.abbreviation == item.unit }
                if (unit != null) {
                    val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit, allUnits)
                    
                    val currentPantryItem = pantryRepository.pantryItems.value.find { it.ingredientId == item.id }
                    val currentBaseQty = if (currentPantryItem != null) {
                        val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
                        if (pUnit != null) UnitConverter.toStandard(currentPantryItem.quantity, pUnit, allUnits).first else 0.0
                    } else 0.0
                    
                    val newQty = currentBaseQty + baseQtyToAdd
                    
                    val (_, stdUnit) = UnitConverter.toStandard(0.0, unit, allUnits)
                    if (stdUnit != null) {
                        pantryRepository.updateQuantity(item.id, newQty, stdUnit.id)
                    }
                }
            }
        }
    }

    fun markUnowned(item: ShoppingListItemUi) {
        viewModelScope.launch {
            if (item.isCustom) {
                shoppingListItemRepository.toggleItem(item.id)
            } else {
                ensureNeeded(item.id)
            }
        }
    }

    fun moveToStore(ingredientId: Uuid, storeId: Uuid) {
        shoppingListRepository.setStoreOverride(ingredientId, storeId)
        viewModelScope.launch {
            ensureNeeded(ingredientId)
        }
    }

    fun addCustomItem(name: String, qty: Double, unitId: Uuid) {
        viewModelScope.launch {
            shoppingListItemRepository.addItem(
                ShoppingListItem(
                    customName = name, 
                    neededQuantity = qty, 
                    unitId = unitId,
                    storeId = Uuid.parse("00000000-0000-0000-0000-000000000000"), // Default Any Store
                    isPurchased = false
                )
            )
        }
    }
}

data class ShoppingListUiState(
    val sections: List<ShoppingListSection>,
    val ownedItems: List<ShoppingListItemUi>,
    val allStores: List<Store>,
    val allUnits: List<UnitModel>,
    val taxRate: Double = 0.0,
    val warnings: List<DataWarning> = emptyList()
)

data class ShoppingListSection(
    val storeId: Uuid,
    val storeName: String,
    val items: List<ShoppingListItemUi>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val warnings: List<DataWarning> = emptyList()
)

data class ShoppingListItemUi(
    val id: Uuid, 
    val name: String,
    val quantity: Double, 
    val requiredQuantity: Double, 
    val unit: String,
    val priceCents: Long,
    val isOwned: Boolean,
    val isCustom: Boolean,
    val isInCart: Boolean = false
)
