@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.meals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import io.github.and19081.mealplanner.uicomponents.ExpandableListItem
import io.github.and19081.mealplanner.uicomponents.ListControlToolbar
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpEditDialogScaffold
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun MealsView(
    viewModel: MealsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMeal by remember { mutableStateOf<PrePlannedMeal?>(null) }
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
                        allRecipes = uiState.allRecipes,
                        allIngredients = uiState.allIngredients,
                        allPackages = uiState.allPackages,
                        allBridges = uiState.allBridges,
                        allUnits = uiState.allUnits,
                        warnings = uiState.mealWarnings[meal.id] ?: emptyList(),
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
            allUnits = uiState.allUnits,
            warnings = selectedMeal?.let { uiState.mealWarnings[it.id] } ?: emptyList(),
            onDismiss = { showEditDialog = false },
            onSave = { meal ->
                viewModel.saveMeal(meal)
                showEditDialog = false
            },
            onDelete = if (selectedMeal != null) { { viewModel.deleteMeal(selectedMeal!!) } } else null
        )
    }
}

@Composable
fun MealRow(
    meal: PrePlannedMeal,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allPackages: List<Package>,
    allBridges: List<BridgeConversion>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onEditClick: () -> Unit
) {
    val recipeNames = meal.recipes.mapNotNull { id -> allRecipes.find { it.id == id }?.name }
    val ingredientNames = meal.independentIngredients.mapNotNull { item -> allIngredients.find { it.id == item.ingredientId }?.name }
    
    val allNames = recipeNames + ingredientNames
    
    val costCents = PriceCalculator.calculateMealCost(
        meal = meal, 
        recipesMap = allRecipes.associateBy { it.id },
        ingredientsMap = allIngredients.associateBy { it.id },
        allPackages = allPackages,
        allBridges = allBridges,
        allUnits = allUnits
    )
    val costStr = if (costCents > 0) "$${String.format("%.2f", costCents / 100.0)}" else "No price data"

    val subtitle = if (allNames.isEmpty()) "Empty Meal" else "${allNames.joinToString(", ")} • Est. Cost: $costStr"

    ExpandableListItem(
        title = meal.name,
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

        Text("Contents:", style = MaterialTheme.typography.labelMedium)
        if (allNames.isEmpty()) {
            Text("No items.", style = MaterialTheme.typography.bodySmall)
        } else {
            allNames.forEach { name ->
                Text("• $name", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MealEditDialog(
    meal: PrePlannedMeal?,
    initialName: String,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    warnings: List<DataWarning>,
    onDismiss: () -> Unit,
    onSave: (PrePlannedMeal) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(meal?.name ?: initialName) }
    
    val mealId = remember { meal?.id ?: Uuid.random() }
    
    var recipes by remember { mutableStateOf(meal?.recipes ?: emptyList()) }
    var independentIngredients by remember { mutableStateOf(meal?.independentIngredients ?: emptyList()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Contents")

    MpEditDialogScaffold(
        title = if (meal == null) "New Meal" else "Edit Meal",
        onDismiss = onDismiss,
        onSave = {
            val finalMeal = PrePlannedMeal(
                id = mealId,
                name = name,
                recipes = recipes,
                independentIngredients = independentIngredients
            )
            onSave(finalMeal)
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
                    }
                }

                1 -> { // Contents
                    MealContentsEditor(
                        currentRecipes = recipes,
                        currentIngredients = independentIngredients,
                        allRecipes = allRecipes,
                        allIngredients = allIngredients,
                        allUnits = allUnits,
                        onUpdateRecipes = { recipes = it },
                        onUpdateIngredients = { independentIngredients = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MealContentsEditor(
    currentRecipes: List<Uuid>,
    currentIngredients: List<MealIngredient>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onUpdateRecipes: (List<Uuid>) -> Unit,
    onUpdateIngredients: (List<MealIngredient>) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    
    var selectedType by remember { mutableStateOf("Recipe") }
    var selectedItemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var selectedUnitName by remember { mutableStateOf(allUnits.firstOrNull()?.abbreviation ?: "") }

    Column {
        if (currentRecipes.isEmpty() && currentIngredients.isEmpty()) {
            Text("No items added.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            Column {
                // Recipes
                currentRecipes.forEach { rId ->
                    val name = allRecipes.find { it.id == rId }?.name ?: "Unknown Recipe"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recipe: $name", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdateRecipes(currentRecipes - rId) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
                
                // Ingredients
                currentIngredients.forEach { item ->
                    val name = allIngredients.find { it.id == item.ingredientId }?.name ?: "Unknown Ingredient"
                    val uName = allUnits.find { it.id == item.unitId }?.abbreviation ?: "?"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ing: $name ${item.quantity} $uName", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onUpdateIngredients(currentIngredients - item) }, modifier = Modifier.size(24.dp)) {
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
                Text("Add Item")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add Item", style = MaterialTheme.typography.labelMedium)

                    Row {
                        FilterChip(
                            selected = selectedType == "Recipe",
                            onClick = { selectedType = "Recipe"; selectedItemName = "" },
                            label = { Text("Recipe") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedType == "Ingredient",
                            onClick = { selectedType = "Ingredient"; selectedItemName = "" },
                            label = { Text("Ingredient") })
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
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (selectedType == "Recipe") {
                                    val r = allRecipes.find { it.name == selectedItemName }
                                    if (r != null) {
                                        onUpdateRecipes(currentRecipes + r.id)
                                        isAdding = false
                                        selectedItemName = ""
                                    }
                                } else {
                                    val ing = allIngredients.find { it.name == selectedItemName }
                                    val unit = allUnits.find { it.abbreviation == selectedUnitName }
                                    val qty = quantityStr.toDoubleOrNull()
                                    if (ing != null && qty != null && unit != null) {
                                        val comp = MealIngredient(
                                            ingredientId = ing.id,
                                            quantity = qty,
                                            unitId = unit.id
                                        )
                                        onUpdateIngredients(currentIngredients + comp)
                                        isAdding = false
                                        selectedItemName = ""
                                        quantityStr = ""
                                    }
                                }
                            },
                            enabled = selectedItemName.isNotBlank() && (selectedType == "Recipe" || (quantityStr.isNotBlank() && selectedUnitName.isNotBlank()))
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
