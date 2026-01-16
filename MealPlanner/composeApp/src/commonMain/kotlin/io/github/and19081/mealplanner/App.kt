package io.github.and19081.mealplanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.github.and19081.mealplanner.main.MainView
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
@Preview
fun App() {
    // Initialize Mock Data Once
    LaunchedEffect(Unit) {
        MockData.initialize()
    }

    MaterialTheme {
        MainView()
    }
}
