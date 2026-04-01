@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.feature.pantry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.feature.recipes.EmptyDetailPlaceholder
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.ui.components.EmptyListMessage
import io.github.and19081.mealplanner.ui.components.ListControlToolbar
import io.github.and19081.mealplanner.ui.components.MpDetailScaffold
import io.github.and19081.mealplanner.ui.components.MpNumericStepper
import io.github.and19081.mealplanner.ui.components.SearchableDropdown
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun PantryView(
    viewModel: PantryViewModel,
    mode: Mode,
    isExpanded: Boolean,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedPantryItem by remember { mutableStateOf<PantryItemUi?>(null) }
    var selectedLeftoverItem by remember { mutableStateOf<LeftoverItemUi?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    val actualIsExpanded =
        when (mode) {
            Mode.AUTO -> isExpanded
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

    var showConsumeDialog by remember { mutableStateOf<PantryItemUi?>(null) }

    val onItemClick: (PantryItemUi) -> Unit = {
        selectedPantryItem = it
        selectedLeftoverItem = null
        isAdding = false
    }

    val onLeftoverClick: (LeftoverItemUi) -> Unit = {
        selectedLeftoverItem = it
        selectedPantryItem = null
        isAdding = false
    }

    val onConsumeClick: (PantryItemUi) -> Unit = {
        showConsumeDialog = it
    }

    val onDismissConsume: () -> Unit = {
        showConsumeDialog = null
    }

    val onAddClick: () -> Unit = {
        selectedPantryItem = null
        selectedLeftoverItem = null
        isAdding = true
    }

    val onDismissDetail: () -> Unit = {
        selectedPantryItem = null
        selectedLeftoverItem = null
        isAdding = false
    }

    showConsumeDialog?.let { itemToConsume ->
        ConsumePantryItemDialog(
            item = itemToConsume,
            onDismiss = onDismissConsume,
            onConsume = { qty ->
                viewModel.consumeItem(itemToConsume.batchId, qty)
            }
        )
    }

    if (actualIsExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            // List Pane
            Box(modifier = Modifier.weight(0.4f)) {
                PantryListPane(
                    uiState = uiState,
                    viewModel = viewModel,
                    onItemClick = onItemClick,
                    onLeftoverClick = onLeftoverClick,
                    onAddClick = onAddClick,
                    onConsumeClick = onConsumeClick,
                )
            }

            VerticalDivider(modifier = Modifier.width(1.dp))

            // Detail Pane
            Box(modifier = Modifier.weight(0.6f)) {
                if (selectedPantryItem != null || isAdding) {
                    PantryForm(
                        item = selectedPantryItem,
                        allIngredients = uiState.allIngredients,
                        allUnits = uiState.allUnits,
                        onDismiss = onDismissDetail,
                        onSave = { ingId, qty, unitId ->
                            viewModel.updateQuantity(ingId, qty, unitId)
                            onDismissDetail()
                        },
                        onDelete =
                            selectedPantryItem?.batchId?.let { id ->
                                {
                                    viewModel.deleteItem(id)
                                    onDismissDetail()
                                }
                            },
                        onAddIngredient = onAddIngredient,
                    )
                } else if (selectedLeftoverItem != null) {
                    val leftover = selectedLeftoverItem!!
                    LeftoverForm(
                        item = leftover,
                        onDismiss = onDismissDetail,
                        onSave = { id: Uuid, qty: Double ->
                            viewModel.updateLeftoverQuantity(id, qty)
                            onDismissDetail()
                        },
                        onDelete = {
                            viewModel.deleteLeftover(leftover.id)
                            onDismissDetail()
                        },
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        }
    } else {
        // Mobile View
        if (selectedPantryItem != null || isAdding) {
            PantryForm(
                item = selectedPantryItem,
                allIngredients = uiState.allIngredients,
                allUnits = uiState.allUnits,
                onDismiss = onDismissDetail,
                onSave = { ingId, qty, unitId ->
                    viewModel.updateQuantity(ingId, qty, unitId)
                    onDismissDetail()
                },
                onDelete =
                    selectedPantryItem?.batchId?.let { id ->
                        {
                            viewModel.deleteItem(id)
                            onDismissDetail()
                        }
                    },
                onAddIngredient = onAddIngredient,
            )
        } else if (selectedLeftoverItem != null) {
            val leftover = selectedLeftoverItem!!
            LeftoverForm(
                item = leftover,
                onDismiss = onDismissDetail,
                onSave = { id: Uuid, qty: Double ->
                    viewModel.updateLeftoverQuantity(id, qty)
                    onDismissDetail()
                },
                onDelete = {
                    viewModel.deleteLeftover(leftover.id)
                    onDismissDetail()
                },
            )
        } else {
            PantryListPane(
                uiState = uiState,
                viewModel = viewModel,
                onItemClick = onItemClick,
                onLeftoverClick = onLeftoverClick,
                onAddClick = onAddClick,
                onConsumeClick = onConsumeClick,
            )
        }
    }
}

@Composable
fun PantryListPane(
    uiState: PantryUiState,
    viewModel: PantryViewModel,
    onItemClick: (PantryItemUi) -> Unit,
    onLeftoverClick: (LeftoverItemUi) -> Unit,
    onAddClick: () -> Unit,
    onConsumeClick: (PantryItemUi) -> Unit,
) {
    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = "",
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Pantry...",
                isSortByPrimary = true,
                onToggleSort = {},
                onAddClick = onAddClick,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            if (uiState.leftovers.isNotEmpty()) {
                item {
                    Text(
                        "Leftovers",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(uiState.leftovers) { item ->
                    LeftoverItemRow(item = item, onClick = { onLeftoverClick(item) })
                    HorizontalDivider()
                }
            }

            if (uiState.items.isNotEmpty()) {
                item {
                    Text(
                        "Ingredients",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(uiState.items) { item ->
                    PantryItemRow(
                        item = item,
                        onEditClick = { onItemClick(item) },
                        onConsumeClick = { onConsumeClick(item) }
                    )
                    HorizontalDivider()
                }
            }

            if (uiState.items.isEmpty() && uiState.leftovers.isEmpty()) {
                item { EmptyListMessage(message = "Pantry is empty.", modifier = Modifier.padding(32.dp)) }
            }
        }
    }
}

@Composable
fun ConsumePantryItemDialog(
    item: PantryItemUi,
    onDismiss: () -> Unit,
    onConsume: (Double) -> Unit,
) {
    var quantityStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consume ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Available: ${String.format("%.2f", item.quantity)} ${item.unit.abbreviation}")
                MpNumericStepper(
                    value = quantityStr.toDoubleOrNull() ?: 0.0,
                    onValueChange = { quantityStr = it.toString() },
                    label = "Amount to consume",
                    modifier = Modifier.fillMaxWidth(),
                    step = 0.5,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty != null && qty > 0) {
                        onConsume(qty)
                        onDismiss()
                    }
                },
                enabled = quantityStr.toDoubleOrNull() != null
            ) {
                Text("Consume")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LeftoverItemRow(item: LeftoverItemUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                item.recipeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("Made on ${item.dateAdded}", style = MaterialTheme.typography.bodySmall)
        }
        Text("${item.remainingServings} servings")
    }
}

@Composable
fun EmptyDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inventory,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a pantry item to view details", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun PantryItemRow(
    item: PantryItemUi,
    onEditClick: () -> Unit,
    onConsumeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditClick)
        ) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                item.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                "${String.format("%.2f", item.quantity)} ${item.unit.abbreviation}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (item.quantity > 0) {
                Button(
                    onClick = onConsumeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Consume",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Consume", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PantryForm(
    item: PantryItemUi?,
    allIngredients: List<FoodItem>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (Uuid, Double, Uuid) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
) {
    var selectedIngName by remember(item) { mutableStateOf(item?.name ?: "") }
    var quantityStr by remember(item) { mutableStateOf(item?.quantity?.toString() ?: "") }
    var selectedUnitName by remember(item) { mutableStateOf(item?.unit?.abbreviation ?: "") }

    MpDetailScaffold(
        title = if (item == null) "Add to Pantry" else "Update Item",
        onClose = onDismiss,
        onSave = {
            val qty = quantityStr.toDoubleOrNull()
            val ingId =
                if (item != null) item.id else allIngredients.find { it.name == selectedIngName }?.id
            val unit = allUnits.find { it.abbreviation == selectedUnitName }

            if (ingId != null && qty != null && unit != null) {
                onSave(ingId, qty, unit.id)
            }
        },
        saveEnabled =
            (item != null || selectedIngName.isNotBlank()) &&
            quantityStr.toDoubleOrNull() != null &&
            selectedUnitName.isNotBlank(),
        onDelete = onDelete,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (item == null) {
                SearchableDropdown(
                    label = "Ingredient",
                    options = allIngredients.map { it.name },
                    selectedOption = selectedIngName,
                    onOptionSelected = { selectedIngName = it },
                    onAddOption = { name ->
                        onAddIngredient(name) { newIng -> selectedIngName = newIng.name }
                    },
                    onDeleteOption = {},
                    deleteWarningMessage = "",
                )
            } else {
                Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    item.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpNumericStepper(
                    value = quantityStr.toDoubleOrNull() ?: 0.0,
                    onValueChange = { quantityStr = it.toString() },
                    label = "Quantity",
                    modifier = Modifier.weight(1f),
                    step = 1.0,
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
        }
    }
}

@Composable
fun LeftoverForm(
    item: LeftoverItemUi,
    onDismiss: () -> Unit,
    onSave: (Uuid, Double) -> Unit,
    onDelete: () -> Unit,
) {
    var quantityStr by remember(item) { mutableStateOf(item.remainingServings.toString()) }

    MpDetailScaffold(
        title = "Update Leftovers",
        onClose = onDismiss,
        onSave = {
            val qty = quantityStr.toDoubleOrNull()
            if (qty != null) {
                onSave(item.id, qty)
            }
        },
        saveEnabled = quantityStr.toDoubleOrNull() != null,
        onDelete = onDelete,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                item.recipeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Made on ${item.dateAdded}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MpNumericStepper(
                value = quantityStr.toDoubleOrNull() ?: 0.0,
                onValueChange = { quantityStr = it.toString() },
                label = "Remaining Servings",
                modifier = Modifier.fillMaxWidth(),
                step = 0.5,
            )
        }
    }
}
