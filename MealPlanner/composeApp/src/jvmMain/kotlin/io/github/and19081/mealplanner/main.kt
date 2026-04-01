package io.github.and19081.mealplanner

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.room.Room
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun main() = application {
  val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
  val userHome = System.getProperty("user.home")
  val appDataDir = File(userHome, ".mealplanner")

  if (!appDataDir.exists()) {
    appDataDir.mkdirs()
  }

  val dbFile = File(appDataDir, "meal_planner.db")
  val builder = Room.databaseBuilder<MealPlannerDatabase>(name = dbFile.absolutePath)
  val db = remember { MealPlannerDatabase.getDatabase(builder) }
  val diContainer = remember { DependencyInjectionContainer(db, appScope) }

  Window(
      onCloseRequest = {
        appScope.cancel()
        exitApplication()
      },
      title = "MealPlanner",
  ) {
    App(diContainer)
  }
}
