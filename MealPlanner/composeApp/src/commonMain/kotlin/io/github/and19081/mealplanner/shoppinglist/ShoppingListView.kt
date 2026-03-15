@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.shoppinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ShoppingListView(
    viewModel: ShoppingListViewModel,
    isExpanded: Boolean,
    onTransaction: (KitchenTransaction, (KitchenTransaction) -> Unit) -> Unit = { _, _ -> }
) {
  val uiState by viewModel.uiState.collectAsState()

  val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
  val showDiscrepancyDialog by viewModel.showDiscrepancyDialog.collectAsState()
  val pendingActualTotal by viewModel.pendingActualTotal.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var showStoreSelectDialog by remember { mutableStateOf(false) }
  var shoppingModeStoreId by remember { mutableStateOf<Uuid?>(null) }

  // Desktop Side Panel State
  var sidePanelType by remember { mutableStateOf<String?>(null) }

  val actualIsExpanded = isExpanded // Or use app mode if preferred

  // ... (Total calculations) ...
  val displayedSections =
      if (shoppingModeStoreId != null) {
        uiState.sections.filter { it.storeId == shoppingModeStoreId }
      } else {
        uiState.sections
      }
  val cartSubtotal =
      displayedSections.flatMap { it.items }.filter { it.isInCart }.sumOf { it.priceCents }
  val cartTax = (cartSubtotal * uiState.taxRate).toLong()
  val cartTotal = cartSubtotal + cartTax

  val closeSidePanel = {
    sidePanelType = null
    showAddDialog = false
    showStoreSelectDialog = false
    viewModel.dismissReceiptDialog()
  }

  Scaffold(
      floatingActionButton = {
        if (!actualIsExpanded || sidePanelType == null) {
          Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (shoppingModeStoreId != null) {
              FloatingActionButton(
                  onClick = {
                    val transaction = viewModel.createShoppingTransaction(shoppingModeStoreId)
                    onTransaction(transaction) { committed ->
                        viewModel.commitTransaction(committed)
                        shoppingModeStoreId = null
                    }
                  },
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Icon(Icons.Default.Check, contentDescription = "Complete Trip")
              }
              FloatingActionButton(
                  onClick = { shoppingModeStoreId = null },
                  containerColor = MaterialTheme.colorScheme.errorContainer,
              ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Shopping Mode")
              }
            } else {
              FloatingActionButton(
                  onClick = {
                    if (actualIsExpanded) sidePanelType = "StoreSelect"
                    else showStoreSelectDialog = true
                  },
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Go Shopping")
              }

              FloatingActionButton(
                  onClick = {
                    if (actualIsExpanded) sidePanelType = "Add" else showAddDialog = true
                  }
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
              }
            }
          }
        }
      }
  ) { innerPadding ->
    if (actualIsExpanded) {
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // Main List
        Box(modifier = Modifier.weight(0.6f)) {
          ShoppingListContent(
              displayedSections = displayedSections,
              shoppingModeStoreId = shoppingModeStoreId,
              uiState = uiState,
              viewModel = viewModel,
          )
        }

        VerticalDivider(modifier = Modifier.width(1.dp))

        // Side Panel
        Box(modifier = Modifier.weight(0.4f)) {
          when {
            sidePanelType == "Add" -> {
              AddAnyItemForm(
                  allUnits = uiState.allUnits,
                  onClose = closeSidePanel,
                  onAddCustom = { name, qty, unitId, isPantry ->
                    viewModel.addCustomItem(name, qty, unitId, isPantry)
                    closeSidePanel()
                  },
              )
            }
            sidePanelType == "StoreSelect" -> {
              StoreSelectForm(
                  sections = uiState.sections,
                  onClose = closeSidePanel,
                  onSelect = { id ->
                    shoppingModeStoreId = id
                    closeSidePanel()
                  },
              )
            }
            sidePanelType == "Receipt" || showReceiptDialog -> {
              CompleteShoppingForm(
                  estimatedTotalCents = cartTotal,
                  taxRate = uiState.taxRate,
                  onClose = closeSidePanel,
                  onConfirm = { total, time, force ->
                    viewModel.submitReceiptTotal(total, time, force)
                    if (!force) closeSidePanel() else sidePanelType = "Discrepancy"
                  },
              )
            }
            sidePanelType == "Discrepancy" || showDiscrepancyDialog -> {
              val cartItems =
                  displayedSections.flatMap { it.items }.filter { it.isInCart && !it.isCustom }
              PriceUpdateForm(
                  cartItems = cartItems,
                  onClose = closeSidePanel,
                  onConfirm = { updates ->
                    val priceUpdates = updates.map { PriceUpdate(it.key, it.value.toInt()) }
                    viewModel.updatePricesAndFinalize(priceUpdates)
                    if (shoppingModeStoreId != null) shoppingModeStoreId = null
                    closeSidePanel()
                  },
              )
            }
            else -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select an action", color = MaterialTheme.colorScheme.outline)
              }
            }
          }
        }
      }
    } else {
      // Mobile View
      ShoppingListContent(
          displayedSections = displayedSections,
          shoppingModeStoreId = shoppingModeStoreId,
          uiState = uiState,
          viewModel = viewModel,
      )
    }
  }

  // Mobile Dialogs
  if (!actualIsExpanded) {
    if (showStoreSelectDialog) {
      AlertDialog(
          onDismissRequest = { showStoreSelectDialog = false },
          title = { Text("Select Store") },
          text = {
            StoreSelectContent(uiState.sections) { id ->
              shoppingModeStoreId = id
              showStoreSelectDialog = false
            }
          },
          confirmButton = {},
          dismissButton = {
            TextButton(onClick = { showStoreSelectDialog = false }) { Text("Cancel") }
          },
      )
    }

    if (showAddDialog) {
      AlertDialog(
          onDismissRequest = { showAddDialog = false },
          content = {
            AddAnyItemForm(
                allUnits = uiState.allUnits,
                onClose = { showAddDialog = false },
                onAddCustom = { name, qty, unitId, isPantry ->
                  viewModel.addCustomItem(name, qty, unitId, isPantry)
                  showAddDialog = false
                },
            )
          },
      )
    }

    if (showReceiptDialog) {
      AlertDialog(
          onDismissRequest = { viewModel.dismissReceiptDialog() },
          content = {
            CompleteShoppingForm(
                estimatedTotalCents = cartTotal,
                taxRate = uiState.taxRate,
                onClose = { viewModel.dismissReceiptDialog() },
                onConfirm = { total, time, force ->
                  viewModel.submitReceiptTotal(total, time, force)
                },
            )
          },
      )
    }

    if (showDiscrepancyDialog && pendingActualTotal != null) {
      val cartItems = displayedSections.flatMap { it.items }.filter { it.isInCart && !it.isCustom }
      AlertDialog(
          onDismissRequest = { viewModel.skipPriceUpdate() },
          content = {
            PriceUpdateForm(
                cartItems = cartItems,
                onClose = { viewModel.skipPriceUpdate() },
                onConfirm = { updates ->
                  val priceUpdates = updates.map { PriceUpdate(it.key, it.value.toInt()) }
                  viewModel.updatePricesAndFinalize(priceUpdates)
                  if (shoppingModeStoreId != null) shoppingModeStoreId = null
                },
            )
          },
      )
    }
  }
}

@Composable
fun ShoppingListContent(
    displayedSections: List<ShoppingListSection>,
    shoppingModeStoreId: Uuid?,
    uiState: ShoppingListUiState,
    viewModel: ShoppingListViewModel,
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    if (shoppingModeStoreId == null) {
      item { MpValidationWarning(warnings = uiState.warnings) }
    } else {
      val currentStoreWarnings =
          uiState.sections.find { it.storeId == shoppingModeStoreId }?.warnings ?: emptyList()
      item { MpValidationWarning(warnings = currentStoreWarnings) }
    }

    if (displayedSections.isEmpty() && shoppingModeStoreId != null) {
      item {
        Text(
            "No items for this store.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
      }
    }

    displayedSections.forEach { section ->
      item { ShoppingListHeader(section) }
      if (section.items.isEmpty()) {
        item {
          Text(
              "Nothing needed from ${section.storeName}.",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        items(section.items) { item ->
          ShoppingListItemRow(
              item = item,
              allStores = uiState.allStores,
              isShoppingMode = shoppingModeStoreId != null,
              onMoveToStore = { storeId -> viewModel.moveToStore(item.id, storeId) },
              onMarkOwned = { viewModel.markOwned(item) },
              onMarkUnowned = { viewModel.markUnowned(item) },
              onToggleCart = { viewModel.toggleCart(item.id) },
          )
          HorizontalDivider()
        }
      }
    }
  }
}

@Composable
fun StoreSelectContent(sections: List<ShoppingListSection>, onSelect: (Uuid) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    sections
        .filter { it.items.isNotEmpty() }
        .forEach { section ->
          TextButton(
              onClick = { onSelect(section.storeId) },
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              colors =
                  ButtonDefaults.textButtonColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant,
                      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                  section.storeName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
              )
              Column(horizontalAlignment = Alignment.End) {
                Text("${section.items.size} items", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$${String.format("%.2f", section.totalCents / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
  }
}

@Composable
fun StoreSelectForm(
    sections: List<ShoppingListSection>,
    onClose: () -> Unit,
    onSelect: (Uuid) -> Unit,
) {
  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = "Select Store",
      onClose = onClose,
      onSave = {}, // Not needed here
  ) {
    StoreSelectContent(sections, onSelect)
  }
}

@Composable
fun AddAnyItemForm(
    allUnits: List<UnitModel>,
    onClose: () -> Unit,
    onAddCustom: (String, Double, Uuid, Boolean) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var qtyStr by remember { mutableStateOf("1") }
  var selectedUnitName by remember { mutableStateOf("") }
  var isPantry by remember { mutableStateOf(true) }

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = "Add Shopping Item",
      onClose = onClose,
      onSave = {
        val q = qtyStr.toDoubleOrNull()
        val unit = allUnits.find { it.abbreviation == selectedUnitName }
        if (name.isNotBlank() && q != null && unit != null) {
          onAddCustom(name, q, unit.id, isPantry)
        }
      },
      saveEnabled =
          name.isNotBlank() && qtyStr.toDoubleOrNull() != null && selectedUnitName.isNotBlank(),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      MpOutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Item Name") },
          modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MpOutlinedTextField(
            value = qtyStr,
            onValueChange = { qtyStr = it },
            label = { Text("Qty") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Box(modifier = Modifier.weight(1f)) {
          SearchableDropdown(
              label = "Unit",
              options = allUnits.map { it.abbreviation },
              selectedOption = selectedUnitName,
              onOptionSelected = { selectedUnitName = it },
              onAddOption = {},
              onDeleteOption = {},
              deleteWarningMessage = "",
          )
        }
      }

      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { isPantry = !isPantry }
      ) {
          Checkbox(checked = isPantry, onCheckedChange = { isPantry = it })
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add to Pantry upon purchase")
      }
    }
  }
}

@Composable
fun CompleteShoppingForm(
    estimatedTotalCents: Long,
    taxRate: Double,
    onClose: () -> Unit,
    onConfirm: (Long, LocalTime, Boolean) -> Unit,
) {
  var actualTotalStr by remember { mutableStateOf((estimatedTotalCents / 100.0).toString()) }
  var showPriceUpdatePrompt by remember { mutableStateOf(false) }
  val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
  var selectedTime by remember { mutableStateOf(LocalTime(now.hour, now.minute)) }
  val timePickerState =
      rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute)
  var showTimePicker by remember { mutableStateOf(false) }
  val hasTax = taxRate > 0.0

  if (showPriceUpdatePrompt) {
    io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
        title = "Update Prices?",
        onClose = { showPriceUpdatePrompt = false },
        onSave = {
          val actualCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
          onConfirm(actualCents, selectedTime, true)
        },
    ) {
      Text(
          "The actual amount entered does not match our estimate. Would you like to review and update individual item prices?"
      )
    }
  } else {
    io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
        title = "Complete Trip",
        onClose = onClose,
        onSave = {
          val actual = actualTotalStr.toDoubleOrNull()
          if (actual != null) {
            val actualCents = (actual * 100).toLong()
            if (actualCents != estimatedTotalCents) showPriceUpdatePrompt = true
            else onConfirm(actualCents, selectedTime, false)
          }
        },
        saveEnabled = actualTotalStr.toDoubleOrNull() != null,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            if (hasTax) "Estimated Total: $${String.format("%.2f", estimatedTotalCents / 100.0)}"
            else "Estimated Subtotal: $${String.format("%.2f", estimatedTotalCents / 100.0)}"
        )
        Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
          Text("Trip Time: $selectedTime")
        }
        MpOutlinedTextField(
            value = actualTotalStr,
            onValueChange = { actualTotalStr = it },
            label = { Text("Actual Total ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }

  if (showTimePicker) {
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
              }
          ) {
            Text("Ok")
          }
        },
        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        text = { TimePicker(state = timePickerState) },
    )
  }
}

@Composable
fun PriceUpdateForm(
    cartItems: List<ShoppingListItemUi>,
    onClose: () -> Unit,
    onConfirm: (Map<Uuid, Long>) -> Unit,
) {
  val priceEdits = remember { mutableStateMapOf<Uuid, String>() }
  LaunchedEffect(cartItems) {
    cartItems.forEach { item ->
      if (!priceEdits.containsKey(item.id))
          priceEdits[item.id] = (item.priceCents / 100.0).toString()
    }
  }

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = "Update Prices",
      onClose = onClose,
      onSave = {
        val updates = mutableMapOf<Uuid, Long>()
        priceEdits.forEach { (id, priceStr) ->
          priceStr.toDoubleOrNull()?.let { updates[id] = (it * 100).toLong() }
        }
        onConfirm(updates)
      },
  ) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(cartItems) { item ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${item.quantity} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          MpOutlinedTextField(
              value = priceEdits[item.id] ?: "",
              onValueChange = { priceEdits[item.id] = it },
              modifier = Modifier.width(100.dp),
              label = { Text("$") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
        }
        HorizontalDivider()
      }
    }
  }
}

@Composable
fun ShoppingListHeader(section: ShoppingListSection) {
  Surface(
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          section.storeName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
      )
      val formattedTotal = (section.totalCents / 100.0).toString()
      Text("$$formattedTotal", style = MaterialTheme.typography.titleSmall)
    }
  }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItemUi,
    allStores: List<Store>,
    isShoppingMode: Boolean,
    onMoveToStore: (Uuid) -> Unit,
    onMarkOwned: () -> Unit,
    onMarkUnowned: () -> Unit,
    onToggleCart: () -> Unit,
) {
  var showStoreDialog by remember { mutableStateOf(false) }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .let {
                  if (isShoppingMode) {
                      it.toggleable(
                          value = item.isInCart,
                          onValueChange = { onToggleCart() },
                          role = Role.Checkbox,
                      )
                  } else it
              },
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (isShoppingMode) {
        Checkbox(
            checked = item.isInCart,
            onCheckedChange = null, // Handled by toggleable Row
        )

        Spacer(modifier = Modifier.width(16.dp))
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
          item.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          textDecoration = if (item.isInCart) TextDecoration.LineThrough else null,
      )
      val formattedPrice = (item.priceCents / 100.0).toString()
      Text(
          "${item.quantity} ${item.unit} • $$formattedPrice",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    IconButton(onClick = { showStoreDialog = true }) {
      Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
  }

  if (showStoreDialog) {
    AlertDialog(
        onDismissRequest = { showStoreDialog = false },
        title = { Text("Item Options") },
        text = {
          Column {
            Text("Move to Store:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            allStores.forEach { store ->
              TextButton(
                  onClick = {
                    onMoveToStore(store.id)
                    showStoreDialog = false
                  },
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Text(store.name)
              }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(
                onClick = {
                  if (item.isOwned) onMarkUnowned() else onMarkOwned()
                  showStoreDialog = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
              Text(if (item.isOwned) "Mark as Unowned" else "Mark as Owned")
            }
          }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { showStoreDialog = false }) { Text("Cancel") } },
    )
  }
}
