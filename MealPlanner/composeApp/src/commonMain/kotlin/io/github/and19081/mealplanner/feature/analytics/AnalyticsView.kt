@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.ui.components.MpDetailScaffold
import io.github.and19081.mealplanner.ui.components.MpOutlinedTextField
import io.github.and19081.mealplanner.ui.components.MpValidationWarning
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.find
import kotlin.collections.forEach

@Composable
fun AnalyticsView(viewModel: AnalyticsViewModel, mode: Mode, isExpanded: Boolean) {
  val uiState by viewModel.uiState.collectAsState()

  var selectedTrip by remember { mutableStateOf<ReceiptHistory?>(null) }
  var showCustomDatePicker by remember { mutableStateOf(false) }

  val actualIsExpanded =
      when (mode) {
        Mode.AUTO -> isExpanded
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
      }

  Scaffold { innerPadding ->
    if (actualIsExpanded) {
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        // Left Column: Summary & Location Breakdown
        Column(
            modifier = Modifier.weight(0.4f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          AnalyticsHeader(
              uiState,
              onDateRangeClick = { showCustomDatePicker = true },
              viewModel,
              isExpanded,
          )
          AnalyticsSummaryCards(uiState)
          CostComparisonSection(uiState)
          AnalyticsLocationBreakdown(uiState)
        }

        VerticalDivider(modifier = Modifier.width(1.dp).padding(horizontal = 16.dp))

        // Right Column: Lists or Form
        Box(modifier = Modifier.weight(0.6f)) {
          selectedTrip?.let { trip ->
            var fullTrip by remember(trip.id) { mutableStateOf<ReceiptHistory?>(null) }
            LaunchedEffect(trip.id) { fullTrip = viewModel.getTripDetails(trip.id) }

            if (fullTrip != null) {
              val currentFullTrip = fullTrip!! // Safe here due to null check above
              val locName =
                  if (currentFullTrip.restaurantId != null) {
                    uiState.allRestaurants.find { it.id == currentFullTrip.restaurantId }?.name
                        ?: "Unknown Restaurant"
                  } else {
                    uiState.allStores.find { it.id == currentFullTrip.storeId }?.name
                        ?: "Unknown Store"
                  }
              ReceiptForm(
                  trip = currentFullTrip,
                  locationName = locName,
                  allIngredients = uiState.allIngredients,
                  allUnits = uiState.allUnits,
                  onClose = { selectedTrip = null },
                  onSave = { updatedTrip ->
                    viewModel.updateTrip(updatedTrip)
                    selectedTrip = null
                  },
                  onDelete = {
                    viewModel.deleteTrip(trip.id)
                    selectedTrip = null
                  },
              )
            } else {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
            }
          }
              ?: run {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                  analyticsListContent(uiState, onTripClick = { selectedTrip = it })
                }
              }
        }
      }
    } else {
      // Mobile View
      selectedTrip?.let { trip ->
        // Mobile branch for full-screen form
        var fullTrip by remember(trip.id) { mutableStateOf<ReceiptHistory?>(null) }
        LaunchedEffect(trip.id) { fullTrip = viewModel.getTripDetails(trip.id) }
        if (fullTrip != null) {
          val currentFullTrip = fullTrip!!
          val locName =
              if (currentFullTrip.restaurantId != null) {
                uiState.allRestaurants.find { it.id == currentFullTrip.restaurantId }?.name
                    ?: "Unknown Restaurant"
              } else {
                uiState.allStores.find { it.id == currentFullTrip.storeId }?.name ?: "Unknown Store"
              }
          ReceiptForm(
              trip = currentFullTrip,
              locationName = locName,
              allIngredients = uiState.allIngredients,
              allUnits = uiState.allUnits,
              onClose = { selectedTrip = null },
              onSave = { updatedTrip ->
                viewModel.updateTrip(updatedTrip)
                selectedTrip = null
              },
              onDelete = {
                viewModel.deleteTrip(trip.id)
                selectedTrip = null
              },
          )
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      }
          ?: run {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
              item {
                AnalyticsHeader(
                    uiState,
                    onDateRangeClick = { showCustomDatePicker = true },
                    viewModel,
                    isExpanded,
                )
              }
              item { AnalyticsSummaryCards(uiState) }
              item { CostComparisonSection(uiState) }
              item { AnalyticsLocationBreakdown(uiState) }
              analyticsListContent(uiState, onTripClick = { selectedTrip = it })
            }
          }
    }
  }

  if (showCustomDatePicker) {
    CustomDateRangePicker(
        onDismiss = { showCustomDatePicker = false },
        onConfirm = { start, end ->
          viewModel.setCustomRange(start, end)
          showCustomDatePicker = false
        },
    )
  }
}

@Composable
fun AnalyticsHeader(
    uiState: AnalyticsUiState,
    onDateRangeClick: () -> Unit,
    viewModel: AnalyticsViewModel,
    isExpanded: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        "Financial Overview",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )

    // Date Range Selector
    if (isExpanded) {
      ScrollableTabRow(
          selectedTabIndex = uiState.currentDateRange.ordinal,
          edgePadding = 0.dp,
          containerColor = Color.Transparent,
          divider = {},
      ) {
        AnalyticsDateRange.entries.forEach { range ->
          Tab(
              selected = uiState.currentDateRange == range,
              onClick = {
                if (range == AnalyticsDateRange.CUSTOM) onDateRangeClick()
                else viewModel.setDateRange(range)
              },
              text = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) },
          )
        }
      }
    } else {
      var expanded by remember { mutableStateOf(false) }
      Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
          Text(
              "Range: ${uiState.currentDateRange.name.lowercase().replaceFirstChar { it.uppercase() }}"
          )
          Spacer(Modifier.width(8.dp))
          Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
          AnalyticsDateRange.entries.forEach { range ->
            DropdownMenuItem(
                text = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) },
                onClick = {
                  if (range == AnalyticsDateRange.CUSTOM) onDateRangeClick()
                  else viewModel.setDateRange(range)
                  expanded = false
                },
            )
          }
        }
      }
    }

    // Filter Selector
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      AnalyticsFilter.entries.forEachIndexed { index, filter ->
        SegmentedButton(
            selected = uiState.currentFilter == filter,
            onClick = { viewModel.setFilter(filter) },
            shape =
                SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AnalyticsFilter.entries.size,
                ),
        ) {
          Text(filter.name.lowercase().replaceFirstChar { it.uppercase() })
        }
      }
    }

    Text(
        text = "${uiState.startDate} - ${uiState.endDate}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )

    MpValidationWarning(warnings = uiState.warnings)
  }
}

@Composable
fun AnalyticsSummaryCards(uiState: AnalyticsUiState) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Actual Spending", style = MaterialTheme.typography.titleMedium)
        Text(
            "$${String.format("%.2f", uiState.actualTotalCents / 100.0)}",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Projected Spending (Future)", style = MaterialTheme.typography.titleMedium)
        Text(
            "$${String.format("%.2f", uiState.projectedTotalCents / 100.0)}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Card(
          modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Avg / Meal", style = MaterialTheme.typography.titleSmall)
          Text(
              "$${String.format("%.2f", uiState.avgMealCostCents / 100.0)}",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
          )
        }
      }
      Card(
          modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Avg / Person", style = MaterialTheme.typography.titleSmall)
          Text(
              "$${String.format("%.2f", uiState.avgCostPerPersonCents / 100.0)}",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun CostComparisonSection(uiState: AnalyticsUiState) {
    val comparisons = listOfNotNull(
        uiState.projectedVsActualWeekly,
        uiState.projectedVsActualMonthly,
        uiState.projectedVsActualAnnual,
    )
    if (comparisons.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Projected vs. Actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            comparisons.forEach { comparison ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(comparison.period, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Projected: $${String.format("%.2f", comparison.projected / 100.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            "Actual: $${String.format("%.2f", comparison.actual / 100.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val diff = comparison.percentDifference
                        val sign = if (diff >= 0) "+" else ""
                        Text(
                            "${sign}${String.format("%.1f", diff)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                if (comparison !== comparisons.last()) HorizontalDivider()
            }
        }
    }
}

@Composable
fun AnalyticsLocationBreakdown(uiState: AnalyticsUiState) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        "Spending by Location",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Column {
        uiState.spendingByLocation.forEach { (location, amount) ->
          ListItem(
              headlineContent = { Text(location) },
              trailingContent = {
                Text("$${String.format("%.2f", amount / 100.0)}", fontWeight = FontWeight.Bold)
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
          HorizontalDivider()
        }
        if (uiState.spendingByLocation.isEmpty()) {
          Text("No data for this range.", modifier = Modifier.padding(16.dp))
        }
      }
    }
  }
}

fun LazyListScope.analyticsListContent(
    uiState: AnalyticsUiState,
    onTripClick: (ReceiptHistory) -> Unit,
) {
  if (uiState.currentFilter != AnalyticsFilter.RESTAURANTS) {
    item {
      Text(
          "Grocery Trips",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
      )
    }

    item {
      Card(
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          uiState.recentShoppingTrips.forEach { trip ->
            val storeName =
                uiState.allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
            ListItem(
                modifier = Modifier.clickable { onTripClick(trip) },
                headlineContent = { Text(storeName) },
                supportingContent = { Text(trip.date.toString()) },
                trailingContent = {
                  Text(
                      "$${String.format("%.2f", trip.actualTotalCents / 100.0)}",
                      fontWeight = FontWeight.Bold,
                  )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            HorizontalDivider()
          }
          if (uiState.recentShoppingTrips.isEmpty()) {
            Text("No trips for this range.", modifier = Modifier.padding(16.dp))
          }
        }
      }
    }
  }

  if (uiState.currentFilter != AnalyticsFilter.STORES) {
    item {
      Text(
          "Restaurant Meals",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
      )
    }

    item {
      Card(
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          uiState.recentRestaurantMeals.forEach { trip ->
            val restName =
                uiState.allRestaurants.find { it.id == trip.restaurantId }?.name
                    ?: "Unknown Restaurant"
            ListItem(
                modifier = Modifier.clickable { onTripClick(trip) },
                headlineContent = { Text(restName) },
                supportingContent = { Text(trip.date.toString()) },
                trailingContent = {
                  Text(
                      "$${String.format("%.2f", trip.actualTotalCents / 100.0)}",
                      fontWeight = FontWeight.Bold,
                  )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            HorizontalDivider()
          }
          if (uiState.recentRestaurantMeals.isEmpty()) {
            Text("No restaurant meals for this range.", modifier = Modifier.padding(16.dp))
          }
        }
      }
    }
  }

  item {
    Text(
        "Most Expensive Home Meals",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
  }

  item {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Column {
        uiState.mostExpensiveMeals.forEachIndexed { index, (name, cost) ->
          ListItem(
              headlineContent = { Text(name) },
              trailingContent = {
                val costStr = if (cost != null) "$${String.format("%.2f", cost / 100.0)}" else "N/A"
                Text(costStr, fontWeight = FontWeight.Bold)
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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

@Composable
fun ReceiptForm(
    trip: ReceiptHistory,
    locationName: String,
    allIngredients: List<FoodItem>,
    allUnits: List<UnitModel>,
    onClose: () -> Unit,
    onSave: (ReceiptHistory) -> Unit,
    onDelete: () -> Unit,
) {
  var actualTotalStr by remember { mutableStateOf((trip.actualTotalCents / 100.0).toString()) }
  var taxPaidStr by remember { mutableStateOf((trip.taxPaidCents / 100.0).toString()) }
  var lineItems by remember { mutableStateOf(trip.lineItems) }
  var selectedTime by remember { mutableStateOf(trip.time) }

  val timePickerState =
      rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute)
  var showTimePicker by remember { mutableStateOf(false) }

    MpDetailScaffold(
        title = "Edit Receipt",
        onClose = onClose,
        onSave = {
          val updatedTrip =
              trip.copy(
                  actualTotalCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                  taxPaidCents = ((taxPaidStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                  lineItems = lineItems,
                  time = selectedTime,
              )
          onSave(updatedTrip)
        },
        onDelete = onDelete,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(locationName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
              "Date: ${trip.date}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.weight(1f),
          )
          Button(onClick = { showTimePicker = true }) { Text(selectedTime.toString()) }
        }

        MpOutlinedTextField(
            value = actualTotalStr,
            onValueChange = { actualTotalStr = it },
            label = { Text("Total Paid ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        MpOutlinedTextField(
            value = taxPaidStr,
            onValueChange = { taxPaidStr = it },
            label = { Text("Tax Paid ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Line Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        lineItems.forEachIndexed { index, item ->
          Column(modifier = Modifier.padding(vertical = 8.dp)) {
            val name =
                item.customName
                    ?: allIngredients.find { it.id == item.measurement.foodItemId }?.name
                    ?: "Unknown"
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
              IconButton(onClick = { lineItems = lineItems.filterIndexed { i, _ -> i != index } }) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              MpOutlinedTextField(
                  value = item.measurement.quantity.toString(),
                  onValueChange = { qty ->
                    val q = qty.toDoubleOrNull() ?: 0.0
                    lineItems =
                        lineItems.mapIndexed { i, old ->
                          if (i == index) old.copy(measurement = old.measurement.copy(quantity = q)) else old
                        }
                  },
                  label = { Text("Qty") },
                  modifier = Modifier.weight(1f),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              )
              MpOutlinedTextField(
                  value = (item.pricePaidCents / 100.0).toString(),
                  onValueChange = { price ->
                    val p = ((price.toDoubleOrNull() ?: 0.0) * 100).toInt()
                    lineItems =
                        lineItems.mapIndexed { i, old ->
                          if (i == index) old.copy(pricePaidCents = p) else old
                        }
                  },
                  label = { Text("Price ($)") },
                  modifier = Modifier.weight(1f),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              )
            }
          }
          HorizontalDivider()
        }
      }
    }

  if (showTimePicker) {
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
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
fun CustomDateRangePicker(onDismiss: () -> Unit, onConfirm: (LocalDate, LocalDate) -> Unit) {
  val dateRangePickerState = rememberDateRangePickerState()

  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              val start =
                  dateRangePickerState.selectedStartDateMillis?.let {
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                  }
              val end =
                  dateRangePickerState.selectedEndDateMillis?.let {
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                  }
              if (start != null && end != null) {
                onConfirm(start, end)
              }
            },
            enabled =
                dateRangePickerState.selectedStartDateMillis != null &&
                    dateRangePickerState.selectedEndDateMillis != null,
        ) {
          Text("Confirm")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  ) {
    DateRangePicker(
        state = dateRangePickerState,
        title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
        modifier = Modifier.fillMaxWidth().height(500.dp),
    )
  }
}
