package io.github.and19081.mealplanner.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import io.github.and19081.mealplanner.App
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dbFile = applicationContext.getDatabasePath("meal_planner.db")
        val builder = Room.databaseBuilder<MealPlannerDatabase>(
            context = applicationContext,
            name = dbFile.absolutePath
        )

        setContent {
            App(builder)
        }
    }
}
