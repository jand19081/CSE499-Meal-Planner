@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

import io.github.and19081.mealplanner.settings.Mode

@Composable
fun DashboardView(
    viewModel: DashboardViewModel,
    mode: Mode
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    BoxWithConstraints {
        val isExpanded = when (mode) {
            Mode.AUTO -> maxWidth > 840.dp
            Mode.DESKTOP -> true
            Mode.MOBILE -> false
        }

        Scaffold { innerPadding ->
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Welcome & Stats
                    Column(
                        modifier = Modifier.weight(0.4f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardHeader(today)
                        MpValidationWarning(warnings = uiState.warnings)
                        DashboardStats(uiState)
                    }

                    VerticalDivider(modifier = Modifier.width(1.dp).padding(vertical = 16.dp))

                    // Right Column: Meal Plan
                    Column(
                        modifier = Modifier.weight(0.6f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardMealPlan(uiState, viewModel)
                    }
                }
            } else {
                // Mobile View
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { DashboardHeader(today) }
                    item { MpValidationWarning(warnings = uiState.warnings) }
                    item { DashboardStats(uiState) }
                    dashboardMealPlanContent(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(today: kotlinx.datetime.LocalDate) {
    Column {
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
}

@Composable
fun DashboardStats(uiState: DashboardViewModel.DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Weekly Cost (If Enabled)
        if (uiState.dashboardConfig.showWeeklyCost) {
            DashboardStatCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Last 7 Days Spending",
                value = "$${String.format("%.2f", uiState.currentWeekCost / 100.0)}",
                icon = Icons.Default.AttachMoney,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Stats (Shopping List)
        if (uiState.dashboardConfig.showShoppingListSummary) {
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
                    value = "${uiState.shoppingListCount}",
                    icon = Icons.Default.ShoppingCart,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun DashboardMealPlan(uiState: DashboardViewModel.DashboardUiState, viewModel: DashboardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (uiState.dashboardConfig.showMealPlan) {
            // Next Meal
            if (uiState.nextMeal != null) {
                Card(
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
                        Button(
                            onClick = { viewModel.toggleMealConsumption(uiState.nextMeal!!.entryId, uiState.nextMeal!!.isConsumed) },
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

            // Today's Schedule
            Text(
                "Today's Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (uiState.todaysMeals.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No meals planned for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                uiState.todaysMeals.forEach { meal ->
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
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.toggleMealConsumption(meal.entryId, meal.isConsumed) }) {
                                if (meal.isConsumed) {
                                    Icon(Icons.Default.CheckCircle, "Undo Consumption", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.CheckCircle, "Mark as Consumed", tint = Color.Gray)
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

fun LazyListScope.dashboardMealPlanContent(uiState: DashboardViewModel.DashboardUiState, viewModel: DashboardViewModel) {
    item {
        DashboardMealPlan(uiState, viewModel)
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
    Card(
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
