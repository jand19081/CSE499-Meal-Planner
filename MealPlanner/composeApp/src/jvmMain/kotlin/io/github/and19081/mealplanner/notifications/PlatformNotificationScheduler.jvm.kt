package io.github.and19081.mealplanner.notifications

import io.github.and19081.mealplanner.ScheduledMeal
import kotlin.uuid.Uuid

class JvmMealNotificationScheduler : MealNotificationScheduler {
    override fun scheduleMealNotification(meal: ScheduledMeal, mealName: String) {
        println("MealNotification: Scheduled notification for meal: \$mealName at \${meal.date}")
    }

    override fun cancelMealNotification(mealId: Uuid) {
        println("MealNotification: Cancelled notification for mealId: \$mealId")
    }
}

actual fun createNotificationScheduler(): MealNotificationScheduler = JvmMealNotificationScheduler()
