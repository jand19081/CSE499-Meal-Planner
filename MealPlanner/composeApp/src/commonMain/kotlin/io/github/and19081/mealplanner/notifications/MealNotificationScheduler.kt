package io.github.and19081.mealplanner.notifications

import io.github.and19081.mealplanner.ScheduledMeal
import kotlin.uuid.Uuid

interface MealNotificationScheduler {
    fun scheduleMealNotification(meal: ScheduledMeal, mealName: String)
    fun cancelMealNotification(mealId: Uuid)
}

expect fun createNotificationScheduler(): MealNotificationScheduler
