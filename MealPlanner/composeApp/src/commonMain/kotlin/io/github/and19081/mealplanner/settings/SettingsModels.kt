package io.github.and19081.mealplanner.settings

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class Mode {
    AUTO,
    DESKTOP,
    MOBILE
}

@Serializable
data class AppSettings(
    val id: Uuid = Uuid.random(),
    val mealConsumedNotificationDelayMinutes: Int = 30,
    val defaultTaxRatePercentage: Double = 0.0,
    val view: Mode = Mode.AUTO
)

@Serializable
data class DashboardConfig(
    val showWeeklyCost: Boolean = true,
    val showShoppingListSummary: Boolean = true,
    val showMealPlan: Boolean = true
)
