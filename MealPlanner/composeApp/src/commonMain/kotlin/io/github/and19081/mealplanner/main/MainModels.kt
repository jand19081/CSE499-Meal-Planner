package io.github.and19081.mealplanner.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable


// 1. Define the Routes (The addresses)
@Serializable
object HomeRoute

@Serializable
object SettingsRoute

// 2. Define the Rail Items (The menu options)
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Any // The route object defined above
) {
    HOME("Home", Icons.Filled.Home, HomeRoute),
    SETTINGS("Settings", Icons.Filled.Settings, SettingsRoute)
}