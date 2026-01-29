package io.github.and19081.mealplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import io.github.and19081.mealplanner.main.MainView
import io.github.and19081.mealplanner.settings.MealPlannerTheme
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
@Preview
fun App() {
    // Initialize Mock Data Once
    LaunchedEffect(Unit) {
        MockData.initialize()
    }

    MealPlannerTheme {
        MainView()
    }
}
