package io.github.and19081.mealplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.theme.MealPlannerTheme
import io.github.and19081.mealplanner.feature.main.MainView
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App(dependencyInjectionContainer: DependencyInjectionContainer) {
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
