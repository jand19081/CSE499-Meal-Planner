package io.github.and19081.mealplanner.dependencyinjection

import io.github.and19081.mealplanner.analytics.AnalyticsViewModel
import io.github.and19081.mealplanner.calendar.CalendarViewModel
import io.github.and19081.mealplanner.dashboard.DashboardViewModel
import io.github.and19081.mealplanner.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.meals.MealsViewModel
import io.github.and19081.mealplanner.pantry.PantryViewModel
import io.github.and19081.mealplanner.recipes.RecipesViewModel
import io.github.and19081.mealplanner.settings.SettingsViewModel
import io.github.and19081.mealplanner.shoppinglist.ShoppingListViewModel
import kotlinx.coroutines.flow.StateFlow

class ViewModelFactory(private val di: DependencyInjectionContainer) {
    fun createDashboardViewModel() = DashboardViewModel(
        di.mealPlanRepository,
        di.foodItemRepository,
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
        foodItemRepository = di.foodItemRepository,
        pantryRepository = di.pantryRepository,
        unitRepository = di.unitRepository,
        restaurantRepository = di.restaurantRepository,
        settingsRepository = di.settingsRepository,
        notificationScheduler = di.notificationScheduler,
    )

    fun createAnalyticsViewModel() = AnalyticsViewModel(
        di.mealPlanRepository,
        di.foodItemRepository,
        di.receiptHistoryRepository,
        di.storeRepository,
        di.unitRepository,
        di.restaurantRepository,
    )

    fun createSettingsViewModel() = SettingsViewModel(
        di.settingsRepository,
        di.foodItemRepository,
        di.storeRepository,
        di.restaurantRepository,
        di.unitRepository,
    )

    fun createIngredientsViewModel() = IngredientsViewModel(
        di.foodItemRepository,
        di.storeRepository,
        di.unitRepository,
        di.shoppingListItemRepository
    )

    fun createRecipesViewModel() = RecipesViewModel(
        di.foodItemRepository,
        di.pantryRepository,
        di.unitRepository,
        di.shoppingListItemRepository
    )

    fun createMealsViewModel() = MealsViewModel(
        di.foodItemRepository,
        di.pantryRepository,
        di.unitRepository,
    )

    fun createPantryViewModel() = PantryViewModel(
        di.pantryRepository,
        di.foodItemRepository,
        di.unitRepository,
    )

    fun createShoppingListViewModel() = ShoppingListViewModel(
        di.foodItemRepository,
        di.storeRepository,
        di.unitRepository,
        di.settingsRepository,
        di.mealPlanRepository,
        di.shoppingListRepository,
        di.pantryRepository,
        di.shoppingListItemRepository,
        di.receiptHistoryRepository,
    )
}
