package io.github.and19081.mealplanner.domain.model

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Sealed class representing different sources of meals in the meal planner.
 * Provides type-safe representations for pre-planned meals, standalone recipes,
 * standalone ingredients, and restaurant consumption.
 */
@Serializable
sealed class MealSource {
    /**
     * A meal that was pre-planned in the system (Meal or Recipe type).
     */
    @Serializable
    data class PrePlannedMeal(val id: Uuid) : MealSource()

    /**
     * A standalone recipe used without being tied to a planned meal.
     */
    @Serializable
    data class StandaloneRecipe(val id: Uuid) : MealSource()

    /**
     * A standalone ingredient consumed for its nutritional value.
     */
    @Serializable
    data class StandaloneIngredient(
        val id: Uuid,
        val quantity: Double,
        val unitId: Uuid,
    ) : MealSource()

    /**
     * A restaurant meal that was consumed.
     */
    @Serializable
    data class Restaurant(
        val restaurantId: Uuid,
        val anticipatedCostCents: Int,
    ) : MealSource()
}
