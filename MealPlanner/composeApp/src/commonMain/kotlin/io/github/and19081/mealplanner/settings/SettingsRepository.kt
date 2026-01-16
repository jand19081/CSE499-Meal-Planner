package io.github.and19081.mealplanner.settings

import kotlinx.coroutines.flow.MutableStateFlow

object SettingsRepository {
    // 0.08 = 8%
    val salesTaxRate = MutableStateFlow(0.0)
}