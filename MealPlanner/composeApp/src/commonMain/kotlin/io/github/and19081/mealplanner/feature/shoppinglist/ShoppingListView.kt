@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.feature.shoppinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.feature.meals.PriceUpdate
import io.github.and19081.mealplanner.ui.components.EmptyListMessage
import io.github.and19081.mealplanner.ui.components.ListSectionHeader
import io.github.and19081.mealplanner.ui.components.MpOutlinedTextField
import io.github.and19081.mealplanner.ui.components.MpValidationWarning
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ShoppingListView(
    viewModel: ShoppingListViewModel,
    isExpanded: Boolean,
    onTransaction: (KitchenTransaction, (KitchenTransaction) -> Unit) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val inCartItems by viewModel.inCartItems.collectAsState()
  val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
  val showDiscrepancyDialog by viewModel.showDiscrepancyDialog.collectAsState()
  val pendingTotal by viewModel.pendingActualTotal.collectAsState()

  var shoppingModeStoreId by remember { mutableStateOf<Uuid?>(null) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Shopping List") },
            actions = {
              if (shoppingModeStoreId != null) {
                Button(
                    onClick = {
                      val tx = viewModel.createShoppingTransaction(shoppingModeStoreId)
                      onTransaction(tx) { committed ->
                        viewModel.commitTransaction(committed)
                        shoppingModeStoreId = null
                      }
                    }
                ) {
                  Text("Finish Trip")
                }
              }
            }
        )
      }
  ) { padding ->
    LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
      if (uiState.warnings.isNotEmpty()) {
        item { MpValidationWarning(warnings = uiState.warnings) }
      }

      uiState.sections.forEach { section ->
        item {
            ListSectionHeader(
                text = "${section.storeName} - $${
                    String.format(
                        "%.2f",
                        section.totalCents / 100.0
                    )
                }",
                trailing = {
                    if (shoppingModeStoreId == null) {
                        TextButton(onClick = { shoppingModeStoreId = section.storeId }) {
                            Text("Start Trip")
                        }
                    } else if (shoppingModeStoreId == section.storeId) {
                        TextButton(onClick = { shoppingModeStoreId = null }) { Text("Cancel") }
                    }
                }
            )
        }

        items(section.items) { item ->
          ShoppingItemRow(
              item = item,
              isInCart = inCartItems.contains(item.id),
              isShoppingMode = shoppingModeStoreId == section.storeId,
              onToggleCart = { viewModel.toggleCart(item.id) },
              onMarkOwned = { viewModel.markOwned(item) }
          )
          HorizontalDivider()
        }
      }

      if (uiState.ownedItems.isNotEmpty()) {
        item { ListSectionHeader(text = "Already Owned") }
        items(uiState.ownedItems) { item ->
          ShoppingItemRow(
              item = item,
              isInCart = false,
              isShoppingMode = false,
              onToggleCart = {},
              onMarkOwned = { viewModel.markUnowned(item) }
          )
          HorizontalDivider()
        }
      }

      if (uiState.sections.isEmpty() && uiState.ownedItems.isEmpty()) {
        item { EmptyListMessage("Your shopping list is empty.") }
      }
    }
  }

  if (showDiscrepancyDialog && pendingTotal != null) {
    PriceDiscrepancyDialog(
        totalCents = pendingTotal!!,
        items = uiState.sections.find { it.storeId == shoppingModeStoreId }?.items?.filter { it.isInCart } ?: emptyList(),
        onConfirm = { viewModel.updatePricesAndFinalize(it) },
        onSkip = { viewModel.skipPriceUpdate() }
    )
  }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingListItemUi,
    isInCart: Boolean,
    isShoppingMode: Boolean,
    onToggleCart: () -> Unit,
    onMarkOwned: () -> Unit,
) {
  ListItem(
      headlineContent = {
        Text(
            item.name,
            fontWeight = if (item.isOwned) FontWeight.Normal else FontWeight.Bold,
            color = if (item.isOwned) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
      },
      supportingContent = {
        if (!item.isOwned) {
          Text("${item.quantity} ${item.unit} • Est: $${String.format("%.2f", item.priceCents / 100.0)}")
        }
      },
      leadingContent = {
        if (isShoppingMode) {
          Checkbox(checked = isInCart, onCheckedChange = { onToggleCart() })
        } else {
          Icon(
              if (item.isOwned) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
              contentDescription = null,
              tint = if (item.isOwned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
          )
        }
      },
      trailingContent = {
        IconButton(onClick = onMarkOwned) {
          Icon(
              if (item.isOwned) Icons.Default.RemoveCircle else Icons.Default.CheckCircle,
              contentDescription = if (item.isOwned) "Unown" else "Own"
          )
        }
      },
      modifier = Modifier.clickable { if (isShoppingMode) onToggleCart() else onMarkOwned() }
  )
}

@Composable
fun PriceDiscrepancyDialog(
    totalCents: Long,
    items: List<ShoppingListItemUi>,
    onConfirm: (List<PriceUpdate>) -> Unit,
    onSkip: () -> Unit,
) {
  var priceUpdates by remember {
    mutableStateOf(items.map { PriceUpdate(it.id, it.priceCents.toInt()) })
  }

  AlertDialog(
      onDismissRequest = onSkip,
      title = { Text("Update Prices?") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("The receipt total ($${String.format("%.2f", totalCents / 100.0)}) doesn't match the estimate. Update individual prices?")
          LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(priceUpdates.size) { index ->
              val update = priceUpdates[index]
              val item = items.find { it.id == update.foodItemId }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item?.name ?: "Unknown", modifier = Modifier.weight(1f))
                  MpOutlinedTextField(
                      value = (update.priceCents / 100.0).toString(),
                      onValueChange = {
                          val newCents = (it.toDoubleOrNull()?.let { it * 100 })?.toInt() ?: 0
                          priceUpdates =
                              priceUpdates.mapIndexed { i, u -> if (i == index) u.copy(priceCents = newCents) else u }
                      },
                      modifier = Modifier.width(100.dp)
                  )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { onConfirm(priceUpdates) }) { Text("Update & Finish") }
      },
      dismissButton = {
        TextButton(onClick = onSkip) { Text("Skip") }
      }
  )
}
