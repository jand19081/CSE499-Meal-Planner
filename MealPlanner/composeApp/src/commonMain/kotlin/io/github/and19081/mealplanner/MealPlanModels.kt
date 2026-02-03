package io.github.and19081.mealplanner

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.uuid.Uuid

data class Restaurant(
    val id: Uuid = Uuid.random(),
    val name: String
)

data class MealIngredient(
    val ingredientId: Uuid,
    val quantity: Double,
    val unitId: Uuid
)

data class PrePlannedMeal(
    val id: Uuid = Uuid.random(),
    val name: String,
    val recipes: List<Uuid> = emptyList(),
    val independentIngredients: List<MealIngredient> = emptyList()
)

data class ScheduledMeal(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val time: LocalTime? = null,
    val mealType: RecipeMealType, // Using existing Enum from Recipe.kt
    val prePlannedMealId: Uuid? = null,
    val restaurantId: Uuid? = null,
    val peopleCount: Int,
    val isConsumed: Boolean = false
)

data class PantryItem(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid,
    val quantity: Double,
    val unitId: Uuid
)