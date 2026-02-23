@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.pantry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.uicomponents.DialogActionButtons
import io.github.and19081.mealplanner.uicomponents.EmptyListMessage
import io.github.and19081.mealplanner.uicomponents.ListControlToolbar
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField 
import io.github.and19081.mealplanner.uicomponents.MpNumericStepper 
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import io.github.and19081.mealplanner.settings.Mode

@Composable
fun PantryView(
    viewModel: PantryViewModel,
    mode: Mode
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PantryItemUi?>(null) }
    
    BoxWithConstraints {
        val isExpanded = when (mode) {
            Mode.AUTO -> maxWidth > 840.dp
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                // List Pane
                Box(modifier = Modifier.weight(0.4f)) {
                    PantryListPane(
                        uiState = uiState,
                        viewModel = viewModel,
                        onItemClick = { selectedItem = it },
                        onAddClick = {
                            selectedItem = null
                            showEditDialog = true
                        }
                    )
                }

                VerticalDivider(modifier = Modifier.width(1.dp))

                // Detail Pane
                Box(modifier = Modifier.weight(0.6f)) {
                    val item = selectedItem
                    if (item != null) {
                        PantryDetailPane(
                            item = item,
                            allIngredients = uiState.allIngredients,
                            allUnits = uiState.allUnits,
                            onSave = { ingId, qty, unitId ->
                                viewModel.updateQuantity(ingId, qty, unitId)
                                // We don't need to update selectedItem here as the UI state will refresh
                            }
                        )
                    } else {
                        EmptyDetailPlaceholder()
                    }
                }
            }
        } else {
            // Mobile View
            PantryListPane(
                uiState = uiState,
                viewModel = viewModel,
                onItemClick = { 
                    selectedItem = it
                    showEditDialog = true 
                },
                onAddClick = {
                    selectedItem = null
                    showEditDialog = true
                }
            )
        }
    }

    if (showEditDialog) {
        PantryEditDialog(
            item = selectedItem,
            allIngredients = uiState.allIngredients,
            allUnits = uiState.allUnits,
            onDismiss = { showEditDialog = false },
            onSave = { ingId, qty, unitId ->
                viewModel.updateQuantity(ingId, qty, unitId)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun PantryListPane(
    uiState: PantryUiState,
    viewModel: PantryViewModel,
    onItemClick: (PantryItemUi) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = "", 
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Pantry...",
                isSortByPrimary = true,
                onToggleSort = { },
                onAddClick = onAddClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (uiState.items.isEmpty()) {
                 item {
                    EmptyListMessage(
                        message = "Pantry is empty.",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                items(uiState.items) { item ->
                    PantryItemRow(
                        item = item,
                        onEditClick = { onItemClick(item) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PantryDetailPane(
    item: PantryItemUi,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onSave: (Uuid, Double, Uuid) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(item.category, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))

            var quantityStr by remember(item) { mutableStateOf(item.quantity.toString()) }
            var selectedUnitName by remember(item) { mutableStateOf(item.unit.abbreviation) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpNumericStepper(
                        value = quantityStr.toDoubleOrNull() ?: 0.0,
                        onValueChange = { quantityStr = it.toString() },
                        label = "Quantity",
                        modifier = Modifier.weight(1f),
                        step = 1.0
                    )
                    
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

                Button(
                    onClick = {
                        val qty = quantityStr.toDoubleOrNull()
                        val unit = allUnits.find { it.abbreviation == selectedUnitName }
                        if (qty != null && unit != null) {
                            onSave(item.id, qty, unit.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = quantityStr.toDoubleOrNull() != null && selectedUnitName.isNotBlank()
                ) {
                    Text("Update Quantity")
                }
            }
        }
    }
}

@Composable
fun EmptyDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(androidx.compose.material.icons.Icons.Default.Inventory, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a pantry item to view details", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun PantryItemRow(
    item: PantryItemUi,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${String.format("%.2f", item.quantity)} ${item.unit.abbreviation}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PantryEditDialog(
    item: PantryItemUi?,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (Uuid, Double, Uuid) -> Unit
) {
    var selectedIngName by remember { mutableStateOf(item?.name ?: "") }
    var quantityStr by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var selectedUnitName by remember { mutableStateOf(item?.unit?.abbreviation ?: (allUnits.firstOrNull()?.abbreviation ?: "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add to Pantry" else "Update Quantity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item == null) {
                    SearchableDropdown(
                        label = "Ingredient",
                        options = allIngredients.map { it.name },
                        selectedOption = selectedIngName,
                        onOptionSelected = { selectedIngName = it },
                        onAddOption = {}, 
                        onDeleteOption = {},
                        deleteWarningMessage = ""
                    )
                } else {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                }

                Row {
                    MpOutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
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
                    val qty = quantityStr.toDoubleOrNull()
                    val ingId = if (item != null) item.id else allIngredients.find { it.name == selectedIngName }?.id
                    val unit = allUnits.find { it.abbreviation == selectedUnitName }
                    
                    if (ingId != null && qty != null && unit != null) {
                        onSave(ingId, qty, unit.id)
                    }
                },
                saveEnabled = (item != null || selectedIngName.isNotBlank()) && quantityStr.toDoubleOrNull() != null && selectedUnitName.isNotBlank()
            )
        },
        dismissButton = {}
    )
}
