@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package io.github.and19081.mealplanner.feature.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.RecipeInfo
import io.github.and19081.mealplanner.ui.components.CreateNewItemRow
import io.github.and19081.mealplanner.ui.components.EmptyListMessage
import io.github.and19081.mealplanner.ui.components.ExpandableListItem
import io.github.and19081.mealplanner.ui.components.ListControlToolbar
import io.github.and19081.mealplanner.ui.components.ListSectionHeader
import io.github.and19081.mealplanner.ui.components.MpDetailScaffold
import io.github.and19081.mealplanner.ui.components.MpNumericStepper
import io.github.and19081.mealplanner.ui.components.MpOutlinedTextField
import io.github.and19081.mealplanner.ui.components.MpValidationWarning
import io.github.and19081.mealplanner.ui.components.SearchableDropdown
import kotlin.uuid.Uuid

@Composable
fun RecipesView(
    viewModel: RecipesViewModel,
    mode: Mode,
    isExpanded: Boolean,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddSubRecipe: (String, (FoodItem) -> Unit) -> Unit,
    onMakeRecipe: (FoodItem, Double, Uuid?, Double?, Uuid?) -> Unit = { _, _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    val draftState by viewModel.draftState.collectAsState()
    val stockWarnings by viewModel.draftStockWarnings.collectAsState()

    var recipeToMake by remember { mutableStateOf<FoodItem?>(null) }

    val actualIsExpanded =
        when (mode) {
            Mode.AUTO -> isExpanded
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

    val onRecipeClick: (FoodItem) -> Unit = {
        viewModel.startEditing(it)
    }

    val onAddClick: () -> Unit = {
        viewModel.startEditing(null, uiState.searchQuery)
    }

    val onDismissDetail: () -> Unit = {
        viewModel.clearDraft()
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
                if (draftState != null) {
                    RecipeForm(
                        draftState = draftState!!,
                        stockWarnings = stockWarnings,
                        uiState = uiState,
                        onDismiss = onDismissDetail,
                        onSave = {
                            viewModel.saveRecipe(it)
                            onDismissDetail()
                        },
                        onDelete = {
                            viewModel.deleteRecipe(draftState!!.id)
                            onDismissDetail()
                        },
                        onUpdateDraft = { viewModel.updateDraft(it) },
                        onAddIngredient = onAddIngredient,
                        onAddSubRecipe = onAddSubRecipe,
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        }
    } else {
        if (draftState != null) {
            RecipeForm(
                draftState = draftState!!,
                stockWarnings = stockWarnings,
                uiState = uiState,
                onDismiss = onDismissDetail,
                onSave = {
                    viewModel.saveRecipe(it)
                    onDismissDetail()
                },
                onDelete = {
                    viewModel.deleteRecipe(draftState!!.id)
                    onDismissDetail()
                },
                onUpdateDraft = { viewModel.updateDraft(it) },
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
            Column {
                ListControlToolbar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    searchPlaceholder = "Search recipes...",
                    isSortByPrimary = uiState.isSortByAlpha,
                    onToggleSort = { viewModel.toggleSortMode() },
                    onAddClick = onAddClick,
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FilterChip(
                        selected = uiState.isCanMakeNowFilterActive,
                        onClick = { viewModel.toggleCanMakeNowFilter() },
                        label = { Text("Can Make Now") },
                        leadingIcon = if (uiState.isCanMakeNowFilterActive) {
                            { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
            recipeInfo.instructions.forEachIndexed { idx, line ->
                Text("${idx + 1}. $line", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RecipeForm(
    draftState: RecipeDraftState,
    stockWarnings: List<StockWarning>,
    uiState: RecipesUiState,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    onUpdateDraft: ((RecipeDraftState) -> RecipeDraftState) -> Unit,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit,
    onAddSubRecipe: (String, (FoodItem) -> Unit) -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("General", "Ingredients", "Instructions")

    MpDetailScaffold(
        title = if (uiState.allRecipes.any { it.id == draftState.id }) "Edit Recipe" else "New Recipe",
        onClose = onDismiss,
        onSave = {
            val finalRecipe =
                FoodItem(
                    id = draftState.id,
                    name = draftState.name,
                    recipeInfo = RecipeInfo(
                        description = draftState.description.ifBlank { null },
                        servings = draftState.servingsStr.toDoubleOrNull() ?: 4.0,
                        instructions = draftState.instructions.filter { it.isNotBlank() },
                        mealType = draftState.mealType,
                        prepTimeMinutes = draftState.prepTimeStr.toIntOrNull() ?: 0,
                        cookTimeMinutes = draftState.cookTimeStr.toIntOrNull() ?: 0,
                        requirements = draftState.requirementGroups.flatMap { it.requirements }
                    )
                )
            onSave(finalRecipe)
        },
        saveEnabled = draftState.name.isNotBlank(),
        onDelete = onDelete,
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
    ) {
        MpValidationWarning(warnings = uiState.recipeWarnings[draftState.id] ?: emptyList())

        when (selectedTabIndex) {
            0 -> { // General
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MpOutlinedTextField(
                        value = draftState.name,
                        onValueChange = { newName -> onUpdateDraft { it.copy(name = newName) } },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MpNumericStepper(
                            value = draftState.servingsStr.toDoubleOrNull() ?: 4.0,
                            onValueChange = { newServings -> onUpdateDraft { it.copy(servingsStr = newServings.toString()) } },
                            label = "Servings",
                            modifier = Modifier.weight(1f),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Type: ${draftState.mealType.name}")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                RecipeMealType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            onUpdateDraft { it.copy(mealType = type) }
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MpNumericStepper(
                            value = draftState.prepTimeStr.toDoubleOrNull() ?: 0.0,
                            onValueChange = { newPrep -> onUpdateDraft { it.copy(prepTimeStr = newPrep.toInt().toString()) } },
                            label = "Prep (min)",
                            modifier = Modifier.weight(1f),
                            step = 5.0,
                        )
                        MpNumericStepper(
                            value = draftState.cookTimeStr.toDoubleOrNull() ?: 0.0,
                            onValueChange = { newCook -> onUpdateDraft { it.copy(cookTimeStr = newCook.toInt().toString()) } },
                            label = "Cook (min)",
                            modifier = Modifier.weight(1f),
                            step = 5.0,
                        )
                    }

                    MpOutlinedTextField(
                        value = draftState.description,
                        onValueChange = { newDesc -> onUpdateDraft { it.copy(description = newDesc) } },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Stock Check
                    Text("Stock Check", style = MaterialTheme.typography.titleMedium)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stockWarnings.forEach { warning ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when {
                                        warning.isSufficient -> Color(0xFF2E7D32)
                                        warning.isPartial -> Color(0xFFFBC02D)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Icon(
                                        if (warning.isSufficient) Icons.Default.CheckCircle else if (warning.isPartial) Icons.Default.RemoveCircle else Icons.Default.Warning,
                                        null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${warning.itemName}: ${String.format("%.1f", warning.totalInStock)} / ${warning.requiredQuantity} ${warning.unitAbbreviation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor,
                                    )
                                }
                            }
                            if (stockWarnings.isEmpty()) {
                                Text("Add ingredients to check stock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            1 -> { // Ingredients
                RecipeIngredientsEditor(
                    requirementGroups = draftState.requirementGroups,
                    allItems = uiState.allItems,
                    allUnits = uiState.allUnits,
                    onUpdate = { newGroups -> onUpdateDraft { it.copy(requirementGroups = newGroups) } },
                    onAddIngredient = onAddIngredient,
                    onAddSubRecipe = onAddSubRecipe,
                )
            }

            2 -> { // Instructions
                RecipeInstructionsEditor(
                    instructions = draftState.instructions,
                    onUpdate = { newInst -> onUpdateDraft { it.copy(instructions = newInst) } }
                )
            }
        }
    }
}

@Composable
fun RecipeInstructionsEditor(
    instructions: List<String>,
    onUpdate: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (instructions.isEmpty()) {
            Text("No instructions added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            instructions.forEachIndexed { index, step ->
                InstructionRow(
                    step = step,
                    index = index,
                    onUpdateStep = { newStep ->
                        onUpdate(instructions.mapIndexed { i, s -> if (i == index) newStep else s })
                    },
                    onRemove = {
                        onUpdate(instructions.toMutableList().apply { removeAt(index) })
                    }
                )
                HorizontalDivider()
            }
        }
        Button(onClick = { onUpdate(instructions + "") }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Step")
        }
    }
}

@Composable
fun InstructionRow(
    step: String,
    index: Int,
    onUpdateStep: (String) -> Unit,
    onRemove: () -> Unit
) {
    var isEditing by remember { mutableStateOf(step.isBlank()) }
    var draftStep by remember(step) { mutableStateOf(step) }

    if (isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium)
            MpOutlinedTextField(
                value = draftStep,
                onValueChange = { draftStep = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Step ${index + 1}") },
            )
            IconButton(onClick = {
                onUpdateStep(draftStep)
                isEditing = false
            }) {
                Icon(Icons.Default.CheckCircle, "Done", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isEditing = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = step.ifBlank { "Empty step (Tap to edit)" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Slot ${groupIndex + 1}", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex }) }) {
                            Icon(Icons.Default.Close, "Remove Slot", modifier = Modifier.size(20.dp))
                        }
                    }

                    group.requirements.forEachIndexed { reqIndex, req ->
                        IngredientRequirementRow(
                            req = req,
                            isPrimary = req.isPrimary,
                            options = options,
                            allItems = allItems,
                            allUnits = allUnits,
                            onUpdateReq = { updatedReq ->
                                onUpdate(requirementGroups.mapIndexed { i, g ->
                                    if (i == groupIndex) {
                                        g.copy(requirements = g.requirements.mapIndexed { ri, r ->
                                            if (ri == reqIndex) updatedReq else r
                                        })
                                    } else g
                                })
                            },
                            onMakePrimary = {
                                onUpdate(requirementGroups.mapIndexed { i, g ->
                                    if (i == groupIndex) {
                                        g.copy(requirements = g.requirements.mapIndexed { ri, r ->
                                            r.copy(isPrimary = ri == reqIndex)
                                        })
                                    } else g
                                })
                            },
                            onRemove = {
                                val newReqs = group.requirements.toMutableList().apply { removeAt(reqIndex) }
                                if (newReqs.none { it.isPrimary } && newReqs.isNotEmpty()) {
                                    newReqs[0] = newReqs[0].copy(isPrimary = true)
                                }
                                if (newReqs.isEmpty()) {
                                    onUpdate(requirementGroups.filterIndexed { i, _ -> i != groupIndex })
                                } else {
                                    onUpdate(requirementGroups.mapIndexed { i, g ->
                                        if (i == groupIndex) g.copy(requirements = newReqs) else g
                                    })
                                }
                            },
                            onAddIngredient = onAddIngredient
                        )
                    }

                    TextButton(onClick = {
                        onUpdate(requirementGroups.mapIndexed { i, g ->
                            if (i == groupIndex) g.copy(
                                requirements = g.requirements + FoodItemRequirement(
                                    foodItemId = Uuid.NIL,
                                    quantity = 0.0,
                                    isPrimary = false
                                )
                            ) else g
                        })
                    }) {
                        Text("+ Add Alternative")
                    }
                }
            }
        }

        Button(
            onClick = {
                onUpdate(
                    requirementGroups + FoodItemRequirementGroup(
                        requirements = listOf(
                            FoodItemRequirement(foodItemId = Uuid.NIL, quantity = 0.0, isPrimary = true)
                        )
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add New Slot")
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun IngredientRequirementRow(
    req: FoodItemRequirement,
    isPrimary: Boolean,
    options: List<String>,
    allItems: List<FoodItem>,
    allUnits: List<UnitModel>,
    onUpdateReq: (FoodItemRequirement) -> Unit,
    onMakePrimary: () -> Unit,
    onRemove: () -> Unit,
    onAddIngredient: (String, (FoodItem) -> Unit) -> Unit
) {
    var isEditing by remember { mutableStateOf(req.foodItemId == Uuid.NIL) }

    // Add a local string state to buffer the raw text input (allows typing decimals safely)
    var qtyText by remember {
        mutableStateOf(if (req.quantity == 0.0) "" else req.quantity.toString())
    }

    val selectedItem = allItems.find { it.id == req.foodItemId }
    val selectedItemName = (if (selectedItem?.isRecipe == true) "[Recipe] " else "") + (selectedItem?.name ?: "")
    val selectedUnitName = allUnits.find { it.id == req.unitId }?.abbreviation ?: ""

    if (isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isPrimary, onClick = onMakePrimary)

            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                SearchableDropdown(
                    label = "Item",
                    options = options,
                    selectedOption = selectedItemName,
                    onOptionSelected = { option ->
                        val isRecipe = option.startsWith("[Recipe] ")
                        val cleanName = if (isRecipe) option.removePrefix("[Recipe] ") else option
                        val found = allItems.find { it.name == cleanName && it.isRecipe == isRecipe }
                        onUpdateReq(req.copy(foodItemId = found?.id ?: Uuid.NIL))
                    },
                    onAddOption = { name ->
                        onAddIngredient(name) { newIng ->
                            onUpdateReq(req.copy(foodItemId = newIng.id))
                        }
                    },
                    onDeleteOption = {},
                    deleteWarningMessage = "",
                )

                MpOutlinedTextField(
                    // Bind the text field to our raw string state
                    value = qtyText,
                    onValueChange = { newText ->
                        qtyText = newText // Update the raw text immediately so decimals don't disappear
                        // Silently parse and update the Double state in the background
                        onUpdateReq(req.copy(quantity = newText.toDoubleOrNull() ?: 0.0))
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                SearchableDropdown(
                    label = "Unit",
                    options = allUnits.map { it.abbreviation },
                    selectedOption = selectedUnitName,
                    onOptionSelected = { abbr ->
                        val unit = allUnits.find { it.abbreviation == abbr }
                        onUpdateReq(req.copy(unitId = unit?.id))
                    },
                    onAddOption = {},
                    onDeleteOption = {},
                    deleteWarningMessage = "",
                )
            }

            IconButton(onClick = { isEditing = false }) {
                Icon(Icons.Default.CheckCircle, "Done", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isPrimary, onClick = onMakePrimary)
            Spacer(modifier = Modifier.width(8.dp))

            val qtyString = if (req.quantity > 0.0) req.quantity.toString() else ""
            val displayText = listOf(selectedItemName, ":", qtyString, selectedUnitName )
                .filter { it.isNotBlank() }
                .joinToString(" ")

            Text(
                text = displayText.ifBlank { "Empty Requirement (Click edit)" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}