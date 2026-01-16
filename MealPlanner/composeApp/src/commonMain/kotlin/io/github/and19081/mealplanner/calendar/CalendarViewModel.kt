@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.MealPlanEntry
import io.github.and19081.mealplanner.MealType
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.RecipeRepository
import io.github.and19081.mealplanner.ScheduledItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// The UI State
data class CalendarUiState(
    val currentMonth: LocalDate,
    val dates: List<DateUiModel>,
    val availableRecipes: List<Recipe> = emptyList()
) {
    data class DateUiModel(
        val date: LocalDate,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val isSelected: Boolean,
        val events: List<CalendarEvent> = emptyList()
    )
}

// The Logic to generate the grid
object CalendarDataSource {
    fun getDates(currentMonth: LocalDate, selectedDate: LocalDate?): List<LocalDate> {
        val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
        val startDayOfWeek = firstDayOfMonth.dayOfWeek
        val daysToSubtract = startDayOfWeek.ordinal
        val startOfGrid = firstDayOfMonth.minus(DatePeriod(days = daysToSubtract))

        val dates = mutableListOf<LocalDate>()
        for (i in 0 until 42) {
            dates.add(startOfGrid.plus(DatePeriod(days = i)))
        }
        return dates
    }
}

// The ViewModel
class CalendarViewModel : ViewModel() {
    private val _currentMonth = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)

    private val recipesFlow = RecipeRepository.recipes

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        MealPlanRepository.entries,
        MealPlanRepository.scheduledItems,
        recipesFlow
    ) { currentMonth, selectedDate, entries, items, recipes ->

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateList = CalendarDataSource.getDates(currentMonth, selectedDate)

        val entriesByDate = entries.groupBy { it.date }
        val itemsByEntryId = items.groupBy { it.mealPlanEntryId }
        val recipesById = recipes.associateBy { it.id }

        val dateUiModels = dateList.map { date ->
            val daysEntries = entriesByDate[date] ?: emptyList()

            val resolvedEvents = daysEntries.map { entry ->
                val entryItems = itemsByEntryId[entry.id] ?: emptyList()

                val title = if (entryItems.isNotEmpty()) {
                    val firstItem = entryItems.first()
                    if (firstItem.recipeId != null) {
                        recipesById[firstItem.recipeId]?.name ?: "Unknown Recipe"
                    } else {
                        "Custom Item"
                    }
                } else {
                    "Empty Meal"
                }

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = title,
                    servings = entry.targetServings
                )
            }

            CalendarUiState.DateUiModel(
                date = date,
                isCurrentMonth = date.month == currentMonth.month,
                isToday = date == today,
                isSelected = date == selectedDate,
                events = resolvedEvents
            )
        }

        CalendarUiState(
            currentMonth = currentMonth,
            dates = dateUiModels,
            availableRecipes = recipes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(Clock.System.todayIn(TimeZone.currentSystemDefault()), emptyList())
    )

    fun toNextMonth() {
        _currentMonth.update { it.plus(DatePeriod(months = 1)) }
    }

    fun toPreviousMonth() {
        _currentMonth.update { it.minus(DatePeriod(months = 1)) }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addPlan(date: LocalDate, recipe: Recipe, mealType: MealType, servings: Double) {
        val newEntry = MealPlanEntry(
            date = date,
            mealType = mealType,
            targetServings = servings
        )

        val newItem = ScheduledItem(
            mealPlanEntryId = newEntry.id,
            recipeId = recipe.id
        )

        MealPlanRepository.addPlan(newEntry, listOf(newItem))
    }
}

