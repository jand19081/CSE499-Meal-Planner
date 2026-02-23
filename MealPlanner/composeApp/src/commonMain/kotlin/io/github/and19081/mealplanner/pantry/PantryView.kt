@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.pantry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField // Added import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun PantryView(
    viewModel: PantryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PantryItemUi?>(null) }
    
    // For adding new
    var isAddingNew by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = "", // TODO: Bind to uiState.searchQuery when available
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Pantry...",
                isSortByPrimary = true,
                onToggleSort = { },
                onAddClick = {
                    selectedItem = null
                    isAddingNew = true
                    showEditDialog = true
                }
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
                        onEditClick = {
                            selectedItem = item
                            isAddingNew = false
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }
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
