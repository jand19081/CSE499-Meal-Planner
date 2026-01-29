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
                     val (reqBase, _) = UnitConverter.toStandard(ri.quantity.amount, ri.quantity.unit)
                     val (optBase, _) = UnitConverter.toStandard(bestOption.quantity.amount, bestOption.quantity.unit)
                     
                     if (optBase > 0) {
                         val pricePerBase = bestOption.priceCents / optBase
                         totalCents += (pricePerBase * reqBase).toLong()
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
                     if (bestOption != null) {
                         val (reqBase, _) = UnitConverter.toStandard(comp.quantity.amount, comp.quantity.unit)
                         val (optBase, _) = UnitConverter.toStandard(bestOption.quantity.amount, bestOption.quantity.unit)
                         
                         if (optBase > 0) {
                            val pricePerBase = bestOption.priceCents / optBase
                            totalCents += (pricePerBase * reqBase).toLong()
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

                            if (bestOption != null) {
                                val (reqBase, _) = UnitConverter.toStandard(ri.quantity.amount * scale, ri.quantity.unit)
                                val (optBase, _) = UnitConverter.toStandard(bestOption.quantity.amount, bestOption.quantity.unit)
                                 if (optBase > 0) {
                                    val pricePerBase = bestOption.priceCents / optBase
                                    totalCents += (pricePerBase * reqBase).toLong()
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

                    if (bestOption != null) {
                         val (reqBase, _) = UnitConverter.toStandard(requiredAmount, comp.quantity?.unit ?: MeasureUnit.EACH)
                         val (optBase, _) = UnitConverter.toStandard(bestOption.quantity.amount, bestOption.quantity.unit)
                         if (optBase > 0) {
                            val pricePerBase = bestOption.priceCents / optBase
                            totalCents += (pricePerBase * reqBase).toLong()
                         }
                    }
                }
            }
        }
        return totalCents
    }
}
