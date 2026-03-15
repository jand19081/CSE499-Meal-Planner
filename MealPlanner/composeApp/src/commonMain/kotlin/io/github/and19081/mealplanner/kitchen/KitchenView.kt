@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.kitchen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.dependencyinjection.DependencyInjectionContainer
import io.github.and19081.mealplanner.ingredients.*
import io.github.and19081.mealplanner.ingredients.IngredientsView
import io.github.and19081.mealplanner.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.meals.MealsView
import io.github.and19081.mealplanner.meals.MealsViewModel
import io.github.and19081.mealplanner.recipes.RecipeForm
import io.github.and19081.mealplanner.recipes.RecipesView
import io.github.and19081.mealplanner.recipes.RecipesViewModel
import io.github.and19081.mealplanner.settings.Mode
import kotlin.uuid.Uuid

sealed class KitchenModal {
  data class IngredientCreator(val name: String, val onCreated: (Ingredient) -> Unit) :
      KitchenModal()

  data class RecipeCreator(val name: String, val onCreated: (Recipe) -> Unit) : KitchenModal()

  data class KitchenTransactionReview(
      val transaction: KitchenTransaction,
      val onCommit: (KitchenTransaction) -> Unit
  ) : KitchenModal()
}

@Composable
fun KitchenView(
    diContainer: DependencyInjectionContainer,
    mode: Mode,
    isExpanded: Boolean,
    pushModal: (KitchenModal) -> Unit = {},
    popModal: () -> Unit = {},
    modalStack: List<KitchenModal> = emptyList(),
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Meals", "Recipes", "Ingredients")

  // Hoist ViewModels
  val mealsVm = viewModel {
    MealsViewModel(
        diContainer.mealRepository,
        diContainer.recipeRepository,
        diContainer.ingredientRepository,
        diContainer.pantryRepository,
        diContainer.unitRepository,
    )
  }
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

  Column(modifier = Modifier.fillMaxSize()) {
    if (isExpanded) {
      PrimaryTabRow(selectedTabIndex = selectedTab) {
        tabs.forEachIndexed { index, title ->
          Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = { Text(title) },
          )
        }
      }
    } else {
      var expanded by remember { mutableStateOf(false) }
      Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
          Text(tabs[selectedTab])
          Spacer(Modifier.width(8.dp))
          Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
          tabs.forEachIndexed { index, title ->
            DropdownMenuItem(
                text = { Text(title) },
                onClick = {
                  selectedTab = index
                  expanded = false
                },
            )
          }
        }
      }
    }

    when (selectedTab) {
      0 -> {
        MealsView(
            viewModel = mealsVm,
            mode = mode,
            isExpanded = isExpanded,
            onAddIngredient = { name: String, onCreated: (Ingredient) -> Unit ->
              pushModal(KitchenModal.IngredientCreator(name, onCreated))
            },
            onAddRecipe = { name: String, onCreated: (Recipe) -> Unit ->
              pushModal(KitchenModal.RecipeCreator(name, onCreated))
            },
            onMakeMeal = { meal, multiplier, yieldIngId, yieldQty, yieldUnitId ->
                val transaction = mealsVm.createMakeTransaction(meal, multiplier, yieldIngId, yieldQty, yieldUnitId)
                pushModal(KitchenModal.KitchenTransactionReview(transaction) { committed ->
                    mealsVm.commitTransaction(committed)
                })
            }
        )
      }
      1 -> {
        RecipesView(
            viewModel = recipesVm,
            mode = mode,
            isExpanded = isExpanded,
            onAddIngredient = { name: String, onCreated: (Ingredient) -> Unit ->
              pushModal(KitchenModal.IngredientCreator(name, onCreated))
            },
            onAddSubRecipe = { name: String, onCreated: (Recipe) -> Unit ->
              pushModal(KitchenModal.RecipeCreator(name, onCreated))
            },
            onMakeRecipe = { recipe, multiplier, yieldIngId, yieldQty, yieldUnitId ->
                val transaction = recipesVm.createMakeTransaction(recipe, multiplier, yieldIngId, yieldQty, yieldUnitId)
                pushModal(KitchenModal.KitchenTransactionReview(transaction) { committed ->
                    recipesVm.commitTransaction(committed)
                })
            }
        )
      }
      2 -> {
        IngredientsView(ingredientsVm, mode = mode, isExpanded = isExpanded)
      }
    }
  }
}
