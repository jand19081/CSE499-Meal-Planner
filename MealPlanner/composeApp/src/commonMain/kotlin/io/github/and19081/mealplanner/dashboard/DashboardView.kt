@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

import io.github.and19081.mealplanner.MpButton
import io.github.and19081.mealplanner.MpCard
import io.github.and19081.mealplanner.RecipeMealType

@Composable
fun DashboardView() {
    val viewModel = viewModel { DashboardViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${today.day}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Weekly Cost (If Enabled)
            if (uiState.dashboardConfig.showWeeklyCost) {
                item {
                    DashboardStatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Last 7 Days Spending",
                        value = "$${String.format("%.2f", uiState.currentWeekCost / 100.0)}",
                        icon = Icons.Default.AttachMoney,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Stats (Shopping List)
            if (uiState.dashboardConfig.showShoppingListSummary) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Pantry Items",
                            value = "${uiState.lowStockCount}",
                            icon = Icons.Default.Inventory,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            title = "To Buy",
                            value = "?", // Placeholder until linked to ShoppingListVM
                            icon = Icons.Default.ShoppingCart,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            if (uiState.dashboardConfig.showMealPlan) {
                // Next Meal
                if (uiState.nextMeal != null) {
                    item {
                        MpCard(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text(
                                    "Up Next: ${uiState.nextMeal!!.mealType.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    uiState.nextMeal!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MpButton(
                                    onClick = { viewModel.consumeMeal(uiState.nextMeal!!.entryId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mark as Consumed")
                                }
                            }
                        }
                    }
                }

                // Today's Schedule
                item {
                    Text(
                        "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState.todaysMeals.isEmpty()) {
                    item {
                        MpCard(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No meals planned for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.todaysMeals) { meal ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    meal.title, 
                                    fontWeight = if (meal.isConsumed) FontWeight.Normal else FontWeight.Medium,
                                    color = if (meal.isConsumed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            overlineContent = { Text(meal.mealType.name) },
                            leadingContent = {
                                if (meal.isConsumed) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Gray)
                                } else {
                                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            trailingContent = {
                                if (!meal.isConsumed) {
                                    IconButton(onClick = { viewModel.consumeMeal(meal.entryId) }) {
                                        Icon(Icons.Default.CheckCircle, "Consume")
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    MpCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
        }
    }
}
