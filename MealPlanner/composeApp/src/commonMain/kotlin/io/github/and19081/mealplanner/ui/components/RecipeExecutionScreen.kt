@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.RecipeExecutionView
import io.github.and19081.mealplanner.domain.model.RecipeExecutionState
import io.github.and19081.mealplanner.domain.model.ScaledRequirement
import kotlin.uuid.Uuid

/**
 * Full-screen recipe walkthrough experience.
 *
 * @param state The current recipe execution state
 * @param allItemNames Map of item IDs to display names
 * @param allUnitAbbr Map of unit IDs to abbreviation strings
 * @param onBack Callback when user navigates back
 * @param onFinish Callback when recipe is completed with yield information
 * @param modifier Modifier for the root layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeExecutionScreen(
    state: RecipeExecutionState,
    allItemNames: Map<Uuid, String>,
    allUnitAbbr: Map<Uuid, String>,
    onBack: () -> Unit,
    onFinish: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var yieldServings by remember { mutableStateOf(state.targetServings) }
    val scaleFactor = state.scaleFactor

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                title = {
                    Column {
                        Text(
                            state.recipe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Serves ${String.format("%.1f", state.targetServings)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            // In real implementation, call viewModel.setTargetServings
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Decrease servings"
                            )
                        }
                        Text(
                            String.format("%.1f", state.targetServings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = {
                            // In real implementation, call viewModel.setTargetServings
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                "Increase servings"
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val stepLabel = if (state.isOnOverview) "Overview"
            else "Step ${state.currentStepIndex + 1} / ${state.totalSteps}"
            Text(
                stepLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                (-1 until state.totalSteps).forEach { idx ->
                    val isCurrent = idx == state.currentStepIndex
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isCurrent) 10.dp else 7.dp),
                    ) {}
                }
            }

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                when (state.view) {
                    RecipeExecutionView.INSTRUCTIONS -> {
                        if (state.isOnOverview) {
                            OverviewPanel(
                                requirements = state.scaledRequirements(
                                    allItemNames,
                                    allUnitAbbr
                                ),
                                description = state.recipe.recipeInfo.description,
                            )
                        } else {
                            InstructionPanel(
                                stepNumber = state.currentStepIndex + 1,
                                instruction = state.currentInstruction ?: "",
                            )
                        }
                    }

                    RecipeExecutionView.INGREDIENTS -> {
                        IngredientsPanel(
                            requirements = state.scaledRequirements(
                                allItemNames,
                                allUnitAbbr
                            ),
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        // viewModel.prevStep()
                    },
                    enabled = !state.isOnOverview,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }

                FilterChip(
                    selected = state.view == RecipeExecutionView.INGREDIENTS,
                    onClick = {
                        // viewModel.toggleView()
                    },
                    label = {
                        Text(
                            if (state.view == RecipeExecutionView.INSTRUCTIONS)
                                "Ingredients" else "Instructions"
                        )
                    },
                )

                if (state.isOnFinalStep) {
                    Button(onClick = { /* TODO: Show finish sheet */ }) {
                        Text("Finish")
                    }
                } else {
                    Button(onClick = { /* viewModel.nextStep() */ }) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }

    if (state.isOnFinalStep) {
        LaunchedEffect(Unit) {
            // Show finish sheet in real implementation
        }
    }
}

@Composable
private fun OverviewPanel(
    requirements: List<ScaledRequirement>,
    description: String?,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        description?.let {
            item {
                Text(
                    it, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            Text(
                "Ingredients", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(requirements) { req ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(req.ingredientName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${String.format("%.2f", req.scaledQuantity)} ${req.unitAbbreviation}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun InstructionPanel(stepNumber: Int, instruction: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Step $stepNumber",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                instruction,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun IngredientsPanel(requirements: List<ScaledRequirement>) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(requirements) { req ->
            ListItem(
                headlineContent = { Text(req.ingredientName) },
                trailingContent = {
                    Text(
                        "${String.format("%.2f", req.scaledQuantity)} ${req.unitAbbreviation}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            )
            HorizontalDivider()
        }
    }
}
```
</think>

Now let me continue with the ReceiptReviewSheet:

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.domain.model.ReceiptLineItemDraft
import io.github.and19081.mealplanner.domain.model.ShoppingReceiptDraft
import io.github.and19081.mealplanner.domain.model.UnitModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A bottom sheet for reviewing and editing shopping receipt line items.
 * Replaces generic transaction dialog with inline editable prices.
 *
 * @param draft The shopping receipt draft containing line items
 * @param lineItems Current list of line items to display
 * @param onDismiss Request to dismiss the sheet
 * @param onSave Callback when receipt is finalized with actual total
 * @param modifier Modifier for the root layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ReceiptReviewSheet(
    draft: ShoppingReceiptDraft,
    lineItems: List<ReceiptLineItemDraft>,
    onDismiss: () -> Unit,
    onSave: (Int, Int?, Double) -> Unit, // actualTotalCents, taxPaidCents, taxRate
    modifier: Modifier = Modifier,
) {
    var actualTotalStr by remember { mutableStateOf(draft.userEnteredActualTotalCents?.toString() ?: "") }
    val computedSubtotal = draft.computedSubtotalCents
    val computedTax = draft.computedTaxCents
    val computedTotal = draft.computedTotalCents
    val userEnteredTotalCents = actualTotalStr.toIntOrNull()?.times(100)
    val differs = userEnteredTotalCents != null && userEnteredTotalCents != computedTotal
    val discrepancy = if (differs) (userEnteredTotalCents ?: 0) - computedTotal else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LocalWindowViewPadding.current.bottom + 24.dp),
        ) {
            Text(
                "Review Receipt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(lineItems) { item ->
                    ReceiptLineItemRow(
                        item = item,
                        onPriceChange = { /* Update draft */ }
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Subtotal:")
                Text(formatCurrency(computedSubtotal / 100.0), fontWeight = FontWeight.Medium)
            }

            if (draft.taxRate > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tax (${(draft.taxRate * 100).toInt()}%):")
                    Text(formatCurrency(computedTax / 100.0), fontWeight = FontWeight.Medium)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Computed Total:", fontWeight = FontWeight.Bold)
                Text(formatCurrency(computedTotal / 100.0), fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Your Total:", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = actualTotalStr,
                    onValueChange = { actualTotalStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp),
                    isError = differs,
                )
            }

            if (differs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (discrepancy > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Differs by ${formatCurrency(kotlin.math.abs(discrepancy / 100.0))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (discrepancy > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onSave(
                        actualTotalCents = userEnteredTotalCents ?: computedTotal,
                        taxPaidCents = null,
                        taxRate = draft.taxRate
                    )
                },
                enabled = userEnteredTotalCents != null,
            ) {
                Text("Save Receipt")
            }
        }
    }
}

@Composable
private fun ReceiptLineItemRow(
    item: ReceiptLineItemDraft,
    onPriceChange: (Int?) -> Unit,
) {
    var priceStr by remember {
        mutableStateOf(
            item.unitPriceCents?.let { it / 100.0 }?.toString() ?: ""
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${String.format("%.2f", item.plannedQuantity)} units",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = priceStr,
            onValueChange = {
                priceStr = it
                val price = it.toDoubleOrNull()?.times(100)?.toInt()
                onPriceChange(price)
            },
            label = { Text("Unit Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(120.dp),
            isError = priceStr.isEmpty() && item.unitPriceCents == null,
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return "$${String.format("%.2f", amount)}"
}
```

Now let me create the ConsumeMealDialog:

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.domain.model.ConsumptionResult
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.UnitModel
import io.github.and19081.mealplanner.ui.components.MpNumericStepper
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A unified dialog for consuming meals, recipes, ingredients, or restaurant orders.
 * Branches its UI based on the ConsumptionResult type.
 *
 * @param selectedResult The type of consumption result to show
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback with the consumed result
 * @param availableMeals List of available meals
 * @param availableRecipes List of available recipes
 * @param availableIngredients List of available ingredients
 * @param availableRestaurants List of available restaurants
 * @param modifier Modifier for the dialog
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ConsumeMealDialog(
    selectedResult: ConsumptionResult,
    onDismiss: () -> Unit,
    onConfirm: (ConsumptionResult) -> Unit,
    availableMeals: List<FoodItem> = emptyList(),
    availableRecipes: List<FoodItem> = emptyList(),
    availableIngredients: List<FoodItem> = emptyList(),
    availableRestaurants: List<Package> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var leftoverServingsStr by remember { mutableStateOf("") }
    var leftoverDescription by remember { mutableStateOf("") }
    var actualCostStr by remember {
        when (selectedResult) {
            is ConsumptionResult.RestaurantConsumptionSaved ->
                (selectedResult.actualCostCents / 100.0).toString()

            else -> ""
        }
    }

    val title = when (selectedResult) {
        is ConsumptionResult.HomeMealConsumed -> "Consume Meal"
        is ConsumptionResult.RecipeConsumed -> "Consume Recipe"
        is ConsumptionResult.IngredientConsumed -> "Consume Ingredient"
        is ConsumptionResult.RestaurantConsumptionSaved -> "Restaurant Receipt"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when (selectedResult) {
                    is ConsumptionResult.HomeMealConsumed -> {
                        val meal = availableMeals.find { it.id == selectedResult.mealSource.id }
                        Text("This will deduct ingredients from your pantry.")
                        meal?.let { m ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Leftover Servings?", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            MpNumericStepper(
                                value = leftoverServingsStr.toDoubleOrNull() ?: 0.0,
                                onValueChange = { leftoverServingsStr = it.toString() },
                                label = "Number of servings to save",
                                modifier = Modifier.fillMaxWidth(),
                                step = 0.5
                            )
                        }
                    }

                    is ConsumptionResult.RecipeConsumed -> {
                        val recipe = availableRecipes.find { it.id == selectedResult.recipeSource.id }
                        Text("This will deduct ingredients from your pantry.")
                        recipe?.let { r ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Leftover Servings?", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            MpNumericStepper(
                                value = leftoverServingsStr.toDoubleOrNull() ?: 0.0,
                                onValueChange = { leftoverServingsStr = it.toString() },
                                label = "Number of servings to save",
                                modifier = Modifier.fillMaxWidth(),
                                step = 0.5
                            )
                        }
                    }

                    is ConsumptionResult.IngredientConsumed -> {
                        val ingredient = availableIngredients.find { it.id == selectedResult.ingredientSource.id }
                        Text("This will deduct ")
                        ingredient?.let { i ->
                            Text("${i.name} from your pantry.")
                        }
                    }

                    is ConsumptionResult.RestaurantConsumptionSaved -> {
                        val restaurant = availableRestaurants.find { it.id == selectedResult.receiptId }
                        Text("Confirm restaurant consumption:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Restaurant:")
                            restaurant?.let { r ->
                                Text(r.name, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Actual Cost ($):", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actualCostStr,
                            onValueChange = { actualCostStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Leftover Description (optional):")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = leftoverDescription,
                            onValueChange = { leftoverDescription = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe any leftovers") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(selectedResult)
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

Now let me create the DayDetailPanel:

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.feature.meals.MealPlan
import io.github.and19081.mealplanner.feature.meals.RecipeMealType
import io.github.and19081.mealplanner.feature.settings.Mode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Shows all meals for a selected day in time order.
 * Acts as a dialog on mobile and a side panel on desktop.
 *
 * @param date The date to display
 * @param dayEvents All meal events for the selected day
 * @param onEventClick Callback when an event is clicked
 * @param onAddEvent Callback to add a new event
 * @param onConsumeEvent Callback to consume an event
 * @param onDismiss Request to dismiss the panel
 * @param isExpanded Whether the panel is in desktop expanded mode
 * @param allMeals List of available meals for reference
 * @param allRestaurants List of available restaurants
 * @param mode The UI mode (mobile, desktop, auto)
 * @param modifier Modifier for the root layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayDetailPanel(
    date: LocalDate,
    dayEvents: List<DayEventUi>,
    onEventClick: (Uuid) -> Unit,
    onAddEvent: () -> Unit,
    onConsumeEvent: (Uuid) -> Unit,
    onDismiss: () -> Unit,
    allMeals: List<FoodItem> = emptyList(),
    allRestaurants: List<Package> = emptyList(),
    mode: Mode = Mode.AUTO,
    modifier: Modifier = Modifier,
) {
    val isDesktopExpanded = when (mode) {
        Mode.AUTO -> false
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
    }

    if (isDesktopExpanded) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .wrapContentWidth(),
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "${date.dayOfWeek.name}, ${date.dayOfMonth} ${date.month.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "$dayEvents events for the day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close panel")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (dayEvents.isEmpty()) {
                        item {
                            EmptyListMessage(
                                message = "No meals planned for ${date.month.name} $date.dayOfMonth",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(dayEvents) { event ->
                            DayEventRow(
                                event = event,
                                onEventClick = { onEventClick(event.entryId) },
                                onConsume = { onConsumeEvent(event.entryId) },
                                allMeals = allMeals,
                                allRestaurants = allRestaurants,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ExtendedFloatingActionButton(onClick = onAddEvent) {
                        Icon(Icons.Default.Add, "Add meal")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Meal")
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            title = {
                Column {
                    Text(
                        "${date.dayOfWeek.name}, ${date.dayOfMonth} ${date.month.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${dayEvents.size} events for the day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                if (dayEvents.isEmpty()) {
                    EmptyListMessage(
                        message = "No meals planned for ${date.month.name} $date.dayOfMonth",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(dayEvents) { event ->
                            DayEventRow(
                                event = event,
                                onEventClick = { onEventClick(event.entryId) },
                                onConsume = { onConsumeEvent(event.entryId) },
                                allMeals = allMeals,
                                allRestaurants = allRestaurants,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onAddEvent) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.padding(end = 8.dp))
                    Text("Add Meal")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

data class DayEventUi(
    val entryId: Uuid,
    val title: String,
    val mealType: RecipeMealType,
    val time: LocalTime,
    val isConsumed: Boolean,
    val peopleCount: Int,
)

@Composable
private fun DayEventRow(
    event: DayEventUi,
    onEventClick: () -> Unit,
    onConsume: () -> Unit,
    allMeals: List<FoodItem>,
    allRestaurants: List<Package>,
) {
    val color = when (event.mealType) {
        RecipeMealType.Breakfast -> MaterialTheme.colorScheme.primary
        RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
        RecipeMealType.Dinner -> MaterialTheme.colorScheme.tertiary
        RecipeMealType.Snack -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEventClick),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isConsumed)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${event.time} • ${event.peopleCount} ppl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.size(12.dp),
                ) {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    event.mealType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!event.isConsumed) {
                    FilledTonalButton(
                        onClick = onConsume,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = color
                        ),
                    ) {
                        Icon(Icons.Default.CheckCircle, "Consume")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Consume")
                    }
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Consumed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
