package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.AppSettingsEntity
import io.github.and19081.mealplanner.data.db.entity.DashboardConfig as DbDashboardConfig
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.settings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoomSettingsRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : SettingsRepository {

    private val dao = db.appSettingsDao()

    private val settingsFlow: StateFlow<AppSettingsEntity> = dao.observe()
        .map { it ?: AppSettingsEntity() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    override val appSettings: StateFlow<AppSettings> = settingsFlow
        .map { it.toModel() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), AppSettings())

    override val theme: StateFlow<AppTheme> = settingsFlow
        .map { it.themePreference }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    override val cornerStyle: StateFlow<CornerStyle> = settingsFlow
        .map { entity ->
            try { CornerStyle.valueOf(entity.cornerStyle) } catch(e: Exception) { CornerStyle.ROUNDED }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), CornerStyle.ROUNDED)

    override val accentColor: StateFlow<AccentColor> = settingsFlow
        .map { entity ->
            try { AccentColor.valueOf(entity.accentColor) } catch(e: Exception) { AccentColor.GREEN }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), AccentColor.GREEN)

    override val dashboardConfig: StateFlow<DashboardConfig> = settingsFlow
        .map { it.dashboard.toModel() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), DashboardConfig())

    override fun updateSettings(update: (AppSettings) -> AppSettings) {
        scope.launch {
            val current = settingsFlow.value
            val model = update(current.toModel())
            dao.upsert(current.copy(
                appMode = model.view,
                defaultTaxRatePercentage = model.defaultTaxRatePercentage
            ))
        }
    }

    override fun setTheme(theme: AppTheme) {
        scope.launch {
            val current = settingsFlow.value
            dao.upsert(current.copy(themePreference = theme))
        }
    }

    override fun setCornerStyle(style: CornerStyle) {
        scope.launch {
            val current = settingsFlow.value
            dao.upsert(current.copy(cornerStyle = style.name))
        }
    }

    override fun setAccentColor(color: AccentColor) {
        scope.launch {
            val current = settingsFlow.value
            dao.upsert(current.copy(accentColor = color.name))
        }
    }

    override fun updateDashboardConfig(update: (DashboardConfig) -> DashboardConfig) {
        scope.launch {
            val current = settingsFlow.value
            val model = update(current.dashboard.toModel())
            dao.upsert(current.copy(
                dashboard = DbDashboardConfig(
                    showWeeklyCost = model.showWeeklyCost,
                    showShoppingListSummary = model.showShoppingListSummary,
                    showMealPlan = model.showMealPlan
                )
            ))
        }
    }
}
