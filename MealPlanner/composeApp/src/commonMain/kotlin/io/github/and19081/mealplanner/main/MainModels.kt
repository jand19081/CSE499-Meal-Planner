package io.github.and19081.mealplanner.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable


// Define Routes
@Serializable
object HomeRoute

@Serializable
object SettingsRoute

@Serializable
object IngredientsRoute

// Define Rail Items
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Any // The route object defined above
) {
    HOME("Home", Icons.Filled.Home, HomeRoute),
    INGREDIENTS("Ingredients", Icons.Filled.Checklist, IngredientsRoute),
    SETTINGS("Settings", Icons.Filled.Settings, SettingsRoute)
}