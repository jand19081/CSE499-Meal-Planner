package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.domain.model.recipeInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import kotlin.collections.get
import kotlin.uuid.Uuid

object PriceCalculator {

    suspend fun calculateFoodItemCost(
        item: FoodItem,
        foodItemRepository: FoodItemRepository,
        allUnits: Map<Uuid, UnitModel>,
    ): Long? {
        // Use CTE to get flat Bill of Materials
        val bom = foodItemRepository.getRecursiveIngredients(item.id)
        
        var totalCostDouble = 0.0

        for (measurement in bom) {
            val subItemId = measurement.foodItemId ?: continue
            val subItem = foodItemRepository.getFoodItem(subItemId) ?: continue

            // We only care about base ingredients (purchasable items)
            // Recipes that were expanded by the CTE are already accounted for in their base ingredients
            if (subItem is io.github.and19081.mealplanner.domain.model.Ingredient) {
                val bridges = foodItemRepository.getConversionsForFoodItem(subItem.id)
                val bestOption = foodItemRepository.getCheapestPurchaseOption(subItem.id)

                if (bestOption != null && bestOption.quantity > 0) {
                    val convertedReqQty =
                        UnitConverter.convert(
                            amount = measurement.quantity,
                            fromUnitId = measurement.unitId ?: subItem.preferredUnitId ?: Uuid.NIL,
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

    suspend fun calculateEstimatedCost(
        entry: ScheduledMeal,
        foodItemRepository: FoodItemRepository,
        allUnits: Map<Uuid, UnitModel>,
    ): Long {
        // 1. Prioritize explicitly set anticipated cost
        if (entry.anticipatedCostCents != null) {
            return entry.anticipatedCostCents.toLong()
        }

        // 2. If it's a restaurant and no cost set, we can't estimate
        if (entry.restaurantId != null) return 0L

        // 3. Fallback to calculated estimate for home meals
        val mealId = entry.prePlannedMealId ?: return 0L
        val meal = foodItemRepository.getFoodItem(mealId) ?: return 0L

        val recipeInfo = meal.recipeInfo ?: return 0L

        // Scale based on people count vs recipe servings
        val scale = if (recipeInfo.servings > 0) entry.peopleCount / recipeInfo.servings else 1.0

        val baseCost =
            calculateFoodItemCost(
                meal,
                foodItemRepository,
                allUnits,
            ) ?: 0L

        return (baseCost * scale).toLong()
    }

    /** Feature 5: "Make vs Buy" Cost Analysis. */
    suspend fun calculateMakeToStockAnalysis(
        recipe: FoodItem,
        foodItemRepository: FoodItemRepository,
        allUnits: Map<Uuid, UnitModel>,
    ): Pair<Long?, Long?> {
        // 1. Cost to Make (Raw Materials)
        val costToMake =
            calculateFoodItemCost(
                recipe,
                foodItemRepository,
                allUnits,
            )

        // 2. Cost to Buy (Equivalent Purchase Option)
        val costToBuy: Long? = null

        return costToMake to costToBuy
    }
}
