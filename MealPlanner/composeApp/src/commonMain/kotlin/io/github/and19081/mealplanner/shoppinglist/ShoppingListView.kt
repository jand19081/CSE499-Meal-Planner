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
import androidx.compose.material.icons.filled.ShoppingCartCheckout
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
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Store
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ShoppingListView() {
    val viewModel = viewModel { ShoppingListViewModel() }
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
                    MpFloatingActionButton(
                        onClick = { viewModel.openReceiptDialog(shoppingModeStoreId) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete Trip")
                    }
                    MpFloatingActionButton(
                        onClick = { shoppingModeStoreId = null },
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Shopping Mode")
                    }
                } else {
                    MpFloatingActionButton(
                        onClick = { showStoreSelectDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Go Shopping")
                    }
                    
                    MpFloatingActionButton(onClick = { showAddDialog = true }) {
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

            // 2. Owned Items (Only show if not in shopping mode)
            if (shoppingModeStoreId == null && uiState.ownedItems.isNotEmpty()) {
                stickyHeader {
                    ListSectionHeader(text = "Already Owned")
                }
                items(uiState.ownedItems) { item ->
                    ShoppingListItemRow(
                        item = item,
                        allStores = uiState.allStores,
                        onMoveToStore = { storeId -> viewModel.moveToStore(item.id, storeId) },
                        onMarkOwned = { /* Already owned */ },
                        onMarkUnowned = { viewModel.markUnowned(item) },
                        onToggleCart = { /* No cart for owned */ }
                    )
                    HorizontalDivider()
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
                        MpTextButton(
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
                                    Text("${section.items.size} items", style = MaterialTheme.typography.bodyMedium)
                                    Text("$${String.format("%.2f", section.totalCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                MpTextButton(onClick = { showStoreSelectDialog = false }) { Text("Cancel") }
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
            onDismiss = { viewModel.dismissReceiptDialog() },
            onConfirm = { actualTotal, _ ->
                viewModel.submitReceiptTotal(actualTotal)
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
    MpSurface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
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
                    text = "Subtotal: $${String.format("%.2f", section.subtotalCents / 100.0)} + Tax: $${String.format("%.2f", section.taxCents / 100.0)}",
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
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit
) {
    var actualTotalStr by remember { mutableStateOf((estimatedTotalCents / 100.0).toString()) }
    var showPriceUpdatePrompt by remember { mutableStateOf(false) }

    if (showPriceUpdatePrompt) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Update Prices?") },
            text = { Text("The actual total differed from the estimate. Would you like to review item prices?") },
            confirmButton = {
                MpButton(
                    onClick = { 
                        onConfirm((actualTotalStr.toDoubleOrNull() ?: 0.0 * 100).toLong(), true)
                    }
                ) {
                    Text("Yes, Review Prices")
                }
            },
            dismissButton = {
                MpTextButton(
                    onClick = {
                         onConfirm((actualTotalStr.toDoubleOrNull() ?: 0.0 * 100).toLong(), false)
                    }
                ) { Text("No, Just Complete") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Complete Shopping Trip") },
            text = {
                Column {
                    Text("Estimated Total: $${String.format("%.2f", estimatedTotalCents / 100.0)}")
                    Spacer(modifier = Modifier.height(16.dp))
                    MpOutlinedTextField(
                        value = actualTotalStr,
                        onValueChange = { actualTotalStr = it },
                        label = { Text("Actual Receipt Total ($)") },
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
                            if (kotlin.math.abs(actualCents - estimatedTotalCents) > 50) { // > $0.50 diff
                                showPriceUpdatePrompt = true
                            } else {
                                onConfirm(actualCents, false)
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
