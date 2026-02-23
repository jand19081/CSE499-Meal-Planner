@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.uicomponents.MpOutlinedTextField
import io.github.and19081.mealplanner.uicomponents.MpValidationWarning
import kotlin.uuid.ExperimentalUuidApi
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

@Composable
fun AnalyticsView(
    viewModel: AnalyticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTrip by remember { mutableStateOf<ReceiptHistory?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("Financial Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Date Range Selector
                ScrollableTabRow(
                    selectedTabIndex = uiState.currentDateRange.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    AnalyticsDateRange.entries.forEach { range ->
                        Tab(
                            selected = uiState.currentDateRange == range,
                            onClick = { 
                                if (range == AnalyticsDateRange.CUSTOM) showCustomDatePicker = true
                                else viewModel.setDateRange(range) 
                            },
                            text = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Filter Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsFilter.entries.forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = uiState.currentFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AnalyticsFilter.entries.size)
                        ) { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
                
                Text(
                    text = "${uiState.startDate} - ${uiState.endDate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        item {
            MpValidationWarning(warnings = uiState.warnings)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Actual Spending", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.actualTotalCents / 100.0)}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Projected Spending (Future)", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format("%.2f", uiState.projectedTotalCents / 100.0)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg / Meal", style = MaterialTheme.typography.titleSmall)
                        Text("$${String.format("%.2f", uiState.avgMealCostCents / 100.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg / Person", style = MaterialTheme.typography.titleSmall)
                        Text("$${String.format("%.2f", uiState.avgCostPerPersonCents / 100.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Spending by Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.spendingByLocation.forEach { (location, amount) ->
                        ListItem(
                            headlineContent = { Text(location) },
                            trailingContent = { Text("$${String.format("%.2f", amount / 100.0)}", fontWeight = FontWeight.Bold) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider()
                    }
                    if (uiState.spendingByLocation.isEmpty()) {
                        Text("No data for this range.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        if (uiState.currentFilter != AnalyticsFilter.RESTAURANTS) {
            item {
                Text("Grocery Trips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        uiState.recentShoppingTrips.forEach { trip ->
                            val storeName = uiState.allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
                            ListItem(
                                modifier = Modifier.clickable { selectedTrip = trip },
                                headlineContent = { Text(storeName) },
                                supportingContent = { Text(trip.date.toString()) },
                                trailingContent = { Text("$${String.format("%.2f", trip.actualTotalCents / 100.0)}", fontWeight = FontWeight.Bold) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                Text("Restaurant Meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        uiState.recentRestaurantMeals.forEach { trip ->
                            val restName = uiState.allRestaurants.find { it.id == trip.restaurantId }?.name ?: "Unknown Restaurant"
                            ListItem(
                                modifier = Modifier.clickable { selectedTrip = trip },
                                headlineContent = { Text(restName) },
                                supportingContent = { Text(trip.date.toString()) },
                                trailingContent = { Text("$${String.format("%.2f", trip.actualTotalCents / 100.0)}", fontWeight = FontWeight.Bold) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
            Text("Most Expensive Home Meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    uiState.mostExpensiveMeals.forEachIndexed { index, (name, cost) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            trailingContent = { Text("$${String.format("%.2f", cost / 100.0)}", fontWeight = FontWeight.Bold) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
    
    val scope = rememberCoroutineScope()
    selectedTrip?.let { trip ->
        val locName = if (trip.restaurantId != null) {
            uiState.allRestaurants.find { it.id == trip.restaurantId }?.name ?: "Unknown Restaurant"
        } else {
            uiState.allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
        }
        
        var fullTrip by remember { mutableStateOf<ReceiptHistory?>(null) }
        LaunchedEffect(trip.id) {
            fullTrip = viewModel.getTripDetails(trip.id)
        }

        fullTrip?.let { currentFullTrip ->
            EditReceiptDialog(
                trip = currentFullTrip,
                locationName = locName,
                allIngredients = uiState.allIngredients,
                allUnits = uiState.allUnits,
                onDismiss = { selectedTrip = null },
                onSave = { updatedTrip ->
                    viewModel.updateTrip(updatedTrip)
                    selectedTrip = null
                },
                onDelete = {
                    viewModel.deleteTrip(trip.id)
                    selectedTrip = null
                }
            )
        } ?: AlertDialog(
            onDismissRequest = { selectedTrip = null },
            title = { Text("Loading...") },
            text = { CircularProgressIndicator() },
            confirmButton = {}
        )
    }

    if (showCustomDatePicker) {
        CustomDateRangePicker(
            onDismiss = { showCustomDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showCustomDatePicker = false
            }
        )
    }
}

@Composable
fun EditReceiptDialog(
    trip: ReceiptHistory,
    locationName: String,
    allIngredients: List<Ingredient>,
    allUnits: List<UnitModel>,
    onDismiss: () -> Unit,
    onSave: (ReceiptHistory) -> Unit,
    onDelete: () -> Unit
) {
    var actualTotalStr by remember { mutableStateOf((trip.actualTotalCents / 100.0).toString()) }
    var taxPaidStr by remember { mutableStateOf((trip.taxPaidCents / 100.0).toString()) }
    var lineItems by remember { mutableStateOf(trip.lineItems) }
    var selectedTime by remember { mutableStateOf(trip.time) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Receipt: $locationName") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date: ${trip.date}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Button(onClick = { showTimePicker = true }) {
                        Text(selectedTime.toString())
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                MpOutlinedTextField(
                    value = actualTotalStr,
                    onValueChange = { actualTotalStr = it },
                    label = { Text("Total Paid ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                MpOutlinedTextField(
                    value = taxPaidStr,
                    onValueChange = { taxPaidStr = it },
                    label = { Text("Tax Paid ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Line Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                lineItems.forEachIndexed { index, item ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        val name = item.customName ?: allIngredients.find { it.id == item.ingredientId }?.name ?: "Unknown"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { lineItems = lineItems.toMutableList().apply { removeAt(index) } }) {
                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MpOutlinedTextField(
                                value = item.quantityBought.toString(),
                                onValueChange = { qty ->
                                    val q = qty.toDoubleOrNull() ?: 0.0
                                    lineItems = lineItems.toMutableList().apply { this[index] = item.copy(quantityBought = q) }
                                },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            MpOutlinedTextField(
                                value = (item.pricePaidCents / 100.0).toString(),
                                onValueChange = { price ->
                                    val p = ((price.toDoubleOrNull() ?: 0.0) * 100).toInt()
                                    lineItems = lineItems.toMutableList().apply { this[index] = item.copy(pricePaidCents = p) }
                                },
                                label = { Text("Price ($)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedTrip = trip.copy(
                    actualTotalCents = ((actualTotalStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                    taxPaidCents = ((taxPaidStr.toDoubleOrNull() ?: 0.0) * 100).toInt(),
                    lineItems = lineItems,
                    time = selectedTime
                )
                onSave(updatedTrip)
            }) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Trip")
            }
        }
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Ok") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTimePicker = false 
                }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
fun CustomDateRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                    }
                    val end = dateRangePickerState.selectedEndDateMillis?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                    }
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
            modifier = Modifier.fillMaxWidth().height(500.dp)
        )
    }
}
