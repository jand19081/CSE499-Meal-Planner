package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Ingredient

// Logic
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
                        val requiredAmount = ri.quantity.amount * scale

                        // TODO: Implement actual best option logic
                        val bestOption = ingredient.purchaseOptions.minByOrNull { it.priceCents }

                        if (bestOption != null) {
                            val pricePerUnit = bestOption.priceCents / bestOption.quantity.amount
                            totalCents += (pricePerUnit * requiredAmount).toLong()
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
                    val pricePerUnit = bestOption.priceCents / bestOption.quantity.amount
                    totalCents += (pricePerUnit * requiredAmount).toLong()
                }
            }
        }
    }
    return totalCents
}