package io.github.and19081.mealplanner.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.Meal
import io.github.and19081.mealplanner.MealComponent
import io.github.and19081.mealplanner.MealPlanEntry
import io.github.and19081.mealplanner.MealPlannerRepository
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.RecipeIngredient
import io.github.and19081.mealplanner.RecipeRepository
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.Uuid
import kotlin.math.ceil

class ShoppingListViewModel : ViewModel() {

    data class RepoData(
        val entries: List<MealPlanEntry>,
        val meals: List<Meal>,
        val mealComponents: List<MealComponent>,
        val recipes: List<Recipe>,
        val overrides: Map<Uuid, ShoppingListOverride>,
        val taxRate: Double,
        val allIngredients: List<Ingredient>,
        val allStores: List<Store>,
        val allRecipeIngredients: List<RecipeIngredient>
    )

    private val repoDataFlow = combine(
        MealPlanRepository.entries,
        MealPlannerRepository.meals,
        MealPlannerRepository.mealComponents,
        RecipeRepository.recipes,
        ShoppingListRepository.overrides,
        SettingsRepository.salesTaxRate,
        MealPlannerRepository.ingredients,
        MealPlannerRepository.stores,
        MealPlannerRepository.recipeIngredients
    ) { args: Array<Any> ->
        RepoData(
            entries = args[0] as List<MealPlanEntry>,
            meals = args[1] as List<Meal>,
            mealComponents = args[2] as List<MealComponent>,
            recipes = args[3] as List<Recipe>,
            overrides = args[4] as Map<Uuid, ShoppingListOverride>,
            taxRate = args[5] as Double,
            allIngredients = args[6] as List<Ingredient>,
            allStores = args[7] as List<Store>,
            allRecipeIngredients = args[8] as List<RecipeIngredient>
        )
    }

    // Map the combined data to UI State
    val uiState = repoDataFlow.combine(kotlinx.coroutines.flow.flowOf(Unit)) { data, _ ->
        val (entries, meals, mealComponents, recipes, overrides, taxRate, allIngredients, allStores, allRecipeIngredients) = data

        // Aggregate Needs
        val requirements = mutableMapOf<Pair<Uuid, String>, Double>() // (IngId, UnitName) -> Qty

        entries.forEach { entry ->
            // Find the Meal Definition
            val meal = meals.find { it.id == entry.mealId }
            if (meal != null) {
                // Find items in this meal
                val components = mealComponents.filter { it.mealId == meal.id }
                
                components.forEach { comp ->
                    if (comp.ingredientId != null) {
                        // Direct Ingredient
                        val ing = allIngredients.find { it.id == comp.ingredientId }
                        if (ing != null) {
                            val key = ing.id to (comp.quantity?.unit?.name ?: "EACH")
                            val baseQty = comp.quantity?.amount ?: 1.0
                            requirements[key] = (requirements[key] ?: 0.0) + (baseQty * entry.targetServings)
                        }
                    } else if (comp.recipeId != null) {
                        // Recipe
                        val recipe = recipes.find { it.id == comp.recipeId }
                        if (recipe != null) {
                            // Scale: (Target / Base)
                            val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0
                            
                            val recipeIngs = allRecipeIngredients.filter { it.recipeId == recipe.id }
                            recipeIngs.forEach { ri ->
                                val key = ri.ingredientId to ri.quantity.unit.name
                                requirements[key] = (requirements[key] ?: 0.0) + (ri.quantity.amount * scale)
                            }
                        }
                    }
                }
            }
        }

        // Assign to Lists
        val shoppingLists = mutableMapOf<Uuid, ShoppingListSection>() // StoreId -> Section
        val ownedItems = mutableListOf<ShoppingListItemUi>()

        allStores.forEach { store ->
            shoppingLists[store.id] = ShoppingListSection(
                storeId = store.id,
                storeName = store.name,
                items = emptyList(),
                subtotalCents = 0,
                taxCents = 0,
                totalCents = 0
            )
        }

        requirements.forEach { (key, qty) ->
            val (ingId, unitName) = key
            val ing = allIngredients.find { it.id == ingId } ?: return@forEach
            val override = overrides[ingId]

            if (override?.isOwned == true) {
                ownedItems.add(
                    ShoppingListItemUi(
                        ingredientId = ingId,
                        name = ing.name,
                        quantity = qty,
                        requiredQuantity = qty,
                        unit = unitName,
                        priceCents = 0,
                        isOwned = true
                    )
                )
            } else {
                val forcedStoreId = override?.forceStoreId
                
                val bestOption = if (forcedStoreId != null) {
                    ing.purchaseOptions.find { it.storeId == forcedStoreId } 
                        ?: ing.purchaseOptions.minByOrNull { it.unitPrice }
                } else {
                    ing.purchaseOptions.minByOrNull { it.unitPrice }
                }

                val targetStoreId = bestOption?.storeId ?: forcedStoreId
                
                if (targetStoreId != null) {
                    var finalPrice = 0L
                    var purchasedQty = qty // Default if we can't find option size

                    if (bestOption != null) {
                        val packSize = bestOption.quantity.amount
                        if (packSize > 0) {
                             val packsNeeded = ceil(qty / packSize).toLong()
                             finalPrice = packsNeeded * bestOption.priceCents
                             purchasedQty = packsNeeded * packSize
                        }
                    }

                    val currentList = shoppingLists[targetStoreId] 
                        ?: ShoppingListSection(targetStoreId,
                            allStores.find { it.id == targetStoreId }?.name ?: "Unknown",
                            emptyList(),
                            0,
                            0,
                            0
                        )
                    
                    val newItem = ShoppingListItemUi(
                        ingredientId = ingId,
                        name = ing.name,
                        quantity = purchasedQty,
                        requiredQuantity = qty,
                        unit = unitName,
                        priceCents = finalPrice,
                        isOwned = false
                    )
                    
                    shoppingLists[targetStoreId] = currentList.copy(
                        items = currentList.items + newItem,
                        subtotalCents = currentList.subtotalCents + finalPrice
                    )
                }
            }
        }

        // Calculate Totals (Tax)
        val finalLists = shoppingLists.values.map { section ->
            val tax = (section.subtotalCents * taxRate).toLong()
            section.copy(
                taxCents = tax,
                totalCents = section.subtotalCents + tax
            )
        }.filter { it.items.isNotEmpty() || it.storeName.isNotEmpty() }

        ShoppingListUiState(
            sections = finalLists.sortedBy { it.storeName },
            ownedItems = ownedItems,
            allStores = allStores
        )
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ShoppingListUiState(emptyList(),
            emptyList(),
            emptyList()
        )
    )

    fun markOwned(ingredientId: Uuid) {
        ShoppingListRepository.markAsOwned(ingredientId)
    }

    fun moveToStore(ingredientId: Uuid, storeId: Uuid) {
        ShoppingListRepository.setStoreOverride(ingredientId, storeId)
    }

    fun clearOverride(ingredientId: Uuid) {
        ShoppingListRepository.clearOverride(ingredientId)
    }
}

data class ShoppingListUiState(
    val sections: List<ShoppingListSection>,
    val ownedItems: List<ShoppingListItemUi>,
    val allStores: List<Store>
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
    val ingredientId: Uuid,
    val name: String,
    val quantity: Double, // Amount to buy
    val requiredQuantity: Double, // Actual amount needed
    val unit: String,
    val priceCents: Long,
    val isOwned: Boolean
)
