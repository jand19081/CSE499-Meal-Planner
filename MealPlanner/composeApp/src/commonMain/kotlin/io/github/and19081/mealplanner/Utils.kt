@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlin.uuid.ExperimentalUuidApi

// Logic
fun calculateEstimatedCost(entry: MealPlanEntry, allRecipes: List<Recipe>, allIngredients: List<Ingredient>): Long {
    // Find all items scheduled for this meal
    val items = MealPlanRepository.scheduledItems.value.filter { it.mealPlanEntryId == entry.id }

    var totalCents = 0L

    for (item in items) {
        if (item.recipeId != null) {
            val recipe = allRecipes.find { it.id == item.recipeId }
            if (recipe != null) {
                // Scaling Factor: "We are feeding 8, recipe serves 4" -> Factor 2.0
                val scale = entry.targetServings / recipe.baseServings

                // Look up ingredients from the central repo
                val recipeIngredients = MealPlannerRepository.recipeIngredients.filter { it.recipeId == recipe.id }

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
        }
    }
    return totalCents
}