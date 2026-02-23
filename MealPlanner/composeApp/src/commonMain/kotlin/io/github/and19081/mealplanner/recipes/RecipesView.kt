@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.uicomponents.CreateNewItemRow
import io.github.and19081.mealplanner.uicomponents.EmptyListMessage
import io.github.and19081.mealplanner.uicomponents.ExpandableListItem
import io.github.and19081.mealplanner.uicomponents.ListControlToolbar
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpEditDialogScaffold
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.MpNumericStepper
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

import io.github.and19081.mealplanner.settings.Mode

@Composable
fun RecipesView(
    viewModel: RecipesViewModel,
    mode: Mode
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var creationName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BoxWithConstraints {
        val isExpanded = when (mode) {
            Mode.AUTO -> maxWidth > 840.dp
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                // List Pane
                Box(modifier = Modifier.weight(0.4f)) {
                    RecipeListPane(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRecipeClick = { selectedRecipe = it },
                        onAddClick = {
                            selectedRecipe = null
                            creationName = ""
                            showEditDialog = true
                        }
                    )
                }

                VerticalDivider(modifier = Modifier.width(1.dp))

                // Detail Pane
                Box(modifier = Modifier.weight(0.6f)) {
                    val recipe = selectedRecipe
                    if (recipe != null) {
                        RecipeDetailPane(
                            recipe = recipe,
                            allIngredients = uiState.allIngredients,
                            allRecipes = uiState.groupedRecipes.values.flatten(),
                            allUnits = uiState.allUnits,
                            warnings = uiState.recipeWarnings[recipe.id] ?: emptyList(),
                            onSave = { 
                                viewModel.saveRecipe(it)
                                selectedRecipe = it
                            },
                            onDelete = { 
                                viewModel.deleteRecipe(recipe.id)
                                selectedRecipe = null
                            },
                            onAddIngredient = { viewModel.addIngredient(it) }
                        )
                    } else {
                        EmptyDetailPlaceholder()
                    }
                }
            }
        } else {
            // Mobile View (Single Column)
            RecipeListPane(
                uiState = uiState,
                viewModel = viewModel,
                onRecipeClick = { 
                    selectedRecipe = it
                    showEditDialog = true 
                },
                onAddClick = {
                    selectedRecipe = null
                    creationName = ""
                    showEditDialog = true
                }
            )
        }
    }

    val recipeForDialog = selectedRecipe
    if (showEditDialog) {
        RecipeEditDialog(
            recipe = recipeForDialog,
            initialName = creationName,
            allIngredients = uiState.allIngredients,
            allRecipes = uiState.groupedRecipes.values.flatten(),
            allUnits = uiState.allUnits,
            warnings = recipeForDialog?.let { uiState.recipeWarnings[it.id] } ?: emptyList(),
            onDismiss = { showEditDialog = false },
            onSave = { recipe ->
                viewModel.saveRecipe(recipe)
                showEditDialog = false
            },
            onDelete = if (recipeForDialog != null) { { viewModel.deleteRecipe(recipeForDialog.id) } } else null,
            onAddIngredient = { viewModel.addIngredient(it) }
        )
    }
}

@Composable
fun RecipeListPane(
    uiState: RecipesUiState,
    viewModel: RecipesViewModel,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                ListControlToolbar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    searchPlaceholder = "Search Recipes...",
                    isSortByPrimary = true,
                    primarySortIcon = Icons.Default.SortByAlpha,
                    onToggleSort = { /* No-op */ },
                    onAddClick = onAddClick
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.isCanMakeNowFilterActive,
                        onClick = { viewModel.toggleCanMakeNowFilter() },
                        label = { Text("What can I make now?") },
                        leadingIcon = if (uiState.isCanMakeNowFilterActive) {
                            { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
                HorizontalDivider()
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
                item {
                    CreateNewItemRow(
                        searchQuery = uiState.searchQuery,
                        onClick = onAddClick
                    )
                    HorizontalDivider()
                }
            }

            uiState.groupedRecipes.forEach { (header, recipes) ->
                stickyHeader {
                    ListSectionHeader(
                        text = header
                    )
                }

                items(recipes) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        allIngredients = uiState.allIngredients,
                        allPackages = uiState.allPackages,
                        allBridges = uiState.allBridges,
                        allUnits = uiState.allUnits,
                        warnings = uiState.recipeWarnings[recipe.id] ?: emptyList(),
                        onEditClick = { onRecipeClick(recipe) }
                    )
                    HorizontalDivider()
                }
            }
            
            if (uiState.groupedRecipes.isEmpty() && uiState.isCanMakeNowFilterActive) {
                item {
                    EmptyListMessage(
                        message = "No recipes match your current inventory.",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeDetailPane(
    recipe: Recipe,
    allIngredients: List<Ingredient>,
    allRecipes: List<Recipe>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onSave: (Recipe) -> Unit,
    onDelete: () -> Unit,
    onAddIngredient: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(recipe.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            MpValidationWarning(warnings = warnings)
            Spacer(modifier = Modifier.height(16.dp))
            
            // For now, we reuse the Edit Dialog's content structure but without the dialog scaffold
            var name by remember(recipe) { mutableStateOf(recipe.name) }
            var servingsStr by remember(recipe) { mutableStateOf(recipe.servings.toString()) }
            var description by remember(recipe) { mutableStateOf(recipe.description ?: "") }
            var prepTimeStr by remember(recipe) { mutableStateOf(recipe.prepTimeMinutes.toString()) }
            var cookTimeStr by remember(recipe) { mutableStateOf(recipe.cookTimeMinutes.toString()) }
            var mealType by remember(recipe) { mutableStateOf(recipe.mealType) }
            var currentIngredients by remember(recipe) { mutableStateOf(recipe.ingredients) }
            var instructions by remember(recipe) { mutableStateOf(recipe.instructions) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MpOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpNumericStepper(
                        value = servingsStr.toDoubleOrNull() ?: 4.0,
                        onValueChange = { servingsStr = it.toString() },
                        label = "Servings",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                        Text("Type: ${mealType.name}")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpNumericStepper(
                        value = prepTimeStr.toDoubleOrNull() ?: 0.0,
                        onValueChange = { prepTimeStr = it.toInt().toString() },
                        label = "Prep (min)",
                        modifier = Modifier.weight(1f),
                        step = 5.0
                    )
                    MpNumericStepper(
                        value = cookTimeStr.toDoubleOrNull() ?: 0.0,
                        onValueChange = { cookTimeStr = it.toInt().toString() },
                        label = "Cook (min)",
                        modifier = Modifier.weight(1f),
                        step = 5.0
                    )
                }

                MpOutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                RecipeIngredientsEditor(
                    currentIngredients = currentIngredients,
                    allIngredients = allIngredients,
                    allRecipes = allRecipes,
                    allUnits = allUnits,
                    onUpdate = { currentIngredients = it },
                    onAddIngredient = onAddIngredient
                )

                Text("Instructions", style = MaterialTheme.typography.titleMedium)
                RecipeInstructionsEditor(
                    instructions = instructions,
                    onUpdate = { instructions = it }
                )

                Button(
                    onClick = {
                        onSave(recipe.copy(
                            name = name,
                            description = description.ifBlank { null },
                            servings = servingsStr.toDoubleOrNull() ?: 4.0,
                            instructions = instructions.filter { it.isNotBlank() },
                            mealType = mealType,
                            prepTimeMinutes = prepTimeStr.toIntOrNull() ?: 0,
                            cookTimeMinutes = cookTimeStr.toIntOrNull() ?: 0,
                            ingredients = currentIngredients
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
fun EmptyDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a recipe to view details", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun RecipeRow(
    recipe: Recipe,
    allIngredients: List<Ingredient>,
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onEditClick: () -> Unit
) {
    val ingredientCount = recipe.ingredients.size
    val costCents = PriceCalculator.calculateRecipeCost(recipe, allIngredients.associateBy { it.id }, allPackages, allBridges, allUnits)
    val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "No price data"
    
    val perPersonCents = if (recipe.servings > 0) (costCents / recipe.servings).toLong() else costCents
    val perPersonStr = if (costCents > 0) " ($${String.format("%.2f", perPersonCents / 100.0)}/person)" else ""

    val subtitle = "Serves ${recipe.servings} • $ingredientCount ingredients • Total: $costStr$perPersonStr"

    ExpandableListItem(
        title = recipe.name,
        subtitle = subtitle,
        trailingIcon = if (warnings.isNotEmpty()) {
            { Icon(Icons.Default.Warning, "Data Warnings", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
        } else null,
        onEditClick = onEditClick
    ) {
        if (warnings.isNotEmpty()) {
            Text(
                "⚠️ ${warnings.size} data issues (e.g., missing prices/conversions)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (recipe.description != null) {
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            "Time: Prep ${recipe.prepTimeMinutes}m / Cook ${recipe.cookTimeMinutes}m",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))

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
fun RecipeEditDialog(
    recipe: Recipe?,
    initialName: String,
    allIngredients: List<Ingredient>,
    allRecipes: List<Recipe>, // Added
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddIngredient: (String) -> Unit
) {
    var name by remember { mutableStateOf(recipe?.name ?: initialName) }
    var servingsStr by remember { mutableStateOf(recipe?.servings?.toString() ?: "4.0") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var instructionsText by remember { mutableStateOf(recipe?.instructions?.joinToString("\n") ?: "") }
    
    var prepTimeStr by remember { mutableStateOf(recipe?.prepTimeMinutes?.toString() ?: "0") }
    var cookTimeStr by remember { mutableStateOf(recipe?.cookTimeMinutes?.toString() ?: "0") }
    
    // Default MealType
    var mealType by remember { mutableStateOf(recipe?.mealType ?: RecipeMealType.Dinner) }

    val recipeId = remember { recipe?.id ?: Uuid.random() }
    var currentIngredients by remember { mutableStateOf(recipe?.ingredients ?: emptyList()) }
    var instructions by remember { mutableStateOf(recipe?.instructions ?: emptyList()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Ingredients", "Instructions")

    MpEditDialogScaffold(
        title = if (recipe == null) "New Recipe" else "Edit Recipe",
        onDismiss = onDismiss,
        onSave = {
            val finalRecipe = Recipe(
                id = recipeId,
                name = name,
                description = description.ifBlank { null },
                servings = servingsStr.toDoubleOrNull() ?: 4.0,
                instructions = instructions.filter { it.isNotBlank() },
                mealType = mealType,
                prepTimeMinutes = prepTimeStr.toIntOrNull() ?: 0,
                cookTimeMinutes = cookTimeStr.toIntOrNull() ?: 0,
                ingredients = currentIngredients
            )
            onSave(finalRecipe)
        },
        saveEnabled = name.isNotBlank(),
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
        Column {
            MpValidationWarning(warnings = warnings)

            when (selectedTabIndex) {
                0 -> { // General
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MpOutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MpNumericStepper(
                                value = servingsStr.toDoubleOrNull() ?: 4.0,
                                onValueChange = { servingsStr = it.toString() },
                                label = "Servings",
                                modifier = Modifier.weight(1f)
                            )
                            // Meal Type - Simplified selector
                            Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                                Text("Type: ${mealType.name}")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MpNumericStepper(
                                value = prepTimeStr.toDoubleOrNull() ?: 0.0,
                                onValueChange = { prepTimeStr = it.toInt().toString() },
                                label = "Prep (min)",
                                modifier = Modifier.weight(1f),
                                step = 5.0
                            )
                            MpNumericStepper(
                                value = cookTimeStr.toDoubleOrNull() ?: 0.0,
                                onValueChange = { cookTimeStr = it.toInt().toString() },
                                label = "Cook (min)",
                                modifier = Modifier.weight(1f),
                                step = 5.0
                            )
                        }

                        MpOutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                1 -> { // Ingredients
                    RecipeIngredientsEditor(
                        currentIngredients = currentIngredients,
                        allIngredients = allIngredients,
                        allRecipes = allRecipes, // Added
                        allUnits = allUnits,
                        onUpdate = { currentIngredients = it },
                        onAddIngredient = onAddIngredient
                    )
                }

                2 -> { // Instructions
                    RecipeInstructionsEditor(
                        instructions = instructions,
                        onUpdate = { instructions = it }
                    )
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium)
                    MpOutlinedTextField(
                        value = step,
                        onValueChange = { newStep ->
                            val newList = instructions.toMutableList()
                            newList[index] = newStep
                            onUpdate(newList)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Step ${index + 1}") }
                    )
                    IconButton(onClick = {
                        val newList = instructions.toMutableList()
                        newList.removeAt(index)
                        onUpdate(newList)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
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
    currentIngredients: List<RecipeIngredient>,
    allIngredients: List<Ingredient>,
    allRecipes: List<Recipe>, // Added
    allUnits: List<UnitModel>,
    onUpdate: (List<RecipeIngredient>) -> Unit,
    onAddIngredient: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Ingredient") } // Ingredient or Recipe
    var selectedItemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var selectedUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }

    Column {
        if (currentIngredients.isEmpty()) {
            Text("No items added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            Column {
                currentIngredients.forEach { ri ->
                    val name = if (ri.subRecipeId != null) {
                        allRecipes.find { it.id == ri.subRecipeId }?.name ?: "Unknown Recipe"
                    } else {
                        allIngredients.find { it.id == ri.ingredientId }?.name ?: "Unknown Ingredient"
                    }
                    val uName = allUnits.find { it.id == ri.unitId }?.abbreviation ?: "?"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${if (ri.subRecipeId != null) "[Recipe] " else ""}$name: ${ri.quantity} $uName", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdate(currentIngredients - ri) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdding) {
            Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Component")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add Ingredient or Sub-Recipe", style = MaterialTheme.typography.labelMedium)

                    Row {
                        FilterChip(
                            selected = selectedType == "Ingredient",
                            onClick = { selectedType = "Ingredient"; selectedItemName = "" },
                            label = { Text("Ingredient") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedType == "Recipe",
                            onClick = { selectedType = "Recipe"; selectedItemName = "" },
                            label = { Text("Recipe") })
                    }

                    SearchableDropdown(
                        label = if (selectedType == "Ingredient") "Ingredient" else "Recipe",
                        options = if (selectedType == "Ingredient") allIngredients.map { it.name } else allRecipes.map { it.name },
                        selectedOption = selectedItemName,
                        onOptionSelected = { selectedItemName = it },
                        onAddOption = { if (selectedType == "Ingredient") onAddIngredient(it) },
                        onDeleteOption = { },
                        deleteWarningMessage = ""
                    )

                    Row {
                        MpOutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
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
                                deleteWarningMessage = ""
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val item = if (selectedType == "Ingredient") {
                                    allIngredients.find { it.name == selectedItemName }
                                } else {
                                    allRecipes.find { it.name == selectedItemName }
                                }
                                val unit = allUnits.find { it.abbreviation == selectedUnitName }
                                val qty = quantityStr.toDoubleOrNull()
                                
                                if (item != null && qty != null && unit != null) {
                                    val newRi = if (selectedType == "Ingredient") {
                                        RecipeIngredient(ingredientId = (item as Ingredient).id, quantity = qty, unitId = unit.id)
                                    } else {
                                        RecipeIngredient(subRecipeId = (item as Recipe).id, quantity = qty, unitId = unit.id)
                                    }
                                    onUpdate(currentIngredients + newRi)
                                    isAdding = false
                                    selectedItemName = ""
                                    quantityStr = ""
                                }
                            },
                            enabled = selectedItemName.isNotBlank() && quantityStr.isNotBlank() && selectedUnitName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
