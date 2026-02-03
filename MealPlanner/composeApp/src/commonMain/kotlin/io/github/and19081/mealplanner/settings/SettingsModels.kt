package io.github.and19081.mealplanner.settings

import kotlin.uuid.Uuid

data class AppSettings(
    val id: Uuid = Uuid.random(),
    val mealConsumedNotificationDelayMinutes: Int = 30,
    val defaultTaxRatePercentage: Double = 0.0
)

data class DashboardConfig(
    val showWeeklyCost: Boolean = true,
    val showShoppingListSummary: Boolean = true,
    val showMealPlan: Boolean = true
)
