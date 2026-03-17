package io.github.and19081.mealplanner.notification

import kotlin.uuid.Uuid

interface MealNotificationScheduler {
    /**
     * Schedules a verification notification for a specific meal.
     * @param mealId The ID of the ScheduledMeal.
     * @param mealName The name of the meal for the notification text.
     * @param delayMinutes The user-configured delay (e.g., 30 minutes) after the scheduled time.
     */
    fun scheduleVerificationNotification(mealId: Uuid, mealName: String, delayMinutes: Long)
    
    /**
     * Schedules a notification reminding the user to start cooking.
     * @param mealId The ID of the ScheduledMeal.
     * @param mealName The name of the meal.
     * @param triggerTimeMillis The exact epoch timestamp (in milliseconds) when the user should start cooking.
     */
    fun scheduleStartCookingNotification(mealId: Uuid, mealName: String, triggerTimeMillis: Long)
    
    fun cancelNotification(mealId: Uuid)
}

expect fun createNotificationScheduler(platformContext: Any? = null): MealNotificationScheduler
