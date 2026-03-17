package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.BridgeConversion
import io.github.and19081.mealplanner.domain.Package
import kotlin.uuid.Uuid

object PriceCalculator {

  fun calculateFoodItemCost(
      item: FoodItem,
      allItemsMap: Map<Uuid, FoodItem>,
      packagesByIngredient: Map<Uuid, List<Package>>,
      bridgesByIngredient: Map<Uuid, List<BridgeConversion>>,
      allUnits: Map<Uuid, UnitModel>,
      visited: Set<Uuid> = emptySet(),
  ): Long {
    if (visited.contains(item.id)) {
      throw IllegalArgumentException("Circular dependency detected in item: ${item.id}")
    }
    val newVisited = visited + item.id
    var totalCents = 0L

    val recipeInfo = item.recipeInfo ?: return 0L

    for (req in recipeInfo.requirements) {
      val subItem = allItemsMap[req.foodItemId] ?: continue
      
      if (subItem.isRecipe) {
        // Recursive Call for Sub-Recipe
        val subRecipeBaseCost =
            calculateFoodItemCost(
                subItem,
                allItemsMap,
                packagesByIngredient,
                bridgesByIngredient,
                allUnits,
                newVisited,
            )

        // For sub-recipes, req.quantity is assumed to be the number of servings needed
        totalCents += (subRecipeBaseCost * req.quantity).toLong()
        continue
      }

      // It's an ingredient (purchasable)
      val packages = packagesByIngredient[subItem.id] ?: emptyList()
      val bridges = bridgesByIngredient[subItem.id] ?: emptyList()

      val bestOption =
          packages.minByOrNull {
            if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE
          }

      if (bestOption != null && bestOption.quantity > 0) {
        val convertedReqQty =
            UnitConverter.convert(
                amount = req.quantity,
                fromUnitId = req.unitId ?: subItem.preferredUnitId ?: Uuid.NIL,
                toUnitId = bestOption.unitId,
                allUnits = allUnits,
                bridges = bridges,
            ) ?: 0.0

        if (convertedReqQty > 0) {
          val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
          totalCents += (pricePerUnit * convertedReqQty).toLong()
        }
      }
    }
    return totalCents
  }

  fun calculateEstimatedCost(
      entry: ScheduledMeal,
      allItemsMap: Map<Uuid, FoodItem>,
      packagesByIngredient: Map<Uuid, List<Package>>,
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
    val baseCost = calculateFoodItemCost(
        meal,
        allItemsMap,
        packagesByIngredient,
        bridgesByIngredient,
        allUnits,
        emptySet()
    )
    
    return (baseCost * scale).toLong()
  }

  /**
   * Feature 5: "Make vs Buy" Cost Analysis.
   */
  fun calculateMakeToStockAnalysis(
      recipe: FoodItem,
      allItemsMap: Map<Uuid, FoodItem>,
      packagesByIngredient: Map<Uuid, List<Package>>,
      bridgesByIngredient: Map<Uuid, List<BridgeConversion>>,
      allUnits: Map<Uuid, UnitModel>,
  ): Pair<Long, Long> {
    // 1. Cost to Make (Raw Materials)
    val costToMake =
        calculateFoodItemCost(recipe, allItemsMap, packagesByIngredient, bridgesByIngredient, allUnits)

    // 2. Cost to Buy (Equivalent Package Option)
    var costToBuy = 0L
    
    // In ECS, "produces ingredient" logic needs to be revisited.
    // For now, let's assume we look for a FoodItem that is an ingredient and matches the name?
    // Or add producesFoodItemId to RecipeInfo.
    
    // For now, return 0 for costToBuy until schema updated or logic refined
    return costToMake to costToBuy
  }
}
