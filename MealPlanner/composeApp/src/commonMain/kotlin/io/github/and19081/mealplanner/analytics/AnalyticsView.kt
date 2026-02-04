@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.uicomponents.MpButton
import io.github.and19081.mealplanner.uicomponents.MpCard
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun AnalyticsView() {
    val viewModel = viewModel { AnalyticsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTrip by remember { mutableStateOf<ReceiptHistory?>(null) }

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
                        val storeName = uiState.allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
                        ListItem(
                            modifier = Modifier.clickable { selectedTrip = trip },
                            headlineContent = { Text(storeName) },
                            supportingContent = { Text(trip.date.toString()) },
                            trailingContent = { Text("$${String.format("%.2f", trip.actualTotalCents / 100.0)}", fontWeight = FontWeight.Bold) },
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
        val storeName = uiState.allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
        ReceiptDetailsDialog(trip = trip, storeName = storeName, onDismiss = { selectedTrip = null })
    }
}

@Composable
fun ReceiptDetailsDialog(trip: ReceiptHistory, storeName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt: $storeName") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Date: ${trip.date}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Projected Total:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${String.format("%.2f", trip.projectedTotalCents / 100.0)}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tax Paid:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${String.format("%.2f", trip.taxPaidCents / 100.0)}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$${String.format("%.2f", trip.actualTotalCents / 100.0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            MpButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
