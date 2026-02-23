@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.shoppinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.uicomponents.DialogActionButtons
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.domain.DataWarning
import androidx.compose.material.icons.filled.Warning
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ShoppingListView(
    viewModel: ShoppingListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
    val showDiscrepancyDialog by viewModel.showDiscrepancyDialog.collectAsState()
    val pendingActualTotal by viewModel.pendingActualTotal.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showStoreSelectDialog by remember { mutableStateOf(false) }
    var shoppingModeStoreId by remember { mutableStateOf<Uuid?>(null) }

    // Calculate displayed sections based on mode
    val displayedSections = if (shoppingModeStoreId != null) {
        uiState.sections.filter { it.storeId == shoppingModeStoreId }
    } else {
        uiState.sections
    }

    // Calculate total needed cost (un-owned items) for displayed sections
    val totalEstimatedCost = displayedSections.sumOf { it.totalCents }

    // Calculate cart total (checked items only) for completion
    val cartSubtotal = displayedSections.flatMap { it.items }.filter { it.isInCart }.sumOf { it.priceCents }
    val cartTax = (cartSubtotal * uiState.taxRate).toLong()
    val cartTotal = cartSubtotal + cartTax

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (shoppingModeStoreId != null) {
                    FloatingActionButton(
                        onClick = { viewModel.openReceiptDialog(shoppingModeStoreId) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete Trip")
                    }
                    FloatingActionButton(
                        onClick = { shoppingModeStoreId = null },
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Shopping Mode")
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showStoreSelectDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Go Shopping")
                    }

                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Global Warnings
            if (shoppingModeStoreId == null) {
                item {
                    MpValidationWarning(warnings = uiState.warnings)
                }
            } else {
                val currentStoreWarnings = uiState.sections.find { it.storeId == shoppingModeStoreId }?.warnings ?: emptyList()
                item {
                    MpValidationWarning(warnings = currentStoreWarnings)
                }
            }

            // 1. Stores
            if (displayedSections.isEmpty() && shoppingModeStoreId != null) {
                 item {
                     Text(
                        "No items for this store.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                     )
                 }
            }
            
            displayedSections.forEach { section ->
                item {
                    ShoppingListHeader(section)
                }

                if (section.items.isEmpty()) {
                    item {
                        Text(
                            "Nothing needed from ${section.storeName}.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(section.items) { item ->
                        ShoppingListItemRow(
                            item = item,
                            allStores = uiState.allStores,
                            onMoveToStore = { storeId -> viewModel.moveToStore(item.id, storeId) },
                            onMarkOwned = { viewModel.markOwned(item) },
                            onMarkUnowned = { viewModel.markUnowned(item) },
                            onToggleCart = { viewModel.toggleCart(item.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    
    if (showStoreSelectDialog) {
        AlertDialog(
            onDismissRequest = { showStoreSelectDialog = false },
            title = { Text("Select Store") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    uiState.sections.filter { it.items.isNotEmpty() }.forEach { section ->
                        TextButton(
                            onClick = {
                                shoppingModeStoreId = section.storeId
                                showStoreSelectDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    section.storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${section.items.size} items",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "$${String.format("%.2f", section.totalCents / 100.0)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStoreSelectDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showAddDialog) {
        AddAnyItemDialog(
            allUnits = uiState.allUnits,
            onDismiss = { showAddDialog = false },
            onAddCustom = { name, qty, unitId -> 
                viewModel.addCustomItem(name, qty, unitId)
                showAddDialog = false
            }
        )
    }

    if (showReceiptDialog) {
        CompleteShoppingDialog(
            estimatedTotalCents = cartTotal,
            taxRate = uiState.taxRate,
            onDismiss = { viewModel.dismissReceiptDialog() },
            onConfirm = { actualTotal, time, forceUpdate ->
                viewModel.submitReceiptTotal(actualTotal, time, forceUpdate)
            }
        )
    }
    
    if (showDiscrepancyDialog && pendingActualTotal != null) {
        // Collect cart items to edit from displayed sections
        val cartItems = displayedSections.flatMap { it.items }.filter { it.isInCart && !it.isCustom }
        
        PriceUpdateDialog(
            cartItems = cartItems,
            onDismiss = { 
                viewModel.skipPriceUpdate()
                if (shoppingModeStoreId != null) shoppingModeStoreId = null
            },
            onConfirm = { updates ->
                val priceUpdates = updates.map { PriceUpdate(it.key, it.value.toInt()) }
                viewModel.updatePricesAndFinalize(priceUpdates)
                if (shoppingModeStoreId != null) shoppingModeStoreId = null
            }
        )
    }
}

@Composable
fun ShoppingListHeader(section: ShoppingListSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = section.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$${String.format("%.2f", section.totalCents / 100.0)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (section.items.isNotEmpty()) {
                Text(
                    text = "Subtotal: $${
                        String.format(
                            "%.2f",
                            section.subtotalCents / 100.0
                        )
                    } + Tax: $${String.format("%.2f", section.taxCents / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ShoppingListItemRow(
    item: ShoppingListItemUi,
    allStores: List<Store>,
    onMoveToStore: (Uuid) -> Unit,
    onMarkOwned: () -> Unit,
    onMarkUnowned: () -> Unit,
    onToggleCart: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isInCart) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            .clickable { if (!item.isOwned) onToggleCart() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isInCart || item.isOwned,
            onCheckedChange = { if (!item.isOwned) onToggleCart() },
            enabled = !item.isOwned
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.isInCart || item.isOwned) TextDecoration.LineThrough else null,
                color = if (item.isInCart || item.isOwned) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
            val qtyText = if (item.isCustom) {
                "${String.format("%.1f", item.quantity)} ${item.unit}"
            } else {
                "${String.format("%.1f", item.quantity)} ${item.unit} (Need ${String.format("%.1f", item.requiredQuantity)})"
            }
            Text(
                text = qtyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!item.isOwned && !item.isCustom) {
            Text(
                text = "$${String.format("%.2f", item.priceCents / 100.0)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp),
                color = if (item.isInCart) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!item.isOwned) {
                    DropdownMenuItem(
                        text = { Text("Already Owned (Move to Pantry)") },
                        onClick = {
                            onMarkOwned()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Check, null) }
                    )
                } else {
                     DropdownMenuItem(
                        text = { Text("I don't have this (Move to List)") },
                        onClick = {
                            onMarkUnowned()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Close, null) }
                    )
                }
                
                if (!item.isCustom) {
                    HorizontalDivider()
                    Text("Move to...", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                    
                    allStores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store.name) },
                            onClick = {
                                onMoveToStore(store.id)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddAnyItemDialog(
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onAddCustom: (String, Double, Uuid) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var selectedUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MpOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row {
                    MpOutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        SearchableDropdown(
                            label = "Unit",
                            options = allUnits.map { it.abbreviation },
                            selectedOption = selectedUnitName,
                            onOptionSelected = { selectedUnitName = it },
                            onAddOption = {},
                            onDeleteOption = {},
                            deleteWarningMessage = ""
                        )
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButtons(
                onCancel = onDismiss,
                onSave = {
                    val q = qtyStr.toDoubleOrNull()
                    val unit = allUnits.find { it.abbreviation == selectedUnitName }
                    if (name.isNotBlank() && q != null && unit != null) {
                        onAddCustom(name, q, unit.id)
                    }
                },
                saveEnabled = name.isNotBlank() && qtyStr.isNotBlank() && selectedUnitName.isNotBlank()
            )
        },
        dismissButton = {}
    )
}

@Composable
fun CompleteShoppingDialog(
    estimatedTotalCents: Long,
    taxRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalTime, Boolean) -> Unit
) {
    var actualTotalStr by remember { mutableStateOf((estimatedTotalCents / 100.0).toString()) }
    var showPriceUpdatePrompt by remember { mutableStateOf(false) }
    
    val now = remember { 
        val currentInstant = Clock.System.now()
        currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time 
    }
    var selectedTime by remember { mutableStateOf(LocalTime(now.hour, now.minute)) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )
    var showTimePicker by remember { mutableStateOf(false) }

    val hasTax = taxRate > 0.0
    val label = if (hasTax) "Actual Receipt Total (Incl. Tax) ($)" else "Actual Subtotal ($)"

    if (showPriceUpdatePrompt) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Update Prices?") },
            text = { Text("The actual amount entered does not match our estimate. Would you like to review and update individual item prices?") },
            confirmButton = {
                Button(
                    onClick = {
                        val actualCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        onConfirm(actualCents, selectedTime, true)
                    }
                ) {
                    Text("Yes, Review Prices")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val actualCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        onConfirm(actualCents, selectedTime, false)
                    }
                ) { Text("No, Just Complete") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Complete Shopping Trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (hasTax) "Estimated Total: $${String.format("%.2f", estimatedTotalCents / 100.0)}" else "Estimated Subtotal: $${String.format("%.2f", estimatedTotalCents / 100.0)}")
                    
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Trip Time: $selectedTime")
                    }

                    MpOutlinedTextField(
                        value = actualTotalStr,
                        onValueChange = { actualTotalStr = it },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                DialogActionButtons(
                    onCancel = onDismiss,
                    onSave = {
                        val actual = actualTotalStr.toDoubleOrNull()
                        if (actual != null) {
                            val actualCents = (actual * 100).toLong()
                            // Exact match check
                            if (actualCents != estimatedTotalCents) { 
                                showPriceUpdatePrompt = true
                            } else {
                                onConfirm(actualCents, selectedTime, false)
                            }
                        }
                    },
                    saveEnabled = actualTotalStr.isNotBlank(),
                    saveLabel = "Complete"
                )
            },
            dismissButton = {}
        )
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Ok") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTimePicker = false 
                }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
fun PriceUpdateDialog(
    cartItems: List<ShoppingListItemUi>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Uuid, Long>) -> Unit
) {
    // Local state to track price edits
    // Map: ItemId -> NewPriceString
    val priceEdits = remember { mutableStateMapOf<Uuid, String>() }
    
    // Init state
    LaunchedEffect(cartItems) {
        cartItems.forEach { item ->
            if (!priceEdits.containsKey(item.id)) {
                priceEdits[item.id] = (item.priceCents / 100.0).toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Item Prices") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(cartItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        MpOutlinedTextField(
                            value = priceEdits[item.id] ?: "",
                            onValueChange = { priceEdits[item.id] = it },
                            modifier = Modifier.width(100.dp),
                            label = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            DialogActionButtons(
                onCancel = onDismiss,
                onSave = {
                    val updates = mutableMapOf<Uuid, Long>()
                    priceEdits.forEach { (id, priceStr) ->
                        val price = priceStr.toDoubleOrNull()
                        if (price != null) {
                            updates[id] = (price * 100).toLong()
                        }
                    }
                    onConfirm(updates)
                },
                saveLabel = "Update & Finish"
            )
        },
        dismissButton = {}
    )
}
