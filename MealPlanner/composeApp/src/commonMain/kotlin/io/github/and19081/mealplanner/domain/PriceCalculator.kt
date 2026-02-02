package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlin.uuid.Uuid

object PriceCalculator {

    fun calculateRecipeCost(
        recipe: Recipe,
        ingredientsMap: Map<Uuid, Ingredient>,
        recipeIngredientsMap: Map<Uuid, List<RecipeIngredient>>
    ): Long {
        val recipeIngredients = recipeIngredientsMap[recipe.id] ?: emptyList()
        var totalCents = 0L

        for (ri in recipeIngredients) {
            val ingredient = ingredientsMap[ri.ingredientId]
            if (ingredient != null) {
                 val bestOption = ingredient.purchaseOptions.minByOrNull { it.priceCents }
                 if (bestOption != null && bestOption.quantity.amount > 0) {
                     val convertedReqQty = UnitConverter.convert(
                         amount = ri.quantity.amount,
                         from = ri.quantity.unit,
                         to = bestOption.quantity.unit,
                         bridges = ingredient.conversionBridges
                     )
                     
                     if (convertedReqQty > 0) {
                         val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity.amount
                         totalCents += (pricePerUnit * convertedReqQty).toLong()
                     } else if (convertedReqQty <= 0 && ri.quantity.amount > 0) {
                         println("Warning: Cannot convert units for ${ingredient.name}")
                     }
                 }
            }
        }
        return totalCents
    }

    fun calculateMealCost(
        meal: Meal,
        componentsMap: Map<Uuid, List<MealComponent>>,
        recipesMap: Map<Uuid, Recipe>,
        ingredientsMap: Map<Uuid, Ingredient>,
        recipeIngredientsMap: Map<Uuid, List<RecipeIngredient>>
    ): Long {
        val components = componentsMap[meal.id] ?: emptyList()
        var totalCents = 0L

        for (comp in components) {
            if (comp.recipeId != null) {
                val recipe = recipesMap[comp.recipeId]
                if (recipe != null) {
                    totalCents += calculateRecipeCost(recipe, ingredientsMap, recipeIngredientsMap)
                }
            } else if (comp.ingredientId != null) {
                val ingredient = ingredientsMap[comp.ingredientId]
                if (ingredient != null && comp.quantity != null) {
                     val bestOption = ingredient.purchaseOptions.minByOrNull { it.priceCents }
                     if (bestOption != null && bestOption.quantity.amount > 0) {
                         val convertedReqQty = UnitConverter.convert(
                             amount = comp.quantity.amount,
                             from = comp.quantity.unit,
                             to = bestOption.quantity.unit,
                             bridges = ingredient.conversionBridges
                         )
                         
                         if (convertedReqQty > 0) {
                            val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity.amount
                            totalCents += (pricePerUnit * convertedReqQty).toLong()
                         } else if (convertedReqQty <= 0 && comp.quantity.amount > 0) {
                             println("Warning: Cannot convert units for ${ingredient.name}")
                         }
                     }
                }
            }
        }
        return totalCents
    }

    fun calculateEstimatedCost(
        entry: MealPlanEntry,
        mealsMap: Map<Uuid, Meal>,
        componentsMap: Map<Uuid, List<MealComponent>>,
        recipesMap: Map<Uuid, Recipe>,
        ingredientsMap: Map<Uuid, Ingredient>,
        recipeIngredientsMap: Map<Uuid, List<RecipeIngredient>>
    ): Long {
        // Find Meal
        val meal = mealsMap[entry.mealId] ?: return 0L
        
        // Find Components
        val components = componentsMap[meal.id] ?: emptyList()

        var totalCents = 0L

        for (comp in components) {
            if (comp.recipeId != null) {
                val recipe = recipesMap[comp.recipeId]
                if (recipe != null) {
                    val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0

                    val recipeIngredients = recipeIngredientsMap[recipe.id] ?: emptyList()

                    for (ri in recipeIngredients) {
                        val ingredient = ingredientsMap[ri.ingredientId]
                        if (ingredient != null) {
                            val bestOption = ingredient.purchaseOptions.minByOrNull { it.priceCents }

                            if (bestOption != null && bestOption.quantity.amount > 0) {
                                val convertedReqQty = UnitConverter.convert(
                                    amount = ri.quantity.amount * scale,
                                    from = ri.quantity.unit,
                                    to = bestOption.quantity.unit,
                                    bridges = ingredient.conversionBridges
                                )
                                 if (convertedReqQty > 0) {
                                    val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity.amount
                                    totalCents += (pricePerUnit * convertedReqQty).toLong()
                                 } else if (convertedReqQty <= 0 && ri.quantity.amount > 0) {
                                     println("Warning: Cannot convert units for ${ingredient.name}")
                                 }
                            }
                        }
                    }
                }
            } else if (comp.ingredientId != null) {
                val ingredient = ingredientsMap[comp.ingredientId]
                if (ingredient != null) {
                    val qty = comp.quantity?.amount ?: 1.0
                    val requiredAmount = qty * entry.targetServings
                    
                    val bestOption = ingredient.purchaseOptions.minByOrNull { it.priceCents }

                    if (bestOption != null && bestOption.quantity.amount > 0) {
                         val convertedReqQty = UnitConverter.convert(
                             amount = requiredAmount,
                             from = comp.quantity?.unit ?: MeasureUnit.EACH,
                             to = bestOption.quantity.unit,
                             bridges = ingredient.conversionBridges
                         )
                         if (convertedReqQty > 0) {
                            val pricePerUnit = bestOption.priceCents.toDouble() / bestOption.quantity.amount
                            totalCents += (pricePerUnit * convertedReqQty).toLong()
                         } else if (convertedReqQty <= 0 && requiredAmount > 0) {
                             println("Warning: Cannot convert units for ${ingredient.name}")
                         }
                    }
                }
            }
        }
        return totalCents
    }
}
