@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.feature.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.domain.model.FoodItem
import kotlin.math.*
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.*
import kotlin.collections.find

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
    calendarViewMode: CalendarViewMode,
    mode: Mode,
    isExpanded: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleViewMode: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTransaction: (KitchenTransaction, (KitchenTransaction) -> Unit) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<Uuid?>(null) }

    var showDayOverviewDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(today) }

    val actualSelectedDate = uiState.dates.find { it.isSelected }?.date ?: today

    var showServeMealDialog by remember { mutableStateOf(false) }
    var entryToConsumeId by remember { mutableStateOf<Uuid?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalendarTopBar(
                title =
                    if (calendarViewMode == CalendarViewMode.MONTH) {
                        "${uiState.currentMonth.month.name} ${uiState.currentMonth.year}"
                    } else {
                        "Week View"
                    },
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onToggleViewMode = onToggleViewMode,
                viewMode = calendarViewMode,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedEntryId = null
                    showEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (calendarViewMode == CalendarViewMode.MONTH) {
                MonthViewGrid(
                    uiState = uiState,
                    onDateClick = onDateSelected,
                    onEventClick = { entryId ->
                        selectedEntryId = entryId
                        showEditDialog = true
                    },
                    onDateLongClick = { date ->
                        selectedDateForDialog = date
                        showDayOverviewDialog = true
                    }
                )
            } else {
                WeekViewList(
                    uiState = uiState,
                    onEventClick = { entryId ->
                        selectedEntryId = entryId
                        showEditDialog = true
                    },
                    onServeClick = { entryId ->
                        entryToConsumeId = entryId
                        showServeMealDialog = true
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        val entry = uiState.allEntries.find { it.id == selectedEntryId }
        EditMealDialog(
            entry = entry,
            initialDate = actualSelectedDate,
            availableMeals = uiState.availableMeals,
            allRestaurants = uiState.allRestaurants,
            onDismiss = { showEditDialog = false },
            onSave = { date, time, meal, restaurant, type, count, cost ->
                if (entry == null) {
                    viewModel.addPlan(date, time, meal, restaurant, type, count, cost)
                } else {
                    viewModel.updatePlan(
                        entry.copy(
                            date = date,
                            time = time,
                            prePlannedMealId = meal?.id,
                            restaurantId = restaurant?.id,
                            mealType = type,
                            peopleCount = count,
                            anticipatedCostCents = cost
                        )
                    )
                }
                showEditDialog = false
            },
            onDelete =
                if (entry != null) {
                    {
                        viewModel.removePlan(entry.id)
                        showEditDialog = false
                    }
                } else null
        )
    }

    if (showDayOverviewDialog && selectedDateForDialog != null) {
        DayOverviewDialog(
            date = selectedDateForDialog!!,
            events = uiState.dates.find { it.date == selectedDateForDialog }?.events ?: emptyList(),
            onDismiss = { showDayOverviewDialog = false },
            onEventClick = { entryId ->
                selectedEntryId = entryId
                showEditDialog = true
                showDayOverviewDialog = false
            },
            onAddEvent = {
                selectedEntryId = null
                showEditDialog = true
                showDayOverviewDialog = false
            },
            onServeEvent = { entryId ->
                entryToConsumeId = entryId
                showServeMealDialog = true
                showDayOverviewDialog = false
            }
        )
    }

    if (showServeMealDialog && entryToConsumeId != null) {
        val entry = uiState.allEntries.find { it.id == entryToConsumeId }
        if (entry != null) {
            ServeMealDialog(
                entry = entry,
                allMeals = uiState.availableMeals,
                allItems = uiState.allItems,
                onDismiss = { showServeMealDialog = false },
                onConfirm = { entryId, leftovers ->
                    val transaction = viewModel.createConsumptionTransaction(entry)
                    if (transaction != null) {
                        onTransaction(transaction) { finalizedTx ->
                            viewModel.commitTransaction(entryId, finalizedTx, leftovers)
                        }
                    } else {
                        viewModel.toggleMealConsumption(entryId)
                    }
                    showServeMealDialog = false
                }
            )
        }
    }
}

@Composable
fun CalendarTopBar(
    title: String,
    viewMode: CalendarViewMode,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onPrevClick) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
        },
        actions = {
            IconButton(onClick = onNextClick) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    if (viewMode == CalendarViewMode.MONTH) Icons.Default.CalendarViewWeek
                    else Icons.Default.CalendarMonth,
                    contentDescription = "Toggle View Mode"
                )
            }
        }
    )
}

@Composable
fun MonthViewGrid(
    uiState: CalendarUiState,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Uuid) -> Unit,
    onDateLongClick: (LocalDate) -> Unit,
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            dayNames.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.dates) { dateModel ->
                DateCell(
                    dateModel = dateModel,
                    onClick = { onDateClick(dateModel.date) },
                    onLongClick = { onDateLongClick(dateModel.date) },
                    onEventClick = onEventClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateCell(
    dateModel: CalendarUiState.DateUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEventClick: (Uuid) -> Unit,
) {
    val backgroundColor =
        when {
            dateModel.isSelected -> MaterialTheme.colorScheme.primaryContainer
            dateModel.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            !dateModel.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        }

    val textColor =
        if (dateModel.isCurrentMonth) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier =
            Modifier.aspectRatio(0.8f)
                .background(backgroundColor, MaterialTheme.shapes.small)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(4.dp)
    ) {
        Column {
            Text(
                text = dateModel.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (dateModel.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(2.dp))

            dateModel.events.take(3).forEach { event ->
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .background(
                                getMealTypeColor(event.mealType).copy(alpha = 0.3f),
                                MaterialTheme.shapes.extraSmall
                            )
                            .clickable { onEventClick(event.entryId) }
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (event.isConsumed) 6.dp else 8.dp)
                            .background(
                                if (event.isConsumed)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else getMealTypeColor(event.mealType),
                                MaterialTheme.shapes.extraSmall
                            ),
                        contentAlignment = Alignment.Center
                    )
                }
            }

            if (dateModel.events.size > 3) {
                Text(
                    text = "+${dateModel.events.size - 3} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun WeekViewList(
    uiState: CalendarUiState,
    onEventClick: (Uuid) -> Unit,
    onServeClick: (Uuid) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(uiState.weekDates) { dateModel ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors =
                    if (dateModel.isToday) CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                            alpha = 0.3f
                        )
                    )
                    else CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${dateModel.date.dayOfWeek.name}, ${dateModel.date.month.name} ${dateModel.date.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (dateModel.isToday) {
                            SuggestionChip(onClick = {}, label = { Text("Today") })
                        }
                    }

                    if (dateModel.events.isEmpty()) {
                        Text(
                            "No meals planned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        dateModel.events.forEach { event ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable { onEventClick(event.entryId) }
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (event.mealType) {
                                        RecipeMealType.Breakfast -> Icons.Default.CalendarViewDay
                                        else -> Icons.Default.CalendarViewWeek
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${event.mealType.name} • ${event.peopleCount} servings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (event.warnings.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${event.warnings.size} warnings",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                if (!event.isConsumed) {
                                    IconButton(onClick = { onServeClick(event.entryId) }) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Mark Consumed",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Consumed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditMealDialog(
    entry: ScheduledMeal?,
    initialDate: LocalDate,
    availableMeals: List<FoodItem>,
    allRestaurants: List<Restaurant>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalTime, FoodItem?, io.github.and19081.mealplanner.feature.meals.Restaurant?, RecipeMealType, Int, Int?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var selectedDate by remember { mutableStateOf(entry?.date ?: initialDate) }
    var selectedTime by remember { mutableStateOf(entry?.time ?: LocalTime(18, 0)) }
    var selectedMeal by remember {
        mutableStateOf(availableMeals.find { it.id == entry?.prePlannedMealId })
    }
    var selectedRestaurant by remember {
        mutableStateOf(allRestaurants.find { it.id == entry?.restaurantId })
    }
    var mealType by remember { mutableStateOf(entry?.mealType ?: RecipeMealType.Dinner) }
    var peopleCount by remember { mutableIntStateOf(entry?.peopleCount ?: 4) }
    var anticipatedCostStr by remember {
        mutableStateOf(entry?.anticipatedCostCents?.let { (it / 100.0).toString() } ?: "")
    }

    var isRestaurantMode by remember { mutableStateOf(entry?.restaurantId != null) }

    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Add Planned Meal" else "Edit Planned Meal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isRestaurantMode,
                        onClick = { isRestaurantMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Home")
                    }
                    SegmentedButton(
                        selected = isRestaurantMode,
                        onClick = { isRestaurantMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Out")
                    }
                }

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: ${selectedDate}")
                }

                if (!isRestaurantMode) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = selectedMeal?.name ?: "Select a Meal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Meal") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            availableMeals.forEach { meal ->
                                DropdownMenuItem(
                                    text = { Text(meal.name) },
                                    onClick = {
                                        selectedMeal = meal
                                        selectedRestaurant = null
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = selectedRestaurant?.name ?: "Select a Restaurant",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Restaurant") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            allRestaurants.forEach { rest ->
                                DropdownMenuItem(
                                    text = { Text(rest.name) },
                                    onClick = {
                                        selectedRestaurant = rest
                                        selectedMeal = null
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                var typeExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = mealType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { typeExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        RecipeMealType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    mealType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Servings:", modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (peopleCount > 1) peopleCount-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease")
                    }
                    Text(
                        peopleCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { peopleCount++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase")
                    }
                }

                OutlinedTextField(
                    value = anticipatedCostStr,
                    onValueChange = { anticipatedCostStr = it },
                    label = { Text("Anticipated Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val costCents = (anticipatedCostStr.toDoubleOrNull()?.let { it * 100 })?.toInt()
                    onSave(
                        selectedDate,
                        selectedTime,
                        selectedMeal,
                        selectedRestaurant,
                        mealType,
                        peopleCount,
                        costCents
                    )
                },
                enabled = (selectedMeal != null || selectedRestaurant != null)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DayOverviewDialog(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onEventClick: (Uuid) -> Unit,
    onAddEvent: () -> Unit,
    onServeEvent: (Uuid) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${date.dayOfWeek.name}, ${date.dayOfMonth} ${date.month.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) {
                    Text("No meals planned for this day.")
                } else {
                    events.forEach { event ->
                        ListItem(
                            headlineContent = { Text(event.title) },
                            supportingContent = { Text("${event.mealType.name} • ${event.peopleCount} ppl") },
                            trailingContent = {
                                if (!event.isConsumed) {
                                    IconButton(onClick = { onServeEvent(event.entryId) }) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Consume")
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onEventClick(event.entryId) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAddEvent) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ServeMealDialog(
    entry: io.github.and19081.mealplanner.feature.meals.ScheduledMeal,
    allMeals: List<FoodItem>,
    allItems: List<FoodItem>,
    onDismiss: () -> Unit,
    onConfirm: (Uuid, List<Pair<Uuid, Double>>) -> Unit,
) {
    val meal = allMeals.find { it.id == entry.prePlannedMealId }

    var leftovers by remember {
        mutableStateOf(
            if (meal != null) listOf(meal.id to 0.0) else emptyList<Pair<Uuid, Double>>()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Serve Meal: ${meal?.name ?: "Restaurant"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("This will deduct ingredients from your pantry based on the recipe.")

                if (meal != null) {
                    HorizontalDivider()
                    Text("Leftovers?", style = MaterialTheme.typography.titleSmall)

                    leftovers.forEachIndexed { index, pair ->
                        val leftoverMeal = allItems.find { it.id == pair.first }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(leftoverMeal?.name ?: "Unknown", modifier = Modifier.weight(1f))

                            IconButton(onClick = {
                                leftovers = leftovers.mapIndexed { i, p ->
                                    if (i == index) p.first to max(0.0, p.second - 0.5) else p
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                            }
                            Text(pair.second.toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                leftovers = leftovers.mapIndexed { i, p ->
                                    if (i == index) p.first to (p.second + 0.5) else p
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                            Text("servings")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(entry.id, leftovers.filter { it.second > 0 }) }) {
                Text("Serve & Mark Consumed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Helper function to determine meal type color
 */
@Composable
private fun getMealTypeColor(mealType: RecipeMealType): Color {
    return when (mealType) {
        RecipeMealType.Breakfast -> MaterialTheme.colorScheme.primary
        RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
        RecipeMealType.Dinner -> MaterialTheme.colorScheme.tertiary
        RecipeMealType.Snack -> MaterialTheme.colorScheme.outline
    }
}
