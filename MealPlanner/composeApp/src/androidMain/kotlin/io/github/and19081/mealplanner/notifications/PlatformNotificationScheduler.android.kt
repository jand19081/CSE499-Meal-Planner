package io.github.and19081.mealplanner.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

class AndroidMealNotificationScheduler(private val context: Context) : MealNotificationScheduler {

    override fun scheduleVerificationNotification(mealId: Uuid, mealName: String, delayMinutes: Long) {
        val workData = Data.Builder()
            .putString("MEAL_ID", mealId.toString())
            .putString("MEAL_NAME", mealName)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MealNotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workData)
            .addTag(mealId.toString())
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    override fun cancelNotification(mealId: Uuid) {
        WorkManager.getInstance(context).cancelAllWorkByTag(mealId.toString())
    }
}

actual fun createNotificationScheduler(platformContext: Any?): MealNotificationScheduler {
    val context = platformContext as? Context ?: throw IllegalArgumentException("AndroidMealNotificationScheduler requires a Context")
    return AndroidMealNotificationScheduler(context)
}
