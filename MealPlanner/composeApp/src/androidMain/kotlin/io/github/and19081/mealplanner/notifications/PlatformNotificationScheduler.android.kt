package io.github.and19081.mealplanner.notifications

import android.util.Log
import io.github.and19081.mealplanner.ScheduledMeal
import kotlin.uuid.Uuid

class AndroidMealNotificationScheduler : MealNotificationScheduler {
    override fun scheduleMealNotification(meal: ScheduledMeal, mealName: String) {
        // In a full production app, this would enqueue a WorkManager task
        // or set an AlarmManager intent based on meal.date and meal.time
        Log.d("MealNotification", "Scheduled notification for meal: \$mealName at \${meal.date}")
    }

    override fun cancelMealNotification(mealId: Uuid) {
        Log.d("MealNotification", "Cancelled notification for mealId: \$mealId")
    }
}

actual fun createNotificationScheduler(): MealNotificationScheduler = AndroidMealNotificationScheduler()
