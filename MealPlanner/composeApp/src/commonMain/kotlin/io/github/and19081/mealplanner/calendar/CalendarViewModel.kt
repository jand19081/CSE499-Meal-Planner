@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.Meal
import io.github.and19081.mealplanner.MealPlanEntry
import io.github.and19081.mealplanner.MealPlannerRepository
import io.github.and19081.mealplanner.MealType
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
    val availableMeals: List<Meal> = emptyList()
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

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        MealPlanRepository.entries,
        MealPlannerRepository.meals
    ) { currentMonth, selectedDate, entries, meals ->

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateList = CalendarDataSource.getDates(currentMonth, selectedDate)

        val entriesByDate = entries.groupBy { it.date }
        val mealsById = meals.associateBy { it.id }

        val dateUiModels = dateList.map { date ->
            val daysEntries = entriesByDate[date] ?: emptyList()

            val resolvedEvents = daysEntries.map { entry ->
                val mealName = mealsById[entry.mealId]?.name ?: "Unknown Meal"

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = mealName,
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
            availableMeals = meals
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

    fun addPlan(date: LocalDate, meal: Meal, mealType: MealType, servings: Double) {
        val newEntry = MealPlanEntry(
            date = date,
            mealType = mealType,
            targetServings = servings,
            mealId = meal.id
        )

        MealPlanRepository.addPlan(newEntry)
    }
}