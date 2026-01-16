package io.github.and19081.mealplanner.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable


// Define Routes
@Serializable
object CalendarRoute
@Serializable
object HomeRoute

@Serializable
object RecipesRoute

@Serializable
object IngredientsRoute

@Serializable
object MealsRoute

@Serializable
object SettingsRoute

@Serializable
object ShoppingListRoute

// Define Rail Items
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Any // The route object defined above
) {
    HOME("Home", Icons.Filled.Home, HomeRoute),
    CALENDAR("Calendar", Icons.Default.CalendarMonth, CalendarRoute),
    INGREDIENTS("Ingredients", Icons.Filled.Checklist, IngredientsRoute),
    MEALS("Meals", Icons.Default.Dining, MealsRoute),
    RECIPES("Recipes", Icons.Default.Restaurant, RecipesRoute),
    SHOPPINGLIST("Shopping List", Icons.Default.ShoppingCart, ShoppingListRoute),
    SETTINGS("Settings", Icons.Filled.Settings, SettingsRoute)
}