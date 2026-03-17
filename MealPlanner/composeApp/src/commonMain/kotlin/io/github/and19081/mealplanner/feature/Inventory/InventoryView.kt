@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.feature.Inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.core.di.DependencyInjectionContainer
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.feature.ingredients.IngredientsViewModel
import io.github.and19081.mealplanner.feature.kitchen.KitchenModal
import io.github.and19081.mealplanner.feature.pantry.PantryView
import io.github.and19081.mealplanner.feature.pantry.PantryViewModel
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListView
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListViewModel

@Composable
fun InventoryView(
    diContainer: DependencyInjectionContainer,
    mode: Mode,
    isExpanded: Boolean,
    pushModal: (KitchenModal) -> Unit = {},
    popModal: () -> Unit = {},
    modalStack: List<KitchenModal> = emptyList(),
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Shopping List", "Pantry")

  // Use the factory to create ViewModels
  val shoppingListVm: ShoppingListViewModel = viewModel { diContainer.viewModelFactory.createShoppingListViewModel() }
  val pantryVm: PantryViewModel = viewModel { diContainer.viewModelFactory.createPantryViewModel() }
  val ingredientsVm: IngredientsViewModel = viewModel { diContainer.viewModelFactory.createIngredientsViewModel() }

  val ingredientsUiState by ingredientsVm.uiState.collectAsState()

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
          ShoppingListView(
              viewModel = shoppingListVm,
              isExpanded = isExpanded,
              onTransaction = { transaction, commit ->
                  pushModal(KitchenModal.KitchenTransactionReview(transaction, commit))
              }
          )
      }
      1 -> {
          PantryView(
              viewModel = pantryVm,
              mode = mode,
              isExpanded = isExpanded,
              onAddIngredient = { name: String, onCreated: (FoodItem) -> Unit ->
                  pushModal(KitchenModal.FoodItemCreator(name, onCreated))
              },
          )
      }
    }
  }
}
