package io.github.and19081.mealplanner.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
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
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.uuid.Uuid
import kotlin.math.ceil
import kotlin.math.max

class ShoppingListViewModel : ViewModel() {

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
        val customItems: List<ShoppingListItem>,
        val inCartItems: Set<Uuid>
    )

    private val coreDataFlow = combine(
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        StoreRepository.stores,
        IngredientRepository.packages,
        IngredientRepository.bridges,
        UnitRepository.units,
        SettingsRepository.appSettings
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
        MealPlanRepository.entries,
        MealRepository.meals,
        ShoppingListRepository.overrides,
        PantryRepository.pantryItems,
        ShoppingListItemRepository.items,
        ShoppingSessionRepository.inCartItems
    ) { args: Array<Any> ->
        UserData(
            entries = args[0] as List<ScheduledMeal>,
            meals = args[1] as List<PrePlannedMeal>,
            overrides = args[2] as Map<Uuid, ShoppingListOverride>,
            pantryItems = args[3] as List<PantryItem>,
            customItems = args[4] as List<ShoppingListItem>,
            inCartItems = args[5] as Set<Uuid>
        )
    }

    // Map the combined data to UI State
    val uiState = combine(coreDataFlow, userDataFlow) { core, user ->
        val (recipes, allIngredients, allStores, allPackages, allBridges, allUnits, taxRate) = core
        val (entries, meals, overrides, pantryItems, customItems, inCartItems) = user

        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = meals.associateBy { it.id }
        val recipesMap = recipes.associateBy { it.id }
        val ingredientsMap = allIngredients.associateBy { it.id }

        // 1. Calculate Requirements (Normalized to Base Unit)
        val grossRequirements = mutableMapOf<Uuid, Double>()

        entries.forEach { entry ->
            val meal = mealsMap[entry.prePlannedMealId]
            if (meal != null) {
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
                             add(ri.ingredientId, ri.quantity * scale, ri.unitId)
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
            if (override?.isOwned == true) {
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
                isInCart = inCartItems.contains(ingId)
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
                    isInCart = inCartItems.contains(custom.id)
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

        ShoppingListUiState(finalSections, ownedItemsUi, allStores, allUnits, taxRate)

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingListUiState(emptyList(), emptyList(), emptyList(), emptyList(), 0.0))

    // ... ensureNeeded ...
    private fun ensureNeeded(ingredientId: Uuid) {
        viewModelScope.launch {
             val entries = MealPlanRepository.entries.value
             val meals = MealRepository.meals.value
             val recipes = RecipeRepository.recipes.value
             val allUnits = UnitRepository.units.value
             
             var req = 0.0
             entries.forEach { entry ->
                val meal = meals.find { it.id == entry.prePlannedMealId }
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
             
             val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == ingredientId }
             if (currentPantryItem != null) {
                 val unit = allUnits.find { it.id == currentPantryItem.unitId }
                 if (unit != null) {
                     val (currentBase, currentUnit) = UnitConverter.toStandard(currentPantryItem.quantity, unit, allUnits)
                     val newBase = max(0.0, currentBase - req)
                     if (currentUnit != null) {
                         PantryRepository.updateQuantity(ingredientId, newBase, currentUnit.id)
                     }
                 }
             }
        }
    }

    fun toggleCart(id: Uuid) {
        ShoppingSessionRepository.toggleCartStatus(id)
    }

    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog = _showReceiptDialog.asStateFlow()

    private val _showDiscrepancyDialog = MutableStateFlow(false)
    val showDiscrepancyDialog = _showDiscrepancyDialog.asStateFlow()

    private val _pendingActualTotal = MutableStateFlow<Long?>(null)
    val pendingActualTotal = _pendingActualTotal.asStateFlow()
    
    private var _pendingStoreId: Uuid? = null

    fun openReceiptDialog(storeId: Uuid? = null) {
        _pendingStoreId = storeId
        _showReceiptDialog.value = true
    }

    fun dismissReceiptDialog() {
        _showReceiptDialog.value = false
        _pendingStoreId = null
    }

    fun submitReceiptTotal(actualTotalCents: Long) {
        _showReceiptDialog.value = false
        
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

        val difference = kotlin.math.abs(actualTotalCents - projectedWithTax)
        val percentDiff = if (projectedWithTax > 0) (difference.toDouble() / projectedWithTax.toDouble()) * 100 else 0.0
        
        if (percentDiff > 5.0) {
            _pendingActualTotal.value = actualTotalCents
            _showDiscrepancyDialog.value = true
        } else {
            finalizeTrip(actualTotalCents, _pendingStoreId)
        }
    }

    fun updatePricesAndFinalize(updates: List<PriceUpdate>) {
        updates.forEach { update ->
            updatePrice(update.ingredientId, update.newPriceCents)
        }
        
        _showDiscrepancyDialog.value = false
        _pendingActualTotal.value?.let { finalizeTrip(it, _pendingStoreId) }
        _pendingActualTotal.value = null
        _pendingStoreId = null
    }

    fun skipPriceUpdate() {
        _showDiscrepancyDialog.value = false
        _pendingActualTotal.value?.let { finalizeTrip(it, _pendingStoreId) }
        _pendingActualTotal.value = null
        _pendingStoreId = null
    }

    fun finalizeTrip(totalPaidCents: Long, storeId: Uuid? = null) {
        viewModelScope.launch {
            val currentState = uiState.value
            val allUnits = currentState.allUnits
            
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
                storeId = dominantStoreId,
                projectedTotalCents = subtotalCents.toInt(),
                actualTotalCents = totalPaidCents.toInt(),
                taxPaidCents = taxCents.toInt()
            )
            ReceiptHistoryRepository.addTrip(trip)

            // 2. Process Ingredients in Cart
            cartItems.filter { !it.isCustom }.forEach { item ->
                val unit = allUnits.find { it.abbreviation == item.unit }
                if (unit != null) {
                    val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit, allUnits)
                    
                    val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == item.id }
                    val currentBaseQty = if (currentPantryItem != null) {
                        val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
                        if (pUnit != null) UnitConverter.toStandard(currentPantryItem.quantity, pUnit, allUnits).first else 0.0
                    } else 0.0
                    
                    val newQty = currentBaseQty + baseQtyToAdd
                    val (_, stdUnit) = UnitConverter.toStandard(0.0, unit, allUnits)
                    if (stdUnit != null) {
                        PantryRepository.updateQuantity(item.id, newQty, stdUnit.id)
                    }
                }
            }
            
            // 3. Process Custom Items in Cart
            cartItems.filter { it.isCustom }.forEach { item ->
                ShoppingListItemRepository.toggleItem(item.id) // Marks as purchased
            }
            
            // 4. Clear Cart
            if (storeId == null) {
                ShoppingSessionRepository.clearCart()
            } else {
                ShoppingSessionRepository.removeItemsFromCart(cartItems.map { it.id })
            }
        }
    }
    
    fun updatePrice(ingredientId: Uuid, newPriceCents: Int) {
        viewModelScope.launch {
            // Update the purchase option for this ingredient
            val packages = IngredientRepository.packages.value
            val pkg = packages.filter { it.ingredientId == ingredientId }.minByOrNull { it.priceCents }
            
            if (pkg != null) {
                IngredientRepository.updatePackage(pkg.copy(priceCents = newPriceCents))
            }
        }
    }

    fun markOwned(item: ShoppingListItemUi) {
        if (item.isCustom) {
            ShoppingListItemRepository.toggleItem(item.id)
        } else {
            val allUnits = UnitRepository.units.value
            val unit = allUnits.find { it.abbreviation == item.unit }
            if (unit != null) {
                val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit, allUnits)
                
                val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == item.id }
                val currentBaseQty = if (currentPantryItem != null) {
                    val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
                    if (pUnit != null) UnitConverter.toStandard(currentPantryItem.quantity, pUnit, allUnits).first else 0.0
                } else 0.0
                
                val newQty = currentBaseQty + baseQtyToAdd
                
                val (_, stdUnit) = UnitConverter.toStandard(0.0, unit, allUnits)
                if (stdUnit != null) {
                    PantryRepository.updateQuantity(item.id, newQty, stdUnit.id)
                }
            }
        }
    }

    fun markUnowned(item: ShoppingListItemUi) {
        if (item.isCustom) {
            ShoppingListItemRepository.toggleItem(item.id)
        } else {
            ensureNeeded(item.id)
        }
    }

    fun moveToStore(ingredientId: Uuid, storeId: Uuid) {
        ShoppingListRepository.setStoreOverride(ingredientId, storeId)
        ensureNeeded(ingredientId)
    }

    fun addCustomItem(name: String, qty: Double, unitId: Uuid) {
        ShoppingListItemRepository.addItem(
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

data class ShoppingListUiState(
    val sections: List<ShoppingListSection>,
    val ownedItems: List<ShoppingListItemUi>,
    val allStores: List<Store>,
    val allUnits: List<UnitModel>,
    val taxRate: Double = 0.0
)

data class ShoppingListSection(
    val storeId: Uuid,
    val storeName: String,
    val items: List<ShoppingListItemUi>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long
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