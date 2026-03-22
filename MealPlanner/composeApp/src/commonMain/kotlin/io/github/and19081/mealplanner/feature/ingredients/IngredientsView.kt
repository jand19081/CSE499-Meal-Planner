@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.feature.ingredients

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.PurchasableInfo
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.feature.recipes.EmptyDetailPlaceholder
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.ui.components.CreateNewItemRow
import io.github.and19081.mealplanner.ui.components.EmptyListMessage
import io.github.and19081.mealplanner.ui.components.ExpandableListItem
import io.github.and19081.mealplanner.ui.components.ListControlToolbar
import io.github.and19081.mealplanner.ui.components.ListSectionHeader
import io.github.and19081.mealplanner.ui.components.MpDetailScaffold
import io.github.and19081.mealplanner.ui.components.MpOutlinedTextField
import io.github.and19081.mealplanner.ui.components.SearchableDropdown
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun IngredientsView(viewModel: IngredientsViewModel, mode: Mode, isExpanded: Boolean) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedIngredientId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    var creationName by rememberSaveable { mutableStateOf("") }

    val selectedIngredient = uiState.groupedIngredients.values.flatten().find { it.id.toString() == selectedIngredientId }

  val onIngredientClick: (FoodItem) -> Unit = {
    selectedIngredientId = it.id.toString()
    isAdding = false
  }

  val onAddClick: () -> Unit = {
    selectedIngredientId = null
    isAdding = true
  }

  val onDismissDetail: () -> Unit = {
    selectedIngredientId = null
    isAdding = false
  }

  val onSave: (FoodItem, List<Package>, List<BridgeConversion>) -> Unit =
      { updatedIngredient, newPackages, newBridges ->
        viewModel.saveIngredient(updatedIngredient, newPackages, newBridges)
        onDismissDetail()
      }

  val actualIsExpanded =
      when (mode) {
        Mode.AUTO -> isExpanded
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
      }

  if (actualIsExpanded) {
    Row(modifier = Modifier.fillMaxSize()) {
      // List Pane
      Box(modifier = Modifier.weight(0.4f)) {
        IngredientListPane(
            uiState = uiState,
            viewModel = viewModel,
            onIngredientClick = onIngredientClick,
            onAddClick = onAddClick,
        )
      }

      VerticalDivider(modifier = Modifier.width(1.dp))

      // Detail Pane
      Box(modifier = Modifier.weight(0.6f)) {
        if (selectedIngredient != null || isAdding) {
          IngredientForm(
              ingredient = selectedIngredient,
              initialName = if (isAdding) uiState.searchQuery else "",
              allPackages = uiState.allPackages,
              allBridges = uiState.allBridges,
              allStores = uiState.allStores,
              allCategories = uiState.allCategories,
              allUnits = uiState.allUnits,
              onDismiss = onDismissDetail,
              onSave = onSave,
              onDelete =
                  selectedIngredient?.let { ing ->
                    {
                      viewModel.deleteIngredient(ing.id)
                      onDismissDetail()
                    }
                  },
              onAddStore = { viewModel.addStore(it) },
              onDeleteStore = { viewModel.deleteStore(it) },
              onAddCategory = { viewModel.addCategory(it) },
              onDeleteCategory = { categoryId -> viewModel.deleteCategory(categoryId) },
          )
        } else {
            EmptyDetailPlaceholder()
        }
      }
    }
  } else {
    // Mobile View
    if (selectedIngredient != null || isAdding) {
      IngredientForm(
          ingredient = selectedIngredient,
          initialName = if (isAdding) uiState.searchQuery else "",
          allPackages = uiState.allPackages,
          allBridges = uiState.allBridges,
          allStores = uiState.allStores,
          allCategories = uiState.allCategories,
          allUnits = uiState.allUnits,
          onDismiss = onDismissDetail,
          onSave = onSave,
          onDelete =
              selectedIngredient?.let { ing ->
                {
                  viewModel.deleteIngredient(ing.id)
                  onDismissDetail()
                }
              },
          onAddStore = { viewModel.addStore(it) },
          onDeleteStore = { viewModel.deleteStore(it) },
          onAddCategory = { viewModel.addCategory(it) },
          onDeleteCategory = { categoryId -> viewModel.deleteCategory(categoryId) },
      )
    } else {
      IngredientListPane(
          uiState = uiState,
          viewModel = viewModel,
          onIngredientClick = onIngredientClick,
          onAddClick = onAddClick,
      )
    }
  }
}

@Composable
fun EmptyDetailPlaceholder() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
          Icons.Default.FilterList,
          null,
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.outline,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text("Select an ingredient to view details", color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
fun IngredientForm(
    ingredient: FoodItem?,
    initialName: String = "",
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allStores: List<Store>,
    allCategories: List<Category>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (FoodItem, List<Package>, List<BridgeConversion>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddStore: (String) -> Unit,
    onDeleteStore: (Uuid) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Uuid) -> Unit,
) {
  val ingredientId = remember(ingredient) { ingredient?.id ?: Uuid.random() }

  var name by remember(ingredient) { mutableStateOf(ingredient?.name ?: initialName) }

  val initialCatName = allCategories.find { it.id == ingredient?.purchasableInfo?.categoryId }?.name ?: ""
  var categoryName by remember(ingredient) { mutableStateOf(initialCatName) }

  var preferredUnitId by remember(ingredient) { mutableStateOf(ingredient?.preferredUnitId) }

  var packages by
      remember(ingredient) {
        mutableStateOf(
            if (ingredient != null) allPackages.filter { it.foodItemId == ingredient.id }
            else emptyList()
        )
      }
  var bridges by
      remember(ingredient) {
        mutableStateOf(
            if (ingredient != null) allBridges.filter { it.foodItemId == ingredient.id }
            else emptyList()
        )
      }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("General", "Purchase Options", "Conversions")

    MpDetailScaffold(
        title = if (ingredient == null) "Add FoodItem" else "Edit FoodItem",
        onClose = onDismiss,
        onSave = {
            val catId = allCategories.find { it.name == categoryName }?.id ?: Uuid.random()
            val finalIngredient =
                FoodItem(
                    id = ingredientId,
                    name = name,
                    preferredUnitId = preferredUnitId,
                    purchasableInfo = PurchasableInfo(
                        expectedPriceCents = ingredient?.purchasableInfo?.expectedPriceCents,
                        categoryId = catId
                    )
                )
            onSave(finalIngredient, packages, bridges)
        },
        saveEnabled =
            name.isNotBlank() &&
                    categoryName.isNotBlank() &&
                    allCategories.any { it.name == categoryName },
        onDelete =
            if (onDelete != null) {
                {
                    onDelete()
                    onDismiss()
                }
            } else null,
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
    ) {
        when (selectedTabIndex) {
            0 -> { // General
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MpOutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SearchableDropdown(
                        label = "Category",
                        options = allCategories.map { it.name },
                        selectedOption = categoryName,
                        onOptionSelected = { categoryName = it },
                        onAddOption = onAddCategory,
                        onDeleteOption = { name ->
                            allCategories.find { it.name == name }?.let { onDeleteCategory(it.id) }
                        },
                        deleteWarningMessage =
                            "Deleting this category will remove ALL ingredients in it. Are you sure?",
                    )

                    Box {
                        var unitExpanded by remember { mutableStateOf(false) }
                        val unitName =
                            allUnits.find { it.id == preferredUnitId }?.displayName ?: "None"
                        OutlinedButton(
                            onClick = { unitExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Preferred Unit: $unitName")
                        }
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    preferredUnitId = null
                                    unitExpanded = false
                                },
                            )
                            allUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.displayName) },
                                    onClick = {
                                        preferredUnitId = unit.id
                                        unitExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            1 -> { // Purchase Options
                PackageOptionEditor(
                    currentPackages = packages,
                    allStores = allStores,
                    allUnits = allUnits,
                    foodItemId = ingredientId,
                    onUpdate = { packages = it },
                    onAddStore = onAddStore,
                    onDeleteStore = onDeleteStore,
                )
            }

            2 -> { // Conversions
                ConversionEditor(
                    currentBridges = bridges,
                    allUnits = allUnits,
                    foodItemId = ingredientId,
                    onUpdate = { bridges = it },
                )
            }
        }
    }
}

@Composable
fun IngredientListPane(
    uiState: IngredientsUiState,
    viewModel: IngredientsViewModel,
    onIngredientClick: (FoodItem) -> Unit,
    onAddClick: () -> Unit,
) {
  Scaffold(
      topBar = {
          ListControlToolbar(
              searchQuery = uiState.searchQuery,
              onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
              searchPlaceholder = "Search ingredients...",
              isSortByPrimary = uiState.isSortByCategory,
              onToggleSort = { viewModel.toggleSortMode() },
              onAddClick = onAddClick,
          )
      }
  ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
      uiState.groupedIngredients.forEach { (category, ingredients) ->
        stickyHeader { ListSectionHeader(category) }
        items(ingredients) { ingredient ->
          IngredientRow(
              ingredient = ingredient,
              categoryName = category,
              packages = uiState.allPackages.filter { it.foodItemId == ingredient.id },
              allStores = uiState.allStores,
              allUnits = uiState.allUnits,
              allBridges = uiState.allBridges,
              onEditClick = { onIngredientClick(ingredient) },
          )
          HorizontalDivider()
        }
      }

      if (uiState.groupedIngredients.isEmpty()) {
        item {
            EmptyListMessage(
                message =
                    if (uiState.searchQuery.isBlank())
                        "No ingredients found. Add your first ingredient!"
                    else "No ingredients match your search.",
                modifier = Modifier.padding(32.dp),
            )
        }
      }

      if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
        item { CreateNewItemRow(searchQuery = uiState.searchQuery, onClick = onAddClick) }
      }
    }
  }
}

@Composable
fun IngredientRow(
    ingredient: FoodItem,
    categoryName: String,
    packages: List<Package>,
    allStores: List<Store>,
    allUnits: List<UnitModel>,
    allBridges: List<BridgeConversion>,
    onEditClick: () -> Unit,
) {
  val priceRange =
      if (packages.isEmpty()) "No price data"
      else {
        val min = packages.minOf { it.priceCents.toDouble() / it.quantity }
        val max = packages.maxOf { it.priceCents.toDouble() / it.quantity }
        if (min == max) "$${String.format("%.2f", min / 100.0)} / unit"
        else
            "$${String.format("%.2f", min / 100.0)} - $${String.format("%.2f", max / 100.0)} / unit"
      }

    ExpandableListItem(
        title = ingredient.name,
        subtitle = "$categoryName • $priceRange",
        onEditClick = onEditClick,
    ) {
        if (packages.isNotEmpty()) {
            Text("Best prices:", style = MaterialTheme.typography.labelSmall)
            packages
                .sortedBy { it.priceCents.toDouble() / it.quantity }
                .take(3)
                .forEach { pkg ->
                    val store = allStores.find { it.id == pkg.storeId }?.name ?: "Unknown Store"
                    val unit = allUnits.find { it.id == pkg.unitId }?.abbreviation ?: ""
                    Text(
                        "• $store: $${
                            String.format(
                                "%.2f",
                                pkg.priceCents / 100.0
                            )
                        } for ${pkg.quantity} $unit",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
        }

        if (allBridges.any { it.foodItemId == ingredient.id }) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Conversions:", style = MaterialTheme.typography.labelSmall)
            allBridges
                .filter { it.foodItemId == ingredient.id }
                .forEach { bridge ->
                    val fromUnit = allUnits.find { it.id == bridge.fromUnitId }?.abbreviation ?: ""
                    val toUnit = allUnits.find { it.id == bridge.toUnitId }?.abbreviation ?: ""
                    Text(
                        "• ${bridge.fromQuantity} $fromUnit = ${bridge.toQuantity} $toUnit",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
        }
    }
}

@Composable
fun PackageOptionEditor(
    currentPackages: List<Package>,
    allStores: List<Store>,
    allUnits: List<UnitModel>,
    foodItemId: Uuid,
    onUpdate: (List<Package>) -> Unit,
    onAddStore: (String) -> Unit,
    onDeleteStore: (Uuid) -> Unit,
) {
  var isAdding by remember { mutableStateOf(false) }
  var selectedStoreName by remember { mutableStateOf("") }
  var priceStr by remember { mutableStateOf("") }
  var quantityStr by remember { mutableStateOf("") }
  var selectedUnitAbbr by remember { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    currentPackages.forEach { pkg ->
      val storeName = allStores.find { it.id == pkg.storeId }?.name ?: "Unknown"
      val unitAbbr = allUnits.find { it.id == pkg.unitId }?.abbreviation ?: ""
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(storeName, style = MaterialTheme.typography.bodyMedium)
          Text(
              "$${String.format("%.2f", pkg.priceCents / 100.0)} for ${pkg.quantity} $unitAbbr",
              style = MaterialTheme.typography.bodySmall,
          )
        }
        IconButton(onClick = { onUpdate(currentPackages - pkg) }) {
          Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
        }
      }
      HorizontalDivider()
    }

    if (!isAdding) {
      Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Add Package Option")
      }
    } else {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SearchableDropdown(
              label = "Store",
              options = allStores.map { it.name },
              selectedOption = selectedStoreName,
              onOptionSelected = { selectedStoreName = it },
              onAddOption = onAddStore,
              onDeleteOption = { name ->
                  allStores.find { it.name == name }?.let { onDeleteStore(it.id) }
              },
              deleteWarningMessage = "Delete this store? This will affect other items too.",
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpOutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Price ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
              MpOutlinedTextField(
                  value = quantityStr,
                  onValueChange = { quantityStr = it },
                  label = { Text("Quantity") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.weight(1f),
              )
          }
            SearchableDropdown(
                label = "Unit",
                options = allUnits.map { it.abbreviation },
                selectedOption = selectedUnitAbbr,
                onOptionSelected = { selectedUnitAbbr = it },
                onAddOption = {},
                onDeleteOption = {},
                deleteWarningMessage = "",
            )
          Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { isAdding = false }) { Text("Cancel") }
            Button(
                onClick = {
                  val store = allStores.find { it.name == selectedStoreName }
                  val unit = allUnits.find { it.abbreviation == selectedUnitAbbr }
                  val price = (priceStr.toDoubleOrNull()?.let { it * 100 })?.toInt()
                  val qty = quantityStr.toDoubleOrNull()
                  if (store != null && unit != null && price != null && qty != null) {
                    onUpdate(
                        currentPackages +
                                Package(
                                    foodItemId = foodItemId,
                                    storeId = store.id,
                                    priceCents = price,
                                    quantity = qty,
                                    unitId = unit.id,
                                )
                    )
                    isAdding = false
                    selectedStoreName = ""
                    priceStr = ""
                    quantityStr = ""
                    selectedUnitAbbr = ""
                  }
                },
                enabled =
                    selectedStoreName.isNotBlank() &&
                        priceStr.isNotBlank() &&
                        quantityStr.isNotBlank() &&
                        selectedUnitAbbr.isNotBlank(),
            ) {
              Text("Add")
            }
          }
        }
      }
    }
  }
}

@Composable
fun ConversionEditor(
    currentBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
    foodItemId: Uuid,
    onUpdate: (List<BridgeConversion>) -> Unit,
) {
  var isAdding by remember { mutableStateOf(false) }
  var fromQtyStr by remember { mutableStateOf("") }
  var toQtyStr by remember { mutableStateOf("") }
  var fromUnitName by remember { mutableStateOf("") }
  var toUnitName by remember { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    currentBridges.forEach { bridge ->
      val fromU = allUnits.find { it.id == bridge.fromUnitId }?.abbreviation ?: ""
      val toU = allUnits.find { it.id == bridge.toUnitId }?.abbreviation ?: ""
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${bridge.fromQuantity} $fromU = ${bridge.toQuantity} $toU",
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onUpdate(currentBridges - bridge) }) {
          Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
        }
      }
      HorizontalDivider()
    }

    if (!isAdding) {
      Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Add Conversion Bridge")
      }
    } else {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpOutlinedTextField(
                value = fromQtyStr,
                onValueChange = { fromQtyStr = it },
                label = { Text("From Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
                SearchableDropdown(
                    label = "From Unit",
                    options = allUnits.map { it.abbreviation },
                    selectedOption = fromUnitName,
                    onOptionSelected = { fromUnitName = it },
                    onAddOption = {}, onDeleteOption = {}, deleteWarningMessage = ""
                )
            }
          }
          Icon(Icons.Default.SyncAlt, null, modifier = Modifier.align(Alignment.CenterHorizontally))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpOutlinedTextField(
                value = toQtyStr,
                onValueChange = { toQtyStr = it },
                label = { Text("To Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
                SearchableDropdown(
                    label = "To Unit",
                    options = allUnits.map { it.abbreviation },
                    selectedOption = toUnitName,
                    onOptionSelected = { toUnitName = it },
                    onAddOption = {}, onDeleteOption = {}, deleteWarningMessage = ""
                )
            }
          }
          Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { isAdding = false }) { Text("Cancel") }
            Button(
                onClick = {
                  val fromU = allUnits.find { it.abbreviation == fromUnitName }
                  val toU = allUnits.find { it.abbreviation == toUnitName }
                  val fQty = fromQtyStr.toDoubleOrNull()
                  val tQty = toQtyStr.toDoubleOrNull()
                  if (fromU != null && toU != null && fQty != null && tQty != null) {
                    onUpdate(
                        currentBridges +
                                BridgeConversion(
                                    foodItemId = foodItemId,
                                    fromUnitId = fromU.id,
                                    toUnitId = toU.id,
                                    fromQuantity = fQty,
                                    toQuantity = tQty,
                                )
                    )
                    isAdding = false
                    fromQtyStr = ""
                    toQtyStr = ""
                    fromUnitName = ""
                    toUnitName = ""
                  }
                },
                enabled =
                    fromQtyStr.isNotBlank() &&
                        toQtyStr.isNotBlank() &&
                        fromUnitName.isNotBlank() &&
                        toUnitName.isNotBlank(),
            ) {
              Text("Add")
            }
          }
        }
      }
    }
  }
}
