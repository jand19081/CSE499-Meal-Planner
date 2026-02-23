package io.github.and19081.mealplanner.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import io.github.and19081.mealplanner.ingredients.IngredientsView
import io.github.and19081.mealplanner.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.main.AnalyticsRoute
import io.github.and19081.mealplanner.main.CalendarRoute
import io.github.and19081.mealplanner.main.DashboardRoute
import io.github.and19081.mealplanner.main.IngredientsRoute
import io.github.and19081.mealplanner.main.MainViewModel
import io.github.and19081.mealplanner.main.MainDestinations
import io.github.and19081.mealplanner.main.MealsRoute
import io.github.and19081.mealplanner.main.PantryRoute
import io.github.and19081.mealplanner.main.RecipesRoute
import io.github.and19081.mealplanner.main.SettingsRoute
import io.github.and19081.mealplanner.main.ShoppingListRoute
import io.github.and19081.mealplanner.meals.MealsView
import io.github.and19081.mealplanner.meals.MealsViewModel
import io.github.and19081.mealplanner.pantry.PantryView
import io.github.and19081.mealplanner.pantry.PantryViewModel
import io.github.and19081.mealplanner.recipes.RecipesView
import io.github.and19081.mealplanner.recipes.RecipesViewModel
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.settings.SettingsView
import io.github.and19081.mealplanner.settings.SettingsViewModel
import io.github.and19081.mealplanner.shoppinglist.ShoppingListView
import io.github.and19081.mealplanner.shoppinglist.ShoppingListViewModel

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MpNav(
    diContainer: DependencyInjectionContainer,
    mode: Mode,
    isNavRailVisible: Boolean,
    navController: NavHostController,
    selectedItemIndex: Int,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
){
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // 1. BoxWithConstraints allows us to check width/height without extra dependencies
    BoxWithConstraints(modifier = modifier) {

        // Simple heuristic: If width > height, we are in landscape (or desktop wide).
        val isLandscape = maxWidth > maxHeight

        // Determine the "intended" layout type based on mode and adaptability
        val targetLayoutType = when (mode) {
            Mode.AUTO -> {
                // If isLandscape is true, prefer Rail to save vertical space.
                // Otherwise fallback to system default.
                if (isLandscape) NavigationSuiteType.NavigationRail else defaultLayoutType
            }
            Mode.DESKTOP -> NavigationSuiteType.NavigationRail
            Mode.MOBILE -> NavigationSuiteType.NavigationBar
        }

        // Determine which custom navigation component to show
        val showNavRail = targetLayoutType == NavigationSuiteType.NavigationRail && isNavRailVisible
        val showBottomBar = targetLayoutType == NavigationSuiteType.NavigationBar

        // We set the Scaffold layout to None because we are manually rendering:
        // 1. The CustomBottomBar (for mobile)
        // 2. The CustomNavigationRail (for desktop, to support scrolling)
        val scaffoldLayoutType = NavigationSuiteType.None

        NavigationSuiteScaffold(
            layoutType = scaffoldLayoutType,
            navigationSuiteItems = {
                // We are not passing items here because we handle both Rail and BottomBar manually
            },
            // Note: We used 'modifier' on the BoxWithConstraints, so pass empty Modifier here
            modifier = Modifier
        ) {
            // Wrap everything in a Row to support the Navigation Rail on the left
            Row(modifier = Modifier.fillMaxSize()) {

                // 1. Custom Scrollable Rail
                if (showNavRail) {
                    CustomNavigationRail(
                        selectedItemIndex = selectedItemIndex,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 2. Main Content Scaffold
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
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
                        composable<DashboardRoute> { 
                            val vm = viewModel { 
                                DashboardViewModel(
                                    diContainer.mealPlanRepository,
                                    diContainer.mealRepository,
                                    diContainer.recipeRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.pantryRepository,
                                    diContainer.unitRepository,
                                    diContainer.settingsRepository,
                                    diContainer.receiptHistoryRepository,
                                    diContainer.shoppingListItemRepository,
                                    diContainer.restaurantRepository
                                ) 
                            }
                            DashboardView(vm, mode = mode) 
                        }
                        composable<CalendarRoute> {
                            val vm = viewModel {
                                CalendarViewModel(
                                    currentMonthFlow = viewModel.currentMonth,
                                    mealPlanRepository = diContainer.mealPlanRepository,
                                    mealRepository = diContainer.mealRepository,
                                    recipeRepository = diContainer.recipeRepository,
                                    ingredientRepository = diContainer.ingredientRepository,
                                    pantryRepository = diContainer.pantryRepository,
                                    unitRepository = diContainer.unitRepository,
                                    restaurantRepository = diContainer.restaurantRepository
                                )
                            }
                            CalendarView(
                                viewModel = vm,
                                calendarViewMode = viewModel.calendarViewMode.value,
                                mode = mode
                            )
                        }
                        composable<IngredientsRoute> { 
                            val vm = viewModel {
                                IngredientsViewModel(
                                    diContainer.ingredientRepository,
                                    diContainer.storeRepository,
                                    diContainer.unitRepository
                                )
                            }
                            IngredientsView(vm, mode = mode) 
                        }
                        composable<MealsRoute> { 
                            val vm = viewModel {
                                MealsViewModel(
                                    diContainer.mealRepository,
                                    diContainer.recipeRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.unitRepository
                                )
                            }
                            MealsView(vm, mode = mode) 
                        }
                        composable<RecipesRoute> { 
                            val vm = viewModel {
                                RecipesViewModel(
                                    diContainer.recipeRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.pantryRepository,
                                    diContainer.unitRepository
                                )
                            }
                            RecipesView(vm, mode = mode) 
                        }
                        composable<ShoppingListRoute> { 
                            val vm = viewModel {
                                ShoppingListViewModel(
                                    diContainer.recipeRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.storeRepository,
                                    diContainer.unitRepository,
                                    diContainer.settingsRepository,
                                    diContainer.mealPlanRepository,
                                    diContainer.mealRepository,
                                    diContainer.shoppingListRepository,
                                    diContainer.pantryRepository,
                                    diContainer.shoppingListItemRepository,
                                    diContainer.receiptHistoryRepository
                                )
                            }
                            ShoppingListView(vm) 
                        }
                        composable<PantryRoute> { 
                            val vm = viewModel {
                                PantryViewModel(
                                    diContainer.pantryRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.unitRepository
                                )
                            }
                            PantryView(vm, mode = mode) 
                        }
                        composable<AnalyticsRoute> { 
                            val vm = viewModel {
                                AnalyticsViewModel(
                                    diContainer.mealPlanRepository,
                                    diContainer.mealRepository,
                                    diContainer.recipeRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.receiptHistoryRepository,
                                    diContainer.storeRepository,
                                    diContainer.unitRepository,
                                    diContainer.restaurantRepository
                                )
                            }
                            AnalyticsView(vm, mode = mode) 
                        }
                        composable<SettingsRoute> { 
                            val vm = viewModel {
                                SettingsViewModel(
                                    diContainer.settingsRepository,
                                    diContainer.ingredientRepository,
                                    diContainer.storeRepository,
                                    diContainer.restaurantRepository,
                                    diContainer.unitRepository
                                )
                            }
                            SettingsView(vm) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomNavigationRail(
    selectedItemIndex: Int,
    onNavigate: (Any) -> Unit
) {
    NavigationRail {
        // Wrap items in a Column with verticalScroll to support short screens
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            // You can use Arrangement.Center, but if content overflows, Top is usually safer for scrolling
            verticalArrangement = Arrangement.Top
        ) {
            // Optional: Spacer to push items down slightly if using Arrangement.Top
            Spacer(Modifier.padding(vertical = 8.dp))

            MainDestinations.forEachIndexed { index, destination ->
                NavigationRailItem(
                    selected = selectedItemIndex == index,
                    onClick = { onNavigate(destination.route) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) }
                )
            }

            // Optional: Bottom padding
            Spacer(Modifier.padding(vertical = 8.dp))
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
    val selectedItem = MainDestinations.getOrNull(selectedItemIndex) ?: MainDestinations[0]

    BottomAppBar(
        modifier = Modifier.height(56.dp),
        // 1. Remove default internal padding so we can center manually
        contentPadding = PaddingValues(0.dp),
        // 2. CRITICAL: Remove the automatic system gesture padding.
        // This prevents the "extra space" at the bottom.
        windowInsets = WindowInsets(0.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight() // Ensure the box fills the height to allow centering
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .fillMaxHeight(), // Fill height so CenterVertically works
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = selectedItem.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedItem.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(4.dp))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                MainDestinations.forEach { destination ->
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
