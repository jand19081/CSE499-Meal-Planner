package io.github.and19081.mealplanner.feature.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Define Routes
@Serializable
object CalendarRoute

@Serializable
object DashboardRoute

@Serializable
object RecipesRoute

@Serializable
object IngredientsRoute

@Serializable
object MealsRoute

@Serializable
object SettingsRoute

@Serializable
object InventoryRoute

@Serializable
object PantryRoute

@Serializable
object AnalyticsRoute

@Serializable
object ReceiptsRoute

@Serializable
object KitchenRoute

@Serializable
object RecipeExecutionRoute

data class TopLevelDestination(val label: String, val icon: ImageVector, val route: Any)

val MainDestinations =
    listOf(
        TopLevelDestination("Dashboard", Icons.Filled.Home, DashboardRoute),
        TopLevelDestination("Calendar", Icons.Default.CalendarMonth, CalendarRoute),
        TopLevelDestination("Inventory", Icons.Default.Inventory, InventoryRoute),
        TopLevelDestination("Kitchen", Icons.Default.Dining, KitchenRoute),
    )

val AllDestinations =
    MainDestinations +
            listOf(
                TopLevelDestination("Receipts", Icons.Default.Receipt, ReceiptsRoute),
                TopLevelDestination("Analytics", Icons.Default.Analytics, AnalyticsRoute),
                TopLevelDestination("Settings", Icons.Default.Settings, SettingsRoute),
            )
