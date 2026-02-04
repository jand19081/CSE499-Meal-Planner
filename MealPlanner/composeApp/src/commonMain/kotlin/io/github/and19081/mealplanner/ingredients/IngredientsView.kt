@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.ingredients

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.UiWrappers.CreateNewItemRow
import io.github.and19081.mealplanner.UiWrappers.ExpandableListItem
import io.github.and19081.mealplanner.UiWrappers.ListControlToolbar
import io.github.and19081.mealplanner.UiWrappers.ListSectionHeader
import io.github.and19081.mealplanner.UiWrappers.MpButton
import io.github.and19081.mealplanner.UiWrappers.MpCard
import io.github.and19081.mealplanner.UiWrappers.MpEditDialogScaffold
import io.github.and19081.mealplanner.UiWrappers.MpOutlinedTextField
import io.github.and19081.mealplanner.UiWrappers.MpTextButton
import io.github.and19081.mealplanner.UiWrappers.SearchableDropdown
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun IngredientsView() {
    val viewModel = viewModel { IngredientsViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var creationName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        categoryName = header,
                        packages = uiState.allPackages.filter { it.ingredientId == ingredient.id },
                        allStores = uiState.allStores,
                        allUnits = uiState.allUnits,
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
        val ingredientPackages = if (selectedIngredient != null) {
            uiState.allPackages.filter { it.ingredientId == selectedIngredient!!.id }
        } else emptyList()
        
        val ingredientBridges = if (selectedIngredient != null) {
            uiState.allBridges.filter { it.ingredientId == selectedIngredient!!.id }
        } else emptyList()

        IngredientEditDialog(
            ingredient = selectedIngredient,
            initialName = creationName,
            initialPackages = ingredientPackages,
            initialBridges = ingredientBridges,
            allStores = uiState.allStores,
            allCategories = uiState.allCategories,
            allUnits = uiState.allUnits,
            onDismiss = { showEditDialog = false },
            onSave = { updatedIngredient, newPackages, newBridges ->
                viewModel.saveIngredient(updatedIngredient)
                
                // Handling Packages
                val originalPackageIds = ingredientPackages.map { it.id }.toSet()
                val newPackageIds = newPackages.map { it.id }.toSet()
                val deletedPackageIds = originalPackageIds - newPackageIds
                
                deletedPackageIds.forEach { viewModel.deletePackage(it) }
                newPackages.forEach { viewModel.savePackage(it) }
                
                // Handling Bridges (Simplified: just save new, no delete logic exposed properly yet in VM for bridges specifically by ID diff, but we can do additive for now or implement full diff)
                // Assuming simple additive for bridges in this prototype
                newBridges.forEach { viewModel.saveBridge(it) }

                showEditDialog = false
            },
            onDelete = if (selectedIngredient != null) { { viewModel.deleteIngredient(selectedIngredient!!.id) } } else null,
            onAddStore = { viewModel.addStore(it) },
            onDeleteStore = { storeName ->
                val store = uiState.allStores.find { it.name == storeName }
                if (store != null) viewModel.deleteStore(store.id)
            },
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { catName ->
                val cat = uiState.allCategories.find { it.name == catName }
                if (cat != null) viewModel.deleteCategory(cat.id)
            }
        )
    }
}

@Composable
fun IngredientRow(
    ingredient: Ingredient,
    categoryName: String,
    packages: List<Package>,
    allStores: List<Store>,
    allUnits: List<UnitModel>,
    onEditClick: () -> Unit
) {
    // Calculate Best Price
    val bestOption = packages.minByOrNull { 
        if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE 
    }
    
    val bestPriceString = if (bestOption != null) {
        val store = allStores.find { it.id == bestOption.storeId }?.name ?: "Unknown"
        val price = bestOption.priceCents / 100.0
        val perUnit = if (bestOption.quantity > 0) price / bestOption.quantity else 0.0
        val rounded = String.format("%.2f", perUnit)
        val unitName = allUnits.find { it.id == bestOption.unitId }?.abbreviation ?: "?"
        "Best: $store - $$rounded/$unitName"
    } else {
        "No prices"
    }

    val subtitle = "$categoryName • ${packages.size} options • $bestPriceString"

    ExpandableListItem(
        title = ingredient.name,
        subtitle = subtitle,
        onEditClick = onEditClick
    ) {
        Text("Category: $categoryName", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))

        if (packages.isEmpty()) {
            Text(
                "No purchase options listed.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
        } else {
            Text("Purchase Options:", style = MaterialTheme.typography.labelMedium)
            packages.forEach { opt ->
                val storeName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown Store"
                val unitName = allUnits.find { it.id == opt.unitId }?.abbreviation ?: "?"
                Text(
                    "• $storeName: $${opt.priceCents / 100.0} for ${opt.quantity} $unitName",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun IngredientEditDialog(
    ingredient: Ingredient?,
    initialName: String = "",
    initialPackages: List<Package> = emptyList(),
    initialBridges: List<BridgeConversion> = emptyList(),
    allStores: List<Store>,
    allCategories: List<Category>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (Ingredient, List<Package>, List<BridgeConversion>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val ingredientId = remember { ingredient?.id ?: Uuid.random() }
    
    var name by remember { mutableStateOf(ingredient?.name ?: initialName) }
    
    // Find initial category name
    val initialCatName = allCategories.find { it.id == ingredient?.categoryId }?.name ?: ""
    var categoryName by remember { mutableStateOf(initialCatName) }
    
    var packages by remember { mutableStateOf(initialPackages) }
    var bridges by remember { mutableStateOf(initialBridges) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Purchase Options", "Conversions")

    MpEditDialogScaffold(
        title = if (ingredient == null) "Add Ingredient" else "Edit Ingredient",
        onDismiss = onDismiss,
        onSave = {
            val catId = allCategories.find { it.name == categoryName }?.id
                ?: Uuid.random() // Should exist validation
            val finalIngredient = Ingredient(
                id = ingredientId,
                name = name,
                categoryId = catId
            )
            onSave(finalIngredient, packages, bridges)
        },
        saveEnabled = name.isNotBlank() && categoryName.isNotBlank() && allCategories.any { it.name == categoryName },
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
                        options = allCategories.map { it.name },
                        selectedOption = categoryName,
                        onOptionSelected = { categoryName = it },
                        onAddOption = onAddCategory,
                        onDeleteOption = onDeleteCategory,
                        deleteWarningMessage = "Deleting this category will remove ALL ingredients in it."
                    )
                }
            }

            1 -> { // Purchase Options Tab
                PurchaseOptionsEditor(
                    ingredientId = ingredientId,
                    options = packages,
                    allStores = allStores,
                    allUnits = allUnits,
                    onUpdateOptions = { packages = it },
                    onAddStore = onAddStore,
                    onDeleteStore = onDeleteStore
                )
            }

            2 -> { // Conversions Tab
                ConversionBridgesEditor(
                    ingredientId = ingredientId,
                    currentBridges = bridges,
                    allUnits = allUnits,
                    onUpdate = { bridges = it }
                )
            }
        }
    }
}

@Composable
fun PurchaseOptionsEditor(
    ingredientId: Uuid,
    options: List<Package>,
    allStores: List<Store>,
    allUnits: List<UnitModel>,
    onUpdateOptions: (List<Package>) -> Unit,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var editingOption by remember { mutableStateOf<Package?>(null) }

    var storeName by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    
    // Default unit or first available
    var selectedUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }
    
    fun startEditing(opt: Package?) {
        if (opt != null) {
            val store = allStores.find { it.id == opt.storeId }
            val unit = allUnits.find { it.id == opt.unitId }
            storeName = store?.name ?: ""
            priceStr = (opt.priceCents / 100.0).toString()
            qtyStr = opt.quantity.toString()
            selectedUnitName = unit?.abbreviation ?: ""
            editingOption = opt
            isAdding = true
        } else {
            storeName = ""
            priceStr = ""
            qtyStr = ""
            selectedUnitName = allUnits.firstOrNull()?.abbreviation ?: ""
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
                    val uName = allUnits.find { it.id == opt.unitId }?.abbreviation ?: "?"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { startEditing(opt) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$sName: $${opt.priceCents / 100.0} / ${opt.quantity} $uName", style = MaterialTheme.typography.bodySmall)
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
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (editingOption == null) "New Option" else "Edit Option",
                        style = MaterialTheme.typography.labelMedium
                    )

                    SearchableDropdown(
                        label = "Store",
                        options = allStores.map { it.name },
                        selectedOption = storeName,
                        onOptionSelected = { storeName = it },
                        onAddOption = onAddStore,
                        onDeleteOption = onDeleteStore,
                        deleteWarningMessage = "This will remove this store and ALL associated prices from ALL ingredients."
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MpOutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Qty + Unit
                        Column(modifier = Modifier.weight(1.5f)) {
                            MpOutlinedTextField(
                                value = qtyStr,
                                onValueChange = { qtyStr = it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
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

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val store = allStores.find { it.name == storeName }
                                val unit = allUnits.find { it.abbreviation == selectedUnitName }
                                val price = priceStr.toDoubleOrNull()
                                val qty = qtyStr.toDoubleOrNull()

                                if (store != null && unit != null && price != null && qty != null) {
                                    val newOpt = Package(
                                        id = editingOption?.id ?: Uuid.random(),
                                        ingredientId = ingredientId,
                                        storeId = store.id,
                                        priceCents = (price * 100).toInt(),
                                        quantity = qty,
                                        unitId = unit.id
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
                            enabled = storeName.isNotBlank() && priceStr.isNotBlank() && qtyStr.isNotBlank() && selectedUnitName.isNotBlank()
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
    currentBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
    onUpdate: (List<BridgeConversion>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    
    var fromQtyStr by remember { mutableStateOf("1") }
    var fromUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }
    
    var toQtyStr by remember { mutableStateOf("") }
    var toUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }

    Column {
        if (currentBridges.isEmpty() && !isAdding) {
            Text("No conversions defined.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else if (!isAdding) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 150.dp)) {
                items(currentBridges) { bridge ->
                    val fName = allUnits.find { it.id == bridge.fromUnitId }?.abbreviation ?: "?"
                    val tName = allUnits.find { it.id == bridge.toUnitId }?.abbreviation ?: "?"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${bridge.fromQuantity} $fName = ${bridge.toQuantity} $tName", style = MaterialTheme.typography.bodySmall)
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
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "New Conversion (e.g. 1 Cup = 120 Grams)",
                        style = MaterialTheme.typography.labelMedium
                    )

                    // FROM
                    Row {
                        MpOutlinedTextField(
                            value = fromQtyStr,
                            onValueChange = { fromQtyStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            SearchableDropdown(
                                label = "Unit",
                                options = allUnits.map { it.abbreviation },
                                selectedOption = fromUnitName,
                                onOptionSelected = { fromUnitName = it },
                                onAddOption = {},
                                onDeleteOption = {},
                                deleteWarningMessage = ""
                            )
                        }
                    }

                    Text(
                        "EQUALS",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // TO
                    Row {
                        MpOutlinedTextField(
                            value = toQtyStr,
                            onValueChange = { toQtyStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            SearchableDropdown(
                                label = "Unit",
                                options = allUnits.map { it.abbreviation },
                                selectedOption = toUnitName,
                                onOptionSelected = { toUnitName = it },
                                onAddOption = {},
                                onDeleteOption = {},
                                deleteWarningMessage = ""
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val fq = fromQtyStr.toDoubleOrNull()
                                val tq = toQtyStr.toDoubleOrNull()
                                val fUnit = allUnits.find { it.abbreviation == fromUnitName }
                                val tUnit = allUnits.find { it.abbreviation == toUnitName }

                                if (fq != null && tq != null && fUnit != null && tUnit != null) {
                                    val newBridge = BridgeConversion(
                                        id = Uuid.random(),
                                        ingredientId = ingredientId,
                                        fromUnitId = fUnit.id,
                                        fromQuantity = fq,
                                        toUnitId = tUnit.id,
                                        toQuantity = tq
                                    )
                                    onUpdate(currentBridges + newBridge)
                                    isAdding = false
                                }
                            },
                            enabled = fromQtyStr.isNotBlank() && toQtyStr.isNotBlank() && fromUnitName.isNotBlank() && toUnitName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}