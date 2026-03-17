@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.*
import io.github.and19081.mealplanner.uicomponents.*
import kotlin.uuid.Uuid

@Composable
fun RecipesView(
    viewModel: RecipesViewModel,
    mode: io.github.and19081.mealplanner.settings.Mode,
    isExpanded: Boolean,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddSubRecipe: (String, (FoodItem) -> Unit) -> Unit,
    onMakeRecipe: (FoodItem, Double, Uuid?, Double?, Uuid?) -> Unit = { _, _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    val selectedRecipe = uiState.allRecipes.find { it.id.toString() == selectedRecipeId }

    var recipeToMake by remember { mutableStateOf<FoodItem?>(null) }

    val actualIsExpanded =
        when (mode) {
            io.github.and19081.mealplanner.settings.Mode.AUTO -> isExpanded
            io.github.and19081.mealplanner.settings.Mode.DESKTOP -> true
            io.github.and19081.mealplanner.settings.Mode.MOBILE -> false
        }

    val onRecipeClick: (FoodItem) -> Unit = {
        selectedRecipeId = it.id.toString()
        isAdding = false
    }

    val onAddClick: () -> Unit = {
        selectedRecipeId = null
        isAdding = true
    }

    val onDismissDetail: () -> Unit = {
        selectedRecipeId = null
        isAdding = false
    }

    if (recipeToMake != null) {
        PrepareBatchDialog(
            itemName = recipeToMake!!.name,
            allItems = uiState.allItems,
            allUnits = uiState.allUnits,
            onDismiss = { recipeToMake = null },
            onConfirm = { multiplier, yieldIngId, yieldQty, yieldUnitId ->
                onMakeRecipe(recipeToMake!!, multiplier, yieldIngId, yieldQty, yieldUnitId)
                recipeToMake = null
            }
        )
    }

  if (actualIsExpanded) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(0.4f)) {
        RecipeListPane(
            uiState = uiState,
            viewModel = viewModel,
            onRecipeClick = onRecipeClick,
            onAddClick = onAddClick,
            onMakeRecipe = { recipeToMake = it },
        )
      }

      VerticalDivider(modifier = Modifier.width(1.dp))

      Box(modifier = Modifier.weight(0.6f)) {
        if (selectedRecipe != null || isAdding) {
          RecipeForm(
              recipe = selectedRecipe,
              initialName = if (isAdding) uiState.searchQuery else "",
              uiState = uiState,
              onDismiss = onDismissDetail,
              onSave = {
                viewModel.saveRecipe(it)
                onDismissDetail()
              },
              onDelete = selectedRecipe?.let { r -> { viewModel.deleteRecipe(r.id) } },
              onAddIngredient = onAddIngredient,
              onAddSubRecipe = onAddSubRecipe,
          )
        } else {
          EmptyDetailPlaceholder()
        }
      }
    }
  } else {
    if (selectedRecipe != null || isAdding) {
      RecipeForm(
          recipe = selectedRecipe,
          initialName = if (isAdding) uiState.searchQuery else "",
          uiState = uiState,
          onDismiss = onDismissDetail,
          onSave = {
            viewModel.saveRecipe(it)
            onDismissDetail()
          },
          onDelete = selectedRecipe?.let { r -> { viewModel.deleteRecipe(r.id) } },
          onAddIngredient = onAddIngredient,
          onAddSubRecipe = onAddSubRecipe,
      )
    } else {
      RecipeListPane(
          uiState = uiState,
          viewModel = viewModel,
          onRecipeClick = onRecipeClick,
          onAddClick = onAddClick,
          onMakeRecipe = { recipeToMake = it },
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
      Text("Select a recipe to view details", color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
fun RecipeListPane(
    uiState: RecipesUiState,
    viewModel: RecipesViewModel,
    onRecipeClick: (FoodItem) -> Unit,
    onAddClick: () -> Unit,
    onMakeRecipe: (FoodItem) -> Unit = {},
) {
  Scaffold(
      topBar = {
        ListControlToolbar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            searchPlaceholder = "Search recipes...",
            isSortByPrimary = uiState.isCanMakeNowFilterActive,
            onToggleSort = { viewModel.toggleCanMakeNowFilter() },
            onAddClick = onAddClick,
        )
      }
  ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
      uiState.groupedRecipes.forEach { (category, recipes) ->
        item { ListSectionHeader(category) }
        items(recipes) { recipe ->
          RecipeItemRow(
              recipe = recipe,
              warnings = uiState.recipeWarnings[recipe.id] ?: emptyList(),
              onEditClick = { onRecipeClick(recipe) },
              onMakeClick = { onMakeRecipe(recipe) },
          )
          HorizontalDivider()
        }
      }

      if (uiState.groupedRecipes.isEmpty()) {
        item {
          EmptyListMessage(
              message =
                  if (uiState.searchQuery.isBlank()) "No recipes found. Add your first recipe!"
                  else "No recipes match your search.",
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
fun RecipeItemRow(
    recipe: FoodItem,
    warnings: List<DataWarning>,
    onEditClick: () -> Unit,
    onMakeClick: () -> Unit,
) {
  val recipeInfo = recipe.recipeInfo
  val ingredientCount = recipeInfo?.requirements?.size ?: 0
  val costCents = 0L // Placeholder
  val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "---"
  val perPersonStr =
      if (recipeInfo != null && recipeInfo.servings > 0.1 && costCents > 0) {
        val perPerson = (costCents / recipeInfo.servings) / 100.0
        if (perPerson.isFinite()) " ($${String.format("%.2f", perPerson)}/p)" else ""
      } else ""
  val subtitle =
      "Serves ${recipeInfo?.servings ?: 0} • $ingredientCount requirements • Total: $costStr$perPersonStr"

  ExpandableListItem(
      title = recipe.name,
      subtitle = subtitle,
      trailingIcon =
          if (warnings.isNotEmpty()) {
            { Icon(Icons.Default.Warning, "Warning", tint = MaterialTheme.colorScheme.error) }
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
    if (recipeInfo?.description != null) {
      Text(
          recipeInfo.description,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(bottom = 8.dp),
      )
    }

    Text("Instructions:", style = MaterialTheme.typography.labelMedium)
    if (recipeInfo?.instructions.isNullOrEmpty()) {
      Text("No instructions.", style = MaterialTheme.typography.bodySmall)
    } else {
      recipeInfo!!.instructions.forEachIndexed { idx, line ->
        Text("${idx + 1}. $line", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
fun RecipeForm(
    recipe: FoodItem?,
    initialName: String,
    uiState: RecipesUiState,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddSubRecipe: (String, (FoodItem) -> Unit) -> Unit,
) {
  val allUnits = uiState.allUnits
  val allItems = uiState.allItems
  val allBridges = uiState.allBridges
  
  var name by rememberSaveable(recipe) { mutableStateOf(recipe?.name ?: initialName) }
  var servingsStr by rememberSaveable(recipe) { mutableStateOf(recipe?.recipeInfo?.servings?.toString() ?: "4.0") }
  var description by rememberSaveable(recipe) { mutableStateOf(recipe?.recipeInfo?.description ?: "") }
  var prepTimeStr by rememberSaveable(recipe) { mutableStateOf(recipe?.recipeInfo?.prepTimeMinutes?.toString() ?: "0") }
  var cookTimeStr by rememberSaveable(recipe) { mutableStateOf(recipe?.recipeInfo?.cookTimeMinutes?.toString() ?: "0") }
  var mealType by rememberSaveable(recipe) { mutableStateOf(recipe?.recipeInfo?.mealType ?: RecipeMealType.Dinner) }

  val recipeId = remember(recipe) { recipe?.id ?: Uuid.random() }
  var requirementGroups by remember(recipe) { 
      mutableStateOf(
          recipe?.recipeInfo?.requirements?.let { 
              listOf(FoodItemRequirementGroup(requirements = it)) 
          } ?: emptyList()
      )
  }
  var instructions by remember(recipe) { mutableStateOf(recipe?.recipeInfo?.instructions ?: emptyList()) }

  var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("General", "Ingredients", "Instructions")

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = if (recipe == null) "New Recipe" else "Edit Recipe",
      onClose = onDismiss,
      onSave = {
        val finalRecipe =
            FoodItem(
                id = recipeId,
                name = name,
                recipeInfo = RecipeInfo(
                    description = description.ifBlank { null },
                    servings = servingsStr.toDoubleOrNull() ?: 4.0,
                    instructions = instructions.filter { it.isNotBlank() },
                    mealType = mealType,
                    prepTimeMinutes = prepTimeStr.toIntOrNull() ?: 0,
                    cookTimeMinutes = cookTimeStr.toIntOrNull() ?: 0,
                    requirements = requirementGroups.flatMap { it.requirements }
                )
            )
        onSave(finalRecipe)
      },
      saveEnabled = name.isNotBlank(),
      onDelete = onDelete,
      tabs = tabs,
      selectedTabIndex = selectedTabIndex,
      onTabSelected = { selectedTabIndex = it },
  ) {
    MpValidationWarning(warnings = uiState.recipeWarnings[recipeId] ?: emptyList())

    when (selectedTabIndex) {
      0 -> { // General
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          MpOutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Name") },
              modifier = Modifier.fillMaxWidth(),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpNumericStepper(
                value = servingsStr.toDoubleOrNull() ?: 4.0,
                onValueChange = { servingsStr = it.toString() },
                label = "Servings",
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
              var expanded by remember { mutableStateOf(false) }
              OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Type: ${mealType.name}")
              }
              DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RecipeMealType.entries.forEach { type ->
                  DropdownMenuItem(
                      text = { Text(type.name) },
                      onClick = {
                        mealType = type
                        expanded = false
                      },
                  )
                }
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpNumericStepper(
                value = prepTimeStr.toDoubleOrNull() ?: 0.0,
                onValueChange = { prepTimeStr = it.toInt().toString() },
                label = "Prep (min)",
                modifier = Modifier.weight(1f),
                step = 5.0,
            )
            MpNumericStepper(
                value = cookTimeStr.toDoubleOrNull() ?: 0.0,
                onValueChange = { cookTimeStr = it.toInt().toString() },
                label = "Cook (min)",
                modifier = Modifier.weight(1f),
                step = 5.0,
            )
          }

          MpOutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Description") },
              modifier = Modifier.fillMaxWidth(),
          )

          // Stock Check
          Text("Stock Check", style = MaterialTheme.typography.titleMedium)
          val pantryByItem = remember(uiState.pantryItems) {
              uiState.pantryItems.groupBy { it.foodItemId }
          }
          Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              requirementGroups.flatMap { it.requirements }.forEach { req ->
                    val item = allItems.find { it.id == req.foodItemId }
                    val itemName = item?.name ?: "Unknown Item"
                    val pantryItems = pantryByItem[req.foodItemId] ?: emptyList()

                    var totalInStock = 0.0
                    for (pItem in pantryItems) {
                      totalInStock += UnitConverter.convert(
                              amount = pItem.quantity,
                              fromUnitId = pItem.unitId,
                              toUnitId = req.unitId ?: item?.preferredUnitId ?: Uuid.NIL,
                              allUnits = allUnits.associateBy { it.id },
                              bridges = allBridges,
                          ) ?: 0.0
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      val statusColor = when {
                            totalInStock >= req.quantity -> Color(0xFF2E7D32)
                            totalInStock > 0 -> Color(0xFFFBC02D)
                            else -> MaterialTheme.colorScheme.error
                          }
                      Icon(if (totalInStock >= req.quantity) Icons.Default.CheckCircle else if (totalInStock > 0) Icons.Default.RemoveCircle else Icons.Default.Warning, null, tint = statusColor, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                          text = "$itemName: ${String.format("%.1f", totalInStock)} / ${req.quantity} ${allUnits.find { it.id == req.unitId }?.abbreviation ?: ""}",
                          style = MaterialTheme.typography.bodySmall,
                          color = statusColor,
                      )
                    }
                  }
            }
          }
        }
      }

      1 -> { // Ingredients
        RecipeIngredientsEditor(
            requirementGroups = requirementGroups,
            allItems = allItems,
            allUnits = allUnits,
            onUpdate = { requirementGroups = it },
            onAddIngredient = onAddIngredient,
            onAddSubRecipe = onAddSubRecipe,
        )
      }

      2 -> { // Instructions
        RecipeInstructionsEditor(instructions = instructions, onUpdate = { instructions = it })
      }
    }
  }
}

@Composable
fun RecipeInstructionsEditor(instructions: List<String>, onUpdate: (List<String>) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (instructions.isEmpty()) {
      Text("No instructions added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
    } else {
      instructions.forEachIndexed { index, step ->
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium)
          MpOutlinedTextField(
              value = step,
              onValueChange = { newStep -> onUpdate(instructions.mapIndexed { i, s -> if (i == index) newStep else s }) },
              modifier = Modifier.weight(1f),
              placeholder = { Text("Step ${index + 1}") },
          )
          IconButton(onClick = { onUpdate(instructions.toMutableList().apply { removeAt(index) }) }) {
            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
          }
        }
        HorizontalDivider()
      }
    }
    Button(onClick = { onUpdate(instructions + "") }, modifier = Modifier.fillMaxWidth()) {
      Text("+ Add Step")
    }
  }
}

@Composable
fun RecipeIngredientsEditor(
    requirementGroups: List<FoodItemRequirementGroup>,
    allItems: List<FoodItem>,
    allUnits: List<UnitModel>,
    onUpdate: (List<FoodItemRequirementGroup>) -> Unit,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddSubRecipe: (String, (FoodItem) -> Unit) -> Unit,
) {
  val options = remember(allItems) {
      allItems.map { (if (it.isRecipe) "[Recipe] " else "") + it.name }
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    requirementGroups.forEachIndexed { groupIndex, group ->
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Slot ${groupIndex + 1}", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex }) }) {
              Icon(Icons.Default.Close, "Remove Slot", modifier = Modifier.size(20.dp))
            }
          }

          group.requirements.forEachIndexed { reqIndex, req ->
            val selectedItem = allItems.find { it.id == req.foodItemId }
            val selectedItemName = (if (selectedItem?.isRecipe == true) "[Recipe] " else "") + (selectedItem?.name ?: "")
            val selectedUnitName = allUnits.find { it.id == req.unitId }?.abbreviation ?: ""

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              RadioButton(selected = req.isPrimary, onClick = {
                    onUpdate(requirementGroups.mapIndexed { i, g ->
                        if (i == groupIndex) g.copy(requirements = g.requirements.mapIndexed { ri, r -> r.copy(isPrimary = ri == reqIndex) }) else g
                    })
              })

              FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.widthIn(min = 120.dp).fillMaxWidth(0.6f)) {
                  SearchableDropdown(
                      label = "Item",
                      options = options,
                      selectedOption = selectedItemName,
                      onOptionSelected = { option ->
                        val isRecipe = option.startsWith("[Recipe] ")
                        val cleanName = if (isRecipe) option.removePrefix("[Recipe] ") else option
                        val found = allItems.find { it.name == cleanName && it.isRecipe == isRecipe }
                        onUpdate(requirementGroups.mapIndexed { i, g ->
                            if (i == groupIndex) g.copy(requirements = g.requirements.mapIndexed { ri, r -> if (ri == reqIndex) r.copy(foodItemId = found?.id ?: Uuid.NIL) else r }) else g
                        })
                      },
                      onAddOption = { name ->
                        onAddIngredient(name) { newIng ->
                          onUpdate(requirementGroups.mapIndexed { i, g ->
                              if (i == groupIndex) g.copy(requirements = g.requirements.mapIndexed { ri, r -> if (ri == reqIndex) r.copy(foodItemId = newIng.id) else r }) else g
                          })
                        }
                      },
                      onDeleteOption = {},
                      deleteWarningMessage = "",
                  )
                }

                MpOutlinedTextField(
                    value = if (req.quantity == 0.0) "" else req.quantity.toString(),
                    onValueChange = { onUpdate(requirementGroups.mapIndexed { i, g ->
                        if (i == groupIndex) g.copy(requirements = g.requirements.mapIndexed { ri, r -> if (ri == reqIndex) r.copy(quantity = it.toDoubleOrNull() ?: 0.0) else r }) else g
                    }) },
                    label = { Text("Qty") },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Box(modifier = Modifier.width(100.dp)) {
                  SearchableDropdown(
                      label = "Unit",
                      options = allUnits.map { it.abbreviation },
                      selectedOption = selectedUnitName,
                      onOptionSelected = { abbr ->
                        val unit = allUnits.find { it.abbreviation == abbr }
                        onUpdate(requirementGroups.mapIndexed { i, g ->
                            if (i == groupIndex) g.copy(requirements = g.requirements.mapIndexed { ri, r -> if (ri == reqIndex) r.copy(unitId = unit?.id) else r }) else g
                        })
                      },
                      onAddOption = {},
                      onDeleteOption = {},
                      deleteWarningMessage = "",
                  )
                }
              }

              IconButton(onClick = {
                val newReqs = group.requirements.toMutableList().apply { removeAt(reqIndex) }
                if (newReqs.none { it.isPrimary } && newReqs.isNotEmpty()) newReqs[0] = newReqs[0].copy(isPrimary = true)
                if (newReqs.isEmpty()) onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex })
                else onUpdate(requirementGroups.mapIndexed { i, g -> if (i == groupIndex) g.copy(requirements = newReqs) else g })
              }) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
              }
            }
          }

          TextButton(onClick = {
            onUpdate(requirementGroups.mapIndexed { i, g ->
                if (i == groupIndex) g.copy(requirements = g.requirements + FoodItemRequirement(foodItemId = Uuid.NIL, quantity = 0.0, isPrimary = false)) else g
            })
          }) {
            Text("+ Add Alternative")
          }
        }
      }
    }

    Button(onClick = { onUpdate(requirementGroups + FoodItemRequirementGroup(requirements = listOf(FoodItemRequirement(foodItemId = Uuid.NIL, quantity = 0.0, isPrimary = true)))) }, modifier = Modifier.fillMaxWidth()) {
      Text("+ Add New Slot")
    }
  }
}
