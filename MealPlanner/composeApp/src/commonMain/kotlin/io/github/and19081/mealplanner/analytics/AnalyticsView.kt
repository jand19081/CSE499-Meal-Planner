@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.MpButton
import io.github.and19081.mealplanner.MpCard
import io.github.and19081.mealplanner.shoppinglist.ShoppingTrip
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun AnalyticsView() {
    val viewModel = viewModel { AnalyticsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTrip by remember { mutableStateOf<ShoppingTrip?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("Financial Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last 7 Days (Actual)", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.actualWeekCents / 100.0)}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Next 7 Days (Projected)", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.projectedWeekCents / 100.0)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        
        item {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last 30 Days (Actual)", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.actualMonthCents / 100.0)}", style = MaterialTheme.typography.headlineMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Next 30 Days (Projected)", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.projectedMonthCents / 100.0)}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg / Meal", style = MaterialTheme.typography.titleSmall)
                        Text("$${String.format("%.2f", uiState.avgMealCostCents / 100.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                MpCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg / Person", style = MaterialTheme.typography.titleSmall)
                        Text("$${String.format("%.2f", uiState.avgCostPerPersonCents / 100.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Spending by Store", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.spendingByStore.forEach { (store, amount) ->
                        ListItem(
                            headlineContent = { Text(store) },
                            trailingContent = { Text("$${String.format("%.2f", amount / 100.0)}", fontWeight = FontWeight.Bold) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider()
                    }
                    if (uiState.spendingByStore.isEmpty()) {
                        Text("No shopping data available.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        item {
            Text("Recent Shopping Trips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.recentTrips.take(5).forEach { trip ->
                        ListItem(
                            modifier = Modifier.clickable { selectedTrip = trip },
                            headlineContent = { Text(trip.storeName) },
                            supportingContent = { Text(trip.date.toString()) },
                            trailingContent = { Text("$${String.format("%.2f", trip.totalPaidCents / 100.0)}", fontWeight = FontWeight.Bold) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider()
                    }
                    if (uiState.recentTrips.isEmpty()) {
                        Text("No recent trips recorded.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        item {
            Text("Most Expensive Meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.mostExpensiveMeals.forEachIndexed { index, (name, cost) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            trailingContent = { Text("$${String.format("%.2f", cost / 100.0)}", fontWeight = FontWeight.Bold) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index < uiState.mostExpensiveMeals.lastIndex) HorizontalDivider()
                    }
                    if (uiState.mostExpensiveMeals.isEmpty()) {
                        Text("No meal data available.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
    
    selectedTrip?.let { trip ->
        ReceiptDetailsDialog(trip = trip, onDismiss = { selectedTrip = null })
    }
}

@Composable
fun ReceiptDetailsDialog(trip: ShoppingTrip, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt: ${trip.storeName}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Date: ${trip.date}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(trip.items) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$${String.format("%.2f", item.priceCents / 100.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${String.format("%.2f", trip.subtotalCents / 100.0)}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tax:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${String.format("%.2f", trip.taxCents / 100.0)}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$${String.format("%.2f", trip.totalPaidCents / 100.0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            MpButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

