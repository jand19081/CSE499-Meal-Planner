package io.github.and19081.mealplanner.core.util

import kotlinx.serialization.Serializable

@Serializable
enum class RecipeMealType {
  Breakfast,
  Lunch,
  Dinner,
  Side,
  Snack,
  Other,
}
