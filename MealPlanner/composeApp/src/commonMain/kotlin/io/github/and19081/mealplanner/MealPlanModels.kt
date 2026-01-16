@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, DESSERT, CUSTOM
}

/**
 * Represents a specific "Slot" on the calendar.
 * Example: "Lunch on Jan 15th, 2026 for 3 people"
 */
data class MealPlanEntry(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val mealType: MealType,
    // Scaling: If Recipe base is 4, but this is 2, we halve ingredients.
    val targetServings: Double
)

/**
 * What is actually IN that slot?
 * Usually a Recipe, but maybe just a raw Ingredient (e.g., "An Apple").
 */
data class ScheduledItem(
    val id: Uuid = Uuid.random(),
    val mealPlanEntryId: Uuid,

    // It can be a Recipe OR a standalone Ingredient
    val recipeId: Uuid? = null,
    val ingredientId: Uuid? = null
)