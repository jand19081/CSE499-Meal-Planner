@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.Meal
import io.github.and19081.mealplanner.MealType
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun CalendarView() {
    val viewModel = viewModel { CalendarViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDayOverviewDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedDateForDialog == null) {
                        viewModel.selectDate(today)
                        selectedDateForDialog = today
                    }
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plan Meal")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            CalendarHeader(
                currentMonth = uiState.currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + uiState.currentMonth.year,
                onPreviousClick = { viewModel.toPreviousMonth() },
                onNextClick = { viewModel.toNextMonth() }
            )

            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))

            CalendarGrid(
                dates = uiState.dates,
                onDateClick = { dateModel ->
                    viewModel.selectDate(dateModel.date)
                    selectedDateForDialog = dateModel.date
                    showDayOverviewDialog = true
                }
            )
        }
    }

    if (showAddDialog && selectedDateForDialog != null) {
        AddPlanDialog(
            date = selectedDateForDialog!!,
            availableMeals = uiState.availableMeals,
            onDismiss = { showAddDialog = false },
            onConfirm = { meal, mealType, servings ->
                viewModel.addPlan(selectedDateForDialog!!, meal, mealType, servings)
                showAddDialog = false
            }
        )
    }

    if (showDayOverviewDialog && selectedDateForDialog != null) {
        val dateUiModel = uiState.dates.find { it.date == selectedDateForDialog }
        val events = dateUiModel?.events ?: emptyList()

        DayOverviewDialog(
            date = selectedDateForDialog!!,
            events = events,
            onDismiss = { showDayOverviewDialog = false },
            onAddClick = {
                showDayOverviewDialog = false
                showAddDialog = true
            }
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
        }
        Text(
            text = currentMonth,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = DayOfWeek.entries.sortedBy { it.ordinal }
        days.forEach { day ->
            Text(
                text = day.name.take(3),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarGrid(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit
) {
    Column {
        var index = 0
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                repeat(7) {
                    if (index < dates.size) {
                        val dateModel = dates[index]
                        CalendarCell(
                            dateModel = dateModel,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onDateClick(dateModel) }
                        )
                        index++
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    dateModel: CalendarUiState.DateUiModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (dateModel.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = dateModel.date.day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (dateModel.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (dateModel.isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            dateModel.events.forEach { event ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = when(event.mealType) {
                                MealType.BREAKFAST -> Color(0xFFFFA000)
                                MealType.LUNCH -> Color(0xFF1976D2)
                                MealType.DINNER -> Color(0xFF388E3C)
                                MealType.DESSERT -> Color(0xFF8E24AA)
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun DayOverviewDialog(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Meals for $date")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No meals planned for this day.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = event.mealType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Serves: ${event.servings}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Meal")
            }
        }
    )
}

@Composable
fun AddPlanDialog(
    date: LocalDate,
    availableMeals: List<Meal>,
    onDismiss: () -> Unit,
    onConfirm: (Meal, MealType, Double) -> Unit
) {
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var selectedType by remember { mutableStateOf(MealType.DINNER) }
    var servingsText by remember { mutableStateOf("4") }

    var expandedMeal by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Meal for $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Meal Selector
                Box {
                    OutlinedButton(onClick = { expandedMeal = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedMeal?.name ?: "Select Meal")
                    }
                    DropdownMenu(expanded = expandedMeal, onDismissRequest = { expandedMeal = false }) {
                        availableMeals.forEach { meal ->
                            DropdownMenuItem(
                                text = { Text(meal.name) },
                                onClick = {
                                    selectedMeal = meal
                                    expandedMeal = false
                                }
                            )
                        }
                    }
                }

                // Meal Type Selector
                Box {
                    OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedType.name)
                    }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        MealType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Servings Input
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val servings = servingsText.toDoubleOrNull()
                    if (selectedMeal != null && servings != null) {
                        onConfirm(selectedMeal!!, selectedType, servings)
                    }
                },
                enabled = selectedMeal != null && servingsText.toDoubleOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}