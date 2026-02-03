package io.github.and19081.mealplanner.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.meals.MealRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.uuid.Uuid
import io.github.and19081.mealplanner.calendar.CalendarEvent
import io.github.and19081.mealplanner.settings.DashboardConfig
import io.github.and19081.mealplanner.settings.SettingsRepository

class DashboardViewModel : ViewModel() {

    data class DashboardUiState(
        val todaysMeals: List<CalendarEvent>,
        val lowStockCount: Int,
        val shoppingListCount: Int,
        val nextMeal: CalendarEvent?,
        val dashboardConfig: DashboardConfig,
        val currentWeekCost: Long = 0L // Placeholder for cost widget
    )

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val uiState = combine(
        MealPlanRepository.entries,
        MealRepository.meals,
        PantryRepository.pantryItems,
        SettingsRepository.dashboardConfig,
        ReceiptHistoryRepository.trips
    ) { entries, meals, pantry, config, trips ->
        
        val mealsMap = meals.associateBy { it.id }

        // 1. Today's Meals
        val todaysEntries = entries.filter { it.date == today }
            .sortedBy { it.mealType.ordinal }
            
        val todaysEvents = todaysEntries.map { entry ->
            val meal = mealsMap[entry.prePlannedMealId]
            CalendarEvent(
                entryId = entry.id,
                title = meal?.name ?: "Unknown Meal",
                mealType = entry.mealType,
                peopleCount = entry.peopleCount,
                isConsumed = entry.isConsumed
            )
        }

        val nextMeal = todaysEvents.firstOrNull { !it.isConsumed }

        // 2. Low Stock
        val stockCount = pantry.size

        // 3. Shopping List Count
        val shoppingCount = 0 
        
        // 4. Weekly Cost (Sum of actual totals in cents)
        val cost = trips.sumOf { it.actualTotalCents.toLong() }

        DashboardUiState(
            todaysMeals = todaysEvents,
            lowStockCount = stockCount,
            shoppingListCount = shoppingCount,
            nextMeal = nextMeal,
            dashboardConfig = config,
            currentWeekCost = cost
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(emptyList(), 0, 0, null, DashboardConfig()))
    
    fun consumeMeal(entryId: Uuid) {
        MealPlanRepository.markConsumed(entryId)
    }
}
