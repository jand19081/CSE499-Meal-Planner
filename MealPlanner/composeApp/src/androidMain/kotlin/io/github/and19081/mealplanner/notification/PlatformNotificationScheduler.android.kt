package io.github.and19081.mealplanner.notification

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

class AndroidMealNotificationScheduler(private val context: Context) : MealNotificationScheduler {

  override fun scheduleVerificationNotification(
      mealId: Uuid,
      mealName: String,
      delayMinutes: Long,
  ) {
    val workData =
        Data.Builder()
            .putString("MEAL_ID", mealId.toString())
            .putString("MEAL_NAME", mealName)
            .putString("NOTIFICATION_TYPE", "VERIFICATION")
            .build()

    val workRequest =
        OneTimeWorkRequestBuilder<MealNotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workData)
            .addTag("verify_${mealId}")
            .build()

    WorkManager.getInstance(context).enqueue(workRequest)
  }

  override fun scheduleStartCookingNotification(
      mealId: Uuid,
      mealName: String,
      triggerTimeMillis: Long,
  ) {
    val currentTimeMillis = System.currentTimeMillis()
    val delayMillis = triggerTimeMillis - currentTimeMillis

    if (delayMillis <= 0) return

    val workData =
        Data.Builder()
            .putString("MEAL_ID", mealId.toString())
            .putString("MEAL_NAME", mealName)
            .putString("NOTIFICATION_TYPE", "START_COOKING")
            .build()

    val workRequest =
        OneTimeWorkRequestBuilder<MealNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .addTag("start_cooking_${mealId}")
            .build()

    WorkManager.getInstance(context).enqueue(workRequest)
  }

  override fun cancelNotification(mealId: Uuid) {
    WorkManager.getInstance(context).cancelAllWorkByTag("verify_${mealId}")
    WorkManager.getInstance(context).cancelAllWorkByTag("start_cooking_${mealId}")
  }
}

actual fun createNotificationScheduler(platformContext: Any?): MealNotificationScheduler {
  val context =
      platformContext as? Context
          ?: throw IllegalArgumentException("AndroidMealNotificationScheduler requires a Context")
  return AndroidMealNotificationScheduler(context)
}
