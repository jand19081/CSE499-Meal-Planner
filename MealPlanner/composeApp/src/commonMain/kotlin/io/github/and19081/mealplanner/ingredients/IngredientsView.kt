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
import androidx.compose.material3.HorizontalDivider
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
            // Create New Option
            if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
                item {
                    CreateNewItemRow(
                        searchQuery = uiState.searchQuery,
                        onClick = {
                            selectedIngredient = null
                            creationName = uiState.searchQuery
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }

            uiState.groupedIngredients.forEach { (header, ingredients) ->
                stickyHeader {
                    ListSectionHeader(text = header)
                }

                items(ingredients) { ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        allStores = uiState.allStores,
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
            onDelete = if (selectedIngredient != null) { { viewModel.deleteIngredient(selectedIngredient!!.id) } } else null,
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
    allStores: List<Store>,
    onEditClick: () -> Unit
) {
    // Calculate Best Price
    val bestOption = ingredient.purchaseOptions.minByOrNull { it.unitPrice }
    val bestPriceString = if (bestOption != null) {
        val store = allStores.find { it.id == bestOption.storeId }?.name ?: "Unknown"
        val price = bestOption.priceCents / 100.0
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
                val storeName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown Store"
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
    onDelete: (() -> Unit)? = null,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val ingredientId = remember { ingredient?.id ?: Uuid.random() }
    
    var name by remember { mutableStateOf(ingredient?.name ?: initialName) }
    var category by remember { mutableStateOf(ingredient?.category ?: "") }
    var purchaseOptions by remember { mutableStateOf(ingredient?.purchaseOptions ?: emptyList()) }
    var bridges by remember { mutableStateOf(ingredient?.conversionBridges ?: emptyList()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Purchase Options", "Conversions")

    MpEditDialogScaffold(
        title = if (ingredient == null) "Add Ingredient" else "Edit Ingredient",
        onDismiss = onDismiss,
        onSave = {
            val finalIngredient = Ingredient(
                id = ingredientId,
                name = name,
                category = category,
                purchaseOptions = purchaseOptions,
                conversionBridges = bridges
            )
            onSave(finalIngredient)
        },
        saveEnabled = name.isNotBlank() && category.isNotBlank(),
        onDelete = if (onDelete != null) {
            {
                onDelete()
                onDismiss()
            }
        } else null,
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it }
    ) {
        when (selectedTabIndex) {
            0 -> { // General Tab
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpOutlinedTextField(
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
                        onAddOption = { category = it },
                        onDeleteOption = { onDeleteCategory(it) },
                        deleteWarningMessage = "Deleting this category will remove ALL ingredients in it."
                    )
                }
            }
            1 -> { // Purchase Options Tab
                PurchaseOptionsEditor(
                    ingredientId = ingredientId,
                    options = purchaseOptions,
                    allStores = allStores,
                    onUpdateOptions = { purchaseOptions = it },
                    onAddStore = onAddStore,
                    onDeleteStore = onDeleteStore
                )
            }
            2 -> { // Conversions Tab
                ConversionBridgesEditor(
                    ingredientId = ingredientId,
                    currentBridges = bridges,
                    onUpdate = { bridges = it }
                )
            }
        }
    }
}

@Composable
fun PurchaseOptionsEditor(
    ingredientId: Uuid,
    options: List<PurchaseOption>,
    allStores: List<Store>,
    onUpdateOptions: (List<PurchaseOption>) -> Unit,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var editingOption by remember { mutableStateOf<PurchaseOption?>(null) }

    var storeName by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(MeasureUnit.EACH) }
    var unitExpanded by remember { mutableStateOf(false) }
    
    fun startEditing(opt: PurchaseOption?) {
        if (opt != null) {
            val store = allStores.find { it.id == opt.storeId }
            storeName = store?.name ?: ""
            priceStr = (opt.priceCents / 100.0).toString()
            qtyStr = opt.quantity.amount.toString()
            unit = opt.quantity.unit
            editingOption = opt
            isAdding = true
        } else {
            storeName = ""
            priceStr = ""
            qtyStr = ""
            unit = MeasureUnit.EACH
            editingOption = null
            isAdding = true
        }
    }

    Column {
        if (options.isEmpty() && !isAdding) {
            Text("No options added yet.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else if (!isAdding) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 150.dp)) {
                items(options) { opt ->
                    val sName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { startEditing(opt) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$sName: $${opt.priceCents / 100.0} / ${opt.quantity}", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdateOptions(options - opt) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdding) {
            MpButton(onClick = { startEditing(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Purchase Option")
            }
        } else {
            MpCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (editingOption == null) "New Option" else "Edit Option", style = MaterialTheme.typography.labelMedium)

                    SearchableDropdown(
                        label = "Store",
                        options = allStores.map { it.name },
                        selectedOption = storeName,
                        onOptionSelected = { storeName = it },
                        onAddOption = onAddStore,
                        onDeleteOption = onDeleteStore,
                        deleteWarningMessage = "This will remove this store and ALL associated prices from ALL ingredients."
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MpOutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        MeasureInputRow(
                            quantity = qtyStr,
                            onQuantityChange = { qtyStr = it },
                            unit = unit,
                            onUnitChange = { unit = it },
                            modifier = Modifier.weight(1.5f),
                            label = "Qty"
                        )
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val store = allStores.find { it.name == storeName }
                                val price = priceStr.toDoubleOrNull()
                                val qty = qtyStr.toDoubleOrNull()

                                if (store != null && price != null && qty != null) {
                                    val newOpt = PurchaseOption(
                                        id = editingOption?.id ?: Uuid.random(),
                                        ingredientId = ingredientId,
                                        storeId = store.id,
                                        priceCents = (price * 100).toLong(),
                                        quantity = Measure(qty, unit)
                                    )
                                    val newList = if (editingOption != null) {
                                        options.map { if (it.id == editingOption!!.id) newOpt else it }
                                    } else {
                                        options + newOpt
                                    }
                                    onUpdateOptions(newList)
                                    isAdding = false
                                }
                            },
                            enabled = storeName.isNotBlank() && priceStr.isNotBlank() && qtyStr.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversionBridgesEditor(
    ingredientId: Uuid,
    currentBridges: List<UnitBridge>,
    onUpdate: (List<UnitBridge>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    
    var fromQtyStr by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf(MeasureUnit.EACH) }
    var toQtyStr by remember { mutableStateOf("") }
    var toUnit by remember { mutableStateOf(MeasureUnit.GRAM) }
    
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Column {
        if (currentBridges.isEmpty() && !isAdding) {
            Text("No conversions defined.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else if (!isAdding) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 150.dp)) {
                items(currentBridges) { bridge ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${bridge.from} = ${bridge.to}", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdate(currentBridges - bridge) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdding) {
            MpButton(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Conversion")
            }
        } else {
            MpCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New Conversion (e.g. 1 Cup = 120 Grams)", style = MaterialTheme.typography.labelMedium)

                    MeasureInputRow(
                        quantity = fromQtyStr,
                        onQuantityChange = { fromQtyStr = it },
                        unit = fromUnit,
                        onUnitChange = { fromUnit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Qty"
                    )
                    
                    Text("EQUALS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))

                    MeasureInputRow(
                        quantity = toQtyStr,
                        onQuantityChange = { toQtyStr = it },
                        unit = toUnit,
                        onUnitChange = { toUnit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Qty"
                    )

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val fq = fromQtyStr.toDoubleOrNull()
                                val tq = toQtyStr.toDoubleOrNull()
                                if (fq != null && tq != null) {
                                    val newBridge = UnitBridge(
                                        ingredientId = ingredientId,
                                        from = Measure(fq, fromUnit),
                                        to = Measure(tq, toUnit)
                                    )
                                    onUpdate(currentBridges + newBridge)
                                    isAdding = false
                                }
                            },
                            enabled = fromQtyStr.isNotBlank() && toQtyStr.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}