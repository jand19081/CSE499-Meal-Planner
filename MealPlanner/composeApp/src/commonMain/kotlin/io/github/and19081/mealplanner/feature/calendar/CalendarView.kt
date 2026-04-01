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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.model.isIngredient
import io.github.and19081.mealplanner.domain.model.isMeal
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.feature.kitchen.ConsumptionResult
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.ui.components.ConsumeMealDialog
import io.github.and19081.mealplanner.ui.components.MealSourcePicker
import kotlin.collections.find
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.*

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
      },
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
            },
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
            },
        )
      }
    }
  }

  if (showEditDialog) {
    val entry = uiState.allEntries.find { it.id == selectedEntryId }
    EditMealDialog(
        entry = entry,
        initialDate = actualSelectedDate,
        availableMeals = uiState.allItems.filter { it.isMeal() },
        availableRecipes = uiState.allItems.filter { it.isRecipe() },
        availableIngredients = uiState.allItems.filter { it.isIngredient() },
        allRestaurants = uiState.allRestaurants,
        allUnits = uiState.allUnits,
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
                    anticipatedCostCents = cost,
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
            } else null,
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
        },
    )
  }

  if (showServeMealDialog && entryToConsumeId != null) {
    val entry = uiState.allEntries.find { it.id == entryToConsumeId }
    if (entry != null) {
      val initialResult =
          when (val src = entry.source) {
            is MealSource.StandaloneRecipe -> ConsumptionResult.HomeRecipeConsumed(entry.id)
            is MealSource.StandaloneIngredient -> ConsumptionResult.HomeIngredientConsumed(entry.id)
            is MealSource.Restaurant ->
                ConsumptionResult.RestaurantMealConsumed(entry.id, src.anticipatedCostCents)
            is MealSource.PrePlannedMeal,
            null ->
                if (entry.restaurantId != null)
                    ConsumptionResult.RestaurantMealConsumed(
                        entry.id,
                        entry.anticipatedCostCents ?: 0,
                    )
                else ConsumptionResult.HomeMealConsumed(entry.id)
          }
      val mealName =
          uiState.allItems.find { it.id == entry.prePlannedMealId }?.name
              ?: uiState.allRestaurants.find { it.id == entry.restaurantId }?.name
              ?: "Meal"
      ConsumeMealDialog(
          selectedResult = initialResult,
          mealName = mealName,
          onDismiss = { showServeMealDialog = false },
          onConfirm = { result ->
            viewModel.commitConsumptionResult(result)
            showServeMealDialog = false
          },
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
              contentDescription = "Toggle View Mode",
          )
        }
      },
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
            fontWeight = FontWeight.Bold,
        )
      }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(uiState.dates) { dateModel ->
        DateCell(
            dateModel = dateModel,
            onClick = { onDateClick(dateModel.date) },
            onLongClick = { onDateLongClick(dateModel.date) },
            onEventClick = onEventClick,
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
          modifier = Modifier.align(Alignment.End),
      )

      Spacer(modifier = Modifier.height(2.dp))

      dateModel.events.take(3).forEach { event ->
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .background(
                        getMealTypeColor(event.mealType).copy(alpha = 0.3f),
                        MaterialTheme.shapes.extraSmall,
                    )
                    .clickable { onEventClick(event.entryId) }
                    .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
          Box(
              modifier =
                  Modifier.size(if (event.isConsumed) 6.dp else 8.dp)
                      .background(
                          if (event.isConsumed) MaterialTheme.colorScheme.onSurfaceVariant
                          else getMealTypeColor(event.mealType),
                          MaterialTheme.shapes.extraSmall,
                      ),
          )
        }
      }

      if (dateModel.events.size > 3) {
        Text(
            text = "+${dateModel.events.size - 3} more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
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
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(uiState.weekDates) { dateModel ->
      Card(
          modifier = Modifier.fillMaxWidth(),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          colors =
              if (dateModel.isToday)
                  CardDefaults.cardColors(
                      containerColor =
                          MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                  )
              else CardDefaults.cardColors(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text =
                    "${dateModel.date.dayOfWeek.name}, ${dateModel.date.month.name} ${dateModel.date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                modifier = Modifier.padding(vertical = 8.dp),
            )
          } else {
            dateModel.events.forEach { event ->
              Row(
                  modifier =
                      Modifier.fillMaxWidth()
                          .clickable { onEventClick(event.entryId) }
                          .padding(vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                    when (event.mealType) {
                      RecipeMealType.Breakfast -> Icons.Default.CalendarViewDay
                      else -> Icons.Default.CalendarViewWeek
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
                  Text(
                      text = "${event.mealType.name} • ${event.peopleCount} servings",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline,
                  )
                  if (event.warnings.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          Icons.Default.Warning,
                          contentDescription = null,
                          modifier = Modifier.size(12.dp),
                          tint = MaterialTheme.colorScheme.error,
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                          "${event.warnings.size} warnings",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
                if (!event.isConsumed) {
                  IconButton(onClick = { onServeClick(event.entryId) }) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Mark Consumed",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                  }
                } else {
                  Icon(
                      Icons.Default.CheckCircle,
                      contentDescription = "Consumed",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(12.dp),
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
    availableRecipes: List<FoodItem>,
    availableIngredients: List<FoodItem>,
    allRestaurants: List<Restaurant>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave:
        (
            LocalDate,
            LocalTime,
            FoodItem?,
            io.github.and19081.mealplanner.feature.meals.Restaurant?,
            RecipeMealType,
            Int,
            Int?,
        ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
  var selectedDate by remember { mutableStateOf(entry?.date ?: initialDate) }
  var selectedTime by remember { mutableStateOf(entry?.time ?: LocalTime(18, 0)) }
  var mealType by remember { mutableStateOf(entry?.mealType ?: RecipeMealType.Dinner) }
  var peopleCount by remember { mutableIntStateOf(entry?.peopleCount ?: 4) }

  var anticipatedCost by remember {
    mutableStateOf(entry?.anticipatedCostCents?.let { (it / 100.0).toString() } ?: "")
  }
  var selectedQuantity by remember { mutableStateOf(0.0) }
  var selectedUnit by remember { mutableStateOf<UnitModel?>(null) }

  // Reconstruct initial MealSource from entry
  var selectedSource by remember {
    mutableStateOf<MealSource?>(
        when {
          entry?.restaurantId != null ->
              MealSource.Restaurant(
                  restaurantId = entry.restaurantId,
                  anticipatedCostCents = entry.anticipatedCostCents ?: 0,
              )
          entry?.prePlannedMealId != null -> {
            val item = (availableMeals + availableRecipes).find { it.id == entry.prePlannedMealId }
            when {
              item?.isMeal() == true -> MealSource.PrePlannedMeal(entry.prePlannedMealId)
              item?.isRecipe() == true -> MealSource.StandaloneRecipe(entry.prePlannedMealId)
              else -> null
            }
          }
          else -> null
        }
    )
  }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(if (entry == null) "Add Planned Meal" else "Edit Planned Meal") },
      text = {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Date: ${selectedDate}")
          }

          OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Time: %02d:%02d".format(selectedTime.hour, selectedTime.minute))
          }

          MealSourcePicker(
              selectedSource = selectedSource,
              onSourceSelected = { selectedSource = it },
              availableMeals = availableMeals,
              availableRecipes = availableRecipes,
              availableIngredients = availableIngredients,
              availableRestaurants = allRestaurants,
              selectedQuantity = selectedQuantity,
              onQuantityChange = { selectedQuantity = it },
              selectedUnit = selectedUnit,
              onUnitChange = { selectedUnit = it },
              anticipatedCost = anticipatedCost,
              onAnticipatedCostChange = { cost ->
                anticipatedCost = cost
                val src = selectedSource
                if (src is MealSource.Restaurant) {
                  selectedSource =
                      MealSource.Restaurant(
                          restaurantId = src.restaurantId,
                          anticipatedCostCents =
                              cost.toIntOrNull()?.times(100) ?: src.anticipatedCostCents,
                      )
                }
              },
          )

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
                },
            )
            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
              RecipeMealType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                      mealType = type
                      typeExpanded = false
                    },
                )
              }
            }
          }

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Servings:", modifier = Modifier.weight(1f))
            IconButton(onClick = { if (peopleCount > 1) peopleCount-- }) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease")
            }
            Text(
                peopleCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { peopleCount++ }) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase")
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              val src = selectedSource
              val saveMeal: FoodItem? =
                  when (src) {
                    is MealSource.PrePlannedMeal -> availableMeals.find { it.id == src.id }
                    is MealSource.StandaloneRecipe -> availableRecipes.find { it.id == src.id }
                    is MealSource.StandaloneIngredient ->
                        availableIngredients.find { it.id == src.id }
                    is MealSource.Restaurant,
                    null -> null
                  }
              val saveRestaurant: Restaurant? =
                  when (src) {
                    is MealSource.Restaurant -> allRestaurants.find { it.id == src.restaurantId }
                    else -> null
                  }
              val costCents: Int? =
                  when (src) {
                    is MealSource.Restaurant ->
                        anticipatedCost.toIntOrNull()?.times(100) ?: src.anticipatedCostCents
                    else -> null
                  }
              onSave(
                  selectedDate,
                  selectedTime,
                  saveMeal,
                  saveRestaurant,
                  mealType,
                  peopleCount,
                  costCents,
              )
            },
            enabled = selectedSource != null,
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        Row {
          if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
              Text("Delete")
            }
          }
          TextButton(onClick = onDismiss) { Text("Cancel") }
        }
      },
  )

  if (showDatePicker) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let {
                  selectedDate =
                      Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                }
                showDatePicker = false
              }
          ) {
            Text("OK")
          }
        },
        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false,
        )
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
              }
          ) {
            Text("OK")
          }
        },
        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        text = { TimePicker(state = timePickerState) },
    )
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
                          tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                  },
                  modifier = Modifier.clickable { onEventClick(event.entryId) },
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
      dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

/** Helper function to determine meal type color */
@Composable
private fun getMealTypeColor(mealType: RecipeMealType): Color {
  return when (mealType) {
    RecipeMealType.Breakfast -> MaterialTheme.colorScheme.primary
    RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
    RecipeMealType.Dinner -> MaterialTheme.colorScheme.tertiary
    RecipeMealType.Snack -> MaterialTheme.colorScheme.outline
    RecipeMealType.Side,
    RecipeMealType.Other -> MaterialTheme.colorScheme.surfaceVariant
  }
}
