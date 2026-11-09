package io.github.and19081.mealplanner

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.theme.MealPlannerTheme
import io.github.and19081.mealplanner.feature.main.MainView
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App(dependencyInjectionContainer: DependencyInjectionContainer) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            dependencyInjectionContainer.initializeMockData()
        } catch (e: Exception) {
            errorMessage = "Failed to initialize application data: ${e.message}"
        }
    }

    MealPlannerTheme(settingsRepository = dependencyInjectionContainer.settingsRepository) {
        Box {
            MainView(diContainer = dependencyInjectionContainer)

            errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Initialization Error") },
                    text = { Text(error) },
                    confirmButton = {
                        Button(onClick = { errorMessage = null }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}
