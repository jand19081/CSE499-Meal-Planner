package io.github.and19081.mealplanner.feature.meals

import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.model.NamedReference
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(override val id: Uuid, override val name: String) : NamedReference

@Serializable
data class ScheduledMeal(
    val id: Uuid,
    val date: LocalDate,
    val time: LocalTime,
    val mealType: RecipeMealType,
    val source: MealSource? = null,
    val peopleCount: Int,
    val isConsumed: Boolean = false,
    val anticipatedCostCents: Int? = null,
) {
  val prePlannedMealId: Uuid?
    get() =
        when (source) {
          is MealSource.PrePlannedMeal -> source.id
          is MealSource.StandaloneRecipe -> source.id
          is MealSource.StandaloneIngredient -> source.id
          else -> null
        }

  val restaurantId: Uuid?
    get() = (source as? MealSource.Restaurant)?.restaurantId
}

@Serializable
data class PantryItem(
    val id: Uuid,
    val measurement: ItemMeasurement,
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
