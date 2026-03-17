package io.github.and19081.mealplanner.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.and19081.mealplanner.analytics.AnalyticsView
import io.github.and19081.mealplanner.analytics.AnalyticsViewModel
import io.github.and19081.mealplanner.calendar.CalendarView
import io.github.and19081.mealplanner.calendar.CalendarViewModel
import io.github.and19081.mealplanner.dashboard.DashboardView
import io.github.and19081.mealplanner.dashboard.DashboardViewModel
import io.github.and19081.mealplanner.dependencyinjection.DependencyInjectionContainer
import io.github.and19081.mealplanner.dependencyinjection.ViewModelFactory
import io.github.and19081.mealplanner.kitchen.KitchenModal
import io.github.and19081.mealplanner.kitchen.KitchenView
import io.github.and19081.mealplanner.main.AnalyticsRoute
import io.github.and19081.mealplanner.main.CalendarRoute
import io.github.and19081.mealplanner.main.DashboardRoute
import io.github.and19081.mealplanner.main.InventoryRoute
import io.github.and19081.mealplanner.main.KitchenRoute
import io.github.and19081.mealplanner.main.MainViewModel
import io.github.and19081.mealplanner.main.SettingsRoute
import io.github.and19081.mealplanner.provisioning.InventoryView
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.settings.SettingsView
import io.github.and19081.mealplanner.settings.SettingsViewModel

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
          }
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
          onTransaction = { transaction, commit ->
            mainViewModel.pushModal(KitchenModal.KitchenTransactionReview(transaction, commit))
          },
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
