package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.domain.model.recipeInfo
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import kotlin.collections.get
import kotlin.uuid.Uuid

object PriceCalculator {

    fun calculateFoodItemCost(
        item: FoodItem,
        allItemsMap: Map<Uuid, FoodItem>,
        purchaseOptionByIngredient: Map<Uuid, List<PurchaseOption>>,
        bridgesByIngredient: Map<Uuid, List<BridgeConversion>>,
        allUnits: Map<Uuid, UnitModel>,
        visited: Set<Uuid> = emptySet(),
    ): Long? {
        if (visited.contains(item.id)) {
            return null // Circular dependency
        }
        val newVisited = visited + item.id
        var totalCents = 0L

        val recipeInfo = item.recipeInfo ?: return 0L

        var totalCostDouble = 0.0

        for (group in recipeInfo.requirementGroups) {
            val primaryReq = group.requirements.find { it.isPrimary } ?: group.requirements.firstOrNull()
            primaryReq?.let { req ->
                val subItem = allItemsMap[req.measurement.foodItemId] ?: return@let

                if (subItem.isRecipe()) {
                    // Recursive Call for Sub-Recipe
                    val subRecipeBaseCost =
                        calculateFoodItemCost(
                            subItem,
                            allItemsMap,
                            purchaseOptionByIngredient,
                            bridgesByIngredient,
                            allUnits,
                            newVisited,
                        ) ?: return null

                    val recipeServings = subItem.recipeInfo?.servings?.takeIf { it > 0.0 } ?: 1.0
                    val costPerServing = subRecipeBaseCost.toDouble() / recipeServings

                    // Convert the required quantity into the recipe's base unit (servings)
                    val preferredUnitId =
                        subItem.preferredUnitId
                            ?: io.github.and19081.mealplanner.core.util.SystemUnits.Each.id
                    val reqUnitId = req.measurement.unitId ?: preferredUnitId
                    val bridges = bridgesByIngredient[subItem.id] ?: emptyList()

                    val servingsNeeded =
                        io.github.and19081.mealplanner.core.util.UnitConverter.convert(
                            amount = req.measurement.quantity,
                            fromUnitId = reqUnitId,
                            toUnitId = preferredUnitId,
                            allUnits = allUnits,
                            bridges = bridges,
                        ) ?: req.measurement.quantity // Fallback if no conversion exists

                    totalCostDouble += costPerServing * servingsNeeded
                    return@let
                }

                // It's an ingredient (purchasable)
                val purchaseOptions = purchaseOptionByIngredient[subItem.id] ?: emptyList()
                val bridges = bridgesByIngredient[subItem.id] ?: emptyList()

                val bestOption = purchaseOptions.minByOrNull {
                    if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE
                }

                if (bestOption != null && bestOption.quantity > 0) {
                    val convertedReqQty =
                        UnitConverter.convert(
                            amount = req.measurement.quantity,
                            fromUnitId = req.measurement.unitId ?: subItem.preferredUnitId ?: Uuid.NIL,
                            toUnitId = bestOption.unitId,
                            allUnits = allUnits,
                            bridges = bridges,
                        ) ?: 0.0

                    if (convertedReqQty > 0) {
                        val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                        totalCostDouble += pricePerUnit * convertedReqQty
                    }
                }
            }
        }
        return totalCostDouble.toLong()
    }

    fun calculateEstimatedCost(
        entry: ScheduledMeal,
        allItemsMap: Map<Uuid, FoodItem>,
        purchaseOptionsByIngredient: Map<Uuid, List<PurchaseOption>>,
        bridgesByIngredient: Map<Uuid, List<BridgeConversion>>,
        allUnits: Map<Uuid, UnitModel>,
    ): Long {
        // 1. Prioritize explicitly set anticipated cost
        if (entry.anticipatedCostCents != null) {
            return entry.anticipatedCostCents.toLong()
        }

        // 2. If it's a restaurant and no cost set, we can't estimate
        if (entry.restaurantId != null) return 0L

        // 3. Fallback to calculated estimate for home meals
        val meal = allItemsMap[entry.prePlannedMealId] ?: return 0L

        var totalCents = 0L
        val recipeInfo = meal.recipeInfo ?: return 0L

        // Scale based on people count vs recipe servings
        val scale = if (recipeInfo.servings > 0) entry.peopleCount / recipeInfo.servings else 1.0

        // For simplicity, we just use calculateFoodItemCost and multiply by scale
        // Note: This might need more granular scaling for nested recipes if they don't scale linearly
        val baseCost =
            calculateFoodItemCost(
                meal,
                allItemsMap,
                purchaseOptionsByIngredient,
                bridgesByIngredient,
                allUnits,
                emptySet(),
            ) ?: 0L

        return (baseCost * scale).toLong()
    }

    /** Feature 5: "Make vs Buy" Cost Analysis. */
    fun calculateMakeToStockAnalysis(
        recipe: FoodItem,
        allItemsMap: Map<Uuid, FoodItem>,
        purchaseOptionByIngredient: Map<Uuid, List<PurchaseOption>>,
        bridgesByIngredient: Map<Uuid, List<BridgeConversion>>,
        allUnits: Map<Uuid, UnitModel>,
    ): Pair<Long?, Long?> {
        // 1. Cost to Make (Raw Materials)
        val costToMake =
            calculateFoodItemCost(
                recipe,
                allItemsMap,
                purchaseOptionByIngredient,
                bridgesByIngredient,
                allUnits,
            )

        // 2. Cost to Buy (Equivalent Purchase Option)
        val costToBuy: Long? = null

        // In ECS, "produces ingredient" logic needs to be revisited.
        // For now, let's assume we look for a FoodItem that is an ingredient and matches the name?
        // Or add producesFoodItemId to RecipeInfo.

        // For now, return null for costToBuy until schema updated or logic refined
        return costToMake to costToBuy
    }
}
