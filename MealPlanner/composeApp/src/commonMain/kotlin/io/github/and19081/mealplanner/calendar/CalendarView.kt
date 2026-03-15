@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.PrePlannedMeal
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.RecipeMealType
import io.github.and19081.mealplanner.Restaurant
import io.github.and19081.mealplanner.ScheduledMeal
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.settings.Mode
import io.github.and19081.mealplanner.uicomponents.DialogActionButtons
import io.github.and19081.mealplanner.uicomponents.EmptyListMessage
import io.github.and19081.mealplanner.uicomponents.ListSectionHeader
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import kotlin.math.*
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone as KTimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

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

  val today = Clock.System.todayIn(KTimeZone.currentSystemDefault())

  var showEditDialog by remember { mutableStateOf(false) }
  var selectedEntryId by remember { mutableStateOf<Uuid?>(null) }

  var showDayOverviewDialog by remember { mutableStateOf(false) }
  var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(today) }

  val actualSelectedDate = uiState.dates.find { it.isSelected }?.date ?: today

  var showServeMealDialog by remember { mutableStateOf(false) }
  var entryToConsumeId by remember { mutableStateOf<Uuid?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  val handleConsume: (Uuid) -> Unit = { id ->
    val entry = uiState.allEntries.find { it.id == id }
    if (entry != null) {
      if (entry.isConsumed) {
        viewModel.toggleMealConsumption(id)
      } else {
        entryToConsumeId = id
        showServeMealDialog = true
      }
    }
  }

  val handlePrep: (Uuid) -> Unit = { id ->
    val entry = uiState.allEntries.find { it.id == id }
    if (entry != null && entry.restaurantId == null) {
      val transaction = viewModel.createConsumptionTransaction(entry)
      if (transaction != null) {
        onTransaction(transaction.copy(title = "Prepping: ${transaction.title.removePrefix("Consuming: ")}")) { committed ->
          viewModel.commitTransaction(id, committed)
        }
      }
    }
  }


  val selectDay: (LocalDate) -> Unit = { date ->
    viewModel.selectDate(date)
    selectedDateForDialog = date
    onDateSelected(date)
  }

  val actualIsExpanded =
      when (mode) {
        Mode.AUTO -> isExpanded
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
      }

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              val date = selectedDateForDialog ?: today
              if (selectedDateForDialog == null) {
                selectDay(date)
              }
              selectedEntryId = null
              showEditDialog = true
            },
        ) {
          Icon(Icons.Default.Add, contentDescription = "Plan Meal")
        }
      },
  ) { innerPadding ->
    if (actualIsExpanded) {
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(4.dp)) {
        // Main Content Area (Calendar/Week/Day)
        Column(modifier = Modifier.weight(0.6f)) {
          CalendarHeader(
              currentMonth = uiState.currentMonth,
              calendarViewMode = calendarViewMode,
              onPrevClick = onPrevClick,
              onNextClick = onNextClick,
              onToggleViewMode = onToggleViewMode,
          )

          when (calendarViewMode) {
            CalendarViewMode.MONTH -> {
              DaysOfWeekHeader()
              Spacer(modifier = Modifier.height(8.dp))
              CalendarGrid(
                  dates = uiState.dates,
                  onDateClick = { dateModel -> selectDay(dateModel.date) },
              )
            }
            CalendarViewMode.WEEK -> {
              WeekView(
                  dates = uiState.weekDates,
                  onDateClick = { dateModel -> selectDay(dateModel.date) },
                  onConsume = handleConsume,
                  onEditPlan = { id ->
                    selectedEntryId = id
                    // In desktop mode, we just set the ID and it shows in side panel
                  },
                  onPrep = handlePrep,
              )
            }
            CalendarViewMode.DAY -> {
              DayView(
                  events =
                      uiState.dates.find { it.date == actualSelectedDate }?.events ?: emptyList(),
                  onAddClick = {
                    selectedEntryId = null
                    showEditDialog = true // Trigger edit state
                  },
                  onConsume = handleConsume,
                  onEditPlan = { id -> selectedEntryId = id },
                  onPrep = handlePrep,
              )
            }
          }
        }

        VerticalDivider(modifier = Modifier.width(1.dp).padding(horizontal = 8.dp))

        // Detail Side Panel
        Column(modifier = Modifier.weight(0.4f)) {
          val date = selectedDateForDialog ?: today
          val entryId = selectedEntryId

          if (entryId != null || (showEditDialog && actualIsExpanded)) {
            // Show Edit Form in Side Panel
            val entry = entryId?.let { id -> uiState.allEntries.find { it.id == id } }

            ScheduledMealForm(
                initialDate = date,
                entryId = entryId,
                availableMeals = uiState.availableMeals,
                allRecipes = uiState.allRecipes,
                allIngredients = uiState.allIngredients,
                allUnits = uiState.allUnits,
                allRestaurants = uiState.allRestaurants,
                existingEntry = entry,
                onClose = {
                  selectedEntryId = null
                  showEditDialog = false
                },
                onConfirm = { confirmDate, time, meal, restaurant, mealType, count, cost ->
                  if (entryId == null) {
                    viewModel.addPlan(confirmDate, time, meal, restaurant, mealType, count, cost)
                  } else {
                    viewModel.updatePlan(
                        ScheduledMeal(
                            id = entryId,
                            date = confirmDate,
                            time = time,
                            mealType = mealType,
                            prePlannedMealId = meal?.id,
                            restaurantId = restaurant?.id,
                            peopleCount = count,
                            anticipatedCostCents = cost,
                        )
                    )
                  }
                  selectedEntryId = null
                  showEditDialog = false
                },
                onDelete =
                    entryId?.let { id ->
                      {
                        viewModel.removePlan(id)
                        selectedEntryId = null
                        showEditDialog = false
                      }
                    },
            )
          } else {
            // Show Daily List in Side Panel
            val dateUiModel = uiState.dates.find { it.date == date }
            val events = dateUiModel?.events ?: emptyList()
            val allWarnings = events.flatMap { it.warnings }.distinctBy { it.message }

            Text(
                text = "Meals for $date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
            )

            MpValidationWarning(warnings = allWarnings, modifier = Modifier.padding(8.dp))

            if (events.isEmpty()) {
              EmptyListMessage(
                  message = "No meals planned for this day.",
                  modifier = Modifier.fillMaxWidth().height(200.dp),
              )
            } else {
              LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events) { event ->
                  MealEventCard(event, handleConsume, { id -> selectedEntryId = id }, handlePrep)
                }
              }
            }
          }
        }
      }
    } else {
      // Mobile View (Single Column)
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(4.dp)) {
        CalendarHeader(
            currentMonth = uiState.currentMonth,
            calendarViewMode = calendarViewMode,
            onPrevClick = onPrevClick,
            onNextClick = onNextClick,
            onToggleViewMode = onToggleViewMode,
        )
        when (calendarViewMode) {
          CalendarViewMode.MONTH -> {
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))
            CalendarGrid(
                dates = uiState.dates,
                onDateClick = { dateModel ->
                  selectDay(dateModel.date)
                  showDayOverviewDialog = true
                },
            )
          }
          CalendarViewMode.WEEK -> {
            WeekView(
                dates = uiState.weekDates,
                onDateClick = { dateModel ->
                  selectDay(dateModel.date)
                  showDayOverviewDialog = true
                },
                onConsume = handleConsume,
                onEditPlan = { id ->
                  selectedEntryId = id
                  showEditDialog = true
                },
                onPrep = handlePrep,
            )
          }
          CalendarViewMode.DAY -> {
            DayView(
                events =
                    uiState.dates.find { it.date == actualSelectedDate }?.events ?: emptyList(),
                onAddClick = {
                  selectedEntryId = null
                  showEditDialog = true
                },
                onConsume = handleConsume,
                onEditPlan = { id ->
                  selectedEntryId = id
                  showEditDialog = true
                },
                onPrep = handlePrep,
            )
          }
        }
      }
    }
  }

  if (showServeMealDialog && entryToConsumeId != null) {
    val entry = uiState.allEntries.find { it.id == entryToConsumeId }
    val restaurant =
        entry?.restaurantId?.let { rid -> uiState.allRestaurants.find { it.id == rid } }
    val prePlannedMeal =
        entry?.prePlannedMealId?.let { mid -> uiState.availableMeals.find { it.id == mid } }

    ServeMealDialog(
        mealName = restaurant?.name ?: prePlannedMeal?.name ?: "Meal",
        isRestaurant = entry?.restaurantId != null,
        prePlannedMeal = prePlannedMeal,
        allRecipes = uiState.allRecipes,
        peopleCount = entry?.peopleCount ?: 1,
        onDismiss = { showServeMealDialog = false },
        onConfirmRestaurant = { total, tax, items ->
          entryToConsumeId?.let { id -> viewModel.consumeMeal(id, total, tax, items) }
          showServeMealDialog = false
        },
        onConfirmPrePlanned = { leftovers ->
          entryToConsumeId?.let { id ->
            if (entry != null) {
              val transaction = viewModel.createConsumptionTransaction(entry)
              if (transaction != null) {
                onTransaction(transaction) { committed ->
                  viewModel.commitTransaction(id, committed, leftovers)
                }
              } else {
                viewModel.consumeMeal(id, leftovers = leftovers)
              }
            }
          }
          showServeMealDialog = false
        },
    )
  }

  if (showEditDialog && !actualIsExpanded) {
    val date = selectedDateForDialog
    val entryId = selectedEntryId
    if (date != null) {
      val entry = entryId?.let { id -> uiState.allEntries.find { it.id == id } }

      ScheduledMealEditDialog(
          initialDate = date,
          entryId = entryId,
          availableMeals = uiState.availableMeals,
          allRecipes = uiState.allRecipes,
          allIngredients = uiState.allIngredients,
          allUnits = uiState.allUnits,
          allRestaurants = uiState.allRestaurants,
          existingEntry = entry,
          onDismiss = { showEditDialog = false },
          onConfirm = { confirmDate, time, meal, restaurant, mealType, count, cost ->
            if (entryId == null) {
              viewModel.addPlan(confirmDate, time, meal, restaurant, mealType, count, cost)
            } else {
              viewModel.updatePlan(
                  ScheduledMeal(
                      id = entryId,
                      date = confirmDate,
                      time = time,
                      mealType = mealType,
                      prePlannedMealId = meal?.id,
                      restaurantId = restaurant?.id,
                      peopleCount = count,
                      anticipatedCostCents = cost,
                  )
              )
            }
            showEditDialog = false
          },
          onDelete =
              entryId?.let { id ->
                {
                  viewModel.removePlan(id)
                  showEditDialog = false
                }
              },
      )
    }
  }

  val dateForOverview = selectedDateForDialog
  if (showDayOverviewDialog && dateForOverview != null) {
    val dateUiModel = uiState.dates.find { it.date == dateForOverview }
    val events = dateUiModel?.events ?: emptyList()
    val allWarnings = events.flatMap { it.warnings }.distinctBy { it.message }

    DayOverviewDialog(
        date = dateForOverview,
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
        },
        onPrep = handlePrep,
    )
  }
}

@Composable
fun CalendarHeader(
    currentMonth: LocalDate,
    calendarViewMode: CalendarViewMode,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    IconButton(onClick = onPrevClick) {
      Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
    }

    val headerText =
        when (calendarViewMode) {
          CalendarViewMode.MONTH -> {
            "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}"
          }
          CalendarViewMode.WEEK -> {
            "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.day} - " +
                "${currentMonth.plus(DatePeriod(days = 6)).day}, ${currentMonth.year}"
          }
          CalendarViewMode.DAY -> {
            "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.day}, ${currentMonth.year}"
          }
        }

    Text(
        text = headerText,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
    )

    Row {
      IconButton(onClick = onToggleViewMode) {
        val icon =
            when (calendarViewMode) {
              CalendarViewMode.DAY -> Icons.Filled.CalendarViewDay
              CalendarViewMode.WEEK -> Icons.Filled.CalendarViewWeek
              CalendarViewMode.MONTH -> Icons.Filled.CalendarMonth
            }
        Icon(icon, contentDescription = "Toggle Calendar View")
      }
      IconButton(onClick = onNextClick) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
      }
    }
  }
}

@Composable
fun DayView(
    events: List<CalendarEvent>,
    onAddClick: () -> Unit,
    onConsume: (Uuid) -> Unit,
    onEditPlan: (Uuid) -> Unit,
    onPrep: (Uuid) -> Unit,
) {
  if (events.isEmpty()) {
    EmptyListMessage(message = "No meals planned for this day.")
  } else {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(events) { event -> MealEventCard(event, onConsume, onEditPlan, onPrep) }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekView(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit,
    onConsume: (Uuid) -> Unit,
    onEditPlan: (Uuid?) -> Unit,
    onPrep: (Uuid) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    dates.forEach { dateModel ->
      stickyHeader {
        ListSectionHeader(
            text =
                "${dateModel.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} ${dateModel.date.day}",
            modifier =
                Modifier.clickable {
                  onDateClick(dateModel)
                  onEditPlan(null)
                },
        )
      }
      if (dateModel.events.isEmpty()) {
        item {
          EmptyListMessage(
              message = "No meals planned for this day.",
              modifier = Modifier.padding(16.dp),
          )
        }
      } else {
        items(dateModel.events) { event -> MealEventCard(event, onConsume, onEditPlan, onPrep) }
      }
    }
  }
}

@Composable
fun MealEventCard(
    event: CalendarEvent,
    onConsume: (Uuid) -> Unit,
    onEditPlan: (Uuid) -> Unit,
    onPrep: (Uuid) -> Unit,
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 4.dp)
              .defaultMinSize(minHeight = 48.dp)
              .pointerHoverIcon(PointerIcon.Hand)
              .clickable { onEditPlan(event.entryId) },
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (event.isConsumed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  else MaterialTheme.colorScheme.surfaceContainerHighest
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      // Color indicator bar
      Box(
          modifier =
              Modifier.fillMaxHeight()
                  .width(6.dp)
                  .background(
                      if (event.isConsumed) MaterialTheme.colorScheme.outline
                      else
                          when (event.mealType) {
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
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = event.mealType.name,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (event.isConsumed) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
            )
            if (event.warnings.isNotEmpty() && !event.isConsumed) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                  Icons.Default.Warning,
                  "Data Quality Issue",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(14.dp),
              )
            }
          }
          Text(
              text = event.title,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              color =
                  if (event.isConsumed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                  else MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = "People: ${event.peopleCount}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (!event.isConsumed) {
          IconButton(onClick = { onPrep(event.entryId) }) {
            Icon(
                Icons.Default.Build,
                "Prepare Batch",
                tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        IconButton(onClick = { onConsume(event.entryId) }) {
          if (event.isConsumed) {
            Icon(
                Icons.Default.CheckCircle,
                "Undo Consumption",
                tint = MaterialTheme.colorScheme.primary,
            )
          } else {
            Icon(
                Icons.Default.CheckCircle,
                "Mark as Consumed",
                tint = MaterialTheme.colorScheme.outline,
            )
          }
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
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
            text = day.name.take(3),
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun CalendarGrid(
    dates: List<CalendarUiState.DateUiModel>,
    onDateClick: (CalendarUiState.DateUiModel) -> Unit,
) {
  LazyVerticalGrid(
      columns = GridCells.Fixed(7),
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    items(dates) { dateModel ->
      CalendarCell(
          dateModel = dateModel,
          modifier = Modifier.aspectRatio(1f),
          onClick = { onDateClick(dateModel) },
      )
    }
  }
}

@Composable
fun CalendarCell(dateModel: CalendarUiState.DateUiModel, modifier: Modifier, onClick: () -> Unit) {
  val containerColor =
      when {
        dateModel.isSelected -> MaterialTheme.colorScheme.primaryContainer
        dateModel.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
      }

  val borderColor =
      if (dateModel.isToday) MaterialTheme.colorScheme.secondary
      else MaterialTheme.colorScheme.outlineVariant
  val hasWarnings = dateModel.events.any { it.warnings.isNotEmpty() && !it.isConsumed }

  Card(
      modifier = modifier.clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = containerColor),
      shape = MaterialTheme.shapes.extraSmall,
      border =
          BorderStroke(
              1.dp,
              if (dateModel.isSelected) MaterialTheme.colorScheme.primary else borderColor,
          ),
  ) {
    Column(
        modifier = Modifier.padding(4.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = dateModel.date.day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (dateModel.isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color =
                when {
                  !dateModel.isCurrentMonth ->
                      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                  dateModel.isToday -> MaterialTheme.colorScheme.secondary
                  else -> MaterialTheme.colorScheme.onSurface
                },
        )
        if (hasWarnings) {
          Spacer(modifier = Modifier.width(2.dp))
          Box(
              modifier =
                  Modifier.size(4.dp)
                      .background(
                          MaterialTheme.colorScheme.error,
                          shape = MaterialTheme.shapes.extraSmall,
                      )
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Row(
          modifier = Modifier.padding(bottom = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        dateModel.events.take(3).forEach { event ->
          Box(
              modifier =
                  Modifier.size(6.dp)
                      .background(
                          color =
                              if (event.isConsumed) MaterialTheme.colorScheme.outline
                              else
                                  when (event.mealType) {
                                    RecipeMealType.Breakfast -> MaterialTheme.colorScheme.tertiary
                                    RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
                                    RecipeMealType.Dinner -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                  },
                          shape = MaterialTheme.shapes.extraSmall,
                      )
          )
        }
        if (dateModel.events.size > 3) {
          Text(
              "+",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
          )
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
    onEditPlan: (Uuid) -> Unit,
    onPrep: (Uuid) -> Unit,
) {

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
          } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
              items(events) { event -> MealEventCard(event, onConsume, onEditPlan, onPrep) }
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
      },
  )
}

@Composable
fun ScheduledMealForm(
    initialDate: LocalDate,
    entryId: Uuid?,
    availableMeals: List<PrePlannedMeal>,
    allRecipes: List<Recipe>,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    allRestaurants: List<Restaurant>,
    existingEntry: ScheduledMeal?,
    onClose: () -> Unit,
    onConfirm:
        (LocalDate, LocalTime, PrePlannedMeal?, Restaurant?, RecipeMealType, Int, Int?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
  var isRestaurant by
      androidx.compose.runtime.saveable.rememberSaveable(existingEntry) {
        mutableStateOf(existingEntry?.restaurantId != null)
      }

  var selectedMeal by
      remember(existingEntry) {
        mutableStateOf(
            existingEntry?.prePlannedMealId?.let { id -> availableMeals.find { it.id == id } }
        )
      }

  var selectedRestaurant by
      remember(existingEntry) {
        mutableStateOf(
            existingEntry?.restaurantId?.let { id -> allRestaurants.find { it.id == id } }
        )
      }

  var selectedType by
      androidx.compose.runtime.saveable.rememberSaveable(existingEntry) {
        mutableStateOf(existingEntry?.mealType ?: RecipeMealType.Dinner)
      }
  var peopleText by
      androidx.compose.runtime.saveable.rememberSaveable(existingEntry) {
        mutableStateOf(existingEntry?.peopleCount?.toString() ?: "")
      }
  var costText by
      androidx.compose.runtime.saveable.rememberSaveable(existingEntry) {
        mutableStateOf(existingEntry?.anticipatedCostCents?.let { (it / 100.0).toString() } ?: "")
      }
  var currentDisplayDate by
      remember(existingEntry) { mutableStateOf(existingEntry?.date ?: initialDate) }
  var currentDisplayTime by
      remember(existingEntry) { mutableStateOf(existingEntry?.time ?: LocalTime(18, 0)) }

  var expandedMeal by remember { mutableStateOf(false) }
  var expandedRestaurant by remember { mutableStateOf(false) }
  var expandedType by remember { mutableStateOf(false) }

  val initialDateMillis = currentDisplayDate.atStartOfDayIn(KTimeZone.UTC).toEpochMilliseconds()
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis = initialDateMillis,
          initialDisplayedMonthMillis = initialDateMillis,
          initialDisplayMode = DisplayMode.Picker,
          yearRange = (initialDate.year - 10)..(initialDate.year + 10),
      )
  var showDatePicker by remember { mutableStateOf(false) }

  val timePickerState =
      rememberTimePickerState(
          initialHour = currentDisplayTime.hour,
          initialMinute = currentDisplayTime.minute,
      )
  var showTimePicker by remember { mutableStateOf(false) }

  // Resolve ingredients for scaling
  val scaledIngredients =
      remember(isRestaurant, selectedMeal, peopleText, allRecipes, allIngredients, allUnits) {
        if (isRestaurant) return@remember emptyList<String>()
        val count = peopleText.toIntOrNull() ?: 0
        if (selectedMeal == null || count <= 0) return@remember emptyList<String>()

        val ingredientsMap = allIngredients.associateBy { it.id }
        val recipesMap = allRecipes.associateBy { it.id }
        val unitsMap = allUnits.associateBy { it.id }

        val resolvedIngredients = mutableMapOf<Uuid, Double>()
        val ingredientUnits = mutableMapOf<Uuid, Uuid?>()

        fun resolveRecipe(rId: Uuid, scale: Double, visited: Set<Uuid> = emptySet()) {
          if (visited.contains(rId)) return
          val recipe = recipesMap[rId] ?: return
          val currentScale = if (recipe.servings > 0) scale / recipe.servings else scale

          val newVisited = visited + rId
          recipe.requirementGroups
              .flatMap { it.requirements }
              .forEach { ri ->
                if (ri.subRecipeId != null) {
                  resolveRecipe(ri.subRecipeId, currentScale * ri.quantity, newVisited)
                } else if (ri.ingredientId != null) {
                  resolvedIngredients[ri.ingredientId] =
                      (resolvedIngredients[ri.ingredientId] ?: 0.0) + (ri.quantity * currentScale)
                  ingredientUnits[ri.ingredientId] = ri.unitId
                }
              }
        }

        selectedMeal?.let { meal ->
          meal.recipes.forEach { resolveRecipe(it, count.toDouble()) }
          meal.independentIngredients.forEach { mi ->
            resolvedIngredients[mi.ingredientId] =
                (resolvedIngredients[mi.ingredientId] ?: 0.0) + (mi.quantity * count)
            ingredientUnits[mi.ingredientId] = mi.unitId
          }
        }

        resolvedIngredients
            .map { (id, qty) ->
              val name = ingredientsMap[id]?.name ?: "Unknown"
              val unitName = unitsMap[ingredientUnits[id]]?.abbreviation ?: "?"
              "$name: ${String.format("%.1f", qty)} $unitName"
            }
            .sorted()
      }

  io.github.and19081.mealplanner.uicomponents.MpDetailScaffold(
      title = if (entryId == null) "Plan Meal" else "Edit Plan",
      onClose = onClose,
      onSave = {
        val count = peopleText.toIntOrNull()
        val cost = costText.toDoubleOrNull()?.let { (it * 100).toInt() }
        if (count != null) {
          if (isRestaurant) {
            if (selectedRestaurant != null) {
              onConfirm(
                  currentDisplayDate,
                  currentDisplayTime,
                  null,
                  selectedRestaurant,
                  selectedType,
                  count,
                  cost,
              )
            }
          } else {
            if (selectedMeal != null) {
              onConfirm(
                  currentDisplayDate,
                  currentDisplayTime,
                  selectedMeal,
                  null,
                  selectedType,
                  count,
                  cost,
              )
            }
          }
        }
      },
      saveEnabled =
          (if (isRestaurant) selectedRestaurant != null else selectedMeal != null) &&
              peopleText.toIntOrNull() != null,
      onDelete = onDelete,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
          Text("Home Meal")
        }
        SegmentedButton(
            selected = isRestaurant,
            onClick = { isRestaurant = true },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
          Text("Restaurant")
        }
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
                  onClick = {
                    selectedMeal = meal
                    expandedMeal = false
                  },
              )
            }
          }
        }
      } else {
        // Restaurant Selector
        Box {
          OutlinedButton(
              onClick = { expandedRestaurant = true },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text(selectedRestaurant?.name ?: "Select Restaurant")
          }
          DropdownMenu(
              expanded = expandedRestaurant,
              onDismissRequest = { expandedRestaurant = false },
          ) {
            allRestaurants.forEach { restaurant ->
              DropdownMenuItem(
                  text = { Text(restaurant.name) },
                  onClick = {
                    selectedRestaurant = restaurant
                    expandedRestaurant = false
                  },
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
                  onClick = {
                    selectedType = type
                    expandedType = false
                  },
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
            modifier = Modifier.weight(1f),
        )
      }

      if (isRestaurant) {
        MpOutlinedTextField(
            value = costText,
            onValueChange = { costText = it },
            label = { Text("Anticipated Cost ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
      }

      if (!isRestaurant && scaledIngredients.isNotEmpty()) {
        Text("Needed Ingredients:", style = MaterialTheme.typography.labelMedium)
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
          Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
            scaledIngredients.forEach { line ->
              Text(line, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }

  if (showDatePicker) {
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                  currentDisplayDate =
                      Instant.fromEpochMilliseconds(millis).toLocalDateTime(KTimeZone.UTC).date
                }
                showDatePicker = false
              }
          ) {
            Text("Ok")
          }
        },
        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
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
          ) {
            Text("Ok")
          }
        },
        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        text = { TimePicker(state = timePickerState) },
    )
  }
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
    onConfirm:
        (LocalDate, LocalTime, PrePlannedMeal?, Restaurant?, RecipeMealType, Int, Int?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier =
          Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f).windowInsetsPadding(WindowInsets.ime),
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
      content = {
        ScheduledMealForm(
            initialDate = initialDate,
            entryId = entryId,
            availableMeals = availableMeals,
            allRecipes = allRecipes,
            allIngredients = allIngredients,
            allUnits = allUnits,
            allRestaurants = allRestaurants,
            existingEntry = existingEntry,
            onClose = onDismiss,
            onConfirm = onConfirm,
            onDelete = onDelete,
        )
      },
  )
}

@Composable
fun ServeMealDialog(
    mealName: String,
    isRestaurant: Boolean,
    prePlannedMeal: PrePlannedMeal? = null,
    allRecipes: List<Recipe> = emptyList(),
    peopleCount: Int = 1,
    onDismiss: () -> Unit,
    onConfirmRestaurant: (Int, Int, List<Triple<String, Double, Int>>) -> Unit,
    onConfirmPrePlanned: (List<Pair<Uuid, Double>>) -> Unit,
) {
  var isItemized by remember { mutableStateOf(false) }
  var totalText by remember { mutableStateOf("") }
  var taxText by remember { mutableStateOf("") }

  // Restaurant Items: Name, Qty, PriceCents
  val restaurantItems = remember { mutableStateListOf<Triple<String, String, String>>() }

  // Pre-planned Leftovers: RecipeId, RecipeName, ServingsCookedStr, LeftoversStr
  data class LeftoverEntry(val recipeId: Uuid, val name: String, var cooked: String, var leftovers: String)
  val recipeLeftovers = remember(prePlannedMeal) {
    val list = mutableStateListOf<LeftoverEntry>()
    prePlannedMeal?.recipes?.forEach { rid ->
      val recipe = allRecipes.find { it.id == rid }
      if (recipe != null) {
        val servingsPerBatch = if (recipe.servings > 0) recipe.servings else 1.0
        val batches = max(1.0, ceil(peopleCount / servingsPerBatch))
        val totalCooked = batches * servingsPerBatch
        val expectedLeftovers = max(0.0, totalCooked - peopleCount)
        list.add(LeftoverEntry(rid, recipe.name, totalCooked.toString(), expectedLeftovers.toString()))
      }
    }
    list
  }

  val computedTotalCents =
      remember(restaurantItems, taxText) {
        val itemsTotal =
            restaurantItems.sumOf { (_, _, price) -> ((price.toDoubleOrNull() ?: 0.0) * 100).toInt() }
        val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
        itemsTotal + tax
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Serve $mealName") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isRestaurant) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              SegmentedButton(
                  selected = !isItemized,
                  onClick = { isItemized = false },
                  shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
              ) {
                Text("Total Only")
              }
              SegmentedButton(
                  selected = isItemized,
                  onClick = { isItemized = true },
                  shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
              ) {
                Text("Itemized")
              }
            }

            if (!isItemized) {
              MpOutlinedTextField(
                  value = totalText,
                  onValueChange = { totalText = it },
                  label = { Text("Total Cost ($)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.fillMaxWidth(),
              )
              MpOutlinedTextField(
                  value = taxText,
                  onValueChange = { taxText = it },
                  label = { Text("Tax ($)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.fillMaxWidth(),
              )
            } else {
              MpOutlinedTextField(
                  value = taxText,
                  onValueChange = { taxText = it },
                  label = { Text("Tax ($)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.fillMaxWidth(),
              )

              Text("Items:", style = MaterialTheme.typography.labelMedium)

              Column(
                  modifier =
                      Modifier.fillMaxWidth()
                          .heightIn(max = 200.dp)
                          .verticalScroll(rememberScrollState())
              ) {
                restaurantItems.forEachIndexed { index, item ->
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    MpOutlinedTextField(
                        value = item.first,
                        onValueChange = { restaurantItems[index] = item.copy(first = it) },
                        label = { Text("Name") },
                        modifier = Modifier.weight(2f),
                    )
                    MpOutlinedTextField(
                        value = item.third,
                        onValueChange = { restaurantItems[index] = item.copy(third = it) },
                        label = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { restaurantItems.removeAt(index) }) {
                      Icon(Icons.Default.Close, "Remove")
                    }
                  }
                }

                TextButton(onClick = { restaurantItems.add(Triple("", "1.0", "")) }) {
                  Icon(Icons.Default.Add, null)
                  Text("Add Item")
                }
              }

              Text(
                  "Computed Total: $${String.format("%.2f", computedTotalCents / 100.0)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
              )
            }
          } else {
            // Pre-Planned Meal: Verify Servings and Leftovers
            Text("Verify servings and leftovers for this meal.", style = MaterialTheme.typography.bodyMedium)

            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                recipeLeftovers.forEachIndexed { index, entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MpOutlinedTextField(
                                    value = entry.cooked,
                                    onValueChange = { 
                                        recipeLeftovers[index] = entry.copy(cooked = it)
                                    },
                                    label = { Text("Servings Cooked") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                MpOutlinedTextField(
                                    value = entry.leftovers,
                                    onValueChange = { 
                                        recipeLeftovers[index] = entry.copy(leftovers = it)
                                    },
                                    label = { Text("Leftover Servings") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (recipeLeftovers.isEmpty()) {
                    Text("No recipes in this meal to track leftovers for.", style = MaterialTheme.typography.bodySmall)
                }
            }
          }
        }
      },
      confirmButton = {
        DialogActionButtons(
            onCancel = onDismiss,
            onSave = {
              if (isRestaurant) {
                if (isItemized) {
                  val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                  val finalItems =
                      restaurantItems.map { (name, qty, price) ->
                        Triple(
                            name,
                            qty.toDoubleOrNull() ?: 1.0,
                            ((price.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                        )
                      }
                  onConfirmRestaurant(computedTotalCents, tax, finalItems)
                } else {
                  val total = ((totalText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                  val tax = ((taxText.toDoubleOrNull() ?: 0.0) * 100).toInt()
                  onConfirmRestaurant(total, tax, emptyList())
                }
              } else {
                val leftovers = recipeLeftovers.map { it.recipeId to (it.leftovers.toDoubleOrNull() ?: 0.0) }
                onConfirmPrePlanned(leftovers)
              }
            },
            saveLabel = "Serve",
            saveEnabled = if (isRestaurant) {
                if (isItemized) true else totalText.toDoubleOrNull() != null
            } else true,
        )
      },
  )
}
