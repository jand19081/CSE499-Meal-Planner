package io.github.and19081.mealplanner.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.and19081.mealplanner.home.HomeView
import io.github.and19081.mealplanner.settings.SettingsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    viewModel: MainViewModel = viewModel { MainViewModel() }
) {
    val navController = rememberNavController()

    val selectedItemIndex = viewModel.selectedRailIndex.intValue

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(AppDestinations.entries[selectedItemIndex].label)
                }
            )
        }
    ) { innerPadding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // --- NAVIGATION RAIL (Left Side) ---
            NavigationRail {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    NavigationRailItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        selected = selectedItemIndex == index,
                        onClick = {
                            viewModel.onRailItemClicked(index)

                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }

            // --- NAV HOST (Right Side / Main Content) ---
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.weight(1f)
            ) {
                composable<HomeRoute> {
                    HomeView()
                }
                composable<SettingsRoute> {
                    SettingsView()
                }
            }
        }
    }
}