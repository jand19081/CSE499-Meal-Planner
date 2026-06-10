@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import kotlinx.datetime.LocalTime

@Composable
fun ReceiptForm(
    trip: ReceiptHistory,
    locationName: String,
    allIngredients: List<FoodItem>,
    allUnits: List<UnitModel>,
    onClose: () -> Unit,
    onSave: (ReceiptHistory) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
  var actualTotalStr by remember { mutableStateOf((trip.actualTotalCents / 100.0).toString()) }
  var taxPaidStr by remember { mutableStateOf((trip.taxPaidCents / 100.0).toString()) }
  var lineItems by remember { mutableStateOf(trip.lineItems) }
  var selectedTime by remember { mutableStateOf(trip.time) }

  val timePickerState =
      rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute)
  var showTimePicker by remember { mutableStateOf(false) }

  MpDetailScaffold(
      title = "Edit Receipt",
      onClose = onClose,
      onSave = {
        val updatedTrip =
            trip.copy(
                actualTotalCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                taxPaidCents = ((taxPaidStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                lineItems = lineItems,
                time = selectedTime,
            )
        onSave(updatedTrip)
      },
      onDelete = onDelete,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text(locationName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
            "Date: ${trip.date}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { showTimePicker = true }) { Text(selectedTime.toString()) }
      }

      MpOutlinedTextField(
          value = actualTotalStr,
          onValueChange = { actualTotalStr = it },
          label = { Text("Total Paid ($)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
      )

      MpOutlinedTextField(
          value = taxPaidStr,
          onValueChange = { taxPaidStr = it },
          label = { Text("Tax Paid ($)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
      )

      Text("Line Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

      lineItems.forEachIndexed { index, item ->
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
          val name =
              item.customName
                  ?: allIngredients.find { it.id == item.measurement.foodItemId }?.name
                  ?: "Unknown"
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { lineItems = lineItems.filterIndexed { i, _ -> i != index } }) {
              Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpOutlinedTextField(
                value = item.measurement.quantity.toString(),
                onValueChange = { qty ->
                  val q = qty.toDoubleOrNull() ?: 0.0
                  lineItems = lineItems.mapIndexed { i, old ->
                    if (i == index) old.copy(measurement = old.measurement.copy(quantity = q))
                    else old
                  }
                },
                label = { Text("Qty") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            MpOutlinedTextField(
                value = (item.pricePaidCents / 100.0).toString(),
                onValueChange = { price ->
                  val p = ((price.toDoubleOrNull() ?: 0.0) * 100).toInt()
                  lineItems = lineItems.mapIndexed { i, old ->
                    if (i == index) old.copy(pricePaidCents = p) else old
                  }
                },
                label = { Text("Price ($)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
          }
        }
        HorizontalDivider()
      }
    }
  }

  if (showTimePicker) {
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
              }
          ) {
            Text("Ok")
          }
        },
        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        text = { TimePicker(state = timePickerState) },
    )
  }
}
