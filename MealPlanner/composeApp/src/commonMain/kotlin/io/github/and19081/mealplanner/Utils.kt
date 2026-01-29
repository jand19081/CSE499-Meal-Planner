package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient

// Legacy Logic (List-based) - To be deprecated/refactored in Views
// Optimized logic moved to domain/PriceCalculator.kt

fun calculateRecipeCost(
    recipe: Recipe,
    allIngredients: List<Ingredient>,
    allRecipeIngredients: List<RecipeIngredient>
): Long {
    val recipeIngredients = allRecipeIngredients.filter { it.recipeId == recipe.id }
    var totalCents = 0L

    for (ri in recipeIngredients) {
        val ingredient = allIngredients.find { it.id == ri.ingredientId }
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
    allComponents: List<MealComponent>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allRecipeIngredients: List<RecipeIngredient>
): Long {
    val components = allComponents.filter { it.mealId == meal.id }
    var totalCents = 0L

    for (comp in components) {
        if (comp.recipeId != null) {
            val recipe = allRecipes.find { it.id == comp.recipeId }
            if (recipe != null) {
                totalCents += calculateRecipeCost(recipe, allIngredients, allRecipeIngredients)
            }
        } else if (comp.ingredientId != null) {
            val ingredient = allIngredients.find { it.id == comp.ingredientId }
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
    allMeals: List<Meal>,
    allMealComponents: List<MealComponent>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allRecipeIngredients: List<RecipeIngredient>
): Long {
    // Find Meal
    val meal = allMeals.find { it.id == entry.mealId } ?: return 0L
    
    // Find Components
    val components = allMealComponents.filter { it.mealId == meal.id }

    var totalCents = 0L

    for (comp in components) {
        if (comp.recipeId != null) {
            val recipe = allRecipes.find { it.id == comp.recipeId }
            if (recipe != null) {
                // Scaling Factor: "We are feeding 8, recipe serves 4" -> Factor 2.0
                val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0

                // Look up ingredients from the central repo
                val recipeIngredients = allRecipeIngredients.filter { it.recipeId == recipe.id }

                for (ri in recipeIngredients) {
                    val ingredient = allIngredients.find { it.id == ri.ingredientId }
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
            val ingredient = allIngredients.find { it.id == comp.ingredientId }
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
