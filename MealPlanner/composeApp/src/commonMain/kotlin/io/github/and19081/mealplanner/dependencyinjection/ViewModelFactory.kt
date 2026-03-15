package io.github.and19081.mealplanner.dependencyinjection

import io.github.and19081.mealplanner.analytics.AnalyticsViewModel
import io.github.and19081.mealplanner.calendar.CalendarViewModel
import io.github.and19081.mealplanner.dashboard.DashboardViewModel
import io.github.and19081.mealplanner.settings.SettingsViewModel
import kotlinx.coroutines.flow.StateFlow

class ViewModelFactory(private val di: DependencyInjectionContainer) {
    fun createDashboardViewModel() = DashboardViewModel(
        di.mealPlanRepository,
        di.mealRepository,
        di.recipeRepository,
        di.ingredientRepository,
        di.pantryRepository,
        di.unitRepository,
        di.settingsRepository,
        di.receiptHistoryRepository,
        di.shoppingListItemRepository,
        di.restaurantRepository,
    )

    fun createCalendarViewModel(currentMonthFlow: StateFlow<kotlinx.datetime.LocalDate>) = CalendarViewModel(
        currentMonthFlow = currentMonthFlow,
        mealPlanRepository = di.mealPlanRepository,
        mealRepository = di.mealRepository,
        recipeRepository = di.recipeRepository,
        ingredientRepository = di.ingredientRepository,
        pantryRepository = di.pantryRepository,
        leftoverRepository = di.leftoverRepository,
        unitRepository = di.unitRepository,
        restaurantRepository = di.restaurantRepository,
        notificationScheduler = di.notificationScheduler,
    )

    fun createAnalyticsViewModel() = AnalyticsViewModel(
        di.mealPlanRepository,
        di.mealRepository,
        di.recipeRepository,
        di.ingredientRepository,
        di.receiptHistoryRepository,
        di.storeRepository,
        di.unitRepository,
        di.restaurantRepository,
    )

    fun createSettingsViewModel() = SettingsViewModel(
        di.settingsRepository,
        di.ingredientRepository,
        di.storeRepository,
        di.restaurantRepository,
        di.unitRepository,
    )
}
