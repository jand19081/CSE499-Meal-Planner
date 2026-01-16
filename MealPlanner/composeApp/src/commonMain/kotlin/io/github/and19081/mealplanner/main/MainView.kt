package io.github.and19081.mealplanner.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.and19081.mealplanner.calendar.CalendarView
import io.github.and19081.mealplanner.home.HomeView
import io.github.and19081.mealplanner.ingredients.IngredientsView
import io.github.and19081.mealplanner.meals.MealsView
import io.github.and19081.mealplanner.recipes.RecipesView
import io.github.and19081.mealplanner.settings.SettingsView
import io.github.and19081.mealplanner.shoppinglist.ShoppingListView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    viewModel: MainViewModel = viewModel { MainViewModel() }
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val canNavigateBack = navController.previousBackStackEntry != null
    val selectedItemIndex = viewModel.selectedRailIndex.intValue
    val isNavRailVisible = viewModel.isNavRailVisible.value

    // Sync ViewModel state with Navigation State
    LaunchedEffect(currentDestination) {
        if (currentDestination != null) {
            AppDestinations.entries.forEachIndexed { index, destination ->
                if (currentDestination.hierarchy.any { it.hasRoute(destination.route::class) }) {
                    viewModel.onRailItemClicked(index)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Row {
                        IconButton(onClick = { viewModel.toggleRailVisibility() }) {
                            Icon(Icons.Default.Menu, contentDescription = "Toggle Menu")
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
                    val label = AppDestinations.entries.getOrNull(selectedItemIndex)?.label ?: ""
                    Text(label)
                }
            )
        }
    ) { innerPadding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            if (isNavRailVisible) {
                NavigationRail {
                    AppDestinations.entries.forEachIndexed { index, destination ->
                        NavigationRailItem(
                            label = { Text(destination.label) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            selected = selectedItemIndex == index,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.weight(1f)
            ) {
                composable<HomeRoute> { HomeView() }
                composable<CalendarRoute> { CalendarView() }
                composable<IngredientsRoute> { IngredientsView() }
                composable<MealsRoute> { MealsView() }
                composable<RecipesRoute> { RecipesView() }
                composable<ShoppingListRoute> { ShoppingListView() }
                composable<SettingsRoute> { SettingsView() }
            }
        }
    }
}