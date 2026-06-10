package io.github.and19081.mealplanner.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.di.ViewModelFactory
import io.github.and19081.mealplanner.domain.model.Recipe
import io.github.and19081.mealplanner.feature.Inventory.InventoryView
import io.github.and19081.mealplanner.feature.analytics.AnalyticsView
import io.github.and19081.mealplanner.feature.calendar.CalendarView
import io.github.and19081.mealplanner.feature.dashboard.DashboardView
import io.github.and19081.mealplanner.feature.kitchen.KitchenView
import io.github.and19081.mealplanner.feature.main.AnalyticsRoute
import io.github.and19081.mealplanner.feature.main.CalendarRoute
import io.github.and19081.mealplanner.feature.main.DashboardRoute
import io.github.and19081.mealplanner.feature.main.InventoryRoute
import io.github.and19081.mealplanner.feature.main.KitchenRoute
import io.github.and19081.mealplanner.feature.main.MainViewModel
import io.github.and19081.mealplanner.feature.main.RecipeExecutionRoute
import io.github.and19081.mealplanner.feature.main.ReceiptsRoute
import io.github.and19081.mealplanner.feature.main.SettingsRoute
import io.github.and19081.mealplanner.feature.recipes.RecipeExecutionScreen
import io.github.and19081.mealplanner.feature.receipts.ReceiptsView
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.settings.SettingsView

@Composable
fun AppNavigation(
    navController: NavHostController,
    diContainer: DependencyInjectionContainer,
    mainViewModel: MainViewModel,
    mode: Mode,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val factory = remember { ViewModelFactory(diContainer) }
    val recipeExecutionVm = viewModel { factory.createRecipeExecutionViewModel() }

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier.fillMaxSize(),
    ) {
        composable<DashboardRoute> {
            val vm = viewModel { factory.createDashboardViewModel() }
            DashboardView(
                viewModel = vm,
                mode = mode,
                isExpanded = isExpanded,
                onCanMakeNowClick = {
                    mainViewModel.setActivateCanMakeNowFilter(true)
                    navController.navigate(KitchenRoute)
                },
            )
        }
        composable<CalendarRoute> {
            val vm = viewModel { factory.createCalendarViewModel(mainViewModel.currentMonth) }
            CalendarView(
                viewModel = vm,
                calendarViewMode = mainViewModel.calendarViewMode.value,
                mode = mode,
                isExpanded = isExpanded,
                onPrevClick = { mainViewModel.onDateArrowClick(false) },
                onNextClick = { mainViewModel.onDateArrowClick(true) },
                onToggleViewMode = { mainViewModel.toggleCalendarViewMode() },
                onDateSelected = { mainViewModel.updateReferenceDate(it) },
            )
        }
        composable<KitchenRoute> {
            KitchenView(
                diContainer = diContainer,
                mainViewModel = mainViewModel,
                mode = mode,
                isExpanded = isExpanded,
                pushModal = { mainViewModel.pushModal(it) },
                popModal = { mainViewModel.popModal() },
                modalStack = mainViewModel.modalStack.value,
                onMakeRecipe = { foodItem ->
                    (foodItem as? Recipe)?.let { recipeExecutionVm.start(it) }
                    navController.navigate(RecipeExecutionRoute)
                },
            )
        }
        composable<InventoryRoute> {
            InventoryView(
                diContainer = diContainer,
                mode = mode,
                isExpanded = isExpanded,
                pushModal = { mainViewModel.pushModal(it) },
                popModal = { mainViewModel.popModal() },
                modalStack = mainViewModel.modalStack.value,
            )
        }
        composable<AnalyticsRoute> {
            val vm = viewModel { factory.createAnalyticsViewModel() }
            AnalyticsView(vm, mode = mode, isExpanded = isExpanded)
        }
        composable<ReceiptsRoute> {
            val vm = viewModel { factory.createReceiptsViewModel() }
            ReceiptsView(vm, mode = mode, isExpanded = isExpanded)
        }
        composable<SettingsRoute> {
            val vm = viewModel { factory.createSettingsViewModel() }
            SettingsView(vm, isExpanded = isExpanded)
        }
        composable<RecipeExecutionRoute> {
            val allItemNames by recipeExecutionVm.allItemNames.collectAsState()
            val allUnitAbbr by recipeExecutionVm.allUnitAbbr.collectAsState()
            RecipeExecutionScreen(
                viewModel = recipeExecutionVm,
                allItemNames = allItemNames,
                allUnitAbbr = allUnitAbbr,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
