@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import kotlin.time.Instant
import kotlinx.datetime.toLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import androidx.compose.material3.Button
import kotlinx.datetime.TimeZone as KTimeZone

@Composable
fun CalendarView(
    currentMonthFlow: StateFlow<LocalDate>,
    calendarViewMode: CalendarViewMode,
) {
    val viewModel = viewModel { CalendarViewModel(currentMonthFlow) }
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDayOverviewDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }

    val today = Clock.System.todayIn(KTimeZone.currentSystemDefault())

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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(4.dp)) {
            when (calendarViewMode) {
                CalendarViewMode.MONTH -> {
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
                CalendarViewMode.WEEK -> {
                    WeekView(
                        dates = uiState.weekDates,
                        onDateClick = { dateModel ->
                            viewModel.selectDate(dateModel.date)
                            selectedDateForDialog = dateModel.date
                            showDayOverviewDialog = true
                        }
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        events = uiState.dates.find { it.date == uiState.currentMonth }?.events ?: emptyList(),
                        onAddClick = {
                            showDayOverviewDialog = false
                            showAddDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog && selectedDateForDialog != null) {
        AddPlanDialog(
            date = selectedDateForDialog!!,
            availableMeals = uiState.availableMeals,
            onDismiss = { showAddDialog = false },
            onConfirm = { date, meal, mealType, servings ->
                viewModel.addPlan(date, meal, mealType, servings)
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
fun DayView(events: List<CalendarEvent>, onAddClick: () -> Unit) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No meals planned for this day.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekView(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        dates.forEach { dateModel ->
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${dateModel.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} ${dateModel.date.day}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (dateModel.events.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No meals planned for this day.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(dateModel.events) { event ->
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
    }
}




@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = DayOfWeek.entries.sortedBy { it.ordinal }
        days.forEach { day ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = day.name.take(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CalendarGrid(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(dates) { dateModel ->
            CalendarCell(
                dateModel = dateModel,
                modifier = Modifier.aspectRatio(1f),
                onClick = { onDateClick(dateModel) }
            )
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
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
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
    onConfirm: (LocalDate, Meal, MealType, Double) -> Unit
) {
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var selectedType by remember { mutableStateOf(MealType.DINNER) }
    var servingsText by remember { mutableStateOf("4") }

    var expandedMeal by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val initialDateMillis = date.atStartOfDayIn(KTimeZone.currentSystemDefault()).toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis,
        initialDisplayMode = DisplayMode.Picker,
        yearRange = (date.year - 10)..(date.year + 10)
    )
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Meal for ${datePickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(KTimeZone.currentSystemDefault()).date } ?: date}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDatePicker = true }) {
                    Text("Select Date: ${datePickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(KTimeZone.currentSystemDefault()).date } ?: date}")
                }
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
                    val selectedDate = datePickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(KTimeZone.currentSystemDefault()).date } ?: date
                    if (selectedMeal != null && servings != null) {
                        onConfirm(selectedDate, selectedMeal!!, selectedType, servings)
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Ok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
