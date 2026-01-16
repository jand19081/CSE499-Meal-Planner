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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMeal = null
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

            uiState.groupedMeals.forEach { (header, meals) ->
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

                items(meals) { meal ->
                    MealRow(
                        meal = meal,
                        components = uiState.allComponents.filter { it.mealId == meal.id },
                        allRecipes = uiState.allRecipes,
                        allIngredients = uiState.allIngredients,
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
    onEditClick: () -> Unit
) {
    // Resolve names
    val names = components.mapNotNull { item ->
        if (item.recipeId != null) {
            allRecipes.find { it.id == item.recipeId }?.name
        } else {
            allIngredients.find { it.id == item.ingredientId }?.name
        }
    }

    val subtitle = if (names.isEmpty()) "Empty Meal" else names.joinToString(", ")

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (meal == null) "New Meal" else "Edit Meal") },
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
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (onDelete != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        onDelete() 
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Delete Meal")
                                }
                            }
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMeal = Meal(
                        id = mealId,
                        name = name,
                        description = description.ifBlank { null }
                    )
                    onSave(finalMeal, components)
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
fun MealContentsEditor(
    mealId: Uuid,
    currentComponents: List<MealComponent>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    onUpdate: (List<MealComponent>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    
    // Adding State
    var selectedType by remember { mutableStateOf("Recipe") } // Recipe or Ingredient
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
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdding) {
            Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Item")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Item", style = MaterialTheme.typography.labelMedium)

                    // Type Selector
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
                        onAddOption = { /* No-op */ },
                        onDeleteOption = { /* No-op */ },
                        deleteWarningMessage = ""
                    )

                    // Quantity only needed for Ingredients (mostly)
                    if (selectedType == "Ingredient") {
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
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Button(
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
