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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.uicomponents.CreateNewItemRow
import io.github.and19081.mealplanner.uicomponents.EmptyListMessage
import io.github.and19081.mealplanner.uicomponents.ExpandableListItem
import io.github.and19081.mealplanner.uicomponents.ListControlToolbar
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpNumericStepper
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import kotlin.uuid.Uuid

@Composable
fun RecipesView(
    viewModel: RecipesViewModel,
    mode: io.github.and19081.mealplanner.settings.Mode,
    isExpanded: Boolean,
    onAddIngredient: (String, (Ingredient) -> Unit) -> Unit,
    onAddSubRecipe: (String, (Recipe) -> Unit) -> Unit,
    onMakeRecipe: (Recipe, Double, Uuid?, Double?, Uuid?) -> Unit = { _, _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Use rememberSaveable with IDs to survive screen rotation
    var selectedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    val selectedRecipe = uiState.allRecipes.find { it.id.toString() == selectedRecipeId }

    // 2. State for the new Make Recipe dialog
    var recipeToMake by remember { mutableStateOf<Recipe?>(null) }

    val actualIsExpanded =
        when (mode) {
            io.github.and19081.mealplanner.settings.Mode.AUTO -> isExpanded
            io.github.and19081.mealplanner.settings.Mode.DESKTOP -> true
            io.github.and19081.mealplanner.settings.Mode.MOBILE -> false
        }

    val onRecipeClick: (Recipe) -> Unit = {
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
            allIngredients = uiState.allIngredients,
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
      // List Pane
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

      // Detail Pane
      Box(modifier = Modifier.weight(0.6f)) {
        if (selectedRecipe != null || isAdding) {
          RecipeForm(
              recipe = selectedRecipe,
              initialName = if (isAdding) uiState.searchQuery else "",
              uiState = uiState,
              allIngredients = uiState.allIngredients,
              allRecipes = uiState.allRecipes,
              allPackages = uiState.allPackages,
              allBridges = uiState.allBridges,
              allUnits = uiState.allUnits,
              onDismiss = onDismissDetail,
              onSave = {
                viewModel.saveRecipe(it)
                onDismissDetail()
              },
              onDelete = selectedRecipe?.let { r -> { viewModel.deleteRecipe(r.id) } },
              onAddIngredient = onAddIngredient,
              onAddSubRecipe = onAddSubRecipe,
              warnings = selectedRecipe?.let { uiState.recipeWarnings[it.id] } ?: emptyList(),
          )
        } else {
          EmptyDetailPlaceholder()
        }
      }
    }
  } else {
    // Mobile View
    if (selectedRecipe != null || isAdding) {
      RecipeForm(
          recipe = selectedRecipe,
          initialName = if (isAdding) uiState.searchQuery else "",
          uiState = uiState,
          allIngredients = uiState.allIngredients,
          allRecipes = uiState.allRecipes,
          allPackages = uiState.allPackages,
          allBridges = uiState.allBridges,
          allUnits = uiState.allUnits,
          onDismiss = onDismissDetail,
          onSave = {
            viewModel.saveRecipe(it)
            onDismissDetail()
          },
          onDelete = selectedRecipe?.let { r -> { viewModel.deleteRecipe(r.id) } },
          onAddIngredient = onAddIngredient,
          onAddSubRecipe = onAddSubRecipe,
          warnings = selectedRecipe?.let { uiState.recipeWarnings[it.id] } ?: emptyList(),
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
fun RecipeListPane(
    uiState: RecipesUiState,
    viewModel: RecipesViewModel,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit,
    onMakeRecipe: (Recipe) -> Unit = {},
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
    recipe: Recipe,
    warnings: List<DataWarning>,
    onEditClick: () -> Unit,
    onMakeClick: () -> Unit,
) {
  val ingredientCount = recipe.requirementGroups.size
  val costCents = 0L // Placeholder
  val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "---"
  val perPersonStr =
      if (recipe.servings > 0.1 && costCents > 0) {
        val perPerson = (costCents / recipe.servings) / 100.0
        if (perPerson.isFinite()) " ($${String.format("%.2f", perPerson)}/p)" else ""
      } else ""
  val subtitle =
      "Serves ${recipe.servings} • $ingredientCount requirements • Total: $costStr$perPersonStr"

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
    if (recipe.description != null) {
      Text(
          recipe.description,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(bottom = 8.dp),
      )
    }

    Text("Instructions:", style = MaterialTheme.typography.labelMedium)
    if (recipe.instructions.isEmpty()) {
      Text("No instructions.", style = MaterialTheme.typography.bodySmall)
    } else {
      recipe.instructions.forEachIndexed { idx, line ->
        Text("${idx + 1}. $line", style = MaterialTheme.typography.bodySmall)
      }
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
fun RecipeForm(
    recipe: Recipe?,
    initialName: String,
    uiState: RecipesUiState,
    allIngredients: List<Ingredient>,
    allRecipes: List<Recipe>,
    allPackages: List<io.github.and19081.mealplanner.ingredients.Package>,
    allBridges: List<io.github.and19081.mealplanner.ingredients.BridgeConversion>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddIngredient: (String, (Ingredient) -> Unit) -> Unit,
    onAddSubRecipe: (String, (Recipe) -> Unit) -> Unit,
) {
  var name by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.name ?: initialName)
      }
  var servingsStr by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.servings?.toString() ?: "4.0")
      }
  var description by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.description ?: "")
      }

  var prepTimeStr by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.prepTimeMinutes?.toString() ?: "0")
      }
  var cookTimeStr by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.cookTimeMinutes?.toString() ?: "0")
      }

  var mealType by
      androidx.compose.runtime.saveable.rememberSaveable(recipe) {
        mutableStateOf(recipe?.mealType ?: RecipeMealType.Dinner)
      }

  val recipeId = remember(recipe) { recipe?.id ?: kotlin.uuid.Uuid.random() }
  var requirementGroups by
      remember(recipe) { mutableStateOf(recipe?.requirementGroups ?: emptyList()) }
  var instructions by remember(recipe) { mutableStateOf(recipe?.instructions ?: emptyList()) }

  var selectedTabIndex by
      androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("General", "Ingredients", "Instructions")

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = if (recipe == null) "New Recipe" else "Edit Recipe",
      onClose = onDismiss,
      onSave = {
        val finalRecipe =
            Recipe(
                id = recipeId,
                name = name,
                description = description.ifBlank { null },
                servings = servingsStr.toDoubleOrNull() ?: 4.0,
                instructions = instructions.filter { it.isNotBlank() },
                mealType = mealType,
                prepTimeMinutes = prepTimeStr.toIntOrNull() ?: 0,
                cookTimeMinutes = cookTimeStr.toIntOrNull() ?: 0,
                producesIngredientId = null,
                amountPerServing = null,
                requirementGroups = requirementGroups,
            )
        onSave(finalRecipe)
      },
      saveEnabled = name.isNotBlank(),
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
    MpValidationWarning(warnings = warnings)

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
          val pantryByIngredient =
              remember(uiState.pantryItems) {
                (uiState.pantryItems as List<PantryItem>).groupBy { it.ingredientId }
              }
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                  ),
              modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              requirementGroups
                  .flatMap { it.requirements }
                  .forEach { req ->
                    val itemName =
                        if (req.subRecipeId != null) {
                          allRecipes.find { it.id == req.subRecipeId }?.name ?: "Unknown Recipe"
                        } else {
                          allIngredients.find { it.id == req.ingredientId }?.name
                              ?: "Unknown Ingredient"
                        }

                    val pantryItems =
                        if (req.ingredientId != null) {
                          pantryByIngredient[req.ingredientId] ?: emptyList()
                        } else emptyList()

                    var totalInStock = 0.0
                    for (item in pantryItems) {
                      totalInStock +=
                          UnitConverter.convert(
                              amount = item.quantity,
                              fromUnitId = item.unitId,
                              toUnitId = req.unitId,
                              allUnits = allUnits.associateBy { it.id },
                              bridges = allBridges,
                          ) ?: 0.0
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      val statusColor =
                          when {
                            totalInStock >= req.quantity -> Color(0xFF2E7D32)
                            totalInStock > 0 -> Color(0xFFFBC02D)
                            else -> MaterialTheme.colorScheme.error
                          }
                      val icon =
                          when {
                            totalInStock >= req.quantity -> Icons.Default.CheckCircle
                            totalInStock > 0 -> Icons.Default.RemoveCircle
                            else -> Icons.Default.Warning
                          }
                      Icon(icon, null, tint = statusColor, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                          text =
                              "$itemName: ${String.format("%.1f", totalInStock)} / ${req.quantity} ${allUnits.find { it.id == req.unitId }?.abbreviation ?: ""}",
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
            allIngredients = allIngredients,
            allRecipes = allRecipes,
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
      Text(
          "No instructions added.",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
      )
    } else {
      instructions.forEachIndexed { index, step ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium)
          MpOutlinedTextField(
              value = step,
              onValueChange = { newStep ->
                val newList = instructions.mapIndexed { i, s -> if (i == index) newStep else s }
                onUpdate(newList)
              },
              modifier = Modifier.weight(1f),
              placeholder = { Text("Step ${index + 1}") },
          )
          IconButton(
              onClick = {
                val newList = instructions.toMutableList()
                newList.removeAt(index)
                onUpdate(newList)
              }
          ) {
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

    Spacer(modifier = Modifier.height(8.dp))

    Button(onClick = { onUpdate(instructions + "") }, modifier = Modifier.fillMaxWidth()) {
      Text("+ Add Step")
    }
  }
}

@Composable
fun RecipeIngredientsEditor(
    requirementGroups: List<RecipeRequirementGroup>,
    allIngredients: List<Ingredient>,
    allRecipes: List<Recipe>,
    allUnits: List<UnitModel>,
    onUpdate: (List<RecipeRequirementGroup>) -> Unit,
    onAddIngredient: (String, (Ingredient) -> Unit) -> Unit,
    onAddSubRecipe: (String, (Recipe) -> Unit) -> Unit,
) {
  val combinedOptions =
      remember(allIngredients, allRecipes) {
        allIngredients.map { it.name } + allRecipes.map { "[Recipe] ${it.name}" }
      }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    requirementGroups.forEachIndexed { groupIndex, group ->
      Card(
          colors =
              CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
      ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Ingredient Slot ${groupIndex + 1}", style = MaterialTheme.typography.titleSmall)
            IconButton(
                onClick = { onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex }) }
            ) {
              Icon(Icons.Default.Close, "Remove Slot", modifier = Modifier.size(20.dp))
            }
          }

          group.requirements.forEachIndexed { reqIndex, req ->
            val selectedItemName =
                if (req.subRecipeId != null) {
                  val recipeName = allRecipes.find { it.id == req.subRecipeId }?.name ?: ""
                  if (recipeName.isNotEmpty()) "[Recipe] $recipeName" else ""
                } else {
                  allIngredients.find { it.id == req.ingredientId }?.name ?: ""
                }

            val selectedUnitName = allUnits.find { it.id == req.unitId }?.abbreviation ?: ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              RadioButton(
                  selected = req.isPrimary,
                  onClick = {
                    val newReqs =
                        group.requirements.mapIndexed { i, r -> r.copy(isPrimary = i == reqIndex) }
                    val newGroups =
                        requirementGroups.mapIndexed { i, g ->
                          if (i == groupIndex) group.copy(requirements = newReqs) else g
                        }
                    onUpdate(newGroups)
                  },
              )

              // Use FlowRow for the main fields to prevent squishing on narrow screens
              FlowRow(
                  modifier = Modifier.weight(1f),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                // Unified SearchableDropdown for Ingredient/Recipe
                Box(modifier = Modifier.widthIn(min = 120.dp).fillMaxWidth(0.6f)) {
                  SearchableDropdown(
                      label = "Item",
                      options = combinedOptions,
                      selectedOption = selectedItemName,
                      onOptionSelected = { option ->
                        val isRecipe = option.startsWith("[Recipe] ")
                        val cleanName = if (isRecipe) option.removePrefix("[Recipe] ") else option

                        val newReq =
                            if (isRecipe) {
                              val recipe = allRecipes.find { it.name == cleanName }
                              req.copy(subRecipeId = recipe?.id, ingredientId = null)
                            } else {
                              val ingredient = allIngredients.find { it.name == cleanName }
                              req.copy(ingredientId = ingredient?.id, subRecipeId = null)
                            }

                        val newReqs =
                            group.requirements.mapIndexed {
                              i,
                              r ->
                              if (i == reqIndex) newReq else r
                            }
                        onUpdate(
                            requirementGroups.mapIndexed { i, g ->
                              if (i == groupIndex) group.copy(requirements = newReqs) else g
                            }
                        )
                      },
                      onAddOption = { name ->
                        // Default to adding ingredient for now
                        onAddIngredient(name) { newIng ->
                          val newReq = req.copy(ingredientId = newIng.id, subRecipeId = null)
                          val newReqs =
                              group.requirements.mapIndexed {
                                i,
                                r ->
                                if (i == reqIndex) newReq else r
                              }
                          onUpdate(
                              requirementGroups.mapIndexed { i, g ->
                                if (i == groupIndex) group.copy(requirements = newReqs) else g
                              }
                          )
                        }
                      },
                      onDeleteOption = {},
                      deleteWarningMessage = "",
                  )
                }

                MpOutlinedTextField(
                    value = if (req.quantity == 0.0) "" else req.quantity.toString(),
                    onValueChange = { qtyStr ->
                      val qty = qtyStr.toDoubleOrNull() ?: 0.0
                      val newReqs =
                          group.requirements.mapIndexed {
                            i,
                            r ->
                            if (i == reqIndex) r.copy(quantity = qty) else r
                          }
                      onUpdate(
                          requirementGroups.mapIndexed { i, g ->
                            if (i == groupIndex) group.copy(requirements = newReqs) else g
                          }
                      )
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Box(modifier = Modifier.width(100.dp)) {
                  SearchableDropdown(
                      label = "Unit",
                      options = allUnits.map { it.abbreviation },
                      selectedOption = selectedUnitName,
                      onOptionSelected = { unitAbbr ->
                        val unit = allUnits.find { it.abbreviation == unitAbbr }
                        if (unit != null) {
                          val newReqs =
                              group.requirements.mapIndexed {
                                i,
                                r ->
                                if (i == reqIndex) r.copy(unitId = unit.id) else r
                              }
                          onUpdate(
                              requirementGroups.mapIndexed { i, g ->
                                if (i == groupIndex) group.copy(requirements = newReqs) else g
                              }
                          )
                        }
                      },
                      onAddOption = {},
                      onDeleteOption = {},
                      deleteWarningMessage = "",
                  )
                }
              }

              IconButton(
                  onClick = {
                    val newReqs = group.requirements.toMutableList()
                    newReqs.removeAt(reqIndex)
                    if (newReqs.none { it.isPrimary } && newReqs.isNotEmpty()) {
                      newReqs[0] = newReqs[0].copy(isPrimary = true)
                    }
                    if (newReqs.isEmpty()) {
                      onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex })
                    } else {
                      onUpdate(
                          requirementGroups.mapIndexed { i, g ->
                            if (i == groupIndex) group.copy(requirements = newReqs) else g
                          }
                      )
                    }
                  }
              ) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
              }
            }
          }

          TextButton(
              onClick = {
                val newReq =
                    RecipeRequirement(
                        quantity = 0.0,
                        unitId = null,
                        isPrimary = false,
                    )
                onUpdate(
                    requirementGroups.mapIndexed { i, g ->
                      if (i == groupIndex) group.copy(requirements = group.requirements + newReq)
                      else g
                    }
                )
              }
          ) {
            Text("+ Add Alternative")
          }
        }
      }
    }

    Button(
        onClick = {
          val newGroup =
              RecipeRequirementGroup(
                  id = kotlin.uuid.Uuid.random(),
                  requirements =
                      listOf(
                          RecipeRequirement(
                              quantity = 0.0,
                              unitId = null,
                              isPrimary = true,
                          )
                      ),
              )
          onUpdate(requirementGroups + newGroup)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text("+ Add New Ingredient Slot")
    }
  }
}
