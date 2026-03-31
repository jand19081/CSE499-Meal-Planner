package io.github.and19081.mealplanner.feature.kitchen

import kotlin.uuid.Uuid

/**
 * Represents the outcome of a meal consumption confirmation.
 * Mirrors the four MealSource variants — each branch carries only
 * the data that variant actually needs.
 */
sealed class ConsumptionResult {

    /** Consuming a pre-planned FoodItem.Meal. Deducts all nested ingredients. */
    data class HomeMealConsumed(
        val scheduledMealId: Uuid,
        val leftoverServings: Double = 0.0,
    ) : ConsumptionResult()

    /** Consuming a FoodItem.Recipe scheduled directly. Same deduction logic as Meal. */
    data class HomeRecipeConsumed(
        val scheduledMealId: Uuid,
        val leftoverServings: Double = 0.0,
    ) : ConsumptionResult()

    /** Consuming a standalone ingredient (e.g. an apple). Deducts exact quantity. */
    data class HomeIngredientConsumed(
        val scheduledMealId: Uuid,
    ) : ConsumptionResult()

    /** Recording a restaurant meal. No pantry deduction; saves a receipt. */
    data class RestaurantMealConsumed(
        val scheduledMealId: Uuid,
        val actualCostCents: Int,
        val leftoverDescription: String? = null,
        val leftoverServings: Double = 0.0,
    ) : ConsumptionResult()
}
