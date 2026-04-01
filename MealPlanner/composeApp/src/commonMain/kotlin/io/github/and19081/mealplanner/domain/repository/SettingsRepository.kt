package io.github.and19081.mealplanner.domain.repository

import io.github.and19081.mealplanner.core.theme.AccentColor
import io.github.and19081.mealplanner.core.theme.AppTheme
import io.github.and19081.mealplanner.core.theme.CornerStyle
import io.github.and19081.mealplanner.data.db.entity.DashboardConfig
import io.github.and19081.mealplanner.feature.settings.AppSettings
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
