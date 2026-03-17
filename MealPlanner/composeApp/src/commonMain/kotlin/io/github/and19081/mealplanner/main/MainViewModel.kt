package io.github.and19081.mealplanner.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.and19081.mealplanner.calendar.CalendarViewMode
import io.github.and19081.mealplanner.kitchen.KitchenModal
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class MainViewModel : ViewModel() {

  var selectedRailIndex = mutableIntStateOf(0)
    private set

  var isNavRailVisible = mutableStateOf(true)
    private set

  private val _currentMonth =
      MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
  val currentMonth: MutableStateFlow<LocalDate> = _currentMonth

  private val _calendarViewMode = mutableStateOf(CalendarViewMode.MONTH)
  val calendarViewMode: State<CalendarViewMode> = _calendarViewMode

  private val _modalStack = MutableStateFlow<List<KitchenModal>>(emptyList())
  val modalStack = _modalStack

  var shouldActivateCanMakeNowFilter = mutableStateOf(false)
    private set

  fun setActivateCanMakeNowFilter(active: Boolean) {
    shouldActivateCanMakeNowFilter.value = active
  }

  fun pushModal(modal: KitchenModal) {
    _modalStack.update { it + modal }
  }

  fun popModal() {
    _modalStack.update { it.dropLast(1) }
  }

  fun onRailItemClicked(index: Int) {
    selectedRailIndex.intValue = index
  }

  fun toggleRailVisibility() {
    isNavRailVisible.value = !isNavRailVisible.value
  }

  fun toggleCalendarViewMode() {
    _calendarViewMode.value =
        when (_calendarViewMode.value) {
          CalendarViewMode.DAY -> CalendarViewMode.WEEK
          CalendarViewMode.WEEK -> CalendarViewMode.MONTH
          CalendarViewMode.MONTH -> CalendarViewMode.DAY
        }
  }

  fun updateReferenceDate(date: LocalDate) {
    _currentMonth.value = date
  }

  fun onDateArrowClick(isNext: Boolean) {
    when (calendarViewMode.value) {
      CalendarViewMode.DAY -> if (isNext) toNextDay() else toPreviousDay()
      CalendarViewMode.WEEK -> if (isNext) toNextWeek() else toPreviousWeek()
      CalendarViewMode.MONTH -> if (isNext) toNextMonth() else toPreviousMonth()
    }
  }

  private fun toNextDay() {
    _currentMonth.update { it.plus(DatePeriod(days = 1)) }
  }

  private fun toPreviousDay() {
    _currentMonth.update { it.minus(DatePeriod(days = 1)) }
  }

  private fun toNextWeek() {
    _currentMonth.update { it.plus(DatePeriod(days = 7)) }
  }

  private fun toPreviousWeek() {
    _currentMonth.update { it.minus(DatePeriod(days = 7)) }
  }

  private fun toNextMonth() {
    _currentMonth.update { it.plus(DatePeriod(months = 1)) }
  }

  private fun toPreviousMonth() {
    _currentMonth.update { it.minus(DatePeriod(months = 1)) }
  }
}
