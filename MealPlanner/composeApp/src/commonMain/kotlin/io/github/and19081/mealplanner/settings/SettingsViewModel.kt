package io.github.and19081.mealplanner.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel : ViewModel() {

    val uiState = combine(
        SettingsRepository.salesTaxRate,
        SettingsRepository.theme,
        SettingsRepository.cornerStyle,
        SettingsRepository.accentColor,
        SettingsRepository.dashboardConfig,
        SettingsRepository.notificationConfig
    ) { args: Array<Any> ->
        SettingsUiState(
            taxRate = args[0] as Double,
            appTheme = args[1] as AppTheme,
            cornerStyle = args[2] as CornerStyle,
            accentColor = args[3] as AccentColor,
            dashboardConfig = args[4] as DashboardConfig,
            notificationConfig = args[5] as NotificationConfig
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateTaxRate(rate: Double) {
        SettingsRepository.salesTaxRate.value = rate
    }

    fun setTheme(theme: AppTheme) {
        SettingsRepository.setTheme(theme)
    }

    fun setCornerStyle(style: CornerStyle) {
        SettingsRepository.setCornerStyle(style)
    }

    fun setAccentColor(color: AccentColor) {
        SettingsRepository.setAccentColor(color)
    }

    fun toggleShowWeeklyCost(show: Boolean) {
        SettingsRepository.updateDashboardConfig { it.copy(showWeeklyCost = show) }
    }

    fun toggleShowMealPlan(show: Boolean) {
        SettingsRepository.updateDashboardConfig { it.copy(showMealPlan = show) }
    }

    fun toggleShowShoppingList(show: Boolean) {
        SettingsRepository.updateDashboardConfig { it.copy(showShoppingListSummary = show) }
    }

    fun setNotificationDelay(minutes: Int) {
        SettingsRepository.setNotificationDelay(minutes)
    }
}

data class SettingsUiState(
    val taxRate: Double = 0.0,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val cornerStyle: CornerStyle = CornerStyle.SQUARE,
    val accentColor: AccentColor = AccentColor.GREEN,
    val dashboardConfig: DashboardConfig = DashboardConfig(),
    val notificationConfig: NotificationConfig = NotificationConfig()
)
