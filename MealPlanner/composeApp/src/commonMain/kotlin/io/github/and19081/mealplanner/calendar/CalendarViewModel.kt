package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
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
import kotlin.uuid.Uuid
import kotlin.math.max

// The UI State
data class CalendarUiState(
    val currentMonth: LocalDate,
    val dates: List<DateUiModel>,
    val weekDates: List<DateUiModel>,
    val availableMeals: List<PrePlannedMeal> = emptyList(),
    val errorMessage: String? = null
) {
    data class DateUiModel(
        val date: LocalDate,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val isSelected: Boolean,
        val events: List<CalendarEvent> = emptyList()
    )
}

data class CalendarEvent(
    val entryId: Uuid,
    val mealType: RecipeMealType,
    val title: String,
    val peopleCount: Int,
    val isConsumed: Boolean
)

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
class CalendarViewModel(
    currentMonthFlow: StateFlow<LocalDate>
) : ViewModel() {
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        currentMonthFlow,
        _selectedDate,
        MealPlanRepository.entries,
        MealRepository.meals,
        _errorMessage
    ) { currentMonth, selectedDate, entries, meals, error ->

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateList = CalendarDataSource.getDates(currentMonth, selectedDate)

        val entriesByDate = entries.groupBy { it.date }
        val mealsById = meals.associateBy { it.id }

        val dateUiModels = dateList.map { date ->
            val daysEntries = entriesByDate[date] ?: emptyList()

            val resolvedEvents = daysEntries.map { entry ->
                val mealName = mealsById[entry.prePlannedMealId]?.name ?: "Unknown Meal"

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = mealName,
                    peopleCount = entry.peopleCount,
                    isConsumed = entry.isConsumed
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

        val weekDates = if (selectedDate != null) {
            val dayOfWeek = selectedDate.dayOfWeek.ordinal
            dateUiModels.filter {
                it.date >= selectedDate.minus(DatePeriod(days = dayOfWeek)) &&
                        it.date <= selectedDate.plus(DatePeriod(days = 6 - dayOfWeek))
            }
        } else {
            val todayDayOfWeek = today.dayOfWeek.ordinal
            dateUiModels.filter {
                it.date >= today.minus(DatePeriod(days = todayDayOfWeek)) &&
                        it.date <= today.plus(DatePeriod(days = 6 - todayDayOfWeek))
            }
        }


        CalendarUiState(
            currentMonth = currentMonth,
            dates = dateUiModels,
            weekDates = weekDates,
            availableMeals = meals,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(Clock.System.todayIn(TimeZone.currentSystemDefault()), emptyList(), emptyList())
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addPlan(date: LocalDate, meal: PrePlannedMeal, mealType: RecipeMealType, peopleCount: Int) {
        _errorMessage.value = null
        val newEntry = ScheduledMeal(
            date = date,
            mealType = mealType,
            peopleCount = peopleCount,
            prePlannedMealId = meal.id
        )

        MealPlanRepository.addPlan(newEntry)
    }

    fun consumeMeal(entryId: Uuid) {
        val entry = MealPlanRepository.entries.value.find { it.id == entryId } ?: return
        if (entry.isConsumed) return

        // Mark as consumed
        MealPlanRepository.markConsumed(entryId)

        // Decrement Pantry Logic
        val meal = MealRepository.meals.value.find { it.id == entry.prePlannedMealId } ?: return
        
        val allRecipes = RecipeRepository.recipes.value
        val allIngredients = IngredientRepository.ingredients.value
        val allBridges = IngredientRepository.bridges.value
        val allUnits = UnitRepository.units.value
        
        // Helper to decrement
        fun decrement(ingId: Uuid, qtyUsed: Double, unitId: Uuid) {
            // Get Current Pantry
            val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == ingId }
            if (currentPantryItem == null) return

            val ingredient = allIngredients.find { it.id == ingId }
            val bridges = allBridges.filter { it.ingredientId == ingId }

            // Convert used qty to pantry unit
            val usedInPantryUnit = UnitConverter.convert(
                amount = qtyUsed,
                fromUnitId = unitId,
                toUnitId = currentPantryItem.unitId,
                allUnits = allUnits,
                bridges = bridges
            )
            
            val newQty = max(0.0, currentPantryItem.quantity - usedInPantryUnit)
            
            PantryRepository.updateQuantity(ingId, newQty, currentPantryItem.unitId)
        }

        // 1. Recipes
        meal.recipes.forEach { rId ->
            val recipe = allRecipes.find { it.id == rId }
            if (recipe != null) {
                val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                recipe.ingredients.forEach { ri ->
                    decrement(ri.ingredientId, ri.quantity * scale, ri.unitId)
                }
            }
        }
        
        // 2. Independent Ingredients
        meal.independentIngredients.forEach { item ->
            // Assume quantity is per person
            val totalQty = item.quantity * entry.peopleCount
            decrement(item.ingredientId, totalQty, item.unitId)
        }
    }
}
