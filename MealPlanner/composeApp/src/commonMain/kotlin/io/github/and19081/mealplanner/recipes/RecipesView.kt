@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SortByAlpha
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
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

@Composable
fun RecipesView() {
    val viewModel = viewModel { RecipesViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
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
            Column {
                ListControlToolbar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    searchPlaceholder = "Search Recipes...",
                    isSortByPrimary = true,
                    primarySortIcon = Icons.Default.SortByAlpha,
                    onToggleSort = { /* No-op */ },
                    onAddClick = {
                        selectedRecipe = null
                        creationName = ""
                        showEditDialog = true
                    }
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
                        onClick = {
                            selectedRecipe = null
                            creationName = uiState.searchQuery
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }

            uiState.groupedRecipes.forEach { (header, recipes) ->
                stickyHeader {
                    ListSectionHeader(text = header)
                }

                items(recipes) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        allIngredients = uiState.allIngredients,
                        allPackages = uiState.allPackages,
                        allBridges = uiState.allBridges,
                        allUnits = uiState.allUnits,
                        onEditClick = {
                            selectedRecipe = recipe
                            showEditDialog = true
                        }
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

    if (showEditDialog) {
        RecipeEditDialog(
            recipe = selectedRecipe,
            initialName = creationName,
            allIngredients = uiState.allIngredients,
            allUnits = uiState.allUnits,
            onDismiss = { showEditDialog = false },
            onSave = { recipe ->
                viewModel.saveRecipe(recipe)
                showEditDialog = false
            },
            onDelete = if (selectedRecipe != null) { { viewModel.deleteRecipe(selectedRecipe!!.id) } } else null
        )
    }
}

@Composable
fun RecipeRow(
    recipe: Recipe,
    allIngredients: List<Ingredient>,
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
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
        onEditClick = onEditClick
    ) {
        if (recipe.description != null) {
            Text(recipe.description, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Text("Time: Prep ${recipe.prepTimeMinutes}m / Cook ${recipe.cookTimeMinutes}m", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))

        Text("Instructions:", style = MaterialTheme.typography.labelMedium)
        if (recipe.instructions.isEmpty()) {
            Text("No instructions.", style = MaterialTheme.typography.bodySmall)
        } else {
            recipe.instructions.forEachIndexed {
                idx, line ->
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
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit,
    onDelete: (() -> Unit)? = null
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

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Ingredients")

    MpEditDialogScaffold(
        title = if (recipe == null) "New Recipe" else "Edit Recipe",
        onDismiss = onDismiss,
        onSave = {
            val finalRecipe = Recipe(
                id = recipeId,
                name = name,
                description = description.ifBlank { null },
                servings = servingsStr.toDoubleOrNull() ?: 4.0,
                instructions = instructionsText.lines().filter { it.isNotBlank() },
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
                        MpOutlinedTextField(
                            value = servingsStr,
                            onValueChange = { servingsStr = it },
                            label = { Text("Servings") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        // Meal Type - Simplified selector
                        Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                            // Ideally a dropdown, for now text or simple logic.
                            // Assuming default is Dinner, no UI to change yet to save complexity unless asked.
                            Text("Type: ${mealType.name}")
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MpOutlinedTextField(
                            value = prepTimeStr,
                            onValueChange = { prepTimeStr = it },
                            label = { Text("Prep (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        MpOutlinedTextField(
                            value = cookTimeStr,
                            onValueChange = { cookTimeStr = it },
                            label = { Text("Cook (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MpOutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MpOutlinedTextField(
                        value = instructionsText,
                        onValueChange = { instructionsText = it },
                        label = { Text("Instructions (one per line)") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            1 -> { // Ingredients
                RecipeIngredientsEditor(
                    currentIngredients = currentIngredients,
                    allIngredients = allIngredients,
                    allUnits = allUnits,
                    onUpdate = { currentIngredients = it }
                )
            }
        }
    }
}

@Composable
fun RecipeIngredientsEditor(
    currentIngredients: List<RecipeIngredient>,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onUpdate: (List<RecipeIngredient>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var selectedIngredientName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var selectedUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }

    Column {
        if (currentIngredients.isEmpty()) {
            Text("No ingredients added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                items(currentIngredients) { ri ->
                    val ing = allIngredients.find { it.id == ri.ingredientId }
                    val uName = allUnits.find { it.id == ri.unitId }?.abbreviation ?: "?"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ing?.name ?: "Unknown"}: ${ri.quantity} $uName", style = MaterialTheme.typography.bodySmall)
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
            MpButton(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Ingredient")
            }
        } else {
            MpCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Ingredient Link", style = MaterialTheme.typography.labelMedium)

                    SearchableDropdown(
                        label = "Ingredient",
                        options = allIngredients.map { it.name },
                        selectedOption = selectedIngredientName,
                        onOptionSelected = { selectedIngredientName = it },
                        onAddOption = { },
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

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val ing = allIngredients.find { it.name == selectedIngredientName }
                                val unit = allUnits.find { it.abbreviation == selectedUnitName }
                                val qty = quantityStr.toDoubleOrNull()
                                if (ing != null && qty != null && unit != null) {
                                    val newRi = RecipeIngredient(
                                        ingredientId = ing.id,
                                        quantity = qty,
                                        unitId = unit.id
                                    )
                                    onUpdate(currentIngredients + newRi)
                                    isAdding = false
                                    selectedIngredientName = ""
                                    quantityStr = ""
                                }
                            },
                            enabled = selectedIngredientName.isNotBlank() && quantityStr.isNotBlank() && selectedUnitName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
