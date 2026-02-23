package io.github.and19081.mealplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.RoomDatabase
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.dependencyinjection.DependencyInjectionContainer
import io.github.and19081.mealplanner.main.MainView
import io.github.and19081.mealplanner.settings.MealPlannerTheme
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App(dbBuilder: RoomDatabase.Builder<MealPlannerDatabase>) {
    val scope = rememberCoroutineScope()
    val db = remember { MealPlannerDatabase.getDatabase(dbBuilder) }
    val dependencyInjectionContainer = remember { DependencyInjectionContainer(db, scope) }

    MealPlannerTheme(settingsRepository = dependencyInjectionContainer.settingsRepository) {
        MainView(diContainer = dependencyInjectionContainer)
    }
}
