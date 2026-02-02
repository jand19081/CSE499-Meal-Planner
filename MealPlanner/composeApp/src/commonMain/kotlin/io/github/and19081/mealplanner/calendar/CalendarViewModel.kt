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
    val availableMeals: List<Meal> = emptyList(),
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
                val mealName = mealsById[entry.mealId]?.name ?: "Unknown Meal"

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = mealName,
                    servings = entry.targetServings,
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

    fun addPlan(date: LocalDate, meal: Meal, mealType: MealType, servings: Double) {
        val valResult = io.github.and19081.mealplanner.domain.Validators.validateServings(servings)
        if (valResult.isFailure) {
            _errorMessage.value = valResult.exceptionOrNull()?.message
            return
        }

        _errorMessage.value = null
        val newEntry = MealPlanEntry(
            date = date,
            mealType = mealType,
            targetServings = servings,
            mealId = meal.id
        )

        MealPlanRepository.addPlan(newEntry)
    }

    fun consumeMeal(entryId: Uuid) {
        val entry = MealPlanRepository.entries.value.find { it.id == entryId } ?: return
        if (entry.isConsumed) return

        // Mark as consumed
        MealPlanRepository.markConsumed(entryId)

        // Decrement Pantry Logic
        val meal = MealRepository.meals.value.find { it.id == entry.mealId } ?: return
        val components = MealRepository.mealComponents.value.filter { it.mealId == meal.id }
        val allRecipes = RecipeRepository.recipes.value
        val allRecipeIngredients = RecipeRepository.recipeIngredients.value
        val allIngredients = IngredientRepository.ingredients.value
        
        // Helper to decrement
        fun decrement(ingId: Uuid, qtyUsed: Double, unit: MeasureUnit) {
            // Get Current Pantry
            val currentPantryItem = PantryRepository.pantryItems.value.find { it.ingredientId == ingId }
            if (currentPantryItem == null) return

            val ingredient = allIngredients.find { it.id == ingId }
            val bridges = ingredient?.conversionBridges ?: emptyList()

            // Convert used qty to pantry unit
            val usedInPantryUnit = UnitConverter.convert(
                amount = qtyUsed,
                from = unit,
                to = currentPantryItem.quantityOnHand.unit,
                bridges = bridges
            )
            
            val newQty = max(0.0, currentPantryItem.quantityOnHand.amount - usedInPantryUnit)
            
            PantryRepository.updateQuantity(ingId, Measure(newQty, currentPantryItem.quantityOnHand.unit))
        }

        components.forEach { comp ->
            if (comp.ingredientId != null) {
                if (comp.quantity != null) {
                    decrement(comp.ingredientId, comp.quantity.amount * entry.targetServings, comp.quantity.unit)
                }
            } else if (comp.recipeId != null) {
                val recipe = allRecipes.find { it.id == comp.recipeId }
                if (recipe != null) {
                    val scale = if (recipe.baseServings > 0) entry.targetServings / recipe.baseServings else 1.0
                    val ris = allRecipeIngredients.filter { it.recipeId == recipe.id }
                    ris.forEach { ri ->
                        decrement(ri.ingredientId, ri.quantity.amount * scale, ri.quantity.unit)
                    }
                }
            }
        }
    }
}
