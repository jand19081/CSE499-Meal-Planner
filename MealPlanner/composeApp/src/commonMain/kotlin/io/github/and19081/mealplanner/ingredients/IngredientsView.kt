@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.ingredients

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun IngredientsView() {
    val viewModel = viewModel { IngredientsViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var creationName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Ingredients...",
                isSortByPrimary = uiState.isSortByCategory,
                onToggleSort = { viewModel.toggleSortMode() },
                onAddClick = {
                    selectedIngredient = null
                    creationName = ""
                    showEditDialog = true
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Create New Option (if searching and not found)
            if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIngredient = null
                                creationName = uiState.searchQuery
                                showEditDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Create '${uiState.searchQuery}'",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add this new ingredient to the database",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            uiState.groupedIngredients.forEach { (header, ingredients) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = header,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(ingredients) { ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        onEditClick = {
                            selectedIngredient = ingredient
                            creationName = ""
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showEditDialog) {
        IngredientEditDialog(
            ingredient = selectedIngredient,
            initialName = creationName,
            allStores = uiState.allStores,
            allCategories = uiState.allCategories,
            onDismiss = { showEditDialog = false },
            onSave = { updatedIngredient ->
                viewModel.saveIngredient(updatedIngredient)
                showEditDialog = false
            },
            onAddStore = { viewModel.addStore(it) },
            onDeleteStore = { storeName ->
                val store = uiState.allStores.find { it.name == storeName }
                if (store != null) viewModel.deleteStore(store.id)
            },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }
}

@Composable
fun IngredientRow(
    ingredient: Ingredient,
    onEditClick: () -> Unit
) {
    // Calculate Best Price
    val bestOption = ingredient.purchaseOptions.minByOrNull { it.unitPrice }
    val bestPriceString = if (bestOption != null) {
        val store = MealPlannerRepository.stores.value.find { it.id == bestOption.storeId }?.name ?: "Unknown"
        val price = bestOption.priceCents / 100.0
        // Price per 1 Unit
        val perUnit = price / bestOption.quantity.amount
        val rounded = String.format("%.2f", perUnit)
        "Best: $store - $$rounded/${bestOption.quantity.unit.name}"
    } else {
        "No prices"
    }

    val subtitle = "${ingredient.category} • ${ingredient.purchaseOptions.size} options • $bestPriceString"

    ExpandableListItem(
        title = ingredient.name,
        subtitle = subtitle,
        onEditClick = onEditClick
    ) {
        Text("Category: ${ingredient.category}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))

        if (ingredient.purchaseOptions.isEmpty()) {
            Text("No purchase options listed.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            Text("Purchase Options:", style = MaterialTheme.typography.labelMedium)
            ingredient.purchaseOptions.forEach { opt ->
                val storeName = MealPlannerRepository.stores.value.find { it.id == opt.storeId }?.name ?: "Unknown Store"
                Text("• $storeName: $${opt.priceCents / 100.0} for ${opt.quantity}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun IngredientEditDialog(
    ingredient: Ingredient?,
    initialName: String = "",
    allStores: List<Store>,
    allCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    // Local State for the Ingredient being edited
    var name by remember { mutableStateOf(ingredient?.name ?: initialName) }
    var category by remember { mutableStateOf(ingredient?.category ?: "") }
    var purchaseOptions by remember { mutableStateOf(ingredient?.purchaseOptions ?: emptyList()) }
    var bridges by remember { mutableStateOf(ingredient?.conversionBridges ?: emptyList()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Purchase Options", "Conversions")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ingredient == null) "Add Ingredient" else "Edit Ingredient") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    0 -> { // General Tab
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SearchableDropdown(
                                label = "Category",
                                options = allCategories,
                                selectedOption = category,
                                onOptionSelected = { category = it },
                                onAddOption = { category = it }, // Just set it
                                onDeleteOption = { onDeleteCategory(it) },
                                deleteWarningMessage = "Deleting this category will remove ALL ingredients in it."
                            )
                        }
                    }
                    1 -> { // Purchase Options Tab
                        PurchaseOptionsEditor(
                            options = purchaseOptions,
                            allStores = allStores,
                            onUpdateOptions = { purchaseOptions = it },
                            onAddStore = onAddStore,
                            onDeleteStore = onDeleteStore
                        )
                    }
                    2 -> { // Conversions Tab
                        // Minimal implementation for now
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Conversion Logic Coming Soon", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalIngredient = Ingredient(
                        id = ingredient?.id ?: Uuid.random(),
                        name = name,
                        category = category,
                        purchaseOptions = purchaseOptions,
                        conversionBridges = bridges
                    )
                    onSave(finalIngredient)
                },
                enabled = name.isNotBlank() && category.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PurchaseOptionsEditor(
    options: List<PurchaseOption>,
    allStores: List<Store>,
    onUpdateOptions: (List<PurchaseOption>) -> Unit,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }

    // New Option State
    var newStoreName by remember { mutableStateOf("") }
    var newPriceStr by remember { mutableStateOf("") }
    var newQtyStr by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf(MeasureUnit.EACH) }
    var unitExpanded by remember { mutableStateOf(false) }

    Column {
        // List Existing Options
        if (options.isEmpty()) {
            Text("No options added yet.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 150.dp)) {
                items(options) { opt ->
                    val storeName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$storeName: $${opt.priceCents / 100.0} / ${opt.quantity}", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdateOptions(options - opt) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add New Option Form
        if (!isAdding) {
            Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Purchase Option")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New Option", style = MaterialTheme.typography.labelMedium)

                    // Store Selector
                    SearchableDropdown(
                        label = "Store",
                        options = allStores.map { it.name },
                        selectedOption = newStoreName,
                        onOptionSelected = { newStoreName = it },
                        onAddOption = onAddStore,
                        onDeleteOption = onDeleteStore,
                        deleteWarningMessage = "This will remove this store and ALL associated prices from ALL ingredients."
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPriceStr,
                            onValueChange = { newPriceStr = it },
                            label = { Text("Price ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = newQtyStr,
                            onValueChange = { newQtyStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Unit Selector
                    Box {
                        OutlinedButton(onClick = { unitExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(newUnit.name)
                        }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            MeasureUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        newUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val store = allStores.find { it.name == newStoreName }
                                val price = newPriceStr.toDoubleOrNull()
                                val qty = newQtyStr.toDoubleOrNull()

                                if (store != null && price != null && qty != null) {
                                    val newOpt = PurchaseOption(
                                        ingredientId = Uuid.random(), // Temp ID, not used strictly here
                                        storeId = store.id,
                                        priceCents = (price * 100).toLong(),
                                        quantity = Measure(qty, newUnit)
                                    )
                                    onUpdateOptions(options + newOpt)
                                    isAdding = false
                                    // Reset fields
                                    newPriceStr = ""
                                    newQtyStr = ""
                                    newStoreName = ""
                                }
                            },
                            enabled = newStoreName.isNotBlank() && newPriceStr.isNotBlank() && newQtyStr.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
