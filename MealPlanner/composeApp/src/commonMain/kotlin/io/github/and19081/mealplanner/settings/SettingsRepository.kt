package io.github.and19081.mealplanner.settings

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val appSettings: StateFlow<AppSettings>
    val theme: StateFlow<AppTheme>
    val cornerStyle: StateFlow<CornerStyle>
    val accentColor: StateFlow<AccentColor>
    val dashboardConfig: StateFlow<DashboardConfig>

    fun updateSettings(update: (AppSettings) -> AppSettings)
    fun setTheme(theme: AppTheme)
    fun setCornerStyle(style: CornerStyle)
    fun setAccentColor(color: AccentColor)
    fun updateDashboardConfig(update: (DashboardConfig) -> DashboardConfig)
}
