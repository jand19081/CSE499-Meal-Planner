@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun RecipesView() {
    val viewModel = viewModel { RecipesViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var creationName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Recipes...",
                isSortByPrimary = true, // Always alpha for now
                primarySortIcon = Icons.Default.SortByAlpha,
                onToggleSort = { /* No-op or toggle ASC/DESC later */ },
                onAddClick = {
                    selectedRecipe = null
                    creationName = ""
                    showEditDialog = true
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(it),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Create New Option
            if (uiState.searchQuery.isNotBlank() && !uiState.doesExactMatchExist) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRecipe = null
                                creationName = uiState.searchQuery
                                showEditDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Create '${uiState.searchQuery}'",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            uiState.groupedRecipes.forEach { (header, recipes) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = header,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(recipes) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        onEditClick = {
                            selectedRecipe = recipe
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showEditDialog) {
        RecipeEditDialog(
            recipe = selectedRecipe,
            initialName = creationName,
            allIngredients = uiState.allIngredients,
            onDismiss = { showEditDialog = false },
            onSave = { recipe, ingredients ->
                viewModel.saveRecipe(recipe)
                // Update ingredients in repo manually for now (should be in VM but this works for MVP)
                MealPlannerRepository.recipeIngredients.removeAll { it.recipeId == recipe.id }
                MealPlannerRepository.recipeIngredients.addAll(ingredients)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun RecipeRow(
    recipe: Recipe,
    onEditClick: () -> Unit
) {
    // Count ingredients
    val ingredientCount = MealPlannerRepository.recipeIngredients.count { it.recipeId == recipe.id }
    val subtitle = "Serves ${recipe.baseServings} • $ingredientCount ingredients"

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
    onDismiss: () -> Unit,
    onSave: (Recipe, List<RecipeIngredient>) -> Unit
) {
    var name by remember { mutableStateOf(recipe?.name ?: initialName) }
    var servingsStr by remember { mutableStateOf(recipe?.baseServings?.toString() ?: "4.0") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var instructionsText by remember { mutableStateOf(recipe?.instructions?.joinToString("\n") ?: "") }

    // Ingredients State
    // We need a temp ID for new recipes to link ingredients
    val recipeId = remember { recipe?.id ?: Uuid.random() }
    var currentIngredients by remember {
        mutableStateOf(
            if (recipe != null) {
                MealPlannerRepository.recipeIngredients.filter { it.recipeId == recipe.id }
            } else {
                emptyList()
            }
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Ingredients")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipe == null) "New Recipe" else "Edit Recipe") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    0 -> { // General
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = servingsStr,
                                onValueChange = { servingsStr = it },
                                label = { Text("Base Servings") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRecipe = Recipe(
                        id = recipeId,
                        name = name,
                        description = description.ifBlank { null },
                        baseServings = servingsStr.toDoubleOrNull() ?: 4.0,
                        instructions = instructionsText.lines().filter { it.isNotBlank() }
                    )
                    onSave(finalRecipe, currentIngredients)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdding) {
            Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Ingredient")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Ingredient Link", style = MaterialTheme.typography.labelMedium)

                    SearchableDropdown(
                        label = "Ingredient",
                        options = allIngredients.map { it.name },
                        selectedOption = selectedIngredientName,
                        onOptionSelected = { selectedIngredientName = it },
                        onAddOption = { /* No-op, must exist */ },
                        onDeleteOption = { /* No-op */ },
                        deleteWarningMessage = ""
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { unitExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(unit.name)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                MeasureUnit.entries.forEach { u ->
                                    DropdownMenuItem(text = { Text(u.name) }, onClick = { unit = u; unitExpanded = false })
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Button(
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
