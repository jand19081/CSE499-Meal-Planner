package io.github.and19081.mealplanner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.room.Room
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import java.io.File

fun main() = application {
    val userHome = System.getProperty("user.home")
    val appDataDir = File(userHome, ".mealplanner")
    
    if (!appDataDir.exists()) {
        appDataDir.mkdirs()
    }

    val dbFile = File(appDataDir, "meal_planner.db")
    
    val builder = Room.databaseBuilder<MealPlannerDatabase>(
        name = dbFile.absolutePath
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "MealPlanner",
    ) {
        App(builder)
    }
}
