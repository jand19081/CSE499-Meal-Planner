package io.github.and19081.mealplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.room.RoomDatabase
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.theme.MealPlannerTheme
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.feature.main.MainView
import kotlin.uuid.ExperimentalUuidApi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App(dbBuilder: RoomDatabase.Builder<MealPlannerDatabase>, platformContext: Any? = null) {
  val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
  val db = remember { MealPlannerDatabase.getDatabase(dbBuilder) }
  val dependencyInjectionContainer = remember {
    DependencyInjectionContainer(
      db,
      appScope,
      platformContext
    )
  }

  LaunchedEffect(Unit) {
    try {
      dependencyInjectionContainer.initializeMockData()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  MealPlannerTheme(settingsRepository = dependencyInjectionContainer.settingsRepository) {
    MainView(diContainer = dependencyInjectionContainer)
  }
}
