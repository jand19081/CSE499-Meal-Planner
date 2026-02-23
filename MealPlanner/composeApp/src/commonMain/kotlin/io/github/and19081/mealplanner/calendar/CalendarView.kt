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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import io.github.and19081.mealplanner.PrePlannedMeal
import io.github.and19081.mealplanner.RecipeMealType
import io.github.and19081.mealplanner.ScheduledMeal
import io.github.and19081.mealplanner.Restaurant
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.RecipeIngredient
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.UnitModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import io.github.and19081.mealplanner.uicomponents.DialogActionButtons
import io.github.and19081.mealplanner.uicomponents.EmptyListMessage
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.domain.DataWarning
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone as KTimeZone
import kotlin.uuid.Uuid

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
    calendarViewMode: CalendarViewMode,
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<Uuid?>(null) }
    
    var showDayOverviewDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }

    var showConsumeRestaurantDialog by remember { mutableStateOf(false) }
    var entryToConsumeId by remember { mutableStateOf<Uuid?>(null) }

    val today = Clock.System.todayIn(KTimeZone.currentSystemDefault())

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val handleConsume = { id: Uuid ->
        val entry = uiState.allEntries.find { it.id == id }
        if (entry?.restaurantId != null) {
            entryToConsumeId = id
            showConsumeRestaurantDialog = true
        } else {
            viewModel.consumeMeal(id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedDateForDialog == null) {
                        viewModel.selectDate(today)
                        selectedDateForDialog = today
                    }
                    selectedEntryId = null
                    showEditDialog = true
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
                        onConsume = handleConsume,
                        onEditPlan = { id -> 
                            selectedEntryId = id
                            showEditDialog = true
                        }
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        events = uiState.dates.find { it.date == uiState.currentMonth }?.events ?: emptyList(),
                        onAddClick = {
                            selectedEntryId = null
                            showEditDialog = true
                        },
                        onConsume = handleConsume,
                        onEditPlan = { id -> 
                            selectedEntryId = id
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showConsumeRestaurantDialog && entryToConsumeId != null) {
        val entry = uiState.allEntries.find { it.id == entryToConsumeId }
        val restaurant = entry?.restaurantId?.let { rid -> uiState.allRestaurants.find { it.id == rid } }
        
        RestaurantConsumptionDialog(
            restaurantName = restaurant?.name ?: "Restaurant",
            onDismiss = { showConsumeRestaurantDialog = false },
            onConfirm = { total, tax, items ->
                viewModel.consumeRestaurantMeal(entryToConsumeId!!, total, tax, items)
                showConsumeRestaurantDialog = false
            }
        )
    }

    if (showEditDialog && selectedDateForDialog != null) {
        val entry = selectedEntryId?.let { id -> 
            uiState.allEntries.find { it.id == id }
        }

        ScheduledMealEditDialog(
            initialDate = selectedDateForDialog!!,
            entryId = selectedEntryId,
            availableMeals = uiState.availableMeals,
            allRecipes = uiState.allRecipes,
            allIngredients = uiState.allIngredients,
            allUnits = uiState.allUnits,
            allRestaurants = uiState.allRestaurants,
            existingEntry = entry,
            onDismiss = { showEditDialog = false },
            onConfirm = { date, time, meal, restaurant, mealType, count, cost ->
                if (selectedEntryId == null) {
                    viewModel.addPlan(date, time, meal, restaurant, mealType, count, cost)
                } else {
                    viewModel.updatePlan(ScheduledMeal(
                        id = selectedEntryId!!, 
                        date = date, 
                        time = time,
                        mealType = mealType, 
                        prePlannedMealId = meal?.id, 
                        restaurantId = restaurant?.id, 
                        peopleCount = count,
                        anticipatedCostCents = cost
                    ))
                }
                showEditDialog = false
            },
            onDelete = selectedEntryId?.let { id -> { 
                viewModel.removePlan(id)
                showEditDialog = false
            } }
        )
    }

    if (showDayOverviewDialog && selectedDateForDialog != null) {
        val dateUiModel = uiState.dates.find { it.date == selectedDateForDialog }
        val events = dateUiModel?.events ?: emptyList()
        val allWarnings = events.flatMap { it.warnings }.distinctBy { it.message }

        DayOverviewDialog(
            date = selectedDateForDialog!!,
            events = events,
            allWarnings = allWarnings,
            onDismiss = { showDayOverviewDialog = false },
            onAddClick = {
                showDayOverviewDialog = false
                selectedEntryId = null
                showEditDialog = true
            },
            onConsume = handleConsume,
            onEditPlan = { id ->
                showDayOverviewDialog = false
                selectedEntryId = id
                showEditDialog = true
            }
        )
    }
}

@Composable
fun DayView(events: List<CalendarEvent>, onAddClick: () -> Unit, onConsume: (Uuid) -> Unit, onEditPlan: (Uuid) -> Unit) {
    if (events.isEmpty()) {
        EmptyListMessage(message = "No meals planned for this day.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { event ->
                MealEventCard(event, onConsume, onEditPlan)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekView(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit,
    onConsume: (Uuid) -> Unit,
    onEditPlan: (Uuid) -> Unit
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
                    MealEventCard(event, onConsume, onEditPlan)
                }
            }
        }
    }
}

@Composable
fun MealEventCard(event: CalendarEvent, onConsume: (Uuid) -> Unit, onEditPlan: (Uuid) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEditPlan(event.entryId) },
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
                            RecipeMealType.Breakfast -> MaterialTheme.colorScheme.tertiary
                            RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
                            RecipeMealType.Dinner -> MaterialTheme.colorScheme.primary
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.mealType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (event.isConsumed) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                        if (event.warnings.isNotEmpty() && !event.isConsumed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Warning, 
                                "Data Quality Issue", 
                                tint = MaterialTheme.colorScheme.error, 
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isConsumed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "People: ${event.peopleCount}",
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
    val hasWarnings = dateModel.events.any { it.warnings.isNotEmpty() && !it.isConsumed }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, if (dateModel.isSelected) MaterialTheme.colorScheme.primary else borderColor)
    ) {
        Column(
            modifier = Modifier.padding(4.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
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
                if (hasWarnings) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.extraSmall)
                    )
                }
            }

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
                                    RecipeMealType.Breakfast -> MaterialTheme.colorScheme.tertiary
                                    RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
                                    RecipeMealType.Dinner -> MaterialTheme.colorScheme.primary
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
    allWarnings: List<DataWarning>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onConsume: (Uuid) -> Unit,
    onEditPlan: (Uuid) -> Unit
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
            Column {
                MpValidationWarning(warnings = allWarnings, modifier = Modifier.padding(bottom = 8.dp))
                
                if (events.isEmpty()) {
                    EmptyListMessage(
                        message = "No meals planned for this day.",
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(events) { event ->
                            MealEventCard(event, onConsume, onEditPlan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
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
fun ScheduledMealEditDialog(
    initialDate: LocalDate,
    entryId: Uuid?,
    availableMeals: List<PrePlannedMeal>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    allRestaurants: List<Restaurant>,
    existingEntry: ScheduledMeal?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalTime, PrePlannedMeal?, Restaurant?, RecipeMealType, Int, Int?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var isRestaurant by remember { mutableStateOf(existingEntry?.restaurantId != null) }
    
    var selectedMeal by remember { 
        mutableStateOf(existingEntry?.prePlannedMealId?.let { id -> 
            availableMeals.find { it.id == id } 
        } ?: availableMeals.firstOrNull()) 
    }
    
    var selectedRestaurant by remember {
        mutableStateOf(existingEntry?.restaurantId?.let { id ->
            allRestaurants.find { it.id == id }
        } ?: allRestaurants.firstOrNull())
    }

    var selectedType by remember { mutableStateOf(existingEntry?.mealType ?: RecipeMealType.Dinner) }
    var peopleText by remember { mutableStateOf(existingEntry?.peopleCount?.toString() ?: "4") }
    var costText by remember { mutableStateOf(existingEntry?.anticipatedCostCents?.let { (it / 100.0).toString() } ?: "") }
    var currentDisplayDate by remember { mutableStateOf(existingEntry?.date ?: initialDate) }
    var currentDisplayTime by remember { mutableStateOf(existingEntry?.time ?: LocalTime(18, 0)) }

    var expandedMeal by remember { mutableStateOf(false) }
    var expandedRestaurant by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    
    val initialDateMillis = currentDisplayDate.atStartOfDayIn(KTimeZone.UTC).toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis,
        initialDisplayMode = DisplayMode.Picker,
        yearRange = (initialDate.year - 10)..(initialDate.year + 10)
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = currentDisplayTime.hour,
        initialMinute = currentDisplayTime.minute
    )
    var showTimePicker by remember { mutableStateOf(false) }

    // Resolve ingredients for scaling
    val scaledIngredients = remember(isRestaurant, selectedMeal, peopleText) {
        if (isRestaurant) return@remember emptyList<String>()
        val count = peopleText.toIntOrNull() ?: 0
        if (selectedMeal == null || count <= 0) return@remember emptyList<String>()
        
        val ingredientsMap = mutableMapOf<Uuid, Double>()
        val unitsMap = mutableMapOf<Uuid, Uuid>()
        
        fun resolveRecipe(rId: Uuid, scale: Double) {
            val recipe = allRecipes.find { it.id == rId } ?: return
            val currentScale = if (recipe.servings > 0) scale / recipe.servings else scale
            
            recipe.ingredients.forEach { ri ->
                if (ri.subRecipeId != null) {
                    resolveRecipe(ri.subRecipeId, currentScale * ri.quantity)
                } else if (ri.ingredientId != null) {
                    ingredientsMap[ri.ingredientId] = (ingredientsMap[ri.ingredientId] ?: 0.0) + (ri.quantity * currentScale)
                    unitsMap[ri.ingredientId] = ri.unitId
                }
            }
        }

        selectedMeal!!.recipes.forEach { resolveRecipe(it, count.toDouble()) }
        selectedMeal!!.independentIngredients.forEach { mi ->
            ingredientsMap[mi.ingredientId] = (ingredientsMap[mi.ingredientId] ?: 0.0) + (mi.quantity * count)
            unitsMap[mi.ingredientId] = mi.unitId
        }

        ingredientsMap.map { (id, qty) ->
            val name = allIngredients.find { it.id == id }?.name ?: "Unknown"
            val unitName = allUnits.find { it.id == unitsMap[id] }?.abbreviation ?: "?"
            "$name: ${String.format("%.1f", qty)} $unitName"
        }.sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entryId == null) "Plan Meal" else "Edit Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Date: $currentDisplayDate")
                    }
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Time: $currentDisplayTime")
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isRestaurant,
                        onClick = { isRestaurant = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Home Meal") }
                    SegmentedButton(
                        selected = isRestaurant,
                        onClick = { isRestaurant = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Restaurant") }
                }
                
                if (!isRestaurant) {
                    // Meal Selector
                    Box {
                        OutlinedButton(onClick = { expandedMeal = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedMeal?.name ?: "Select Meal")
                        }
                        DropdownMenu(expanded = expandedMeal, onDismissRequest = { expandedMeal = false }) {
                            availableMeals.forEach { meal ->
                                DropdownMenuItem(
                                    text = { Text(meal.name) },
                                    onClick = { selectedMeal = meal; expandedMeal = false }
                                )
                            }
                        }
                    }
                } else {
                    // Restaurant Selector
                    Box {
                        OutlinedButton(onClick = { expandedRestaurant = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedRestaurant?.name ?: "Select Restaurant")
                        }
                        DropdownMenu(expanded = expandedRestaurant, onDismissRequest = { expandedRestaurant = false }) {
                            allRestaurants.forEach { restaurant ->
                                DropdownMenuItem(
                                    text = { Text(restaurant.name) },
                                    onClick = { selectedRestaurant = restaurant; expandedRestaurant = false }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedType.name)
                        }
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            RecipeMealType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = { selectedType = type; expandedType = false }
                                )
                            }
                        }
                    }
                    // Count
                    MpOutlinedTextField(
                        value = peopleText,
                        onValueChange = { peopleText = it },
                        label = { Text("People") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isRestaurant) {
                    MpOutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Anticipated Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!isRestaurant && scaledIngredients.isNotEmpty()) {
                    Text("Needed Ingredients:", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                            scaledIngredients.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButtons(
                onCancel = onDismiss,
                onSave = {
                    val count = peopleText.toIntOrNull()
                    val cost = costText.toDoubleOrNull()?.let { (it * 100).toInt() }
                    if (count != null) {
                        if (isRestaurant) {
                            if (selectedRestaurant != null) {
                                onConfirm(currentDisplayDate, currentDisplayTime, null, selectedRestaurant, selectedType, count, cost)
                            }
                        } else {
                            if (selectedMeal != null) {
                                onConfirm(currentDisplayDate, currentDisplayTime, selectedMeal, null, selectedType, count, cost)
                            }
                        }
                    }
                },
                saveEnabled = (if (isRestaurant) selectedRestaurant != null else selectedMeal != null) && peopleText.toIntOrNull() != null,
                saveLabel = if (entryId == null) "Add" else "Save",
                onDelete = onDelete
            )
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = { 
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            currentDisplayDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(KTimeZone.UTC).date
                        }
                        showDatePicker = false 
                    }
                ) { Text("Ok") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentDisplayTime = LocalTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("Ok") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTimePicker = false 
                }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun RestaurantConsumptionDialog(
    restaurantName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, List<Triple<String, Double, Int>>) -> Unit
) {
    var isItemized by remember { mutableStateOf(false) }
    var totalText by remember { mutableStateOf("") }
    var taxText by remember { mutableStateOf("") }
    
    // Items: Name, Qty, PriceCents
    val items = remember { mutableStateListOf<Triple<String, String, String>>() }
    
    val computedTotalCents = remember(items, taxText) {
        val itemsTotal = items.sumOf { (_, _, price) -> ((price.toDoubleOrNull() ?: 0.0) * 100).toInt() }
        val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
        itemsTotal + tax
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consume Meal at $restaurantName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isItemized,
                        onClick = { isItemized = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Total Only") }
                    SegmentedButton(
                        selected = isItemized,
                        onClick = { isItemized = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Itemized") }
                }

                if (!isItemized) {
                    MpOutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    MpOutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("Tax ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    MpOutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("Tax ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Items:", style = MaterialTheme.typography.labelMedium)
                    
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        items.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MpOutlinedTextField(
                                    value = item.first,
                                    onValueChange = { items[index] = item.copy(first = it) },
                                    label = { Text("Name") },
                                    modifier = Modifier.weight(2f)
                                )
                                MpOutlinedTextField(
                                    value = item.third,
                                    onValueChange = { items[index] = item.copy(third = it) },
                                    label = { Text("$") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { items.removeAt(index) }) {
                                    Icon(Icons.Default.Close, "Remove")
                                }
                            }
                        }
                        
                        TextButton(onClick = { items.add(Triple("", "1.0", "")) }) {
                            Icon(Icons.Default.Add, null)
                            Text("Add Item")
                        }
                    }
                    
                    Text(
                        "Computed Total: $${String.format("%.2f", computedTotalCents / 100.0)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButtons(
                onCancel = onDismiss,
                onSave = {
                    if (isItemized) {
                        val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                        val finalItems = items.map { (name, qty, price) ->
                            Triple(name, qty.toDoubleOrNull() ?: 1.0, ((price.toDoubleOrNull() ?: 0.0) * 100).toInt())
                        }
                        onConfirm(computedTotalCents, tax, finalItems)
                    } else {
                        val total = ((totalText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                        val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                        onConfirm(total, tax, emptyList())
                    }
                },
                saveLabel = "Consume",
                saveEnabled = if (isItemized) true else totalText.toDoubleOrNull() != null
            )
        }
    )
}
