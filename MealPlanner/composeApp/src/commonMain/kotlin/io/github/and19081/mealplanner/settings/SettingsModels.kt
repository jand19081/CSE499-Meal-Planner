package io.github.and19081.mealplanner.settings

import androidx.compose.ui.graphics.Color

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class CornerStyle {
    ROUNDED, SQUARE
}

enum class AccentColor(val color: Color, val label: String) {
    BLUE(Color(0xFF1976D2), "Blue"),
    GREEN(Color(0xFF388E3C), "Green"),
    ORANGE(Color(0xFFFFA000), "Orange"),
    PURPLE(Color(0xFF8E24AA), "Purple"),
    RED(Color(0xFFD32F2F), "Red"),
    TEAL(Color(0xFF009688), "Teal")
}

data class DashboardConfig(
    val showWeeklyCost: Boolean = false,
    val showMealPlan: Boolean = true,
    val showShoppingListSummary: Boolean = true
)

data class NotificationConfig(
    val mealConsumedDelayMinutes: Int = 30
)