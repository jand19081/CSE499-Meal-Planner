package io.github.and19081.mealplanner.calendar

import io.github.and19081.mealplanner.MealType
import kotlin.uuid.Uuid

data class CalendarEvent(
    val entryId: Uuid,
    val mealType: MealType,
    val title: String,
    val servings: Double,
    val isConsumed: Boolean = false
)