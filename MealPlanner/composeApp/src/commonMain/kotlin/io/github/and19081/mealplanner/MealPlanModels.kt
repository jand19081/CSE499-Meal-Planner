package io.github.and19081.mealplanner

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable data class Restaurant(val id: Uuid, val name: String)

@Serializable
data class MealIngredient(val ingredientId: Uuid, val quantity: Double, val unitId: Uuid)

@Serializable
data class PrePlannedMeal(
    val id: Uuid,
    val name: String,
    val mealType: RecipeMealType? = null,
    val recipes: List<Uuid> = emptyList(),
    val independentIngredients: List<MealIngredient> = emptyList(),
)

@Serializable
data class ScheduledMeal(
    val id: Uuid,
    val date: LocalDate,
    val time: LocalTime,
    val mealType: RecipeMealType,
    val prePlannedMealId: Uuid? = null,
    val restaurantId: Uuid? = null,
    val peopleCount: Int,
    val isConsumed: Boolean = false,
    val anticipatedCostCents: Int? = null,
)

@Serializable
data class PantryItem(
    val id: Uuid,
    val ingredientId: Uuid,
    val quantity: Double,
    val unitId: Uuid,
)

@Serializable
data class LeftoverItem(
    val id: Uuid,
    val recipeId: Uuid,
    val remainingServings: Double,
    val dateAdded: String,
    val expirationDate: String? = null,
)
