package io.github.and19081.mealplanner.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.room.Room
import io.github.and19081.mealplanner.App
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled by system
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        val dbFile = applicationContext.getDatabasePath("meal_planner.db")
        val builder =
            Room.databaseBuilder<MealPlannerDatabase>(
                context = applicationContext,
                name = dbFile.absolutePath,
            )
        val db = MealPlannerDatabase.getDatabase(builder)
        val diContainer = DependencyInjectionContainer(db, appScope, applicationContext)

        // Handle possible deep link from notification
        val verifyMealId = intent.getStringExtra("VERIFY_MEAL_ID")
        if (verifyMealId != null) {
            // In a more complex app, we'd navigate to the verification screen
        }

        setContent { App(diContainer) }
    }

    override fun onDestroy() {
        super.onDestroy()
        appScope.cancel()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
