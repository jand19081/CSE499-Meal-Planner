package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

object PriceCalculator {

    fun calculateRecipeCost(
        recipe: Recipe,
        ingredientsMap: Map<Uuid, Ingredient>,
        allPackages: List<Package>,
        allBridges: List<BridgeConversion>,
        allUnits: List<UnitModel>
    ): Long {
        var totalCents = 0L

        for (ri in recipe.ingredients) {
            val ingredient = ingredientsMap[ri.ingredientId]
            if (ingredient != null) {
                 val packages = allPackages.filter { it.ingredientId == ingredient.id }
                 val bridges = allBridges.filter { it.ingredientId == ingredient.id }
                 
                 val bestOption = packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }
                 
                 if (bestOption != null && bestOption.quantity > 0) {
                     val convertedReqQty = UnitConverter.convert(
                         amount = ri.quantity,
                         fromUnitId = ri.unitId,
                         toUnitId = bestOption.unitId,
                         allUnits = allUnits,
                         bridges = bridges
                     )
                     
                     if (convertedReqQty > 0) {
                         val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                         totalCents += (pricePerUnit * convertedReqQty).toLong()
                     }
                 }
            }
        }
        return totalCents
    }

    fun calculateMealCost(
        meal: PrePlannedMeal,
        recipesMap: Map<Uuid, Recipe>,
        ingredientsMap: Map<Uuid, Ingredient>,
        allPackages: List<Package>,
        allBridges: List<BridgeConversion>,
        allUnits: List<UnitModel>
    ): Long {
        var totalCents = 0L

        // 1. Recipes
        for (recipeId in meal.recipes) {
            val recipe = recipesMap[recipeId]
            if (recipe != null) {
                totalCents += calculateRecipeCost(recipe, ingredientsMap, allPackages, allBridges, allUnits)
            }
        }
        
        // 2. Independent Ingredients
        for (comp in meal.independentIngredients) {
            val ingredient = ingredientsMap[comp.ingredientId]
            if (ingredient != null) {
                 val packages = allPackages.filter { it.ingredientId == ingredient.id }
                 val bridges = allBridges.filter { it.ingredientId == ingredient.id }
                 
                 val bestOption = packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }
                 
                 if (bestOption != null && bestOption.quantity > 0) {
                     val convertedReqQty = UnitConverter.convert(
                         amount = comp.quantity,
                         fromUnitId = comp.unitId,
                         toUnitId = bestOption.unitId,
                         allUnits = allUnits,
                         bridges = bridges
                     )
                     
                     if (convertedReqQty > 0) {
                        val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                        totalCents += (pricePerUnit * convertedReqQty).toLong()
                     }
                 }
            }
        }
        return totalCents
    }

    fun calculateEstimatedCost(
        entry: ScheduledMeal,
        mealsMap: Map<Uuid, PrePlannedMeal>,
        recipesMap: Map<Uuid, Recipe>,
        ingredientsMap: Map<Uuid, Ingredient>,
        allPackages: List<Package>,
        allBridges: List<BridgeConversion>,
        allUnits: List<UnitModel>
    ): Long {
        // Find Meal
        val meal = mealsMap[entry.prePlannedMealId] ?: return 0L
        
        var totalCents = 0L

        // 1. Recipes with Scaling
        for (recipeId in meal.recipes) {
            val recipe = recipesMap[recipeId]
            if (recipe != null) {
                val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0

                for (ri in recipe.ingredients) {
                    val ingredient = ingredientsMap[ri.ingredientId]
                    if (ingredient != null) {
                         val packages = allPackages.filter { it.ingredientId == ingredient.id }
                         val bridges = allBridges.filter { it.ingredientId == ingredient.id }

                        val bestOption = packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }

                        if (bestOption != null && bestOption.quantity > 0) {
                            val convertedReqQty = UnitConverter.convert(
                                amount = ri.quantity * scale,
                                fromUnitId = ri.unitId,
                                toUnitId = bestOption.unitId,
                                allUnits = allUnits,
                                bridges = bridges
                            )
                             if (convertedReqQty > 0) {
                                val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                                totalCents += (pricePerUnit * convertedReqQty).toLong()
                             }
                        }
                    }
                }
            }
        }
        
        // 2. Independent Ingredients with Scaling
        for (comp in meal.independentIngredients) {
             val ingredient = ingredientsMap[comp.ingredientId]
             if (ingredient != null) {
                 val requiredAmount = comp.quantity * entry.peopleCount // Assuming quantity is per person
                 
                 val packages = allPackages.filter { it.ingredientId == ingredient.id }
                 val bridges = allBridges.filter { it.ingredientId == ingredient.id }

                 val bestOption = packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }

                 if (bestOption != null && bestOption.quantity > 0) {
                      val convertedReqQty = UnitConverter.convert(
                          amount = requiredAmount,
                          fromUnitId = comp.unitId,
                          toUnitId = bestOption.unitId,
                          allUnits = allUnits,
                          bridges = bridges
                      )
                      if (convertedReqQty > 0) {
                         val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                         totalCents += (pricePerUnit * convertedReqQty).toLong()
                      }
                 }
             }
        }
        
        return totalCents
    }
}
