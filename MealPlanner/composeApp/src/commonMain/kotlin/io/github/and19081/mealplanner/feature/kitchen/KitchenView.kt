@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.feature.kitchen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.feature.ingredients.IngredientsView
import io.github.and19081.mealplanner.feature.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.feature.meals.MealsViewModel
import io.github.and19081.mealplanner.feature.meals.MealsView
import io.github.and19081.mealplanner.feature.main.MainViewModel
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.recipes.RecipesView
import io.github.and19081.mealplanner.feature.recipes.RecipesViewModel
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.domain.model.FoodItem

sealed class KitchenModal {
  data class FoodItemCreator(val name: String, val onCreated: (FoodItem) -> Unit) :
      KitchenModal()

  data class KitchenTransactionReview(
      val transaction: KitchenTransaction,
      val onCommit: (KitchenTransaction) -> Unit
  ) : KitchenModal()
}

@Composable
fun KitchenView(
    diContainer: DependencyInjectionContainer,
    mainViewModel: MainViewModel,
    mode: Mode,
    isExpanded: Boolean,
    pushModal: (KitchenModal) -> Unit = {},
    popModal: () -> Unit = {},
    modalStack: List<KitchenModal> = emptyList(),
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Meals", "Recipes", "Ingredients")

  val mealsViewModel: MealsViewModel = viewModel { diContainer.viewModelFactory.createMealsViewModel() }
  val recipesViewModel: RecipesViewModel = viewModel { diContainer.viewModelFactory.createRecipesViewModel() }
  val ingredientsViewModel: IngredientsViewModel = viewModel { diContainer.viewModelFactory.createIngredientsViewModel() }

  LaunchedEffect(mainViewModel.shouldActivateCanMakeNowFilter.value) {
    if (mainViewModel.shouldActivateCanMakeNowFilter.value) {
      selectedTab = 1
      recipesViewModel.setCanMakeNowFilter(true)
      mainViewModel.setActivateCanMakeNowFilter(false)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = selectedTab) {
      tabs.forEachIndexed { index, title ->
        Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title) }
        )
      }
    }

    Box(modifier = Modifier.weight(1f)) {
      when (selectedTab) {
        0 -> MealsView(
            viewModel = mealsViewModel,
            mode = mode,
            isExpanded = isExpanded,
            onAddIngredient = { name, onCreated ->
                pushModal(
                    KitchenModal.FoodItemCreator(
                        name,
                        onCreated
                    )
                )
            },
            onAddRecipe = { name, onCreated -> /* Logic to push recipe creator if needed */ },
            onMakeMeal = { meal, multi, yieldId, yieldQty, yieldUnitId ->
                val tx = mealsViewModel.createMakeTransaction(
                    meal,
                    multi,
                    yieldId,
                    yieldQty,
                    yieldUnitId
                )
                pushModal(KitchenModal.KitchenTransactionReview(tx) {
                    mealsViewModel.commitTransaction(
                        it
                    )
                })
            }
        )
        1 -> RecipesView(
            viewModel = recipesViewModel,
            mode = mode,
            isExpanded = isExpanded,
            onAddIngredient = { name, onCreated ->
                pushModal(
                    KitchenModal.FoodItemCreator(
                        name,
                        onCreated
                    )
                )
            },
            onAddSubRecipe = { name, onCreated -> /* Logic to push recipe creator */ }
        )
        2 -> IngredientsView(viewModel = ingredientsViewModel, mode = mode, isExpanded = isExpanded)
      }
    }
  }

  // Modals
  modalStack.lastOrNull()?.let { modal ->
    when (modal) {
      is KitchenModal.FoodItemCreator -> {
          // Placeholder: in a real app, this would show a dialog
      }
      is KitchenModal.KitchenTransactionReview -> {
        AlertDialog(
            onDismissRequest = popModal,
            title = { Text(modal.transaction.title) },
            text = {
              Column {
                modal.transaction.changes.forEach { change ->
                  Text("${if (change.direction == TransactionDirection.IN) "+" else "-"} ${change.measurement.quantity} ${change.unitAbbreviation} ${change.ingredientName}")
                }
              }
            },
            confirmButton = {
              Button(onClick = {
                modal.onCommit(modal.transaction)
                popModal()
              }) {
                Text("Confirm")
              }
            },
            dismissButton = {
              TextButton(onClick = popModal) { Text("Cancel") }
            }
        )
      }
    }
  }
}
