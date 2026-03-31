@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.model.Package
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A unified searchable dropdown component for selecting meal sources.
 * Supports selecting Meals, Recipes, Ingredients, or Restaurant options.
 * Displays conditional fields based on the selected meal source type.
 *
 * @param selectedSource The currently selected meal source
 * @param onSourceSelected Callback when a source is selected
 * @param availableMeals List of available pre-planned meals
 * @param availableRecipes List of available standalone recipes
 * @param availableIngredients List of available pantry ingredients
 * @param availableRestaurants List of available restaurants
 * @param selectedQuantity The selected quantity (for ingredients)
 * @param onQuantityChange Callback when quantity changes
 * @param selectedUnit The selected unit (for ingredients)
 * @param onUnitChange Callback when unit changes
 * @param anticipatedCost The anticipated cost (for restaurants)
 * @param onAnticipatedCostChange Callback when cost changes
 * @param label The label for the main selection field
 * @param enabled Whether the component is enabled
 * @param modifier The modifier to apply to the root layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun MealSourcePicker(
    selectedSource: MealSource?,
    onSourceSelected: (MealSource) -> Unit,
    availableMeals: List<FoodItem>,
    availableRecipes: List<FoodItem>,
    availableIngredients: List<FoodItem>,
    availableRestaurants: List<Package> = emptyList(),
    selectedQuantity: Double = 0.0,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: UnitModel?,
    onUnitChange: (UnitModel) -> Unit,
    anticipatedCost: String = "",
    onAnticipatedCostChange: (String) -> Unit,
    label: String = "Select Meal Source",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSourceType by remember { mutableStateOf<SourceType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val filteredMeals by remember(availableMeals, searchQuery, selectedTab) {
        derivedStateOf {
            availableMeals.filter {
                selectedTab == 0 && it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredRecipes by remember(availableRecipes, searchQuery, selectedTab) {
        derivedStateOf {
            availableRecipes.filter {
                selectedTab == 1 && it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredIngredients by remember(availableIngredients, searchQuery, selectedTab) {
        derivedStateOf {
            availableIngredients.filter {
                selectedTab == 2 && it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredRestaurants by remember(availableRestaurants, searchQuery, selectedTab) {
        derivedStateOf {
            availableRestaurants.filter {
                selectedTab == 3 && it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier) {
        // Main selection field
        Box {
            OutlinedTextField(
                value = selectedSource?.let { source ->
                    when (source) {
                        is MealSource.PrePlannedMeal -> {
                            "Meal: ${availableMeals.find { it.id == source.id }?.name ?: "Unknown"}"
                        }

                        is MealSource.StandaloneRecipe -> {
                            "Recipe: ${availableRecipes.find { it.id == source.id }?.name ?: "Unknown"}"
                        }

                        is MealSource.StandaloneIngredient -> {
                            "Ingredient: ${availableIngredients.find { it.id == source.id }?.name ?: "Unknown"}"
                        }

                        is MealSource.Restaurant -> {
                            "Restaurant: ${availableRestaurants.find { it.id == source.restaurantId }?.name ?: "Unknown"}"
                        }
                    }
                } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text("Tap to select") },
                trailingIcon = {
                    IconButton(onClick = { if (enabled) expanded = true }) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select meal source"
                        )
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                }
            ) {
                // Source type tabs
                if (searchQuery.isEmpty()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Meals") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Recipes") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Ingredients") }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Restaurants") }
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> {
                            if (filteredMeals.isEmpty()) {
                                Text(
                                    "No meals available",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn {
                                    items(filteredMeals) { meal ->
                                        SelectionOptionItem(
                                            name = meal.name,
                                            subtitle = "Pre-planned meal",
                                            isSelected = selectedSource is MealSource.PrePlannedMeal &&
                                                    selectedSource?.id == meal.id,
                                            onClick = {
                                                onSourceSelected(MealSource.PrePlannedMeal(meal.id))
                                                expanded = false
                                                selectedSourceType = SourceType.MEAL
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            if (filteredRecipes.isEmpty()) {
                                Text(
                                    "No recipes available",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn {
                                    items(filteredRecipes) { recipe ->
                                        SelectionOptionItem(
                                            name = recipe.name,
                                            subtitle = "Standalone recipe",
                                            isSelected = selectedSource is MealSource.StandaloneRecipe &&
                                                    selectedSource?.id == recipe.id,
                                            onClick = {
                                                onSourceSelected(MealSource.StandaloneRecipe(recipe.id))
                                                expanded = false
                                                selectedSourceType = SourceType.RECIPE
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            if (filteredIngredients.isEmpty()) {
                                Text(
                                    "No ingredients available",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn {
                                    items(filteredIngredients) { ingredient ->
                                        SelectionOptionItem(
                                            name = ingredient.name,
                                            subtitle = "Standalone ingredient",
                                            isSelected = selectedSource is MealSource.StandaloneIngredient &&
                                                    selectedSource?.id == ingredient.id,
                                            onClick = {
                                                onSourceSelected(
                                                    MealSource.StandaloneIngredient(
                                                        id = ingredient.id,
                                                        quantity = selectedQuantity,
                                                        unitId = selectedUnit?.id
                                                    )
                                                )
                                                expanded = false
                                                selectedSourceType = SourceType.INGREDIENT
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            if (filteredRestaurants.isEmpty()) {
                                Text(
                                    "No restaurants available",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn {
                                    items(filteredRestaurants) { restaurant ->
                                        SelectionOptionItem(
                                            name = restaurant.name,
                                            subtitle = "Restaurant",
                                            isSelected = selectedSource is MealSource.Restaurant &&
                                                    selectedSource?.restaurantId == restaurant.id,
                                            onClick = {
                                                onSourceSelected(
                                                    MealSource.Restaurant(
                                                        restaurantId = restaurant.id,
                                                        anticipatedCostCents = anticipatedCost.toIntOrNull()?.times(100)
                                                            ?: 0
                                                    )
                                                )
                                                expanded = false
                                                selectedSourceType = SourceType.RESTAURANT
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Search mode - show all matching results
                    val searchResults = remember(searchQuery) {
                        val meals = availableMeals.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        val recipes = availableRecipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        val ingredients =
                            availableIngredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        val restaurants =
                            availableRestaurants.filter { it.name.contains(searchQuery, ignoreCase = true) }

                        val results = mutableListOf<Pair<String, String>>()
                        meals.forEach { results.add(it.name to "Meal") }
                        recipes.forEach { results.add(it.name to "Recipe") }
                        ingredients.forEach { results.add(it.name to "Ingredient") }
                        restaurants.forEach { results.add(it.name to "Restaurant") }
                        results
                    }

                    if (searchResults.isEmpty()) {
                        Text(
                            "No results found for \"$searchQuery\"",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(searchResults) { (name, type) ->
                                SelectionOptionItem(
                                    name = name,
                                    subtitle = type,
                                    isSelected = false,
                                    onClick = {}
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search all sources") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Conditional fields based on selection type
        selectedSourceType?.let { type ->
            Spacer(modifier = Modifier.height(8.dp))

            when (type) {
                SourceType.INGREDIENT -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Ingredient Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MpNumericStepper(
                                    value = selectedQuantity,
                                    onValueChange = onQuantityChange,
                                    label = "Quantity",
                                    modifier = Modifier.weight(1f),
                                    step = 0.5
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    SearchableDropdown(
                                        label = "Unit",
                                        options = listOf("servings", "g", "kg", "ml", "L", "cups", "tbsp", "tsp"),
                                        selectedOption = selectedUnit?.abbreviation ?: "",
                                        onOptionSelected = { abbr ->
                                            onUnitChange(
                                                UnitModel(
                                                    id = Uuid.random(),
                                                    abbreviation = abbr,
                                                    plural = "${abbr}s",
                                                    category = "cooking"
                                                )
                                            )
                                        },
                                        onAddOption = {},
                                        onDeleteOption = {},
                                        deleteWarningMessage = ""
                                    )
                                }
                            }
                        }
                    }
                }

                SourceType.RESTAURANT -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Restaurant Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = anticipatedCost,
                                onValueChange = onAnticipatedCostChange,
                                label = { Text("Anticipated Cost ($)") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                SourceType.MEAL, SourceType.RECIPE -> {
                    // No additional fields needed for meals and recipes
                }
            }
        }
    }

    /**
     * Internal data class to represent a selectable source type
     */
    private enum class SourceType {
        MEAL,
        RECIPE,
        INGREDIENT,
        RESTAURANT
    }

    /**
     * A dropdown menu item with selection state
     */
    @Composable
    private fun SelectionOptionItem(
        name: String,
        subtitle: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        DropdownMenuItem(
            text = {
                Column {
                    Text(
                        name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = onClick,
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
