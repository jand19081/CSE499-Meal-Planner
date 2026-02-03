package io.github.and19081.mealplanner.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SettingsRepository {
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings = _appSettings.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    val theme = _theme.asStateFlow()
    
    private val _cornerStyle = MutableStateFlow(CornerStyle.SQUARE)
    val cornerStyle = _cornerStyle.asStateFlow()

    private val _accentColor = MutableStateFlow(AccentColor.GREEN)
    val accentColor = _accentColor.asStateFlow()

    private val _dashboardConfig = MutableStateFlow(DashboardConfig())
    val dashboardConfig = _dashboardConfig.asStateFlow()

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        _appSettings.update(update)
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }

    fun setCornerStyle(style: CornerStyle) {
        _cornerStyle.value = style
    }

    fun setAccentColor(color: AccentColor) {
        _accentColor.value = color
    }

    fun updateDashboardConfig(update: (DashboardConfig) -> DashboardConfig) {
        _dashboardConfig.update(update)
    }
}
