package io.github.and19081.mealplanner.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
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
        val allRecipeIngredients: List<RecipeIngredient>,
        val taxRate: Double
    )

    // Helper data class to group user/dynamic data
    data class UserData(
        val entries: List<MealPlanEntry>,
        val meals: List<Meal>,
        val mealComponents: List<MealComponent>,
        val overrides: Map<Uuid, ShoppingListOverride>,
        val pantryItems: List<PantryItem>,
        val customItems: List<CustomShoppingItem>,
        val inCartItems: Set<Uuid>
    )

    private val coreDataFlow = combine(
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        StoreRepository.stores,
        RecipeRepository.recipeIngredients,
        SettingsRepository.salesTaxRate
    ) { recipes, ingredients, stores, recipeIngredients, tax ->
        CoreData(recipes, ingredients, stores, recipeIngredients, tax)
    }

    private val userDataFlow = combine(
        MealPlanRepository.entries,
        MealRepository.meals,
        MealRepository.mealComponents,
        ShoppingListRepository.overrides,
        PantryRepository.pantryItems,
        CustomItemRepository.items,
        ShoppingSessionRepository.inCartItems
    ) { args: Array<Any> ->
        UserData(
            entries = args[0] as List<MealPlanEntry>,
            meals = args[1] as List<Meal>,
            mealComponents = args[2] as List<MealComponent>,
            overrides = args[3] as Map<Uuid, ShoppingListOverride>,
            pantryItems = args[4] as List<PantryItem>,
            customItems = args[5] as List<CustomShoppingItem>,
            inCartItems = args[6] as Set<Uuid>
        )
    }

    // Map the combined data to UI State
    val uiState = combine(coreDataFlow, userDataFlow) { core, user ->
        val (recipes, allIngredients, allStores, allRecipeIngredients, taxRate) = core
        val (entries, meals, mealComponents, overrides, pantryItems, customItems, inCartItems) = user

        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = meals.associateBy { it.id }
        val recipesMap = recipes.associateBy { it.id }
        val ingredientsMap = allIngredients.associateBy { it.id }
        val mealComponentsMap = mealComponents.groupBy { it.mealId }
        val recipeIngredientsMap = allRecipeIngredients.groupBy { it.recipeId }

        // 1. Calculate Requirements (Normalized to Base Unit)
        val grossRequirements = mutableMapOf<Uuid, Double>()

        entries.forEach { entry ->
            val meal = mealsMap[entry.mealId]
            if (meal != null) {
                val components = mealComponentsMap[meal.id] ?: emptyList()
                components.forEach { comp ->
                    fun add(ingId: Uuid, qty: Double, unit: MeasureUnit) {
                        val (baseQty, _) = UnitConverter.toStandard(qty, unit)
                        grossRequirements[ingId] = (grossRequirements[ingId] ?: 0.0) + baseQty
                    }

                    if (comp.ingredientId != null) {
                        val ing = ingredientsMap[comp.ingredientId]
                        if (ing != null && comp.quantity != null) {
                            add(ing.id, comp.quantity.amount * entry.targetServings, comp.quantity.unit)
                        }
                    } else if (comp.recipeId != null) {
                        val recipe = recipesMap[comp.recipeId]
                        if (recipe != null) {
                            val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0
                            val recipeIngs = recipeIngredientsMap[recipe.id] ?: emptyList()
                            recipeIngs.forEach { ri ->
                                add(ri.ingredientId, ri.quantity.amount * scale, ri.quantity.unit)
                            }
                        }
                    }
                }
            }
        }

        // 2. Apply Pantry
        val netRequirements = mutableMapOf<Uuid, Double>()
        val fullyOwnedIngredients = mutableListOf<Uuid>()

        grossRequirements.forEach { (ingId, reqQty) ->
            val pantryItem = pantryItems.find { it.ingredientId == ingId }
            val ownedQty = if (pantryItem != null) {
                UnitConverter.toStandard(pantryItem.quantityOnHand.amount, pantryItem.quantityOnHand.unit).first
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

            val forcedStoreId = override?.forceStoreId
            val bestOption = if (forcedStoreId != null) {
                 ing.purchaseOptions.find { it.storeId == forcedStoreId }
                     ?: ing.purchaseOptions.minByOrNull { it.unitPrice }
            } else {
                ing.purchaseOptions.minByOrNull { it.unitPrice }
            }

            val targetStoreId = bestOption?.storeId ?: forcedStoreId ?: anyStoreId
            
            var purchaseQty = baseQtyNeeded
            var price = 0L
            var displayUnit = "Units"

            if (bestOption != null) {
                val (baseOptionQty, _) = UnitConverter.toStandard(bestOption.quantity.amount, bestOption.quantity.unit)
                if (baseOptionQty > 0) {
                     val packs = ceil(baseQtyNeeded / baseOptionQty).toLong()
                     purchaseQty = packs * bestOption.quantity.amount
                     price = packs * bestOption.priceCents
                     displayUnit = bestOption.quantity.unit.name
                }
            } else {
                val (_, stdUnit) = UnitConverter.toStandard(baseQtyNeeded, MeasureUnit.EACH)
                displayUnit = stdUnit.name
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
            if (!custom.isChecked) {
                val item = ShoppingListItemUi(
                    id = custom.id,
                    name = custom.name,
                    quantity = custom.quantity,
                    requiredQuantity = custom.quantity,
                    unit = custom.unit.name,
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
        } + customItems.filter { it.isChecked }.map { 
             ShoppingListItemUi(
                 id = it.id,
                 name = it.name,
                 quantity = it.quantity,
                 requiredQuantity = it.quantity,
                 unit = it.unit.name,
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

        ShoppingListUiState(finalSections, ownedItemsUi, allStores, taxRate)

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingListUiState(emptyList(), emptyList(), emptyList(), 0.0))

    private fun ensureNeeded(ingredientId: Uuid) {
        viewModelScope.launch {
             val entries = MealPlanRepository.entries.value
             val meals = MealRepository.meals.value
             val components = MealRepository.mealComponents.value
             val recipes = RecipeRepository.recipes.value
             val allRecipeIngredients = RecipeRepository.recipeIngredients.value
             
             var req = 0.0
             entries.forEach { entry ->
                val meal = meals.find { it.id == entry.mealId }
                if (meal != null) {
                    val comps = components.filter { it.mealId == meal.id }
                    comps.forEach { comp ->
                        if (comp.ingredientId == ingredientId) {
                            if (comp.quantity != null) {
                                val (base, _) = UnitConverter.toStandard(comp.quantity.amount * entry.targetServings, comp.quantity.unit)
                                req += base
                            }
                        } else if (comp.recipeId != null) {
                            val recipe = recipes.find { it.id == comp.recipeId }
                            if (recipe != null) {
                                val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0
                                val ris = allRecipeIngredients.filter { it.recipeId == recipe.id && it.ingredientId == ingredientId }
                                ris.forEach { ri ->
                                    val (base, _) = UnitConverter.toStandard(ri.quantity.amount * scale, ri.quantity.unit)
                                    req += base
                                }
                            }
                        }
                    }
                }
             }
             
             val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == ingredientId }
             if (currentPantryItem != null) {
                 val (currentBase, currentUnit) = UnitConverter.toStandard(currentPantryItem.quantityOnHand.amount, currentPantryItem.quantityOnHand.unit)
                 val newBase = max(0.0, currentBase - req)
                 PantryRepository.updateQuantity(ingredientId, Measure(newBase, currentUnit))
             }
        }
    }

    fun toggleCart(id: Uuid) {
        ShoppingSessionRepository.toggleCartStatus(id)
    }

    fun finalizeTrip(totalPaidCents: Long, storeId: Uuid? = null) {
        viewModelScope.launch {
            val currentState = uiState.value
            
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
            
            val receiptItems = cartItems.map { 
                ReceiptItem(
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    priceCents = it.priceCents,
                    ingredientId = if (it.isCustom) null else it.id
                )
            }
            
            val subtotalCents = receiptItems.sumOf { it.priceCents }
            val taxCents = max(0L, totalPaidCents - subtotalCents)

            val trip = ShoppingTrip(
                date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                storeName = dominantStore,
                subtotalCents = subtotalCents,
                taxCents = taxCents,
                totalPaidCents = totalPaidCents,
                items = receiptItems
            )
            ShoppingHistoryRepository.addTrip(trip)

            // 2. Process Ingredients in Cart
            cartItems.filter { !it.isCustom }.forEach { item ->
                val unit = MeasureUnit.entries.find { it.name == item.unit } ?: MeasureUnit.EACH
                val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit)
                
                val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == item.id }
                val currentBaseQty = if (currentPantryItem != null) {
                    UnitConverter.toStandard(currentPantryItem.quantityOnHand.amount, currentPantryItem.quantityOnHand.unit).first
                } else 0.0
                
                val newQty = currentBaseQty + baseQtyToAdd
                val (_, stdUnit) = UnitConverter.toStandard(0.0, unit)
                PantryRepository.updateQuantity(item.id, Measure(newQty, stdUnit))
            }
            
            // 3. Process Custom Items in Cart
            cartItems.filter { it.isCustom }.forEach { item ->
                CustomItemRepository.toggleItem(item.id) // Marks as checked/owned
            }
            
            // 4. Clear Cart
            if (storeId == null) {
                ShoppingSessionRepository.clearCart()
            } else {
                ShoppingSessionRepository.removeItemsFromCart(cartItems.map { it.id })
            }
        }
    }
    
    fun updatePrice(ingredientId: Uuid, newPriceCents: Long) {
        viewModelScope.launch {
            // Update the purchase option for this ingredient
            val ingredients = IngredientRepository.ingredients.value
            val ing = ingredients.find { it.id == ingredientId } ?: return@launch
            
            if (ing.purchaseOptions.isNotEmpty()) {
                // Find min price option to update
                val minOpt = ing.purchaseOptions.minByOrNull { it.priceCents }
                if (minOpt != null) {
                    val updatedOptions = ing.purchaseOptions.map { 
                        if (it.id == minOpt.id) it.copy(priceCents = newPriceCents) else it 
                    }
                    IngredientRepository.updateIngredient(ing.copy(purchaseOptions = updatedOptions))
                }
            } else {
                // No options exist, maybe create one?
                // Skip for now as we need store ID.
            }
        }
    }

    fun markOwned(item: ShoppingListItemUi) {
        if (item.isCustom) {
            CustomItemRepository.toggleItem(item.id)
        } else {
            val unit = MeasureUnit.entries.find { it.name == item.unit } ?: MeasureUnit.EACH
            val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit)
            
            val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == item.id }
            val currentBaseQty = if (currentPantryItem != null) {
                UnitConverter.toStandard(currentPantryItem.quantityOnHand.amount, currentPantryItem.quantityOnHand.unit).first
            } else 0.0
            
            val newQty = currentBaseQty + baseQtyToAdd
            
            val (_, stdUnit) = UnitConverter.toStandard(0.0, unit)
            PantryRepository.updateQuantity(item.id, Measure(newQty, stdUnit))
        }
    }

    fun markUnowned(item: ShoppingListItemUi) {
        if (item.isCustom) {
            CustomItemRepository.toggleItem(item.id)
        } else {
            ensureNeeded(item.id)
        }
    }

    fun moveToStore(ingredientId: Uuid, storeId: Uuid) {
        ShoppingListRepository.setStoreOverride(ingredientId, storeId)
        ensureNeeded(ingredientId)
    }

    fun addCustomItem(name: String, qty: Double, unit: MeasureUnit) {
        CustomItemRepository.addItem(CustomShoppingItem(name = name, quantity = qty, unit = unit))
    }
}

data class ShoppingListUiState(
    val sections: List<ShoppingListSection>,
    val ownedItems: List<ShoppingListItemUi>,
    val allStores: List<Store>,
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
