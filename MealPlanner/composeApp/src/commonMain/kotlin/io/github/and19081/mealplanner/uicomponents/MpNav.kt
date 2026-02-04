package io.github.and19081.mealplanner.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.and19081.mealplanner.analytics.AnalyticsView
import io.github.and19081.mealplanner.calendar.CalendarView
import io.github.and19081.mealplanner.dashboard.DashboardView
import io.github.and19081.mealplanner.ingredients.IngredientsView
import io.github.and19081.mealplanner.main.AnalyticsRoute
import io.github.and19081.mealplanner.main.AppDestinations
import io.github.and19081.mealplanner.main.CalendarRoute
import io.github.and19081.mealplanner.main.DashboardRoute
import io.github.and19081.mealplanner.main.IngredientsRoute
import io.github.and19081.mealplanner.main.MainViewModel
import io.github.and19081.mealplanner.main.MealsRoute
import io.github.and19081.mealplanner.main.PantryRoute
import io.github.and19081.mealplanner.main.RecipesRoute
import io.github.and19081.mealplanner.main.SettingsRoute
import io.github.and19081.mealplanner.main.ShoppingListRoute
import io.github.and19081.mealplanner.meals.MealsView
import io.github.and19081.mealplanner.pantry.PantryView
import io.github.and19081.mealplanner.recipes.RecipesView
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.settings.SettingsView
import io.github.and19081.mealplanner.shoppinglist.ShoppingListView

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MpNav(
    mode: Mode,
    isNavRailVisible: Boolean,
    navController: NavHostController,
    selectedItemIndex: Int,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
){
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // Determine the "intended" layout type based on mode and adaptability
    val targetLayoutType = when (mode) {
        Mode.AUTO -> defaultLayoutType
        Mode.DESKTOP -> NavigationSuiteType.NavigationRail
        Mode.MOBILE -> NavigationSuiteType.NavigationBar
    }

    // Use Custom Bottom Bar if the target is NavigationBar.
    // We set the Scaffold layout to None so we can render the custom bar manually.
    val useCustomBottomBar = targetLayoutType == NavigationSuiteType.NavigationBar
    val scaffoldLayoutType = if (useCustomBottomBar || !isNavRailVisible) NavigationSuiteType.None else targetLayoutType

    NavigationSuiteScaffold(
        layoutType = scaffoldLayoutType,
        navigationSuiteItems = {
            // Only add items to the Suite if we are NOT using the custom bar (i.e. using Rail)
            // and navigation is visible.
            if (!useCustomBottomBar && isNavRailVisible) {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    item(
                        selected = selectedItemIndex == index,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            bottomBar = {
                if (useCustomBottomBar && isNavRailVisible) {
                    CustomBottomBar(
                        selectedItemIndex = selectedItemIndex,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DashboardRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<DashboardRoute> { DashboardView() }
                composable<CalendarRoute> {
                    CalendarView(
                        currentMonthFlow = viewModel.currentMonth,
                        calendarViewMode = viewModel.calendarViewMode.value
                    )
                }
                composable<IngredientsRoute> { IngredientsView() }
                composable<MealsRoute> { MealsView() }
                composable<RecipesRoute> { RecipesView() }
                composable<ShoppingListRoute> { ShoppingListView() }
                composable<PantryRoute> { PantryView() }
                composable<AnalyticsRoute> { AnalyticsView() }
                composable<SettingsRoute> { SettingsView() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomBar(
    selectedItemIndex: Int,
    onNavigate: (Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = AppDestinations.entries.getOrNull(selectedItemIndex) ?: AppDestinations.DASHBOARD

    BottomAppBar {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Custom Row instead of TextField to remove "input box" look
            Row(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable) // Links the menu to this Row
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Fix: Wrap the icon property in an Icon composable
                Icon(
                    imageVector = selectedItem.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = selectedItem.label, style = MaterialTheme.typography.bodyLarge)
                // Standard dropdown arrow
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AppDestinations.entries.forEach { destination ->
                    DropdownMenuItem(
                        text = { Text(destination.label) },
                        leadingIcon = { Icon(destination.icon, contentDescription = null) },
                        onClick = {
                            onNavigate(destination.route)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

}