package io.github.and19081.mealplanner.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.theme.AccentColor
import io.github.and19081.mealplanner.core.theme.AppTheme
import io.github.and19081.mealplanner.core.theme.CornerStyle
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.data.db.entity.DashboardConfig
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import io.github.and19081.mealplanner.feature.meals.Restaurant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val foodItemRepository: FoodItemRepository,
    private val storeRepository: StoreRepository,
    private val restaurantRepository: RestaurantRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  val uiState =
      combine(
              settingsRepository.appSettings,
              settingsRepository.theme,
              settingsRepository.cornerStyle,
              settingsRepository.accentColor,
              settingsRepository.dashboardConfig,
              foodItemRepository.categories,
              storeRepository.stores,
              restaurantRepository.restaurants,
              unitRepository.units,
          ) { args: Array<Any> ->
            val appSettings = args[0] as AppSettings
            SettingsUiState(
                taxRate = appSettings.defaultTaxRatePercentage,
                appTheme = args[1] as AppTheme,
                cornerStyle = args[2] as CornerStyle,
                accentColor = args[3] as AccentColor,
                dashboardConfig = args[4] as DashboardConfig,
                notificationDelayMinutes = appSettings.mealConsumedNotificationDelayMinutes,
                appMode = appSettings.view,
                allCategories = args[5] as List<Category>,
                allStores = args[6] as List<Store>,
                allRestaurants = args[7] as List<Restaurant>,
                allUnits = args[8] as List<UnitModel>,
            )
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

  fun updateTaxRate(rate: Double) {
    settingsRepository.updateSettings { it.copy(defaultTaxRatePercentage = rate) }
  }

  fun setAppMode(mode: Mode) {
    settingsRepository.updateSettings { it.copy(view = mode) }
  }

  fun setTheme(theme: AppTheme) {
    settingsRepository.setTheme(theme)
  }

  fun setCornerStyle(style: CornerStyle) {
    settingsRepository.setCornerStyle(style)
  }

  fun setAccentColor(color: AccentColor) {
    settingsRepository.setAccentColor(color)
  }

  fun toggleShowWeeklyCost(show: Boolean) {
    settingsRepository.updateDashboardConfig { it.copy(showWeeklyCost = show) }
  }

  fun toggleShowMealPlan(show: Boolean) {
    settingsRepository.updateDashboardConfig { it.copy(showMealPlan = show) }
  }

  fun toggleShowShoppingList(show: Boolean) {
    settingsRepository.updateDashboardConfig { it.copy(showShoppingListSummary = show) }
  }

  fun setNotificationDelay(minutes: Int) {
    settingsRepository.updateSettings { it.copy(mealConsumedNotificationDelayMinutes = minutes) }
  }

  // --- Data Management Actions ---

  fun saveStore(store: Store) {
    viewModelScope.launch {
      if (uiState.value.allStores.any { it.id == store.id }) {
        storeRepository.updateStore(store)
      } else {
        storeRepository.addStore(store)
      }
    }
  }

  fun deleteStore(id: Uuid) {
    viewModelScope.launch { storeRepository.deleteStore(id) }
  }

  fun saveCategory(category: Category) {
    viewModelScope.launch {
        foodItemRepository.saveCategory(category)
    }
  }

  fun deleteCategory(id: Uuid) {
    viewModelScope.launch { foodItemRepository.deleteCategory(id) }
  }

  fun saveRestaurant(restaurant: Restaurant) {
    viewModelScope.launch {
      if (uiState.value.allRestaurants.any { it.id == restaurant.id }) {
        restaurantRepository.updateRestaurant(restaurant)
      } else {
        restaurantRepository.addRestaurant(restaurant)
      }
    }
  }

  fun deleteRestaurant(id: Uuid) {
    viewModelScope.launch { restaurantRepository.deleteRestaurant(id) }
  }

  fun saveUnit(unit: UnitModel) {
    viewModelScope.launch {
      if (uiState.value.allUnits.any { it.id == unit.id }) {
        unitRepository.updateUnit(unit)
      } else {
        unitRepository.addUnit(unit)
      }
    }
  }

  fun deleteUnit(id: Uuid) {
    viewModelScope.launch { unitRepository.deleteUnit(id) }
  }
}

data class SettingsUiState(
    val taxRate: Double = 0.0,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val cornerStyle: CornerStyle = CornerStyle.SQUARE,
    val accentColor: AccentColor = AccentColor.GREEN,
    val dashboardConfig: DashboardConfig = DashboardConfig(),
    val notificationDelayMinutes: Int = 30,
    val appMode: Mode = Mode.AUTO,
    val allCategories: List<Category> = emptyList(),
    val allStores: List<Store> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
)
