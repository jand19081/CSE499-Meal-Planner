package io.github.and19081.mealplanner.notifications

import kotlin.uuid.Uuid

class JvmMealNotificationScheduler : MealNotificationScheduler {
    override fun scheduleVerificationNotification(mealId: Uuid, mealName: String, delayMinutes: Long) {
        println("MealNotification: Scheduled verification for meal: \$mealName in \$delayMinutes minutes")
    }

    override fun cancelNotification(mealId: Uuid) {
        println("MealNotification: Cancelled notification for mealId: \$mealId")
    }
}

actual fun createNotificationScheduler(platformContext: Any?): MealNotificationScheduler = JvmMealNotificationScheduler()
