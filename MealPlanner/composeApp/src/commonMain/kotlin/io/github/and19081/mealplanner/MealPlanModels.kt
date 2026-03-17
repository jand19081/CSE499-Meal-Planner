package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.domain.FoodItem
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable data class Restaurant(val id: Uuid, val name: String)

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
    val foodItemId: Uuid,
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

data class PriceUpdate(val foodItemId: Uuid, val priceCents: Int)
