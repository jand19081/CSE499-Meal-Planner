package io.github.and19081.mealplanner.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import io.github.and19081.mealplanner.calendar.CalendarViewMode
import io.github.and19081.mealplanner.settings.Mode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.and19081.mealplanner.dependencyinjection.DependencyInjectionContainer
import io.github.and19081.mealplanner.uicomponents.MpNav
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    diContainer: DependencyInjectionContainer,
    viewModel: MainViewModel = viewModel { MainViewModel() }
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val canNavigateBack = navController.previousBackStackEntry != null
    val selectedItemIndex = viewModel.selectedRailIndex.intValue
    val isNavRailVisible = viewModel.isNavRailVisible.value
    val currentMonth by viewModel.currentMonth.collectAsState()
    val cornerStyle by diContainer.settingsRepository.cornerStyle.collectAsState()
    val appSettings by diContainer.settingsRepository.appSettings.collectAsState()

    // Sync ViewModel state with Navigation State
    LaunchedEffect(currentDestination) {
        if (currentDestination != null) {
            MainDestinations.forEachIndexed { index, destination ->
                if (currentDestination.hierarchy.any { it.hasRoute(destination.route::class) }) {
                    viewModel.onRailItemClicked(index)
                }
            }
        }
    }

    BoxWithConstraints {
        val isLandscape = maxWidth > maxHeight
        val adaptiveInfo = currentWindowAdaptiveInfo()
        val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

        val targetLayoutType = when (appSettings.view) {
            Mode.AUTO -> {
                if (isLandscape) NavigationSuiteType.NavigationRail else defaultLayoutType
            }
            Mode.DESKTOP -> NavigationSuiteType.NavigationRail
            Mode.MOBILE -> NavigationSuiteType.NavigationBar
        }

        val showHamburger = targetLayoutType == NavigationSuiteType.NavigationRail

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarColors(
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.inverseSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.inverseSurface,
                        subtitleContentColor = MaterialTheme.colorScheme.inverseSurface
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
                                enabled = canNavigateBack
                            ) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go Back")
                            }
                        }
                    },
                    title = {
                        val label = MainDestinations.getOrNull(selectedItemIndex)?.label ?: ""
                        if (currentDestination?.hasRoute(CalendarRoute::class) == true) {
                            when (viewModel.calendarViewMode.value) {
                                CalendarViewMode.MONTH -> {
                                    Text(currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + currentMonth.year)
                                }
                                CalendarViewMode.WEEK -> {
                                    val week = viewModel.currentMonth.collectAsState().value
                                    Text(
                                        "${week.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${week.day} - " +
                                                "${week.plus(DatePeriod(days = 6)).day}, ${week.year}"
                                    )
                                }
                                CalendarViewMode.DAY -> {
                                    Text(
                                        "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.day}, ${currentMonth.year}"
                                    )
                                }
                            }
                        } else {
                            Text(label)
                        }
                    },
                    actions = {
                        if (currentDestination?.hasRoute(CalendarRoute::class) == true) {
                            IconButton(onClick = { viewModel.onDateArrowClick(false) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                            }
                            IconButton(onClick = { viewModel.toggleCalendarViewMode() }) {
                                val icon = when (viewModel.calendarViewMode.value) {
                                    CalendarViewMode.DAY -> Icons.Filled.CalendarViewDay
                                    CalendarViewMode.WEEK -> Icons.Filled.CalendarViewWeek
                                    CalendarViewMode.MONTH -> Icons.Filled.CalendarMonth
                                }
                                Icon(icon, contentDescription = "Toggle Calendar View")
                            }
                            IconButton(onClick = { viewModel.onDateArrowClick(true) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            MpNav(
                diContainer = diContainer,
                mode = appSettings.view,
                isNavRailVisible = isNavRailVisible,
                navController = navController,
                selectedItemIndex = selectedItemIndex,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
