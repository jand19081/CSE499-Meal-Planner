package io.github.and19081.mealplanner.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel : ViewModel() {

    val uiState = combine(
        SettingsRepository.appSettings,
        SettingsRepository.theme,
        SettingsRepository.cornerStyle,
        SettingsRepository.accentColor,
        SettingsRepository.dashboardConfig
    ) { args: Array<Any> ->
        val appSettings = args[0] as AppSettings
        SettingsUiState(
            taxRate = appSettings.defaultTaxRatePercentage,
            appTheme = args[1] as AppTheme,
            cornerStyle = args[2] as CornerStyle,
            accentColor = args[3] as AccentColor,
            dashboardConfig = args[4] as DashboardConfig,
            notificationDelayMinutes = appSettings.mealConsumedNotificationDelayMinutes,
            appMode = appSettings.view
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateTaxRate(rate: Double) {
        SettingsRepository.updateSettings { it.copy(defaultTaxRatePercentage = rate) }
    }

    fun setAppMode(mode: Mode) {
        SettingsRepository.updateSettings { it.copy(view = mode) }
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
        SettingsRepository.updateSettings { it.copy(mealConsumedNotificationDelayMinutes = minutes) }
    }
}

data class SettingsUiState(
    val taxRate: Double = 0.0,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val cornerStyle: CornerStyle = CornerStyle.SQUARE,
    val accentColor: AccentColor = AccentColor.GREEN,
    val dashboardConfig: DashboardConfig = DashboardConfig(),
    val notificationDelayMinutes: Int = 30,
    val appMode: Mode = Mode.AUTO
)