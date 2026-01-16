@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.shoppinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.ingredients.Store
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ShoppingListView() {
    val viewModel = viewModel { ShoppingListViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shopping List") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Stores
            uiState.sections.forEach { section ->
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
                            onMoveToStore = { storeId -> viewModel.moveToStore(item.ingredientId, storeId) },
                            onMarkOwned = { viewModel.markOwned(item.ingredientId) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // 2. Owned Items
            if (uiState.ownedItems.isNotEmpty()) {
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Already Owned",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                items(uiState.ownedItems) { item ->
                    ShoppingListItemRow(
                        item = item,
                        allStores = uiState.allStores,
                        onMoveToStore = { storeId -> viewModel.moveToStore(item.ingredientId, storeId) }, // Move back to list
                        onMarkOwned = { /* Already owned */ }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ShoppingListHeader(section: ShoppingListSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
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
    onMarkOwned: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${String.format("%.1f", item.quantity)} ${item.unit} (Need ${String.format("%.1f", item.requiredQuantity)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!item.isOwned) {
            Text(
                text = "$${String.format("%.2f", item.priceCents / 100.0)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!item.isOwned) {
                    DropdownMenuItem(
                        text = { Text("Mark as Owned") },
                        onClick = {
                            onMarkOwned()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Check, null) }
                    )
                    HorizontalDivider()
                }
                
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


typealias StoreId = Uuid
