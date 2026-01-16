@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, DESSERT, CUSTOM
}

/**
 * A Meal Definition.
 * Examples: "Taco Night", "Thanksgiving Dinner", "Quick Snack".
 * Consists of one or more Recipes and/or Ingredients.
 */
data class Meal(
    val id: Uuid = Uuid.random(),
    val name: String,
    val description: String? = null
)

/**
 * Items that make up a Meal Definition.
 */
data class MealComponent(
    val id: Uuid = Uuid.random(),
    val mealId: Uuid,

    val recipeId: Uuid? = null,
    val ingredientId: Uuid? = null,
    
    // For Ingredients: Amount per person? Or fixed amount?
    // Let's assume for now this is "Amount per Serving" if we scale, 
    // or just "Amount" if we don't scale ingredients nicely yet.
    // For Recipes: usually null (implies 1x base recipe serving).
    val quantity: Measure? = null
)

/**
 * Represents a specific "Slot" on the calendar.
 * Example: "Lunch on Jan 15th, 2026 for 3 people"
 */
data class MealPlanEntry(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val mealType: MealType,
    val targetServings: Double,
    
    // The defined meal being eaten
    val mealId: Uuid
)
