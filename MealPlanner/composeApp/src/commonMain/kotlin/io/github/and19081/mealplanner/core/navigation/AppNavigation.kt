package io.github.and19081.mealplanner.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.di.ViewModelFactory
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
import io.github.and19081.mealplanner.feature.main.SettingsRoute
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
    composable<SettingsRoute> {
      val vm = viewModel { factory.createSettingsViewModel() }
      SettingsView(vm, isExpanded = isExpanded)
    }
  }
}
