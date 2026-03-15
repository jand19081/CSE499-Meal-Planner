package io.github.and19081.mealplanner.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown
import kotlin.uuid.Uuid

@Composable
fun PrepareBatchDialog(
    itemName: String,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onConfirm: (multiplier: Double, yieldIngId: Uuid?, yieldQty: Double?, yieldUnitId: Uuid?) -> Unit
) {
    var multiplierStr by remember { mutableStateOf("1") }
    var saveYield by remember { mutableStateOf(false) }
    var selectedIngName by remember { mutableStateOf("") }
    var yieldQtyStr by remember { mutableStateOf("") }
    var selectedUnitName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prepare Batch: $itemName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MpOutlinedTextField(
                    value = multiplierStr,
                    onValueChange = { multiplierStr = it },
                    label = { Text("Multiplier (Batches/Servings)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveYield, onCheckedChange = { saveYield = it })
                    Text("Add yield to pantry", style = MaterialTheme.typography.bodyMedium)
                }

                if (saveYield) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchableDropdown(
                                label = "Ingredient to Add",
                                options = allIngredients.map { it.name },
                                selectedOption = selectedIngName,
                                onOptionSelected = { selectedIngName = it },
                                onAddOption = {}, onDeleteOption = {}, deleteWarningMessage = ""
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MpOutlinedTextField(
                                    value = yieldQtyStr,
                                    onValueChange = { yieldQtyStr = it },
                                    label = { Text("Yield Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    SearchableDropdown(
                                        label = "Unit",
                                        options = allUnits.map { it.abbreviation },
                                        selectedOption = selectedUnitName,
                                        onOptionSelected = { selectedUnitName = it },
                                        onAddOption = {}, onDeleteOption = {}, deleteWarningMessage = ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val multi = multiplierStr.toDoubleOrNull() ?: 1.0
                    if (saveYield) {
                        val ingId = allIngredients.find { it.name == selectedIngName }?.id
                        val qty = yieldQtyStr.toDoubleOrNull()
                        val unitId = allUnits.find { it.abbreviation == selectedUnitName }?.id
                        if (ingId != null && qty != null && unitId != null) {
                            onConfirm(multi, ingId, qty, unitId)
                        }
                    } else {
                        onConfirm(multi, null, null, null)
                    }
                },
                enabled = !saveYield || (selectedIngName.isNotBlank() && yieldQtyStr.isNotBlank() && selectedUnitName.isNotBlank())
            ) {
                Text("Review Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}