package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.core.navigation.AppNavigation
import io.github.and19081.mealplanner.feature.main.MainDestinations
import io.github.and19081.mealplanner.feature.main.MainViewModel
import io.github.and19081.mealplanner.feature.settings.Mode
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MpNav(
    diContainer: DependencyInjectionContainer,
    mode: Mode,
    isExpanded: Boolean,
    isNavRailVisible: Boolean,
    navController: NavHostController,
    selectedItemIndex: Int,
    viewModel: MainViewModel,
    showHamburger: Boolean,
    canNavigateBack: Boolean,
    currentDestination: NavDestination?,
    currentMonth: LocalDate,
    topBarTitle: @Composable () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
  val adaptiveInfo = currentWindowAdaptiveInfo()
  val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

  // Determine the "intended" layout type based on mode and adaptability
  val targetLayoutType =
      when (mode) {
        Mode.AUTO -> defaultLayoutType
        Mode.DESKTOP -> NavigationSuiteType.NavigationRail
        Mode.MOBILE -> NavigationSuiteType.NavigationBar
      }

  NavigationSuiteScaffold(
      layoutType = targetLayoutType,
      modifier = modifier,
      navigationSuiteItems = {
        MainDestinations.forEachIndexed { index, destination ->
          item(
              selected = selectedItemIndex == index,
              onClick = {
                navController.navigate(destination.route) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(destination.icon, contentDescription = destination.label) },
              label = { Text(destination.label) },
          )
        }
      },
  ) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
          TopAppBar(
              colors =
                  TopAppBarColors(
                      titleContentColor = MaterialTheme.colorScheme.primary,
                      containerColor = MaterialTheme.colorScheme.onPrimary,
                      scrolledContainerColor = MaterialTheme.colorScheme.primary,
                      navigationIconContentColor = MaterialTheme.colorScheme.inverseSurface,
                      actionIconContentColor = MaterialTheme.colorScheme.inverseSurface,
                      subtitleContentColor = MaterialTheme.colorScheme.inverseSurface,
                  ),
              navigationIcon = {
                Row {
                  if (showHamburger) {
                    IconButton(onClick = { viewModel.toggleRailVisibility() }) {
                      Icon(Icons.Default.Menu, contentDescription = "Toggle Menu")
                    }
                  }

                  IconButton(
                      onClick = { navController.popBackStack() },
                      enabled = canNavigateBack,
                  ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go Back")
                  }
                }
              },
              title = topBarTitle,
              actions = topBarActions,
          )
        },
    ) { innerPadding ->
      AppNavigation(
          navController = navController,
          diContainer = diContainer,
          mainViewModel = viewModel,
          mode = mode,
          isExpanded = isExpanded,
          modifier = Modifier.padding(innerPadding),
      )
    }
  }
}
