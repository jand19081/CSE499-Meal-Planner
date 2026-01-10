package io.github.and19081.mealplanner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MealPlanner",
    ) {
        App()
    }
}