package io.github.and19081.mealplanner.kitchen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.UnitModel
import kotlin.uuid.Uuid

@Composable
fun TransactionReviewSheet(
    transaction: KitchenTransaction,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onCommit: (KitchenTransaction) -> Unit
) {
    var currentChanges by remember { mutableStateOf(transaction.changes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = transaction.title,
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = "Review and adjust what actually happened in the kitchen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(currentChanges) { index, change ->
                TransactionLineItem(
                    change = change,
                    onUpdate = { updated ->
                        currentChanges = currentChanges.toMutableList().apply {
                            set(index, updated)
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onCommit(transaction.copy(changes = currentChanges)) },
                modifier = Modifier.weight(1f)
            ) {
                val actionText = when (transaction.type) {
                    TransactionType.Acquisition -> "Record Trip"
                    TransactionType.Production -> "Finish Prep"
                    TransactionType.Consumption -> "Serve Meal"
                }
                Text(actionText)
            }
        }
    }
}

@Composable
fun TransactionLineItem(
    change: InventoryChange,
    onUpdate: (InventoryChange) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val directionColor = if (change.direction == TransactionDirection.IN) {
                Color(0xFF2E7D32) // Green
            } else {
                MaterialTheme.colorScheme.error
            }

            Icon(
                imageVector = if (change.direction == TransactionDirection.IN) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = null,
                tint = directionColor,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.ingredientName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${if (change.direction == TransactionDirection.IN) "Add to" else "Use from"} Pantry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Simplified quantity adjustment for now - in a real app this would be a text field
                // but for this prototype, we'll keep it simple or use a basic text field
                OutlinedTextField(
                    value = if (change.quantity == 0.0) "" else change.quantity.toString(),
                    onValueChange = { 
                        val qty = it.toDoubleOrNull() ?: 0.0
                        onUpdate(change.copy(quantity = qty, isAdjusted = true))
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )
                Text(
                    text = change.unitAbbreviation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
