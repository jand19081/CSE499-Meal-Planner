package io.github.and19081.mealplanner.domain.model

import kotlin.uuid.Uuid

/**
 * Sealed class representing the result of consuming a meal or ingredient.
 * Provides typed representations for different consumption scenarios.
 */
sealed class ConsumptionResult {

    /**
     * Represents consumption of a pre-planned meal.
     * @param mealSource The source reference to the pre-planned meal.
     * @param leftoverServings Optional remaining servings that can be saved as leftovers.
     */
    data class HomeMealConsumed(
        val mealSource: MealSource.PrePlannedMeal,
        val leftoverServings: Double? = null
    ) : ConsumptionResult()

    /**
     * Represents consumption of a standalone recipe.
     * @param recipeSource The source reference to the standalone recipe.
     * @param leftoverServings Optional remaining servings that can be saved as leftovers.
     */
    data class RecipeConsumed(
        val recipeSource: MealSource.StandaloneRecipe,
        val leftoverServings: Double? = null
    ) : ConsumptionResult()

    /**
     * Represents consumption of individual ingredients.
     * @param ingredientSource The source reference to the consumed ingredient.
     */
    data class IngredientConsumed(
        val ingredientSource: MealSource.StandaloneIngredient
    ) : ConsumptionResult()

    /**
     * Represents consumption at a restaurant where a receipt was saved.
     * @param receiptId Unique identifier for the saved receipt.
     * @param actualCostCents The actual cost of the meal in cents.
     * @param leftoverDescription Optional description of any leftovers from the restaurant meal.
     */
    data class RestaurantConsumptionSaved(
        val receiptId: Uuid,
        val actualCostCents: Int,
        val leftoverDescription: String? = null
    ) : ConsumptionResult()
}
