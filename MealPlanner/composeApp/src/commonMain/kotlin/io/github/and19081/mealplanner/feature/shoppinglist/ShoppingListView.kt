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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import io.github.and19081.mealplanner.core.util.UnitModel
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
  val receiptDraft by viewModel.receiptDraft.collectAsState()
  val showDiscrepancyDialog = receiptDraft != null && receiptDraft!!.userEnteredActualTotalCents != null
  val pendingTotal = receiptDraft?.userEnteredActualTotalCents?.toLong()

  var shoppingModeStoreId by remember { mutableStateOf<Uuid?>(null) }
  var showAddItemDialog by remember { mutableStateOf(false) }

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
      },
      floatingActionButton = {
          FloatingActionButton(onClick = { showAddItemDialog = true }) {
              Icon(Icons.Default.Add, contentDescription = "Add Item")
          }
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

  if (showAddItemDialog) {
      AddShoppingItemDialog(
          allUnits = uiState.allUnits,
          onDismiss = { showAddItemDialog = false },
          onAdd = { name, qty, unitId ->
              viewModel.addCustomItem(name, qty, unitId, isPantry = false)
              showAddItemDialog = false
          }
      )
  }
}

@Composable
fun AddShoppingItemDialog(
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Uuid) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf<UnitModel?>(allUnits.firstOrNull()) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit?.abbreviation ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = {
                                IconButton(onClick = { unitExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            allUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text("${unit.displayName} (${unit.abbreviation})") },
                                    onClick = {
                                        selectedUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val unit = selectedUnit
                    if (name.isNotBlank() && unit != null) {
                        onAdd(name.trim(), qty, unit.id)
                    }
                },
                enabled = name.isNotBlank() && quantityStr.toDoubleOrNull() != null && selectedUnit != null
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
