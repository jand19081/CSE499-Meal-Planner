@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.meals

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.clickable
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun MealsView() {
    val viewModel = viewModel { MealsViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var creationName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ListControlToolbar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchPlaceholder = "Search Meals...",
                isSortByPrimary = true,
                primarySortIcon = Icons.Default.SortByAlpha,
                onToggleSort = { /* No-op */ },
                onAddClick = {
                    selectedMeal = null
                    creationName = ""
                    showEditDialog = true
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Create New Option
            if (uiState.searchQuery.isNotBlank() && uiState.groupedMeals.values.flatten().none { it.name.equals(uiState.searchQuery, ignoreCase = true) }) {
                item {
                    CreateNewItemRow(
                        searchQuery = uiState.searchQuery,
                        onClick = {
                            selectedMeal = null
                            creationName = uiState.searchQuery
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }

            uiState.groupedMeals.forEach { (header, meals) ->
                stickyHeader {
                    ListSectionHeader(text = header)
                }

                items(meals) { meal ->
                    MealRow(
                        meal = meal,
                        components = uiState.allComponents.filter { it.mealId == meal.id },
                        allRecipes = uiState.allRecipes,
                        allIngredients = uiState.allIngredients,
                        allRecipeIngredients = uiState.allRecipeIngredients,
                        onEditClick = {
                            selectedMeal = meal
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showEditDialog) {
        MealEditDialog(
            meal = selectedMeal,
            initialName = creationName,
            allRecipes = uiState.allRecipes,
            allIngredients = uiState.allIngredients,
            currentComponents = if (selectedMeal != null) uiState.allComponents.filter { it.mealId == selectedMeal!!.id } else emptyList(),
            onDismiss = { showEditDialog = false },
            onSave = { meal, components ->
                viewModel.saveMeal(meal, components)
                showEditDialog = false
            },
            onDelete = if (selectedMeal != null) { { viewModel.deleteMeal(selectedMeal!!) } } else null
        )
    }
}

@Composable
fun MealRow(
    meal: Meal,
    components: List<MealComponent>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allRecipeIngredients: List<RecipeIngredient>,
    onEditClick: () -> Unit
) {
    val names = components.mapNotNull { item ->
        if (item.recipeId != null) {
            allRecipes.find { it.id == item.recipeId }?.name
        } else {
            allIngredients.find { it.id == item.ingredientId }?.name
        }
    }
    
    val costCents = calculateMealCost(meal, components, allRecipes, allIngredients, allRecipeIngredients)
    val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "No price data"

    val subtitle = if (names.isEmpty()) "Empty Meal" else "${names.joinToString(", ")} • Est. Cost: $costStr"

    ExpandableListItem(
        title = meal.name,
        subtitle = subtitle,
        onEditClick = onEditClick
    ) {
        if (meal.description != null) {
            Text(meal.description, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Contents:", style = MaterialTheme.typography.labelMedium)
        if (names.isEmpty()) {
            Text("No items.", style = MaterialTheme.typography.bodySmall)
        } else {
            names.forEach { name ->
                Text("• $name", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MealEditDialog(
    meal: Meal?,
    initialName: String,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    currentComponents: List<MealComponent>,
    onDismiss: () -> Unit,
    onSave: (Meal, List<MealComponent>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(meal?.name ?: initialName) }
    var description by remember { mutableStateOf(meal?.description ?: "") }
    
    val mealId = remember { meal?.id ?: Uuid.random() }
    var components by remember { mutableStateOf(currentComponents) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Contents")

    MpEditDialogScaffold(
        title = if (meal == null) "New Meal" else "Edit Meal",
        onDismiss = onDismiss,
        onSave = {
            val finalMeal = Meal(
                id = mealId,
                name = name,
                description = description.ifBlank { null }
            )
            onSave(finalMeal, components)
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
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            1 -> { // Contents
                MealContentsEditor(
                    mealId = mealId,
                    currentComponents = components,
                    allRecipes = allRecipes,
                    allIngredients = allIngredients,
                    onUpdate = { components = it }
                )
            }
        }
    }
}

@Composable
fun MealContentsEditor(
    mealId: Uuid,
    currentComponents: List<MealComponent>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    onUpdate: (List<MealComponent>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    
    var selectedType by remember { mutableStateOf("Recipe") }
    var selectedItemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(MeasureUnit.EACH) }
    var unitExpanded by remember { mutableStateOf(false) }

    Column {
        if (currentComponents.isEmpty()) {
            Text("No items added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                items(currentComponents) { comp ->
                    val name = if (comp.recipeId != null) {
                        allRecipes.find { it.id == comp.recipeId }?.name ?: "Unknown Recipe"
                    } else {
                        allIngredients.find { it.id == comp.ingredientId }?.name ?: "Unknown Ingredient"
                    }
                    val qtyStr = if (comp.quantity != null) "${comp.quantity}" else ""
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$name $qtyStr", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdate(currentComponents - comp) }, modifier = Modifier.size(24.dp)) {
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
                Text("Add Item")
            }
        } else {
            MpCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Item", style = MaterialTheme.typography.labelMedium)

                    Row {
                        FilterChip(selected = selectedType == "Recipe", onClick = { selectedType = "Recipe"; selectedItemName = "" }, label = { Text("Recipe") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = selectedType == "Ingredient", onClick = { selectedType = "Ingredient"; selectedItemName = "" }, label = { Text("Ingredient") })
                    }

                    SearchableDropdown(
                        label = if (selectedType == "Recipe") "Recipe" else "Ingredient",
                        options = if (selectedType == "Recipe") allRecipes.map { it.name } else allIngredients.map { it.name },
                        selectedOption = selectedItemName,
                        onOptionSelected = { selectedItemName = it },
                        onAddOption = { },
                        onDeleteOption = { },
                        deleteWarningMessage = ""
                    )

                    if (selectedType == "Ingredient") {
                        MeasureInputRow(
                            quantity = quantityStr,
                            onQuantityChange = { quantityStr = it },
                            unit = unit,
                            onUnitChange = { unit = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        MpTextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        MpButton(
                            onClick = {
                                if (selectedType == "Recipe") {
                                    val r = allRecipes.find { it.name == selectedItemName }
                                    if (r != null) {
                                        val comp = MealComponent(mealId = mealId, recipeId = r.id)
                                        onUpdate(currentComponents + comp)
                                        isAdding = false
                                        selectedItemName = ""
                                    }
                                } else {
                                    val ing = allIngredients.find { it.name == selectedItemName }
                                    val qty = quantityStr.toDoubleOrNull()
                                    if (ing != null && qty != null) {
                                        val comp = MealComponent(
                                            mealId = mealId, 
                                            ingredientId = ing.id,
                                            quantity = Measure(qty, unit)
                                        )
                                        onUpdate(currentComponents + comp)
                                        isAdding = false
                                        selectedItemName = ""
                                        quantityStr = ""
                                    }
                                }
                            },
                            enabled = selectedItemName.isNotBlank() && (selectedType == "Recipe" || quantityStr.isNotBlank())
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
