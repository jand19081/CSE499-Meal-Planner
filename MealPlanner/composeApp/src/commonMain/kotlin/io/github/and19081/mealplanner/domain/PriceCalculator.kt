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
        allUnits: List<UnitModel>,
        recipesMap: Map<Uuid, Recipe> = emptyMap() // Added recipesMap for recursion
    ): Long {
        var totalCents = 0L

        for (ri in recipe.ingredients) {
            if (ri.subRecipeId != null) {
                // Recursive Call for Sub-Recipe
                val subRecipe = recipesMap[ri.subRecipeId]
                if (subRecipe != null) {
                    // Cost of 1 serving of sub-recipe
                    val subRecipeBaseCost = calculateRecipeCost(subRecipe, ingredientsMap, allPackages, allBridges, allUnits, recipesMap)
                    
                    // Scale to required quantity (assuming ri.quantity is in "Servings" if unit is Each/Servings?)
                    // For now, assume ri.quantity is how many servings of the sub-recipe are needed
                    totalCents += (subRecipeBaseCost * ri.quantity).toLong()
                }
                continue
            }

            val ingredient = ri.ingredientId?.let { ingredientsMap[it] }
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

        // Recipes
        for (recipeId in meal.recipes) {
            val recipe = recipesMap[recipeId]
            if (recipe != null) {
                totalCents += calculateRecipeCost(recipe, ingredientsMap, allPackages, allBridges, allUnits, recipesMap)
            }
        }
        
        // Independent Ingredients
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
        // 1. Prioritize explicitly set anticipated cost
        if (entry.anticipatedCostCents != null) {
            return entry.anticipatedCostCents.toLong()
        }

        // 2. If it's a restaurant and no cost set, we can't estimate
        if (entry.restaurantId != null) return 0L

        // 3. Fallback to calculated estimate for home meals
        val meal = mealsMap[entry.prePlannedMealId] ?: return 0L
        
        var totalCents = 0L

        // Recipes with Scaling
        for (recipeId in meal.recipes) {
            val recipe = recipesMap[recipeId]
            if (recipe != null) {
                val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                
                // Helper to calculate scaled cost
                fun calculateScaled(r: Recipe, currentScale: Double): Long {
                    var subTotal = 0L
                    for (ri in r.ingredients) {
                        if (ri.subRecipeId != null) {
                            val subR = recipesMap[ri.subRecipeId]
                            if (subR != null) {
                                subTotal += calculateScaled(subR, currentScale * ri.quantity)
                            }
                            continue
                        }
                        
                        val ingredient = ri.ingredientId?.let { ingredientsMap[it] }
                        if (ingredient != null) {
                             val packages = allPackages.filter { it.ingredientId == ingredient.id }
                             val bridges = allBridges.filter { it.ingredientId == ingredient.id }

                            val bestOption = packages.minByOrNull { if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE }

                            if (bestOption != null && bestOption.quantity > 0) {
                                val convertedReqQty = UnitConverter.convert(
                                    amount = ri.quantity * currentScale,
                                    fromUnitId = ri.unitId,
                                    toUnitId = bestOption.unitId,
                                    allUnits = allUnits,
                                    bridges = bridges
                                )
                                 if (convertedReqQty > 0) {
                                    val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity
                                    subTotal += (pricePerUnit * convertedReqQty).toLong()
                                 }
                            }
                        }
                    }
                    return subTotal
                }

                totalCents += calculateScaled(recipe, scale)
            }
        }
        
        // Independent Ingredients with Scaling
        for (comp in meal.independentIngredients) {
             val ingredient = ingredientsMap[comp.ingredientId]
             if (ingredient != null) {
                 val requiredAmount = comp.quantity * entry.peopleCount // quantity is per person
                 
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
