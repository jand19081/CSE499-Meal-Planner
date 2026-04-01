@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.kitchen.ConsumptionResult
import kotlin.uuid.ExperimentalUuidApi

/**
 * A unified dialog for consuming meals, recipes, ingredients, or restaurant orders. Branches its UI
 * based on the ConsumptionResult type.
 *
 * @param selectedResult The type of consumption to confirm, keyed by scheduledMealId
 * @param mealName Display name of the meal or restaurant being consumed
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback with the final result including any user-entered extras
 * @param modifier Modifier for the dialog
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ConsumeMealDialog(
    selectedResult: ConsumptionResult,
    mealName: String,
    onDismiss: () -> Unit,
    onConfirm: (ConsumptionResult) -> Unit,
    modifier: Modifier = Modifier,
) {
  var leftoverServings by remember { mutableStateOf(0.0) }
  var leftoverDescription by remember { mutableStateOf("") }
  var actualCostStr by remember {
    mutableStateOf(
        when (selectedResult) {
          is ConsumptionResult.RestaurantMealConsumed ->
              (selectedResult.actualCostCents / 100.0).let { if (it == 0.0) "" else it.toString() }
          else -> ""
        }
    )
  }

  val title =
      when (selectedResult) {
        is ConsumptionResult.HomeMealConsumed -> "Consume Meal"
        is ConsumptionResult.HomeRecipeConsumed -> "Consume Recipe"
        is ConsumptionResult.HomeIngredientConsumed -> "Consume Ingredient"
        is ConsumptionResult.RestaurantMealConsumed -> "Restaurant Receipt"
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = modifier,
      title = { Text(title) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(mealName, style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(12.dp))

          when (selectedResult) {
            is ConsumptionResult.HomeMealConsumed -> {
              Text("This will deduct ingredients from your pantry.")
              Spacer(modifier = Modifier.height(8.dp))
              Text("Leftover Servings?", style = MaterialTheme.typography.titleSmall)
              Spacer(modifier = Modifier.height(8.dp))
              MpNumericStepper(
                  value = leftoverServings,
                  onValueChange = { leftoverServings = it },
                  label = "Servings to save",
                  modifier = Modifier.fillMaxWidth(),
                  step = 0.5,
              )
            }

            is ConsumptionResult.HomeRecipeConsumed -> {
              Text("This will deduct ingredients from your pantry.")
              Spacer(modifier = Modifier.height(8.dp))
              Text("Leftover Servings?", style = MaterialTheme.typography.titleSmall)
              Spacer(modifier = Modifier.height(8.dp))
              MpNumericStepper(
                  value = leftoverServings,
                  onValueChange = { leftoverServings = it },
                  label = "Servings to save",
                  modifier = Modifier.fillMaxWidth(),
                  step = 0.5,
              )
            }

            is ConsumptionResult.HomeIngredientConsumed -> {
              Text("This will deduct $mealName from your pantry.")
            }

            is ConsumptionResult.RestaurantMealConsumed -> {
              Text("Actual Cost (\$):", style = MaterialTheme.typography.titleSmall)
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = actualCostStr,
                  onValueChange = { actualCostStr = it },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text("Leftover Description (optional):")
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = leftoverDescription,
                  onValueChange = { leftoverDescription = it },
                  modifier = Modifier.fillMaxWidth(),
                  placeholder = { Text("Describe any leftovers") },
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              val finalResult =
                  when (selectedResult) {
                    is ConsumptionResult.HomeMealConsumed ->
                        selectedResult.copy(leftoverServings = leftoverServings)
                    is ConsumptionResult.HomeRecipeConsumed ->
                        selectedResult.copy(leftoverServings = leftoverServings)
                    is ConsumptionResult.HomeIngredientConsumed -> selectedResult
                    is ConsumptionResult.RestaurantMealConsumed ->
                        selectedResult.copy(
                            actualCostCents =
                                ((actualCostStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                            leftoverDescription = leftoverDescription.ifBlank { null },
                        )
                  }
              onConfirm(finalResult)
              onDismiss()
            }
        ) {
          Text("Confirm")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
