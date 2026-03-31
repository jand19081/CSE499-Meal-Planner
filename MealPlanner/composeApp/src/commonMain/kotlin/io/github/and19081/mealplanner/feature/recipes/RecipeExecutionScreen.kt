package io.github.and19081.mealplanner.feature.recipes

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
import io.github.and19081.mealplanner.ui.components.MpNumericStepper
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeExecutionScreen(
    viewModel: RecipeExecutionViewModel,
    allItemNames: Map<Uuid, String>,
    allUnitAbbr: Map<Uuid, String>,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val isFinishing by viewModel.isFinishing.collectAsState()
    var yieldServings by remember { mutableStateOf(4.0) }


    val currentState = state
    if (currentState == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Sync default yield to current target servings when state first loads
    LaunchedEffect(currentState.recipe.id) {
        yieldServings = currentState.targetServings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancel()
                        onBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                title = {
                    Column {
                        Text(
                            currentState.recipe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Serves ${String.format("%.1f", currentState.targetServings)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Servings stepper in top bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.setTargetServings(currentState.targetServings - 0.5)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Decrease servings")
                        }
                        Text(
                            String.format("%.1f", currentState.targetServings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = {
                            viewModel.setTargetServings(currentState.targetServings + 0.5)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Increase servings")
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
            // Step indicator
            val stepLabel = if (currentState.isOnOverview) "Overview"
            else "Step ${currentState.currentStepIndex + 1} / ${currentState.totalSteps}"
            Text(
                stepLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
            )

            // Step dot row
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                (-1 until currentState.totalSteps).forEach { idx ->
                    val isCurrent = idx == currentState.currentStepIndex
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

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when (currentState.view) {
                    RecipeExecutionView.INSTRUCTIONS -> {
                        if (currentState.isOnOverview) {
                            OverviewPanel(
                                requirements = currentState.scaledRequirements(allItemNames, allUnitAbbr),
                                description = currentState.recipe.recipeInfo.description,
                            )
                        } else {
                            InstructionPanel(
                                stepNumber = currentState.currentStepIndex + 1,
                                instruction = currentState.currentInstruction ?: "",
                            )
                        }
                    }

                    RecipeExecutionView.INGREDIENTS -> {
                        IngredientsPanel(
                            requirements = currentState.scaledRequirements(allItemNames, allUnitAbbr),
                        )
                    }
                }
            }

            HorizontalDivider()

            // Bottom navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevStep() },
                    enabled = !currentState.isOnOverview,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }

                // View toggle pill
                FilterChip(
                    selected = currentState.view == RecipeExecutionView.INGREDIENTS,
                    onClick = { viewModel.toggleView() },
                    label = {
                        Text(
                            if (currentState.view == RecipeExecutionView.INSTRUCTIONS)
                                "Ingredients" else "Instructions"
                        )
                    },
                )

                if (currentState.isOnFinalStep) {
                    Button(onClick = { viewModel.showFinishSheet() }) {
                        Text("Finish")
                    }
                } else {
                    Button(onClick = { viewModel.nextStep() }) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }

    // Finish sheet
    if (isFinishing) {
        AlertDialog(
            onDismissRequest = { viewModel.hideFinishSheet() },
            title = { Text("How much did you make?") },
            text = {
                MpNumericStepper(
                    value = yieldServings,
                    onValueChange = { yieldServings = it },
                    label = "Yield (servings)",
                    step = 0.5,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.finish(yieldServings) }) {
                    Text("Add to pantry as leftovers")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideFinishSheet() }) { Text("Cancel") }
            },
        )
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
