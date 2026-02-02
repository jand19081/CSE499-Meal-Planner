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
import io.github.and19081.mealplanner.ingredients.Ingredient
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
                        allRecipeIngredients = uiState.allRecipeIngredients,
                        allIngredients = uiState.allIngredients,
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
            initialRecipeIngredients = uiState.allRecipeIngredients.filter { it.recipeId == (selectedRecipe?.id ?: Uuid.NIL) },
            onDismiss = { showEditDialog = false },
            onSave = { recipe, ingredients ->
                viewModel.saveRecipe(recipe, ingredients)
                showEditDialog = false
            },
            onDelete = if (selectedRecipe != null) { { viewModel.deleteRecipe(selectedRecipe!!.id) } } else null
        )
    }
}

@Composable
fun RecipeRow(
    recipe: Recipe,
    allRecipeIngredients: List<RecipeIngredient>,
    allIngredients: List<Ingredient>,
    onEditClick: () -> Unit
) {
    val ingredientCount = allRecipeIngredients.count { it.recipeId == recipe.id }
    val costCents = calculateRecipeCost(recipe, allIngredients, allRecipeIngredients)
    val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "No price data"
    
    val perPersonCents = if (recipe.baseServings > 0) (costCents / recipe.baseServings).toLong() else costCents
    val perPersonStr = if (costCents > 0) " ($${String.format("%.2f", perPersonCents / 100.0)}/person)" else ""

    val subtitle = "Serves ${recipe.baseServings} • $ingredientCount ingredients • Total: $costStr$perPersonStr"

    ExpandableListItem(
        title = recipe.name,
        subtitle = subtitle,
        onEditClick = onEditClick
    ) {
        if (recipe.description != null) {
            Text(recipe.description, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(8.dp))
        }

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
    initialRecipeIngredients: List<RecipeIngredient>,
    onDismiss: () -> Unit,
    onSave: (Recipe, List<RecipeIngredient>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(recipe?.name ?: initialName) }
    var servingsStr by remember { mutableStateOf(recipe?.baseServings?.toString() ?: "4.0") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var instructionsText by remember { mutableStateOf(recipe?.instructions?.joinToString("\n") ?: "") }

    val recipeId = remember { recipe?.id ?: Uuid.random() }
    var currentIngredients by remember { mutableStateOf(initialRecipeIngredients) }

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
                baseServings = servingsStr.toDoubleOrNull() ?: 4.0,
                instructions = instructionsText.lines().filter { it.isNotBlank() }
            )
            onSave(finalRecipe, currentIngredients)
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
                    MpOutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text("Base Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    recipeId = recipeId,
                    currentIngredients = currentIngredients,
                    allIngredients = allIngredients,
                    onUpdate = { currentIngredients = it }
                )
            }
        }
    }
}

@Composable
fun RecipeIngredientsEditor(
    recipeId: Uuid,
    currentIngredients: List<RecipeIngredient>,
    allIngredients: List<Ingredient>,
    onUpdate: (List<RecipeIngredient>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var selectedIngredientName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(MeasureUnit.EACH) }
    var unitExpanded by remember { mutableStateOf(false) }

    Column {
        if (currentIngredients.isEmpty()) {
            Text("No ingredients added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                items(currentIngredients) { ri ->
                    val ing = allIngredients.find { it.id == ri.ingredientId }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ing?.name ?: "Unknown"}: ${ri.quantity}", style = MaterialTheme.typography.bodySmall)
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

                    MeasureInputRow(
                        quantity = quantityStr,
                        onQuantityChange = { quantityStr = it },
                        unit = unit,
                        onUnitChange = { unit = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                val ing = allIngredients.find { it.name == selectedIngredientName }
                                val qty = quantityStr.toDoubleOrNull()
                                if (ing != null && qty != null) {
                                    val newRi = RecipeIngredient(
                                        recipeId = recipeId,
                                        ingredientId = ing.id,
                                        quantity = Measure(qty, unit)
                                    )
                                    onUpdate(currentIngredients + newRi)
                                    isAdding = false
                                    selectedIngredientName = ""
                                    quantityStr = ""
                                }
                            },
                            enabled = selectedIngredientName.isNotBlank() && quantityStr.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
