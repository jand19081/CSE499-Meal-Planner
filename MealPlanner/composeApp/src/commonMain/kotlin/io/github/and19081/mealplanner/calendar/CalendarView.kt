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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import io.github.and19081.mealplanner.DialogActionButtons
import io.github.and19081.mealplanner.EmptyListMessage
import io.github.and19081.mealplanner.ListSectionHeader
import kotlin.time.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone as KTimeZone
import kotlin.uuid.Uuid
import io.github.and19081.mealplanner.MpButton
import io.github.and19081.mealplanner.MpTextButton
import io.github.and19081.mealplanner.MpOutlinedButton
import io.github.and19081.mealplanner.MpOutlinedTextField
import io.github.and19081.mealplanner.MpCard
import io.github.and19081.mealplanner.MpFloatingActionButton
import io.github.and19081.mealplanner.MpSurface

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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            MpFloatingActionButton(
                onClick = {
                    if (selectedDateForDialog == null) {
                        viewModel.selectDate(today)
                        selectedDateForDialog = today
                    }
                    showAddDialog = true
                },
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
                        },
                        onConsume = { viewModel.consumeMeal(it) }
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        events = uiState.dates.find { it.date == uiState.currentMonth }?.events ?: emptyList(),
                        onAddClick = {
                            showDayOverviewDialog = false
                            showAddDialog = true
                        },
                        onConsume = { viewModel.consumeMeal(it) }
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
            },
            onConsume = { viewModel.consumeMeal(it) }
        )
    }
}

@Composable
fun DayView(events: List<CalendarEvent>, onAddClick: () -> Unit, onConsume: (Uuid) -> Unit) {
    if (events.isEmpty()) {
        EmptyListMessage(message = "No meals planned for this day.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { event ->
                MealEventCard(event, onConsume)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekView(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit,
    onConsume: (Uuid) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        dates.forEach { dateModel ->
            stickyHeader {
                ListSectionHeader(
                    text = "${dateModel.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} ${dateModel.date.day}"
                )
            }
            if (dateModel.events.isEmpty()) {
                item {
                    EmptyListMessage(
                        message = "No meals planned for this day.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(dateModel.events) { event ->
                    MealEventCard(event, onConsume)
                }
            }
        }
    }
}

@Composable
fun MealEventCard(event: CalendarEvent, onConsume: (Uuid) -> Unit) {
    MpCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isConsumed) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        if (event.isConsumed) Color.Gray 
                        else when(event.mealType) {
                            MealType.BREAKFAST -> MaterialTheme.colorScheme.tertiary
                            MealType.LUNCH -> MaterialTheme.colorScheme.secondary
                            MealType.DINNER -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
            )

            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.mealType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.isConsumed) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isConsumed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Serves: ${event.servings}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!event.isConsumed) {
                    IconButton(onClick = { onConsume(event.entryId) }) {
                        Icon(Icons.Default.CheckCircle, "Consume", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                     Icon(Icons.Default.CheckCircle, "Consumed", tint = Color.Gray)
                }
            }
        }
    }
}


@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        val days = DayOfWeek.entries.sortedBy { it.ordinal }
        days.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(3),
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
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
    val containerColor = when {
        dateModel.isSelected -> MaterialTheme.colorScheme.primaryContainer
        dateModel.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (dateModel.isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant

    MpCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, if (dateModel.isSelected) MaterialTheme.colorScheme.primary else borderColor)
    ) {
        Column(
            modifier = Modifier.padding(4.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateModel.date.day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (dateModel.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = when {
                    !dateModel.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    dateModel.isToday -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dateModel.events.take(3).forEach { event ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (event.isConsumed) MaterialTheme.colorScheme.outline 
                                else when(event.mealType) {
                                    MealType.BREAKFAST -> MaterialTheme.colorScheme.tertiary
                                    MealType.LUNCH -> MaterialTheme.colorScheme.secondary
                                    MealType.DINNER -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                }
                if (dateModel.events.size > 3) {
                    Text("+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun DayOverviewDialog(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onConsume: (Uuid) -> Unit
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
                EmptyListMessage(
                    message = "No meals planned for this day.",
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(events) { event ->
                        MealEventCard(event, onConsume)
                    }
                }
            }
        },
        confirmButton = {
            MpTextButton(
                onClick = onAddClick
            ) {
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
    // Store current display date in state so it updates when Picker confirms
    var currentDisplayDate by remember { mutableStateOf(date) }

    var expandedMeal by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val initialDateMillis = currentDisplayDate.atStartOfDayIn(KTimeZone.currentSystemDefault()).toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis,
        initialDisplayMode = DisplayMode.Picker,
        yearRange = (date.year - 10)..(date.year + 10)
    )
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Meal for $currentDisplayDate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MpButton(
                    onClick = { showDatePicker = true }
                ) {
                    Text("Select Date: $currentDisplayDate")
                }
                // Meal Selector
                Box {
                    MpOutlinedButton(
                        onClick = { expandedMeal = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    MpOutlinedButton(
                        onClick = { expandedType = true }, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                MpOutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            DialogActionButtons(
                onCancel = onDismiss,
                onSave = {
                    val servings = servingsText.toDoubleOrNull()
                    // Use state date
                    if (selectedMeal != null && servings != null) {
                        onConfirm(currentDisplayDate, selectedMeal!!, selectedType, servings)
                    }
                },
                saveEnabled = selectedMeal != null && servingsText.toDoubleOrNull() != null,
                saveLabel = "Add"
            )
        },
        dismissButton = {}
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                MpTextButton(
                    onClick = { 
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            currentDisplayDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(KTimeZone.currentSystemDefault()).date
                        }
                        showDatePicker = false 
                    }
                ) {
                    Text("Ok")
                }
            },
            dismissButton = {
                MpTextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}