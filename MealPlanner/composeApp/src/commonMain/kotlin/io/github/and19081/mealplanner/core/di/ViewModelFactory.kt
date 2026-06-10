package io.github.and19081.mealplanner.core.di

import io.github.and19081.mealplanner.domain.logic.CommitConsumptionUseCase
import io.github.and19081.mealplanner.domain.logic.CommitRecipeExecutionUseCase
import io.github.and19081.mealplanner.feature.analytics.AnalyticsViewModel
import io.github.and19081.mealplanner.feature.calendar.CalendarViewModel
import io.github.and19081.mealplanner.feature.dashboard.DashboardViewModel
import io.github.and19081.mealplanner.feature.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.feature.meals.MealsViewModel
import io.github.and19081.mealplanner.feature.pantry.PantryViewModel
import io.github.and19081.mealplanner.feature.receipts.ReceiptsViewModel
import io.github.and19081.mealplanner.feature.recipes.RecipeExecutionViewModel
import io.github.and19081.mealplanner.feature.recipes.RecipesViewModel
import io.github.and19081.mealplanner.feature.settings.SettingsViewModel
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

class ViewModelFactory(private val di: DependencyInjectionContainer) {
    fun createDashboardViewModel() =
        DashboardViewModel(
            di.mealPlanRepository,
            di.foodItemRepository,
            di.pantryRepository,
            di.unitRepository,
            di.settingsRepository,
            di.receiptHistoryRepository,
            di.shoppingListItemRepository,
            di.restaurantRepository,
        )

    fun createCalendarViewModel(currentMonthFlow: StateFlow<LocalDate>) =
        CalendarViewModel(
            currentMonthFlow = currentMonthFlow,
            mealPlanRepository = di.mealPlanRepository,
            foodItemRepository = di.foodItemRepository,
            pantryRepository = di.pantryRepository,
            unitRepository = di.unitRepository,
            restaurantRepository = di.restaurantRepository,
            settingsRepository = di.settingsRepository,
            notificationScheduler = di.notificationScheduler,
            commitConsumptionUseCase =
                CommitConsumptionUseCase(
                    mealPlanRepository = di.mealPlanRepository,
                    foodItemRepository = di.foodItemRepository,
                    pantryRepository = di.pantryRepository,
                    receiptHistoryRepository = di.receiptHistoryRepository,
                    unitRepository = di.unitRepository,
                ),
        )

    fun createAnalyticsViewModel() =
        AnalyticsViewModel(
            di.mealPlanRepository,
            di.foodItemRepository,
            di.receiptHistoryRepository,
            di.storeRepository,
            di.unitRepository,
            di.restaurantRepository,
        )

    fun createReceiptsViewModel() =
        ReceiptsViewModel(
            di.receiptHistoryRepository,
            di.foodItemRepository,
            di.storeRepository,
            di.restaurantRepository,
            di.unitRepository,
        )

    fun createSettingsViewModel() =
        SettingsViewModel(
            di.settingsRepository,
            di.foodItemRepository,
            di.storeRepository,
            di.restaurantRepository,
            di.unitRepository,
            di.dataTransferService,
        )

    fun createIngredientsViewModel() =
        IngredientsViewModel(
            di.foodItemRepository,
            di.storeRepository,
            di.unitRepository,
            di.shoppingListItemRepository,
        )

    fun createRecipesViewModel() =
        RecipesViewModel(
            di.foodItemRepository,
            di.pantryRepository,
            di.unitRepository,
            di.shoppingListItemRepository,
        )

    fun createMealsViewModel() =
        MealsViewModel(
            di.foodItemRepository,
            di.pantryRepository,
            di.unitRepository,
        )

    fun createPantryViewModel() =
        PantryViewModel(
            di.pantryRepository,
            di.foodItemRepository,
            di.unitRepository,
        )

    fun createShoppingListViewModel() =
        ShoppingListViewModel(
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

    fun createRecipeExecutionViewModel() =
        RecipeExecutionViewModel(
            commitRecipeExecutionUseCase = CommitRecipeExecutionUseCase(
                foodItemRepository = di.foodItemRepository,
                pantryRepository = di.pantryRepository,
                unitRepository = di.unitRepository,
            ),
            foodItemRepository = di.foodItemRepository,
            unitRepository = di.unitRepository,
        )
}
