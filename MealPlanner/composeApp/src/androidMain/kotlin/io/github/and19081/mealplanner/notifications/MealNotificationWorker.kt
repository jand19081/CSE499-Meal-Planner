package io.github.and19081.mealplanner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MealNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val mealId = inputData.getString("MEAL_ID") ?: return Result.failure()
        val mealName = inputData.getString("MEAL_NAME") ?: "your meal"

        showNotification(mealId, mealName)
        return Result.success()
    }

    private fun showNotification(mealId: String, mealName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_verification_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Meal Verification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to verify if you ate your scheduled meals."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Use package manager to get the launch intent for the app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("VERIFY_MEAL_ID", mealId)
        }

        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                context,
                mealId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Did you eat $mealName?")
            .setContentText("Tap to confirm consumption and update your pantry.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .setAutoCancel(true)
            .build()

        notificationManager.notify(mealId.hashCode(), notification)
    }
}
