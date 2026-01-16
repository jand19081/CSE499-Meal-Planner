package io.github.and19081.mealplanner.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import io.github.and19081.mealplanner.MealType
import io.github.and19081.mealplanner.Recipe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

@Composable
fun CalendarView() {
    val viewModel = viewModel { CalendarViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                showAddDialog = true
            }
        )
    }

    if (showAddDialog && selectedDateForDialog != null) {
        AddPlanDialog(
            date = selectedDateForDialog!!,
            availableRecipes = uiState.availableRecipes,
            onDismiss = { showAddDialog = false },
            onConfirm = { recipe, mealType, servings ->
                viewModel.addPlan(selectedDateForDialog!!, recipe, mealType, servings)
                showAddDialog = false
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

            // Draw a dot for each planned meal
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
fun AddPlanDialog(
    date: LocalDate,
    availableRecipes: List<Recipe>,
    onDismiss: () -> Unit,
    onConfirm: (Recipe, MealType, Double) -> Unit
) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedType by remember { mutableStateOf(MealType.DINNER) }
    var servingsText by remember { mutableStateOf("4") }

    var expandedRecipe by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Meal for $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Recipe Selector
                Box {
                    OutlinedButton(onClick = { expandedRecipe = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedRecipe?.name ?: "Select Recipe")
                    }
                    DropdownMenu(expanded = expandedRecipe, onDismissRequest = { expandedRecipe = false }) {
                        availableRecipes.forEach { recipe ->
                            DropdownMenuItem(
                                text = { Text(recipe.name) },
                                onClick = {
                                    selectedRecipe = recipe
                                    servingsText = recipe.baseServings.toString() // Auto-fill default servings
                                    expandedRecipe = false
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
                    if (selectedRecipe != null && servings != null) {
                        onConfirm(selectedRecipe!!, selectedType, servings)
                    }
                },
                enabled = selectedRecipe != null && servingsText.toDoubleOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}