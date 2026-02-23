package io.github.and19081.mealplanner.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Define Routes
@Serializable object CalendarRoute
@Serializable object DashboardRoute
@Serializable object RecipesRoute
@Serializable object IngredientsRoute
@Serializable object MealsRoute
@Serializable object SettingsRoute
@Serializable object ShoppingListRoute
@Serializable object PantryRoute
@Serializable object AnalyticsRoute

data class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

val MainDestinations = listOf(
    TopLevelDestination("Dashboard", Icons.Filled.Home, DashboardRoute),
    TopLevelDestination("Calendar", Icons.Default.CalendarMonth, CalendarRoute),
    TopLevelDestination("Ingredients", Icons.Filled.Checklist, IngredientsRoute),
    TopLevelDestination("Meals", Icons.Default.Dining, MealsRoute),
    TopLevelDestination("Recipes", Icons.Default.Restaurant, RecipesRoute),
    TopLevelDestination("Shopping List", Icons.Default.ShoppingCart, ShoppingListRoute),
    TopLevelDestination("Pantry", Icons.Default.Inventory, PantryRoute),
    TopLevelDestination("Analytics", Icons.Default.Analytics, AnalyticsRoute),
    TopLevelDestination("Settings", Icons.Filled.Settings, SettingsRoute)
)
