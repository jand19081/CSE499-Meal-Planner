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
import io.github.and19081.mealplanner.feature.recipes.RecipeExecutionState
import io.github.and19081.mealplanner.feature.recipes.RecipeExecutionView
import io.github.and19081.mealplanner.feature.recipes.ScaledRequirement
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
                IconButton(
                    onClick = {
                      // In real implementation, call viewModel.setTargetServings
                    }
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, "Decrease servings")
                }
                Text(
                    String.format("%.1f", state.targetServings),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {
                      // In real implementation, call viewModel.setTargetServings
                    }
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowForward, "Increase servings")
                }
              }
            },
        )
      },
  ) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
      val stepLabel =
          if (state.isOnOverview) "Overview"
          else "Step ${state.currentStepIndex + 1} / ${state.totalSteps}"
      Text(
          stepLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
      )

      Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      ) {
        (-1 until state.totalSteps).forEach { idx ->
          val isCurrent = idx == state.currentStepIndex
          Surface(
              shape = MaterialTheme.shapes.extraLarge,
              color =
                  if (isCurrent) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant,
              modifier = Modifier.padding(horizontal = 3.dp).size(if (isCurrent) 10.dp else 7.dp),
          ) {}
        }
      }

      HorizontalDivider()

      Box(modifier = Modifier.weight(1f)) {
        when (state.view) {
          RecipeExecutionView.INSTRUCTIONS -> {
            if (state.isOnOverview) {
              OverviewPanel(
                  requirements = state.scaledRequirements(allItemNames, allUnitAbbr),
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
                requirements = state.scaledRequirements(allItemNames, allUnitAbbr),
            )
          }
        }
      }

      HorizontalDivider()

      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                  if (state.view == RecipeExecutionView.INSTRUCTIONS) "Ingredients"
                  else "Instructions"
              )
            },
        )

        if (state.isOnFinalStep) {
          Button(onClick = { /* TODO: Show finish sheet */ }) { Text("Finish") }
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
            it,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
      }
    }
    item {
      Text(
          "Ingredients",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
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
