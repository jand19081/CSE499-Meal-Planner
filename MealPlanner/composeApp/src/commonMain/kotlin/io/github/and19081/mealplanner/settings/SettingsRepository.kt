package io.github.and19081.mealplanner.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SettingsRepository {
    // 0.08 = 8%
    val salesTaxRate = MutableStateFlow(0.0)

    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    val theme = _theme.asStateFlow()
    
    private val _cornerStyle = MutableStateFlow(CornerStyle.SQUARE)
    val cornerStyle = _cornerStyle.asStateFlow()

    private val _accentColor = MutableStateFlow(AccentColor.GREEN)
    val accentColor = _accentColor.asStateFlow()

    private val _dashboardConfig = MutableStateFlow(DashboardConfig())
    val dashboardConfig = _dashboardConfig.asStateFlow()

    private val _notificationConfig = MutableStateFlow(NotificationConfig())
    val notificationConfig = _notificationConfig.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }

    fun setCornerStyle(style: CornerStyle) {
        _cornerStyle.value = style
    }

    fun setAccentColor(color: AccentColor) {
        _accentColor.value = color
    }

    fun setDashboardConfig(config: DashboardConfig) {
        _dashboardConfig.value = config
    }
    
    fun updateDashboardConfig(update: (DashboardConfig) -> DashboardConfig) {
        _dashboardConfig.update(update)
    }

    fun setNotificationDelay(minutes: Int) {
        _notificationConfig.update { it.copy(mealConsumedDelayMinutes = minutes) }
    }
}