package io.github.and19081.mealplanner.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import io.github.and19081.mealplanner.dependencyinjection.DependencyInjectionContainer
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientForm
import io.github.and19081.mealplanner.kitchen.KitchenModal
import io.github.and19081.mealplanner.kitchen.TransactionReviewSheet
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.recipes.RecipeForm
import io.github.and19081.mealplanner.recipes.RecipesViewModel
import io.github.and19081.mealplanner.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.uicomponents.MpNav
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    diContainer: DependencyInjectionContainer,
    viewModel: MainViewModel = viewModel { MainViewModel() },
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

  // Global ViewModels for Modals
  val recipesVm = viewModel {
    RecipesViewModel(
        diContainer.recipeRepository,
        diContainer.ingredientRepository,
        diContainer.pantryRepository,
        diContainer.unitRepository,
    )
  }
  val ingredientsVm = viewModel {
    IngredientsViewModel(
        diContainer.ingredientRepository,
        diContainer.storeRepository,
        diContainer.unitRepository,
    )
  }

  val ingredientsUiState by ingredientsVm.uiState.collectAsState()
  val recipesUiState by recipesVm.uiState.collectAsState()
  val modalStack by viewModel.modalStack.collectAsState()
  
  // Centralized Breakpoint Logic
  val adaptiveInfo = currentWindowAdaptiveInfo()
  val windowSizeClass = adaptiveInfo.windowSizeClass
  val isExpanded = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

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
    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    val targetLayoutType =
        when (appSettings.view) {
          Mode.AUTO -> {
            if (isLandscape) NavigationSuiteType.NavigationRail else defaultLayoutType
          }
          Mode.DESKTOP -> NavigationSuiteType.NavigationRail
          Mode.MOBILE -> NavigationSuiteType.NavigationBar
        }

    val showHamburger = targetLayoutType == NavigationSuiteType.NavigationRail

    MpNav(
        diContainer = diContainer,
        mode = appSettings.view,
        isExpanded = isExpanded,
        isNavRailVisible = isNavRailVisible,
        navController = navController,
        selectedItemIndex = selectedItemIndex,
        viewModel = viewModel,
        showHamburger = showHamburger,
        canNavigateBack = canNavigateBack,
        currentDestination = currentDestination,
        currentMonth = currentMonth,
        topBarTitle = {
          val title =
              AllDestinations.find { dest ->
                    currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                  }
                  ?.label ?: ""
          Text(title)
        },
        topBarActions = {
          // Universal Actions: Analytics and Settings
          IconButton(
              onClick = {
                navController.navigate(AnalyticsRoute) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              }
          ) {
            Icon(Icons.Default.Analytics, contentDescription = "Analytics")
          }
          IconButton(
              onClick = {
                navController.navigate(SettingsRoute) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              }
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
    )

    // Global Modal Overlay logic
    modalStack.lastOrNull()?.let { modal ->
      when (modal) {
        is KitchenModal.IngredientCreator -> {
          AlertDialog(
              onDismissRequest = { viewModel.popModal() },
              modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
              properties =
                  androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
              content = {
                IngredientForm(
                    ingredient = null,
                    initialName = modal.name,
                    allPackages = emptyList(),
                    allBridges = emptyList(),
                    allStores = ingredientsUiState.allStores,
                    allCategories = ingredientsUiState.allCategories,
                    allUnits = ingredientsUiState.allUnits,
                    onDismiss = { viewModel.popModal() },
                    onSave = { ingredient, packages, bridges ->
                      ingredientsVm.saveIngredient(ingredient, packages, bridges)
                      modal.onCreated(ingredient)
                      viewModel.popModal()
                    },
                    onAddStore = { ingredientsVm.addStore(it) },
                    onDeleteStore = { /* ... */ },
                    onAddCategory = { ingredientsVm.addCategory(it) },
                    onDeleteCategory = { /* ... */ },
                )
              },
          )
        }
        is KitchenModal.RecipeCreator -> {
          AlertDialog(
              onDismissRequest = { viewModel.popModal() },
              modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
              properties =
                  androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
              content = {
                RecipeForm(
                    recipe = null,
                    initialName = modal.name,
                    uiState = recipesUiState,
                    allIngredients = recipesUiState.allIngredients,
                    allRecipes = recipesUiState.allRecipes,
                    allPackages = recipesUiState.allPackages,
                    allBridges = recipesUiState.allBridges,
                    allUnits = recipesUiState.allUnits,
                    warnings = emptyList(),
                    onDismiss = { viewModel.popModal() },
                    onSave = { recipe ->
                      recipesVm.saveRecipe(recipe)
                      modal.onCreated(recipe)
                      viewModel.popModal()
                    },
                    onAddIngredient = { name: String, onCreated: (Ingredient) -> Unit ->
                      viewModel.pushModal(KitchenModal.IngredientCreator(name, onCreated))
                    },
                    onAddSubRecipe = { name: String, onCreated: (Recipe) -> Unit ->
                      viewModel.pushModal(KitchenModal.RecipeCreator(name, onCreated))
                    },
                )
              },
          )
        }
        is KitchenModal.KitchenTransactionReview -> {
          AlertDialog(
              onDismissRequest = { viewModel.popModal() },
              modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
              properties =
                  androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
              content = {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                  TransactionReviewSheet(
                      transaction = modal.transaction,
                      allUnits = recipesUiState.allUnits,
                      onDismiss = { viewModel.popModal() },
                      onCommit = {
                        modal.onCommit(it)
                        viewModel.popModal()
                      }
                  )
                }
              },
          )
        }
      }
    }
  }
}
