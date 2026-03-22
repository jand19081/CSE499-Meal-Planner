@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalUuidApi::class,
)

package io.github.and19081.mealplanner.feature.meals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.logic.PriceCalculator
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.RecipeInfo
import io.github.and19081.mealplanner.feature.recipes.PrepareBatchDialog
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.ui.components.CreateNewItemRow
import io.github.and19081.mealplanner.ui.components.ExpandableListItem
import io.github.and19081.mealplanner.ui.components.ListControlToolbar
import io.github.and19081.mealplanner.ui.components.ListSectionHeader
import io.github.and19081.mealplanner.ui.components.MpDetailScaffold
import io.github.and19081.mealplanner.ui.components.MpOutlinedTextField
import io.github.and19081.mealplanner.ui.components.MpValidationWarning
import io.github.and19081.mealplanner.ui.components.SearchableDropdown
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun MealsView(
    viewModel: MealsViewModel,
    mode: Mode,
    isExpanded: Boolean,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddRecipe: (String, (FoodItem) -> Unit) -> Unit,
    onMakeMeal: (FoodItem, Double, Uuid?, Double?, Uuid?) -> Unit = { _, _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    val allItems = uiState.allItems
    val allRecipes = allItems.filter { it.isRecipe }
    val allIngredients = allItems.filter { it.isIngredient }

    var selectedMealId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    val selectedMeal = uiState.groupedMeals.values.flatten().find { it.id.toString() == selectedMealId }

    var mealToMake by remember { mutableStateOf<FoodItem?>(null) }

    val onMealClick: (FoodItem) -> Unit = {
        selectedMealId = it.id.toString()
        viewModel.initializeDraft(it)
        isAdding = false
    }

    val onAddClick: () -> Unit = {
        selectedMealId = null
        viewModel.initializeDraft(null)
        viewModel.updateDraftName(uiState.searchQuery)
        isAdding = true
    }

    val onDismissDetail: () -> Unit = {
        selectedMealId = null
        isAdding = false
    }

    val actualIsExpanded =
        when (mode) {
            Mode.AUTO -> isExpanded
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

    if (mealToMake != null) {
        PrepareBatchDialog(
            itemName = mealToMake!!.name,
            allItems = allItems,
            allUnits = uiState.allUnits,
            onDismiss = { mealToMake = null },
            onConfirm = { multiplier, yieldIngId, yieldQty, yieldUnitId ->
                onMakeMeal(mealToMake!!, multiplier, yieldIngId, yieldQty, yieldUnitId)
                mealToMake = null
            }
        )
    }

  if (actualIsExpanded) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(0.4f)) {
        MealListPane(
            uiState = uiState,
            allRecipes = allRecipes,
            allIngredients = allIngredients,
            viewModel = viewModel,
            onMealClick = onMealClick,
            onAddClick = onAddClick,
            onMakeMeal = { mealToMake = it },
        )
      }

      VerticalDivider(modifier = Modifier.width(1.dp))

      Box(modifier = Modifier.weight(0.6f)) {
        if (selectedMeal != null || isAdding) {
          MealForm(
              viewModel = viewModel,
              meal = selectedMeal,
              allItems = allItems,
              allRecipes = allRecipes,
              allIngredients = allIngredients,
              allUnits = uiState.allUnits,
              warnings = selectedMeal?.let { uiState.mealWarnings[it.id] } ?: emptyList(),
              onDismiss = onDismissDetail,
              onSave = {
                viewModel.saveDraft()
                onDismissDetail()
              },
              onDelete = selectedMeal?.let { m -> { viewModel.deleteMeal(m) } },
              onAddIngredient = onAddIngredient,
              onAddRecipe = onAddRecipe,
          )
        } else {
          EmptyDetailPlaceholder()
        }
      }
    }
  } else {
    if (selectedMeal != null || isAdding) {
      MealForm(
          viewModel = viewModel,
          meal = selectedMeal,
          allItems = allItems,
          allRecipes = allRecipes,
          allIngredients = allIngredients,
          allUnits = uiState.allUnits,
          warnings = selectedMeal?.let { uiState.mealWarnings[it.id] } ?: emptyList(),
          onDismiss = onDismissDetail,
          onSave = {
            viewModel.saveDraft()
            onDismissDetail()
          },
          onDelete = selectedMeal?.let { m -> { viewModel.deleteMeal(m) } },
          onAddIngredient = onAddIngredient,
          onAddRecipe = onAddRecipe,
      )
    } else {
      MealListPane(
          uiState = uiState,
          allRecipes = allRecipes,
          allIngredients = allIngredients,
          viewModel = viewModel,
          onMealClick = onMealClick,
          onAddClick = onAddClick,
          onMakeMeal = { mealToMake = it },
      )
    }
  }
}

@Composable
fun MealForm(
    viewModel: MealsViewModel,
    meal: FoodItem?,
    allItems: List<FoodItem>,
    allRecipes: List<FoodItem>,
    allIngredients: List<FoodItem>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddRecipe: (String, (FoodItem) -> Unit) -> Unit,
) {
  val name by viewModel.draftName.collectAsState()
  val mealType by viewModel.draftMealType.collectAsState()
  val requirements by viewModel.draftRequirements.collectAsState()

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("General", "Contents")

    MpDetailScaffold(
        title = if (meal == null) "New Meal" else "Edit Meal",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = name.isNotBlank(),
        onDelete = onDelete,
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
    ) {
        MpValidationWarning(warnings = warnings)

        when (selectedTabIndex) {
            0 -> { // General
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MpOutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.updateDraftName(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var expandedMealType by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedMealType = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Meal Type: ${mealType.name}")
                        }
                        DropdownMenu(
                            expanded = expandedMealType,
                            onDismissRequest = { expandedMealType = false },
                        ) {
                            RecipeMealType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        viewModel.updateDraftMealType(type)
                                        expandedMealType = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            1 -> { // Contents
                MealContentsEditor(
                    currentRequirements = requirements,
                    allRecipes = allRecipes,
                    allIngredients = allIngredients,
                    allUnits = allUnits,
                    onUpdateRequirements = { viewModel.updateDraftRequirements(it) },
                    onAddIngredient = onAddIngredient,
                    onAddRecipe = onAddRecipe,
                )
            }
        }
    }
}

@Composable
fun MealListPane(
    uiState: MealsUiState,
    allRecipes: List<FoodItem>,
    allIngredients: List<FoodItem>,
    viewModel: MealsViewModel,
    onMealClick: (FoodItem) -> Unit,
    onAddClick: () -> Unit,
    onMakeMeal: (FoodItem) -> Unit = {},
) {
  Scaffold(
      topBar = {
          ListControlToolbar(
              searchQuery = uiState.searchQuery,
              onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
              searchPlaceholder = "Search Meals...",
              isSortByPrimary = true,
              primarySortIcon = Icons.Default.SortByAlpha,
              onToggleSort = { /* No-op */ },
              onAddClick = onAddClick,
          )
      }
  ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      // Create New Option
      if (
          uiState.searchQuery.isNotBlank() &&
              uiState.groupedMeals.values.flatten().none {
                it.name.equals(uiState.searchQuery, ignoreCase = true)
              }
      ) {
        item {
          CreateNewItemRow(searchQuery = uiState.searchQuery, onClick = onAddClick)
          HorizontalDivider()
        }
      }

      uiState.groupedMeals.forEach { (header, meals) ->
        stickyHeader { ListSectionHeader(text = header) }

        items(meals) { meal ->
          MealRow(
              meal = meal,
              allRecipes = allRecipes,
              allIngredients = allIngredients,
              allPackages = uiState.allPackages,
              allBridges = uiState.allBridges,
              allUnits = uiState.allUnits,
              warnings = uiState.mealWarnings[meal.id] ?: emptyList(),
              onEditClick = { onMealClick(meal) },
              onMakeClick = { onMakeMeal(meal) },
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
          Icons.Default.Restaurant,
          null,
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.outline,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text("Select a meal to view details", color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
fun MealRow(
    meal: FoodItem,
    allRecipes: List<FoodItem>,
    allIngredients: List<FoodItem>,
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onEditClick: () -> Unit,
    onMakeClick: () -> Unit,
) {
  val requirements = meal.recipeInfo?.requirementGroups?.flatMap { it.requirements } ?: emptyList()
  val recipeNames = requirements
      .filter { req -> allRecipes.any { it.id == req.measurement.foodItemId } }
      .mapNotNull { req -> allRecipes.find { it.id == req.measurement.foodItemId }?.name }
  val ingredientNames = requirements
      .filter { req -> allIngredients.any { it.id == req.measurement.foodItemId } }
      .mapNotNull { req -> allIngredients.find { it.id == req.measurement.foodItemId }?.name }

  val allNames = recipeNames + ingredientNames

  val costCents = PriceCalculator.calculateFoodItemCost(
      item = meal,
      allItemsMap = (allRecipes + allIngredients).associateBy { it.id },
      packagesByIngredient = allPackages.groupBy { it.foodItemId },
      bridgesByIngredient = allBridges.groupBy { it.foodItemId },
      allUnits = allUnits.associateBy { it.id },
  )

  val costStr =
      when {
          costCents == null -> "Circular Dependency"
          costCents > 0 -> "$${String.format("%.2f", costCents / 100.0)}"
          else -> "No price data"
      }

  val subtitle =
      if (allNames.isEmpty()) "Empty Meal"
      else "${allNames.joinToString(", ")} • Est. Cost: $costStr"

    ExpandableListItem(
        title = meal.name,
        subtitle = subtitle,
        trailingIcon =
            if (warnings.isNotEmpty()) {
                {
                    Icon(
                        Icons.Default.Warning,
                        "Data Warnings",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else null,
        actionIcon = {
            Icon(
                Icons.Default.Build,
                "Prepare Batch",
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        onActionClick = onMakeClick,
        onEditClick = onEditClick,
    ) {
        if (warnings.isNotEmpty()) {
            Text(
                "⚠️ ${warnings.size} data issues (e.g., missing prices/conversions)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text("Contents:", style = MaterialTheme.typography.labelMedium)
        if (allNames.isEmpty()) {
            Text("No items.", style = MaterialTheme.typography.bodySmall)
        } else {
            allNames.forEach { name -> Text("• $name", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun MealContentsEditor(
    currentRequirements: List<FoodItemRequirement>,
    allRecipes: List<FoodItem>,
    allIngredients: List<FoodItem>,
    allUnits: List<UnitModel>,
    onUpdateRequirements: (List<FoodItemRequirement>) -> Unit,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddRecipe: (String, (FoodItem) -> Unit) -> Unit,
) {
  var isAdding by remember { mutableStateOf(false) }

  var selectedType by remember { mutableStateOf("Recipe") }
  var selectedItemName by remember { mutableStateOf("") }
  var quantityStr by remember { mutableStateOf("") }
  var selectedUnitName by remember { mutableStateOf("") }

  Column {
    if (currentRequirements.isEmpty()) {
      Text(
          "No items added.",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
      )
    } else {
      Column {
        currentRequirements.forEach { req ->
          val item = (allRecipes + allIngredients).find { it.id == req.measurement.foodItemId }
          val name = item?.name ?: "Unknown Item"
          val uName = allUnits.find { it.id == req.measurement.unitId }?.abbreviation ?: ""
          Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                "${if (item?.isRecipe == true) "Recipe" else "Ing"}: $name ${if (req.measurement.quantity > 0) "${req.measurement.quantity} $uName" else ""}", 
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = { onUpdateRequirements(currentRequirements - req) }) {
              Icon(
                  Icons.Default.Close,
                  "Remove",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(24.dp),
              )
            }
          }
          HorizontalDivider()
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!isAdding) {
      Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Item") }
    } else {
      Card(
          colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Add Item", style = MaterialTheme.typography.labelMedium)

          Row {
            FilterChip(
                selected = selectedType == "Recipe",
                onClick = {
                  selectedType = "Recipe"
                  selectedItemName = ""
                },
                label = { Text("Recipe") },
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = selectedType == "Ingredient",
                onClick = {
                  selectedType = "Ingredient"
                  selectedItemName = ""
                },
                label = { Text("Ingredient") },
            )
          }

            SearchableDropdown(
                label = if (selectedType == "Recipe") "Recipe" else "Ingredient",
                options =
                    if (selectedType == "Recipe") allRecipes.map { it.name }
                    else allIngredients.map { it.name },
                selectedOption = selectedItemName,
                onOptionSelected = { selectedItemName = it },
                onAddOption = { name ->
                    if (selectedType == "Recipe") {
                        onAddRecipe(name) { newRecipe -> selectedItemName = newRecipe.name }
                    } else {
                        onAddIngredient(name) { newIng -> selectedItemName = newIng.name }
                    }
                },
                onDeleteOption = {},
                deleteWarningMessage = "",
            )

          if (selectedType == "Ingredient") {
            Row {
              MpOutlinedTextField(
                  value = quantityStr,
                  onValueChange = { quantityStr = it },
                  label = { Text("Qty") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.weight(1f),
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
                      deleteWarningMessage = "",
                  )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { isAdding = false }) { Text("Cancel") }
            Button(
                onClick = {
                  val found = (if (selectedType == "Recipe") allRecipes else allIngredients).find { it.name == selectedItemName }
                  if (found != null) {
                    val unit = allUnits.find { it.abbreviation == selectedUnitName }
                    val qty = quantityStr.toDoubleOrNull() ?: 0.0
                    onUpdateRequirements(currentRequirements + FoodItemRequirement(
                        measurement = ItemMeasurement(
                            foodItemId = found.id,
                            quantity = if (selectedType == "Ingredient") qty else 1.0,
                            unitId = if (selectedType == "Ingredient") unit?.id else null
                        )
                    )
                    )
                    isAdding = false
                    selectedItemName = ""
                    quantityStr = ""
                  }
                },
                enabled =
                    selectedItemName.isNotBlank() &&
                        (selectedType == "Recipe" ||
                            (quantityStr.isNotBlank() && selectedUnitName.isNotBlank())),
            ) {
              Text("Add")
            }
          }
        }
      }
    }
  }
}
