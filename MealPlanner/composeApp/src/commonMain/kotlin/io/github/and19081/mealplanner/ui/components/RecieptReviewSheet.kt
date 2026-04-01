@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptLineItemDraft
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingReceiptDraft
import kotlin.uuid.ExperimentalUuidApi

/**
 * A bottom sheet for reviewing and editing shopping receipt line items. Replaces generic
 * transaction dialog with inline editable prices.
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
  var actualTotalStr by remember { mutableStateOf(draft.userEnteredTotalCents?.toString() ?: "") }
  val computedSubtotal = draft.subtotalCents
  val computedTax = draft.taxCents
  val computedTotal = draft.computedTotalCents
  val userEnteredTotalCents = actualTotalStr.toDoubleOrNull()?.times(100)?.toLong()
  val differs = userEnteredTotalCents != null && userEnteredTotalCents != computedTotal
  val discrepancy = if (differs) (userEnteredTotalCents ?: 0L) - computedTotal else 0L

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
    ) {
      Text(
          "Review Receipt",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      )

      HorizontalDivider()

      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(lineItems) { item ->
          ReceiptLineItemRow(item = item, onPriceChange = { /* Update draft */ })
        }
      }

      HorizontalDivider()

      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Subtotal:")
        Text(formatCurrency(computedSubtotal / 100.0), fontWeight = FontWeight.Medium)
      }

      if (draft.taxRatePercent > 0) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Tax (${draft.taxRatePercent.toInt()}%):")
          Text(formatCurrency(computedTax / 100.0), fontWeight = FontWeight.Medium)
        }
      }

      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Computed Total:", fontWeight = FontWeight.Bold)
        Text(formatCurrency(computedTotal / 100.0), fontWeight = FontWeight.Bold)
      }

      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint =
                  if (discrepancy > 0) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              "Differs by ${formatCurrency(kotlin.math.abs(discrepancy / 100.0))}",
              style = MaterialTheme.typography.bodySmall,
              color =
                  if (discrepancy > 0) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
      TextButton(onClick = onDismiss) { Text("Cancel") }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
          onClick = {
            onSave(
                (userEnteredTotalCents ?: computedTotal).toInt(),
                null,
                draft.taxRatePercent,
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
    mutableStateOf(if (item.priceCents > 0) (item.priceCents / 100.0).toString() else "")
  }

  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
      Text(
          "${String.format("%.2f", item.quantity)} ${item.unitAbbreviation}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        isError = priceStr.isEmpty() && item.priceCents == 0L,
    )
  }
}

private fun formatCurrency(amount: Double): String {
  return "$${String.format("%.2f", amount)}"
}
