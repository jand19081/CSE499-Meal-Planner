package io.github.and19081.mealplanner.feature.settings

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
enum class Mode {
  AUTO,
  DESKTOP,
  MOBILE,
}

@Serializable
data class AppSettings(
    val id: Uuid = Uuid.random(),
    val isFirstLaunch: Boolean = true,
    val mealConsumedNotificationDelayMinutes: Int = 30,
    val defaultTaxRatePercentage: Double = 0.0,
    val view: Mode = Mode.AUTO,
)

@Serializable
data class DashboardConfig(
    val showWeeklyCost: Boolean = true,
    val showShoppingListSummary: Boolean = true,
    val showMealPlan: Boolean = true,
)
