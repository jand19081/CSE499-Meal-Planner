@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.ingredients

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.uicomponents.CreateNewItemRow
import io.github.and19081.mealplanner.uicomponents.ExpandableListItem
import io.github.and19081.mealplanner.uicomponents.ListControlToolbar
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import kotlin.uuid.ExperimentalUuidApi
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.uuid.Uuid

@Composable
fun IngredientsView(viewModel: IngredientsViewModel, mode: Mode, isExpanded: Boolean) {
    val uiState by viewModel.uiState.collectAsState()

    // Use rememberSaveable
    var selectedIngredientId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    var creationName by rememberSaveable { mutableStateOf("") }

    val selectedIngredient = uiState.groupedIngredients.values.flatten().find { it.id.toString() == selectedIngredientId }

    val onIngredientClick: (Ingredient) -> Unit = {
        selectedIngredientId = it.id.toString()
        isAdding = false
    }

    val onAddClick: () -> Unit = {
        selectedIngredientId = null
        creationName = ""
        isAdding = true
    }

    val onDismissDetail: () -> Unit = {
        selectedIngredientId = null
        isAdding = false
    }

  var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var deleteConfirmationMessage by remember { mutableStateOf("") }

  if (pendingDeleteAction != null) {
    AlertDialog(
        onDismissRequest = { pendingDeleteAction = null },
        title = { Text("Confirm Deletion") },
        text = { Text(deleteConfirmationMessage) },
        confirmButton = {
          Button(
              onClick = {
                pendingDeleteAction?.invoke()
                pendingDeleteAction = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          ) {
            Text("Delete")
          }
        },
        dismissButton = { TextButton(onClick = { pendingDeleteAction = null }) { Text("Cancel") } },
    )
  }

  val onSave: (Ingredient, List<Package>, List<BridgeConversion>) -> Unit =
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
              initialName = creationName,
              allPackages = uiState.allPackages,
              allBridges = uiState.allBridges,
              allStores = uiState.allStores,
              allCategories = uiState.allCategories,
              allUnits = uiState.allUnits,
              onDismiss = onDismissDetail,
              onSave = onSave,
              onDelete = selectedIngredient?.let { ing -> { viewModel.deleteIngredient(ing.id) } },
              onAddStore = { viewModel.addStore(it) },
              onDeleteStore = { storeName ->
                val store = uiState.allStores.find { it.name == storeName }
                if (store != null) {
                  deleteConfirmationMessage =
                      "This will remove this store and ALL associated prices from ALL ingredients. Are you sure?"
                  pendingDeleteAction = { viewModel.deleteStore(store.id) }
                }
              },
              onAddCategory = { viewModel.addCategory(it) },
              onDeleteCategory = { catName ->
                val cat = uiState.allCategories.find { it.name == catName }
                if (cat != null) {
                  deleteConfirmationMessage =
                      "Deleting this category will remove ALL ingredients in it. Are you sure?"
                  pendingDeleteAction = { viewModel.deleteCategory(cat.id) }
                }
              },
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
          initialName = creationName,
          allPackages = uiState.allPackages,
          allBridges = uiState.allBridges,
          allStores = uiState.allStores,
          allCategories = uiState.allCategories,
          allUnits = uiState.allUnits,
          onDismiss = onDismissDetail,
          onSave = onSave,
          onDelete = selectedIngredient?.let { ing -> { viewModel.deleteIngredient(ing.id) } },
          onAddStore = { viewModel.addStore(it) },
          onDeleteStore = { storeName ->
            val store = uiState.allStores.find { it.name == storeName }
            if (store != null) {
              deleteConfirmationMessage =
                  "This will remove this store and ALL associated prices from ALL ingredients. Are you sure?"
              pendingDeleteAction = { viewModel.deleteStore(store.id) }
            }
          },
          onAddCategory = { viewModel.addCategory(it) },
          onDeleteCategory = { catName ->
            val cat = uiState.allCategories.find { it.name == catName }
            if (cat != null) {
              deleteConfirmationMessage =
                  "Deleting this category will remove ALL ingredients in it. Are you sure?"
              pendingDeleteAction = { viewModel.deleteCategory(cat.id) }
            }
          },
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
fun IngredientForm(
    ingredient: Ingredient?,
    initialName: String = "",
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allStores: List<Store>,
    allCategories: List<Category>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (Ingredient, List<Package>, List<BridgeConversion>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddStore: (String) -> Unit,
    onDeleteStore: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
) {
  val ingredientId = remember(ingredient) { ingredient?.id ?: Uuid.random() }

  var name by remember(ingredient) { mutableStateOf(ingredient?.name ?: initialName) }

  val initialCatName = allCategories.find { it.id == ingredient?.categoryId }?.name ?: ""
  var categoryName by remember(ingredient) { mutableStateOf(initialCatName) }

  var preferredUnitId by remember(ingredient) { mutableStateOf(ingredient?.preferredUnitId) }

  var packages by
      remember(ingredient) {
        mutableStateOf(
            if (ingredient != null) allPackages.filter { it.ingredientId == ingredient.id }
            else emptyList()
        )
      }
  var bridges by
      remember(ingredient) {
        mutableStateOf(
            if (ingredient != null) allBridges.filter { it.ingredientId == ingredient.id }
            else emptyList()
        )
      }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("General", "Purchase Options", "Conversions")

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = if (ingredient == null) "Add Ingredient" else "Edit Ingredient",
      onClose = onDismiss,
      onSave = {
        val catId = allCategories.find { it.name == categoryName }?.id ?: Uuid.random()
        val finalIngredient =
            Ingredient(
                id = ingredientId,
                name = name,
                categoryId = catId,
                preferredUnitId = preferredUnitId,
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
      0 -> { // General Tab
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
              onDeleteOption = onDeleteCategory,
              deleteWarningMessage = "Deleting this category will remove ALL ingredients in it.",
          )

          val unitOptions = listOf("None") + allUnits.map { it.abbreviation }
          val selectedUnitAbbreviation =
              allUnits.find { it.id == preferredUnitId }?.abbreviation ?: "None"

          SearchableDropdown(
              label = "Preferred Base Unit (Pantry/Shopping List Display)",
              options = unitOptions,
              selectedOption = selectedUnitAbbreviation,
              onOptionSelected = {
                preferredUnitId = if (it == "None") null else allUnits.find { u -> u.abbreviation == it }?.id
              },
              onAddOption = {},
              onDeleteOption = {},
              deleteWarningMessage = "",
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
            onDeleteStore = onDeleteStore,
        )
      }

      2 -> { // Conversions Tab
        ConversionBridgesEditor(
            ingredientId = ingredientId,
            currentBridges = bridges,
            allUnits = allUnits,
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
    onIngredientClick: (Ingredient) -> Unit,
    onAddClick: () -> Unit,
) {
  Scaffold(
      topBar = {
        ListControlToolbar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            searchPlaceholder = "Search Ingredients...",
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
      // Create New Option
      if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
        item {
          CreateNewItemRow(searchQuery = uiState.searchQuery, onClick = onAddClick)
          HorizontalDivider()
        }
      }

      uiState.groupedIngredients.forEach { (header, ingredients) ->
        stickyHeader { ListSectionHeader(text = header) }

        items(ingredients) { ingredient ->
          IngredientRow(
              ingredient = ingredient,
              categoryName = header,
              packages = uiState.allPackages.filter { it.ingredientId == ingredient.id },
              allStores = uiState.allStores,
              allUnits = uiState.allUnits,
              allBridges = uiState.allBridges,
              onEditClick = { onIngredientClick(ingredient) },
          )
          HorizontalDivider()
        }
      }
    }
  }
}

@Composable
fun EmptyDetailPlaceholder() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
          Icons.Default.ShoppingCart,
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
fun IngredientRow(
    ingredient: Ingredient,
    categoryName: String,
    packages: List<Package>,
    allStores: List<Store>,
    allUnits: List<UnitModel>,
    allBridges: List<BridgeConversion>,
    onEditClick: () -> Unit,
) {
  // Calculate Best Price
  val bestOption =
      packages.minByOrNull {
        if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE
      }

  val preferredUnit = ingredient.preferredUnitId?.let { id -> allUnits.find { it.id == id } }

  val bestPriceString =
      if (bestOption != null) {
        val store = allStores.find { it.id == bestOption.storeId }?.name ?: "Unknown"
        val price = bestOption.priceCents / 100.0

        if (preferredUnit != null) {
          val qtyInPreferred =
              UnitConverter.convert(
                  bestOption.quantity,
                  bestOption.unitId,
                  preferredUnit.id,
                  allUnits.associateBy { it.id },
                  allBridges,
              ) ?: 0.0
          if (qtyInPreferred > 0) {
            val pricePerPreferred = price / qtyInPreferred
            val rounded = String.format("%.2f", pricePerPreferred)
            "Best: $store - $$rounded/${preferredUnit.abbreviation}"
          } else {
            // Fallback to option unit if no conversion possible
            val perUnit = if (bestOption.quantity > 0) price / bestOption.quantity else 0.0
            val rounded = String.format("%.2f", perUnit)
            val unitName = allUnits.find { it.id == bestOption.unitId }?.abbreviation ?: "?"
            "Best: $store - $$rounded/$unitName"
          }
        } else {
          val perUnit = if (bestOption.quantity > 0) price / bestOption.quantity else 0.0
          val rounded = String.format("%.2f", perUnit)
          val unitName = allUnits.find { it.id == bestOption.unitId }?.abbreviation ?: "?"
          "Best: $store - $$rounded/$unitName"
        }
      } else {
        "No prices"
      }

  val subtitle = "$categoryName • ${packages.size} options • $bestPriceString"

  ExpandableListItem(title = ingredient.name, subtitle = subtitle, onEditClick = onEditClick) {
    Text("Category: $categoryName", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    if (packages.isEmpty()) {
      Text(
          "No purchase options listed.",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
      )
    } else {
      Text("Purchase Options:", style = MaterialTheme.typography.labelMedium)
      packages.forEach { opt ->
        val storeName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown Store"
        val unitName = allUnits.find { it.id == opt.unitId }?.abbreviation ?: "?"
        Text(
            "• $storeName: $${opt.priceCents / 100.0} for ${opt.quantity} $unitName",
            style = MaterialTheme.typography.bodySmall,
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
    onDeleteStore: (String) -> Unit,
) {
  var isAdding by remember { mutableStateOf(false) }
  var editingOption by remember { mutableStateOf<Package?>(null) }

  var storeName by remember { mutableStateOf("") }
  var priceStr by remember { mutableStateOf("") }
  var qtyStr by remember { mutableStateOf("") }

  // Default unit or first available
  var selectedUnitName by remember { mutableStateOf("") }

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
      selectedUnitName = ""
      editingOption = null
      isAdding = true
    }
  }

  Column {
    if (options.isEmpty() && !isAdding) {
      Text(
          "No options added yet.",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
      )
    } else if (!isAdding) {
      Column {
        options.forEach { opt ->
          val sName = allStores.find { it.id == opt.storeId }?.name ?: "Unknown"
          val uName = allUnits.find { it.id == opt.unitId }?.abbreviation ?: "?"
          Row(
              modifier =
                  Modifier.fillMaxWidth().clickable { startEditing(opt) }.padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                "$sName: $${opt.priceCents / 100.0} / ${opt.quantity} $uName",
                style = MaterialTheme.typography.bodySmall,
            )
            IconButton(
                onClick = { onUpdateOptions(options - opt) },
                modifier = Modifier.size(24.dp),
            ) {
              Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
          }
          HorizontalDivider()
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!isAdding) {
      Button(onClick = { startEditing(null) }, modifier = Modifier.fillMaxWidth()) {
        Text("Add Purchase Option")
      }
    } else {
      Card(
          colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
              if (editingOption == null) "New Option" else "Edit Option",
              style = MaterialTheme.typography.labelMedium,
          )

          SearchableDropdown(
              label = "Store",
              options = allStores.map { it.name },
              selectedOption = storeName,
              onOptionSelected = { storeName = it },
              onAddOption = onAddStore,
              onDeleteOption = onDeleteStore,
              deleteWarningMessage =
                  "This will remove this store and ALL associated prices from ALL ingredients.",
          )

          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            MpOutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Price ($)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // Qty + Unit
            Column(modifier = Modifier.weight(1.5f)) {
              MpOutlinedTextField(
                  value = qtyStr,
                  onValueChange = { qtyStr = it },
                  label = { Text("Qty") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.fillMaxWidth(),
              )
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

          Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { isAdding = false }) { Text("Cancel") }
            Button(
                onClick = {
                  val store = allStores.find { it.name == storeName }
                  val unit = allUnits.find { it.abbreviation == selectedUnitName }
                  val price = priceStr.toDoubleOrNull()
                  val qty = qtyStr.toDoubleOrNull()

                  if (store != null && unit != null && price != null && qty != null) {
                    val newOpt =
                        Package(
                            id = editingOption?.id ?: Uuid.random(),
                            ingredientId = ingredientId,
                            storeId = store.id,
                            priceCents = (price * 100).toInt(),
                            quantity = qty,
                            unitId = unit.id,
                        )
                    val newList =
                        editingOption?.let { existing ->
                          options.map { if (it.id == existing.id) newOpt else it }
                        } ?: (options + newOpt)
                    onUpdateOptions(newList)
                    isAdding = false
                  }
                },
                enabled =
                    storeName.isNotBlank() &&
                        priceStr.isNotBlank() &&
                        qtyStr.isNotBlank() &&
                        selectedUnitName.isNotBlank(),
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
    onUpdate: (List<BridgeConversion>) -> Unit,
) {
  var isAdding by remember { mutableStateOf(false) }
  var editingBridge by remember { mutableStateOf<BridgeConversion?>(null) }

  var fromQtyStr by remember { mutableStateOf("1") }
  var fromUnitName by remember { mutableStateOf("") }

  var toQtyStr by remember { mutableStateOf("") }
  var toUnitName by remember { mutableStateOf("") }

  fun startEditing(bridge: BridgeConversion?) {
    if (bridge != null) {
      fromQtyStr = bridge.fromQuantity.toString()
      fromUnitName = allUnits.find { it.id == bridge.fromUnitId }?.abbreviation ?: ""
      toQtyStr = bridge.toQuantity.toString()
      toUnitName = allUnits.find { it.id == bridge.toUnitId }?.abbreviation ?: ""
      editingBridge = bridge
      isAdding = true
    } else {
      fromQtyStr = "1"
      fromUnitName = ""
      toQtyStr = ""
      toUnitName = ""
      editingBridge = null
      isAdding = true
    }
  }

  Column {
    if (currentBridges.isEmpty() && !isAdding) {
      Text(
          "No conversions defined.",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
      )
    } else if (!isAdding) {
      Column {
        currentBridges.forEach { bridge ->
          val fName = allUnits.find { it.id == bridge.fromUnitId }?.abbreviation ?: "?"
          val tName = allUnits.find { it.id == bridge.toUnitId }?.abbreviation ?: "?"
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clickable { startEditing(bridge) }
                      .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                "${bridge.fromQuantity} $fName = ${bridge.toQuantity} $tName",
                style = MaterialTheme.typography.bodySmall,
            )
            IconButton(
                onClick = { onUpdate(currentBridges - bridge) },
                modifier = Modifier.size(24.dp),
            ) {
              Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
          }
          HorizontalDivider()
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!isAdding) {
      Button(onClick = { startEditing(null) }, modifier = Modifier.fillMaxWidth()) {
        Text("Add Conversion")
      }
    } else {
      Card(
          colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
              if (editingBridge == null) "New Conversion" else "Edit Conversion",
              style = MaterialTheme.typography.labelMedium,
          )

          // FROM
          Row {
            MpOutlinedTextField(
                value = fromQtyStr,
                onValueChange = { fromQtyStr = it },
                label = { Text("Qty") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                  deleteWarningMessage = "",
              )
            }
          }

          Text(
              "EQUALS",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.align(Alignment.CenterHorizontally),
          )

          // TO
          Row {
            MpOutlinedTextField(
                value = toQtyStr,
                onValueChange = { toQtyStr = it },
                label = { Text("Qty") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                  deleteWarningMessage = "",
              )
            }
          }

          Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { isAdding = false }) { Text("Cancel") }
            Button(
                onClick = {
                  val fq = fromQtyStr.toDoubleOrNull()
                  val tq = toQtyStr.toDoubleOrNull()
                  val fUnit = allUnits.find { it.abbreviation == fromUnitName }
                  val tUnit = allUnits.find { it.abbreviation == toUnitName }

                  if (fq != null && tq != null && fUnit != null && tUnit != null) {
                    val newBridge =
                        BridgeConversion(
                            id = editingBridge?.id ?: Uuid.random(),
                            ingredientId = ingredientId,
                            fromUnitId = fUnit.id,
                            fromQuantity = fq,
                            toUnitId = tUnit.id,
                            toQuantity = tq,
                        )
                    val newList =
                        editingBridge?.let { existing ->
                          currentBridges.map { if (it.id == existing.id) newBridge else it }
                        } ?: (currentBridges + newBridge)
                    onUpdate(newList)
                    isAdding = false
                  }
                },
                enabled =
                    fromQtyStr.isNotBlank() &&
                        toQtyStr.isNotBlank() &&
                        fromUnitName.isNotBlank() &&
                        toUnitName.isNotBlank(),
            ) {
              Text("Save")
            }
          }
        }
      }
    }
  }
}
